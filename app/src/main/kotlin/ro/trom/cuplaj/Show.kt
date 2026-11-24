package ro.trom.cuplaj

import java.io.File
import java.io.RandomAccessFile
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.security.MessageDigest
import java.util.Base64

// ====================================================================== prioritate audio (ducking)

/** Câștig pentru restul sunetelor cât vorbește Regia/PA: coboară repede, revine încet. */
class Ducker(private val level: Double = 0.3, private val holdFrames: Int = 8) {
    var gain = 1.0
        private set
    private var hold = 0

    /** Un pas pe cadru de 20 ms; întoarce câștigul de aplicat peste restul. */
    fun step(priorityActive: Boolean): Double {
        if (priorityActive) hold = holdFrames else if (hold > 0) hold--
        val target = if (hold > 0) level else 1.0
        gain += (target - gain) * (if (target < gain) 0.5 else 0.08)
        return gain
    }
}

// ====================================================================== OSC (UDP): ieșire către pupitre, intrare pentru comenzi

object Osc {
    private fun pad(b: java.io.ByteArrayOutputStream) { while (b.size() % 4 != 0) b.write(0) }

    private fun str(b: java.io.ByteArrayOutputStream, s: String) { b.write(s.toByteArray(Charsets.UTF_8)); b.write(0); pad(b) }

    /** Mesaj OSC 1.0 cu argumente text, întregi (Int) sau reale (Float). */
    fun encode(addr: String, vararg args: Any): ByteArray {
        val b = java.io.ByteArrayOutputStream()
        str(b, addr)
        val tags = StringBuilder(",")
        for (a in args) tags.append(when (a) { is Int -> 'i'; is Float -> 'f'; else -> 's' })
        str(b, tags.toString())
        for (a in args) when (a) {
            is Int -> b.write(java.nio.ByteBuffer.allocate(4).putInt(a).array())
            is Float -> b.write(java.nio.ByteBuffer.allocate(4).putFloat(a).array())
            else -> str(b, a.toString())
        }
        return b.toByteArray()
    }

    class Msg(val addr: String, val args: List<Any>)

    private fun readStr(d: ByteArray, p: IntArray): String? {
        var e = p[0]
        while (e < d.size && d[e] != 0.toByte()) e++
        if (e >= d.size || e - p[0] > 512) return null
        val s = String(d, p[0], e - p[0], Charsets.UTF_8)
        p[0] = (e + 4) and 3.inv()
        return if (p[0] > d.size) null else s
    }

    /** Decodează un mesaj (nu pachete „bundle”); null dacă e malformat. Nu aruncă niciodată. */
    fun decode(d: ByteArray, len: Int = d.size): Msg? {
        try {
            if (len < 8 || len > 2048) return null
            val x = d.copyOf(len)
            val p = intArrayOf(0)
            val addr = readStr(x, p) ?: return null
            if (!addr.startsWith("/")) return null
            val tags = readStr(x, p) ?: return null
            if (!tags.startsWith(",") || tags.length > 17) return null
            val out = ArrayList<Any>()
            for (t in tags.substring(1)) when (t) {
                'i' -> { if (p[0] + 4 > x.size) return null; out.add(java.nio.ByteBuffer.wrap(x, p[0], 4).int); p[0] += 4 }
                'f' -> { if (p[0] + 4 > x.size) return null; out.add(java.nio.ByteBuffer.wrap(x, p[0], 4).float); p[0] += 4 }
                's' -> out.add(readStr(x, p) ?: return null)
                else -> return null
            }
            return Msg(addr, out)
        } catch (_: Exception) {
            return null
        }
    }

    /** Comanda primită prin OSC. */
    class Cmd(val kind: String, val to: String = "all", val cue: String = "", val text: String = "")

