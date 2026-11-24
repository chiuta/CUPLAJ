package ro.trom.cuplaj

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

const val RATE = 8000   // calitate de telefon: 8 kHz, 16 biți, mono
const val FR = 160      // 20 ms

class ToneSpec(val f1: Int, val f2: Int, val onMs: Int, val offMs: Int, val amp: Double = 0.22)

/** Tonuri de linie (425 Hz, ca în rețeaua telefonică românească). */
object Tones {
    val DIAL = ToneSpec(425, 0, 1000, 0)
    val RINGBACK = ToneSpec(425, 0, 1000, 4000)
    val BUSY = ToneSpec(425, 0, 500, 500)
    val UNKNOWN = ToneSpec(425, 0, 200, 200)
}

/** Efecte scurte: clic de receptor, impulsuri de disc, DTMF, bipuri. */
object Fx {
    private val rnd = java.util.Random(42)

    fun click(amp: Double = 0.5): ShortArray {
        val n = 90
        return ShortArray(n) { i -> ((rnd.nextDouble() * 2 - 1) * amp * 32767 * exp(-i / 14.0)).toInt().toShort() }
    }

    /** Disc rotativ: n impulsuri, 10 pe secundă. */
    fun pulses(n: Int): ShortArray {
        val per = RATE / 10
        val out = ShortArray(per * n + per)
        val c = click(0.7)
        for (k in 0 until n) System.arraycopy(c, 0, out, k * per, c.size)
        return out
    }

    fun beep(f: Int, ms: Int, amp: Double = 0.3): ShortArray {
        val n = RATE * ms / 1000
        return ShortArray(n) { i ->
            val env = min(1.0, min(i, n - i) / 40.0)
            (sin(2 * PI * f * i / RATE) * amp * 32767 * env).toInt().toShort()
        }
    }

    private fun env(i: Int, n: Int, edge: Int): Double = min(1.0, min(i, n - i) / edge.toDouble())

    /** Sunete pentru indicii: clopot, sirenă, bătăi, static, bip. */
    fun preset(name: String): ShortArray {
        return when (name) {
            "bell" -> {
                val n = RATE * 18 / 10
                ShortArray(n) { i ->
                    val d = exp(-i / (RATE * 0.45))
                    ((sin(2 * PI * 880 * i / RATE) * 0.6 + sin(2 * PI * 1760 * i / RATE) * 0.25 + sin(2 * PI * 2349 * i / RATE) * 0.1) * d * 0.7 * 32767).toInt().toShort()
                }
            }
            "siren" -> {
                val n = RATE * 3
                ShortArray(n) { i ->
                    val f = 800 + 400 * sin(2 * PI * 0.8 * i / RATE)
                    (sin(2 * PI * f * i / RATE) * 0.35 * 32767 * env(i, n, 400)).toInt().toShort()
                }
            }
            "knock" -> {
                val n = RATE * 17 / 10
                val starts = intArrayOf(0, RATE * 4 / 10, RATE * 8 / 10, RATE * 13 / 10)
                ShortArray(n) { i ->
                    var v = 0.0
                    for (s in starts) {
                        val k = i - s
                        if (k in 0 until RATE / 8) v += sin(2 * PI * 95 * k / RATE) * exp(-k / (RATE * 0.02)) + (rnd.nextDouble() - 0.5) * 0.3 * exp(-k / (RATE * 0.005))
                    }
                    (v * 0.8 * 32767).toInt().coerceIn(-32767, 32767).toShort()
                }
            }
            "static" -> {
                val n = RATE * 2
                var lp = 0.0
                ShortArray(n) { i ->
                    lp += 0.45 * ((rnd.nextDouble() * 2 - 1) - lp)
                    (lp * 0.5 * 32767 * env(i, n, 800)).toInt().toShort()
                }
            }
            else -> beep(1000, 300)
        }
    }

