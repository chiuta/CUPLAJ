package ro.trom.cuplaj

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/*
 * CUPLAJ‑AIR: trei protocole proprii pentru legătură fără rețea, folosind alte funcții ale telefonului.
 *
 *   LUMEN  — lumină: lanterna (sau ecranul) → camera video. Cod Manchester pe sloturi de 100 ms, sincronizare Barker‑13,
 *            decodor care caută faza și deriva ceasului pe mostre cu marcă de timp.
 *   SONUS  — sunet: difuzor → microfon. 8‑FSK în bandă aproape ultrasonică (17–20 kHz) sau audibilă, simbol 50 ms (3 biți),
 *            preambul „zig‑zag” propriu, detecție Goertzel pe fereastră glisantă.
 *   TACTUS — vibrații: motorul de vibrații → accelerometrul altui telefon pus pe aceeași masă/scenă sau ținut în același corp.
 *            Cod prin durata rafalelor (scurt = 0, lung = 1) cu pauză fixă, auto‑sincronizat din preambul.
 *
 * Toate trei duc același pachet mic, autentificat cu HMAC (cheia de rețea) și protejat la reluare.
 */

/** Un pachet Air: tip (1 = cue, 2 = invitație), număr de secvență, date. */
class AirPacket(val type: Int, val seq: Int, val data: ByteArray)

object Air {
    const val T_CUE = 1
    const val T_INVITE = 2
    private val OPEN_KEY = "CUPLAJ-AIR-OPEN".toByteArray()

    private fun mac(key: ByteArray?, msg: ByteArray, n: Int): ByteArray {
        val m = Mac.getInstance("HmacSHA256")
        m.init(SecretKeySpec(key ?: OPEN_KEY, "HmacSHA256"))
        return m.doFinal(msg).copyOf(n)
    }

    /** Octeți: [ver<<4|tip][seq][len][date…][mac…]. Fără cheie de rețea, MAC‑ul e doar sumă de control (oricine poate falsifica). */
    fun pack(p: AirPacket, key: ByteArray?, macLen: Int = 4): ByteArray {
        require(p.data.size <= 90 && p.type in 1..15)
        val head = byteArrayOf(((1 shl 4) or p.type).toByte(), p.seq.toByte(), p.data.size.toByte())
        val body = head + p.data
        return body + mac(key, body, macLen)
    }

    /** Verifică și deschide; null dacă lungimea sau MAC‑ul nu se potrivesc. */
    fun unpack(b: ByteArray, key: ByteArray?, macLen: Int = 4): AirPacket? {
        if (b.size < 3 + macLen) return null
        if ((b[0].toInt() shr 4) and 0xF != 1) return null
        val len = b[2].toInt() and 0xFF
        if (len > 90 || b.size != 3 + len + macLen) return null
        val body = b.copyOfRange(0, 3 + len)
        val tag = b.copyOfRange(3 + len, b.size)
        if (!MessageDigest.isEqual(tag, mac(key, body, macLen))) return null
        return AirPacket(b[0].toInt() and 0xF, b[1].toInt() and 0xFF, b.copyOfRange(3, 3 + len))
    }

    fun bits(b: ByteArray): IntArray = IntArray(b.size * 8) { (b[it / 8].toInt() shr (7 - it % 8)) and 1 }

    fun bytes(bits: IntArray, from: Int, n: Int): ByteArray {
        val o = ByteArray(n)
        for (i in 0 until n * 8) if (bits[from + i] == 1) o[i / 8] = (o[i / 8].toInt() or (0x80 shr (i % 8))).toByte()
        return o
    }

    /** Cue: [index din CUE_KINDS][text UTF‑8]. */
    fun cue(kind: Int, text: String): ByteArray = byteArrayOf(kind.toByte()) + text.toByteArray(Charsets.UTF_8).take(60).toByteArray()
}

/** Anti‑reluare: refuză un (tip, secvență) văzut în ultimele 10 minute. */
class AirGuard {
    private val seen = LinkedHashMap<Int, Long>()

    @Synchronized
    fun accept(p: AirPacket, now: Long = System.currentTimeMillis()): Boolean {
        val it = seen.entries.iterator()
        while (it.hasNext()) if (now - it.next().value > 600_000) it.remove()
        val k = p.type * 256 + p.seq
        if (seen.containsKey(k)) return false
        seen[k] = now
        return true
    }
}

// ====================================================================== LUMEN (lumină)

/** Mostră de luminozitate cu marcă de timp (ms). */
class LSample(val t: Double, val v: Double)

object Lumen {
    const val SLOT_MS = 100.0
    private val BARKER = intArrayOf(1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1)

