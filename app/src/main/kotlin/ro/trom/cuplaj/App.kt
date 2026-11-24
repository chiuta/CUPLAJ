package ro.trom.cuplaj

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.util.UUID

class Msg(val from: String, val num: String, val text: String, val mine: Boolean, val to: String)

class PeerInfo(
    val id: String, val name: String, val role: String, val number: String, val group: String,
    val line: Int, val slot: Int, val online: Boolean, val busy: Boolean, val mon: Boolean,
    val muted: Boolean, val bat: Int
)

class Found(val name: String, val host: String, val locked: Boolean)

class Fake(val name: String, val num: String, val text: String)

class Step(var name: String, var to: String, var kind: String, var text: String, var delay: Int)

/** Starea aplicației și logica telefonului; trăiește cât trăiește procesul (serviciul îl ține în viață). */
object App {
    lateinit var ctx: Context
    private var inited = false
    val ui = Handler(Looper.getMainLooper())

    var hubObj: Hub? = null
    var net: Net? = null
    val playout = Playout()
    val ringer = Ringer()
    val mic = Mic { f -> net?.sendAudio(micMode, f) }

    @Volatile
    var micMode = 0

    // profil
    var role = "spectator"
    var myName = ""
    var hubHost = ""
    var apin = ""
    var room = ""
    var era = 1995
    var hubPin = ""
    var deviceId = ""
    var textScale = 1f

    // rețea
    var connected = false
    var status = ""
    var number = ""
    var myGroup = ""
    var line = 0
    var slot = 0
    var partnerNum = ""
    private var curTarget = ""

    // telefon
    var ph = "idle"   // idle dialtone dialing ringback ringing talking listening busy unknown
    val dialed = StringBuilder()
    var peerNumber = ""
    var peerName = ""
    var forNum = ""
    var intervening = false
    var ringNum = ""
    var ringName = ""
    var ringParty = false
    var pttMode = -1
    var speaker = true
    var fake: Fake? = null

    // ecran negru comandat de regie
    var blackoutOn = false
    var blackoutText = ""

    val msgs = ArrayList<Msg>()
    val contacts = ArrayList<Pair<String, String>>()
    val log = ArrayList<String>()
    var peers: List<PeerInfo> = emptyList()
    var optListen = true
    var optClick = true
    var optPeerMsg = true
    val found = LinkedHashMap<String, Found>()

    // scenariu
    val script = ArrayList<Step>()
    var cursor = 0
    var playing = false
    private var nextStep = 0

    var onEvent: ((String) -> Unit)? = null

    fun notify(k: String) {
        onEvent?.invoke(k)
    }

    private fun prefs() = ctx.getSharedPreferences("cuplaj", Context.MODE_PRIVATE)

    fun init(c: Context) {
        if (inited) return
        ctx = c.applicationContext
        inited = true
        L.load(ctx)
        val p = prefs()
        deviceId = p.getString("did", null) ?: UUID.randomUUID().toString().also { p.edit().putString("did", it).apply() }
        myName = p.getString("name", "") ?: ""
        role = p.getString("role", "spectator") ?: "spectator"
        hubHost = p.getString("host", "") ?: ""
        era = p.getInt("era", 1995)
        room = p.getString("room", "") ?: ""
        textScale = p.getFloat("scale", 1f)
        L.lang = p.getString("lang", null) ?: L.detect()
        hubPin = p.getString("hpin", null) ?: (1000 + java.util.Random().nextInt(9000)).toString().also { p.edit().putString("hpin", it).apply() }
        apin = p.getString("apin", "") ?: ""
        val cs = p.getString("contacts", "") ?: ""
        for (l in cs.split("\n")) {
            val i = l.indexOf(';')
            if (i > 0) contacts.add(Pair(l.substring(0, i), l.substring(i + 1)))
        }
        loadScript(p.getString("script", "") ?: "")
    }

    fun saveProfile() {
        prefs().edit().putString("name", myName).putString("role", role).putString("host", hubHost)
            .putInt("era", era).putString("apin", apin).putString("room", room).apply()
    }

