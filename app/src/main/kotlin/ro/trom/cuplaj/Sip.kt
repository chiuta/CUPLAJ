package ro.trom.cuplaj

import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.LinkedBlockingQueue

class SipConfig(
    val server: String,
    val port: Int = 5060,
    val user: String,
    val pass: String,
    /** Domeniul din URI-uri (implicit adresa serverului). */
    val domain: String = "",
    /** Numărul intern care sună la un apel SIP primit. */
    val inbound: String = "100",
    val localIp: String = "",
    val hubPort: Int = PORT,
    val hubPin: String = "",
    val hubKey: String = "",
    val maxSeconds: Int = 900
)

private fun md5(s: String): String = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

private class SipCall(val callId: String, val outbound: Boolean) {
    var localTag = Sec.randomBytes(5).joinToString("") { "%02x".format(it) }
    var remoteTag = ""
    var cseq = 1
    var uri = ""            // URI-ul țintei (apel ieșit)
    var inviteReq: String? = null   // INVITE-ul primit (apel intrat), pentru răspunsuri
    var inviteFrom: InetSocketAddress? = null
    var authTried = false
    var answered = false
    var acked = false
    var remoteRtp: InetSocketAddress? = null
    var rtp: DatagramSocket? = null
    var seq = 0
    var ts = 0L
    val ssrc = Sec.randomBytes(4).fold(0L) { a, b -> (a shl 8) or (b.toLong() and 0xFF) }
    var branch = ""
    var lastTo = ""
    var contactUri = ""
    var started = 0L
    val out = LinkedBlockingQueue<ByteArray>(12)
}

/**
 * Poarta VoIP: un „telefon” special (rol gateway, număr 90) legat de hub prin loopback, care vorbește SIP (UDP) cu un furnizor/centrală
 * și RTP PCMU (G.711 μ-law, 8 kHz — exact formatul audio al aplicației).
 * Ieșire: echipa formează 0 + număr. Intrare: apelurile SIP sună la numărul intern din configurație.
 */
class SipGateway(private val cfg: SipConfig, private val log: (String) -> Unit = {}) {
    @Volatile
    private var run = false
    private var hub: Conn? = null
    private var sip: DatagramSocket? = null
    private var serverAddr: InetSocketAddress? = null
    private var myIp = "127.0.0.1"
    private val domain = cfg.domain.ifEmpty { cfg.server }
    private val lock = Any()
    private var call: SipCall? = null
    private var regCallId = "reg-" + Sec.randomBytes(6).joinToString("") { "%02x".format(it) }
    private var regCseq = 1
    private var regAuthTried = false

    @Volatile
    var registered = false
        private set

    val sipPort: Int get() = sip?.localPort ?: 0

    fun start() {
        run = true
        val sa = InetSocketAddress(InetAddress.getByName(cfg.server), cfg.port)
        serverAddr = sa
        myIp = cfg.localIp.ifEmpty {
            try {
                DatagramSocket().use { d -> d.connect(sa); d.localAddress.hostAddress ?: "127.0.0.1" }
            } catch (_: Exception) {
                "127.0.0.1"
            }
        }
        val s = DatagramSocket(0)
        s.soTimeout = 500
        sip = s
        Thread { sipLoop() }.apply { isDaemon = true; start() }
        Thread { hubLoop() }.apply { isDaemon = true; start() }
        Thread { timerLoop() }.apply { isDaemon = true; start() }
        register()
    }

    fun stop() {
        run = false
        synchronized(lock) { endSip("stop") }
        try {
            hub?.close()
        } catch (_: Exception) {
        }
        sip?.close()
    }

    // ------------------------------------------------------------------ hub

    private fun hubSend(o: JSONObject) {
        hub?.sendJson(o)
    }

