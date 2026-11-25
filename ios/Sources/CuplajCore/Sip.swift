import Foundation
import CryptoKit
#if canImport(Darwin)
import Darwin
#endif

public struct SipConfig {
    public var server: String
    public var port: Int = 5060
    public var user: String
    public var pass: String
    public var domain: String = ""
    /// Internal number that rings when a SIP call arrives.
    public var inbound: String = "100"
    public var localIp: String = ""
    public var hubPort: Int = PORT
    public var hubPin: String = ""
    public var hubKey: String = ""
    public var maxSeconds: Int = 900
    public var hubRoom: String = ""
    public init(server: String, port: Int = 5060, user: String, pass: String, domain: String = "", inbound: String = "100", hubPort: Int = PORT, hubPin: String = "", hubKey: String = "", hubRoom: String = "") {
        self.server = server; self.port = port; self.user = user; self.pass = pass; self.domain = domain; self.inbound = inbound
        self.hubPort = hubPort; self.hubPin = hubPin; self.hubKey = hubKey; self.hubRoom = hubRoom
    }
}

private func md5(_ s: String) -> String { Insecure.MD5.hash(data: Data(s.utf8)).map { String(format: "%02x", $0) }.joined() }
private func rhex(_ n: Int) -> String { Sec.randomBytes(n).map { String(format: "%02x", $0) }.joined() }

private func rx(_ pattern: String, _ s: String, _ group: Int = 1) -> String? {
    guard let re = try? NSRegularExpression(pattern: pattern), let m = re.firstMatch(in: s, range: NSRange(s.startIndex..., in: s)),
          m.numberOfRanges > group, let r = Range(m.range(at: group), in: s) else { return nil }
    return String(s[r])
}

private final class SipCall {
    let callId: String
    let outbound: Bool
    var localTag = rhex(5)
    var remoteTag = ""
    var cseq = 1
    var uri = ""
    var inviteReq: String?
    var inviteFrom: sockaddr_in?
    var authTried = false
    var answered = false
    var acked = false
    var remoteRtp: sockaddr_in?
    var rtp: UdpPort?
    var seq = 0
    var ts: UInt32 = 0
    let ssrc: UInt32 = Sec.randomBytes(4).reduce(0) { ($0 << 8) | UInt32($1) }
    var branch = ""
    var lastTo = ""
    var contactUri = ""
    var started = nowMs()
    private let ql = NSLock()
    private var queue = [Data]()
    var loops = 0
    var wantClose = false
    init(_ id: String, outbound: Bool) { callId = id; self.outbound = outbound }
    func offer(_ d: Data) { ql.lock(); if queue.count < 12 { queue.append(d) }; ql.unlock() }
    func poll() -> Data? { ql.lock(); defer { ql.unlock() }; return queue.isEmpty ? nil : queue.removeFirst() }
}

/// VoIP gateway: a special "phone" (role gateway, number 90) attached to the hub through loopback; it speaks SIP over UDP to a provider
/// or PBX and RTP PCMU (G.711 mu-law, 8 kHz - exactly the app's audio format). Out: staff dial 0 + number. In: SIP calls ring the configured internal number.
public final class SipGateway {
    private let cfg: SipConfig
    private let log: (String) -> Void
    private let lock = NSRecursiveLock()
    private var run = false
    private var hub: Conn?
    private var sip: UdpPort?
    private var serverAddr: sockaddr_in?
    private var myIp = "127.0.0.1"
    private let domain: String
    private var call: SipCall?
    private let regCallId = "reg-" + rhex(6)
    private var regCseq = 1
    private var regAuthTried = false
    public private(set) var registered = false
    public var allowUnregistered = true

    public init(_ cfg: SipConfig, log: @escaping (String) -> Void = { _ in }) {
        self.cfg = cfg; self.log = log
        domain = cfg.domain.isEmpty ? cfg.server : cfg.domain
    }

    public var sipPort: Int { sip?.port ?? 0 }
    private func isRun() -> Bool { lock.lock(); defer { lock.unlock() }; return run }