    fun dtmf(d: Char): ShortArray {
        val idx = "123456789*0#".indexOf(d)
        if (idx < 0) return ShortArray(0)
        val lo = intArrayOf(697, 770, 852, 941)[idx / 3]
        val hi = intArrayOf(1209, 1336, 1477)[idx % 3]
        val n = RATE * 110 / 1000
        return ShortArray(n) { i ->
            val env = min(1.0, min(i, n - i) / 30.0)
            ((sin(2 * PI * lo * i / RATE) + sin(2 * PI * hi * i / RATE)) * 0.18 * 32767 * env).toInt().toShort()
        }
    }
}

/** Mixer de ieșire: voce de la oricâte surse + ton de linie + efecte, într-un singur AudioTrack. */
class Playout {
    private class SrcQ {
        val q = java.util.ArrayDeque<ShortArray>()
        var primed = false
    }

    private class Effect(val d: ShortArray) {
        var pos = 0
    }

    private val srcs = ConcurrentHashMap<Int, SrcQ>()
    private val effects = ConcurrentLinkedQueue<Effect>()

    @Volatile
    var tone: ToneSpec? = null

    @Volatile
    private var run = false
    private var lastTone: ToneSpec? = null
    private var tpos = 0L
    private var ph1 = 0.0
    private var ph2 = 0.0