    private fun hubLoop() {
        while (run) {
            try {
                val s = Socket("127.0.0.1", cfg.hubPort)
                s.soTimeout = 45000
                val c = Conn(s, Sec.deriveKey(cfg.hubKey), false)
                hub = c
                c.startWriter()
                c.sendJson(jo("t" to "hello", "id" to "sip-gateway", "name" to "SIP", "role" to "gateway", "pin" to cfg.hubPin))
                var lastPing = System.currentTimeMillis()
                while (run) {
                    val f = try {
                        c.readFrame()
                    } catch (_: java.net.SocketTimeoutException) {
                        c.sendJson(jo("t" to "ping"))
                        continue
                    }
                    if (f.bad) break
                    if (System.currentTimeMillis() - lastPing > 15000) {
                        c.sendJson(jo("t" to "ping"))
                        lastPing = System.currentTimeMillis()
                    }
                    if (f.type == T_JSON) onHub(parseJson(f))
                    else if ((f.type == T_AUDIO || f.type == T_AUDIO_U) && f.data.size > 1) {
                        val u = if (f.type == T_AUDIO_U) f.data.copyOfRange(1, f.data.size) else G711.encode(f.data, 1, f.data.size - 1)
                        val cl = call
                        if (cl != null && cl.answered) cl.out.offer(u)
                    }
                }
            } catch (_: Exception) {
            }
            hub?.close()
            if (run) Thread.sleep(1500)
        }
    }

    private fun onHub(m: JSONObject) {
        synchronized(lock) {
            when (m.optString("t")) {
                "ring" -> {
                    val dest = m.optString("dest")
                    if (dest.isEmpty()) {
                        hubSend(jo("t" to "hangup"))
                    } else if (call != null || !registered && !cfgAllowUnregistered) {
                        hubSend(jo("t" to "answer")); hubSend(jo("t" to "hangup"))
                    } else {
                        invite(dest)
                    }
                }
                "connected" -> {
                    val c = call
                    if (c != null && !c.outbound && !c.answered) accept200(c)
                }
                "ended", "busy", "unknown" -> {
                    val c = call
                    if (c != null) endSip(m.optString("t"))
                }
            }
        }
    }

    @Volatile
    var cfgAllowUnregistered = true

    // ------------------------------------------------------------------ SIP: mesaje

    private fun sendSip(text: String, to: InetSocketAddress? = serverAddr) {
        try {
            val b = text.toByteArray(Charsets.UTF_8)
            sip?.send(DatagramPacket(b, b.size, to))
        } catch (_: Exception) {
        }
    }

    private fun via(branch: String) = "Via: SIP/2.0/UDP $myIp:$sipPort;branch=$branch;rport"
    private fun newBranch() = "z9hG4bK" + Sec.randomBytes(6).joinToString("") { "%02x".format(it) }
    private val me get() = "sip:${cfg.user}@$domain"
    private val contact get() = "Contact: <sip:${cfg.user}@$myIp:$sipPort>"

    private fun hdr(msg: String, name: String): String {
        val n = name.lowercase()
        for (l in msg.split("\r\n")) {
            if (l.isEmpty()) break
            val i = l.indexOf(':')
            if (i > 0) {
                val k = l.substring(0, i).trim().lowercase()
                val short = mapOf("call-id" to "i", "from" to "f", "to" to "t", "via" to "v", "contact" to "m", "content-type" to "c")[n]
                if (k == n || k == short) return l.substring(i + 1).trim()
            }
        }
        return ""
    }

    private fun hdrAll(msg: String, name: String): List<String> {
        val out = ArrayList<String>()
        for (l in msg.split("\r\n")) {
            if (l.isEmpty()) break
            val i = l.indexOf(':')
            if (i > 0 && l.substring(0, i).trim().lowercase() == name.lowercase()) out.add(l.substring(i + 1).trim())
        }
        return out
    }

    private fun tag(h: String): String = Regex("tag=([^;>\\s]+)").find(h)?.groupValues?.get(1) ?: ""

    private fun body(msg: String): String {
        val i = msg.indexOf("\r\n\r\n")
        return if (i >= 0) msg.substring(i + 4) else ""
    }