    /**
     * Adresele trebuie să înceapă cu `/cuplaj/<PIN>/`: OSC nu are autentificare, iar PIN‑ul hubului împiedică
     * orice altă persoană din rețea să dea panică sau să ruleze cue‑uri.
     * `/go`, `/panic`, `/stop`, `/cue` (to, kind, text).
     */
    fun command(m: Msg, pin: String): Cmd? {
        val pre = "/cuplaj/"
        if (!m.addr.startsWith(pre)) return null
        val rest = m.addr.substring(pre.length)
        val i = rest.indexOf('/')
        if (i < 0 || pin.isEmpty()) return null
        if (!MessageDigest.isEqual(rest.substring(0, i).toByteArray(), pin.toByteArray())) return null
        return when (rest.substring(i + 1)) {
            "go" -> Cmd("go")
            "panic" -> Cmd("panic")
            "stop" -> Cmd("stop")
            "cue" -> {
                val a = m.args.map { it.toString() }
                if (a.size < 2 || a[1] !in CUE_KIND_NAMES) null else Cmd("cue", a[0].take(40), a[1], a.getOrElse(2) { "" }.take(200))
            }
            else -> null
        }
    }

    val CUE_KIND_NAMES = setOf("text", "vibrate", "ring", "flash", "speaker", "contact", "era", "say", "sound", "blackout", "fakecall", "realcall", "panic", "color", "poll", "pollend", "image", "video", "clip", "caption", "dmx", "midi", "morse", "house")
}

/** Ieșire OSC: fiecare cue trimis de Regie e repetat ca `/cuplaj/cue to kind text`. */
class OscOut(private val host: String, private val port: Int) {
    private val sock = DatagramSocket()
    private val addr: InetAddress? = try { InetAddress.getByName(host) } catch (_: Exception) { null }

    fun send(msg: ByteArray) {
        val a = addr ?: return
        try { sock.send(DatagramPacket(msg, msg.size, a, port)) } catch (_: Exception) {}
    }

    fun close() = sock.close()
}

/** Intrare OSC: comenzi de la un pupitru (QLab, Companion…). Maxim 20 de mesaje pe secundă. */
class OscIn(private val port: Int, private val pin: () -> String, private val onCmd: (Osc.Cmd) -> Unit) {
    @Volatile
    private var sock: DatagramSocket? = null
    private var winStart = 0L
    private var winN = 0

