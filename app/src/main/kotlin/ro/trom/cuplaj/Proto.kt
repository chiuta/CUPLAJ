package ro.trom.cuplaj

import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

const val VERSION = "1.5.0"
const val VERSION_DATE = "2026-11-24"
const val PORT = 8470
const val BEACON_PORT = 8471
const val T_JSON = 1
const val T_AUDIO = 2
const val T_AUDIO_U = 3 // audio μ-law (G.711), 160 octeți/20 ms
const val P2P_PORT_OFFSET = 2 // UDP: PORT + 2
const val FLAG_ENC = 0x80
const val T_CHAL = 4 // schimb de nonce-uri pentru cheia de sesiune (în clar)

/** Un cadru pe fir: 1 octet tip, 4 octeți lungime, date. */
class Frame(val type: Int, val data: ByteArray, val plain: Boolean = false, val bad: Boolean = false)

fun jo(vararg p: Pair<String, Any?>): JSONObject {
    val o = JSONObject()
    for ((k, v) in p) o.put(k, v ?: JSONObject.NULL)
    return o
}

fun parseJson(f: Frame): JSONObject = JSONObject(String(f.data, Charsets.UTF_8))

/** Conexiune TCP cu coadă de scriere proprie (un client lent nu blochează restul). */
/** Un canal de octeți către un dispozitiv: TCP, Bluetooth sau orice alt transport care oferă două fluxuri. */
interface Chan {
    val input: InputStream
    val output: OutputStream

    /** Liniște maximă acceptată; după ea citirea eșuează și conexiunea se închide. */
    fun setIdleTimeout(ms: Int)

    /** Originea (IP) pentru limitarea abuzurilor. */
    val origin: String

    /** Conexiune de pe același telefon. */
    val loopback: Boolean

    /** Hubul nu e în rețeaua locală (internet): se comprimă audio. */
    val remote: Boolean

    /** Legătură cu banda mică (Bluetooth): se comprimă audio. */
    val lowBandwidth: Boolean
    fun close()
}

class SocketChan(val sock: Socket) : Chan {
    init {
        try {
            sock.tcpNoDelay = true
            sock.keepAlive = true
        } catch (_: Exception) {
        }
    }

    override val input: InputStream get() = sock.getInputStream()
    override val output: OutputStream get() = sock.getOutputStream()
    override fun setIdleTimeout(ms: Int) {
        sock.soTimeout = ms
    }

    override val loopback: Boolean get() = sock.inetAddress?.isLoopbackAddress == true
    override val origin: String get() = sock.inetAddress?.hostAddress ?: "?"
    override val remote: Boolean
        get() {
            val a = sock.inetAddress ?: return false
            return !(a.isSiteLocalAddress || a.isLoopbackAddress || a.isLinkLocalAddress)
        }
    override val lowBandwidth: Boolean get() = false
    override fun close() {
        try {
            sock.close()
        } catch (_: Exception) {
        }
    }
}

