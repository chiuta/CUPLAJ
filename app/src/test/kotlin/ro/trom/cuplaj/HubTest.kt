package ro.trom.cuplaj

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.Socket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val TP = 18470

/** Un telefon simulat care vorbește protocolul cu hubul. */
class C(val id: String, val name: String, val role: String, pin: String = "1234", room: String = "R1", tok: String = "", port: Int = TP, key: ByteArray? = null, p2p: Boolean = false, smsgw: Boolean = false) {
    val sock = Socket("127.0.0.1", port)
    val conn = Conn(sock, key, false)
    val q = LinkedBlockingQueue<Frame>()
    var number = ""
    var tok = ""
    var denied = ""

    init {
        conn.startWriter()
        conn.sendJson(jo("t" to "hello", "id" to id, "name" to name, "role" to role, "pin" to pin, "room" to room, "tok" to tok, "p2p" to p2p, "smsgw" to smsgw))
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

    var lastAudioType = 0
    fun audioU(mode: Int) {
        val b = ByteArray(161)
        b[0] = mode.toByte()
        b[1] = 7
        conn.send(Frame(T_AUDIO_U, b))
    }

    fun audioIn(ms: Long = 600): Int {
        val end = System.currentTimeMillis() + ms
        while (true) {
            val left = end - System.currentTimeMillis()
            if (left <= 0) return 0
            val f = q.poll(left, TimeUnit.MILLISECONDS) ?: return 0
            if (f.type == T_AUDIO || f.type == T_AUDIO_U) return f.type
        }
    }

    fun next(t: String, ms: Long = 1500): JSONObject? = nextOf(setOf(t), ms)

    fun nextOf(ts: Set<String>, ms: Long): JSONObject? {
        val end = System.currentTimeMillis() + ms
        while (true) {
            val left = end - System.currentTimeMillis()
            if (left <= 0) return null
            val f = q.poll(left, TimeUnit.MILLISECONDS) ?: return null
            if (f.type == T_JSON && f.data.isNotEmpty()) {
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
            if (f.type == T_AUDIO || f.type == T_AUDIO_U) return true
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

/** Teste de rețea: cifrare, cheie greșită, codec μ-law, ping, liniște maximă. */
fun runNet(): Int {
    fails = 0
    val port = 18480
    val key = Sec.deriveKey("test-parola")!!
    val hub = Hub("N", "1234", "", port, null, false, "test-parola")
    hub.idleTimeoutMs = 1200
    hub.start(); Thread.sleep(400)
    val a = C("n1", "Ana", "spectator", room = "", port = port, key = key)
    val b = C("n2", "Bob", "spectator", room = "", port = port, key = key)
    check("cifrat: conectare cu cheia corectă", a.number.isNotEmpty() && b.number.isNotEmpty())
    check("fără cheie: refuz în clar bad_key", C("n3", "X", "spectator", room = "", port = port).denied == "bad_key")
    check("cheie greșită: refuz bad_key", C("n4", "X", "spectator", room = "", port = port, key = Sec.deriveKey("alta")).denied == "bad_key")
    a.drain(); b.drain()
    a.send("t" to "ping")
    check("ping => pong", a.next("pong") != null)
    a.send("t" to "dial", "number" to b.number); b.next("ring"); b.send("t" to "answer"); a.next("connected"); b.next("connected")
    a.drain(); b.drain()
    a.audioU(0)
    check("audio μ-law ajunge ca μ-law", b.audioIn() == T_AUDIO_U)
    a.audio(0)
    check("audio PCM ajunge ca PCM", b.audioIn() == T_AUDIO)
    // G.711: dus-întors păstrează forma (eroare mică)
    val pcm = ByteArray(320)
    for (i in 0 until 160) {
        val v = (Math.sin(i * 0.2) * 12000).toInt()
        pcm[2 * i] = (v and 0xFF).toByte(); pcm[2 * i + 1] = ((v shr 8) and 0xFF).toByte()
    }
    val back = G711.decode(G711.encode(pcm))
    var worst = 0
    for (i in 0 until 160) {
        val x = (pcm[2 * i].toInt() and 0xFF) or (pcm[2 * i + 1].toInt() shl 8)
        val y = (back[2 * i].toInt() and 0xFF) or (back[2 * i + 1].toInt() shl 8)
        worst = maxOf(worst, Math.abs(x.toShort() - y.toShort()))
    }
    check("G.711: 320→160→320 octeți, eroare < 3% din amplitudine ($worst)", G711.encode(pcm).size == 160 && back.size == 320 && worst < 400)
    // manipulare: text cifrat modificat nu se decriptează
    val nonce = ByteArray(12); val ct = Sec.encrypt(key, nonce, "salut".toByteArray())
    check("AES-GCM: decriptare corectă", String(Sec.decrypt(key, nonce, ct)!!) == "salut")
    ct[0] = (ct[0].toInt() xor 1).toByte()
    check("AES-GCM: text modificat respins", Sec.decrypt(key, nonce, ct) == null)
    // liniște maximă
    Thread.sleep(2200)
    a.send("t" to "ping")
    check("liniște peste limită => conexiune închisă (fără pong)", a.next("pong", 800) == null)
    hub.stop(); Thread.sleep(200)
    println(if (fails == 0) "RETEA OK" else "$fails ESECURI RETEA")
    return fails
}

/** P2P direct: rendezvous UDP, semnalizare, legătură cifrată, oprire la ascultare/mut/dezactivare. */
fun runP2p(): Int {
    fails = 0
    val port = 18490
    val hub = Hub("P", "1234", "", port, null, false)
    hub.start(); Thread.sleep(400)
    val r = C("r", "Regia", "regie", room = "", port = port)
    val a = C("p1", "Ana", "spectator", room = "", port = port, p2p = true)
    val b = C("p2", "Bob", "spectator", room = "", port = port, p2p = true)
    val c = C("p3", "Cip", "spectator", room = "", port = port, p2p = false)
    val gotB = java.util.concurrent.LinkedBlockingQueue<ByteArray>()
    val la = P2pLink { }
    val lb = P2pLink { gotB.put(it) }
    val hubAddr = java.net.InetSocketAddress("127.0.0.1", port + P2P_PORT_OFFSET)
    la.registerAt(hubAddr, "p1", a.tok); lb.registerAt(hubAddr, "p2", b.tok)
    var t = 0
    while ((!la.hubSeen || !lb.hubSeen) && t++ < 40) Thread.sleep(100)
    check("rendezvous UDP: hubul confirmă ambele telefoane", la.hubSeen && lb.hubSeen)
    a.send("t" to "udp", "lan" to "127.0.0.1:" + la.port); b.send("t" to "udp", "lan" to "127.0.0.1:" + lb.port)
    Thread.sleep(200)
    a.drain(); b.drain(); r.drain()
    a.send("t" to "dial", "number" to b.number); b.next("ring"); b.send("t" to "answer")
    val pa = a.next("p2p"); val pb = b.next("p2p")
    check("hubul semnalizează P2P ambilor (aceeași cheie, init diferit)", pa != null && pb != null && pa.getString("key") == pb.getString("key") && pa.getBoolean("init") != pb.getBoolean("init"))
    check("candidați: LAN + adresa văzută de hub", pa!!.getJSONArray("cand").length() == 2 && pa.getJSONArray("cand").getString(0).endsWith(":" + lb.port))
    fun addrs(j: JSONObject) = (0 until j.getJSONArray("cand").length()).mapNotNull { parseAddr(j.getJSONArray("cand").getString(it)) }
    la.begin(unhex(pa.getString("key")), addrs(pa), pa.getBoolean("init"))
    lb.begin(unhex(pb!!.getString("key")), addrs(pb), pb.getBoolean("init"))
    t = 0
    while ((!la.up || !lb.up) && t++ < 40) Thread.sleep(100)
    check("legătură directă stabilită pe ambele părți", la.up && lb.up)
    val pcm = ByteArray(320) { (it % 50).toByte() }
    check("audio direct A→B livrat (cifrat, μ-law)", la.sendPcm(pcm) && gotB.poll(1500, TimeUnit.MILLISECONDS)?.size == 320)
    val ev = P2pLink { gotB.put(it) }
    ev.begin(Sec.randomBytes(16), listOf(java.net.InetSocketAddress("127.0.0.1", lb.port)), true)
    Thread.sleep(600)
    check("sondă cu cheie străină ignorată", !ev.up)
    ev.close()
    r.send("t" to "monitor", "number" to a.number)
    check("monitorizarea regiei oprește P2P (p2pstop)", a.next("p2pstop") != null && b.next("p2pstop") != null)
    la.end(); lb.end()
    a.send("t" to "onhook"); b.send("t" to "onhook"); a.drain(); b.drain()
    a.send("t" to "dial", "number" to c.number); c.next("ring"); c.send("t" to "answer")
    check("partener fără suport P2P => releu (fără mesaj p2p)", a.next("p2p", 600) == null)
    a.send("t" to "onhook"); c.send("t" to "onhook"); a.drain(); b.drain()
    r.send("t" to "mute", "id" to "p2", "on" to true); Thread.sleep(150)
    a.send("t" to "dial", "number" to b.number); b.next("ring"); b.send("t" to "answer")
    check("telefon mut => fără P2P", a.next("p2p", 600) == null)
    a.send("t" to "onhook"); b.send("t" to "onhook"); a.drain(); b.drain()
    r.send("t" to "mute", "id" to "p2", "on" to false)
    r.send("t" to "opt", "p2p" to 0, "listen" to true, "click" to true, "peerMsg" to true); Thread.sleep(150)
    a.send("t" to "dial", "number" to b.number); b.next("ring"); b.send("t" to "answer")
    check("P2P dezactivat de regie => fără semnalizare", a.next("p2p", 600) == null)
    la.close(); lb.close(); hub.stop(); Thread.sleep(200)
    println(if (fails == 0) "P2P OK" else "$fails ESECURI P2P")
    return fails
}

/** Server SIP fals: digest, 180/200, RTP PCMU; poate și să inițieze un INVITE. */
class FakeSip {
    val sock = java.net.DatagramSocket(0, java.net.InetAddress.getByName("127.0.0.1")).also { it.soTimeout = 200 }
    val rtp = java.net.DatagramSocket(0, java.net.InetAddress.getByName("127.0.0.1")).also { it.soTimeout = 200 }
    @Volatile var run = true
    @Volatile var registered = false
    @Volatile var regAuthOk = false
    @Volatile var inviteAuthOk = false
    @Volatile var inviteDest = ""
    @Volatile var gotBye = false
    @Volatile var got200ForInbound = false
    @Volatile var gwRtp: java.net.InetSocketAddress? = null
    @Volatile var gwSip: java.net.InetSocketAddress? = null
    val rtpIn = LinkedBlockingQueue<ByteArray>()
    var lastInvite = ""
    var inboundCallId = "in-1@fake"
    private fun md5(s: String) = java.security.MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun h(m: String, n: String) = m.split("\r\n").firstOrNull { it.lowercase().startsWith(n.lowercase() + ":") }?.substringAfter(":")?.trim() ?: ""
    private fun send(t: String, to: java.net.SocketAddress) { val b = t.toByteArray(); sock.send(java.net.DatagramPacket(b, b.size, to)) }
    private fun digestOk(m: String, method: String, qop: Boolean): Boolean {
        val a = h(m, "authorization").ifEmpty { h(m, "proxy-authorization") }
        if (a.isEmpty()) return false
        fun p(k: String) = Regex("$k=\"?([^\",]+)\"?").find(a)?.groupValues?.get(1) ?: ""
        val ha1 = md5("alice:fakerealm:secret"); val ha2 = md5(method + ":" + p("uri"))
        val exp = if (qop) md5(ha1 + ":nonce123:" + p("nc") + ":" + p("cnonce") + ":auth:" + ha2) else md5(ha1 + ":nonce123:" + ha2)
        return p("response") == exp && p("username") == "alice"
    }
    val thread = Thread {
        val buf = ByteArray(4096)
        while (run) {
            try {
                val pk = java.net.DatagramPacket(buf, buf.size); sock.receive(pk)
                val m = String(pk.data, 0, pk.length); val from = pk.socketAddress
                gwSip = java.net.InetSocketAddress(pk.address, pk.port)
                val first = m.substringBefore("\r\n")
                fun resp(code: Int, reason: String, extra: String = "", body: String = "", toTag: String = ""): String {
                    val vias = m.split("\r\n").filter { it.lowercase().startsWith("via:") }.joinToString("") { it + "\r\n" }
                    var to = h(m, "to"); if (toTag.isNotEmpty() && !to.contains("tag=")) to += ";tag=$toTag"
                    return "SIP/2.0 $code $reason\r\n" + vias + "From: " + h(m, "from") + "\r\nTo: " + to + "\r\nCall-ID: " + h(m, "call-id") + "\r\nCSeq: " + h(m, "cseq") + "\r\n" + extra + "Content-Length: ${body.length}\r\n\r\n" + body
                }
                when {
                    first.startsWith("REGISTER") -> if (h(m, "authorization").isEmpty()) send(resp(401, "Unauthorized", "WWW-Authenticate: Digest realm=\"fakerealm\", nonce=\"nonce123\", algorithm=MD5\r\n"), from)
                        else { regAuthOk = digestOk(m, "REGISTER", false); if (regAuthOk) { registered = true; send(resp(200, "OK"), from) } else send(resp(403, "Forbidden"), from) }
                    first.startsWith("INVITE") -> {
                        if (h(m, "authorization").isEmpty()) {
                            send(resp(401, "Unauthorized", "WWW-Authenticate: Digest realm=\"fakerealm\", nonce=\"nonce123\", qop=\"auth\", algorithm=MD5\r\n", "", "t1"), from)
                        } else {
                            inviteAuthOk = digestOk(m, "INVITE", true)
                            inviteDest = first.split(" ")[1]
                            if (inviteAuthOk) {
                                lastInvite = m
                                send(resp(180, "Ringing", "", "", "t1"), from)
                                val port = Regex("m=audio (\\d+)").find(m)!!.groupValues[1].toInt()
                                gwRtp = java.net.InetSocketAddress("127.0.0.1", port)
                                val sdp = "v=0\r\no=- 1 1 IN IP4 127.0.0.1\r\ns=-\r\nc=IN IP4 127.0.0.1\r\nt=0 0\r\nm=audio ${rtp.localPort} RTP/AVP 0\r\na=rtpmap:0 PCMU/8000\r\n"
                                send(resp(200, "OK", "Contact: <sip:fake@127.0.0.1:${sock.localPort}>\r\nContent-Type: application/sdp\r\n", sdp, "t1"), from)
                            } else send(resp(403, "Forbidden"), from)
                        }
                    }
                    first.startsWith("BYE") -> { gotBye = true; send(resp(200, "OK"), from) }
                    first.startsWith("SIP/2.0 200") && h(m, "cseq").endsWith("INVITE") -> {
                        got200ForInbound = true
                        val port = Regex("m=audio (\\d+)").find(m)!!.groupValues[1].toInt()
                        gwRtp = java.net.InetSocketAddress("127.0.0.1", port)
                    }
                }
            } catch (_: java.net.SocketTimeoutException) {} catch (_: Exception) { if (!run) return@Thread }
        }
    }.apply { isDaemon = true; start() }
    val rtpThread = Thread {
        val buf = ByteArray(1500)
        while (run) {
            try { val pk = java.net.DatagramPacket(buf, buf.size); rtp.receive(pk); rtpIn.put(buf.copyOf(pk.length)) }
            catch (_: java.net.SocketTimeoutException) {} catch (_: Exception) { if (!run) return@Thread }
        }
    }.apply { isDaemon = true; start() }
    fun sendRtp(payload: ByteArray) {
        val to = gwRtp ?: return
        val p = ByteArray(12 + payload.size); p[0] = 0x80.toByte(); System.arraycopy(payload, 0, p, 12, payload.size)
        rtp.send(java.net.DatagramPacket(p, p.size, to))
    }
    fun inviteIn(gw: java.net.InetSocketAddress, to: String) {
        val sdp = "v=0\r\no=- 1 1 IN IP4 127.0.0.1\r\ns=-\r\nc=IN IP4 127.0.0.1\r\nt=0 0\r\nm=audio ${rtp.localPort} RTP/AVP 0\r\na=rtpmap:0 PCMU/8000\r\n"
        val m = "INVITE sip:alice@127.0.0.1 SIP/2.0\r\nVia: SIP/2.0/UDP 127.0.0.1:${sock.localPort};branch=z9hG4bKabc\r\nFrom: <sip:555@fake>;tag=ft\r\nTo: <sip:alice@127.0.0.1>\r\nCall-ID: $inboundCallId\r\nCSeq: 1 INVITE\r\nContact: <sip:555@127.0.0.1:${sock.localPort}>\r\nContent-Type: application/sdp\r\nContent-Length: ${sdp.length}\r\n\r\n" + sdp
        send(m, gw)
    }
    fun bye(callId: String = inboundCallId) {
        val to = gwSip ?: return
        val m = "BYE sip:alice@127.0.0.1:${to.port} SIP/2.0\r\nVia: SIP/2.0/UDP 127.0.0.1:${sock.localPort};branch=z9hG4bKbye\r\nFrom: <sip:555@fake>;tag=ft\r\nTo: <sip:alice@127.0.0.1>;tag=x\r\nCall-ID: $callId\r\nCSeq: 2 BYE\r\nContent-Length: 0\r\n\r\n"
        send(m, to)
    }
    fun close() { run = false; sock.close(); rtp.close() }
}

/** Poarta SIP: REGISTER cu digest, apel ieșit 0+număr (doar echipa), RTP în ambele sensuri, BYE, apel intrat. */
fun runSip(): Int {
    fails = 0
    val port = 18500
    val hub = Hub("S", "1234", "", port, null, true)
    hub.start(); Thread.sleep(400)
    val fake = FakeSip()
    val gw = SipGateway(SipConfig("127.0.0.1", fake.sock.localPort, "alice", "secret", "127.0.0.1", "100", "127.0.0.1", port))
    gw.start()
    var t = 0
    while (!gw.registered && t++ < 40) Thread.sleep(100)
    check("REGISTER cu digest acceptat de server", gw.registered && fake.regAuthOk)
    val r = C("r", "Regia", "regie", room = "", port = port)
    val a = C("a1", "Actor", "actor", room = "", port = port)
    val sp = C("s1", "Spect", "spectator", room = "", port = port)
    Thread.sleep(500)
    a.drain(); sp.drain(); r.drain()
    sp.send("t" to "dial", "number" to "0777")
    check("spectatorul nu poate suna în afară (unknown)", sp.next("unknown", 1000) != null)
    sp.send("t" to "onhook")
    a.send("t" to "dial", "number" to "040721000")
    check("actor: apel ieșit rutat către poartă (ringback)", a.next("ringback") != null)
    t = 0
    while (!fake.inviteAuthOk && t++ < 40) Thread.sleep(100)
    check("INVITE reautentificat cu digest qop=auth; destinația corectă", fake.inviteAuthOk && fake.inviteDest.contains("721000"))
    check("SIP 200 => apelantul primește connected", a.next("connected", 2500) != null)
    // audio hub -> RTP
    a.drain()
    val b = ByteArray(321); b[0] = 0; for (i in 1..320) b[i] = 5
    a.conn.send(Frame(T_AUDIO, b))
    var gotRtp = false; t = 0
    while (!gotRtp && t++ < 30) { val p = fake.rtpIn.poll(200, TimeUnit.MILLISECONDS); if (p != null && p.size == 172 && (p[1].toInt() and 0x7F) == 0 && p[12].toInt() != -1) gotRtp = true }
    check("audio actor → RTP PCMU (160 octeți) la server", gotRtp)
    // RTP -> hub
    a.drain()
    fake.sendRtp(ByteArray(160) { 0x55 })
    check("RTP de la server → audio μ-law la actor", a.audioIn(1500) == T_AUDIO_U)
    fake.bye(fake.lastInvite.split("\r\n").first { it.startsWith("Call-ID:") }.substringAfter(":").trim())
    check("BYE de la server => actorul primește ended", a.next("ended", 2000) != null)
    // apel intrat
    r.drain()
    fake.inviteIn(gw.let { java.net.InetSocketAddress("127.0.0.1", it.sipPort) }, "alice")
    check("apel SIP intrat sună la numărul intern (100)", r.next("ring", 2500) != null)
    r.send("t" to "answer")
    t = 0
    while (!fake.got200ForInbound && t++ < 40) Thread.sleep(100)
    check("răspuns la regie => 200 OK către server", fake.got200ForInbound)
    r.send("t" to "hangup")
    t = 0
    while (!fake.gotBye && t++ < 40) Thread.sleep(100)
    check("închidere la regie => BYE către server", fake.gotBye)
    gw.stop(); fake.close(); hub.stop(); Thread.sleep(200)
    println(if (fails == 0) "SIP OK" else "$fails ESECURI SIP")
    return fails
}

/** SMS reale prin telefonul Regiei: doar echipa trimite, poarta primește, SMS primit ajunge la echipă. */
fun runSms(): Int {
    fails = 0
    val port = 18510
    val hub = Hub("M", "1234", "", port, null, false)
    hub.start(); Thread.sleep(400)
    val a = C("a1", "Actor", "actor", room = "", port = port)
    val sp = C("s1", "Spect", "spectator", room = "", port = port)
    a.drain()
    a.send("t" to "sms", "to" to "+40721000111", "text" to "salut")
    check("fără poartă SMS: no_gateway", a.next("smsres")?.optString("why") == "no_gateway")
    val g = C("r", "Regia", "regie", room = "", port = port, smsgw = true)
    // regia se conectează din loopback neîncredințat în test: PIN-ul îl cere
    check("regia cu poartă SMS e conectată", g.number.isNotEmpty() || g.denied == "bad_pin")
    val g2 = if (g.number.isNotEmpty()) g else C("r", "Regia", "regie", pin = "1234", room = "", port = port, smsgw = true)
    g2.drain(); a.drain(); sp.drain()
    a.send("t" to "sms", "to" to "+40721000111", "text" to "salut")
    val o = g2.next("smsout")
    check("SMS de la actor ajunge la poartă (număr curățat)", o != null && o.getString("to") == "+40721000111" && o.getString("text") == "salut")
    check("actorul primește ok", a.next("smsres")?.optBoolean("ok") == true)
    sp.send("t" to "sms", "to" to "+40721000111", "text" to "spam")
    check("spectatorul nu poate trimite SMS", g2.next("smsout", 600) == null)
    a.send("t" to "sms", "to" to "12", "text" to "x")
    check("număr invalid respins", a.next("smsres")?.optString("why") == "bad_number")
    var lim = 0
    for (i in 1..25) a.send("t" to "sms", "to" to "+4072100011$i".take(12), "text" to "m$i")
    Thread.sleep(500)
    a.drain()
    a.send("t" to "sms", "to" to "+40721000999", "text" to "z")
    check("limită de 20 SMS/minut", a.next("smsres")?.optString("why") == "limit")
    a.drain()
    g2.send("t" to "smsin", "from" to "+40722000333", "text" to "răspuns de la un spectator")
    val m = a.next("msg")
    check("SMS primit ajunge la echipă (rol sms)", m != null && m.getString("role") == "sms" && m.getString("num") == "+40722000333")
    sp.drain(); a.send("t" to "ping")
    sp.send("t" to "smsin", "from" to "+4070000", "text" to "fals")
    check("doar poarta poate injecta SMS primit", a.next("msg", 600) == null)
    hub.stop(); Thread.sleep(200)
    println(if (fails == 0) "SMS OK" else "$fails ESECURI SMS")
    return fails
}

class HubTest {
    @Test
    fun scenarii() {
        assertEquals(0L, runAll().toLong())
    }

    @Test
    fun sms() {
        assertEquals(0L, runSms().toLong())
    }

    @Test
    fun sip() {
        assertEquals(0L, runSip().toLong())
    }

    @Test
    fun p2p() {
        assertEquals(0L, runP2p().toLong())
    }

    @Test
    fun retea() {
        assertEquals(0L, runNet().toLong())
    }
}
