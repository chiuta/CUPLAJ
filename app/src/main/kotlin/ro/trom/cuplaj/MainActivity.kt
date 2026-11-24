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
    }

    override fun onRequestPermissionsResult(code: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(code, p, r)
        // serviciul pornit fără microfon nu are voie să capteze; îl repornim după acordare
        if (App.net != null && App.hasMic()) App.startService()
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
            k.startsWith("toast:") -> toast(k.substring(6))
            k.startsWith("bigtext:") -> bigText(k.substring(8))
            k == "denied" -> {
                toast(App.status)
                App.stopSession()
                showStart()
            }
            k == "blackout" -> showBlackout()
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
            col.addView(btn(L.t("btn_refresh")) { saveFields(); showStart() })
        } else {
            col.addView(tv(L.t("hubs_found"), 13f, DIM), lp().apply { topMargin = dp(16) })
            if (App.found.isEmpty()) col.addView(tv(L.t("hubs_none"), 14f, DIM))
            for (f in App.found.values) {
                col.addView(btn((if (f.locked) "🔒 " else "") + f.name + " — " + f.host) {
                    saveFields()
                    App.hubHost = f.host
                    showStart()
                })
            }
            val host = edit(L.t("host_hint"), App.hubHost)
            hostEt = host
            col.addView(host, lp().apply { topMargin = dp(8) })
            col.addView(room)
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
                } else if (App.role != "regie" && App.hubHost.isBlank()) {
                    toast(L.t("err_host"))
                } else {
                    App.start(this, App.role, n, App.hubHost.trim(), App.apin.trim(), App.room, App.era)
                    showMain()
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
        col.addView(btn(L.t("set_about")) { dlg?.dismiss(); aboutDialog() })
        dlg = AlertDialog.Builder(this).setView(sv).setPositiveButton(L.t("ok"), null).create()
        dlg.show()
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
        header?.text = App.myName + " · " + roleLabel(App.role) + " · " + s
    }
}