    @discardableResult
    public func start() -> Bool {
        guard let sa = UdpPort.resolve(cfg.server, cfg.port), let s = UdpPort(bind: 0) else { return false }
        lock.lock(); run = true; serverAddr = sa; sip = s; lock.unlock()
        myIp = cfg.localIp.isEmpty ? SipGateway.localIp(toward: sa) : cfg.localIp
        Thread { [self] in sipLoop() }.start()
        Thread { [self] in hubLoop() }.start()
        Thread { [self] in timerLoop() }.start()
        lock.lock(); register(); lock.unlock()
        return true
    }

    public func stop() {
        lock.lock(); run = false; endSip("stop"); let h = hub; lock.unlock()
        h?.close()
    }

    static func localIp(toward a: sockaddr_in) -> String {
        let fd = socket(AF_INET, SOCK_DGRAM, 0)
        if fd < 0 { return "127.0.0.1" }
        defer { Darwin.close(fd) }
        var to = a
        let r = withUnsafePointer(to: &to) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { connect(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) } }
        if r != 0 { return "127.0.0.1" }
        var l = sockaddr_in(); var ll = socklen_t(MemoryLayout<sockaddr_in>.size)
        _ = withUnsafeMutablePointer(to: &l) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { getsockname(fd, $0, &ll) } }
        return addrString(l).0
    }

    // ------------------------------------------------------------------ hub

    private func hubSend(_ o: JSONObject) { lock.lock(); let h = hub; lock.unlock(); h?.sendJson(o) }

    private func hubLoop() {
        while isRun() {
            do {
                let ch = try TcpChan.connect(host: "127.0.0.1", port: cfg.hubPort, timeoutMs: 3000)
                ch.setIdleTimeout(ms: 45000)
                let c = Conn(chan: ch, key: Sec.deriveKey(cfg.hubKey), server: false)
                lock.lock(); hub = c; lock.unlock()
                try c.clientHandshake()
                c.startWriter()
                c.sendJson(jo(["t": "hello", "id": "sip-gateway", "name": "SIP", "role": "gateway", "pin": cfg.hubPin, "room": cfg.hubRoom]))
                var lastPing = nowMs()
                while isRun() {
                    let f: Frame
                    do { f = try c.readFrame() } catch { break }
                    if f.bad { break }
                    if nowMs() - lastPing > 15000 { c.sendJson(jo(["t": "ping"])); lastPing = nowMs() }
                    if f.type == T_JSON { if let m = parseJSON(f.data) { onHub(m) } }
                    else if (f.type == T_AUDIO || f.type == T_AUDIO_U) && f.data.count > 1 {
                        let body = Data(f.data.dropFirst())
                        let u = f.type == T_AUDIO_U ? body : G711.encode(body)
                        lock.lock(); let cl = call; lock.unlock()
                        if let cl = cl, cl.answered { cl.offer(u) }
                    }
                }
            } catch {}
            lock.lock(); let h = hub; lock.unlock()
            h?.close()
            if isRun() { Thread.sleep(forTimeInterval: 1.5) }
        }
    }

    private func onHub(_ m: JSONObject) {
        lock.lock(); defer { lock.unlock() }
        switch m.optString("t") {
        case "ring":
            let dest = m.optString("dest")
            if dest.isEmpty { hubSend(jo(["t": "hangup"])) }
            else if call != nil || (!registered && !allowUnregistered) { hubSend(jo(["t": "answer"])); hubSend(jo(["t": "hangup"])) }
            else { invite(dest) }
        case "connected":
            if let c = call, !c.outbound, !c.answered { accept200(c) }
        case "ended", "busy", "unknown":
            if call != nil { endSip(m.optString("t")) }
        default: break
        }
    }

    // ------------------------------------------------------------------ SIP helpers

    private func sendSip(_ text: String, to: sockaddr_in? = nil) {
        guard let dst = to ?? serverAddr else { return }
        sip?.send(Data(text.utf8), to: dst)
    }

    private func via(_ branch: String) -> String { "Via: SIP/2.0/UDP \(myIp):\(sipPort);branch=\(branch);rport" }
    private func newBranch() -> String { "z9hG4bK" + rhex(6) }
    private var me: String { "sip:\(cfg.user)@\(domain)" }
    private var contact: String { "Contact: <sip:\(cfg.user)@\(myIp):\(sipPort)>" }

    private func hdr(_ msg: String, _ name: String) -> String {
        let n = name.lowercased()
        let shorts = ["call-id": "i", "from": "f", "to": "t", "via": "v", "contact": "m", "content-type": "c"]
        for l in msg.components(separatedBy: "\r\n") {
            if l.isEmpty { break }
            if let i = l.firstIndex(of: ":") {
                let k = l[..<i].trimmingCharacters(in: .whitespaces).lowercased()
                if k == n || k == shorts[n] { return l[l.index(after: i)...].trimmingCharacters(in: .whitespaces) }
            }
        }
        return ""
    }

    private func hdrAll(_ msg: String, _ name: String) -> [String] {
        var out = [String]()
        for l in msg.components(separatedBy: "\r\n") {
            if l.isEmpty { break }
            if let i = l.firstIndex(of: ":"), l[..<i].trimmingCharacters(in: .whitespaces).lowercased() == name.lowercased() {
                out.append(l[l.index(after: i)...].trimmingCharacters(in: .whitespaces))
            }
        }
        return out
    }

    private func tag(_ h: String) -> String { rx("tag=([^;>\\s]+)", h) ?? "" }

    private func body(_ msg: String) -> String {
        guard let r = msg.range(of: "\r\n\r\n") else { return "" }
        return String(msg[r.upperBound...])
    }

    private func auth(_ h: String, _ method: String, _ uri: String, proxy: Bool) -> String {
        func p(_ k: String) -> String { rx("\(k)=\"?([^\",]+)\"?", h) ?? "" }
        let realm = p("realm"), nonce = p("nonce"), opaque = p("opaque")
        let qop = p("qop").components(separatedBy: ",").map { $0.trimmingCharacters(in: .whitespaces) }.first { $0 == "auth" } ?? ""
        let ha1 = md5("\(cfg.user):\(realm):\(cfg.pass)")
        let ha2 = md5("\(method):\(uri)")
        var value: String
        if !qop.isEmpty {
            let cn = rhex(4)
            let resp = md5("\(ha1):\(nonce):00000001:\(cn):\(qop):\(ha2)")
            value = "Digest username=\"\(cfg.user)\", realm=\"\(realm)\", nonce=\"\(nonce)\", uri=\"\(uri)\", response=\"\(resp)\", algorithm=MD5, qop=auth, nc=00000001, cnonce=\"\(cn)\""
        } else {
            let resp = md5("\(ha1):\(nonce):\(ha2)")
            value = "Digest username=\"\(cfg.user)\", realm=\"\(realm)\", nonce=\"\(nonce)\", uri=\"\(uri)\", response=\"\(resp)\", algorithm=MD5"
        }
        if !opaque.isEmpty { value += ", opaque=\"\(opaque)\"" }
        return (proxy ? "Proxy-Authorization: " : "Authorization: ") + value + "\r\n"
    }

    // ------------------------------------------------------------------ registration

    private func register(_ authHdr: String = "") {
        let b = newBranch()
        let m = "REGISTER sip:\(domain) SIP/2.0\r\n" + via(b) + "\r\nMax-Forwards: 70\r\n" +
            "From: <\(me)>;tag=reg1\r\nTo: <\(me)>\r\nCall-ID: \(regCallId)\r\nCSeq: \(regCseq) REGISTER\r\n" +
            contact + "\r\nExpires: 300\r\nUser-Agent: CUPLAJ\r\n" + authHdr + "Content-Length: 0\r\n\r\n"
        regCseq += 1
        sendSip(m)
    }

    // ------------------------------------------------------------------ outgoing call

    private func sdp(_ port: Int) -> String {
        "v=0\r\no=cuplaj 1 1 IN IP4 \(myIp)\r\ns=CUPLAJ\r\nc=IN IP4 \(myIp)\r\nt=0 0\r\nm=audio \(port) RTP/AVP 0\r\na=rtpmap:0 PCMU/8000\r\na=ptime:20\r\na=sendrecv\r\n"
    }

    private func inviteMsg(_ c: SipCall, _ authHdr: String) -> String {
        let b = sdp(c.rtp?.port ?? 0)
        c.branch = newBranch()
        return "INVITE \(c.uri) SIP/2.0\r\n" + via(c.branch) + "\r\nMax-Forwards: 70\r\n" +
            "From: <\(me)>;tag=\(c.localTag)\r\nTo: <\(c.uri)>\r\nCall-ID: \(c.callId)\r\nCSeq: \(c.cseq) INVITE\r\n" +
            contact + "\r\nUser-Agent: CUPLAJ\r\n" + authHdr + "Content-Type: application/sdp\r\nContent-Length: \(b.utf8.count)\r\n\r\n" + b
    }

    private func invite(_ dest: String) {
        let clean = String(dest.filter { ($0.isASCII && $0.isNumber) || $0 == "+" }.prefix(24))
        if clean.isEmpty { hubSend(jo(["t": "hangup"])); return }
        let c = SipCall("c-" + rhex(8) + "@" + myIp, outbound: true)
        c.uri = "sip:\(clean)@\(domain)"
        c.rtp = UdpPort(bind: 0)
        call = c
        c.started = nowMs()
        log("INVITE \(clean)")
        sendSip(inviteMsg(c, ""))
    }

    // ------------------------------------------------------------------ incoming call

    private func onInvite(_ msg: String, _ from: sockaddr_in) {
        let cid = hdr(msg, "call-id")
        if let e = call {
            if e.callId == cid { return }
            reply(msg, 486, "Busy Here", "", from)
            return
        }
        let c = SipCall(cid, outbound: false)
        c.inviteReq = msg; c.inviteFrom = from
        c.remoteTag = tag(hdr(msg, "from"))
        c.remoteRtp = parseSdp(body(msg))
        c.rtp = UdpPort(bind: 0)
        c.started = nowMs()
        call = c
        reply(msg, 100, "Trying", "", from)
        reply(msg, 180, "Ringing", c.localTag, from)
        log("INVITE in")
        hubSend(jo(["t": "dial", "number": cfg.inbound]))
    }

    private func reply(_ req: String, _ code: Int, _ reason: String, _ toTag: String, _ to: sockaddr_in?, body bodyText: String = "", extra: String = "") {
        let vias = hdrAll(req, "via").map { "Via: \($0)\r\n" }.joined()
        var toH = hdr(req, "to")
        if !toTag.isEmpty && !toH.contains("tag=") { toH += ";tag=\(toTag)" }
        let m = "SIP/2.0 \(code) \(reason)\r\n" + vias + "From: \(hdr(req, "from"))\r\nTo: \(toH)\r\nCall-ID: \(hdr(req, "call-id"))\r\nCSeq: \(hdr(req, "cseq"))\r\n" +
            extra + (bodyText.isEmpty ? "" : "Content-Type: application/sdp\r\n") + "Content-Length: \(bodyText.utf8.count)\r\n\r\n" + bodyText
        sendSip(m, to: to)
    }

    private func accept200(_ c: SipCall) {
        guard let req = c.inviteReq else { return }
        c.answered = true
        reply(req, 200, "OK", c.localTag, c.inviteFrom, body: sdp(c.rtp?.port ?? 0), extra: contact + "\r\n")
        startMedia(c)
    }

    private func parseSdp(_ b: String) -> sockaddr_in? {
        var ip = "", port = 0
        for l in b.components(separatedBy: "\n") {
            let t = l.trimmingCharacters(in: .whitespacesAndNewlines)
            if t.hasPrefix("c=IN IP4 ") { ip = String(t.dropFirst(9)).trimmingCharacters(in: .whitespaces) }
            if t.hasPrefix("m=audio ") { port = Int(t.dropFirst(8).components(separatedBy: " ")[0]) ?? 0 }
        }
        if ip.isEmpty || port <= 0 { return nil }
        return UdpPort.resolve(ip, port)
    }

    // ------------------------------------------------------------------ SIP receive

    private func sipLoop() {
        var lastReg = nowMs()
        while isRun() {
            if let s = sip, let (d, from) = s.receive(), let text = String(data: d, encoding: .utf8) {
                lock.lock(); onSip(text, from); lock.unlock()
            }
            if nowMs() - lastReg > 240_000 { lastReg = nowMs(); lock.lock(); regAuthTried = false; register(); lock.unlock() }
        }
        sip?.close()
    }

    private func onSip(_ msg: String, _ from: sockaddr_in) {
        let first = msg.components(separatedBy: "\r\n").first ?? ""
        if first.hasPrefix("SIP/2.0") {
            let parts = first.components(separatedBy: " ")
            if parts.count > 1, let code = Int(parts[1]) { onResponse(msg, code) }
            return
        }
        let method = first.components(separatedBy: " ").first ?? ""
        // only from the configured server (anti-spoofing)
        if let sa = serverAddr, from.sin_addr.s_addr != sa.sin_addr.s_addr { return }
        switch method {
        case "INVITE": onInvite(msg, from)
        case "ACK": call?.acked = true
        case "BYE":
            reply(msg, 200, "OK", "", from)
            if let c = call, hdr(msg, "call-id") == c.callId {
                c.answered = false
                endSip("remote-bye", sendBye: false)
                hubSend(jo(["t": "hangup"]))
            }
        case "CANCEL":
            reply(msg, 200, "OK", "", from)
            if let c = call, !c.outbound, !c.answered, hdr(msg, "call-id") == c.callId {
                if let r = c.inviteReq { reply(r, 487, "Request Terminated", c.localTag, from) }
                call = nil
                closeRtp(c)
                hubSend(jo(["t": "hangup"]))
            }
        default: reply(msg, 200, "OK", "", from)
        }
    }

    private func onResponse(_ msg: String, _ code: Int) {
        let cseq = hdr(msg, "cseq"), cid = hdr(msg, "call-id")
        if cseq.hasSuffix("REGISTER") && cid == regCallId {
            if (code == 401 || code == 407) && !regAuthTried {
                regAuthTried = true
                register(auth(hdr(msg, code == 401 ? "www-authenticate" : "proxy-authenticate"), "REGISTER", "sip:\(domain)", proxy: code == 407))
            } else if (200...299).contains(code) { registered = true; log("registered") }
            else if code >= 300 { registered = false }
            return
        }
        guard let c = call, cid == c.callId, c.outbound, cseq.hasSuffix("INVITE") else { return }
        switch code {
        case 100...199: break
        case 401, 407:
            sendAck(c, msg)
            if !c.authTried {
                c.authTried = true
                c.cseq += 1
                sendSip(inviteMsg(c, auth(hdr(msg, code == 401 ? "www-authenticate" : "proxy-authenticate"), "INVITE", c.uri, proxy: code == 407)))
            } else { failOut(c) }
        case 200...299:
            c.remoteTag = tag(hdr(msg, "to"))
            c.remoteRtp = parseSdp(body(msg))
            c.lastTo = hdr(msg, "to")
            c.contactUri = rx("<([^>]+)>", hdr(msg, "contact")) ?? c.uri
            sendAck(c, msg, ok: true)
            if !c.answered { c.answered = true; hubSend(jo(["t": "answer"])); startMedia(c) }
        default:
            if code >= 300 { sendAck(c, msg); failOut(c) }
        }
    }

    private func failOut(_ c: SipCall) {
        log("outgoing call refused")
        call = nil
        closeRtp(c)
        hubSend(jo(["t": "answer"]))
        hubSend(jo(["t": "hangup"]))
    }

    private func sendAck(_ c: SipCall, _ resp: String, ok: Bool = false) {
        let target = ok ? (c.contactUri.isEmpty ? c.uri : c.contactUri) : c.uri
        sendSip("ACK \(target) SIP/2.0\r\n" + via(ok ? newBranch() : c.branch) + "\r\nMax-Forwards: 70\r\n" +
                "From: <\(me)>;tag=\(c.localTag)\r\nTo: \(hdr(resp, "to"))\r\nCall-ID: \(c.callId)\r\nCSeq: \(c.cseq) ACK\r\nContent-Length: 0\r\n\r\n")
    }

    private func endSip(_ why: String, sendBye: Bool = true) {
        guard let c = call else { return }
        call = nil
        log("end: \(why)")
        if sendBye {
            if c.answered {
                if c.outbound {
                    sendSip("BYE \(c.contactUri.isEmpty ? c.uri : c.contactUri) SIP/2.0\r\n" + via(newBranch()) + "\r\nMax-Forwards: 70\r\n" +
                            "From: <\(me)>;tag=\(c.localTag)\r\nTo: \(c.lastTo)\r\nCall-ID: \(c.callId)\r\nCSeq: \(c.cseq + 1) BYE\r\nContent-Length: 0\r\n\r\n")
                } else if let req = c.inviteReq {
                    let rc = rx("<([^>]+)>", hdr(req, "contact")) ?? "sip:" + hdr(req, "from")
                    sendSip("BYE \(rc) SIP/2.0\r\n" + via(newBranch()) + "\r\nMax-Forwards: 70\r\n" +
                            "From: \(hdr(req, "to"));tag=\(c.localTag)\r\nTo: \(hdr(req, "from"))\r\nCall-ID: \(c.callId)\r\nCSeq: \(c.cseq + 1) BYE\r\nContent-Length: 0\r\n\r\n", to: c.inviteFrom)
                }
            } else if c.outbound {
                sendSip("CANCEL \(c.uri) SIP/2.0\r\n" + via(c.branch) + "\r\nMax-Forwards: 70\r\n" +
                        "From: <\(me)>;tag=\(c.localTag)\r\nTo: <\(c.uri)>\r\nCall-ID: \(c.callId)\r\nCSeq: \(c.cseq) CANCEL\r\nContent-Length: 0\r\n\r\n")
            } else if let r = c.inviteReq { reply(r, 486, "Busy Here", c.localTag, c.inviteFrom) }
        }
        c.answered = false
        closeRtp(c)
    }

    // ------------------------------------------------------------------ RTP (PCMU)

    private func closeRtp(_ c: SipCall) {
        c.wantClose = true
        if c.loops == 0 { c.rtp?.close(); c.rtp = nil }
    }

    private func loopEnded(_ c: SipCall) {
        lock.lock(); c.loops -= 1; if c.loops == 0 { c.rtp?.close(); c.rtp = nil }; lock.unlock()
    }

    private func active(_ c: SipCall) -> Bool { lock.lock(); defer { lock.unlock() }; return run && call === c && !c.wantClose }

    private func startMedia(_ c: SipCall) {
        guard let sock = c.rtp else { return }
        c.loops = 2
        Thread { [self] in   // RTP in -> hub
            defer { loopEnded(c) }
            while active(c) {
                guard let (d, _) = sock.receive() else { continue }
                let b = [UInt8](d)
                if b.count < 13 || (b[1] & 0x7F) != 0 { continue }
                let off = 12 + Int(b[0] & 0x0F) * 4
                if b.count <= off { continue }
                lock.lock(); let h = hub; lock.unlock()
                h?.send(Frame(type: T_AUDIO_U, data: Data([0]) + Data(b[off...])))
            }
        }.start()
        Thread { [self] in   // every 20 ms: audio from the hub, or silence
            defer { loopEnded(c) }
            let silence = Data(repeating: 0xFF, count: 160)
            var next = DispatchTime.now().uptimeNanoseconds
            while active(c) {
                if let remote = c.remoteRtp {
                    let payload = c.poll() ?? silence
                    var pkt = [UInt8](repeating: 0, count: 12)
                    pkt[0] = 0x80; pkt[1] = 0
                    pkt[2] = UInt8((c.seq >> 8) & 0xFF); pkt[3] = UInt8(c.seq & 0xFF)
                    for i in 0..<4 { pkt[4 + i] = UInt8((c.ts >> UInt32(24 - 8 * i)) & 0xFF); pkt[8 + i] = UInt8((c.ssrc >> UInt32(24 - 8 * i)) & 0xFF) }
                    sock.send(Data(pkt) + payload, to: remote)
                    c.seq = (c.seq + 1) & 0xFFFF
                    c.ts = c.ts &+ UInt32(payload.count)
                }
                next += 20_000_000
                let now = DispatchTime.now().uptimeNanoseconds
                if next > now { Thread.sleep(forTimeInterval: Double(next - now) / 1e9) }
                else if now - next > 200_000_000 { next = now }
            }
        }.start()
    }

    private func timerLoop() {
        while isRun() {
            Thread.sleep(forTimeInterval: 1)
            lock.lock()
            if let c = call, nowMs() - c.started > Int64(cfg.maxSeconds) * 1000 { endSip("max-duration"); hubSend(jo(["t": "hangup"])) }
            lock.unlock()
        }
    }
}