    private fun auth(h: String, method: String, uri: String, proxy: Boolean): String {
        fun p(k: String) = Regex("$k=\"?([^\",]+)\"?").find(h)?.groupValues?.get(1) ?: ""
        val realm = p("realm")
        val nonce = p("nonce")
        val opaque = p("opaque")
        val qop = p("qop").split(",").map { it.trim() }.firstOrNull { it == "auth" } ?: ""
        val ha1 = md5("${cfg.user}:$realm:${cfg.pass}")
        val ha2 = md5("$method:$uri")
        val value: String
        if (qop.isNotEmpty()) {
            val cn = Sec.randomBytes(4).joinToString("") { "%02x".format(it) }
            val resp = md5("$ha1:$nonce:00000001:$cn:$qop:$ha2")
            value = "Digest username=\"${cfg.user}\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$uri\", response=\"$resp\", algorithm=MD5, qop=auth, nc=00000001, cnonce=\"$cn\"" + if (opaque.isNotEmpty()) ", opaque=\"$opaque\"" else ""
        } else {
            val resp = md5("$ha1:$nonce:$ha2")
            value = "Digest username=\"${cfg.user}\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$uri\", response=\"$resp\", algorithm=MD5" + if (opaque.isNotEmpty()) ", opaque=\"$opaque\"" else ""
        }
        return (if (proxy) "Proxy-Authorization: " else "Authorization: ") + value + "\r\n"
    }

    // ------------------------------------------------------------------ SIP: înregistrare

    private fun register(authHdr: String = "") {
        val b = newBranch()
        val m = "REGISTER sip:$domain SIP/2.0\r\n" + via(b) + "\r\nMax-Forwards: 70\r\n" +
            "From: <$me>;tag=reg1\r\nTo: <$me>\r\nCall-ID: $regCallId\r\nCSeq: ${regCseq++} REGISTER\r\n" +
            contact + "\r\nExpires: 300\r\nUser-Agent: CUPLAJ\r\n" + authHdr + "Content-Length: 0\r\n\r\n"
        sendSip(m)
    }

    // ------------------------------------------------------------------ SIP: apel ieșit

    private fun sdp(port: Int) = "v=0\r\no=cuplaj 1 1 IN IP4 $myIp\r\ns=CUPLAJ\r\nc=IN IP4 $myIp\r\nt=0 0\r\nm=audio $port RTP/AVP 0\r\na=rtpmap:0 PCMU/8000\r\na=ptime:20\r\na=sendrecv\r\n"

    private fun inviteMsg(c: SipCall, authHdr: String): String {
        val rtpPort = c.rtp!!.localPort
        val b = sdp(rtpPort)
        c.branch = newBranch()
        return "INVITE ${c.uri} SIP/2.0\r\n" + via(c.branch) + "\r\nMax-Forwards: 70\r\n" +
            "From: <$me>;tag=${c.localTag}\r\nTo: <${c.uri}>\r\nCall-ID: ${c.callId}\r\nCSeq: ${c.cseq} INVITE\r\n" +
            contact + "\r\nUser-Agent: CUPLAJ\r\n" + authHdr + "Content-Type: application/sdp\r\nContent-Length: ${b.toByteArray().size}\r\n\r\n" + b
    }

    private var lastAuthHdr = ""

    private fun invite(dest: String) {
        val clean = dest.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
        if (clean.isEmpty()) {
            hubSend(jo("t" to "hangup"))
            return
        }
        val c = SipCall("c-" + Sec.randomBytes(8).joinToString("") { "%02x".format(it) } + "@$myIp", true)
        c.uri = "sip:$clean@$domain"
        c.rtp = DatagramSocket(0).also { it.soTimeout = 300 }
        call = c
        c.started = System.currentTimeMillis()
        log("INVITE $clean")
        sendSip(inviteMsg(c, ""))
    }

    // ------------------------------------------------------------------ SIP: apel intrat

