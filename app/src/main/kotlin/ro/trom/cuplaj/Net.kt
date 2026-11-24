package ro.trom.cuplaj

import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Socket

/** Clientul: ține conexiunea cu hubul (cu reconectare automată) și duce mesaje JSON și audio. */
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

    fun start(host: String, hello: JSONObject) {
        stop()
        run = true
        gen++
        val my = gen
        Thread { loop(my, host, hello) }.apply {
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

    private fun loop(my: Int, host: String, hello: JSONObject) {
        var delay = 1000L
        while (run && gen == my) {
            try {
                onState("Conectare la $host…")
                val s = Socket()
                s.connect(InetSocketAddress(host, PORT), 4000)
                val c = Conn(s)
                conn = c
                c.startWriter()
                c.sendJson(hello)
                delay = 1000L
                while (run && gen == my) {
                    val f = c.readFrame()
                    if (f.type == T_JSON) {
                        onJson(parseJson(f))
                    } else if (f.type == T_AUDIO && f.data.size > 1) {
                        onAudio(f.data[0].toInt() and 0xFF, f.data)
                    }
                }
            } catch (_: Exception) {
            }
            conn?.close()
            if (run && gen == my) {
                onState("Conexiune pierdută; reîncerc…")
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
        val b = ByteArray(pcm.size + 1)
        b[0] = mode.toByte()
        System.arraycopy(pcm, 0, b, 1, pcm.size)
        conn?.send(Frame(T_AUDIO, b))
    }
}
