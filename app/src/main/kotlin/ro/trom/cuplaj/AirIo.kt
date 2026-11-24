package ro.trom.cuplaj

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.ImageReader
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.VibrationEffect
import android.os.Vibrator

/** Emițătoare și receptoare Android pentru LUMEN, SONUS și TACTUS. Decodarea în sine e în Air.kt (testată pe JVM). */
@SuppressLint("MissingPermission", "NewApi")
object AirIo {
    const val FS = 48000

    // ------------------------------------------------------------------ emisie

    /** LUMEN: lanterna (sau ecranul prin `screen`) urmează sloturile; întoarce false dacă nu există lanternă. */
    fun sendLight(ctx: Context, packet: ByteArray, screen: ((Boolean) -> Unit)?, done: () -> Unit): Boolean {
        val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var torch: String? = null
        if (screen == null) {
            try {
                torch = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
            } catch (_: Exception) {
            }
            if (torch == null) return false
        }
        val slots = Lumen.encode(packet)
        Thread {
            val t0 = System.nanoTime()
            var last = false
            try {
                for ((k, on) in slots.withIndex()) {
                    val target = t0 + (k * Lumen.SLOT_MS * 1e6).toLong()
                    while (System.nanoTime() < target) Thread.sleep(1)
                    if (on != last) {
                        last = on
                        if (screen != null) screen(on) else cm.setTorchMode(torch!!, on)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    if (screen != null) screen(false) else cm.setTorchMode(torch!!, false)
                } catch (_: Exception) {
                }
                done()
            }
        }.apply { isDaemon = true; start() }
        return true
    }

    /** SONUS: redă pachetul pe difuzor (volumul mediei trebuie să fie ridicat). */
    fun sendSound(packet: ByteArray, ultra: Boolean, done: () -> Unit) {
        Thread {
            var tr: AudioTrack? = null
            try {
                val pcm = Sonus.encode(packet, FS, ultra, 0.6)
                tr = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder().setSampleRate(FS).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(pcm.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                tr.write(pcm, 0, pcm.size)
                tr.play()
                Thread.sleep(pcm.size * 1000L / FS + 300)
            } catch (_: Exception) {
            } finally {
                try {
                    tr?.stop()
                    tr?.release()
                } catch (_: Exception) {
                }
                done()
            }
        }.apply { isDaemon = true; start() }
    }

    /** TACTUS: motorul de vibrații urmează duratele rafalelor. */
    @Suppress("DEPRECATION")
    fun sendVibe(ctx: Context, packet: ByteArray): Long {
        val d = Tactus.encode(packet)
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return 0
        val pat = LongArray(d.size + 1)
        for (i in d.indices) pat[i + 1] = d[i].toLong()
        try {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pat, -1)) else v.vibrate(pat, -1)
        } catch (_: Exception) {
        }
        return d.sum().toLong()
    }

    // ------------------------------------------------------------------ recepție

    /** LUMEN: camera (YUV, rezoluție minimă) → luminozitatea medie a cadrului → decodor. */
    class LightRx(private val ctx: Context, private val key: ByteArray?, private val onPacket: (AirPacket) -> Unit) {
        private val buf = ArrayList<LSample>()
        private var cam: CameraDevice? = null
        private var sess: CameraCaptureSession? = null
        private var reader: ImageReader? = null
        private val ht = HandlerThread("cuplaj-air-cam")

        @Volatile
        private var run = false

