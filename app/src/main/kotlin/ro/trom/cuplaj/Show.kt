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

    val CUE_KIND_NAMES = setOf("text", "vibrate", "ring", "flash", "speaker", "contact", "era", "say", "sound", "blackout", "fakecall", "realcall", "panic", "color", "poll", "pollend", "image", "video", "clip")
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