    fun setLang(code: String) {
        L.lang = code
        prefs().edit().putString("lang", code).apply()
        notify("lang")
    }

    fun setScale(s: Float) {
        textScale = s
        prefs().edit().putFloat("scale", s).apply()
        notify("lang")
    }

    private fun saveContacts() {
        prefs().edit().putString("contacts", contacts.joinToString("\n") { it.first + ";" + it.second }).apply()
    }

    fun hasMic() = ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun resetHubMemory() {
        prefs().edit().remove("hubstate").apply()
    }

    // ------------------------------------------------------------------ descoperire hub

    @Volatile
    private var discRun = false
    private var mlock: WifiManager.MulticastLock? = null

    fun startDiscovery() {
        if (discRun) return
        discRun = true
        try {
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val l = wm.createMulticastLock("cuplaj")
            l.setReferenceCounted(false)
            l.acquire()
            mlock = l
        } catch (_: Exception) {
        }
        Thread {
            var ds: DatagramSocket? = null
            try {
                val d = DatagramSocket(null)
                ds = d
                d.reuseAddress = true
                d.bind(InetSocketAddress(BEACON_PORT))
                d.soTimeout = 1000
                val buf = ByteArray(256)
                while (discRun) {
                    try {
                        val pk = DatagramPacket(buf, buf.size)
                        d.receive(pk)
                        val s = String(pk.data, 0, pk.length, Charsets.UTF_8).split("|")
                        val h = pk.address.hostAddress
                        if (s.size >= 3 && s[0] == "CUPLAJ1" && h != null) {
                            val f = Found(s[1], h, s.size >= 4 && s[3] == "1")
                            ui.post { if (found.put(h, f) == null) notify("found") }
                        }
                    } catch (_: SocketTimeoutException) {
                    }
                }
            } catch (_: Exception) {
            } finally {
                ds?.close()
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun stopDiscovery() {
        discRun = false
        try {
            mlock?.release()
        } catch (_: Exception) {
        }
        mlock = null
    }

    fun localIps(): List<String> {
        val out = ArrayList<String>()
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en != null && en.hasMoreElements()) {
                val ni = en.nextElement()
                if (!ni.isUp || ni.isLoopback) continue
                for (a in ni.inetAddresses) {
                    if (a is Inet4Address && !a.isLoopbackAddress) out.add(a.hostAddress ?: "")
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    // ------------------------------------------------------------------ sesiune

    fun start(c: Context, r: String, name: String, host: String, pin: String, rm: String, e: Int) {
        init(c)
        stopSession()
        role = r
        myName = name
        hubHost = host
        apin = pin
        room = rm
        era = e
        saveProfile()
        stopDiscovery()
        resetPhone()
        msgs.clear()
        log.clear()
        peers = emptyList()
        val target: String
        if (r == "regie") {
            val p = prefs()
            val hb = Hub(name, hubPin, rm, PORT, p.getString("hubstate", null))
            hb.onPersist = { s -> p.edit().putString("hubstate", s).apply() }
            hubObj = hb
            hb.start()
            ui.postDelayed({
                if (hubObj?.error != null) notify("toast:" + L.t("err_port"))
            }, 1000)
            target = "127.0.0.1"
        } else {
            target = host
        }
        curTarget = target
        playout.start()
        applyAudioMode()
        val n = Net(
            { m -> ui.post { handle(m) } },
            { src, d -> playout.feed(src, d, 1) },
            { s -> ui.post { connected = false; status = s; if (ph != "idle") resetPhone(); notify("state") } }
        )
        net = n
        val tok = prefs().getString("tok_$target", "") ?: ""
        n.start(target, jo("t" to "hello", "id" to deviceId, "name" to name, "role" to r, "pin" to pin, "room" to rm, "tok" to tok))
        startService()
    }

    fun startService() {
        try {
            val i = Intent(ctx, CoreService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        } catch (_: Exception) {
        }
    }

    fun stopSession() {
        ui.removeCallbacks(statTick)
        stopScript()
        net?.stop()
        net = null
        hubObj?.stop()
        hubObj = null
        resetPhone()
        playout.stop()
        connected = false
        status = ""
        number = ""
        line = 0
        slot = 0
        blackoutOn = false
        try {
            ctx.stopService(Intent(ctx, CoreService::class.java))
        } catch (_: Exception) {
        }
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = false
            am.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {
        }
    }

    fun send(o: JSONObject) {
        net?.send(o)
    }

    @Suppress("DEPRECATION")
    fun applyAudioMode() {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            am.isSpeakerphoneOn = speaker
        } catch (_: Exception) {
        }
    }

    fun changeSpeaker(on: Boolean) {
        speaker = on
        applyAudioMode()
        notify("state")
    }

    fun changeEra(e: Int) {
        era = e
        saveProfile()
        notify("era")
    }

    private val statTick = object : Runnable {
        override fun run() {
            sendStat()
            ui.postDelayed(this, 60000)
        }
    }

    private fun sendStat() {
        try {
            val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val lvl = i?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val sc = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val pct = if (lvl >= 0 && sc > 0) lvl * 100 / sc else -1
            send(jo("t" to "stat", "bat" to pct))
        } catch (_: Exception) {
        }
    }

    // ------------------------------------------------------------------ mesaje de la hub

    private fun handle(m: JSONObject) {
        when (m.optString("t")) {
            "welcome" -> {
                number = m.optString("number")
                myGroup = m.optString("group")
                connected = true
                status = L.t("connected_to", m.optString("hub"))
                val tk = m.optString("tok")
                if (tk.isNotEmpty()) prefs().edit().putString("tok_$curTarget", tk).apply()
                ui.removeCallbacks(statTick)
                ui.postDelayed(statTick, 1500)
                notify("state")
            }
            "denied" -> {
                val why = m.optString("why")
                val key = "denied_$why"
                status = if (L.t(key) == key) L.t("denied_other") else L.t(key)
                net?.stop()
                connected = false
                notify("denied")
            }
            "line" -> {
                line = m.optInt("line")
                slot = m.optInt("slot")
                partnerNum = m.optString("partner")
                notify("state")
            }
            "peers" -> {
                val arr = m.optJSONArray("list")
                val l = ArrayList<PeerInfo>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        l.add(
                            PeerInfo(
                                o.optString("id"), o.optString("name"), o.optString("role"), o.optString("number"),
                                o.optString("group"), o.optInt("line"), o.optInt("slot"), o.optBoolean("online"),
                                o.optBoolean("busy"), o.optBoolean("mon"), o.optBoolean("muted"), o.optInt("bat", -1)
                            )
                        )
                    }
                }
                peers = l
                optListen = m.optBoolean("listen", true)
                optClick = m.optBoolean("click", true)
                optPeerMsg = m.optBoolean("peerMsg", true)
                notify("peers")
            }
            "log" -> {
                log.add(m.optString("line"))
                while (log.size > 500) log.removeAt(0)
                notify("log")
            }
            "msg" -> {
                msgs.add(Msg(m.optString("from"), m.optString("num"), m.optString("text"), false, m.optString("to")))
                playout.effect(Fx.beep(1000, 90))
                vibrate(200)
                notify("msg")
            }
            "cue" -> cue(m)
            "dialtone" -> if (dialed.isEmpty()) enter("dialtone")
            "ringback" -> enter("ringback")
            "busy" -> enter("busy")
            "unknown" -> enter("unknown")
            "listening" -> {
                intervening = false
                enter("listening")
            }
            "ring" -> if (ph == "idle" || ph == "busy" || ph == "unknown") {
                ringNum = m.optString("num")
                ringName = m.optString("name")
                ringParty = m.optBoolean("party")
                fake = null
                startRinging(m.optInt("pat"))
            }
            "stopring" -> if (ph == "ringing" && fake == null) enter("idle")
            "connected" -> {
                peerNumber = m.optString("peer")
                peerName = m.optString("name")
                forNum = m.optString("forNum")
                dialed.setLength(0)
                intervening = false
                enter("talking")
            }
            "ended" -> {
                playout.clearVoice()
                dialed.setLength(0)
                intervening = false
                val why = m.optString("why")
                if ((why == "rejected" || why == "noanswer") && ph == "ringback") enter("busy") else enter("idle")
            }
            "click" -> playout.effect(Fx.click(0.35))
        }
    }

    private fun startRinging(pat: Int) {
        enter("ringing")
        ringer.start(era, pat)
        if (pat == 1) vibratePattern(longArrayOf(0, 400, 200, 400, 4000), 0)
        else vibratePattern(longArrayOf(0, 1300, 3700), 0)
        val who = if (ringParty || era < 1990) L.t("ring_plain") else (ringName.ifEmpty { fmtNum(ringNum) })
        Notif.showRing(ctx, L.t("notif_call"), who, L.t("answer"), L.t("reject"))
    }

    private val fakeTimeout = Runnable {
        if (ph == "ringing" && fake != null) {
            fake = null
            enter("idle")
        }
    }

    private fun cue(m: JSONObject) {
        val text = m.optString("text")
        when (m.optString("kind")) {
            "text" -> {
                msgs.add(Msg(L.t("role_regie"), m.optString("from"), text, false, "all"))
                vibrate(500)
                notify("msg")
                notify("bigtext:$text")
            }
            "vibrate" -> vibrate(text.toLongOrNull() ?: 800L)
            "ring" -> {
                ringer.start(era, 0)
                vibratePattern(longArrayOf(0, 1300, 3700), 0)
                ui.postDelayed({
                    if (ph != "ringing") {
                        ringer.stop()
                        vibCancel()
                    }
                }, 8000)
            }
            "flash" -> torch(text.toLongOrNull() ?: 1500L)
            "speaker" -> changeSpeaker(text == "1")
            "era" -> {
                val e = text.toIntOrNull()
                if (e != null) changeEra(e)
            }
            "contact" -> {
                val i = text.indexOf(';')
                if (i > 0) addContact(text.substring(0, i).trim(), text.substring(i + 1).filter { it.isDigit() })
            }
            "say" -> Voice.speak(ctx, text, L.lang)
            "sound" -> playout.effect(Fx.preset(text.trim()))
            "blackout" -> {
                val i = text.indexOf(';')
                blackoutOn = (if (i >= 0) text.substring(0, i) else text).trim() == "1"
                blackoutText = if (i >= 0) text.substring(i + 1) else ""
                notify("blackout")
            }
            "fakecall" -> {
                if (ph != "idle") return
                val p = text.split(";")
                val nm = p.getOrElse(0) { "?" }.trim()
                val num = p.getOrElse(1) { "" }.filter { it.isDigit() }
                val say = p.drop(2).joinToString(";").trim()
                fake = Fake(nm, num, say)
                ringNum = num
                ringName = nm
                ringParty = false
                startRinging(0)
                ui.removeCallbacks(fakeTimeout)
                ui.postDelayed(fakeTimeout, 30000)
            }
        }
    }

    // ------------------------------------------------------------------ telefon

    private val autoDial = Runnable { if (ph == "dialing") placeCall() }

    fun enter(s: String) {
        ph = s
        playout.tone = when (s) {
            "dialtone" -> Tones.DIAL
            "ringback" -> Tones.RINGBACK
            "busy" -> Tones.BUSY
            "unknown" -> Tones.UNKNOWN
            else -> null
        }
        if (s != "ringing") {
            ringer.stop()
            vibCancel()
            Notif.cancel(ctx)
        }
        if (s == "idle") {
            fake = null
            Voice.stop()
        }
        updateMic()
        notify("phone")
    }

    fun resetPhone() {
        ui.removeCallbacks(autoDial)
        ui.removeCallbacks(fakeTimeout)
        ph = "idle"
        fake = null
        dialed.setLength(0)
        intervening = false
        pttMode = -1
        playout.tone = null
        ringer.stop()
        vibCancel()
        Voice.stop()
        if (inited) Notif.cancel(ctx)
        mic.stop()
        playout.clearVoice()
    }

    private fun updateMic() {
        val want = when {
            pttMode > 0 -> pttMode
            fake == null && (ph == "talking" || (ph == "listening" && intervening)) -> 0
            else -> -1
        }
        if (want < 0) {
            mic.stop()
        } else {
            micMode = want
            if (hasMic()) mic.start() else notify("toast:" + L.t("err_mic"))
        }
    }

    fun ptt(mode: Int, down: Boolean) {
        pttMode = if (down) mode else -1
        updateMic()
        notify("ptt")
    }

    /** Receptorul: răspunde dacă sună, ridică dacă e închis, altfel închide. */
    fun lift() {
        val f = fake
        if (ph == "ringing" && f != null) {
            ui.removeCallbacks(fakeTimeout)
            send(jo("t" to "offhook"))
            peerNumber = f.num
            peerName = f.name
            forNum = ""
            enter("talking")
            if (f.text.isNotEmpty()) Voice.speak(ctx, f.text, L.lang)
            return
        }
        if (!connected) {
            notify("toast:" + L.t("err_conn"))
            return
        }
        when (ph) {
            "ringing" -> send(jo("t" to "answer"))
            "idle" -> send(jo("t" to "offhook"))
            else -> hang()
        }
    }

    fun hang() {
        ui.removeCallbacks(autoDial)
        ui.removeCallbacks(fakeTimeout)
        send(jo("t" to "onhook"))
        dialed.setLength(0)
        intervening = false
        playout.clearVoice()
        enter("idle")
    }

    fun digit(ch: Char) {
        val ok = ph == "dialtone" || ph == "dialing" || (era >= 2000 && ph == "idle")
        if (!ok || dialed.length >= 12) return
        if (ph == "dialtone") {
            playout.tone = null
            ph = "dialing"
        }
        dialed.append(ch)
        if (era in 1990..1999) playout.effect(Fx.dtmf(ch))
        else if (era >= 2000) playout.effect(Fx.beep(800, 40))
        ui.removeCallbacks(autoDial)
        if (era < 2000) ui.postDelayed(autoDial, 3500)
        notify("phone")
    }

    fun backspace() {
        if (dialed.isNotEmpty() && (ph == "dialing" || ph == "idle")) {
            dialed.setLength(dialed.length - 1)
            notify("phone")
        }
    }

    fun placeCall() {
        ui.removeCallbacks(autoDial)
        val n = dialed.toString()
        if (n.isEmpty()) return
        if (!connected) {
            notify("toast:" + L.t("err_conn"))
            return
        }
        if (ph == "idle") send(jo("t" to "offhook"))
        else if (ph != "dialtone" && ph != "dialing") return
        send(jo("t" to "dial", "number" to n))
        playout.tone = null
        ph = "dialing"
        notify("phone")
    }

    fun callNumber(n: String) {
        if (ph == "idle" || ph == "dialtone" || ph == "dialing") {
            dialed.setLength(0)
            dialed.append(n)
            placeCall()
        } else {
            notify("toast:" + L.t("err_busy_line"))
        }
    }

    fun toggleIntervene() {
        intervening = !intervening
        send(jo("t" to "intervene", "on" to intervening))
        updateMic()
        notify("phone")
    }

    // ------------------------------------------------------------------ mesaje, contacte, regie

    fun sendMsg(to: String, text: String) {
        if (text.isBlank()) return
        send(jo("t" to "msg", "to" to to, "text" to text))
        msgs.add(Msg(myName, number, text, true, to))
        notify("msg")
    }

    fun addContact(n: String, num: String) {
        if (n.isBlank() || num.isBlank()) return
        contacts.removeAll { it.second == num }
        contacts.add(Pair(n, num))
        saveContacts()
        notify("contacts")
    }

    fun sendCue(to: String, kind: String, text: String) {
        send(jo("t" to "cue", "to" to to, "kind" to kind, "text" to text))
    }

    // ------------------------------------------------------------------ scenariu

    private fun saveScript() {
        prefs().edit().putString("script", scriptJson()).apply()
    }

    fun scriptJson(): String {
        val a = JSONArray()
        for (s in script) a.put(jo("name" to s.name, "to" to s.to, "kind" to s.kind, "text" to s.text, "delay" to s.delay))
        return a.toString(1)
    }

    fun loadScript(s: String): Boolean {
        return try {
            val a = JSONArray(s)
            val l = ArrayList<Step>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                l.add(Step(o.optString("name"), o.optString("to", "all"), o.optString("kind", "text"), o.optString("text"), o.optInt("delay")))
            }
            script.clear()
            script.addAll(l)
            cursor = 0
            true
        } catch (_: Exception) {
            false
        }
    }

    fun importScript(s: String): Boolean {
        val ok = loadScript(s)
        if (ok) {
            saveScript()
            notify("script")
        }
        return ok
    }

    fun addStep(s: Step) {
        script.add(s)
        saveScript()
        notify("script")
    }

    fun removeStep(i: Int) {
        if (i in script.indices) {
            script.removeAt(i)
            if (cursor > script.size) cursor = script.size
            saveScript()
            notify("script")
        }
    }

    fun moveStep(i: Int, d: Int) {
        val j = i + d
        if (i in script.indices && j in script.indices) {
            val t = script[i]
            script[i] = script[j]
            script[j] = t
            saveScript()
            notify("script")
        }
    }

    fun runStep(s: Step) {
        if (s.kind == "msg") {
            sendMsg(s.to, s.text)
        } else {
            sendCue(s.to, s.kind, s.text)
            if (s.kind == "era" && s.to == "all") s.text.toIntOrNull()?.let { changeEra(it) }
        }
    }

    fun goNext() {
        if (cursor in script.indices) {
            runStep(script[cursor])
            cursor++
            notify("script")
        }
    }

    private val playRun = Runnable { stepTimer(nextStep) }

    fun playFrom(i: Int) {
        if (i !in script.indices) return
        playing = true
        stepTimer(i)
    }

    private fun stepTimer(i: Int) {
        if (!playing || i !in script.indices) {
            playing = false
            notify("script")
            return
        }
        runStep(script[i])
        cursor = i + 1
        notify("script")
        if (cursor >= script.size) {
            playing = false
            notify("script")
            return
        }
        nextStep = cursor
        ui.postDelayed(playRun, script[cursor].delay * 1000L)
    }

    fun stopScript() {
        playing = false
        ui.removeCallbacks(playRun)
        notify("script")
    }

    // ------------------------------------------------------------------ vibrații, bliț

    @Suppress("DEPRECATION")
    private fun vib(): Vibrator? = try {
        ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    } catch (_: Exception) {
        null
    }

    @Suppress("DEPRECATION")
    fun vibrate(ms: Long) {
        val v = vib() ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else v.vibrate(ms)
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    fun vibratePattern(p: LongArray, repeat: Int) {
        val v = vib() ?: return
        try {
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(p, repeat))
            else v.vibrate(p, repeat)
        } catch (_: Exception) {
        }
    }

    fun vibCancel() {
        try {
            vib()?.cancel()
        } catch (_: Exception) {
        }
    }

    fun torch(ms: Long) {
        try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var id: String? = null
            for (c in cm.cameraIdList) {
                if (cm.getCameraCharacteristics(c).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                    id = c
                    break
                }
            }
            val cid = id ?: return
            cm.setTorchMode(cid, true)
            ui.postDelayed({
                try {
                    cm.setTorchMode(cid, false)
                } catch (_: Exception) {
                }
            }, ms.coerceIn(100L, 30000L))
        } catch (_: Exception) {
        }
    }
}
