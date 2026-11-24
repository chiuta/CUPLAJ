package ro.trom.cuplaj

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputFilter
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import kotlin.math.min

private fun c(s: String) = Color.parseColor(s)

val IVORY = c("#E8D9B8")
val DIM = c("#A39883")
val ACC = c("#B5452F")
val BTN = c("#3A3F47")
val BG = c("#14110F")
val GREEN = c("#2E7D4F")
val RED = c("#A33A32")

val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

fun fmtNum(n: String): String =
    if (n.length == 6) n.substring(0, 2) + " " + n.substring(2, 4) + " " + n.substring(4) else n

fun roleLabel(r: String) = L.t("role_$r")

abstract class Tab(val a: MainActivity) {
    abstract val view: View
    open fun update(kind: String) {}
}

fun lpw(w: Int = MATCH, h: Int = WRAP, top: Int = 0, a: MainActivity): LinearLayout.LayoutParams {
    val p = LinearLayout.LayoutParams(w, h)
    p.topMargin = a.dp(top)
    return p
}

// ====================================================================== TELEFON

class PhoneTab(a: MainActivity) : Tab(a) {
    private val sv = ScrollView(a)
    private val col = LinearLayout(a)
    override val view: View get() = sv
    private var display: TextView? = null
    private var recBtn: Button? = null
    private var dial: RotaryDialView? = null
    private var inter: Button? = null
    private var agenda: LinearLayout? = null

    init {
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(a.dp(14), a.dp(10), a.dp(14), a.dp(14))
        sv.addView(col)
        build()
    }

    private fun pal(): IntArray = when {
        App.era < 1990 -> intArrayOf(c("#2B1D14"), c("#1B1410"), c("#E8D9B8"), c("#D8C9A3"), c("#2B1D14"))
        App.era < 2000 -> intArrayOf(c("#2A2D33"), c("#16221B"), c("#7CFFB0"), c("#D9D4C5"), c("#2A2D33"))
        else -> intArrayOf(c("#101810"), c("#8FA876"), c("#1E2A14"), c("#3B3F3B"), c("#E6E6E6"))
    }

    private fun build() {
        col.removeAllViews()
        val p = pal()
        sv.setBackgroundColor(p[0])
        val era = App.era
        val title = when {
            era < 1990 -> L.t("era_title_80")
            era < 2000 -> L.t("era_title_90")
            else -> L.t("era_title_00")
        }
        col.addView(a.tv(title, 12f, if (era >= 2000) IVORY else p[2]))
        val d = a.tv("", 22f, p[2])
        d.typeface = Typeface.MONOSPACE
        d.background = a.box(p[1])
        d.setPadding(a.dp(14), a.dp(10), a.dp(14), a.dp(10))
        d.minLines = 3
        d.gravity = Gravity.CENTER_VERTICAL
        display = d
        col.addView(d, lpw(top = 8, a = a))

        dial = null
        recBtn = null
        if (era < 1990) {
            val size = min(a.resources.displayMetrics.widthPixels - a.dp(40), a.dp(340))
            val dv = RotaryDialView(a, { ch -> App.digit(ch) }, { n -> App.playout.effect(Fx.pulses(n)) })
            dial = dv
            col.addView(dv, LinearLayout.LayoutParams(size, size).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = a.dp(12)
            })
        } else {
            col.addView(keypad(p))
        }

        if (era < 2000) {
            val r = a.btn("", GREEN) { App.lift() }
            r.textSize = 18f
            recBtn = r
            col.addView(r, lpw(h = a.dp(64), top = 10, a = a))
        } else {
            val row = LinearLayout(a)
            val call = a.btn(L.t("btn_call"), GREEN) {
                if (App.ph != "ringing" && App.dialed.isNotEmpty()) App.placeCall() else App.lift()
            }
            val end = a.btn(L.t("btn_end"), RED) {
                if (App.ph != "idle") App.hang() else {
                    App.dialed.setLength(0)
                    App.notify("phone")
                }
            }
            val del = a.btn("C", BTN) { App.backspace() }
            row.addView(call, LinearLayout.LayoutParams(0, a.dp(56), 1f).apply { setMargins(a.dp(4), a.dp(8), a.dp(4), 0) })
            row.addView(end, LinearLayout.LayoutParams(0, a.dp(56), 1f).apply { setMargins(a.dp(4), a.dp(8), a.dp(4), 0) })
            row.addView(del, LinearLayout.LayoutParams(0, a.dp(56), 0.5f).apply { setMargins(a.dp(4), a.dp(8), a.dp(4), 0) })
            col.addView(row)
        }

