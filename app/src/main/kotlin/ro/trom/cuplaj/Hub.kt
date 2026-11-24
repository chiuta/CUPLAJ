package ro.trom.cuplaj

import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Timer
import java.util.TimerTask

class Peer(val id: String) {
    @Volatile
    var conn: Conn? = null
    var name = ""
    var role = "spectator"
    var number = ""
    var group = ""
    var tok = ""
    var line = 0          // 0 = linie proprie; altfel id-ul liniei cuplate
    var slot = 0          // 0 / 1 = poziția pe linia cuplată (decide soneria distinctă)
    var src = 0           // indexul sursei audio
    var callId = 0
    var listening = false // ascultă o convorbire a vecinului
    var talking = false   // intervine în convorbire
    var silent = false    // ascultare discretă (regie)
    var offhook = false
    var muted = false
    var bat = -1
    var zone = 0           // zona de spectacol auzită (0 = nicio zonă)
    var zoneAt = 0L
    var fx = ""            // efect de voce atribuit de regie
    var p2p = false        // clientul știe P2P direct
    var local = false      // telefonul Regiei însuși (conexiune loopback)
    var udp: java.net.InetSocketAddress? = null  // adresa UDP văzută de hub (după NAT)
    var smsgw = false      // telefonul Regiei trimite/primește SMS-uri reale
    var lan = ""            // ip:port local raportat de client
    @Volatile
    var seen = System.currentTimeMillis() // ultima activitate (pentru eliminarea celor deconectate când hubul e plin)
    private var winStart = 0L
    private var jsonCnt = 0
    private var audCnt = 0
    val online: Boolean get() = conn?.open == true

    /** Limitare de flux: kind 0 = JSON, 1 = audio. */
    fun allow(kind: Int, now: Long): Boolean {
        if (now - winStart >= 1000) {
            winStart = now
            jsonCnt = 0
            audCnt = 0
        }
        return if (kind == 0) ++jsonCnt <= 40 else ++audCnt <= 90
    }
}

class Call(val id: Int, var a: Peer, var b: Peer) {
    var connected = false
    val t0 = System.currentTimeMillis()
    var tConn = 0L
    val listeners = ArrayList<Peer>()
    var p2pKey: ByteArray? = null
    fun members(): List<Peer> = listOf(a, b)
}

/**
 * Hubul rulează pe telefonul Regiei. Este centrala: numere, apeluri, linii cuplate, mesaje, indicii, rutare audio.
 * Toată starea se modifică sub un singur lacăt.
 */