    /** Niveluri de sloturi (true = aprins): 2 sloturi stinse, Barker‑13, biți Manchester (1 = aprins‑stins), 2 stinse. */
    fun encode(packet: ByteArray): BooleanArray {
        val out = ArrayList<Boolean>()
        repeat(2) { out.add(false) }
        for (b in BARKER) out.add(b == 1)
        for (bit in Air.bits(packet)) {
            out.add(bit == 1)
            out.add(bit == 0)
        }
        repeat(2) { out.add(false) }
        return out.toBooleanArray()
    }

    private fun slotMean(s: List<LSample>, lo: Int, hi: Int, a: Double, b: Double): Double {
        // căutare binară a primei mostre >= a; media pe fereastra centrală (60 %) a slotului
        var l = lo
        var h = hi
        while (l < h) { val m = (l + h) / 2; if (s[m].t < a) l = m + 1 else h = m }
        var sum = 0.0
        var n = 0
        var i = l
        while (i < s.size && s[i].t < b) { sum += s[i].v; n++; i++ }
        return if (n == 0) Double.NaN else sum / n
    }

    /** Caută un pachet în mostre; încearcă 31 de derive de ceas (±6 %) și 10 faze. `macLen`/`maxLen` stabilesc forma pachetului. */
    fun decode(s: List<LSample>, key: ByteArray?, macLen: Int = 2, maxLen: Int = 12, slotMs: Double = SLOT_MS): AirPacket? {
        if (s.size < 40) return null
        val t0 = s.first().t
        val t1 = s.last().t
        // derivă de ceas: pas de 0,4 % (peste ~100 de sloturi eroarea cumulată rămâne sub un slot), cei mai apropiați de 1 întâi
        for (skew in (-15..15).sortedBy { Math.abs(it) }.map { 1.0 + it * 0.004 }) {
            val T = slotMs * skew
            for (ph in 0 until 10) {
                val start = t0 + ph * T / 10
                val nSlots = ((t1 - start) / T).toInt()
                if (nSlots < BARKER.size + 2 * (3 + macLen) * 8) continue
                val m = DoubleArray(nSlots) { k ->
                    val a = start + k * T + 0.2 * T
                    slotMean(s, 0, s.size, a, a + 0.6 * T)
                }
                val r = scan(m, key, macLen, maxLen)
                if (r != null) return r
            }
        }
        return null
    }

    private fun scan(m: DoubleArray, key: ByteArray?, macLen: Int, maxLen: Int): AirPacket? {
        val nb = BARKER.size
        for (k in 0..(m.size - nb - 16)) {
            // media sloturilor „aprinse” și „stinse” din ipoteza Barker
            var on = 0.0; var off = 0.0; var non = 0; var noff = 0; var bad = false
            for (i in 0 until nb) {
                val v = m[k + i]
                if (v.isNaN()) { bad = true; break }
                if (BARKER[i] == 1) { on += v; non++ } else { off += v; noff++ }
            }
            if (bad) continue
            on /= non; off /= noff
            if (on - off < 6.0) continue // contrast minim (pe scara 0–255)
            val thr = (on + off) / 2
            var mism = 0
            for (i in 0 until nb) if ((m[k + i] > thr) != (BARKER[i] == 1)) mism++
            if (mism > 1) continue
            // biți Manchester diferențiali (nu depind de prag)
            var p = k + nb
            fun readByte(): Int? {
                var v = 0
                for (j in 0 until 8) {
                    if (p + 1 >= m.size) return null
                    val a = m[p]; val b = m[p + 1]
                    if (a.isNaN() || b.isNaN()) return null
                    if (Math.abs(a - b) < (on - off) * 0.3) return null
                    v = (v shl 1) or (if (a > b) 1 else 0)
                    p += 2
                }
                return v
            }
            val h0 = readByte() ?: continue
            val h1 = readByte() ?: continue
            val h2 = readByte() ?: continue
            if (h2 > maxLen) continue
            val buf = ByteArray(3 + h2 + macLen)
            buf[0] = h0.toByte(); buf[1] = h1.toByte(); buf[2] = h2.toByte()
            var ok = true
            for (i in 3 until buf.size) { val v = readByte(); if (v == null) { ok = false; break }; buf[i] = v.toByte() }
            if (!ok) continue
            val pk = Air.unpack(buf, key, macLen)
            if (pk != null) return pk
        }
        return null
    }
}

// ====================================================================== SONUS (sunet)

object Sonus {
    const val SYM_MS = 50.0
    private val SYNC = intArrayOf(0, 7, 1, 6, 2, 5, 3, 4, 0, 7, 1, 6, 2, 5, 3, 4) // zig‑zag, de două ori