        val ib = a.btn(L.t("btn_inter"), ACC) { App.toggleIntervene() }
        ib.visibility = View.GONE
        inter = ib
        col.addView(ib, lpw(h = a.dp(52), top = 8, a = a))

        val ag = LinearLayout(a)
        ag.orientation = LinearLayout.VERTICAL
        agenda = ag
        col.addView(ag, lpw(top = 14, a = a))
        refreshAgenda()
        refresh()
    }

    private fun keypad(p: IntArray): LinearLayout {
        val g = LinearLayout(a)
        g.orientation = LinearLayout.VERTICAL
        for (row in listOf("123", "456", "789", "*0#")) {
            val r = LinearLayout(a)
            for (ch in row) {
                val b = a.btn(ch.toString(), p[3], p[4]) { App.digit(ch) }
                b.textSize = 24f
                r.addView(b, LinearLayout.LayoutParams(0, a.dp(62), 1f).apply {
                    setMargins(a.dp(4), a.dp(4), a.dp(4), a.dp(4))
                })
            }
            g.addView(r)
        }
        return g
    }

    private fun refreshAgenda() {
        val ag = agenda ?: return
        ag.removeAllViews()
        val p = pal()
        val label = when {
            App.era < 1990 -> L.t("agenda_80")
            App.era < 2000 -> L.t("agenda_90")
            else -> L.t("agenda_00")
        }
        ag.addView(a.tv(label, 13f, if (App.era >= 2000) IVORY else p[2]))
        if (App.contacts.isEmpty()) ag.addView(a.tv(L.t("agenda_empty"), 13f, DIM))
        for (ct in App.contacts) {
            ag.addView(a.btn(ct.first + " · " + fmtNum(ct.second), p[3], p[4]) { App.callNumber(ct.second) })
        }
    }

    private fun nameOf(n: String): String {
        val f = App.contacts.firstOrNull { it.second == n }
        return if (f != null) f.first + " (" + fmtNum(n) + ")" else fmtNum(n)
    }

    private fun statusText(): String {
        val era = App.era
        val dialed = App.dialed.toString()
        return when (App.ph) {
            "idle" -> if (era >= 2000 && dialed.isNotEmpty()) dialed else if (App.connected) L.t("ph_idle") else L.t("ph_noline")
            "dialtone" -> L.t("ph_dialtone")
            "dialing" -> L.t("ph_dialing", dialed)
            "ringback" -> L.t("ph_ringback", dialed)
            "ringing" -> if (era < 1990 || App.ringParty) L.t("ph_ringing_plain") else L.t("ph_ringing", nameOf(App.ringNum))
            "talking" -> L.t("ph_talking") +
                (if (era >= 1990) "\n" + nameOf(App.peerNumber) else "") +
                (if (App.forNum.isNotEmpty()) "\n" + L.t("ph_for", fmtNum(App.forNum)) else "")
            "listening" -> L.t("ph_listening")
            "busy" -> L.t("ph_busy")
            "unknown" -> L.t("ph_unknown")
            else -> ""
        }
    }

    private fun refresh() {
        display?.text = statusText()
        val ph = App.ph
        dial?.active = ph == "dialtone" || ph == "dialing"
        recBtn?.let {
            it.text = when (ph) {
                "ringing" -> L.t("btn_answer_big")
                "idle" -> L.t("btn_lift")
                else -> L.t("btn_hang")
            }
            a.styleBtn(it, if (ph == "idle" || ph == "ringing") GREEN else RED)
        }
        inter?.let {
            it.visibility = if (ph == "listening") View.VISIBLE else View.GONE
            it.text = if (App.intervening) L.t("btn_inter_off") else L.t("btn_inter")
        }
    }

    override fun update(kind: String) {
        when (kind) {
            "era", "lang" -> build()
            "contacts" -> refreshAgenda()
            "phone", "state" -> refresh()
        }
    }
}

// ====================================================================== MESAJE

