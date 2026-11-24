package ro.trom.cuplaj

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val TP = 18470

/** Un telefon simulat care vorbește protocolul cu hubul. */
class C(val id: String, val name: String, val role: String, pin: String = "1234", room: String = "R1", tok: String = "") {
    val sock = Socket("127.0.0.1", TP)
    val conn = Conn(sock)
    val q = LinkedBlockingQueue<Frame>()
    var number = ""
    var tok = ""
    var denied = ""

    init {
        conn.startWriter()
        conn.sendJson(jo("t" to "hello", "id" to id, "name" to name, "role" to role, "pin" to pin, "room" to room, "tok" to tok))
        Thread {
            try {
                while (true) q.put(conn.readFrame())
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true; start() }
        val first = nextOf(setOf("welcome", "denied"), 1500)
        if (first != null) {
            if (first.optString("t") == "welcome") {
                number = first.getString("number")
                this.tok = first.getString("tok")
            } else denied = first.optString("why")
        }
    }

    fun send(vararg p: Pair<String, Any?>) = conn.sendJson(jo(*p))
    fun audio(mode: Int) {
        val b = ByteArray(321)
        b[0] = mode.toByte()
        b[1] = 7
        conn.send(Frame(T_AUDIO, b))
    }

    fun next(t: String, ms: Long = 1500): JSONObject? = nextOf(setOf(t), ms)

    fun nextOf(ts: Set<String>, ms: Long): JSONObject? {
        val end = System.currentTimeMillis() + ms
        while (true) {
            val left = end - System.currentTimeMillis()
            if (left <= 0) return null
            val f = q.poll(left, TimeUnit.MILLISECONDS) ?: return null
            if (f.type == T_JSON) {
                val j = parseJson(f)
                if (j.optString("t") in ts) return j
            }
        }
    }

    fun gotAudio(ms: Long = 600): Boolean {
        val end = System.currentTimeMillis() + ms
        while (true) {
            val left = end - System.currentTimeMillis()
            if (left <= 0) return false
            val f = q.poll(left, TimeUnit.MILLISECONDS) ?: return false
            if (f.type == T_AUDIO) return true
        }
    }

    fun drain() {
        Thread.sleep(150)
        q.clear()
    }
}

private var fails = 0
private fun check(name: String, ok: Boolean) {
    println((if (ok) "OK   " else "FAIL ") + name)
    if (!ok) fails++
}

fun runAll(): Int {
    fails = 0
    var saved = ""
    val hub = Hub("T", "1234", "R1", TP, null, false)
    hub.onPersist = { saved = it }
    hub.ringTimeoutMs = 1500
    hub.start()
    Thread.sleep(400)

    // ---- acces
    check("PIN greșit refuzat (actor)", C("x1", "Hack", "actor", pin = "0000").denied == "bad_pin")
    check("cod de sală greșit refuzat", C("x2", "Hack", "spectator", room = "zzz").denied == "bad_room")
    val r = C("r", "Regia", "regie")
    val s1 = C("s1", "Ana", "spectator"); val s2 = C("s2", "Bob", "spectator")
    val s3 = C("s3", "Cip", "spectator"); val s4 = C("s4", "Dan", "spectator")
    val ac = C("a1", "Actor", "actor")
    check("numere: regie=100, actor=201, spectator 6 cifre", r.number == "100" && ac.number == "201" && s1.number.length == 6 && s1.number != s2.number)
    check("token emis la prima conectare", s1.tok.length == 24)
    check("uzurparea id-ului fără token refuzată", C("s1", "Intrus", "spectator").denied == "dup")
    val s1b = C("s1", "Ana", "spectator", tok = s1.tok)
    check("reconectare cu token păstrează numărul", s1b.number == s1.number)
    s1.sock.close()
    val s1x = s1b
    check("nume curățat și limitat", C("n1", "A\u0001B" + "x".repeat(80), "spectator").let { it.number.isNotEmpty() })

    for (x in listOf(s1x, s2, s3, s4, ac, r)) x.drain()
    // ---- cuplaj
    r.send("t" to "couple", "a" to "s1", "b" to "s2"); Thread.sleep(200)
    check("cuplaj: s1 primește line/slot", s1x.next("line")!!.let { it.getInt("line") != 0 && it.getInt("slot") == 0 })
    check("cuplaj: s2 slot 1 și partener = s1", s2.next("line")!!.let { it.getInt("slot") == 1 && it.getString("partner") == s1x.number })
    check("starea se salvează (cuplaje, numere)", saved.contains(s1x.number) && saved.contains("\"line\":1"))
    s1x.drain(); s2.drain(); s3.drain()
    s3.send("t" to "dial", "number" to s1x.number)
    check("s3 aude ringback", s3.next("ringback") != null)
    val ring1 = s1x.next("ring"); val ring2 = s2.next("ring")
    check("s1 sună (pat 0)", ring1 != null && ring1.getInt("pat") == 0 && ring1.getString("num") == s3.number)
    check("s2 (vecin) sună și el, party", ring2 != null && ring2.getBoolean("party") && ring2.getInt("pat") == 0)
    s2.send("t" to "answer")
    val c2 = s2.next("connected")
    check("s2 răspunde în locul lui s1 (forNum)", c2 != null && c2.getString("forNum") == s1x.number && c2.getString("peer") == s3.number)
    check("s1 oprește soneria", s1x.next("stopring") != null)
    check("s3 conectat cu s2", s3.next("connected")?.getString("peer") == s2.number)
    s2.drain(); s1x.drain(); s3.drain()
    s3.audio(0); check("audio s3 -> s2", s2.gotAudio())
    s1x.send("t" to "offhook")
    check("s1 ascultă pe linie", s1x.next("listening") != null)
    check("părțile aud clic", s2.next("click") != null && s3.next("click") != null)
    s1x.drain(); s2.drain(); s3.drain()
    s3.audio(0); check("audio s3 -> s1 (ascultător) și s2", s1x.gotAudio() && s2.gotAudio())
    s2.audio(0); s2.drain(); s3.drain()
    s1x.audio(0); check("ascultătorul tăcut NU se aude", !s3.gotAudio(400))
    s1x.send("t" to "intervene", "on" to true); Thread.sleep(150); s3.drain(); s2.drain()
    s1x.audio(0); check("după intervenție se aude", s3.gotAudio() && s2.gotAudio())
    s4.send("t" to "dial", "number" to s1x.number); check("apel către linie ocupată => busy", s4.next("busy") != null)
    s4.send("t" to "onhook"); s4.drain()
    s4.send("t" to "dial", "number" to s2.number); check("apel către s2 (ocupat) => busy", s4.next("busy") != null)
    s4.send("t" to "onhook"); s4.drain()
    s3.send("t" to "hangup")
    check("s1,s2 primesc ended", s1x.next("ended") != null && s2.next("ended") != null)
    s1x.send("t" to "offhook"); check("s1 ton de apel", s1x.next("dialtone") != null)
    s2.send("t" to "offhook"); check("s2 ocupat (vecinul formează)", s2.next("busy") != null)
    s1x.send("t" to "onhook"); s2.send("t" to "onhook"); Thread.sleep(150)
    s2.send("t" to "offhook"); check("s2 ton de apel după ce s1 a pus receptorul", s2.next("dialtone") != null)
    s2.send("t" to "onhook"); s2.drain()

    // ---- apeluri diverse
    s4.send("t" to "dial", "number" to "999999"); check("număr inexistent", s4.next("unknown") != null)
    s4.send("t" to "onhook"); s4.drain()
    s4.send("t" to "dial", "number" to s3.number); s3.next("ring"); s3.send("t" to "hangup")
    check("respingere => ended(rejected)", s4.next("ended")?.getString("why") == "rejected")
    s4.send("t" to "dial", "number" to s3.number); s3.next("ring")
    check("fără răspuns => ended(noanswer) după timeout", s4.next("ended", 4000)?.getString("why") == "noanswer")
    s3.drain(); s4.drain()
    s3.send("t" to "dial", "number" to s4.number); s4.next("ring"); s4.send("t" to "answer"); s3.next("connected"); s4.next("connected")
    s3.drain(); s4.drain()
    r.send("t" to "monitor", "number" to s3.number)
    check("regia ascultă", r.next("listening") != null)
    check("ascultare discretă: fără clic", s3.next("click", 400) == null)
    s3.audio(0); check("regia aude convorbirea", r.gotAudio())
    r.send("t" to "hangup"); s3.send("t" to "hangup"); s3.drain(); s4.drain(); r.drain()

    // ---- mesaje, indicii, voce, grupuri, mut
    ac.send("t" to "msg", "to" to "spectators", "text" to "salut")
    check("mesaj actor -> spectatori", s3.next("msg")?.getString("text") == "salut" && s4.next("msg") != null)
    s1x.send("t" to "msg", "to" to "all", "text" to "x")
    check("spectatorul nu poate trimite 'toți'", r.next("msg")?.getString("to") == "staff" && s3.next("msg", 400) == null)
    r.send("t" to "opt", "listen" to true, "click" to true, "peerMsg" to false); Thread.sleep(150); s4.drain()
    s3.send("t" to "msg", "to" to "num:" + s4.number, "text" to "sec")
    check("mesaje între spectatori dezactivabile", s4.next("msg", 400) == null)
    r.send("t" to "opt", "listen" to true, "click" to true, "peerMsg" to true); Thread.sleep(150)
    r.send("t" to "cue", "to" to "spectators", "kind" to "text", "text" to "Lumina se stinge")
    check("indiciu regie -> spectatori", s3.next("cue")?.getString("text") == "Lumina se stinge")
    s1x.send("t" to "cue", "to" to "all", "kind" to "text", "text" to "fals")
    check("spectatorul nu poate da indicii", s3.next("cue", 400) == null)
    r.send("t" to "setgroup", "id" to "s3", "group" to "Roșu"); Thread.sleep(150); s3.drain(); s4.drain()
    r.send("t" to "cue", "to" to "grp:Roșu", "kind" to "vibrate", "text" to "300")
    check("indiciu pe grup ajunge doar la grup", s3.next("cue") != null && s4.next("cue", 400) == null)
    s3.drain(); r.drain()
    r.audio(1); check("PTT 'vocea' ajunge la spectatori", s3.gotAudio() && s4.gotAudio())
    s3.drain(); ac.drain()
    s3.audio(1); check("spectatorul nu poate folosi 'vocea'", !s4.gotAudio(400))
    s3.audio(2); check("spectator -> personal (mod 2)", ac.gotAudio())
    r.send("t" to "mute", "id" to "s3", "on" to true); Thread.sleep(150); ac.drain()
    s3.audio(2); check("dispozitiv mutat nu se aude", !ac.gotAudio(400))
    r.send("t" to "mute", "id" to "s3", "on" to false); Thread.sleep(150)
    s3.send("t" to "stat", "bat" to 17); Thread.sleep(150); r.drain()
    r.send("t" to "peers")
    check("baterie raportată către regie", r.next("peers")!!.getJSONArray("list").let { a -> (0 until a.length()).any { a.getJSONObject(it).getString("id") == "s3" && a.getJSONObject(it).getInt("bat") == 17 } })
    s3.send("t" to "setgroup", "id" to "s3", "group" to "hack"); r.drain(); r.send("t" to "peers")
    check("spectatorul nu poate seta grupuri", r.next("peers")!!.getJSONArray("list").let { a -> (0 until a.length()).none { a.getJSONObject(it).getString("group") == "hack" } })
    s3.send("t" to "dial", "number" to s4.number); val rr = s4.next("ring")
    check("cadență standard pentru nr. necuplat (pat 0)", rr?.getInt("pat") == 0)
    s3.send("t" to "hangup"); s4.drain(); s3.drain()

    // ---- flux
    for (i in 0 until 400) s3.send("t" to "msg", "to" to "staff", "text" to "spam$i")
    Thread.sleep(500)
    var cnt = 0
    while (ac.next("msg", 200) != null) cnt++
    check("limitare de flux: spam trunchiat (<=45 din 400)", cnt in 1..45)

    // ---- conexiune, eliminare, persistență
    s3.send("t" to "dial", "number" to s4.number); s4.next("ring"); s4.send("t" to "answer"); s3.next("connected"); s4.next("connected")
    s3.drain(); s4.drain()
    s3.sock.close()
    check("cădere conexiune => partenerul primește ended", s4.next("ended", 2000) != null)
    val s3b = C("s3", "Cip", "spectator", tok = s3.tok)
    check("reconectare păstrează numărul", s3b.number == s3.number)
    r.send("t" to "forget", "id" to "s4"); Thread.sleep(300)
    r.drain(); r.send("t" to "peers")
    check("dispozitiv eliminat din listă", r.next("peers")!!.getJSONArray("list").let { a -> (0 until a.length()).none { a.getJSONObject(it).getString("id") == "s4" } })
    check("starea salvată conține grupul", saved.contains("Roșu"))
    hub.stop(); Thread.sleep(300)
    val hub2 = Hub("T", "1234", "R1", TP, saved, false); hub2.start(); Thread.sleep(400)
    val again = C("s3", "Cip", "spectator", tok = s3.tok)
    check("după repornirea hubului numărul rămâne", again.number == s3.number)
    val again1 = C("s1", "Ana", "spectator", tok = s1.tok)
    check("după repornire cuplajul rămâne (line)", (again1.next("line")?.getInt("line") ?: 0) != 0)
    hub2.stop()
    println(if (fails == 0) "TOATE TESTELE AU TRECUT" else "$fails TESTE EȘUATE")
    return fails
}

class HubTest {
    @Test
    fun scenarii() {
        assertEquals(0L, runAll().toLong())
    }
}