    /** Frecvența tonului i (0–7): aproape ultrasonică (17,0–19,8 kHz) sau audibilă (1,8–3,9 kHz). */
    fun freq(i: Int, ultra: Boolean): Double = if (ultra) 17000.0 + 400.0 * i else 1800.0 + 300.0 * i

    fun symbols(packet: ByteArray): IntArray {
        val bits = Air.bits(packet)
        val n = (bits.size + 2) / 3
        val d = IntArray(n) { s ->
            var v = 0
            for (j in 0 until 3) { val ix = s * 3 + j; v = (v shl 1) or (if (ix < bits.size) bits[ix] else 0) }
            v
        }
        return SYNC + d
    }

    /** PCM 16 biți mono; fiecare simbol are rampă cosinus de 5 ms ca să nu pocnească difuzorul. */
    fun encode(packet: ByteArray, fs: Int, ultra: Boolean, amp: Double = 0.5): ShortArray {
        val sy = symbols(packet)
        val L = (fs * SYM_MS / 1000).toInt()
        val ramp = (fs * 0.005).toInt()
        val lead = L // liniște la început
        val out = ShortArray(lead + sy.size * L + L)
        for ((si, s) in sy.withIndex()) {
            val f = freq(s, ultra)
            for (n in 0 until L) {
                var a = amp
                if (n < ramp) a *= 0.5 * (1 - Math.cos(Math.PI * n / ramp))
                if (n >= L - ramp) a *= 0.5 * (1 - Math.cos(Math.PI * (L - 1 - n) / ramp))
                out[lead + si * L + n] = (a * 32767 * Math.sin(2 * Math.PI * f * n / fs)).toInt().toShort()
            }
        }
        return out
    }

    private fun goertzel(x: FloatArray, from: Int, n: Int, f: Double, fs: Int): Double {
        val w = 2 * Math.PI * f / fs
        val c = 2 * Math.cos(w)
        var s1 = 0.0; var s2 = 0.0
        for (i in 0 until n) { val s0 = x[from + i] + c * s1 - s2; s2 = s1; s1 = s0 }
        return s1 * s1 + s2 * s2 - c * s1 * s2
    }

    /** Caută preambulul pe o fereastră glisantă (pas = 1/8 simbol) și citește simbolurile care urmează. */
    fun decode(pcm: ShortArray, fs: Int, ultra: Boolean, key: ByteArray?, macLen: Int = 4, maxLen: Int = 90): AirPacket? {
        val L = (fs * SYM_MS / 1000).toInt()
        val hop = L / 8
        if (pcm.size < L * (SYNC.size + 8)) return null
        val x = FloatArray(pcm.size) { pcm[it] / 32768f }
        val steps = (pcm.size - L) / hop
        val e = Array(steps) { st -> DoubleArray(8) { goertzel(x, st * hop, L, freq(it, ultra), fs) } }
        // scor de preambul pentru fiecare pas de pornire
        val need = SYNC.size
        var best = -1.0
        val cands = ArrayList<Pair<Int, Double>>()
        for (st in 0 until steps - need * 8) {
            var sc = 0.0
            var ok = true
            for (j in 0 until need) {
                val row = e[st + j * 8]
                val tot = row.sum() + 1e-12
                val r = row[SYNC[j]] / tot
                if (r < 0.35) { ok = false; break }
                sc += r
            }
            if (ok) { sc /= need; cands.add(Pair(st, sc)); if (sc > best) best = sc }
        }
        // încearcă candidații, cei mai buni întâi
        for ((st, _) in cands.sortedByDescending { it.second }.take(12)) {
            val r = readSymbols(e, st + need * 8, macLen, maxLen, key)
            if (r != null) return r
        }
        return null
    }

    private fun readSymbols(e: Array<DoubleArray>, from: Int, macLen: Int, maxLen: Int, key: ByteArray?): AirPacket? {
        fun sym(j: Int): Int? {
            val st = from + j * 8
            if (st >= e.size) return null
            val row = e[st]
            var b = 0
            for (i in 1 until 8) if (row[i] > row[b]) b = i
            return b
        }
        val bits = ArrayList<Int>()
        var j = 0
        fun fill(n: Int): Boolean {
            while (bits.size < n * 8) {
                val s = sym(j++) ?: return false
                bits.add((s shr 2) and 1); bits.add((s shr 1) and 1); bits.add(s and 1)
            }
            return true
        }
        if (!fill(3)) return null
        val head = Air.bytes(bits.toIntArray(), 0, 3)
        val len = head[2].toInt() and 0xFF
        if (len > maxLen) return null
        val total = 3 + len + macLen
        if (!fill(total)) return null
        return Air.unpack(Air.bytes(bits.toIntArray(), 0, total), key, macLen)
    }
}