class Hub(
    hubName: String,
    private val pin: String,
    private val room: String = "",
    private val port: Int = PORT,
    restore: String? = null,
    private val trustLoopback: Boolean = true,
    netKey: String = ""
) {
    private val key: ByteArray? = Sec.deriveKey(netKey)

    /** Liniște maximă pe o conexiune (clienții trimit ping la 15 s), în ms. */
    @Volatile
    var idleTimeoutMs = 60000
    private val hubName = hubName.replace('|', ' ').take(40)
    private val lock = Any()
    private val peers = LinkedHashMap<String, Peer>()
    private val calls = HashMap<Int, Call>()
    private var callSeq = 0
    private var lineSeq = 0
    private var srcSeq = 0
    private var nReg = 0
    private var nAct = 0
    private var nSpec = 0
    private var allowListen = true
    private var clickOnJoin = true
    private var peerMsg = true

    /** 0 = fără P2P, 1 = doar în aceeași rețea locală, 2 = oriunde (își arată adresele IP între ei). */
    @Volatile
    var p2pMode = 1
    private var udpSock: java.net.DatagramSocket? = null
    private val log = ArrayList<String>()
    private val rnd = java.security.SecureRandom()

    /** Timpul de sonerie fără răspuns, în ms (public pentru teste). */
    @Volatile
    var ringTimeoutMs = 40000L

    /** Apelat (sub lacăt) cu starea serializată, ca să poată fi salvată. */
    @Volatile
    var onPersist: ((String) -> Unit)? = null

    @Volatile
    var error: String? = null

    @Volatile
    private var server: ServerSocket? = null

    @Volatile
    private var running = false
    private var timer: Timer? = null

    init {
        if (!restore.isNullOrEmpty()) {
            try {
                restoreState(JSONObject(restore))
            } catch (_: Exception) {
            }
        }
    }

    fun start() {
        if (running) return
        running = true
        Thread { acceptLoop() }.apply { isDaemon = true; start() }
        Thread { beaconLoop() }.apply { isDaemon = true; start() }
        Thread { rendezvousLoop() }.apply { isDaemon = true; start() }
        val t = Timer(true)
        t.schedule(object : TimerTask() {
            override fun run() {
                synchronized(lock) { sweep() }
            }
        }, 1000L, 1000L)
        timer = t
    }

    fun stop() {
        running = false
        try {
            udpSock?.close()
        } catch (_: Exception) {
        }
        timer?.cancel()
        try {
            server?.close()
        } catch (_: Exception) {
        }
        synchronized(lock) { peers.values.forEach { it.conn?.close() } }
    }

    // ------------------------------------------------------------------ persistență

    private fun exportState(): String {
        val arr = JSONArray()
        for (q in peers.values) {
            arr.put(
                jo(
                    "id" to q.id, "name" to q.name, "role" to q.role, "number" to q.number, "group" to q.group,
                    "tok" to q.tok, "line" to q.line, "slot" to q.slot, "src" to q.src, "muted" to q.muted
                )
            )
        }
        return jo(
            "peers" to arr, "nReg" to nReg, "nAct" to nAct, "nSpec" to nSpec, "lineSeq" to lineSeq, "srcSeq" to srcSeq,
            "listen" to allowListen, "click" to clickOnJoin, "peerMsg" to peerMsg, "p2p" to p2pMode
        ).toString()
    }

    private fun restoreState(o: JSONObject) {
        nReg = o.optInt("nReg")
        nAct = o.optInt("nAct")
        nSpec = o.optInt("nSpec")
        lineSeq = o.optInt("lineSeq")
        srcSeq = o.optInt("srcSeq")
        allowListen = o.optBoolean("listen", true)
        p2pMode = o.optInt("p2p", 1)
        clickOnJoin = o.optBoolean("click", true)
        peerMsg = o.optBoolean("peerMsg", true)
        val arr = o.optJSONArray("peers") ?: return
        for (i in 0 until arr.length()) {
            val j = arr.getJSONObject(i)
            val p = Peer(j.getString("id"))
            p.name = j.optString("name")
            p.role = j.optString("role", "spectator")
            p.number = j.optString("number")
            p.group = j.optString("group")
            p.tok = j.optString("tok")
            p.line = j.optInt("line")
            p.slot = j.optInt("slot")
            p.src = j.optInt("src")
            p.muted = j.optBoolean("muted")
            peers[p.id] = p
        }
    }

    private fun persist() {
        val cb = onPersist ?: return
        try {
            cb(exportState())
        } catch (_: Exception) {
        }
    }

    // ------------------------------------------------------------------ jurnal

    private fun logEvent(s: String) {
        val line = SimpleDateFormat("HH:mm:ss").format(Date()) + "  " + s
        log.add(line)
        while (log.size > 300) log.removeAt(0)
        val j = jo("t" to "log", "line" to line)
        for (q in peers.values) if (q.role == "regie" && q.online) sendTo(q, j)
    }

    private fun label(p: Peer) = p.name + " (" + p.number + ")"

    // ------------------------------------------------------------------ rețea

    private fun acceptLoop() {
        try {
            val ss = ServerSocket()
            ss.reuseAddress = true
            ss.bind(InetSocketAddress(port))
            server = ss
            while (running) {
                try {
                    val s = ss.accept()
                    adopt(SocketChan(s))
                } catch (e: SocketException) {
                    if (!running) break
                    Thread.sleep(100)
                } catch (_: Exception) {
                    Thread.sleep(100)
                }
            }
        } catch (e: Exception) {
            error = e.message ?: "bind"
        }
    }

    private fun beaconLoop() {
        var ds: DatagramSocket? = null
        try {
            val d = DatagramSocket()
            ds = d
            d.broadcast = true
            val msg = "CUPLAJ1|$hubName|$port|" + (if (room.isEmpty()) "0" else "1") + "|" + (if (key == null) "0" else "1")
            val bytes = msg.toByteArray(Charsets.UTF_8)
            while (running) {
                try {
                    val targets = ArrayList<InetAddress>()
                    targets.add(InetAddress.getByName("255.255.255.255"))
                    val en = NetworkInterface.getNetworkInterfaces()
                    while (en != null && en.hasMoreElements()) {
                        val ni = en.nextElement()
                        if (!ni.isUp || ni.isLoopback) continue
                        for (ia in ni.interfaceAddresses) {
                            val b = ia.broadcast
                            if (b != null) targets.add(b)
                        }
                    }
                    for (t in targets) {
                        try {
                            d.send(DatagramPacket(bytes, bytes.size, t, BEACON_PORT))
                        } catch (_: Exception) {
                        }
                    }
                } catch (_: Exception) {
                }
                Thread.sleep(2000)
            }
        } catch (_: Exception) {
        } finally {
            ds?.close()
        }
    }

    private fun deny(c: Conn, code: String) {
        c.startWriter()
        c.sendJson(jo("t" to "denied", "why" to code))
        try {
            Thread.sleep(300)
        } catch (_: Exception) {
        }
        c.close()
    }

    private fun clean(s: String, n: Int): String = s.filter { it >= ' ' }.trim().take(n)

    // ------------------------------------------------------------------ apărare împotriva abuzului

    private class Fail {
        var n = 0
        var first = 0L
        var until = 0L
    }

    private val fails = ConcurrentHashMap<String, Fail>()
    private val conns = ConcurrentHashMap<String, AtomicInteger>()
    private val preauth = AtomicInteger()

    /** Doar pentru teste: aplică blocarea și originii loopback. */
    var lockLoopback = false

    private fun blocked(o: String): Boolean = (fails[o]?.until ?: 0L) > System.currentTimeMillis()

    /** Cinci–șase încercări eșuate într-un minut blochează originea un minut. */
    private fun failed(o: String) {
        val now = System.currentTimeMillis()
        if (fails.size > 2000) fails.clear()
        val f = fails.getOrPut(o) { Fail() }
        synchronized(f) {
            if (now - f.first > 60_000) {
                f.n = 0
                f.first = now
            }
            f.n++
            if (f.n >= 6) f.until = now + 60_000
        }
    }

    private fun sameSecret(a: String, b: String): Boolean =
        MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))

    /** Preia un canal nou (TCP, Bluetooth…) și îl tratează într-un fir propriu, cu limite pe origine și pe conexiunile neautentificate. */
    fun adopt(ch: Chan) {
        val o = ch.origin
        val loop = ch.loopback
        if ((!loop || lockLoopback) && blocked(o)) {
            ch.close()
            return
        }
        val cnt = conns.getOrPut(o) { AtomicInteger() }
        if (preauth.get() >= 64 || (!loop && cnt.get() >= 60)) {
            ch.close()
            return
        }
        cnt.incrementAndGet()
        Thread {
            try {
                handle(ch)
            } finally {
                cnt.decrementAndGet()
            }
        }.apply { isDaemon = true; start() }
    }

    private fun handle(ch: Chan) {
        val c = Conn(ch, key, true)
        var me: Peer? = null
        val origin = ch.origin
        val loop = ch.loopback
        var counted = true
        preauth.incrementAndGet()
        try {
            ch.setIdleTimeout(10000)
            if (!c.serverHandshake()) {
                // fie hubul are cheie și clientul nu, fie invers: refuz în clar, ca utilizatorul să afle motivul
                if (!loop || lockLoopback) failed(origin)
                c.startWriter()
                c.sendJsonPlain(jo("t" to "denied", "why" to (if (key == null) "no_key" else "bad_key")))
                Thread.sleep(300)
                c.close()
                return
            }
            val f = c.readFrame()
            if (f.bad) {
                if (!loop || lockLoopback) failed(origin)
                c.startWriter()
                c.sendJsonPlain(jo("t" to "denied", "why" to "bad_key"))
                Thread.sleep(300)
                c.close()
                return
            }
            if (f.type != T_JSON) {
                c.close()
                return
            }
            val m = parseJson(f)
            if (m.optString("t") != "hello") {
                c.close()
                return
            }
            val id = clean(m.getString("id"), 64)
            if (id.isEmpty()) {
                c.close()
                return
            }
            var role = m.optString("role", "spectator")
            val local = trustLoopback && ch.loopback
            if (!local && room.isNotEmpty() && !sameSecret(m.optString("room"), room)) {
                if (!loop || lockLoopback) failed(origin)
                Thread.sleep(1000)
                deny(c, "bad_room")
                return
            }
            if (!local && (role == "actor" || role == "regie" || role == "gateway") && !sameSecret(m.optString("pin"), pin)) {
                if (!loop || lockLoopback) failed(origin)
                Thread.sleep(1000) // frânează ghicirea PIN‑ului, inclusiv de pe același telefon
                deny(c, "bad_pin")
                return
            }
            ch.setIdleTimeout(idleTimeoutMs)
            c.maxFrame = 32768
            if (counted) {
                counted = false
                preauth.decrementAndGet()
            }
            if (role != "regie" && role != "actor" && role != "gateway") role = "spectator"
            c.startWriter()
            var refused: String? = null
            synchronized(lock) {
                var q = peers[id]
                if (q == null && peers.size >= 400) {
                    // plin: face loc eliminând cel mai vechi spectator deconectat, altfel refuză
                    val old = peers.values.filter { !it.online && it.role == "spectator" && it.line == 0 }.minByOrNull { it.seen }
                    if (old != null) peers.remove(old.id)
                }
                if (q == null && peers.size >= 400) {
                    refused = "full"
                } else if (q != null && !local && q.tok.isNotEmpty() && q.tok != m.optString("tok")) {
                    refused = "dup"
                } else {
                    if (q == null) {
                        q = Peer(id)
                        peers[id] = q
                        srcSeq++
                        q.src = srcSeq % 250 + 1
                    }
                    q.conn?.close()
                    if (q.number.isEmpty() || q.role != role) {
                        q.role = role
                        q.number = newNumber(role)
                    }
                    if (q.tok.isEmpty()) q.tok = newToken()
                    q.name = clean(m.optString("name", "?"), 40).ifEmpty { "?" }
                    q.p2p = m.optBoolean("p2p")
                    q.smsgw = role == "regie" && m.optBoolean("smsgw")
                    q.local = local || (loop && (role == "regie" || role == "gateway"))
                    q.udp = null
                    q.lan = ""
                    q.conn = c
                    me = q
                    sendTo(q, welcome(q))
                    pushLine(q)
                    if (q.role == "regie") for (l in log) sendTo(q, jo("t" to "log", "line" to l))
                    logEvent("+ " + label(q))
                    broadcastPeers()
                    persist()
                }
            }
            val why = refused
            if (why != null) {
                c.sendJson(jo("t" to "denied", "why" to why))
                Thread.sleep(300)
                c.close()
                return
            }
            while (true) {
                val fr = c.readFrame()
                if (fr.bad) break
                val who = me ?: break
                val now = System.currentTimeMillis()
                if (fr.type == T_JSON) {
                    val jm = parseJson(fr)
                    synchronized(lock) { if (who.allow(0, now)) onJson(who, jm) }
                } else if ((fr.type == T_AUDIO || fr.type == T_AUDIO_U) && fr.data.size > 1) {
                    synchronized(lock) { if (who.allow(1, now)) onAudio(who, fr.data, fr.type) }
                }
            }
        } catch (_: Throwable) {
        } finally {
            if (counted) preauth.decrementAndGet()
            val q = me
            if (q != null) {
                q.seen = System.currentTimeMillis(); q.zone = 0
                synchronized(lock) {
                    if (q.conn === c) {
                        hangup(q)
                        q.conn = null
                        logEvent("− " + label(q))
                        broadcastPeers()
                    }
                }
            }
            c.close()
        }
    }

    // ------------------------------------------------------------------ utilitare

    private fun isStaff(p: Peer) = p.role == "regie" || p.role == "actor"

    private fun newToken(): String {
        val b = ByteArray(12)
        rnd.nextBytes(b)
        return b.joinToString("") { String.format("%02x", it) }
    }

    private fun newNumber(role: String): String {
        while (true) {
            val n = when (role) {
                "regie" -> "10" + (nReg++)
                "gateway" -> "90"
                "actor" -> "2" + String.format("%02d", ++nAct)
                else -> "21" + String.format("%04d", 1000 + (++nSpec))
            }
            if (peers.values.none { it.number == n }) return n
        }
    }

    private class Poll(val id: String, val q: String, val opts: List<String>, val rec: Set<String>) {
        val votes = ConcurrentHashMap<String, Int>()
    }

    @Volatile
    private var poll: Poll? = null
    private var pollSeq = 0

    private fun pollJson(pl: Poll, open: Boolean): JSONObject {
        val counts = IntArray(pl.opts.size)
        for (v in pl.votes.values) if (v in counts.indices) counts[v]++
        val ar = JSONArray()
        for (c in counts) ar.put(c)
        val os = JSONArray()
        for (o in pl.opts) os.put(o)
        return jo("t" to "pollres", "id" to pl.id, "q" to pl.q, "opts" to os, "counts" to ar, "n" to pl.votes.size, "of" to pl.rec.size, "open" to open)
    }

    private fun pushPoll(pl: Poll, open: Boolean) {
        val j = pollJson(pl, open)
        for (q in peers.values) if (isStaff(q) && q.online) sendTo(q, j)
    }

    // ---- reguli, zone, contor de public, efecte de voce (v1.7)
    @Volatile
    private var rules: List<Rule> = emptyList()
    private val fired = ConcurrentHashMap<String, Long>()

    private class Meter(val id: String, val kind: String, val rec: Set<String>) {
        val vals = ConcurrentHashMap<String, Int>()
    }

    @Volatile
    private var meter: Meter? = null
    private var meterSeq = 0

    private fun fire(r: Rule, who: Peer?) {
        val key = r.id + ":" + (who?.id ?: "")
        val now = System.currentTimeMillis()
        val prev = fired[key]
        if (prev != null && (r.once || now - prev < 3000)) return
        fired[key] = now
        val to = if (r.to == "self") (if (who != null && who.online) listOf(who) else emptyList()) else targets(r.to, null).filter { it.role != "regie" || r.kind == "goto" }
        if (r.kind == "goto") {
            for (q in peers.values) if (q.role == "regie" && q.online) sendTo(q, jo("t" to "goto", "step" to r.text))
        } else {
            val cue = jo("t" to "cue", "kind" to r.kind, "text" to r.text, "from" to "rule")
            for (q in to) sendTo(q, cue)
        }
        logEvent("regulă " + r.id + " (" + r.on + ") → " + r.to + " " + r.kind + (if (who != null) " [" + label(who) + "]" else ""))
    }

    private fun fireRules(on: String, who: Peer?, arg: Int, mk: String = "", v: Int = 0) {
        for (r in rules) {
            if (r.on != on) continue
            val ok = when (on) {
                "zone", "pollwin" -> r.arg == arg
                "meter" -> r.meterKind == mk && v >= r.arg
                else -> false
            }
            if (ok) fire(r, who)
        }
    }

    private fun meterJson(m: Meter, open: Boolean): JSONObject {
        val v = m.vals.values
        return jo("t" to "meterres", "id" to m.id, "kind" to m.kind, "avg" to (if (v.isEmpty()) 0 else v.sum() / v.size), "max" to (v.maxOrNull() ?: 0), "n" to v.size, "of" to m.rec.size, "open" to open)
    }

    private fun closeMeter(m: Meter) {
        if (meter !== m) return
        meter = null
        val j = meterJson(m, false)
        for (q in peers.values) if (isStaff(q) && q.online) sendTo(q, j)
        if (m.vals.isNotEmpty()) fireRules("meter", null, 0, m.kind, j.getInt("avg"))
        logEvent("contor " + m.kind + ": medie " + j.getInt("avg") + ", max " + j.getInt("max") + ", " + m.vals.size + "/" + m.rec.size)
    }

    private fun welcome(q: Peer) = jo(
        "t" to "welcome", "number" to q.number, "name" to q.name, "src" to q.src,
        "hub" to hubName, "role" to q.role, "tok" to q.tok, "group" to q.group, "fx" to q.fx
    )

    private fun sendTo(p: Peer, o: JSONObject) {
        p.conn?.sendJson(o)
    }

    private fun partnerOf(p: Peer): Peer? =
        if (p.line == 0) null else peers.values.firstOrNull { it !== p && it.line == p.line }

    private fun pushLine(p: Peer) {
        sendTo(p, jo("t" to "line", "line" to p.line, "slot" to p.slot, "partner" to (partnerOf(p)?.number ?: "")))
    }

    private fun targets(to: String, from: Peer?): List<Peer> {
        val on = peers.values.filter { it.online && it !== from }
        return when {
            to == "all" -> on
            to == "staff" -> on.filter { isStaff(it) }
            to == "spectators" -> on.filter { it.role == "spectator" }
            to == "actors" -> on.filter { it.role == "actor" }
            to.startsWith("num:") -> on.filter { it.number == to.substring(4) }
            to.startsWith("id:") -> on.filter { it.id == to.substring(3) }
            to.startsWith("grp:") -> on.filter { it.group.isNotEmpty() && it.group == to.substring(4) }
            else -> emptyList()
        }
    }

    private fun peersJson(): JSONObject {
        val arr = JSONArray()
        for (q in peers.values) {
            arr.put(
                jo(
                    "id" to q.id, "name" to q.name, "role" to q.role, "number" to q.number, "group" to q.group,
                    "line" to q.line, "slot" to q.slot, "online" to q.online, "muted" to q.muted, "bat" to q.bat,
                    "zone" to q.zone, "fx" to q.fx,
                    "busy" to (q.callId != 0 || q.offhook),
                    "mon" to (q.callId != 0 && calls[q.callId]?.connected == true)
                )
            )
        }
        return jo("t" to "peers", "list" to arr, "listen" to allowListen, "click" to clickOnJoin, "peerMsg" to peerMsg, "p2p" to p2pMode)
    }

    private fun broadcastPeers() {
        val j = peersJson()
        for (q in peers.values) if (isStaff(q) && q.online) sendTo(q, j)
    }

    // ------------------------------------------------------------------ mesaje JSON

    private fun onJson(p: Peer, m: JSONObject) {
        val reg = p.role == "regie"
        when (m.optString("t")) {
            "msg" -> {
                var to = m.optString("to", "all")
                if (!isStaff(p) && to == "all") to = "staff"
                val out = jo(
                    "t" to "msg", "from" to p.name, "num" to p.number, "role" to p.role,
                    "text" to m.optString("text").take(500), "to" to to
                )
                var list = targets(to, p)
                if (p.role == "spectator" && !peerMsg) list = list.filter { isStaff(it) }
                for (r in list) sendTo(r, out)
            }
            "cue" -> if (reg) {
                m.put("from", p.number)
                val list = targets(m.optString("to", "all"), p)
                for (r in list) sendTo(r, m)
                logEvent("cue " + m.optString("kind") + " → " + m.optString("to"))
                val cid = clean(m.optString("id"), 16)
                if (cid.isNotEmpty()) sendTo(p, jo("t" to "cueack", "id" to cid, "sent" to list.size, "online" to peers.values.count { it.online && it !== p }))
            }
            "media" -> if (reg) {
                val mid = m.optString("mid")
                if (Media.validId(mid) && m.optString("kind") in Media.KINDS && m.optString("b64").length <= Media.MAX_B64 && m.optInt("i", -1) >= 0) {
                    val out = jo("t" to "media", "mid" to mid, "kind" to m.optString("kind"), "i" to m.optInt("i"), "n" to m.optInt("n"), "b64" to m.optString("b64"))
                    if (m.optInt("i") == 0) { out.put("size", m.optLong("size")); out.put("sha", m.optString("sha")) }
                    for (r in targets(m.optString("to", "all"), p)) sendTo(r, out)
                    if (m.optInt("i") == 0) logEvent("media " + m.optString("kind") + " " + mid + " → " + m.optString("to"))
                }
            }
            "rules" -> if (reg) {
                rules = Rules.fromJson(m.optJSONArray("list"))
                fired.clear()
                sendTo(p, jo("t" to "rulesok", "n" to rules.size))
            }
            "zone" -> if (!reg) {
                val z = m.optInt("z", -1)
                val now = System.currentTimeMillis()
                if (z in 0..99 && z != p.zone && now - p.zoneAt > 2000) {
                    p.zone = z
                    p.zoneAt = now
                    logEvent(label(p) + " → zona " + z)
                    broadcastPeers()
                    if (z > 0) fireRules("zone", p, z)
                }
            }
            "setfx" -> if (reg) {
                val q = peers[m.optString("id")]
                val k = m.optString("k")
                if (q != null && (k.isEmpty() || k in VoiceFx.KINDS)) {
                    q.fx = k
                    sendTo(q, jo("t" to "fx", "k" to k))
                    broadcastPeers()
                }
            }
            "meter" -> if (reg) {
                val kind = m.optString("kind")
                val secs = m.optInt("secs", 5).coerceIn(1, 30)
                if (kind == "noise" || kind == "motion") {
                    val rec = targets(m.optString("to", "all"), p).filter { it.role != "regie" }
                    val mt = Meter("m" + (++meterSeq), kind, rec.map { it.id }.toSet())
                    meter = mt
                    val req = jo("t" to "meterreq", "id" to mt.id, "kind" to kind, "secs" to secs)
                    for (r in rec) sendTo(r, req)
                    Thread {
                        try { Thread.sleep(secs * 1000L + 3000) } catch (_: Exception) {}
                        closeMeter(mt)
                    }.apply { isDaemon = true; start() }
                    sendTo(p, meterJson(mt, true))
                }
            }
            "meterval" -> {
                val mt = meter
                val v = m.optInt("v", -1)
                if (mt != null && !reg && m.optString("id") == mt.id && p.id in mt.rec && v in 0..100) {
                    mt.vals[p.id] = v
                    val j = meterJson(mt, true)
                    for (q in peers.values) if (isStaff(q) && q.online) sendTo(q, j)
                }
            }
            "sync" -> sendTo(p, jo("t" to "sync", "c" to m.optLong("c"), "s" to System.currentTimeMillis()))
            "poll" -> if (reg) {
                val oa = m.optJSONArray("opts")
                val opts = ArrayList<String>()
                if (oa != null) for (i in 0 until minOf(oa.length(), 6)) clean(oa.optString(i), 40).let { if (it.isNotEmpty()) opts.add(it) }
                val q = clean(m.optString("q"), 120)
                if (opts.size >= 2 && q.isNotEmpty()) {
                    val rec = targets(m.optString("to", "all"), p).filter { !isStaff(it) || it.role == "actor" }
                    val id = clean(m.optString("id"), 16).ifEmpty { "p" + (++pollSeq) }
                    val pl = Poll(id, q, opts, rec.map { it.id }.toSet())
                    poll = pl
                    val os = JSONArray()
                    for (o in opts) os.put(o)
                    val out = jo("t" to "poll", "id" to id, "q" to q, "opts" to os)
                    for (r in rec) sendTo(r, out)
                    pushPoll(pl, true)
                    logEvent("sondaj \"" + q + "\" → " + rec.size)
                }
            }
            "vote" -> {
                val pl = poll
                val o = m.optInt("o", -1)
                if (pl != null && !reg && m.optString("id") == pl.id && p.id in pl.rec && o in pl.opts.indices) {
                    pl.votes[p.id] = o
                    pushPoll(pl, true)
                }
            }
            "pollend" -> if (reg) {
                val pl = poll
                if (pl != null) {
                    pushPoll(pl, false)
                    logEvent("sondaj încheiat: " + pl.votes.size + "/" + pl.rec.size)
                    val cnt = IntArray(pl.opts.size)
                    for (v in pl.votes.values) if (v in cnt.indices) cnt[v]++
                    val mx = cnt.maxOrNull() ?: 0
                    if (mx > 0 && cnt.count { it == mx } == 1) fireRules("pollwin", null, cnt.indexOf(mx))
                    poll = null
                }
            }
            "offhook" -> { offhook(p); broadcastPeers() }
            "onhook", "hangup" -> { hangup(p); broadcastPeers() }
            "dial" -> { dial(p, m.optString("number").filter { it.isDigit() }); broadcastPeers() }
            "answer" -> { answer(p); broadcastPeers() }
            "intervene" -> if (p.listening) p.talking = m.optBoolean("on")
            "monitor" -> if (reg) { monitor(p, m.optString("number")); broadcastPeers() }
            "couple" -> if (reg) couple(m.optString("a"), m.optString("b"))
            "uncouple" -> if (reg) uncouple(m.optString("id"))
            "autocouple" -> if (reg) autoCouple()
            "setnum" -> if (reg) setNum(m.optString("id"), m.optString("number").filter { it.isDigit() })
            "setgroup" -> if (reg) {
                val q = peers[m.optString("id")]
                if (q != null) {
                    q.group = clean(m.optString("group"), 24)
                    persist()
                    broadcastPeers()
                }
            }
            "mute" -> if (reg) {
                val q = peers[m.optString("id")]
                if (q != null) {
                    q.muted = m.optBoolean("on")
                    calls[q.callId]?.let { stopP2p(it) }
                    persist()
                    broadcastPeers()
                }
            }
            "forget" -> if (reg) forget(m.optString("id"))
            "ping" -> sendTo(p, jo("t" to "pong"))
            "udp" -> p.lan = clean(m.optString("lan"), 64).let { if (privateCand(it)) it else "" }
            "sms" -> if (isStaff(p)) smsOut(p, m)
            "smsin" -> if (p.smsgw) {
                val from = m.optString("from").filter { it.isDigit() || it == '+' }.take(20)
                val text = clean(m.optString("text"), 500)
                if (from.isNotEmpty() && text.isNotEmpty()) {
                    val out = jo("t" to "msg", "from" to "SMS", "num" to from, "role" to "sms", "text" to text, "to" to "staff")
                    for (q in peers.values) if (isStaff(q) && q.online) sendTo(q, out)
                    logEvent("SMS ← $from")
                }
            }
            "stat" -> {
                p.bat = m.optInt("bat", -1)
                broadcastPeers()
            }
            "getlog" -> if (reg) for (l in log) sendTo(p, jo("t" to "log", "line" to l))
            "opt" -> if (reg) {
                allowListen = m.optBoolean("listen", true)
                val pm = m.optInt("p2p", p2pMode).coerceIn(0, 2)
                if (pm == 0 && p2pMode != 0) for (c in calls.values) stopP2p(c)
                p2pMode = pm
                clickOnJoin = m.optBoolean("click", true)
                peerMsg = m.optBoolean("peerMsg", true)
                persist()
                broadcastPeers()
            }
            "peers" -> if (isStaff(p)) sendTo(p, peersJson())
        }
    }

    // ------------------------------------------------------------------ telefonie

    private fun reset(x: Peer) {
        x.callId = 0
        x.listening = false
        x.talking = false
        x.silent = false
        x.offhook = false
    }

    private fun joinListen(p: Peer, c: Call, silent: Boolean) {
        p.callId = c.id
        p.listening = true
        p.talking = false
        p.silent = silent
        p.offhook = true
        stopP2p(c)
        c.listeners.add(p)
        sendTo(p, jo("t" to "listening"))
        logEvent((if (silent) "monitor " else "ascultă ") + label(p) + " @ " + label(c.a) + "↔" + label(c.b))
        if (clickOnJoin && !silent) for (x in c.members()) sendTo(x, jo("t" to "click"))
    }

    /** Ridicarea receptorului: ton de apel, ocupat, sau — pe linie cuplată — ascultarea convorbirii vecinului. */
    private fun offhook(p: Peer) {
        if (p.callId != 0 || p.offhook) return
        val pa = partnerOf(p)
        val c = if (pa != null) calls[pa.callId] else null
        if (pa != null && c != null && c.connected && allowListen && !pa.listening) {
            joinListen(p, c, false)
        } else if (pa != null && (pa.callId != 0 || pa.offhook)) {
            sendTo(p, jo("t" to "busy"))
        } else {
            p.offhook = true
            sendTo(p, jo("t" to "dialtone"))
        }
    }

    private fun dial(p: Peer, number: String) {
        if (number.isEmpty() || number.length > 16 || p.callId != 0) return
        if (!p.offhook) {
            offhook(p)
            if (!p.offhook || p.callId != 0) return
        }
        // prefixul 0 = ieșire prin poarta SIP/VoIP (doar echipa; pe o linie reală costă bani)
        var dest = ""
        var t = peers.values.firstOrNull { it.number == number }
        if (number.length > 1 && number.startsWith("0")) {
            t = if (isStaff(p)) peers.values.firstOrNull { it.role == "gateway" && it.online } else null
            dest = number.substring(1)
        }
        if (t == null || !t.online || t === p) {
            sendTo(p, jo("t" to "unknown"))
            return
        }
        val tp = partnerOf(t)
        if (t.callId != 0 || t.offhook || (tp != null && (tp.callId != 0 || tp.offhook))) {
            sendTo(p, jo("t" to "busy"))
            return
        }
        callSeq++
        val c = Call(callSeq, p, t)
        calls[c.id] = c
        p.callId = c.id
        t.callId = c.id
        sendTo(p, jo("t" to "ringback"))
        sendTo(t, jo("t" to "ring", "num" to p.number, "name" to p.name, "pat" to t.slot, "who" to t.number, "dest" to dest))
        // pe linia cuplată sună și telefonul vecinului, cu aceeași cadență (a numărului chemat)
        if (tp != null && tp.online) {
            sendTo(tp, jo("t" to "ring", "num" to p.number, "name" to "", "pat" to t.slot, "who" to t.number, "party" to true))
        }
        logEvent("apel " + label(p) + " → " + label(t))
    }

    // ------------------------------------------------------------------ SMS prin rețeaua mobilă (telefonul Regiei)

    private var smsWinStart = 0L
    private var smsCount = 0

    private fun smsOut(p: Peer, m: JSONObject) {
        val to = m.optString("to").filter { it.isDigit() || it == '+' }
        val text = clean(m.optString("text"), 320)
        val gw = peers.values.firstOrNull { it.smsgw && it.online }
        val why = when {
            to.length !in 5..16 || text.isEmpty() -> "bad_number"
            gw == null -> "no_gateway"
            else -> {
                val now = System.currentTimeMillis()
                if (now - smsWinStart > 60000) { smsWinStart = now; smsCount = 0 }
                if (++smsCount > 20) "limit" else ""
            }
        }
        if (why.isNotEmpty()) {
            sendTo(p, jo("t" to "smsres", "ok" to false, "why" to why))
            return
        }
        sendTo(gw!!, jo("t" to "smsout", "to" to to, "text" to text))
        sendTo(p, jo("t" to "smsres", "ok" to true))
        logEvent("SMS → $to (" + label(p) + ")")
    }

    // ------------------------------------------------------------------ P2P direct (semnalizare)

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    private fun sameNet(a: java.net.InetSocketAddress, b: java.net.InetSocketAddress): Boolean {
        val x = a.address
        val y = b.address
        return x == y || (x.isSiteLocalAddress && y.isSiteLocalAddress)
    }

    /** Punctul de întâlnire UDP: clientul trimite „CUPLAJP|id|tok”, hubul îi notează adresa văzută (după NAT). */
    private fun rendezvousLoop() {
        var d: DatagramSocket? = null
        try {
            d = DatagramSocket(null)
            d.reuseAddress = true
            d.bind(InetSocketAddress(port + P2P_PORT_OFFSET))
            udpSock = d
            val buf = ByteArray(256)
            while (running) {
                val pk = DatagramPacket(buf, buf.size)
                d.receive(pk)
                val t = String(pk.data, 0, pk.length, Charsets.UTF_8).split("|")
                if (t.size == 3 && t[0] == "CUPLAJP") {
                    var ok = false
                    synchronized(lock) {
                        val q = peers[t[1]]
                        if (q != null && q.online && q.tok.isNotEmpty() && q.tok == t[2]) {
                            q.udp = InetSocketAddress(pk.address, pk.port)
                            ok = true
                        }
                    }
                    if (ok) {
                        val r = "CUPLAJP-OK".toByteArray()
                        d.send(DatagramPacket(r, r.size, pk.socketAddress))
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            d?.close()
        }
    }

    private fun tryP2p(c: Call) {
        if (p2pMode == 0 || c.listeners.isNotEmpty() || c.p2pKey != null) return
        val a = c.a
        val b = c.b
        if (!a.p2p || !b.p2p || a.local || b.local || a.muted || b.muted) return
        val ua = a.udp ?: return
        val ub = b.udp ?: return
        if (p2pMode == 1 && !sameNet(ua, ub)) return
        val key = ByteArray(16).also { rnd.nextBytes(it) }
        c.p2pKey = key
        fun cands(p: Peer, u: InetSocketAddress): JSONArray {
            val arr = JSONArray()
            if (p.lan.isNotEmpty()) arr.put(p.lan)
            arr.put((u.address.hostAddress ?: "") + ":" + u.port)
            return arr
        }
        sendTo(a, jo("t" to "p2p", "key" to hex(key), "cand" to cands(b, ub), "init" to true))
        sendTo(b, jo("t" to "p2p", "key" to hex(key), "cand" to cands(a, ua), "init" to false))
        logEvent("P2P " + label(a) + " ↔ " + label(b))
    }

    private fun stopP2p(c: Call) {
        if (c.p2pKey == null) return
        c.p2pKey = null
        sendTo(c.a, jo("t" to "p2pstop"))
        sendTo(c.b, jo("t" to "p2pstop"))
        logEvent("P2P oprit " + label(c.a) + " ↔ " + label(c.b))
    }

    private fun connect(c: Call, forNum: String) {
        c.connected = true
        c.tConn = System.currentTimeMillis()
        c.a.offhook = true
        c.b.offhook = true
        sendTo(c.a, jo("t" to "connected", "peer" to c.b.number, "name" to c.b.name))
        sendTo(c.b, jo("t" to "connected", "peer" to c.a.number, "name" to c.a.name, "forNum" to forNum))
        val pb = partnerOf(c.b)
        if (pb != null && pb.callId == 0) sendTo(pb, jo("t" to "stopring"))
        tryP2p(c)
        logEvent("conectat " + label(c.a) + " ↔ " + label(c.b) + if (forNum.isNotEmpty()) " (răspuns greșit, era pentru $forNum)" else "")
    }

    private fun answer(p: Peer) {
        val c = calls[p.callId]
        if (c != null) {
            if (c.b === p && !c.connected) connect(c, "")
            return
        }
        if (p.offhook) return
        // vecinul de pe linia cuplată a ridicat receptorul când suna pentru celălalt abonat
        val pa = partnerOf(p) ?: return
        val c2 = calls[pa.callId] ?: return
        if (c2.b === pa && !c2.connected) {
            val old = c2.b
            c2.b = p
            old.callId = 0
            p.callId = c2.id
            sendTo(old, jo("t" to "stopring"))
            connect(c2, old.number)
        }
    }

    private fun endCall(c: Call, by: Peer?, why: String) {
        calls.remove(c.id)
        val all = ArrayList<Peer>()
        all.add(c.a)
        all.add(c.b)
        all.addAll(c.listeners)
        val pb = partnerOf(c.b)
        for (x in all) {
            reset(x)
            sendTo(x, jo("t" to "ended", "why" to why, "by" to (by?.number ?: "")))
        }
        if (!c.connected && pb != null) sendTo(pb, jo("t" to "stopring"))
        val dur = if (c.connected) (System.currentTimeMillis() - c.tConn) / 1000 else 0
        logEvent("încheiat " + label(c.a) + " ↔ " + label(c.b) + " [" + why + (if (c.connected) ", ${dur}s" else "") + "]")
    }

    private fun hangup(p: Peer) {
        val c = calls[p.callId]
        if (c == null) {
            reset(p)
            return
        }
        if (p.listening) {
            c.listeners.remove(p)
            val wasSilent = p.silent
            reset(p)
            sendTo(p, jo("t" to "ended", "why" to "left"))
            if (clickOnJoin && !wasSilent) for (x in c.members()) sendTo(x, jo("t" to "click"))
            return
        }
        val rejected = !c.connected && p === c.b
        endCall(c, p, if (rejected) "rejected" else "hangup")
    }

    private fun sweep() {
        val now = System.currentTimeMillis()
        for (c in calls.values.toList()) {
            if (!c.connected && now - c.t0 > ringTimeoutMs) endCall(c, null, "noanswer")
        }
    }

    private fun monitor(p: Peer, number: String) {
        if (p.callId != 0) return
        val t = peers.values.firstOrNull { it.number == number } ?: return
        val c = calls[t.callId] ?: return
        if (!c.connected) return
        joinListen(p, c, true)
    }

    // ------------------------------------------------------------------ cuplaj (linii comune)

    private fun couple(a: String, b: String) {
        val pa = peers[a] ?: return
        val pb = peers[b] ?: return
        if (pa === pb) return
        detach(pa, false)
        detach(pb, false)
        lineSeq++
        pa.line = lineSeq
        pa.slot = 0
        pb.line = lineSeq
        pb.slot = 1
        pushLine(pa)
        pushLine(pb)
        logEvent("cuplaj " + label(pa) + " ⇄ " + label(pb))
        persist()
        broadcastPeers()
    }

    private fun detach(p: Peer, push: Boolean = true) {
        val pa = partnerOf(p)
        p.line = 0
        p.slot = 0
        if (pa != null) {
            pa.line = 0
            pa.slot = 0
            pushLine(pa)
        }
        if (push) pushLine(p)
    }

    private fun uncouple(id: String) {
        val p = peers[id] ?: return
        detach(p)
        persist()
        broadcastPeers()
    }

    private fun autoCouple() {
        val sp = peers.values.filter { it.role == "spectator" }
        for (x in peers.values) {
            x.line = 0
            x.slot = 0
        }
        var i = 0
        while (i + 1 < sp.size) {
            lineSeq++
            sp[i].line = lineSeq
            sp[i].slot = 0
            sp[i + 1].line = lineSeq
            sp[i + 1].slot = 1
            i += 2
        }
        for (x in peers.values) pushLine(x)
        logEvent("auto-perechi: " + (sp.size / 2))
        persist()
        broadcastPeers()
    }

    private fun setNum(id: String, number: String) {
        val p = peers[id] ?: return
        if (number.isEmpty() || number.length > 12) return
        if (peers.values.any { it !== p && it.number == number }) return
        p.number = number
        sendTo(p, welcome(p))
        persist()
        broadcastPeers()
    }

    private fun forget(id: String) {
        val p = peers[id] ?: return
        if (p.role == "regie" && p.online) return
        hangup(p)
        val pa = partnerOf(p)
        if (pa != null) {
            pa.line = 0
            pa.slot = 0
            pushLine(pa)
        }
        peers.remove(id)
        p.conn?.close()
        logEvent("eliminat " + label(p))
        persist()
        broadcastPeers()
    }

    // ------------------------------------------------------------------ audio

    private fun onAudio(p: Peer, data: ByteArray, type: Int) {
        if (p.muted) return
        val mode = data[0].toInt()
        val out = ByteArray(data.size)
        System.arraycopy(data, 1, out, 1, data.size - 1)
        out[0] = (if (mode == 1) Media_PA_SRC else p.src).toByte()
        val f = Frame(type, out)
        val dest: List<Peer> = when (mode) {
            0 -> {
                val c = calls[p.callId]
                if (c == null || !c.connected || (p.listening && !p.talking)) emptyList()
                else (c.members() + c.listeners).filter { it !== p }
            }
            1 -> if (isStaff(p)) peers.values.filter { it !== p } else emptyList()
            2 -> peers.values.filter { it !== p && isStaff(it) }
            else -> emptyList()
        }
        for (d in dest) d.conn?.send(f)
    }
}

/** Sursa audio rezervată anunțurilor PA (au prioritate: restul sunetelor se atenuează). */
const val Media_PA_SRC = 254