class MsgTab(a: MainActivity) : Tab(a) {
    private val rootL = LinearLayout(a)
    override val view: View get() = rootL
    private val sv = ScrollView(a)
    private val list = LinearLayout(a)
    private val title = a.tv("", 13f, DIM)
    private val input = EditText(a)
    private val numEt = EditText(a)
    private val sp = Spinner(a)
    private val staff = App.role != "spectator"
    private val keys = if (staff) listOf("tgt_all", "tgt_spectators", "tgt_actors", "tgt_staff", "tgt_number", "tgt_sms") else listOf("tgt_staff_sp", "tgt_number")
    private val opts = keys.map { L.t(it) }

    init {
        rootL.orientation = LinearLayout.VERTICAL
        rootL.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(8))
        rootL.addView(title)
        list.orientation = LinearLayout.VERTICAL
        sv.addView(list)
        rootL.addView(sv, LinearLayout.LayoutParams(MATCH, 0, 1f))

        sp.adapter = ArrayAdapter(a, android.R.layout.simple_spinner_dropdown_item, opts)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                numEt.visibility = if (keys[pos] == "tgt_number" || keys[pos] == "tgt_sms") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        rootL.addView(sp)
        numEt.hint = L.t("num_hint")
        numEt.inputType = InputType.TYPE_CLASS_NUMBER
        numEt.setTextColor(Color.WHITE)
        numEt.setHintTextColor(DIM)
        numEt.visibility = View.GONE
        rootL.addView(numEt)

        val row = LinearLayout(a)
        row.gravity = Gravity.CENTER_VERTICAL
        input.hint = L.t("msg_hint")
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(DIM)
        row.addView(input, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(a.btn(L.t("btn_send"), GREEN) { send() })
        rootL.addView(row)
        applyEra()
        refresh()
    }

    private fun applyEra() {
        title.text = when {
            App.era < 1990 -> L.t("msg_title_80")
            App.era < 2000 -> L.t("msg_title_90")
            else -> L.t("msg_title_00")
        }
        input.filters = arrayOf(InputFilter.LengthFilter(if (App.era >= 2000) 160 else 500))
    }

    private fun target(): String = when (keys[sp.selectedItemPosition]) {
        "tgt_all" -> "all"
        "tgt_spectators" -> "spectators"
        "tgt_actors" -> "actors"
        "tgt_number" -> "num:" + numEt.text.toString().filter { it.isDigit() }
        else -> "staff"
    }

    private fun send() {
        val t = input.text.toString()
        if (t.isBlank()) return
        if (!App.connected) {
            a.toast(L.t("err_conn"))
            return
        }
        if (keys[sp.selectedItemPosition] == "tgt_sms") {
            App.sendSms(numEt.text.toString().trim(), t)
        } else {
            App.sendMsg(target(), t)
        }
        input.setText("")
    }

    private fun refresh() {
        list.removeAllViews()
        for (m in App.msgs) {
            val s = if (m.mine) "→ " + m.text else m.from + " (" + fmtNum(m.num) + "): " + m.text
            val t = a.tv(s, 15f, if (m.mine) IVORY else Color.WHITE)
            t.setPadding(a.dp(8), a.dp(6), a.dp(8), a.dp(6))
            t.background = a.box(if (m.mine) c("#2B2F36") else c("#33281F"))
            list.addView(t, lpw(top = 4, a = a))
        }
        sv.post { sv.fullScroll(View.FOCUS_DOWN) }
    }

    override fun update(kind: String) {
        when (kind) {
            "msg" -> refresh()
            "era" -> applyEra()
        }
    }
}

// ====================================================================== VOCE

class VoiceTab(a: MainActivity) : Tab(a) {
    private val col = LinearLayout(a)
    override val view: View get() = col
    private val state = a.tv("", 16f, IVORY)
    private var spk: Button? = null