// ====================================================================== TACTUS (vibrații)

/** Mostră de accelerometru (ms, m/s²). */
class ASample(val t: Double, val x: Double, val y: Double, val z: Double)

object Tactus {
    const val SHORT_MS = 100
    const val LONG_MS = 240
    const val GAP_MS = 100

    /** Durate (ms) alternând „vibrează”, „pauză”, … (începe cu vibrație). Preambul: lung, lung, lung, scurt. */
    fun encode(packet: ByteArray): IntArray {
        val out = ArrayList<Int>()
        fun burst(long: Boolean, last: Boolean) {
            out.add(if (long) LONG_MS else SHORT_MS)
            if (!last) out.add(GAP_MS)
        }
        burst(true, false); burst(true, false); burst(true, false); burst(false, false)
        val bits = Air.bits(packet)
        for ((i, b) in bits.withIndex()) burst(b == 1, i == bits.size - 1)
        return out.toIntArray()
    }

    /** Rafale detectate (început, durată) după filtrare și prag cu histerezis. */
    private fun bursts(s: List<ASample>): List<Pair<Double, Double>> {
        if (s.size < 30) return emptyList()
        // magnitudine fără componenta lentă (gravitația): scădem media mobilă
        val mag = DoubleArray(s.size) { Math.sqrt(s[it].x * s[it].x + s[it].y * s[it].y + s[it].z * s[it].z) }
        val hp = DoubleArray(s.size)
        var ema = mag[0]
        for (i in mag.indices) { ema += (mag[i] - ema) * 0.05; hp[i] = Math.abs(mag[i] - ema) }
        // înfășurătoare: maxim glisant pe ~40 ms (rezistă la aliasing: energia rămâne, faza nu contează)
        val dt = (s.last().t - s.first().t) / (s.size - 1)
        val w = maxOf(2, (40.0 / dt).toInt())
        val env = DoubleArray(s.size)
        for (i in env.indices) { var mx = 0.0; for (j in maxOf(0, i - w + 1)..i) if (hp[j] > mx) mx = hp[j]; env[i] = mx }
        val sorted = env.sorted()
        val floor = sorted[(sorted.size * 0.4).toInt()]
        val peak = sorted[(sorted.size * 0.98).toInt()]
        if (peak - floor < 0.15) return emptyList()
        val on = floor + 0.35 * (peak - floor)
        val off = floor + 0.2 * (peak - floor)
        val out = ArrayList<Pair<Double, Double>>()
        var inB = false
        var t0 = 0.0
        for (i in env.indices) {
            if (!inB && env[i] > on) { inB = true; t0 = s[i].t }
            else if (inB && env[i] < off) { inB = false; out.add(Pair(t0, s[i].t - t0)) }
        }
        return out
    }

    fun decode(s: List<ASample>, key: ByteArray?, macLen: Int = 2, maxLen: Int = 6): AirPacket? {
        val b = bursts(s)
        if (b.size < 4 + 3 * 8 + macLen * 8) return null
        for (k in 0..(b.size - 4)) {
            val l1 = b[k].second; val l2 = b[k + 1].second; val l3 = b[k + 2].second; val sh = b[k + 3].second
            val L = (l1 + l2 + l3) / 3
            if (L < sh * 1.5 || Math.abs(l1 - l2) > L * 0.35 || Math.abs(l2 - l3) > L * 0.35) continue
            // pauzele dintre rafale trebuie să fie cele de preambul (≈ GAP)
            val gaps = (0 until 3).map { b[k + it + 1].first - (b[k + it].first + b[k + it].second) }
            if (gaps.any { it < GAP_MS * 0.4 || it > GAP_MS * 2.2 }) continue
            val bound = (L + sh) / 2
            var idx = k + 4
            fun readByte(): Int? {
                if (idx + 8 > b.size) return null
                var v = 0
                for (j in 0 until 8) { v = (v shl 1) or (if (b[idx].second > bound) 1 else 0); idx++ }
                return v
            }
            val h0 = readByte() ?: continue
            val h1 = readByte() ?: continue
            val h2 = readByte() ?: continue
            if (h2 > maxLen) continue
            val buf = ByteArray(3 + h2 + macLen)
            buf[0] = h0.toByte(); buf[1] = h1.toByte(); buf[2] = h2.toByte()
            var ok = true
            for (i in 3 until buf.size) { val v = readByte(); if (v == null) { ok = false; break }; buf[i] = v.toByte() }
            if (!ok) continue
            val pk = Air.unpack(buf, key, macLen)
            if (pk != null) return pk
        }
        return null
    }
}
