package ro.trom.cuplaj

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiInputPort
import android.media.midi.MidiManager
import android.os.Handler
import android.os.Looper
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

/** Ieșire Art‑Net către mixerul de lumini: 25 Hz în timpul tranzițiilor, 1 Hz altfel. Doar pe telefonul Regiei. */
class DmxOut(private val host: String, private val universe0: Int = 0) {
    private val fader = DmxFader()
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r).apply { isDaemon = true } }
    private val ui = Handler(Looper.getMainLooper())
    @Volatile private var run = true
    private var seq = 0
    private var sock: DatagramSocket? = null
    private var uni = universe0

    private val tick = object : Runnable {
        override fun run() {
            if (!this@DmxOut.run) return
            val now = System.currentTimeMillis()
            val frame = fader.frame(now)
            val u = uni
            io.execute { sendFrame(u, frame) }
            ui.postDelayed(this, if (fader.fading(now)) 40L else 1000L)
        }
    }

    init { ui.post(tick) }

    private fun sendFrame(u: Int, frame: ByteArray) {
        try {
            val s = sock ?: DatagramSocket().also { it.broadcast = true; sock = it }
            val pk = ArtNet.packet(u, (++seq and 0xFF).let { if (it == 0) 1 else it }, frame)
            s.send(DatagramPacket(pk, pk.size, InetAddress.getByName(host.ifBlank { "255.255.255.255" }), ArtNet.PORT))
        } catch (_: Exception) {
        }
    }

    fun go(c: DmxCmd) {
        uni = c.universe
        fader.go(c, System.currentTimeMillis())
        ui.removeCallbacks(tick)
        ui.post(tick)
    }

    fun close() {
        run = false
        ui.removeCallbacks(tick)
        io.execute { try { sock?.close() } catch (_: Exception) {} }
    }
}

/** Ieșire MIDI USB / Bluetooth prin MidiManager (prima ieșire găsită). Nu poate fi verificată fără hardware. */
class MidiOut(private val ctx: Context) {
    private var port: MidiInputPort? = null
    private var dev: MidiDevice? = null
    private var opening = false

    private fun open() {
        if (port != null || opening) return
        try {
            val mm = ctx.getSystemService(Context.MIDI_SERVICE) as? MidiManager ?: return
            val info = mm.devices.firstOrNull { it.inputPortCount > 0 } ?: return
            opening = true
            mm.openDevice(info, { d ->
                opening = false
                dev = d
                port = d?.openInputPort(0)
            }, Handler(Looper.getMainLooper()))
        } catch (_: Exception) {
            opening = false
        }
    }

    /** @return true dacă mesajul a fost scris pe port. */
    fun send(b: ByteArray): Boolean {
        open()
        val p = port ?: return false
        return try { p.send(b, 0, b.size); true } catch (_: Exception) { false }
    }

    fun close() {
        try { port?.close() } catch (_: Exception) {}
        try { dev?.close() } catch (_: Exception) {}
        port = null; dev = null
    }
}
