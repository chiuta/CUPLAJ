package ro.trom.cuplaj

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private var screen = "start"
    private var tabs: List<Tab> = emptyList()
    private var tabBtns: List<Button> = emptyList()
    private var header: TextView? = null
    private var content: FrameLayout? = null
    private var nameEt: EditText? = null
    private var hostEt: EditText? = null
    private var pinEt: EditText? = null
    private var roomEt: EditText? = null
    private var keyEt: EditText? = null
    private var logView: TextView? = null
    private var overlay: FrameLayout? = null
    private var startScroll: ScrollView? = null

    @Suppress("DEPRECATION")
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        App.init(this)
        volumeControlStream = AudioManager.STREAM_VOICE_CALL
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        root = FrameLayout(this)
        root.setBackgroundColor(BG)
        setContentView(root)
        val perms = ArrayList<String>()
        perms.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        requestPermissions(perms.toTypedArray(), 1)
        if (App.net != null) showMain() else showStart()
        handleInvite(intent)
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(code, p, r)
        // serviciul pornit fără microfon nu are voie să capteze; îl repornim după acordare
        if (App.net != null && App.hasMic()) App.startService()
        if (code == 6) { AirLink.applyRx(); return }
        if (code == 5) {
            if (App.role == "regie" && App.net != null) App.openLinks()
            else if (screen == "start" && App.role != "regie") App.scanAll(App.autoJoin)
        }
    }

    /** O singură cerere pentru toate permisiunile legăturilor (Bluetooth, dispozitive din apropiere). */
    private fun ensureLinkPerms(): Boolean {
        val need = (Bt.permissions().toList() + Wfd.permissions().toList()).distinct()
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (need.isEmpty()) return true
        requestPermissions(need.toTypedArray(), 5)
        return false
    }

    private fun connectNow() {
        saveFields()
        if (App.myName.trim().isEmpty()) App.myName = Build.MODEL ?: "CUPLAJ"
        val n = App.myName.trim()
        if (App.role != "regie" && App.hubHost.isBlank()) {
            toast(L.t("err_host"))
        } else {
            App.start(this, App.role, n, App.hubHost.trim(), App.apin.trim(), App.room, App.era)
            showMain()
        }
    }

    /** Link de invitație: configurează tot și se alătură (sau începe căutarea dacă nu are adrese). */
    private fun handleInvite(i: Intent?) {
        val inv = InviteLink.parse(i?.dataString) ?: return
        i?.data = null
        // un link poate veni de pe orice pagină web: fără confirmare nu se schimbă nimic și nu se conectează nimic
        val what = (if (inv.hub.isNotEmpty()) inv.hub else L.t("invite_unknown_hub")) +
            (if (inv.hosts.isNotEmpty()) "\n" + inv.hosts.joinToString(", ") else "") +
            "\n" + roleLabel(inv.role)
        AlertDialog.Builder(this).setTitle(L.t("invite_confirm")).setMessage(what)
            .setPositiveButton(L.t("invite_join")) { _, _ -> applyInvite(inv) }
            .setNegativeButton(L.t("cancel"), null).show()
    }

    private fun applyInvite(inv: Invite) {
        App.role = inv.role
        App.joinKey = inv.key
        App.room = inv.room
        App.apin = inv.pin
        App.saveNet()
        App.saveProfile()
        toast(L.t("invite_applied", inv.hub))
        if (inv.hosts.isEmpty()) {
            showStart()
            ensureLinkPerms()
            App.scanAll(true)
            return
        }
        Thread {
            var pick = inv.hosts[0]
            for (h in inv.hosts) {
                try {
                    val (hh, pp) = parseHostPort(h)
                    val s = java.net.Socket()
                    s.connect(java.net.InetSocketAddress(hh, pp), 1500)
                    s.close()
                    pick = h
                    break
                } catch (_: Exception) {
                }
            }
            runOnUiThread {
                App.hubHost = pick
                connectNow()
            }
        }.start()
    }

    override fun onNewIntent(i: Intent?) {
        super.onNewIntent(i)
        handleInvite(i)
    }

    override fun onResume() {
        super.onResume()
        App.onEvent = { k -> onEvent(k) }
        if (screen == "start") App.startDiscovery()
        if (screen == "main") {
            onEvent("state")
            showBlackout()
        }
    }

    override fun onPause() {
        super.onPause()
        App.onEvent = null
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (screen == "main") moveTaskToBack(true) else super.onBackPressed()
    }

    fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    fun shareText(s: String) {
        try {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, s)
            startActivity(Intent.createChooser(i, null))
        } catch (_: Exception) {
        }
    }

    private fun onEvent(k: String) {
        when {
            k == "found" -> if (screen == "start") {
                saveFields()
                showStart()
            }
            k == "lang" -> {
                saveFields()
                if (screen == "main") showMain() else showStart()
            }
            k.startsWith("autojoin:") -> if (screen == "start") {
                toast(L.t("auto_found"))
                App.hubHost = k.substring(9)
                connectNow()
            }
            k.startsWith("airinvite:") -> {
                val q = k.substring(10).split("|")
                if (q.size == 2) AlertDialog.Builder(this).setMessage(L.t("air_invite_q", q[0]))
                    .setPositiveButton(L.t("invite_join")) { _, _ ->
                        App.hubHost = q[0]; App.apin = q[1]; App.saveNet(); App.saveProfile()
                        if (screen != "main") connectNow()
                    }.setNegativeButton(L.t("cancel"), null).show()
            }
            k.startsWith("toast:") -> toast(k.substring(6))
            k.startsWith("bigtext:") -> bigText(k.substring(8))
            k == "denied" -> {
                toast(App.status)
                App.stopSession()
                showStart()
            }
            k == "blackout" -> showBlackout()
            k == "color" -> showColor()
            k == "media" -> showMedia()
            k == "caption" -> showCaption()
            k == "meterfinal" -> bigText(App.meterSummary)
            k == "poll" -> showPoll()
            k == "chain" -> chainDialog()
            k == "chainfinal" -> bigText(App.chainSummary)
            k == "report" -> shareText(App.reportMd)
            k == "pollfinal" -> bigText(App.pollSummary)
            k == "log" -> logView?.let { v -> v.text = App.log.joinToString("\n") }
            screen == "main" -> {
                updateHeader()
                for (t in tabs) t.update(k)
            }
        }
    }

    private fun bigText(s: String) {
        val d = AlertDialog.Builder(this).setMessage(s).setPositiveButton(L.t("ok"), null).create()
        d.show()
        d.findViewById<TextView>(android.R.id.message)?.textSize = 24f * App.textScale
    }

    /** Ecran negru comandat de regie: acoperă totul, lasă să treacă doar un text discret. */
    private fun showBlackout() {
        overlay?.let { root.removeView(it) }
        overlay = null
        if (!App.blackoutOn || screen != "main") return
        val o = FrameLayout(this)
        o.setBackgroundColor(Color.BLACK)
        o.isClickable = true
        if (App.blackoutText.isNotEmpty()) {
            val t = tv(App.blackoutText, 20f, Color.parseColor("#777777"))
            t.gravity = Gravity.CENTER
            o.addView(t, FrameLayout.LayoutParams(MATCH, MATCH).apply { setMargins(dp(24), dp(24), dp(24), dp(24)) })
        }
        root.addView(o, FrameLayout.LayoutParams(MATCH, MATCH))
        overlay = o
    }

    private var colorOv: View? = null
    private var blinkVisible = true
    private val blinkRun = object : Runnable {
        override fun run() {
            val cp = App.colorPlanNow ?: return
            blinkVisible = !blinkVisible
            colorOv?.visibility = if (blinkVisible) View.VISIBLE else View.INVISIBLE
            App.ui.postDelayed(this, (500.0 / cp.hz).toLong())
        }
    }

    /** Cue de culoare: tot ecranul într-o culoare (pulsează cel mult 2 Hz); apăsare lungă = închide. */
    private fun showColor() {
        colorOv?.let { root.removeView(it) }
        colorOv = null
        App.ui.removeCallbacks(blinkRun)
        val cp = App.colorPlanNow ?: return
        if (screen != "main") return
        val o = View(this)
        o.setBackgroundColor(cp.rgb)
        o.isClickable = true
        o.setOnLongClickListener { App.colorPlanNow = null; showColor(); true }
        root.addView(o, FrameLayout.LayoutParams(MATCH, MATCH))
        colorOv = o
        blinkVisible = true
        if (cp.hz > 0) App.ui.postDelayed(blinkRun, (500.0 / cp.hz).toLong())
    }

    private var mediaOv: View? = null
    private var capView: TextView? = null

    /** Subtitrare mare în josul ecranului (în limba telefonului dacă textul are variante `ro=…|en=…`). */
    private fun showCaption() {
        capView?.let { root.removeView(it) }
        capView = null
        if (App.captionText.isEmpty() || screen != "main") return
        val t = tv(App.captionText, 26f * App.textScale, Color.WHITE)
        t.gravity = Gravity.CENTER
        t.setBackgroundColor(Color.parseColor("#CC000000"))
        t.setPadding(dp(16), dp(14), dp(16), dp(14))
        root.addView(t, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM))
        capView = t
    }

    private fun rulesDialog() {
        val sv = ScrollView(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(12))
        sv.addView(col)
        col.addView(tv(L.t("rules_hint"), 12f, DIM))
        val et = EditText(this)
        et.setText(App.rulesText)
        et.setTextColor(Color.WHITE)
        et.hint = "zone 3 once > self | text | Bun venit, {name}"
        et.setHintTextColor(DIM)
        et.minLines = 6
        et.gravity = Gravity.TOP
        col.addView(et)
        AlertDialog.Builder(this).setView(sv).setPositiveButton(L.t("ok")) { _, _ ->
            App.rulesText = et.text.toString()
            App.saveRules()
        }.show()
    }

    /** Imagine sau video primite de la regie, pe tot ecranul; apăsare lungă = închide. */
    private fun showMedia() {
        mediaOv?.let { root.removeView(it) }
        mediaOv = null
        val t = App.mediaShow ?: return
        if (screen != "main") return
        val v: View
        if (t.first == "video") {
            val vv = android.widget.VideoView(this)
            vv.setVideoPath(t.second)
            vv.setOnPreparedListener { it.isLooping = t.third > 0; vv.start() }
            vv.setOnCompletionListener { if (t.third == 0L) { App.mediaShow = null; showMedia() } }
            v = vv
        } else {
            val iv = android.widget.ImageView(this)
            iv.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            iv.setImageURI(android.net.Uri.fromFile(java.io.File(t.second)))
            v = iv
        }
        val f = FrameLayout(this)
        f.setBackgroundColor(Color.BLACK)
        f.isClickable = true
        f.setOnLongClickListener { App.mediaShow = null; showMedia(); true }
        v.setOnLongClickListener { App.mediaShow = null; showMedia(); true }
        f.addView(v, FrameLayout.LayoutParams(MATCH, MATCH, Gravity.CENTER))
        root.addView(f, FrameLayout.LayoutParams(MATCH, MATCH))
        mediaOv = f
    }

    private var pickTargets: List<String> = emptyList()

    /** Regia: alege un fișier (imagine, video, audio) și îl trimite țintelor. */
    fun pickMedia(targets: List<String>) {
        pickTargets = targets
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        i.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*", "audio/*"))
        @Suppress("DEPRECATION")
        startActivityForResult(i, 31)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val u = data?.data ?: return
        if (req != 31 || res != RESULT_OK) return
        val mime = contentResolver.getType(u) ?: ""
        val kind = when {
            mime.startsWith("image/") -> "image"
            mime.startsWith("video/") -> "video"
            mime.startsWith("audio/") -> "clip"
            else -> return
        }
        val to = pickTargets
        Thread {
            val bytes = try {
                contentResolver.openInputStream(u)?.use { s ->
                    val o = java.io.ByteArrayOutputStream()
                    val b = ByteArray(65536)
                    while (true) {
                        val n = s.read(b)
                        if (n < 0) break
                        o.write(b, 0, n)
                        if (o.size() > Media.MAX_BYTES) return@use null
                    }
                    o.toByteArray()
                }
            } catch (_: Exception) { null }
            runOnUiThread {
                if (bytes == null) toast(L.t("media_big")) else { toast(L.t("media_sending")); App.pushMedia(kind, bytes, to) }
            }
        }.start()
    }

    private fun oscDialog() {
        val sv = ScrollView(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(12))
        sv.addView(col)
        col.addView(tv(L.t("osc_hint", App.hubPin), 12f, DIM))
        val oh = edit(L.t("osc_out_host"), App.oscOutHost)
        val op = edit(L.t("osc_out_port"), App.oscOutPort.toString())
        op.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        val ip = edit(L.t("osc_in_port"), App.oscInPort.toString())
        ip.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        var inOn = App.oscInOn
        col.addView(oh); col.addView(op)
        val tb = btn(L.t(if (inOn) "osc_in_on" else "osc_in_off"), if (inOn) GREEN else BTN) {}
        tb.setOnClickListener { inOn = !inOn; tb.text = L.t(if (inOn) "osc_in_on" else "osc_in_off") }
        col.addView(tb); col.addView(ip)
        AlertDialog.Builder(this).setView(sv).setPositiveButton(L.t("ok")) { _, _ ->
            App.oscOutHost = oh.text.toString().trim()
            App.oscOutPort = op.text.toString().toIntOrNull()?.coerceIn(1, 65535) ?: 53000
            App.oscInPort = ip.text.toString().toIntOrNull()?.coerceIn(1024, 65535) ?: 8473
            App.oscInOn = inOn
            App.saveOsc()
            toast(L.t("restart_hub"))
        }.show()
    }

    /** „Telefonul fără fir”: spectatorul vede mesajul și îl transmite mai departe, cum l-a înțeles. */
    private fun chainDialog() {
        val m = App.chainCur ?: return
        val e = edit(L.t("chain_text"), m.optString("text"))
        AlertDialog.Builder(this).setTitle(L.t("chain_title", m.optInt("hop"), m.optInt("of"))).setView(e)
            .setCancelable(false)
            .setPositiveButton(L.t("chain_send")) { _, _ -> App.chainForward(e.text.toString()) }.show()
    }

    private fun guessDialog() {
        val e = edit(L.t("guess_hint"), "")
        AlertDialog.Builder(this).setTitle(L.t("guess_btn")).setView(e)
            .setPositiveButton(L.t("ok")) { _, _ -> App.guess(e.text.toString()) }
            .setNegativeButton(L.t("cancel"), null).show()
    }

    private fun dmxDialog() {
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(12))
        col.addView(tv(L.t("dmx_hint"), 12f, DIM))
        val h = edit(L.t("dmx_host"), App.dmxHost)
        var on = App.dmxOn
        val tb = btn(L.t(if (on) "dmx_on" else "dmx_off"), if (on) GREEN else BTN) {}
        tb.setOnClickListener { on = !on; tb.text = L.t(if (on) "dmx_on" else "dmx_off") }
        col.addView(h); col.addView(tb)
        AlertDialog.Builder(this).setView(col).setPositiveButton(L.t("ok")) { _, _ ->
            App.dmxHost = h.text.toString().trim()
            App.dmxOn = on
            App.saveDmx()
        }.show()
    }

    /** Instrumente Regie v1.8: lanț, secret, casetofon‑fantomă, raport. */
    fun toolsDialog(to: List<String>, sel: List<String>) {
        val items = arrayOf(L.t("tool_chain"), L.t("tool_secret"), L.t("tape_rec"), L.t("tape_stop"), L.t("tape_play"), L.t("report_btn"))
        AlertDialog.Builder(this).setTitle(L.t("tools18")).setItems(items) { _, i ->
            when (i) {
                0 -> {
                    val col = LinearLayout(this); col.orientation = LinearLayout.VERTICAL; col.setPadding(dp(16), dp(12), dp(16), dp(12))
                    val t = edit(L.t("chain_text"), ""); val n = edit(L.t("chain_nums"), "")
                    col.addView(t); col.addView(n)
                    AlertDialog.Builder(this).setTitle(L.t("tool_chain")).setView(col).setPositiveButton(L.t("ok")) { _, _ ->
                        App.startChain(t.text.toString(), n.text.toString().split(Regex("[^0-9]+")).filter { it.isNotEmpty() })
                    }.show()
                }
                1 -> {
                    val e = edit(L.t("secret_phrase"), "")
                    AlertDialog.Builder(this).setTitle(L.t("tool_secret")).setMessage(L.t("secret_hint")).setView(e).setPositiveButton(L.t("ok")) { _, _ ->
                        App.startSecret(e.text.toString(), to.firstOrNull() ?: "spectators")
                    }.show()
                }
                2 -> if (sel.isEmpty()) toast(L.t("sel_one")) else App.tape("rec", sel[0])
                3 -> App.tape("stop")
                4 -> App.tape("play", to = to.firstOrNull() ?: "all")
                5 -> App.requestReport()
            }
        }.show()
    }

    /** Sondaj primit de la regie: întrebarea și opțiunile ca butoane. */
    private fun showPoll() {
        val m = App.pollCur ?: return
        val os = m.optJSONArray("opts") ?: return
        val items = Array(os.length()) { os.optString(it) }
        AlertDialog.Builder(this).setTitle(m.optString("q")).setItems(items) { _, i -> App.vote(i) }.show()
    }

    override fun onKeyDown(code: Int, e: KeyEvent): Boolean {
        if (App.hwPtt && App.net != null && screen == "main" && isPttKey(code)) {
            if (e.repeatCount == 0) App.ptt(if (App.role == "regie") 1 else 2, true)
            return true
        }
        return super.onKeyDown(code, e)
    }

    override fun onKeyUp(code: Int, e: KeyEvent): Boolean {
        if (App.hwPtt && App.net != null && screen == "main" && isPttKey(code)) {
            App.ptt(if (App.role == "regie") 1 else 2, false)
            return true
        }
        return super.onKeyUp(code, e)
    }

    private fun isPttKey(c: Int) = c == KeyEvent.KEYCODE_HEADSETHOOK || c == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || c == KeyEvent.KEYCODE_VOLUME_UP

    // ------------------------------------------------------------------ ecran de pornire

    private fun edit(hint: String, text: String): EditText {
        val e = EditText(this)
        e.hint = hint
        e.setText(text)
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(DIM)
        e.isSingleLine = true
        return e
    }

    private fun saveFields() {
        nameEt?.let { App.myName = it.text.toString() }
        hostEt?.let { App.hubHost = it.text.toString() }
        pinEt?.let { App.apin = it.text.toString() }
        roomEt?.let { App.room = it.text.toString().trim() }
        keyEt?.let { App.joinKey = it.text.toString().trim(); App.saveNet() }
    }

    private fun lp() = LinearLayout.LayoutParams(MATCH, WRAP)

    private fun showStart() {
        screen = "start"
        App.startDiscovery()
        root.removeAllViews()
        overlay = null
        val sv = ScrollView(this)
        startScroll = sv
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(20), dp(28), dp(20), dp(24))
        sv.addView(col)
        root.addView(sv, FrameLayout.LayoutParams(MATCH, MATCH))

        val top = LinearLayout(this)
        top.gravity = Gravity.CENTER_VERTICAL
        top.addView(tv("CUPLAJ", 38f, IVORY), LinearLayout.LayoutParams(0, WRAP, 1f))
        top.addView(btn("Aa", BTN) { saveFields(); settingsDialog() })
        col.addView(top)
        col.addView(tv(L.t("tagline"), 14f, DIM))

        val name = edit(L.t("name_hint"), App.myName)
        nameEt = name
        col.addView(name, lp().apply { topMargin = dp(18) })

        col.addView(tv(L.t("role"), 13f, DIM), lp().apply { topMargin = dp(12) })
        val rr = LinearLayout(this)
        for (r in listOf("spectator", "actor", "regie")) {
            rr.addView(
                btn(roleLabel(r), if (App.role == r) ACC else BTN) {
                    saveFields()
                    App.role = r
                    showStart()
                },
                LinearLayout.LayoutParams(0, WRAP, 1f)
            )
        }
        col.addView(rr)

        hostEt = null
        pinEt = null
        keyEt = null
        val room = edit(L.t("room_hint"), App.room)
        roomEt = room
        if (App.role == "regie") {
            col.addView(tv(L.t("regie_explain"), 14f, DIM), lp().apply { topMargin = dp(16) })
            val ips = App.localIps()
            col.addView(
                tv(L.t("addresses", if (ips.isEmpty()) L.t("no_net") else ips.joinToString(", ")), 15f, IVORY),
                lp().apply { topMargin = dp(8) }
            )
            col.addView(tv(L.t("actor_pin", App.hubPin), 20f, IVORY), lp().apply { topMargin = dp(8) })
            col.addView(room, lp().apply { topMargin = dp(8) })
            col.addView(btn(if (App.encOn) L.t("enc_on") else L.t("enc_off"), if (App.encOn) GREEN else BTN) {
                saveFields()
                App.encOn = !App.encOn
                App.saveNet()
                showStart()
            })
            if (App.encOn) {
                col.addView(tv(L.t("enc_key", App.netKey), 18f, IVORY), lp().apply { topMargin = dp(4) })
                col.addView(tv(L.t("enc_help"), 12f, DIM))
            }
            col.addView(tv(L.t("wan_help", PORT, PORT + P2P_PORT_OFFSET), 12f, DIM), lp().apply { topMargin = dp(6) })
            col.addView(tv(L.t("set_links"), 13f, DIM), lp().apply { topMargin = dp(10) })
            col.addView(btn(if (App.autoMode) L.t("auto_on") else L.t("auto_off"), if (App.autoMode) GREEN else BTN) {
                saveFields()
                App.autoMode = !App.autoMode
                App.saveNet()
                showStart()
            })
            if (App.autoMode) {
                col.addView(tv(L.t("auto_help"), 12f, DIM))
            } else {
                col.addView(btn(if (App.btOn) L.t("bt_on") else L.t("bt_off"), if (App.btOn) GREEN else BTN) {
                    saveFields()
                    App.btOn = !App.btOn
                    if (App.btOn && !Bt.hasPerm(this)) {
                        requestPermissions(Bt.permissions(), 3)
                        toast(L.t("perm_nearby"))
                    }
                    App.saveNet()
                    showStart()
                })
                if (App.btOn) {
                    col.addView(btn(L.t("bt_visible")) {
                        try {
                            startActivity(Bt.discoverableIntent())
                        } catch (_: Exception) {
                            toast(L.t("bt_unavailable"))
                        }
                    })
                    col.addView(tv(L.t("bt_help"), 12f, DIM))
                }
                col.addView(btn(if (App.wfdOn) L.t("wfd_on") else L.t("wfd_off"), if (App.wfdOn) GREEN else BTN) {
                    saveFields()
                    App.wfdOn = !App.wfdOn
                    if (App.wfdOn && !Wfd.hasPerm(this)) {
                        requestPermissions(Wfd.permissions(), 4)
                        toast(L.t("perm_nearby"))
                    }
                    App.saveNet()
                    showStart()
                })
                if (App.wfdOn) col.addView(tv(L.t("wfd_help"), 12f, DIM))
            }
            col.addView(btn(L.t("btn_refresh")) { saveFields(); showStart() })
        } else {
            col.addView(btn(L.t("btn_auto_join"), GREEN) {
                saveFields()
                ensureLinkPerms()
                App.scanAll(true)
                toast(L.t("auto_searching"))
            }, lp().apply { topMargin = dp(16) })
            col.addView(tv(L.t("hubs_found"), 13f, DIM), lp().apply { topMargin = dp(8) })
            if (App.found.isEmpty()) col.addView(tv(L.t("hubs_none"), 14f, DIM))
            col.addView(row2(
                btn(L.t("btn_scan_bt")) {
                    if (!Bt.available(this)) toast(L.t("bt_unavailable"))
                    else if (!Bt.hasPerm(this)) {
                        requestPermissions(Bt.permissions(), 3)
                        toast(L.t("perm_nearby"))
                    } else if (!App.scanBluetooth()) toast(L.t("bt_turn_on"))
                },
                btn(L.t("btn_scan_wfd")) {
                    if (!Wfd.available(this)) toast(L.t("wfd_unavailable"))
                    else if (!Wfd.hasPerm(this)) {
                        requestPermissions(Wfd.permissions(), 4)
                        toast(L.t("perm_nearby"))
                    } else if (!App.scanWifiDirect()) toast(L.t("wfd_fail"))
                }
            ))
            for (f in App.found.values) {
                val tag = when (f.via) {
                    "bt" -> "🔵 "
                    "wfd" -> "📡 "
                    "nsd" -> "🌐 "
                    else -> ""
                }
                col.addView(btn(tag + (if (f.locked) "🔒 " else "") + (if (f.keyed) "🔑 " else "") + f.name + " — " + f.host) {
                    saveFields()
                    if (f.host.startsWith("wfd:") && !f.host.contains(".")) {
                        App.joinWifiDirect(f.host.substring(4))
                    } else {
                        App.hubHost = f.host
                        showStart()
                    }
                })
            }
            val host = edit(L.t("host_hint"), App.hubHost)
            hostEt = host
            col.addView(host, lp().apply { topMargin = dp(8) })
            col.addView(room)
            val ke = edit(L.t("key_hint"), App.joinKey)
            keyEt = ke
            col.addView(ke)
            if (App.role == "actor") {
                val pin = edit(L.t("pin_hint"), App.apin)
                pin.inputType = InputType.TYPE_CLASS_NUMBER
                pinEt = pin
                col.addView(pin)
            }
        }

        col.addView(tv(L.t("era_pick"), 13f, DIM), lp().apply { topMargin = dp(14) })
        val er = LinearLayout(this)
        for ((e, key) in listOf(1985 to "era_btn_80", 1995 to "era_btn_90", 2003 to "era_btn_00")) {
            er.addView(
                btn(L.t(key), if (App.era == e) ACC else BTN) {
                    saveFields()
                    App.era = e
                    showStart()
                },
                LinearLayout.LayoutParams(0, WRAP, 1f)
            )
        }
        col.addView(er)

        col.addView(
            btn(if (App.role == "regie") L.t("btn_start_hub") else L.t("btn_connect"), GREEN) {
                saveFields()
                val n = App.myName.trim()
                if (n.isEmpty()) {
                    toast(L.t("err_name"))
                } else {
                    if (App.role == "regie" && App.autoMode) ensureLinkPerms()
                    connectNow()
                }
            },
            lp().apply { topMargin = dp(20) }
        )

        // săgeată de revenire sus
        val up = btn("▲", BTN) { sv.smoothScrollTo(0, 0) }
        up.contentDescription = L.t("scroll_up")
        root.addView(up, FrameLayout.LayoutParams(dp(52), dp(52)).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            setMargins(0, 0, dp(12), dp(12))
        })
    }

    // ------------------------------------------------------------------ setări

    private fun settingsDialog() {
        val sv = ScrollView(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(12))
        sv.addView(col)
        var dlg: AlertDialog? = null
        col.addView(tv(L.t("set_lang"), 13f, DIM))
        for (i in L.codes.indices) {
            col.addView(btn(L.names[i], if (L.lang == L.codes[i]) ACC else BTN) {
                dlg?.dismiss()
                App.setLang(L.codes[i])
            })
        }
        col.addView(tv(L.t("set_scale"), 13f, DIM), lp().apply { topMargin = dp(12) })
        val sr = LinearLayout(this)
        for ((s, label) in listOf(1f to "A", 1.25f to "A+", 1.5f to "A++")) {
            sr.addView(btn(label, if (App.textScale == s) ACC else BTN) {
                dlg?.dismiss()
                App.setScale(s)
            }, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        col.addView(sr)
        col.addView(btn(L.t("set_battery")) { batteryPrompt() }, lp().apply { topMargin = dp(12) })
        col.addView(btn(L.t("set_reset_hub"), RED) {
            AlertDialog.Builder(this).setMessage(L.t("confirm_reset_hub"))
                .setPositiveButton(L.t("yes")) { _, _ -> App.resetHubMemory(); toast(L.t("sent")) }
                .setNegativeButton(L.t("no"), null).show()
        })
        if (App.role == "regie") {
            col.addView(tv(L.t("links_active", App.linksSummary()), 13f, IVORY), lp().apply { topMargin = dp(12) })
            col.addView(btn(L.t("btn_invite_sp")) { dlg?.dismiss(); shareInvite("spectator") })
            col.addView(btn(L.t("btn_invite_ac")) { dlg?.dismiss(); shareInvite("actor") })
        }
        col.addView(btn(L.t("set_air")) { dlg?.dismiss(); airDialog() })
        if (App.role == "regie") col.addView(btn(L.t("set_osc")) { dlg?.dismiss(); oscDialog() })
        if (App.role == "regie") col.addView(btn(L.t("set_dmx")) { dlg?.dismiss(); dmxDialog() })
        if (App.role != "regie") col.addView(btn(L.t("guess_btn")) { dlg?.dismiss(); guessDialog() })
        if (App.role == "regie") col.addView(btn(L.t("set_rules")) { dlg?.dismiss(); rulesDialog() })
        col.addView(btn(L.t(if (App.playout.duckOn) "duck_on" else "duck_off"), if (App.playout.duckOn) GREEN else BTN) {
            App.playout.duckOn = !App.playout.duckOn
            dlg?.dismiss()
            settingsDialog()
        })
        col.addView(btn(L.t(if (App.hwPtt) "hw_ptt_on" else "hw_ptt_off"), if (App.hwPtt) GREEN else BTN) {
            App.hwPtt = !App.hwPtt
            App.saveNet()
            dlg?.dismiss()
            settingsDialog()
        })
        col.addView(tv(L.t("set_gateways"), 13f, DIM), lp().apply { topMargin = dp(12) })
        col.addView(btn(L.t("btn_sip")) { dlg?.dismiss(); sipDialog() })
        col.addView(btn(if (App.smsGw) L.t("sms_gw_on") else L.t("sms_gw_off"), if (App.smsGw) GREEN else BTN) {
            dlg?.dismiss()
            App.smsGw = !App.smsGw
            if (App.smsGw && !Telco.hasSms(this)) {
                requestPermissions(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS), 2)
                toast(L.t("sms_perm"))
            }
            App.saveNet()
            toast(L.t("restart_hub"))
        })
        col.addView(btn(L.t("set_about")) { dlg?.dismiss(); aboutDialog() })
        dlg = AlertDialog.Builder(this).setView(sv).setPositiveButton(L.t("ok"), null).create()
        dlg.show()
    }

    private fun airNeeds(): Array<String> {
        val l = ArrayList<String>()
        if (AirLink.rxLight || AirLink.rxVibe) l.add(Manifest.permission.CAMERA)
        if (AirLink.rxSound) l.add(Manifest.permission.RECORD_AUDIO)
        return l.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }.toTypedArray()
    }

    private fun airApply() {
        val need = airNeeds()
        if (need.isNotEmpty()) { requestPermissions(need, 6); return }
        val bad = AirLink.applyRx()
        if (bad.isNotEmpty()) toast(L.t("air_unavailable", bad.joinToString(",")))
    }

    private fun airDialog() {
        val sv = ScrollView(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(12), dp(16), dp(12))
        sv.addView(col)
        var dlg: AlertDialog? = null
        col.addView(tv(L.t("air_title"), 15f, IVORY))
        col.addView(tv(L.t("air_hint"), 12f, DIM), lp().apply { topMargin = dp(6) })
        fun tog(label: String, on: Boolean, f: () -> Unit) {
            col.addView(btn(label + ": " + L.t(if (on) "air_on" else "air_off"), if (on) GREEN else BTN) { f(); dlg?.dismiss(); airApply(); airDialog() })
        }
        tog(L.t("air_rx_light"), AirLink.rxLight) { AirLink.rxLight = !AirLink.rxLight }
        tog(L.t("air_rx_sound"), AirLink.rxSound) { AirLink.rxSound = !AirLink.rxSound }
        tog(L.t("air_rx_vibe"), AirLink.rxVibe) { AirLink.rxVibe = !AirLink.rxVibe }
        col.addView(btn(L.t("beacon_btn", if (AirLink.beaconZone == 0) L.t("air_off") else AirLink.beaconZone.toString())) {
            AirLink.setBeacon((AirLink.beaconZone + 1) % 10)
            dlg?.dismiss(); airDialog()
        })
        col.addView(btn(L.t(if (AirLink.ultra) "air_ultra" else "air_audible")) { AirLink.ultra = !AirLink.ultra; dlg?.dismiss(); airApply(); airDialog() })
        if (App.role == "regie" && App.net != null) {
            col.addView(tv(L.t("air_send_cue"), 13f, DIM), lp().apply { topMargin = dp(12) })
            for (m in AirLink.MODES) col.addView(btn(L.t("air_$m")) {
                if (AirLink.sendCue(m, "vibrate", "800")) toast(L.t("air_sending")) else toast(L.t("air_unavailable", m))
            })
            col.addView(tv(L.t("air_send_invite"), 13f, DIM), lp().apply { topMargin = dp(12) })
            for (m in listOf("light", "sound")) col.addView(btn(L.t("air_$m")) {
                if (AirLink.sendInvite(m)) toast(L.t("air_sending")) else toast(L.t("air_unavailable", m))
            })
        }
        dlg = AlertDialog.Builder(this).setView(sv).setPositiveButton(L.t("ok"), null).create()
        dlg.show()
    }

    private fun row2(a: View, b: View): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.addView(a, LinearLayout.LayoutParams(0, WRAP, 1f))
        r.addView(b, LinearLayout.LayoutParams(0, WRAP, 1f))
        return r
    }

    private fun shareInvite(role: String) {
        try {
            val i = Intent(Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(Intent.EXTRA_TEXT, App.inviteText(role))
            startActivity(Intent.createChooser(i, null))
        } catch (_: Exception) {
        }
    }

    private fun sipDialog() {
        val sv = ScrollView(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(8), dp(16), dp(8))
        sv.addView(col)
        col.addView(tv(L.t("sip_help"), 13f, DIM))
        val server = edit(L.t("sip_server"), if (App.sipHost.isEmpty()) "" else App.sipHost + ":" + App.sipPort)
        val user = edit(L.t("sip_user"), App.sipUser)
        val pass = edit(L.t("sip_pass"), App.sipPass)
        pass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val domain = edit(L.t("sip_domain"), App.sipDomain)
        val inbound = edit(L.t("sip_inbound"), App.sipInbound)
        for (e in listOf(server, user, pass, domain, inbound)) col.addView(e)
        var on = App.sipOn
        val sw = btn(if (on) L.t("sip_on") else L.t("sip_off"), if (on) GREEN else BTN) {}
        sw.setOnClickListener {
            on = !on
            sw.text = if (on) L.t("sip_on") else L.t("sip_off")
            styleBtn(sw, if (on) GREEN else BTN)
        }
        col.addView(sw)
        AlertDialog.Builder(this).setTitle(L.t("btn_sip")).setView(sv)
            .setPositiveButton(L.t("ok")) { _, _ ->
                val (h, p) = parseHostPort(server.text.toString(), 5060)
                App.sipHost = h
                App.sipPort = p
                App.sipUser = user.text.toString()
                App.sipPass = pass.text.toString()
                App.sipDomain = domain.text.toString()
                App.sipInbound = inbound.text.toString().filter { it.isDigit() }.ifEmpty { "100" }
                App.sipOn = on
                App.saveNet()
                toast(L.t("restart_hub"))
            }
            .setNegativeButton(L.t("cancel"), null).show()
    }

    private fun batteryPrompt() {
        try {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (pm.isIgnoringBatteryOptimizations(packageName)) {
                toast(L.t("battery_ok"))
                return
            }
            val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            i.data = Uri.parse("package:$packageName")
            startActivity(i)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
            }
        }
    }

    private fun aboutDialog() {
        val t = tv(L.t("about_text", VERSION, VERSION_DATE), 14f, Color.WHITE)
        t.setPadding(dp(20), dp(12), dp(20), dp(8))
        val sv = ScrollView(this)
        sv.addView(t)
        AlertDialog.Builder(this).setTitle("CUPLAJ").setView(sv)
            .setPositiveButton(L.t("ok"), null)
            .setNeutralButton("Buy Me a Coffee") { _, _ ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/echo.of.the.strings")))
                } catch (_: Exception) {
                }
            }.show()
    }

    fun showLogDialog() {
        val t = tv(App.log.joinToString("\n"), 12f, Color.WHITE)
        t.typeface = android.graphics.Typeface.MONOSPACE
        t.setPadding(dp(12), dp(8), dp(12), dp(8))
        logView = t
        val sv = ScrollView(this)
        sv.addView(t)
        AlertDialog.Builder(this).setTitle(L.t("btn_journal")).setView(sv)
            .setPositiveButton(L.t("ok"), null)
            .setNeutralButton(L.t("btn_export")) { _, _ -> shareText(App.log.joinToString("\n")) }
            .setOnDismissListener { logView = null }
            .show()
    }

    // ------------------------------------------------------------------ ecranul principal

    private fun showMain() {
        screen = "main"
        App.stopDiscovery()
        root.removeAllViews()
        overlay = null
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        root.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(10), dp(6), dp(6), dp(6))
        top.setBackgroundColor(Color.parseColor("#1E1915"))
        val h = tv("", 13f, IVORY)
        header = h
        top.addView(h, LinearLayout.LayoutParams(0, WRAP, 1f))
        top.addView(btn("Aa", BTN) { settingsDialog() })
        top.addView(btn(L.t("btn_exit"), BTN) {
            AlertDialog.Builder(this).setMessage(L.t("confirm_exit"))
                .setPositiveButton(L.t("yes")) { _, _ ->
                    App.stopSession()
                    showStart()
                }
                .setNegativeButton(L.t("no"), null).show()
        })
        col.addView(top)

        val cf = FrameLayout(this)
        content = cf
        col.addView(cf, LinearLayout.LayoutParams(MATCH, 0, 1f))

        val list = ArrayList<Tab>()
        val names = ArrayList<String>()
        list.add(PhoneTab(this)); names.add(L.t("tab_phone"))
        list.add(MsgTab(this)); names.add(L.t("tab_msgs"))
        list.add(VoiceTab(this)); names.add(L.t("tab_voice"))
        if (App.role == "regie") {
            list.add(RegieTab(this)); names.add(L.t("tab_regie"))
            list.add(ScriptTab(this)); names.add(L.t("tab_script"))
        }
        tabs = list
        val bar = LinearLayout(this)
        val bl = ArrayList<Button>()
        for (i in list.indices) {
            val b = btn(names[i], BTN) { selectTab(i) }
            b.textSize = 13f
            bl.add(b)
            bar.addView(b, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        tabBtns = bl
        col.addView(bar)
        selectTab(0)
        updateHeader()
        showBlackout()
    }

    fun selectTab(i: Int) {
        if (i < 0 || i >= tabs.size) return
        val cf = content ?: return
        cf.removeAllViews()
        cf.addView(tabs[i].view, FrameLayout.LayoutParams(MATCH, MATCH))
        for (j in tabBtns.indices) styleBtn(tabBtns[j], if (j == i) ACC else BTN)
    }

    private fun updateHeader() {
        val s = if (App.connected) L.t("hdr_num", fmtNum(App.number)) else App.status
        header?.text = App.myName + " · " + roleLabel(App.role) + " · " + s + (if (App.p2pOn) " · P2P" else "")
    }
}
