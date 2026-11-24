package ro.trom.cuplaj

import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

const val VERSION = "1.0.0"
const val VERSION_DATE = "2026-11-24"
const val PORT = 8470
const val BEACON_PORT = 8471
const val T_JSON = 1
const val T_AUDIO = 2

/** Un cadru pe fir: 1 octet tip, 4 octeți lungime, date. */
class Frame(val type: Int, val data: ByteArray)

fun jo(vararg p: Pair<String, Any?>): JSONObject {
    val o = JSONObject()
    for ((k, v) in p) o.put(k, v ?: JSONObject.NULL)
    return o
}

fun parseJson(f: Frame): JSONObject = JSONObject(String(f.data, Charsets.UTF_8))

/** Conexiune TCP cu coadă de scriere proprie (un client lent nu blochează restul). */
class Conn(val sock: Socket) {
    private val din = DataInputStream(BufferedInputStream(sock.getInputStream(), 8192))
    private val dout = DataOutputStream(BufferedOutputStream(sock.getOutputStream(), 8192))
    private val q = LinkedBlockingQueue<Frame>(400)

    @Volatile
    var open = true

    init {
        try {
            sock.tcpNoDelay = true
        } catch (_: Exception) {
        }
    }

    fun startWriter() {
        Thread {
            try {
                while (open) {
                    val f = q.poll(500, TimeUnit.MILLISECONDS) ?: continue
                    dout.writeByte(f.type)
                    dout.writeInt(f.data.size)
                    dout.write(f.data)
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

    fun readFrame(): Frame {
        val t = din.readUnsignedByte()
        val n = din.readInt()
        if (n < 0 || n > 200_000) throw IOException("cadru invalid")
        val b = ByteArray(n)
        din.readFully(b)
        return Frame(t, b)
    }

    fun close() {
        open = false
        try {
            sock.close()
        } catch (_: Exception) {
        }
    }
}
