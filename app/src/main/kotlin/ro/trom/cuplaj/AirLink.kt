package ro.trom.cuplaj

import java.net.InetAddress

/** Legătura dintre protocoalele Air și aplicație: trimitere (Regie) și recepție (orice telefon). */
object AirLink {
    val MODES = listOf("light", "sound", "vibe")
    private val guard = AirGuard()
    private var seq = (System.nanoTime() and 0x7F).toInt()
    private var light: AirIo.LightRx? = null
    private var sound: AirIo.SoundRx? = null
    private var vibe: AirIo.VibeRx? = null

    @Volatile
    var busy = false
    var rxLight = false
    var rxSound = false
    var rxVibe = false
    var ultra = true

    private fun key(): ByteArray? {
        val k = if (App.role == "regie") (if (App.encOn) App.netKey else "") else App.joinKey.trim()
        return if (k.isEmpty()) null else Sec.deriveKey(k)
    }

    private fun nextSeq(): Int { seq = (seq + 1) and 0xFF; return seq }

    /** Cue scurt (≤ 11 octeți de text) pe lumină, sunet sau vibrații. Întoarce false dacă mediul nu e disponibil. */
    fun sendCue(mode: String, kind: String, text: String): Boolean {
        val ix = CUE_KINDS.indexOf(kind)
        if (ix < 0 || busy) return false
        val data = Air.cue(ix, text).let { if (it.size > 12) it.copyOf(12) else it }
        return transmit(mode, Air.pack(AirPacket(Air.T_CUE, nextSeq(), if (mode == "vibe" && data.size > 6) data.copyOf(6) else data), key(), if (mode == "sound") 4 else 2), mode == "sound")
    }

    /** Invitație compactă: IPv4 (4 octeți) + PIN de 6 cifre (3 octeți). Fără cheie: telefonul nou nu o are încă; confirmarea o face omul. */
    fun sendInvite(mode: String): Boolean {
        if (busy || mode == "vibe") return false
        val ip = App.localIps().firstOrNull { it.contains('.') } ?: return false
        val a = try { InetAddress.getByName(ip).address } catch (_: Exception) { return false }
        if (a.size != 4) return false
        val pin = App.hubPin.toIntOrNull() ?: return false
        val d = a + byteArrayOf((pin shr 16).toByte(), (pin shr 8).toByte(), pin.toByte())
        return transmit(mode, Air.pack(AirPacket(Air.T_INVITE, nextSeq(), d), null, if (mode == "sound") 4 else 2), mode == "sound")
    }

    private fun transmit(mode: String, raw: ByteArray, @Suppress("UNUSED_PARAMETER") long: Boolean): Boolean {
        val c = App.ctx
        busy = true
        val done = { busy = false; App.notify("air") }
        val ok = when (mode) {
            "light" -> AirIo.sendLight(c, raw, null, done)
            "sound" -> { AirIo.sendSound(raw, ultra, done); true }
            "vibe" -> {
                val ms = AirIo.sendVibe(c, raw)
                App.ui.postDelayed(done, ms + 500)
                ms > 0
            }
            else -> false
        }
        if (!ok) busy = false
        return ok
    }

    /** Pornește/oprește receptoarele cerute. Întoarce modurile care nu au putut porni (lipsă permisiune/senzor). */
    fun applyRx(): List<String> {
        val failed = ArrayList<String>()
        light?.stop(); light = null
        sound?.stop(); sound = null
        vibe?.stop(); vibe = null
        val c = App.ctx
        val k = key()
        if (rxLight) { val r = AirIo.LightRx(c, k) { got(it) }; if (r.start()) light = r else failed.add("light") }
        if (rxSound) { val r = AirIo.SoundRx(c, ultra, k) { got(it) }; if (r.start()) sound = r else failed.add("sound") }
        if (rxVibe) { val r = AirIo.VibeRx(c, k) { got(it) }; if (r.start()) vibe = r else failed.add("vibe") }
        return failed
    }

    fun stopRx() {
        rxLight = false; rxSound = false; rxVibe = false
        applyRx()
    }

    /** Pachet primit și deja verificat (MAC). Reluările sunt respinse. */
    private fun got(p: AirPacket) {
        if (!guard.accept(p)) return
        when (p.type) {
            Air.T_CUE -> {
                if (p.data.isEmpty()) return
                val kind = CUE_KINDS.getOrNull(p.data[0].toInt() and 0xFF) ?: return
                // numai efecte locale inofensive prin aer; apelurile false/reale și schimbările de eră rămân pe rețea
                if (kind !in setOf("text", "vibrate", "ring", "flash", "blackout", "sound", "panic", "color")) return
                val text = String(p.data, 1, p.data.size - 1, Charsets.UTF_8)
                App.airCue(kind, text)
            }
            Air.T_INVITE -> {
                if (p.data.size != 7 || App.role == "regie") return
                val ip = (0 until 4).joinToString(".") { (p.data[it].toInt() and 0xFF).toString() }
                val pin = ((p.data[4].toInt() and 0xFF) shl 16) or ((p.data[5].toInt() and 0xFF) shl 8) or (p.data[6].toInt() and 0xFF)
                if (pin > 999999) return
                App.notify("airinvite:" + ip + "|" + "%06d".format(pin))
            }
        }
    }
}