    init {
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(a.dp(16), a.dp(14), a.dp(16), a.dp(14))
        col.addView(a.tv(L.t("voice_title"), 16f, IVORY))
        col.addView(a.tv(if (App.role == "spectator") L.t("voice_hint_sp") else L.t("voice_hint_staff"), 13f, DIM))
        if (App.role != "spectator") {
            col.addView(hold(L.t("voice_pa_btn"), 1, ACC), lpw(h = a.dp(110), top = 14, a = a))
        }
        col.addView(
            hold(if (App.role == "spectator") L.t("voice_sp_btn") else L.t("voice_back_btn"), 2, BTN),
            lpw(h = a.dp(110), top = 14, a = a)
        )
        col.addView(state, lpw(top = 14, a = a))
        val s = a.btn("", BTN) { App.changeSpeaker(!App.speaker) }
        spk = s
        col.addView(s, lpw(h = a.dp(52), top = 14, a = a))
        refresh()
    }

    private fun hold(text: String, mode: Int, bg: Int): Button {
        val b = Button(a)
        b.text = text
        b.isAllCaps = false
        b.textSize = 18f
        b.setTextColor(Color.WHITE)
        a.styleBtn(b, bg)
        b.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    App.ptt(mode, true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    App.ptt(mode, false)
                }
            }
            true
        }
        return b
    }

    private fun refresh() {
        state.text = if (App.pttMode > 0) L.t("voice_tx") else if (App.mic.running) L.t("voice_mic") else ""
        spk?.text = if (App.speaker) L.t("spk_on") else L.t("spk_off")
    }

    override fun update(kind: String) {
        if (kind == "ptt" || kind == "phone" || kind == "state") refresh()
    }
}

// ====================================================================== REGIE

val CUE_KINDS = listOf("text", "vibrate", "ring", "flash", "speaker", "contact", "era", "say", "sound", "blackout", "fakecall", "realcall", "panic", "color", "poll", "pollend", "image", "video", "clip", "caption")

/** Țintele pentru indicii: poziție în spinner → șir de țintă. */
fun targetOptions(): List<String> = listOf(L.t("tgt_all"), L.t("tgt_spectators"), L.t("tgt_actors"), L.t("tgt_selected"), L.t("tgt_group"), L.t("tgt_number"))

class RegieTab(a: MainActivity) : Tab(a) {
    private val sv = ScrollView(a)
    private val col = LinearLayout(a)
    override val view: View get() = sv
    private val sel = LinkedHashSet<String>()
    private val list = LinearLayout(a)
    private val info = a.tv("", 14f, IVORY)
    private val tgt = Spinner(a)
    private val kind = Spinner(a)
    private val txt = EditText(a)
    private val grpEt = EditText(a)
    private val numEt = EditText(a)
    private var optL: Button? = null
    private var optC: Button? = null
    private var optP: Button? = null
    private var optQ: Button? = null

    init {
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(14))
        sv.addView(col)
        val ips = App.localIps()
        info.text = L.t("hub_info", if (ips.isEmpty()) L.t("no_net") else ips.joinToString(", "), PORT, App.hubPin, if (App.room.isEmpty()) L.t("room_none") else App.room)
        col.addView(info)

        col.addView(a.tv(L.t("sec_devices"), 13f, DIM), lpw(top = 10, a = a))
        list.orientation = LinearLayout.VERTICAL
        col.addView(list)

        col.addView(row(
            a.btn(L.t("btn_couple"), BTN) { couple() },
            a.btn(L.t("btn_uncouple"), BTN) { for (id in sel) App.send(jo("t" to "uncouple", "id" to id)) },
            a.btn(L.t("btn_auto"), BTN) { autoPairs() }
        ))
        col.addView(row(
            a.btn(L.t("btn_number"), BTN) { editNumber() },
            a.btn(L.t("btn_group"), BTN) { editGroup() },
            a.btn(L.t("btn_call_sel"), BTN) { callSel() }
        ))
        col.addView(row(
            a.btn(L.t("btn_listen"), BTN) { monitor() },
            a.btn(L.t("btn_mute"), BTN) { for (p in picked()) App.send(jo("t" to "mute", "id" to p.id, "on" to !p.muted)) },
            a.btn(L.t("btn_forget"), RED) { forget() }
        ))
        val ol = a.btn("", BTN) { sendOpt(!App.optListen, App.optClick, App.optPeerMsg) }
        val oc = a.btn("", BTN) { sendOpt(App.optListen, !App.optClick, App.optPeerMsg) }
        val op = a.btn("", BTN) { sendOpt(App.optListen, App.optClick, !App.optPeerMsg) }
        val oq = a.btn("", BTN) { sendOpt(App.optListen, App.optClick, App.optPeerMsg, (App.optP2p + 1) % 3) }
        optQ = oq
        optL = ol
        optC = oc
        optP = op
        col.addView(row(ol, oc))
        col.addView(row(op, oq))
        col.addView(row(a.btn(L.t("btn_journal"), BTN) { showLog() }))