    private fun onInvite(msg: String, from: InetSocketAddress) {
        val cid = hdr(msg, "call-id")
        val existing = call
        if (existing != null) {
            if (existing.callId == cid) return // retransmisie
            reply(msg, 486, "Busy Here", "", from)
            return
        }
        val c = SipCall(cid, false)
        c.inviteReq = msg
        c.inviteFrom = from
        c.remoteTag = tag(hdr(msg, "from"))
        parseSdp(body(msg))?.let { c.remoteRtp = it }
        c.rtp = DatagramSocket(0).also { it.soTimeout = 300 }
        c.started = System.currentTimeMillis()
        call = c
        reply(msg, 100, "Trying", "", from)
        reply(msg, 180, "Ringing", c.localTag, from)
        log("INVITE intrat")
        hubSend(jo("t" to "dial", "number" to cfg.inbound))
    }

    private fun reply(req: String, code: Int, reason: String, toTag: String, to: InetSocketAddress?, bodyText: String = "", extra: String = "") {
        val vias = hdrAll(req, "via").joinToString("") { "Via: $it\r\n" }
        var toH = hdr(req, "to")
        if (toTag.isNotEmpty() && !toH.contains("tag=")) toH += ";tag=$toTag"
        val m = "SIP/2.0 $code $reason\r\n" + vias + "From: ${hdr(req, "from")}\r\nTo: $toH\r\nCall-ID: ${hdr(req, "call-id")}\r\nCSeq: ${hdr(req, "cseq")}\r\n" +
            extra + (if (bodyText.isNotEmpty()) "Content-Type: application/sdp\r\n" else "") + "Content-Length: ${bodyText.toByteArray().size}\r\n\r\n" + bodyText
        sendSip(m, to)
    }

    private fun accept200(c: SipCall) {
        val req = c.inviteReq ?: return
        c.answered = true
        reply(req, 200, "OK", c.localTag, c.inviteFrom, sdp(c.rtp!!.localPort), contact + "\r\n")
        startMedia(c)
    }

    // ------------------------------------------------------------------ SIP: recepție

    private fun parseSdp(b: String): InetSocketAddress? {
        var ip = ""
        var port = 0
        for (l in b.split("\r\n", "\n")) {
            if (l.startsWith("c=IN IP4 ")) ip = l.substring(9).trim()
            if (l.startsWith("m=audio ")) port = l.substring(8).split(" ")[0].toIntOrNull() ?: 0
        }
        if (ip.isEmpty() || port <= 0) return null
        return try {
            InetSocketAddress(InetAddress.getByName(ip), port)
        } catch (_: Exception) {
            null
        }
    }

    private fun sipLoop() {
        val buf = ByteArray(4096)
        var lastReg = System.currentTimeMillis()
        while (run) {
            try {
                val pk = DatagramPacket(buf, buf.size)
                sip!!.receive(pk)
                val text = String(pk.data, 0, pk.length, Charsets.UTF_8)
                val from = InetSocketAddress(pk.address, pk.port)
                synchronized(lock) { onSip(text, from) }
            } catch (_: java.net.SocketTimeoutException) {
            } catch (_: Exception) {
                if (!run) return
            }
            if (System.currentTimeMillis() - lastReg > 240000) {
                lastReg = System.currentTimeMillis()
                regAuthTried = false
                register()
            }
        }
    }

    private fun onSip(msg: String, from: InetSocketAddress) {
        val first = msg.substringBefore("\r\n")
        if (first.startsWith("SIP/2.0")) {
            val code = first.split(" ").getOrNull(1)?.toIntOrNull() ?: return
            onResponse(msg, code)
            return
        }
        val method = first.substringBefore(" ")
        // doar de la serverul configurat (anti‑uzurpare)
        if (from.address != serverAddr?.address) return
        when (method) {
            "INVITE" -> onInvite(msg, from)
            "ACK" -> call?.acked = true
            "BYE" -> {
                reply(msg, 200, "OK", "", from)
                val c = call
                if (c != null && hdr(msg, "call-id") == c.callId) {
                    c.answered = false
                    endSip("remote-bye", sendBye = false)
                    hubSend(jo("t" to "hangup"))
                }
            }
            "CANCEL" -> {
                reply(msg, 200, "OK", "", from)
                val c = call
                if (c != null && !c.outbound && !c.answered && hdr(msg, "call-id") == c.callId) {
                    c.inviteReq?.let { reply(it, 487, "Request Terminated", c.localTag, from) }
                    call = null
                    c.rtp?.close()
                    hubSend(jo("t" to "hangup"))
                }
            }
            "OPTIONS" -> reply(msg, 200, "OK", "", from)
            else -> reply(msg, 200, "OK", "", from)
        }
    }