    fun start() {
        if (run) return
        run = true
        Thread { loop() }.apply {
            isDaemon = true
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        run = false
    }

    fun clearVoice() {
        srcs.clear()
    }

    fun clearEffects() {
        effects.clear()
    }

    fun effect(d: ShortArray) {
        if (d.isNotEmpty()) effects.add(Effect(d))
    }

    fun feed(src: Int, data: ByteArray, off: Int) {
        val n = (data.size - off) / 2
        if (n <= 0) return
        val s = ShortArray(n)
        ByteBuffer.wrap(data, off, n * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(s)
        val sq = srcs.getOrPut(src) { SrcQ() }
        synchronized(sq) {
            sq.q.addLast(s)
            while (sq.q.size > 14) sq.q.pollFirst()
        }
    }

    private fun loop() {
        val minBuf = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track: AudioTrack
        try {
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBuf, FR * 2 * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
        } catch (_: Exception) {
            run = false
            return
        }
        val mix = IntArray(FR)
        val out = ShortArray(FR)
        try {
            while (run) {
                java.util.Arrays.fill(mix, 0)
                var any = false
                for (sq in srcs.values) {
                    var chunk: ShortArray? = null
                    synchronized(sq) {
                        if (!sq.primed && sq.q.size >= 3) sq.primed = true
                        if (sq.primed) {
                            chunk = sq.q.pollFirst()
                            if (chunk == null) sq.primed = false
                        }
                    }
                    val ch = chunk
                    if (ch != null) {
                        any = true
                        for (i in 0 until min(FR, ch.size)) mix[i] += ch[i].toInt()
                    }
                }
                val tn = tone
                if (tn == null) {
                    lastTone = null
                } else {
                    if (tn !== lastTone) {
                        lastTone = tn
                        tpos = 0
                        ph1 = 0.0
                        ph2 = 0.0
                    }
                    any = true
                    val spms = RATE / 1000
                    val period = (tn.onMs + tn.offMs) * spms
                    for (i in 0 until FR) {
                        val inPeriod = if (period > 0) (tpos % period).toInt() else 0
                        val on = tn.offMs == 0 || inPeriod < tn.onMs * spms
                        tpos++
                        if (on) {
                            var v = sin(ph1)
                            ph1 += 2 * PI * tn.f1 / RATE
                            if (ph1 > 2 * PI) ph1 -= 2 * PI
                            if (tn.f2 > 0) {
                                v += sin(ph2)
                                ph2 += 2 * PI * tn.f2 / RATE
                                if (ph2 > 2 * PI) ph2 -= 2 * PI
                            }
                            mix[i] += (v * tn.amp * 32767).toInt()
                        }
                    }
                }
                val iter = effects.iterator()
                while (iter.hasNext()) {
                    val e = iter.next()
                    any = true
                    for (i in 0 until FR) {
                        if (e.pos < e.d.size) {
                            mix[i] += e.d[e.pos].toInt()
                            e.pos++
                        }
                    }
                    if (e.pos >= e.d.size) iter.remove()
                }
                if (any) {
                    for (i in 0 until FR) out[i] = max(-32768, min(32767, mix[i])).toShort()
                    track.write(out, 0, FR)
                } else {
                    Thread.sleep(15)
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                track.stop()
                track.release()
            } catch (_: Exception) {
            }
        }
    }
}

/** Microfon: cadre de 20 ms, cu poartă de zgomot (nu trimite liniștea). */
class Mic(private val onFrame: (ByteArray) -> Unit) {
    @Volatile
    private var run = false

    @Volatile
    private var tok = 0

    val running: Boolean get() = run

    @Suppress("MissingPermission")
    fun start() {
        if (run) return
        run = true
        tok++
        val my = tok
        Thread {
            var rec: AudioRecord? = null
            try {
                val minBuf = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                val r = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(minBuf, FR * 2 * 8)
                )
                rec = r
                if (r.state != AudioRecord.STATE_INITIALIZED) return@Thread
                r.startRecording()
                val buf = ShortArray(FR)
                var hang = 0
                while (run && tok == my) {
                    var got = 0
                    while (got < FR && run && tok == my) {
                        val n = r.read(buf, got, FR - got)
                        if (n <= 0) return@Thread
                        got += n
                    }
                    if (got < FR) break
                    var e = 0.0
                    for (v in buf) e += v.toDouble() * v
                    val rms = sqrt(e / FR)
                    if (rms > 150) hang = 20 else if (hang > 0) hang--
                    if (hang > 0) {
                        val b = ByteArray(FR * 2)
                        ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(buf)
                        onFrame(b)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    rec?.stop()
                } catch (_: Exception) {
                }
                try {
                    rec?.release()
                } catch (_: Exception) {
                }
                if (tok == my) run = false
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        run = false
        tok++
    }
}

/** Soneria: buclă pe un AudioTrack separat (flux de sonerie, se aude pe difuzor). */
class Ringer {
    private var track: AudioTrack? = null

    fun start(era: Int, pat: Int) {
        stop()
        val sr = 16000
        val data = renderRing(era, pat, sr)
        try {
            val t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(data.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            t.write(data, 0, data.size)
            t.setLoopPoints(0, data.size, -1)
            t.play()
            track = t
        } catch (_: Exception) {
        }
    }

    fun stop() {
        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) {
        }
        track = null
    }

    /**
     * Ciclu de 5 s. Cadența distinge abonații de pe o linie cuplată:
     * pat 0 = un apel lung, pat 1 = două apeluri scurte.
     * Timbrul depinde de epocă: clopoțel mecanic (anii '80), trilu electronic (anii '90), arpegiu (anii 2000).
     */
    private fun renderRing(era: Int, pat: Int, sr: Int): ShortArray {
        val total = 5 * sr
        val out = ShortArray(total)
        val segs = if (pat == 1) listOf(0 to 400, 600 to 1000) else listOf(0 to 1300)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 659.25)
        for ((a, b) in segs) {
            val s0 = a * sr / 1000
            val s1 = min(total, b * sr / 1000)
            for (i in s0 until s1) {
                val t = i.toDouble() / sr
                val local = (i - s0).toDouble() / sr
                val env = min(1.0, min(local / 0.01, (s1 - i) / (0.02 * sr)))
                val v = when {
                    era < 1990 -> (sin(2 * PI * 1400 * t) * 0.5 + sin(2 * PI * 2000 * t) * 0.3) * (0.55 + 0.45 * sin(2 * PI * 22 * t))
                    era < 2000 -> sin(2 * PI * (if ((local * 20).toInt() % 2 == 0) 880.0 else 1175.0) * t) * 0.6
                    else -> sin(2 * PI * notes[(local / 0.12).toInt() % 4] * t) * 0.6
                }
                out[i] = (v * env * 0.95 * 32767).toInt().toShort()
            }
        }
        return out
    }
}