/** Canal peste două fluxuri oarecare (Bluetooth RFCOMM, țevi în teste); un paznic închide canalul după liniștea setată. */
class StreamChan(
    inp: InputStream,
    override val output: OutputStream,
    private val onClose: () -> Unit,
    override val lowBandwidth: Boolean = true
) : Chan {
    @Volatile
    private var lastRx = System.currentTimeMillis()

    @Volatile
    private var idle = 0

    @Volatile
    private var closed = false
    override val input: InputStream = object : InputStream() {
        override fun read(): Int {
            val r = inp.read()
            if (r >= 0) lastRx = System.currentTimeMillis()
            return r
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val r = inp.read(b, off, len)
            if (r > 0) lastRx = System.currentTimeMillis()
            return r
        }

        override fun available(): Int = inp.available()
    }
    override val loopback: Boolean get() = false
    override val remote: Boolean get() = false
    override val origin: String get() = "stream"

    init {
        Thread {
            while (!closed) {
                try {
                    Thread.sleep(500)
                } catch (_: Exception) {
                }
                val t = idle
                if (t > 0 && System.currentTimeMillis() - lastRx > t) {
                    close()
                    break
                }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    override fun setIdleTimeout(ms: Int) {
        lastRx = System.currentTimeMillis()
        idle = ms
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            onClose()
        } catch (_: Exception) {
        }
    }
}

class Conn(val chan: Chan, private val key: ByteArray? = null, private val server: Boolean = false) {
    constructor(sock: Socket, key: ByteArray? = null, server: Boolean = false) : this(SocketChan(sock), key, server)

    private val nPrefix = ByteArray(4).also { java.security.SecureRandom().nextBytes(it); it[0] = if (server) 1 else 2 }
    private var nCtr = 0L
    private var rxCtr = 0L

    /** Cheia de sesiune (după schimbul de nonce-uri); fără ea nu se trimite și nu se acceptă nimic cifrat. */
    @Volatile
    private var skey: ByteArray? = null
    private var pending: Frame? = null

    /** Limita de lungime a unui cadru primit: strânsă înainte de autentificare, apoi mai largă. */
    @Volatile
    var maxFrame = if (server) 2048 else 200_000

    /** Client cu cheie: trimite nonce‑ul, primește nonce‑ul hubului și derivă cheia de sesiune. Se apelează înainte de startWriter. */
    fun clientHandshake() {
        val mk = key ?: return
        val cn = Sec.randomBytes(16)
        dout.writeByte(T_CHAL)
        dout.writeInt(16)
        dout.write(cn)
        dout.flush()
        val t = din.readUnsignedByte()
        val n = din.readInt()
        if (n < 0 || n > 4096) throw IOException("cadru invalid")
        val b = ByteArray(n)
        din.readFully(b)
        if (t == T_CHAL && n == 16) {
            skey = Sec.session(mk, b, cn)
        } else {
            pending = Frame(t and FLAG_ENC.inv(), b, bad = true) // refuz în clar (ex. denied)
        }
    }

    /**
     * Hub: citește primul cadru. Cu cheie așteaptă T_CHAL și răspunde cu nonce‑ul hubului;
     * fără cheie primul cadru rămâne pentru readFrame. Întoarce false dacă cheia nu se potrivește (cineva cere/nu cere cifrare).
     */
    fun serverHandshake(): Boolean {
        val t = din.readUnsignedByte()
        val n = din.readInt()
        if (n < 0 || n > maxFrame) throw IOException("cadru invalid")
        val b = ByteArray(n)
        din.readFully(b)
        val mk = key
        if (mk == null) {
            if (t == T_CHAL) return false
            pending = Frame(t and FLAG_ENC.inv(), b, bad = (t and FLAG_ENC) != 0)
            return true
        }
        if (t != T_CHAL || n != 16) return false
        val sn = Sec.randomBytes(16)
        dout.writeByte(T_CHAL)
        dout.writeInt(16)
        dout.write(sn)
        dout.flush()
        skey = Sec.session(mk, sn, b)
        return true
    }
    private val din = DataInputStream(BufferedInputStream(chan.input, 8192))
    private val dout = DataOutputStream(BufferedOutputStream(chan.output, 8192))
    private val q = LinkedBlockingQueue<Frame>(400)

    @Volatile
    var open = true

    fun startWriter() {
        Thread {
            try {
                while (open) {
                    val f = q.poll(500, TimeUnit.MILLISECONDS) ?: continue
                    val sk = skey
                    if (key != null && !f.plain && sk == null) continue // fără cheie de sesiune nu se trimite nimic în clar
                    if (key != null && !f.plain && sk != null) {
                        val nonce = ByteArray(12)
                        System.arraycopy(nPrefix, 0, nonce, 0, 4)
                        var c = ++nCtr
                        for (i in 11 downTo 4) { nonce[i] = (c and 0xFF).toByte(); c = c shr 8 }
                        val ct = Sec.encrypt(sk, nonce, f.data)
                        dout.writeByte(f.type or FLAG_ENC)
                        dout.writeInt(12 + ct.size)
                        dout.write(nonce)
                        dout.write(ct)
                    } else {
                        dout.writeByte(f.type)
                        dout.writeInt(f.data.size)
                        dout.write(f.data)
                    }
                    if (q.isEmpty()) dout.flush()
                }
            } catch (_: Exception) {
            } finally {
                close()
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun send(f: Frame) {
        if (open) q.offer(f)
    }

    /** Mesajele de control nu se pierd: dacă coada e plină, conexiunea se închide și clientul se resincronizează. */
    fun sendJson(o: JSONObject) {
        if (!open) return
        if (!q.offer(Frame(T_JSON, o.toString().toByteArray(Charsets.UTF_8)))) close()
    }

    /** Mesaj în clar chiar și pe legătură cifrată (doar pentru refuzuri, ca clientul să afle de ce). */
    fun sendJsonPlain(o: JSONObject) {
        if (open) q.offer(Frame(T_JSON, o.toString().toByteArray(Charsets.UTF_8), plain = true))
    }

    fun readFrame(): Frame {
        pending?.let {
            pending = null
            return it
        }
        val t = din.readUnsignedByte()
        val n = din.readInt()
        if (n < 0 || n > maxFrame) throw IOException("cadru invalid")
        val b = ByteArray(n)
        din.readFully(b)
        val enc = (t and FLAG_ENC) != 0
        val ty = t and FLAG_ENC.inv()
        if (key == null) return if (enc) Frame(ty, ByteArray(0), bad = true) else Frame(ty, b)
        val sk = skey ?: return Frame(ty, ByteArray(0), bad = true)
        if (!enc) return Frame(ty, b, bad = true)
        // cifrat: nonce(12) + text cifrat; contorul trebuie să crească (anti-reluare); primul octet = direcția
        if (n < 12 + 16 || (b[0].toInt() == 1) == server) return Frame(ty, ByteArray(0), bad = true)
        var ctr = 0L
        for (i in 4 until 12) ctr = (ctr shl 8) or (b[i].toLong() and 0xFF)
        if (ctr <= rxCtr) return Frame(ty, ByteArray(0), bad = true)
        val pt = Sec.decrypt(sk, b.copyOfRange(0, 12), b.copyOfRange(12, n)) ?: return Frame(ty, ByteArray(0), bad = true)
        rxCtr = ctr
        return Frame(ty, pt)
    }

    fun close() {
        open = false
        chan.close()
    }
}

/** „host” sau „host:port”; adresele IPv6 se scriu [::1]:8470. */
fun parseHostPort(s: String, defPort: Int = PORT): Pair<String, Int> {
    val t = s.trim()
    if (t.startsWith("[")) {
        val e = t.indexOf(']')
        if (e > 0) {
            val p = t.substring(e + 1).removePrefix(":").toIntOrNull()
            return Pair(t.substring(1, e), p ?: defPort)
        }
    }
    val i = t.lastIndexOf(':')
    if (i > 0 && t.indexOf(':') == i) {
        val p = t.substring(i + 1).toIntOrNull()
        if (p != null && p in 1..65535) return Pair(t.substring(0, i), p)
    }
    return Pair(t, defPort)
}

/** Doar adrese locale (RFC 1918 / link‑local) sunt candidate P2P acceptate în modul „doar rețea locală”. */
fun privateCand(s: String): Boolean {
    val (h, p) = parseHostPort(s, -1)
    if (p !in 1024..65535) return false
    val parts = h.split('.')
    if (parts.size != 4) return false
    val n = parts.map { it.toIntOrNull() ?: return false }
    if (n.any { it !in 0..255 }) return false
    return n[0] == 10 || n[0] == 127 || (n[0] == 172 && n[1] in 16..31) || (n[0] == 192 && n[1] == 168) || (n[0] == 169 && n[1] == 254)
}

/** Doar cifre și un „+” la început: fără coduri USSD (*…#) sau de redirecționare a apelurilor. */
fun safeNumber(n: String): String {
    val d = n.filter { it.isDigit() || it == '+' }
    val plus = d.startsWith("+")
    return (if (plus) "+" else "") + d.filter { it.isDigit() }.take(20)
}


// ====================================================================== v1.5: ceas comun, cue programat, culoare

/** Ceasul hubului văzut de client: ping–pong repetat, se păstrează estimarea cu cel mai mic RTT din ultimele 8. */
class ClockSync {
    private val rtts = LongArray(8) { Long.MAX_VALUE }
    private val offs = LongArray(8)
    private var n = 0

    @Synchronized
    fun add(t0: Long, hub: Long, t1: Long) {
        val rtt = t1 - t0
        if (rtt < 0 || rtt > 5000) return
        val i = n++ % 8
        rtts[i] = rtt
        offs[i] = hub - (t0 + t1) / 2
    }

    /** hub = local + offset */
    @Synchronized
    fun offset(): Long {
        var b = -1
        for (i in 0 until 8) if (rtts[i] != Long.MAX_VALUE && (b < 0 || rtts[i] < rtts[b])) b = i
        return if (b < 0) 0 else offs[b]
    }

    @Synchronized
    fun ready() = rtts.any { it != Long.MAX_VALUE }

    fun hubNow(local: Long) = local + offset()

    /** Cât să aștepte (ms) pentru un cue cu ora hubului `at`; null = prea departe sau prea vechi (> 10 s întârziere). */
    fun delayFor(at: Long, local: Long): Long? {
        val d = at - hubNow(local)
        return if (d > 600_000 || d < -10_000) null else maxOf(0L, d)
    }
}

/** „@3.5;text” → (3500 ms, „text”); fără prefix → (0, text). */
fun parseAt(text: String): Pair<Long, String> {
    val m = Regex("^@(\\d{1,3})(?:\\.(\\d))?;").find(text) ?: return Pair(0L, text)
    val ms = m.groupValues[1].toLong() * 1000 + (m.groupValues[2].ifEmpty { "0" }.toLong() * 100)
    return Pair(minOf(ms, 600_000L), text.substring(m.value.length))
}

/** Cue de culoare pe tot ecranul. Cadența e plafonată la 2 Hz (siguranță fotosensibilă), durata între 0,2 s și 10 min. */
class ColorPlan(val rgb: Int, val ms: Long, val hz: Double)

fun colorPlan(text: String): ColorPlan? {
    val p = text.split(";").map { it.trim() }
    val c = p.getOrNull(0)?.removePrefix("#") ?: return null
    if (!Regex("[0-9a-fA-F]{6}").matches(c)) return null
    val ms = ((p.getOrNull(1)?.toDoubleOrNull() ?: 3.0) * 1000).toLong().coerceIn(200L, 600_000L)
    val hz = (p.getOrNull(2)?.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 2.0)
    return ColorPlan(0xFF000000.toInt() or c.toInt(16), ms, hz)
}