        col.addView(a.tv(L.t("sec_cues"), 13f, DIM), lpw(top = 14, a = a))
        tgt.adapter = ArrayAdapter(a, android.R.layout.simple_spinner_dropdown_item, targetOptions())
        col.addView(tgt)
        grpEt.hint = L.t("grp_hint")
        grpEt.setTextColor(Color.WHITE)
        grpEt.setHintTextColor(DIM)
        col.addView(grpEt)
        numEt.hint = L.t("num_hint")
        numEt.inputType = InputType.TYPE_CLASS_NUMBER
        numEt.setTextColor(Color.WHITE)
        numEt.setHintTextColor(DIM)
        col.addView(numEt)
        kind.adapter = ArrayAdapter(a, android.R.layout.simple_spinner_dropdown_item, CUE_KINDS.map { L.t("cue_$it") })
        kind.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                txt.hint = L.t("hint_" + CUE_KINDS[pos])
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        col.addView(kind)
        txt.setTextColor(Color.WHITE)
        txt.setHintTextColor(DIM)
        txt.hint = L.t("hint_text")
        col.addView(txt)
        col.addView(a.btn(L.t("btn_send_cue"), GREEN) { sendCue(CUE_KINDS[kind.selectedItemPosition], txt.text.toString()) })
        col.addView(row(
            a.btn(L.t("meter_noise_btn"), BTN) { val ts = targetsNow(); if (ts.isEmpty()) a.toast(L.t("sel_one")) else ts.forEach { App.sendMeter("noise", it) } },
            a.btn(L.t("meter_motion_btn"), BTN) { val ts = targetsNow(); if (ts.isEmpty()) a.toast(L.t("sel_one")) else ts.forEach { App.sendMeter("motion", it) } }
        ))
        col.addView(a.btn(L.t("fx_btn"), BTN) {
            if (sel.isEmpty()) { a.toast(L.t("sel_one")); return@btn }
            val names = (listOf("") + VoiceFx.KINDS)
            android.app.AlertDialog.Builder(a).setTitle(L.t("fx_btn")).setItems(names.map { if (it.isEmpty()) L.t("fx_none") else L.t("fx_$it") }.toTypedArray()) { _, i ->
                for (id in sel) App.send(jo("t" to "setfx", "id" to id, "k" to names[i]))
            }.show()
        })
        col.addView(a.btn(L.t("media_send"), BTN) {
            val ts = targetsNow()
            if (ts.isEmpty()) a.toast(L.t("sel_one")) else a.pickMedia(ts)
        })
        col.addView(row(
            a.btn(L.t("btn_era", 1985), BTN) { sendCue("era", "1985") },
            a.btn(L.t("btn_era", 1995), BTN) { sendCue("era", "1995") },
            a.btn(L.t("btn_era", 2003), BTN) { sendCue("era", "2003") }
        ))
        refreshList()
        App.send(jo("t" to "peers"))
    }

    private fun sendOpt(l: Boolean, k: Boolean, p: Boolean, p2p: Int = App.optP2p) {
        App.send(jo("t" to "opt", "listen" to l, "click" to k, "peerMsg" to p, "p2p" to p2p))
    }

    private fun row(vararg v: View): LinearLayout {
        val r = LinearLayout(a)
        for (x in v) r.addView(x, LinearLayout.LayoutParams(0, WRAP, 1f))
        return r
    }

    private fun picked(): List<PeerInfo> = App.peers.filter { it.id in sel }

    private fun couple() {
        val p = picked()
        if (p.size != 2) {
            a.toast(L.t("sel_two"))
            return
        }
        App.send(jo("t" to "couple", "a" to p[0].id, "b" to p[1].id))
    }

    private fun autoPairs() {
        AlertDialog.Builder(a).setMessage(L.t("confirm_auto"))
            .setPositiveButton(L.t("yes")) { _, _ -> App.send(jo("t" to "autocouple")) }
            .setNegativeButton(L.t("no"), null).show()
    }

    private fun forget() {
        val p = picked()
        if (p.isEmpty()) {
            a.toast(L.t("sel_one"))
            return
        }
        AlertDialog.Builder(a).setMessage(L.t("confirm_forget", p.size))
            .setPositiveButton(L.t("yes")) { _, _ -> for (x in p) App.send(jo("t" to "forget", "id" to x.id)); sel.clear() }
            .setNegativeButton(L.t("no"), null).show()
    }

    private fun editNumber() {
        val p = picked()
        if (p.size != 1) {
            a.toast(L.t("sel_one"))
            return
        }
        val et = EditText(a)
        et.inputType = InputType.TYPE_CLASS_NUMBER
        et.setText(p[0].number)
        AlertDialog.Builder(a).setTitle(L.t("dlg_number", p[0].name)).setView(et)
            .setPositiveButton(L.t("ok")) { _, _ -> App.send(jo("t" to "setnum", "id" to p[0].id, "number" to et.text.toString())) }
            .setNegativeButton(L.t("cancel"), null).show()
    }

    private fun editGroup() {
        val p = picked()
        if (p.isEmpty()) {
            a.toast(L.t("sel_one"))
            return
        }
        val et = EditText(a)
        et.setText(p[0].group)
        AlertDialog.Builder(a).setTitle(L.t("dlg_group", p.size)).setView(et)
            .setPositiveButton(L.t("ok")) { _, _ -> for (x in p) App.send(jo("t" to "setgroup", "id" to x.id, "group" to et.text.toString())) }
            .setNegativeButton(L.t("cancel"), null).show()
    }

    private fun monitor() {
        val p = picked()
        if (p.size != 1) {
            a.toast(L.t("sel_one"))
            return
        }
        App.send(jo("t" to "monitor", "number" to p[0].number))
        a.selectTab(0)
    }

    private fun callSel() {
        val p = picked()
        if (p.size != 1) {
            a.toast(L.t("sel_one"))
            return
        }
        a.selectTab(0)
        App.callNumber(p[0].number)
    }

    private fun showLog() {
        App.send(jo("t" to "getlog"))
        a.showLogDialog()
    }

    /** Ținta curentă ca listă de șiruri pentru hub; gol = nu s-a ales nimic valid. */
    private fun targetsNow(): List<String> = when (tgt.selectedItemPosition) {
        0 -> listOf("all")
        1 -> listOf("spectators")
        2 -> listOf("actors")
        3 -> sel.map { "id:$it" }
        4 -> listOf("grp:" + grpEt.text.toString().trim())
        else -> listOf("num:" + numEt.text.toString().filter { it.isDigit() })
    }

    private fun sendCue(k: String, text: String) {
        val ts = targetsNow()
        if (ts.isEmpty()) {
            a.toast(L.t("sel_one"))
            return
        }
        for (t in ts) App.sendCue(t, k, text)
        if (k == "era" && tgt.selectedItemPosition == 0) text.toIntOrNull()?.let { App.changeEra(it) }
        a.toast(L.t("sent"))
    }

    private fun refreshList() {
        list.removeAllViews()
        for (p in App.peers) {
            val line = if (p.line != 0) "  ⇄ " + p.line + "/" + (p.slot + 1) else ""
            val st = (if (p.online) "● " else "○ ") + (if (p.busy) "☎ " else "") + (if (p.muted) "🔇 " else "")
            val bat = if (p.bat in 0..100) "  " + p.bat + "%" + (if (p.bat < 20) " ⚠" else "") else ""
            val grp = if (p.group.isNotEmpty()) "  [" + p.group + "]" else ""
            val zf = (if (p.zone > 0) "  📍" + p.zone else "") + (if (p.fx.isNotEmpty()) "  🎙" + L.t("fx_" + p.fx) else "")
            val t = a.tv(st + p.name + " · " + roleLabel(p.role) + " · " + fmtNum(p.number) + line + grp + bat + zf, 15f, if (p.online) Color.WHITE else DIM)
            t.setPadding(a.dp(8), a.dp(8), a.dp(8), a.dp(8))
            t.background = a.box(if (p.id in sel) ACC else c("#2B2F36"))
            t.setOnClickListener {
                if (!sel.add(p.id)) sel.remove(p.id)
                refreshList()
            }
            list.addView(t, lpw(top = 3, a = a))
        }
        if (App.peers.isEmpty()) list.addView(a.tv(L.t("no_devices"), 14f, DIM))
        optL?.text = L.t("opt_listen", if (App.optListen) L.t("yes") else L.t("no"))
        optC?.text = L.t("opt_click", if (App.optClick) L.t("yes") else L.t("no"))
        optP?.text = L.t("opt_peermsg", if (App.optPeerMsg) L.t("yes") else L.t("no"))
        optQ?.text = L.t("opt_p2p", L.t(listOf("p2p_off", "p2p_lan", "p2p_any")[App.optP2p.coerceIn(0, 2)]))
    }

    override fun update(kind: String) {
        if (kind == "peers" || kind == "state") refreshList()
    }
}