    fun start(): Boolean {
        return try {
            val s = DatagramSocket(port)
            sock = s
            Thread {
                val buf = ByteArray(2048)
                while (!s.isClosed) {
                    try {
                        val p = DatagramPacket(buf, buf.size)
                        s.receive(p)
                        val now = System.currentTimeMillis()
                        if (now - winStart > 1000) { winStart = now; winN = 0 }
                        if (++winN > 20) continue
                        val m = Osc.decode(p.data, p.length) ?: continue
                        val c = Osc.command(m, pin()) ?: continue
                        onCmd(c)
                    } catch (_: Exception) {
                    }
                }
            }.apply { isDaemon = true; start() }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stop() { try { sock?.close() } catch (_: Exception) {} }
}

// ====================================================================== fișiere media (imagine, video, clip audio) prin hub

object Media {
    const val MAX_BYTES = 8L * 1024 * 1024
    const val CHUNK = 12000          // octeți pe bucată (≈ 16 KB în base64, sub limita de 32 KB a cadrului)
    const val MAX_B64 = 17000
    val KINDS = mapOf("image" to ".img", "video" to ".vid", "clip" to ".snd")

    fun validId(s: String) = Regex("[a-z0-9]{1,16}").matches(s)

    fun sha256(b: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    /** Bucățile de trimis: (index, total, base64). */
    fun chunks(data: ByteArray): List<Triple<Int, Int, String>> {
        val n = maxOf(1, (data.size + CHUNK - 1) / CHUNK)
        return (0 until n).map { i ->
            Triple(i, n, Base64.getEncoder().encodeToString(data.copyOfRange(i * CHUNK, minOf(data.size, (i + 1) * CHUNK))))
        }
    }
}

/** Primește bucățile în ordine într‑un fișier din `dir`; verifică mărimea și SHA‑256 la sfârșit. Calea nu depinde niciodată de text primit. */
class MediaAsm(private val dir: File) {
    private class Tr(val f: File, val kind: String, val size: Long, val sha: String, val n: Int) {
        var next = 0
        var got = 0L
        val raf = RandomAccessFile(f, "rw")
    }

    private val open = HashMap<String, Tr>()

    @Synchronized
    fun file(id: String, kind: String): File? =
        if (Media.validId(id) && kind in Media.KINDS) File(dir, id + Media.KINDS[kind]).takeIf { it.isFile } else null

    /** Întoarce: "ok" bucată primită, "done" fișier complet și verificat, altfel motivul respingerii. */
    @Synchronized
    fun add(m: org.json.JSONObject): String {
        try {
            val id = m.optString("mid")
            val kind = m.optString("kind")
            if (!Media.validId(id) || kind !in Media.KINDS) return "bad_id"
            val i = m.optInt("i", -1)
            val b64 = m.optString("b64")
            if (b64.length > Media.MAX_B64) return "chunk_big"
            var t = open[id]
            if (t == null) {
                if (i != 0) return "order"
                val size = m.optLong("size", -1)
                val n = m.optInt("n", -1)
                val sha = m.optString("sha")
                if (size <= 0 || size > Media.MAX_BYTES || n < 1 || n > 1000 || !Regex("[0-9a-f]{64}").matches(sha)) return "bad_meta"
                if (open.size >= 3) return "busy"
                dir.mkdirs()
                t = Tr(File(dir, id + Media.KINDS[kind] + ".part"), kind, size, sha, n)
                t.raf.setLength(0)
                open[id] = t
            }
            if (i != t.next) { abort(id); return "order" }
            val bytes = Base64.getDecoder().decode(b64)
            if (t.got + bytes.size > t.size) { abort(id); return "overflow" }
            t.raf.write(bytes)
            t.got += bytes.size
            t.next++
            if (t.next < t.n) return "ok"
            t.raf.close()
            open.remove(id)
            val data = t.f.readBytes()
            if (data.size.toLong() != t.size || Media.sha256(data) != t.sha) { t.f.delete(); return "bad_hash" }
            val dst = File(dir, id + Media.KINDS[kind])
            dst.delete()
            if (!t.f.renameTo(dst)) { t.f.delete(); return "io" }
            return "done"
        } catch (_: Exception) {
            m.optString("mid").takeIf { Media.validId(it) }?.let { abort(it) }
            return "bad_data"
        }
    }

    private fun abort(id: String) {
        val t = open.remove(id) ?: return
        try { t.raf.close() } catch (_: Exception) {}
        t.f.delete()
    }

    /** Șterge fișierele peste `maxBytes` în total, începând cu cele mai vechi. */
    @Synchronized
    fun trim(maxBytes: Long) {
        val fs = (dir.listFiles() ?: return).filter { it.isFile }.sortedBy { it.lastModified() }
        var tot = fs.sumOf { it.length() }
        for (f in fs) { if (tot <= maxBytes) break; tot -= f.length(); f.delete() }
    }
}

// ====================================================================== v1.7: text multilingv și personalizat

/** „en=Hello|ro=Salut|fr=Bonjour” → textul în limba telefonului (apoi en, apoi primul). Text obișnuit rămâne neschimbat. */
fun pickLang(text: String, lang: String): String {
    val parts = text.split("|")
    if (parts.size < 2 || !parts.all { Regex("^[a-z]{2}=.*", RegexOption.DOT_MATCHES_ALL).matches(it) }) return text
    val m = parts.associate { it.substring(0, 2) to it.substring(3) }
    return m[lang] ?: m["en"] ?: parts[0].substring(3)
}

/** {name} și {number} din telefonul care primește; fără alt cod, deci textul nu poate scoate alte date. */
fun expandTokens(text: String, name: String, number: String): String =
    text.replace("{name}", name.filter { it >= ' ' }.take(40)).replace("{number}", number.filter { it.isDigit() || it == '+' }.take(20))

// ====================================================================== efecte de voce (8 kHz, cadre de 20 ms)

class VoiceFx(val kind: String) {
    companion object {
        val KINDS = listOf("robot", "radio", "echo", "deep", "high", "lofi")
        private const val L = 400   // fereastra pitch‑shifterului: 50 ms
    }

    private val dl = FloatArray(4096)
    private var w = 0
    private var tap = 0.0
    private var ph = 0.0
    private var lp1 = 0f; private var lp2 = 0f; private var hp1 = 0f; private var hpPrev = 0f
    private val ech = FloatArray(1440)
    private var ei = 0
    private var hold = 0f; private var hc = 0

    private fun shift(x: Float, r: Double): Float {
        dl[w] = x
        val adv = (1.0 - r) / L
        tap = ((tap + adv) % 1.0 + 1.0) % 1.0
        fun rd(p: Double): Float {
            val d = p * L + 2
            val pos = w - d
            val i0 = Math.floor(pos).toInt()
            val fr = (pos - i0).toFloat()
            val a = dl[(i0 % dl.size + dl.size) % dl.size]
            val b = dl[((i0 + 1) % dl.size + dl.size) % dl.size]
            return a + (b - a) * fr
        }
        val p2 = (tap + 0.5) % 1.0
        val g1 = Math.sin(Math.PI * tap).let { it * it }.toFloat()
        val g2 = 1f - g1
        val y = rd(tap) * g1 + rd(p2) * g2
        w = (w + 1) % dl.size
        return y
    }

    fun process(f: ShortArray): ShortArray {
        val o = ShortArray(f.size)
        for (i in f.indices) {
            var x = f[i] / 32768f
            val y: Float = when (kind) {
                "robot" -> { ph += 2 * Math.PI * 55.0 / 8000; if (ph > 2 * Math.PI) ph -= 2 * Math.PI; x * Math.sin(ph).toFloat() * 1.6f }
                "radio" -> {
                    hp1 += 0.31f * (x - hp1); val h1 = x - hp1          // trece‑sus ≈ 400 Hz, două trepte
                    hpPrev += 0.31f * (h1 - hpPrev); val hpv = h1 - hpPrev
                    lp1 += 0.8f * (hpv - lp1); lp2 += 0.8f * (lp1 - lp2)  // trece‑jos ≈ 2,5 kHz
                    Math.tanh((lp2 * 3f).toDouble()).toFloat() * 0.8f
                }
                "echo" -> { val d = ech[ei]; ech[ei] = x + d * 0.4f; ei = (ei + 1) % ech.size; x + d * 0.5f }
                "deep" -> shift(x, 0.75)
                "high" -> shift(x, 1.5)
                "lofi" -> { if (hc++ % 3 == 0) hold = Math.round(x * 15f) / 15f; hold }
                else -> x
            }
            o[i] = (y.coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
        return o
    }

    /** Cadru PCM16 little‑endian de la microfon. */
    fun processBytes(b: ByteArray): ByteArray {
        val n = b.size / 2
        val s = ShortArray(n) { ((b[2 * it + 1].toInt() shl 8) or (b[2 * it].toInt() and 0xFF)).toShort() }
        val r = process(s)
        val out = ByteArray(b.size)
        for (i in 0 until n) { out[2 * i] = r[i].toByte(); out[2 * i + 1] = (r[i].toInt() shr 8).toByte() }
        return out
    }
}

// ====================================================================== reguli (zonă, sondaj, contor → replică)

/** O regulă a Regiei, evaluată de hub. `on`: zone (arg = zona), pollwin (arg = opțiunea câștigătoare, de la 0), meter (kind + prag). */
class Rule(val id: String, val on: String, val arg: Int, val meterKind: String, val to: String, val kind: String, val text: String, val once: Boolean)

object Rules {
    val ALLOWED = Osc.CUE_KIND_NAMES - setOf("poll", "pollend", "realcall", "dmx", "midi") + "goto"

    /**
     * Linii: `zone 3 once > self | text | Bine ai venit, {name}`; `pollwin 0 > all | color | #00ff00;3`;
     * `meter noise 70 > all | blackout | 1;` ; ținta: all, staff, spectators, actors, self (cel care a declanșat), num:/id:/grp:.
     */
    fun parse(text: String): List<Rule> {
        val out = ArrayList<Rule>()
        for ((n, raw) in text.lines().withIndex()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val gt = line.indexOf('>')
            if (gt < 0) continue
            val left = line.substring(0, gt).trim().split(Regex("\\s+")).toMutableList()
            val right = line.substring(gt + 1).split("|", limit = 3).map { it.trim() }
            if (right.size < 2) continue
            val once = left.remove("once")
            val on = left.getOrNull(0) ?: continue
            val (arg, mk) = when (on) {
                "solved" -> Pair(0, "")
                "zone", "pollwin" -> Pair(left.getOrNull(1)?.toIntOrNull() ?: continue, "")
                "meter" -> {
                    val k = left.getOrNull(1) ?: continue
                    if (k != "noise" && k != "motion") continue
                    Pair(left.getOrNull(2)?.toIntOrNull()?.coerceIn(1, 100) ?: continue, k)
                }
                else -> continue
            }
            val to = right[0]
            val kind = right[1]
            if (kind !in ALLOWED) continue
            if (!(to in setOf("all", "staff", "spectators", "actors", "self") || Regex("(num|id|grp):[A-Za-z0-9 _.-]{1,40}").matches(to))) continue
            out.add(Rule("r" + (n + 1), on, arg, mk, to, kind, right.getOrElse(2) { "" }.take(200), once))
            if (out.size >= 50) break
        }
        return out
    }

    fun toJson(rs: List<Rule>): org.json.JSONArray {
        val a = org.json.JSONArray()
        for (r in rs) a.put(jo("id" to r.id, "on" to r.on, "arg" to r.arg, "mk" to r.meterKind, "to" to r.to, "kind" to r.kind, "text" to r.text, "once" to r.once))
        return a
    }

    fun fromJson(a: org.json.JSONArray?): List<Rule> {
        val out = ArrayList<Rule>()
        if (a == null) return out
        for (i in 0 until minOf(a.length(), 50)) {
            val o = a.optJSONObject(i) ?: continue
            val kind = o.optString("kind")
            val on = o.optString("on")
            val to = o.optString("to")
            if (kind !in ALLOWED || on !in setOf("zone", "pollwin", "meter", "solved")) continue
            if (!(to in setOf("all", "staff", "spectators", "actors", "self") || Regex("(num|id|grp):[A-Za-z0-9 _.-]{1,40}").matches(to))) continue
            out.add(Rule(o.optString("id").filter { it.isLetterOrDigit() }.take(8).ifEmpty { "r$i" }, on, o.optInt("arg"), o.optString("mk"), to, kind, o.optString("text").filter { it >= ' ' }.take(200), o.optBoolean("once")))
        }
        return out
    }
}

// ====================================================================== v1.8: Art‑Net/DMX, MIDI, Morse, valuri, raport

/** Art‑Net ArtDMX (UDP 6454). */
object ArtNet {
    const val PORT = 6454

    fun packet(universe: Int, seq: Int, data: ByteArray): ByteArray {
        val len = (data.size + 1) and 1.inv()
        val b = ByteArray(18 + len)
        "Art-Net".toByteArray().copyInto(b)
        b[8] = 0x00; b[9] = 0x50                 // OpDmx, little‑endian
        b[10] = 0; b[11] = 14                    // versiunea protocolului
        b[12] = (seq and 0xFF).toByte(); b[13] = 0
        b[14] = (universe and 0xFF).toByte(); b[15] = ((universe shr 8) and 0x7F).toByte()
        b[16] = ((len shr 8) and 0xFF).toByte(); b[17] = (len and 0xFF).toByte()
        data.copyInto(b, 18)
        return b
    }
}

/** „0;1=255,2-4=128;2000” = univers 0 (de la 0), canalul 1 la 255, canalele 2–4 la 128, tranziție de 2 s. */
class DmxCmd(val universe: Int, val target: Map<Int, Int>, val fadeMs: Long) {
    companion object {
        fun parse(text: String): DmxCmd? {
            val p = text.split(";").map { it.trim() }
            val u = p.getOrNull(0)?.toIntOrNull() ?: return null
            if (u !in 0..32767) return null
            val t = HashMap<Int, Int>()
            for (item in (p.getOrNull(1) ?: return null).split(",")) {
                val kv = item.split("=")
                if (kv.size != 2) return null
                val v = kv[1].trim().toIntOrNull()?.coerceIn(0, 255) ?: return null
                val r = kv[0].trim().split("-")
                val a = r.getOrNull(0)?.toIntOrNull() ?: return null
                val b = if (r.size == 2) (r[1].toIntOrNull() ?: return null) else a
                if (r.size > 2 || a < 1 || b > 512 || b < a) return null
                for (c in a..b) t[c] = v
            }
            if (t.isEmpty()) return null
            return DmxCmd(u, t, (p.getOrNull(2)?.toLongOrNull() ?: 0L).coerceIn(0L, 600_000L))
        }
    }
}

/** 512 de canale cu tranziții liniare. */
class DmxFader {
    private val cur = DoubleArray(512)
    private var from = DoubleArray(512)
    private var to = DoubleArray(512)
    private var t0 = 0L
    private var dur = 0L

    @Synchronized
    fun go(c: DmxCmd, now: Long) {
        val f = frameD(now)
        from = f
        to = f.copyOf()
        for ((ch, v) in c.target) to[ch - 1] = v.toDouble()
        t0 = now
        dur = c.fadeMs
    }

    private fun frameD(now: Long): DoubleArray {
        val k = if (dur <= 0) 1.0 else ((now - t0).toDouble() / dur).coerceIn(0.0, 1.0)
        return DoubleArray(512) { from[it] + (to[it] - from[it]) * k }
    }

    @Synchronized
    fun frame(now: Long): ByteArray {
        val d = frameD(now)
        return ByteArray(512) { Math.round(d[it]).toInt().coerceIn(0, 255).toByte() }
    }

    @Synchronized
    fun fading(now: Long) = dur > 0 && now - t0 < dur
}

/** MIDI din text: `note 60 127 [canal]`, `off 60 [canal]`, `cc 7 100 [canal]`, `pc 5 [canal]`, `mmc play|stop|record`. */
object Midi {
    fun encode(text: String): ByteArray? {
        val p = text.trim().lowercase().split(Regex("\\s+"))
        fun n(i: Int, lo: Int, hi: Int): Int? = p.getOrNull(i)?.toIntOrNull()?.takeIf { it in lo..hi }
        fun ch(i: Int): Int = (p.getOrNull(i)?.toIntOrNull()?.takeIf { it in 1..16 } ?: 1) - 1
        return when (p[0]) {
            "note" -> { val a = n(1, 0, 127) ?: return null; val v = n(2, 0, 127) ?: return null; byteArrayOf((0x90 or ch(3)).toByte(), a.toByte(), v.toByte()) }
            "off" -> { val a = n(1, 0, 127) ?: return null; byteArrayOf((0x80 or ch(2)).toByte(), a.toByte(), 0) }
            "cc" -> { val a = n(1, 0, 127) ?: return null; val v = n(2, 0, 127) ?: return null; byteArrayOf((0xB0 or ch(3)).toByte(), a.toByte(), v.toByte()) }
            "pc" -> { val a = n(1, 0, 127) ?: return null; byteArrayOf((0xC0 or ch(2)).toByte(), a.toByte()) }
            "mmc" -> {
                val c = when (p.getOrNull(1)) { "stop" -> 1; "play" -> 2; "record" -> 6; else -> return null }
                byteArrayOf(0xF0.toByte(), 0x7F, 0x7F, 0x06, c.toByte(), 0xF7.toByte())
            }
            else -> null
        }
    }
}

/** Cod Morse ca model de vibrații (ms: pauză, vibrație, pauză, …); începe cu pauză 0. */
object Morse {
    private val T = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".", 'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..",
        'J' to ".---", 'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---", 'P' to ".--.", 'Q' to "--.-", 'R' to ".-.",
        'S' to "...", 'T' to "-", 'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--", 'Z' to "--..",
        '0' to "-----", '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----."
    )

    /** Model Android (pauză, vibrație, pauză, …), începe cu pauză 0. */
    fun pattern(text: String, unit: Long = 120): LongArray {
        val out = ArrayList<Long>()
        for (w in text.uppercase().take(60).split(Regex("\\s+"))) {
            var inWord = false
            for (ch in w) {
                val code = T[ch] ?: continue
                for ((i, s) in code.withIndex()) {
                    if (out.sum() > 45_000) return out.toLongArray()
                    val pause = if (i > 0) unit else if (inWord) unit * 3 else if (out.isNotEmpty()) unit * 7 else 0L
                    out.add(pause)
                    out.add(if (s == '.') unit else unit * 3)
                }
                inWord = true
            }
        }
        if (out.isEmpty()) out.add(0)
        return out.toLongArray()
    }
}

/** Raport după spectacol (Markdown). */
class RPeer(val name: String, val role: String, val number: String, val online: Boolean, val bat: Int, val zone: Int, val calls: Int, val msgs: Int, val zones: Int)

object Report {
    fun markdown(hub: String, startMs: Long, nowMs: Long, peers: List<RPeer>, events: List<String>, counters: Map<String, Int>): String {
        val sb = StringBuilder()
        val mins = (nowMs - startMs) / 60000
        sb.append("# ").append(clean(hub.ifEmpty { "CUPLAJ" })).append(" — show report\n\n")
        sb.append("Generated: ").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date(nowMs))).append(" · hub up ").append(mins).append(" min\n\n")
        sb.append("## Totals\n\n")
        for ((k, v) in counters) sb.append("* ").append(k).append(": ").append(v).append('\n')
        val on = peers.count { it.online }
        sb.append("* phones known: ").append(peers.size).append(" (online now: ").append(on).append(")\n")
        val lowBat = peers.filter { it.bat in 0..19 }
        if (lowBat.isNotEmpty()) sb.append("* low battery (<20 %): ").append(lowBat.joinToString(", ") { it.name }).append('\n')
        sb.append("\n## Phones\n\n| Name | Role | Number | Calls | Messages | Zone entries | Battery |\n|---|---|---|---|---|---|---|\n")
        for (p in peers.take(150)) sb.append("| ").append(clean(p.name)).append(" | ").append(p.role).append(" | ").append(p.number).append(" | ").append(p.calls).append(" | ").append(p.msgs).append(" | ").append(p.zones).append(" | ").append(if (p.bat >= 0) p.bat.toString() + " %" else "–").append(" |\n")
        if (peers.size > 150) sb.append("\n(").append(peers.size - 150).append(" more not listed)\n")
        sb.append("\n## Timeline\n\n")
        for (e in events.takeLast(400)) sb.append("* ").append(clean(e)).append('\n')
        return sb.toString()
    }

    private fun clean(s: String) = s.replace("|", "/").replace("\n", " ")
}
