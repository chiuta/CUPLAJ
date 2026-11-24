package ro.trom.cuplaj

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * Legătură audio directă între două telefoane, pe UDP.
 * Un singur soclu UDP: (1) se anunță la hub („punch”, ca să existe o mapare NAT și hubul să-i afle adresa),
 * (2) sondează adresele candidate ale partenerului, (3) trimite audio μ-law cifrat cu cheia apelului.
 * Pachet: tip(1) | nonce(12) | AES-GCM(date). Tipuri: 1 sondă, 2 confirmare, 3 audio.
 */
class P2pLink(private val onPcm: (ByteArray) -> Unit) {
    private val sock = DatagramSocket(0).also { it.soTimeout = 300 }
    val port: Int get() = sock.localPort

    @Volatile
    private var run = true

    @Volatile
    private var hubAddr: InetSocketAddress? = null
    private var punch: ByteArray = ByteArray(0)

    @Volatile
    private var key: ByteArray? = null
    private var cands: List<InetSocketAddress> = emptyList()
    private var dir = 0

    @Volatile
    var peer: InetSocketAddress? = null
        private set
    private var ctr = 0L
    private var rx = 0L
    private val prefix = ByteArray(4).also { Sec.randomBytes(4).copyInto(it) }

    @Volatile
    var hubSeen = false
        private set

    @Volatile
    var rxPackets = 0
        private set

    /** Calea directă e gata: am primit sondă sau confirmare de la partener. */
    val up: Boolean get() = peer != null && key != null

    init {
        Thread { rxLoop() }.apply { isDaemon = true; start() }
        Thread { txLoop() }.apply { isDaemon = true; start() }
    }

    /** Adresa locală (ip:port) pentru candidatul LAN, dacă se găsește una privată. */
    fun lanCandidate(ips: List<String>): String = ips.firstOrNull { it.isNotEmpty() }?.let { "$it:$port" } ?: ""

    fun registerAt(hub: InetSocketAddress, id: String, tok: String) {
        hubAddr = hub
        punch = "CUPLAJP|$id|$tok".toByteArray(Charsets.UTF_8)
    }

    /** Pornește o sesiune: cheia apelului, candidatele partenerului, iar `init` alege direcția nonce-ului. */
    fun begin(callKey: ByteArray, candidates: List<InetSocketAddress>, init: Boolean) {
        key = callKey
        dir = if (init) 1 else 2
        prefix[0] = dir.toByte()
        cands = candidates
        peer = null
        rx = 0
        probeUntil = System.currentTimeMillis() + 4000
    }

    @Volatile
    private var probeUntil = 0L

    fun end() {
        key = null
        peer = null
        cands = emptyList()
    }

    fun close() {
        run = false
        sock.close()
    }

    private fun nonce(): ByteArray {
        val n = ByteArray(12)
        System.arraycopy(prefix, 0, n, 0, 4)
        var c = ++ctr
        for (i in 11 downTo 4) { n[i] = (c and 0xFF).toByte(); c = c shr 8 }
        return n
    }

    private fun pack(type: Int, data: ByteArray): ByteArray? {
        val k = key ?: return null
        val n = nonce()
        val ct = Sec.encrypt(k, n, data)
        val out = ByteArray(1 + 12 + ct.size)
        out[0] = type.toByte()
        System.arraycopy(n, 0, out, 1, 12)
        System.arraycopy(ct, 0, out, 13, ct.size)
        return out
    }

    private fun sendTo(a: InetSocketAddress, b: ByteArray) {
        try {
            sock.send(DatagramPacket(b, b.size, a))
        } catch (_: Exception) {
        }
    }

    /** Trimite audio PCM (320 octeți) direct partenerului; false dacă legătura nu e gata. */
    fun sendPcm(pcm: ByteArray): Boolean {
        val p = peer ?: return false
        val b = pack(3, G711.encode(pcm)) ?: return false
        sendTo(p, b)
        return true
    }

    private fun txLoop() {
        var tick = 0
        while (run) {
            try {
                Thread.sleep(200)
            } catch (_: Exception) {
            }
            val h = hubAddr
            // menține maparea NAT și (re)anunță adresa către hub: des înainte de apel, rar apoi
            if (h != null && punch.isNotEmpty() && (tick % 25 == 0 || !hubSeen && tick % 5 == 0)) sendTo(h, punch)
            tick++
            if (key != null && peer == null && System.currentTimeMillis() < probeUntil) {
                val pr = pack(1, byteArrayOf(1))
                if (pr != null) for (c in cands) sendTo(c, pr)
            }
        }
    }

    private fun rxLoop() {
        val buf = ByteArray(1500)
        while (run) {
            try {
                val pk = DatagramPacket(buf, buf.size)
                sock.receive(pk)
                val len = pk.length
                if (len == 10 && String(buf, 0, 10, Charsets.UTF_8) == "CUPLAJP-OK") {
                    hubSeen = true
                    continue
                }
                val k = key ?: continue
                if (len < 1 + 12 + 16) continue
                val type = buf[0].toInt()
                val n = buf.copyOfRange(1, 13)
                if (n[0].toInt() == dir) continue // reflexie
                var c = 0L
                for (i in 4 until 12) c = (c shl 8) or (n[i].toLong() and 0xFF)
                val pt = Sec.decrypt(k, n, buf.copyOfRange(13, len)) ?: continue
                val from = InetSocketAddress(pk.address, pk.port)
                when (type) {
                    1 -> {
                        if (peer == null) peer = from
                        val a = pack(2, byteArrayOf(1))
                        if (a != null) sendTo(from, a)
                    }
                    2 -> if (peer == null) peer = from
                    3 -> {
                        if (c <= rx) continue
                        rx = c
                        rxPackets++
                        if (peer == null) peer = from
                        onPcm(G711.decode(pt))
                    }
                }
            } catch (_: SocketTimeoutException) {
            } catch (_: Exception) {
                if (!run) return
            }
        }
    }
}

fun unhex(s: String): ByteArray = ByteArray(s.length / 2) { i -> s.substring(2 * i, 2 * i + 2).toInt(16).toByte() }

fun parseAddr(s: String): InetSocketAddress? {
    val (h, p) = parseHostPort(s, -1)
    if (p <= 0 || h.isEmpty()) return null
    return try {
        InetSocketAddress(java.net.InetAddress.getByName(h), p)
    } catch (_: Exception) {
        null
    }
}