// ====================================================================== SCENARIU

class ScriptTab(a: MainActivity) : Tab(a) {
    private val sv = ScrollView(a)
    private val col = LinearLayout(a)
    override val view: View get() = sv
    private val list = LinearLayout(a)
    private val ctrl = LinearLayout(a)
    private val nameEt = EditText(a)
    private val tgt = Spinner(a)
    private val kind = Spinner(a)
    private val txt = EditText(a)
    private val delayEt = EditText(a)
    private val extra = EditText(a)
    private var picked = -1

    init {
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(a.dp(10), a.dp(8), a.dp(10), a.dp(14))
        sv.addView(col)
        col.addView(a.tv(L.t("sec_script"), 13f, DIM))
        col.addView(a.tv(L.t("script_help"), 12f, DIM))
        list.orientation = LinearLayout.VERTICAL
        col.addView(list, lpw(top = 6, a = a))
        ctrl.orientation = LinearLayout.VERTICAL
        col.addView(ctrl)

        col.addView(a.tv(L.t("sec_newstep"), 13f, DIM), lpw(top = 14, a = a))
        nameEt.hint = L.t("step_name_hint")
        nameEt.setTextColor(Color.WHITE)
        nameEt.setHintTextColor(DIM)
        col.addView(nameEt)
        val to = targetOptions() + listOf(L.t("tgt_staff"))
        tgt.adapter = ArrayAdapter(a, android.R.layout.simple_spinner_dropdown_item, to.filterIndexed { i, _ -> i != 3 })
        col.addView(tgt)
        extra.hint = L.t("grp_or_num_hint")
        extra.setTextColor(Color.WHITE)
        extra.setHintTextColor(DIM)
        col.addView(extra)
        val kinds = CUE_KINDS + "msg"
        kind.adapter = ArrayAdapter(a, android.R.layout.simple_spinner_dropdown_item, kinds.map { if (it == "msg") L.t("kind_msg") else L.t("cue_$it") })
        kind.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                txt.hint = if (kinds[pos] == "msg") L.t("hint_msg") else L.t("hint_" + kinds[pos])
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        col.addView(kind)
        txt.setTextColor(Color.WHITE)
        txt.setHintTextColor(DIM)
        col.addView(txt)
        delayEt.hint = L.t("step_delay_hint")
        delayEt.inputType = InputType.TYPE_CLASS_NUMBER
        delayEt.setTextColor(Color.WHITE)
        delayEt.setHintTextColor(DIM)
        col.addView(delayEt)
        col.addView(a.btn(L.t("btn_add_step"), GREEN) { addStep(kinds) })
        col.addView(row(
            a.btn(L.t("btn_export"), BTN) { a.shareText(App.scriptJson()) },
            a.btn(L.t("btn_import"), BTN) { importDialog() }
        ))
        refresh()
    }