        fun start(): Boolean {
            if (ctx.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return false
            try {
                val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK }
                    ?: cm.cameraIdList.firstOrNull() ?: return false
                val map = cm.getCameraCharacteristics(id).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val size = map?.getOutputSizes(ImageFormat.YUV_420_888)?.minByOrNull { it.width * it.height } ?: return false
                ht.start()
                val h = Handler(ht.looper)
                val r = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
                reader = r
                r.setOnImageAvailableListener({ rd ->
                    val im = rd.acquireLatestImage()
                    if (im != null) {
                        try {
                            val p = im.planes[0]
                            val b = p.buffer
                            var sum = 0L
                            var n = 0
                            val x0 = size.width / 4
                            val x1 = size.width * 3 / 4
                            val y0 = size.height / 4
                            val y1 = size.height * 3 / 4
                            var y = y0
                            while (y < y1) {
                                var x = x0
                                while (x < x1) {
                                    sum += b.get(y * p.rowStride + x * p.pixelStride).toInt() and 0xFF
                                    n++
                                    x += 3
                                }
                                y += 3
                            }
                            if (n > 0) synchronized(buf) { buf.add(LSample(im.timestamp / 1e6, sum.toDouble() / n)) }
                        } finally {
                            im.close()
                        }
                    }
                }, h)
                run = true
                cm.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(c: CameraDevice) {
                        cam = c
                        try {
                            @Suppress("DEPRECATION")
                            c.createCaptureSession(listOf(r.surface), object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(s: CameraCaptureSession) {
                                    sess = s
                                    try {
                                        val rq = c.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                        rq.addTarget(r.surface)
                                        rq.set(CaptureRequest.CONTROL_AE_LOCK, false)
                                        s.setRepeatingRequest(rq.build(), null, h)
                                    } catch (_: Exception) {
                                    }
                                }

                                override fun onConfigureFailed(s: CameraCaptureSession) {}
                            }, h)
                        } catch (_: Exception) {
                        }
                    }

                    override fun onDisconnected(c: CameraDevice) = c.close()
                    override fun onError(c: CameraDevice, e: Int) = c.close()
                }, h)
                Thread {
                    while (run) {
                        try {
                            Thread.sleep(1000)
                        } catch (_: Exception) {
                        }
                        val win: List<LSample>
                        synchronized(buf) {
                            if (buf.isEmpty()) return@synchronized
                            val last = buf.last().t
                            while (buf.isNotEmpty() && buf.first().t < last - 32000) buf.removeAt(0)
                        }
                        synchronized(buf) { win = ArrayList(buf) }
                        val pk = try { Lumen.decode(win, key) } catch (_: Exception) { null }
                        if (pk != null) {
                            synchronized(buf) { buf.clear() }
                            onPacket(pk)
                        }
                    }
                }.apply { isDaemon = true; start() }
            } catch (_: Exception) {
                stop()
                return false
            }
            return true
        }

        fun stop() {
            run = false
            try {
                sess?.close()
                cam?.close()
                reader?.close()
                ht.quitSafely()
            } catch (_: Exception) {
            }
        }
    }

    /** SONUS: microfon 48 kHz (sursa fără procesare, ca să nu fie tăiat ultrasunetul) → fereastră de 12 s → decodor. */
    class SoundRx(private val ctx: Context, private val ultra: Boolean, private val key: ByteArray?, private val onPacket: (AirPacket) -> Unit) {
        @Volatile
        private var run = false
        private var rec: AudioRecord? = null

        fun start(): Boolean {
            if (ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return false
            val min = AudioRecord.getMinBufferSize(FS, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) return false
            var r: AudioRecord? = null
            for (src in intArrayOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)) {
                try {
                    val x = AudioRecord(src, FS, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 4)
                    if (x.state == AudioRecord.STATE_INITIALIZED) { r = x; break } else x.release()
                } catch (_: Exception) {
                }
            }
            val rr = r ?: return false
            rec = rr
            run = true
            rr.startRecording()
            Thread {
                val ring = ShortArray(FS * 12)
                var pos = 0
                var filled = 0
                val chunk = ShortArray(4800)
                var lastTry = System.currentTimeMillis()
                while (run) {
                    val n = rr.read(chunk, 0, chunk.size)
                    if (n <= 0) continue
                    for (i in 0 until n) { ring[pos] = chunk[i]; pos = (pos + 1) % ring.size }
                    filled = minOf(ring.size, filled + n)
                    if (System.currentTimeMillis() - lastTry > 2000 && filled > FS * 4) {
                        lastTry = System.currentTimeMillis()
                        val w = ShortArray(filled)
                        val start = (pos - filled + ring.size) % ring.size
                        for (i in 0 until filled) w[i] = ring[(start + i) % ring.size]
                        val pk = try { Sonus.decode(w, FS, ultra, key) } catch (_: Exception) { null }
                        if (pk != null) {
                            filled = 0
                            onPacket(pk)
                        }
                    }
                }
            }.apply { isDaemon = true; start() }
            return true
        }

        fun stop() {
            run = false
            try {
                rec?.stop()
                rec?.release()
            } catch (_: Exception) {
            }
            rec = null
        }
    }

    /** TACTUS: accelerometru la rata maximă → decodor de rafale. */
    class VibeRx(private val ctx: Context, private val key: ByteArray?, private val onPacket: (AirPacket) -> Unit) : SensorEventListener {
        private val buf = ArrayList<ASample>()

        @Volatile
        private var run = false
        private var sm: SensorManager? = null

        fun start(): Boolean {
            val m = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
            val s = m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
            sm = m
            run = true
            m.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST)
            Thread {
                while (run) {
                    try {
                        Thread.sleep(1000)
                    } catch (_: Exception) {
                    }
                    val win: List<ASample>
                    synchronized(buf) { win = ArrayList(buf) }
                    val pk = try { Tactus.decode(win, key) } catch (_: Exception) { null }
                    if (pk != null) {
                        synchronized(buf) { buf.clear() }
                        onPacket(pk)
                    }
                }
            }.apply { isDaemon = true; start() }
            return true
        }

        override fun onSensorChanged(e: SensorEvent) {
            synchronized(buf) {
                buf.add(ASample(e.timestamp / 1e6, e.values[0].toDouble(), e.values[1].toDouble(), e.values[2].toDouble()))
                val last = buf.last().t
                var drop = 0
                while (drop < buf.size && buf[drop].t < last - 40000) drop++
                if (drop > 0) buf.subList(0, drop).clear()
            }
        }

        override fun onAccuracyChanged(s: Sensor?, a: Int) {}

        fun stop() {
            run = false
            sm?.unregisterListener(this)
        }
    }
}
