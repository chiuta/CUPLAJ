package ro.trom.cuplaj

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

/** Contor de public: măsoară zgomotul sau mișcarea câteva secunde și trimite DOAR un număr 0–100 (fără audio). */
object Meter {
    @SuppressLint("MissingPermission")
    fun noise(ctx: Context, secs: Int): Int? {
        if (ctx.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return null
        val min = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return null
        val r = try { AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 2) } catch (_: Exception) { return null }
        if (r.state != AudioRecord.STATE_INITIALIZED) { r.release(); return null }
        val buf = ShortArray(800)
        var sum = 0.0
        var n = 0L
        try {
            r.startRecording()
            val end = System.currentTimeMillis() + secs * 1000L
            while (System.currentTimeMillis() < end) {
                val k = r.read(buf, 0, buf.size)
                for (i in 0 until maxOf(k, 0)) { sum += buf[i].toDouble() * buf[i]; n++ }
            }
        } catch (_: Exception) {
            return null
        } finally {
            try { r.stop(); r.release() } catch (_: Exception) {}
        }
        if (n == 0L) return null
        return level(Math.sqrt(sum / n) / 32768.0)
    }

    /** −60 dBFS → 0, 0 dBFS → 100. */
    fun level(rms: Double): Int = (((20 * Math.log10(maxOf(rms, 1e-6))) + 60) / 60 * 100).toInt().coerceIn(0, 100)

    fun motion(ctx: Context, secs: Int): Int? {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return null
        val s = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return null
        var sum = 0.0
        var n = 0
        var mean = 9.81
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val m = Math.sqrt((e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]).toDouble())
                mean += (m - mean) * 0.05
                val d = m - mean
                synchronized(this) { sum += d * d; n++ }
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, s, SensorManager.SENSOR_DELAY_GAME)
        try { Thread.sleep(secs * 1000L) } catch (_: Exception) {}
        sm.unregisterListener(l)
        val v = synchronized(l) { if (n == 0) return null else Math.sqrt(sum / n) }
        return motionLevel(v)
    }

    /** Abatere de 0,05 m/s² ≈ 0, 5 m/s² (scuturat tare) ≈ 100. */
    fun motionLevel(sd: Double): Int = ((Math.log10(maxOf(sd, 0.05) / 0.05) / 2.0) * 100).toInt().coerceIn(0, 100)
}