    private fun row(vararg v: View): LinearLayout {
        val r = LinearLayout(a)
        for (x in v) r.addView(x, LinearLayout.LayoutParams(0, WRAP, 1f))
        return r
    }

    private fun addStep(kinds: List<String>) {
        // spinner fără „Dispozitive selectate”: 0 toți, 1 spectatori, 2 actori, 3 grup, 4 număr, 5 personal
        val e = extra.text.toString().trim()
        val to = when (tgt.selectedItemPosition) {
            0 -> "all"
            1 -> "spectators"
            2 -> "actors"
            3 -> "grp:$e"
            4 -> "num:" + e.filter { it.isDigit() }
            else -> "staff"
        }
        val k = kinds[kind.selectedItemPosition]
        val nm = nameEt.text.toString().trim().ifEmpty { L.t("cue_" + (if (k == "msg") "text" else k)) }
        val d = delayEt.text.toString().toIntOrNull() ?: 0
        App.addStep(Step(nm, to, k, txt.text.toString(), d.coerceIn(0, 3600)))
        nameEt.setText("")
        txt.setText("")
    }

    private fun importDialog() {
        val et = EditText(a)
        et.hint = "[ { \"name\": … } ]"
        AlertDialog.Builder(a).setTitle(L.t("btn_import")).setView(et)
            .setPositiveButton(L.t("ok")) { _, _ ->
                a.toast(if (App.importScript(et.text.toString())) L.t("script_imported") else L.t("script_bad"))
            }
            .setNegativeButton(L.t("cancel"), null).show()
    }

