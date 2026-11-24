package ro.trom.cuplaj

import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

/** Clientul: ține conexiunea cu hubul (cu reconectare automată, bătăi de inimă, cifrare opțională) și duce JSON și audio. */
class Net(
    private val onJson: (JSONObject) -> Unit,
    private val onAudio: (Int, ByteArray) -> Unit,
    private val onState: (String) -> Unit
) {
    @Volatile
    private var conn: Conn? = null

    @Volatile
    private var run = false

    @Volatile
    private var gen = 0

    /** true = trimite audio μ-law (mai puțin trafic, pentru internet/date mobile). */
    @Volatile
    var compress = false

    /** null = automat (comprimă când hubul nu e în rețeaua locală). */
    var forceCompress: Boolean? = null

    @Volatile
    var remote = false
        private set

    fun start(host: String, hello: JSONObject, key: ByteArray? = null) {
        stop()
        run = true
        gen++
        val my = gen
        Thread { loop(my, host, hello, key) }.apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        run = false
        gen++
        conn?.close()
        conn = null
    }

    private fun loop(my: Int, hostPort: String, hello: JSONObject, key: ByteArray?) {
        val (host, port) = parseHostPort(hostPort)
        var delay = 1000L
        while (run && gen == my) {
            try {
                onState(L.t("net_connecting", hostPort))
                val ch: Chan = if (hostPort.startsWith("bt:")) {
                    Bt.connect(hostPort.substring(3))
                } else if (hostPort.startsWith("aw:")) {
                    Aware.connect(Bt.ctx, hostPort)
                } else {
                    val s = Socket()
                    s.connect(InetSocketAddress(host, port), 5000)
                    SocketChan(s)
                }
                ch.setIdleTimeout(45000) // hubul răspunde la ping la 15 s; liniște de 45 s = legătură moartă
                remote = ch.remote
                compress = forceCompress ?: (ch.remote || ch.lowBandwidth)
                val c = Conn(ch, key, false)
                conn = c
                c.startWriter()
                c.sendJson(hello)
                delay = 1000L
                val pinger = Thread {
                    try {
                        while (run && gen == my && c.open) {
                            Thread.sleep(15000)
                            c.sendJson(jo("t" to "ping"))
                        }
                    } catch (_: Exception) {
                    }
                }
                pinger.isDaemon = true
                pinger.start()
                while (run && gen == my) {
                    val f = c.readFrame()
                    if (f.bad) {
                        // hub fără/cu altă cheie: singurul mesaj acceptat în clar este refuzul
                        if (f.type == T_JSON && f.data.isNotEmpty()) {
                            val m = parseJson(f)
                            if (m.optString("t") == "denied") onJson(m)
                        }
                        break
                    }
                    if (f.type == T_JSON) {
                        onJson(parseJson(f))
                    } else if (f.type == T_AUDIO && f.data.size > 1) {
                        onAudio(f.data[0].toInt() and 0xFF, f.data)
                    } else if (f.type == T_AUDIO_U && f.data.size > 1) {
                        val pcm = G711.decode(f.data, 1, f.data.size - 1)
                        val out = ByteArray(pcm.size + 1)
                        out[0] = f.data[0]
                        System.arraycopy(pcm, 0, out, 1, pcm.size)
                        onAudio(f.data[0].toInt() and 0xFF, out)
                    }
                }
            } catch (_: Exception) {
            }
            conn?.close()
            if (run && gen == my) {
                onState(L.t("net_lost"))
                try {
                    Thread.sleep(delay)
                } catch (_: Exception) {
                }
                delay = minOf(delay * 2, 5000L)
            }
        }
    }

    fun send(o: JSONObject) {
        conn?.sendJson(o)
    }

    fun sendAudio(mode: Int, pcm: ByteArray) {
        val payload = if (compress) G711.encode(pcm) else pcm
        val b = ByteArray(payload.size + 1)
        b[0] = mode.toByte()
        System.arraycopy(payload, 0, b, 1, payload.size)
        conn?.send(Frame(if (compress) T_AUDIO_U else T_AUDIO, b))
    }
}