    private fun onResponse(msg: String, code: Int) {
        val cseq = hdr(msg, "cseq")
        val cid = hdr(msg, "call-id")
        if (cseq.endsWith("REGISTER") && cid == regCallId) {
            if ((code == 401 || code == 407) && !regAuthTried) {
                regAuthTried = true
                val h = hdr(msg, if (code == 401) "www-authenticate" else "proxy-authenticate")
                register(auth(h, "REGISTER", "sip:$domain", code == 407))
            } else if (code in 200..299) {
                registered = true
                log("registered")
            } else if (code >= 300) {
                registered = false
            }
            return
        }
        val c = call ?: return
        if (cid != c.callId || !c.outbound) return
        if (cseq.endsWith("INVITE")) {
            when {
                code in 100..199 -> {}
                code == 401 || code == 407 -> {
                    sendAck(c, msg)
                    if (!c.authTried) {
                        c.authTried = true
                        val h = hdr(msg, if (code == 401) "www-authenticate" else "proxy-authenticate")
                        c.cseq++
                        sendSip(inviteMsg(c, auth(h, "INVITE", c.uri, code == 407)))
                    } else {
                        failOut(c)
                    }
                }
                code in 200..299 -> {
                    c.remoteTag = tag(hdr(msg, "to"))
                    c.remoteRtp = parseSdp(body(msg))
                    c.lastTo = hdr(msg, "to")
                    c.contactUri = Regex("<([^>]+)>").find(hdr(msg, "contact"))?.groupValues?.get(1) ?: c.uri
                    sendAck(c, msg, ok = true)
                    if (!c.answered) {
                        c.answered = true
                        hubSend(jo("t" to "answer"))
                        startMedia(c)
                    }
                }
                code >= 300 -> {
                    sendAck(c, msg)
                    failOut(c)
                }
            }
        }
    }

    private fun failOut(c: SipCall) {
        log("apel ieșit refuzat")
        call = null
        c.rtp?.close()
        hubSend(jo("t" to "answer"))
        hubSend(jo("t" to "hangup"))
    }

    private fun sendAck(c: SipCall, resp: String, ok: Boolean = false) {
        val toH = hdr(resp, "to")
        val target = if (ok) (c.contactUri.ifEmpty { c.uri }) else c.uri
        val m = "ACK $target SIP/2.0\r\n" + via(if (ok) newBranch() else c.branch) + "\r\nMax-Forwards: 70\r\n" +
            "From: <$me>;tag=${c.localTag}\r\nTo: $toH\r\nCall-ID: ${c.callId}\r\nCSeq: ${c.cseq} ACK\r\nContent-Length: 0\r\n\r\n"
        sendSip(m)
    }