    private fun describe(s: Step): String {
        val to = when {
            s.to == "all" -> L.t("tgt_all")
            s.to == "spectators" -> L.t("tgt_spectators")
            s.to == "actors" -> L.t("tgt_actors")
            s.to == "staff" -> L.t("tgt_staff")
            s.to.startsWith("grp:") -> L.t("tgt_group") + " " + s.to.substring(4)
            s.to.startsWith("num:") -> s.to.substring(4)
            else -> s.to
        }
        val k = if (s.kind == "msg") L.t("kind_msg") else L.t("cue_" + s.kind)
        return s.name + "\n" + k + " → " + to + (if (s.text.isNotEmpty()) " · " + s.text.take(40) else "") + (if (s.delay > 0) " · +" + s.delay + "s" else "")
    }

    private fun refresh() {
        list.removeAllViews()
        for (i in App.script.indices) {
            val s = App.script[i]
            val mark = if (i == App.cursor) "▶ " else "   "
            val t = a.tv(mark + (i + 1) + ". " + describe(s), 14f, if (i < App.cursor) DIM else Color.WHITE)
            t.setPadding(a.dp(8), a.dp(6), a.dp(8), a.dp(6))
            t.background = a.box(if (i == picked) ACC else if (i == App.cursor) c("#3A2F22") else c("#2B2F36"))
            t.setOnClickListener {
                picked = if (picked == i) -1 else i
                refresh()
            }
            list.addView(t, lpw(top = 3, a = a))
        }
        if (App.script.isEmpty()) list.addView(a.tv(L.t("script_empty"), 14f, DIM))
        ctrl.removeAllViews()
        val r1 = LinearLayout(a)
        r1.addView(a.btn(L.t("btn_go"), GREEN) { App.goNext() }, LinearLayout.LayoutParams(0, a.dp(64), 2f))
        r1.addView(
            a.btn(if (App.playing) L.t("btn_stop") else L.t("btn_play"), if (App.playing) RED else BTN) {
                if (App.playing) App.stopScript() else App.playFrom(if (picked >= 0) picked else App.cursor)
            },
            LinearLayout.LayoutParams(0, a.dp(64), 1f)
        )
        ctrl.addView(r1, lpw(top = 6, a = a))
        val r2 = LinearLayout(a)
        r2.addView(a.btn(L.t("btn_rewind"), BTN) { App.cursor = 0; App.notify("script") }, LinearLayout.LayoutParams(0, WRAP, 1f))
        r2.addView(a.btn("▲", BTN) { if (picked >= 0) { App.moveStep(picked, -1); picked = maxOf(0, picked - 1); refresh() } }, LinearLayout.LayoutParams(0, WRAP, 1f))
        r2.addView(a.btn("▼", BTN) { if (picked >= 0) { App.moveStep(picked, 1); picked = minOf(App.script.size - 1, picked + 1); refresh() } }, LinearLayout.LayoutParams(0, WRAP, 1f))
        r2.addView(a.btn(L.t("btn_del"), RED) { if (picked >= 0) { App.removeStep(picked); picked = -1 } }, LinearLayout.LayoutParams(0, WRAP, 1f))
        ctrl.addView(r2)
    }

    override fun update(kind: String) {
        if (kind == "script") refresh()
    }
}