    private fun endSip(why: String, sendBye: Boolean = true) {
        val c = call ?: return
        call = null
        log("end: $why")
        if (sendBye) {
            if (c.answered) {
                val toH = if (c.outbound) c.lastTo else ""
                val m: String
                if (c.outbound) {
                    m = "BYE ${c.contactUri.ifEmpty { c.uri }} SIP/2.0\r\n" + via(newBranch()) + "\r\nMax-Forwards: 70\r\n" +
                        "From: <$me>;tag=${c.localTag}\r\nTo: $toH\r\nCall-ID: ${c.callId}\r\nCSeq: ${++c.cseq} BYE\r\nContent-Length: 0\r\n\r\n"
                } else {
                    val req = c.inviteReq!!
                    val remoteContact = Regex("<([^>]+)>").find(hdr(req, "contact"))?.groupValues?.get(1) ?: "sip:${hdr(req, "from")}"
                    m = "BYE $remoteContact SIP/2.0\r\n" + via(newBranch()) + "\r\nMax-Forwards: 70\r\n" +
                        "From: ${hdr(req, "to")};tag=${c.localTag}\r\nTo: ${hdr(req, "from")}\r\nCall-ID: ${c.callId}\r\nCSeq: ${++c.cseq} BYE\r\nContent-Length: 0\r\n\r\n"
                }
                sendSip(m, if (c.outbound) serverAddr else c.inviteFrom)
            } else if (c.outbound) {
                val m = "CANCEL ${c.uri} SIP/2.0\r\n" + via(c.branch) + "\r\nMax-Forwards: 70\r\n" +
                    "From: <$me>;tag=${c.localTag}\r\nTo: <${c.uri}>\r\nCall-ID: ${c.callId}\r\nCSeq: ${c.cseq} CANCEL\r\nContent-Length: 0\r\n\r\n"
                sendSip(m)
            } else {
                c.inviteReq?.let { reply(it, 486, "Busy Here", c.localTag, c.inviteFrom) }
            }
        }
        c.answered = false
        c.rtp?.close()
    }

    // ------------------------------------------------------------------ RTP (PCMU)

    private fun startMedia(c: SipCall) {
        val sock = c.rtp ?: return
        // recepție RTP → hub
        Thread {
            val buf = ByteArray(1500)
            while (run && call === c && !sock.isClosed) {
                try {
                    val pk = DatagramPacket(buf, buf.size)
                    sock.receive(pk)
                    if (pk.length < 13) continue
                    val pt = buf[1].toInt() and 0x7F
                    if (pt != 0) continue
                    val cc = buf[0].toInt() and 0x0F
                    val off = 12 + cc * 4
                    if (pk.length <= off) continue
                    val payload = buf.copyOfRange(off, pk.length)
                    val frame = ByteArray(payload.size + 1)
                    System.arraycopy(payload, 0, frame, 1, payload.size)
                    hub?.send(Frame(T_AUDIO_U, frame))
                } catch (_: java.net.SocketTimeoutException) {
                } catch (_: Exception) {
                    return@Thread
                }
            }
        }.apply { isDaemon = true; start() }
        // transmisie la 20 ms: audio din hub sau liniște
        Thread {
            val silence = ByteArray(160) { 0xFF.toByte() }
            var next = System.nanoTime()
            while (run && call === c && !sock.isClosed) {
                val remote = c.remoteRtp
                if (remote != null) {
                    val payload = c.out.poll() ?: silence
                    val pkt = ByteArray(12 + payload.size)
                    pkt[0] = 0x80.toByte()
                    pkt[1] = 0
                    pkt[2] = (c.seq shr 8).toByte(); pkt[3] = c.seq.toByte()
                    for (i in 0 until 4) pkt[4 + i] = (c.ts shr (24 - 8 * i)).toByte()
                    for (i in 0 until 4) pkt[8 + i] = (c.ssrc shr (24 - 8 * i)).toByte()
                    System.arraycopy(payload, 0, pkt, 12, payload.size)
                    c.seq = (c.seq + 1) and 0xFFFF
                    c.ts += payload.size
                    try {
                        sock.send(DatagramPacket(pkt, pkt.size, remote))
                    } catch (_: Exception) {
                    }
                }
                next += 20_000_000L
                val sl = (next - System.nanoTime()) / 1_000_000
                if (sl > 0) try { Thread.sleep(sl) } catch (_: Exception) {}
                else if (sl < -200) next = System.nanoTime()
            }
        }.apply { isDaemon = true; start() }
    }

    private fun timerLoop() {
        while (run) {
            try {
                Thread.sleep(1000)
            } catch (_: Exception) {
            }
            synchronized(lock) {
                val c = call
                if (c != null && System.currentTimeMillis() - c.started > cfg.maxSeconds * 1000L) {
                    endSip("max-duration")
                    hubSend(jo("t" to "hangup"))
                }
            }
        }
    }
}
