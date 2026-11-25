import Foundation
#if canImport(Darwin)
import Darwin
#endif

public final class Peer {
    public let id: String
    public var conn: Conn?
    public var name = ""
    public var role = "spectator"
    public var number = ""
    public var group = ""
    public var tok = ""
    public var line = 0
    public var slot = 0
    public var src = 0
    public var callId = 0
    public var listening = false
    public var talking = false
    public var silent = false
    public var offhook = false
    public var muted = false
    public var bat = -1
    public var zone = 0
    public var zoneAt: Int64 = 0
    public var nMsgs = 0
    public var nCalls = 0
    public var nZones = 0
    public var fx = ""
    public var p2p = false
    public var udp: sockaddr_in?
    public var local = false
    public var smsgw = false
    public var lan = ""
    public var seen: Int64 = nowMs()
    private var winStart: Int64 = 0
    private var jsonCnt = 0
    private var audCnt = 0

    public init(_ id: String) { self.id = id }
    public var online: Bool { conn?.open == true }

    /// Rate limit: kind 0 = JSON (40/s), 1 = audio (90/s).
    func allow(_ kind: Int, _ now: Int64) -> Bool {
        if now - winStart >= 1000 { winStart = now; jsonCnt = 0; audCnt = 0 }
        if kind == 0 { jsonCnt += 1; return jsonCnt <= 40 }
        audCnt += 1
        return audCnt <= 90
    }
}

final class Call {
    let id: Int
    var a: Peer
    var b: Peer
    var connected = false
    let t0 = nowMs()
    var tConn: Int64 = 0
    var listeners = [Peer]()
    var p2pKey: Data?
    init(id: Int, a: Peer, b: Peer) { self.id = id; self.a = a; self.b = b }
    func members() -> [Peer] { [a, b] }
}

private final class Fail { var n = 0; var first: Int64 = 0; var until: Int64 = 0 }
private final class Poll {
    let id: String, q: String, opts: [String], rec: Set<String>
    var votes = [String: Int]()
    init(id: String, q: String, opts: [String], rec: Set<String>) { self.id = id; self.q = q; self.opts = opts; self.rec = rec }
}
private final class MeterState {
    let id: String, kind: String, rec: Set<String>
    var vals = [String: Int]()
    init(id: String, kind: String, rec: Set<String>) { self.id = id; self.kind = kind; self.rec = rec }
}
private final class Chain {
    let id: String, orig: String, order: [String]
    var hop = 0
    var texts = [String]()
    init(id: String, orig: String, order: [String]) { self.id = id; self.orig = orig; self.order = order }
}
private final class Secret {
    let phrase: String, rec: Set<String>
    var solved = false
    var tries = [String: Int]()
    init(phrase: String, rec: Set<String>) { self.phrase = phrase; self.rec = rec }
}

/// The hub runs on the Regie phone. It is the exchange: numbers, calls, party lines, messages, cues, audio routing.
/// All state changes happen under one recursive lock. (Direct P2P audio is negotiated by the hub;
/// relayed audio is the fallback when the direct path cannot be established.)
public final class Hub {
    private let key: Data?
    public var idleTimeoutMs = 60000
    private let hubName: String
    private let pin: String
    private let room: String
    private let port: Int
    private let trustLoopback: Bool
    private let lock = NSRecursiveLock()
    private var peers = [String: Peer]()
    private var order = [String]()
    private var calls = [Int: Call]()
    private var callSeq = 0, lineSeq = 0, srcSeq = 0, nReg = 0, nAct = 0, nSpec = 0
    private var allowListen = true, clickOnJoin = true, peerMsg = true
    public var p2pMode = 1
    private var log = [String]()
    public var ringTimeoutMs: Int64 = 40000
    public var onPersist: ((String) -> Void)?
    public private(set) var error: String?
    public var lockLoopback = false
    private var listenFd: Int32 = -1
    private var running = false
    private var rules = [Rule]()
    private var fired = [String: Int64]()
    private var poll: Poll?
    private var pollSeq = 0
    private var meter: MeterState?
    private var meterSeq = 0
    private let startedAt = nowMs()
    private var cueCount = 0
    private var chain: Chain?
    private var secret: Secret?
    private var tapeFrames = [Data]()
    private var tapeBytes = 0
    private var tapeSrc: String?
    private var tapePlaying = false
    private var tapeUntil: Int64 = 0
    private var waves = 0
    private var fails = [String: Fail]()
    private var conns = [String: Int]()
    private var preauth = 0
    private var smsWinStart: Int64 = 0
    private var smsCount = 0

    public init(hubName: String, pin: String, room: String = "", port: Int = PORT, restore: String? = nil, trustLoopback: Bool = true, netKey: String = "") {
        self.key = Sec.deriveKey(netKey)
        self.hubName = String(hubName.replacingOccurrences(of: "|", with: " ").prefix(40))
        self.pin = pin
        self.room = room
        self.port = port
        self.trustLoopback = trustLoopback
        if let r = restore, !r.isEmpty, let o = parseJSON(Data(r.utf8)) { restoreState(o) }
    }

    // ------------------------------------------------------------------ lifecycle

    public func start() {
        lock.lock(); defer { lock.unlock() }
        if running { return }
        running = true
        spawn { [self] in acceptLoop() }
        spawn { [self] in beaconLoop() }
        spawn { [self] in rendezvousLoop() }
        spawn { [self] in
            while isRunning() {
                Thread.sleep(forTimeInterval: 1)
                lock.lock(); sweep(); lock.unlock()
            }
        }
    }

    private func isRunning() -> Bool { lock.lock(); defer { lock.unlock() }; return running }

    public func stop() {
        lock.lock()
        running = false
        let fd = listenFd
        listenFd = -1
        rdv?.close(); rdv = nil
        let cs = peers.values.compactMap { $0.conn }
        lock.unlock()
        if fd >= 0 { Darwin.close(fd) }
        for c in cs { c.close() }
    }

    private func spawn(_ f: @escaping () -> Void) {
        let t = Thread(block: f)
        t.start()
    }

    // ------------------------------------------------------------------ persistence

    private func plist() -> [Peer] { order.compactMap { peers[$0] } }

    private func exportState() -> String {
        let arr: [Any] = plist().map { q in
            jo(["id": q.id, "name": q.name, "role": q.role, "number": q.number, "group": q.group, "tok": q.tok,
                "line": q.line, "slot": q.slot, "src": q.src, "muted": q.muted])
        }
        return jsonString(jo(["peers": arr, "nReg": nReg, "nAct": nAct, "nSpec": nSpec, "lineSeq": lineSeq, "srcSeq": srcSeq,
                              "listen": allowListen, "click": clickOnJoin, "peerMsg": peerMsg, "p2p": p2pMode]))
    }

    private func restoreState(_ o: JSONObject) {
        nReg = o.optInt("nReg"); nAct = o.optInt("nAct"); nSpec = o.optInt("nSpec")
        lineSeq = o.optInt("lineSeq"); srcSeq = o.optInt("srcSeq")
        allowListen = o.optBool("listen", true)
        p2pMode = o.optInt("p2p", 1)
        clickOnJoin = o.optBool("click", true)
        peerMsg = o.optBool("peerMsg", true)
        for e in o.optArray("peers") ?? [] {
            guard let j = e as? JSONObject else { continue }
            let id = j.optString("id")
            if id.isEmpty { continue }
            let p = Peer(id)
            p.name = j.optString("name"); p.role = j.optString("role", "spectator"); p.number = j.optString("number")
            p.group = j.optString("group"); p.tok = j.optString("tok"); p.line = j.optInt("line"); p.slot = j.optInt("slot")
            p.src = j.optInt("src"); p.muted = j.optBool("muted")
            peers[id] = p
            order.append(id)
        }
    }

    private func persist() { if let cb = onPersist { cb(exportState()) } }

    // ------------------------------------------------------------------ log

    private func logEvent(_ s: String) {
        let f = DateFormatter()
        f.dateFormat = "HH:mm:ss"
        let line = f.string(from: Date()) + "  " + s
        log.append(line)
        while log.count > 300 { log.removeFirst() }
        let j = jo(["t": "log", "line": line])
        for q in plist() where q.role == "regie" && q.online { sendTo(q, j) }
    }

    private func label(_ p: Peer) -> String { p.name + " (" + p.number + ")" }
    private func isStaff(_ p: Peer) -> Bool { p.role == "regie" || p.role == "actor" }

    // ------------------------------------------------------------------ network

    private func acceptLoop() {
        let fd = socket(AF_INET, SOCK_STREAM, 0)
        if fd < 0 { error = "socket"; return }
        var one: Int32 = 1
        setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &one, socklen_t(MemoryLayout<Int32>.size))
        var tv = timeval(tv_sec: 1, tv_usec: 0)
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))
        var addr = sockaddr_in()
        addr.sin_family = sa_family_t(AF_INET)
        addr.sin_port = in_port_t(UInt16(port).bigEndian)
        addr.sin_addr = in_addr(s_addr: INADDR_ANY.bigEndian)
        let br = withUnsafePointer(to: &addr) { p in
            p.withMemoryRebound(to: sockaddr.self, capacity: 1) { bind(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) }
        }
        if br != 0 || listen(fd, 64) != 0 { error = "bind"; Darwin.close(fd); return }
        lock.lock(); listenFd = fd; lock.unlock()
        while isRunning() {
            let c = accept(fd, nil, nil)
            if c < 0 {
                if errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR { continue }
                if !isRunning() { break }
                Thread.sleep(forTimeInterval: 0.1)
                continue
            }
            adopt(TcpChan(fd: c))
        }
    }

    private func beaconLoop() {
        let s = socket(AF_INET, SOCK_DGRAM, 0)
        if s < 0 { return }
        defer { Darwin.close(s) }
        var one: Int32 = 1
        setsockopt(s, SOL_SOCKET, SO_BROADCAST, &one, socklen_t(MemoryLayout<Int32>.size))
        let msg = Array("CUPLAJ1|\(hubName)|\(port)|\(room.isEmpty ? "0" : "1")|\(key == nil ? "0" : "1")".utf8)
        while isRunning() {
            var targets = ["255.255.255.255"]
            targets.append(contentsOf: Hub.broadcastAddresses())
            for t in Set(targets) {
                var a = sockaddr_in()
                a.sin_family = sa_family_t(AF_INET)
                a.sin_port = in_port_t(UInt16(BEACON_PORT).bigEndian)
                if inet_pton(AF_INET, t, &a.sin_addr) == 1 {
                    _ = withUnsafePointer(to: &a) { p in
                        p.withMemoryRebound(to: sockaddr.self, capacity: 1) { sendto(s, msg, msg.count, 0, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) }
                    }
                }
            }
            Thread.sleep(forTimeInterval: 2)
        }
    }

    /// Directed broadcast addresses of the up, non-loopback IPv4 interfaces.
    public static func broadcastAddresses() -> [String] {
        var out = [String]()
        var ifap: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifap) == 0, let first = ifap else { return out }
        defer { freeifaddrs(ifap) }
        var p: UnsafeMutablePointer<ifaddrs>? = first
        while let i = p {
            let fl = Int32(i.pointee.ifa_flags)
            if let da = i.pointee.ifa_dstaddr, (fl & IFF_UP) != 0, (fl & IFF_LOOPBACK) == 0, (fl & IFF_BROADCAST) != 0, da.pointee.sa_family == sa_family_t(AF_INET) {
                var buf = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
                let sin = da.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee }
                var ad = sin.sin_addr
                if inet_ntop(AF_INET, &ad, &buf, socklen_t(INET_ADDRSTRLEN)) != nil { out.append(String(cString: buf)) }
            }
            p = i.pointee.ifa_next
        }
        return out
    }

    /// This device's private IPv4 addresses (for invitations and the screen).
    public static func localAddresses() -> [String] {
        var out = [String]()
        var ifap: UnsafeMutablePointer<ifaddrs>?
        guard getifaddrs(&ifap) == 0, let first = ifap else { return out }
        defer { freeifaddrs(ifap) }
        var p: UnsafeMutablePointer<ifaddrs>? = first
        while let i = p {
            let fl = Int32(i.pointee.ifa_flags)
            if let a = i.pointee.ifa_addr, (fl & IFF_UP) != 0, (fl & IFF_LOOPBACK) == 0, a.pointee.sa_family == sa_family_t(AF_INET) {
                var buf = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
                let sin = a.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee }
                var ad = sin.sin_addr
                if inet_ntop(AF_INET, &ad, &buf, socklen_t(INET_ADDRSTRLEN)) != nil {
                    let s = String(cString: buf)
                    if isPrivateHost(s) { out.append(s) }
                }
            }
            p = i.pointee.ifa_next
        }
        return out
    }

    private func deny(_ c: Conn, _ code: String) {
        c.startWriter()
        c.sendJson(jo(["t": "denied", "why": code]))
        Thread.sleep(forTimeInterval: 0.3)
        c.close()
    }

    // ------------------------------------------------------------------ abuse defence

    private func blocked(_ o: String) -> Bool { (fails[o]?.until ?? 0) > nowMs() }

    /// Five or six failed tries within a minute block the origin for a minute.
    private func failed(_ o: String) {
        let now = nowMs()
        lock.lock(); defer { lock.unlock() }
        if fails.count > 2000 { fails.removeAll() }
        let f = fails[o] ?? Fail()
        fails[o] = f
        if now - f.first > 60_000 { f.n = 0; f.first = now }
        f.n += 1
        if f.n >= 6 { f.until = now + 60_000 }
    }

    /// Takes over a new channel (TCP or other) and handles it on its own thread.
    public func adopt(_ ch: Chan) {
        let o = ch.origin
        let loop = ch.loopback
        lock.lock()
        if (!loop || lockLoopback) && blocked(o) { lock.unlock(); ch.close(); return }
        let cnt = conns[o] ?? 0
        if preauth >= 64 || (!loop && cnt >= 60) { lock.unlock(); ch.close(); return }
        conns[o] = cnt + 1
        lock.unlock()
        spawn { [self] in
            handle(ch)
            lock.lock(); conns[o] = max(0, (conns[o] ?? 1) - 1); lock.unlock()
        }
    }

    private func handle(_ ch: Chan) {
        let c = Conn(chan: ch, key: key, server: true)
        var me: Peer?
        let origin = ch.origin
        let loop = ch.loopback
        var counted = true
        lock.lock(); preauth += 1; lock.unlock()
        defer {
            lock.lock()
            if counted { preauth -= 1 }
            lock.unlock()
            if let q = me {
                q.seen = nowMs(); q.zone = 0
                lock.lock()
                if q.conn === c {
                    hangup(q)
                    q.conn = nil
                    logEvent("− " + label(q))
                    broadcastPeers()
                }
                lock.unlock()
            }
            c.close()
        }
        do {
            ch.setIdleTimeout(ms: 10000)
            if try !c.serverHandshake() {
                if !loop || lockLoopback { failed(origin) }
                c.startWriter()
                c.sendJsonPlain(jo(["t": "denied", "why": key == nil ? "no_key" : "bad_key"]))
                Thread.sleep(forTimeInterval: 0.3)
                return
            }
            let f = try c.readFrame()
            if f.bad {
                if !loop || lockLoopback { failed(origin) }
                c.startWriter()
                c.sendJsonPlain(jo(["t": "denied", "why": "bad_key"]))
                Thread.sleep(forTimeInterval: 0.3)
                return
            }
            guard f.type == T_JSON, let m = parseJSON(f.data), m.optString("t") == "hello" else { return }
            let id = cleanText(m.optString("id"), 64)
            if id.isEmpty { return }
            var role = m.optString("role", "spectator")
            let local = trustLoopback && ch.loopback
            if !local && !room.isEmpty && !Sec.same(m.optString("room"), room) {
                if !loop || lockLoopback { failed(origin) }
                Thread.sleep(forTimeInterval: 1)
                deny(c, "bad_room")
                return
            }
            if !local && (role == "actor" || role == "regie" || role == "gateway") && !Sec.same(m.optString("pin"), pin) {
                if !loop || lockLoopback { failed(origin) }
                Thread.sleep(forTimeInterval: 1)
                deny(c, "bad_pin")
                return
            }
            ch.setIdleTimeout(ms: idleTimeoutMs)
            c.maxFrame = 32768
            lock.lock()
            if counted { counted = false; preauth -= 1 }
            lock.unlock()
            if role != "regie" && role != "actor" && role != "gateway" { role = "spectator" }
            c.startWriter()
            var refused: String?
            lock.lock()
            var q = peers[id]
            if q == nil && peers.count >= 400 {
                // full: make room by dropping the oldest offline spectator on no line, else refuse
                if let old = plist().filter({ !$0.online && $0.role == "spectator" && $0.line == 0 }).min(by: { $0.seen < $1.seen }) {
                    peers[old.id] = nil
                    order.removeAll { $0 == old.id }
                }
            }
            if q == nil && peers.count >= 400 {
                refused = "full"
            } else if let qq = q, !local, !qq.tok.isEmpty, qq.tok != m.optString("tok") {
                refused = "dup"
            } else {
                if q == nil {
                    let np = Peer(id)
                    peers[id] = np
                    order.append(id)
                    srcSeq += 1
                    np.src = srcSeq % 250 + 1
                    q = np
                }
                let qq = q!
                qq.conn?.close()
                if qq.number.isEmpty || qq.role != role {
                    qq.role = role
                    qq.number = newNumber(role)
                }
                if qq.tok.isEmpty { qq.tok = newToken() }
                let nm = cleanText(m.optString("name", "?"), 40)
                qq.name = nm.isEmpty ? "?" : nm
                qq.p2p = m.optBool("p2p") && role != "regie"
                qq.smsgw = role == "regie" && m.optBool("smsgw")
                qq.local = local || (loop && (role == "regie" || role == "gateway"))
                qq.lan = ""
                qq.conn = c
                me = qq
                sendTo(qq, welcome(qq))
                pushLine(qq)
                if qq.role == "regie" { for l in log { sendTo(qq, jo(["t": "log", "line": l])) } }
                logEvent("+ " + label(qq))
                broadcastPeers()
                persist()
            }
            lock.unlock()
            if let why = refused {
                c.sendJson(jo(["t": "denied", "why": why]))
                Thread.sleep(forTimeInterval: 0.3)
                return
            }
            while true {
                let fr = try c.readFrame()
                if fr.bad { break }
                guard let who = me else { break }
                let now = nowMs()
                if fr.type == T_JSON {
                    guard let jm = parseJSON(fr.data) else { continue }
                    lock.lock(); if who.allow(0, now) { onJson(who, jm) }; lock.unlock()
                } else if (fr.type == T_AUDIO || fr.type == T_AUDIO_U) && fr.data.count > 1 {
                    lock.lock(); if who.allow(1, now) { onAudio(who, fr.data, fr.type) }; lock.unlock()
                }
            }
        } catch {
            // connection ended
        }
    }

    // ------------------------------------------------------------------ helpers

    private func newToken() -> String { hex(Sec.randomBytes(12)) }

    private func newNumber(_ role: String) -> String {
        while true {
            let n: String
            switch role {
            case "regie": n = "10" + String(nReg); nReg += 1
            case "gateway": n = "90"
            case "actor": nAct += 1; n = "2" + String(format: "%02d", nAct)
            default: nSpec += 1; n = "21" + String(format: "%04d", 1000 + nSpec)
            }
            if !peers.values.contains(where: { $0.number == n }) { return n }
            if role == "gateway" { return n }
        }
    }

    private func norm(_ s: String) -> String {
        let d = s.lowercased().decomposedStringWithCanonicalMapping
        return String(String.UnicodeScalarView(d.unicodeScalars.filter { $0.value < 128 && CharacterSet.alphanumerics.contains($0) }))
    }

    private func pollJson(_ pl: Poll, _ open: Bool) -> JSONObject {
        var counts = [Int](repeating: 0, count: pl.opts.count)
        for v in pl.votes.values where v >= 0 && v < counts.count { counts[v] += 1 }
        return jo(["t": "pollres", "id": pl.id, "q": pl.q, "opts": pl.opts, "counts": counts, "n": pl.votes.count, "of": pl.rec.count, "open": open])
    }

    private func pushPoll(_ pl: Poll, _ open: Bool) {
        let j = pollJson(pl, open)
        for q in plist() where isStaff(q) && q.online { sendTo(q, j) }
    }

    private func chainStep(_ c: Chain) {
        if c.hop >= c.order.count {
            let j = jo(["t": "chainres", "id": c.id, "orig": c.orig, "hops": c.texts])
            for q in plist() where isStaff(q) && q.online { sendTo(q, j) }
            logEvent("telefon fără fir: \"" + c.orig + "\" → \"" + (c.texts.last ?? c.orig) + "\"")
            chain = nil
            return
        }
        let cur = c.texts.last ?? c.orig
        guard let q = peers[c.order[c.hop]], q.online else {
            c.texts.append(cur); c.hop += 1; chainStep(c); return
        }
        sendTo(q, jo(["t": "chainmsg", "id": c.id, "text": cur, "hop": c.hop + 1, "of": c.order.count]))
        let h = c.hop
        spawn { [self] in
            Thread.sleep(forTimeInterval: 90)
            lock.lock(); defer { lock.unlock() }
            if chain === c && c.hop == h {
                c.texts.append(cur); c.hop += 1
                logEvent("telefon fără fir: " + label(q) + " nu a răspuns, sărit")
                chainStep(c)
            }
        }
    }

    private func reportMd() -> String {
        let ps = plist().map { RPeer(name: $0.name, role: $0.role, number: $0.number, online: $0.online, bat: $0.bat, zone: $0.zone, calls: $0.nCalls, msgs: $0.nMsgs, zones: $0.nZones) }
        return Report.markdown(hub: hubName, startMs: startedAt, nowMs: nowMs(), peers: ps, events: log,
                               counters: [("cues sent", cueCount), ("calls placed", ps.reduce(0) { $0 + $1.calls }), ("messages", ps.reduce(0) { $0 + $1.msgs })])
    }

    private func waveSend(_ list: [Peer], _ m: JSONObject, _ stepMs: Int) {
        let ord = list.sorted { (Int64($0.number) ?? Int64.max) < (Int64($1.number) ?? Int64.max) }
        if waves >= 4 { for r in ord { sendTo(r, m) }; return }
        let step = min(Int64(stepMs), 20_000 / Int64(max(1, ord.count - 1)))
        waves += 1
        spawn { [self] in
            for (i, r) in ord.enumerated() {
                if i > 0 { Thread.sleep(forTimeInterval: Double(step) / 1000) }
                lock.lock(); sendTo(r, m); lock.unlock()
            }
            lock.lock(); waves -= 1; lock.unlock()
        }
    }

    private func fire(_ r: Rule, _ who: Peer?) {
        let key = r.id + ":" + (who?.id ?? "")
        let now = nowMs()
        if let prev = fired[key], r.once || now - prev < 3000 { return }
        fired[key] = now
        let to: [Peer]
        if r.to == "self" { to = (who != nil && who!.online) ? [who!] : [] }
        else { to = targets(r.to, nil).filter { $0.role != "regie" || r.kind == "goto" } }
        if r.kind == "goto" {
            for q in plist() where q.role == "regie" && q.online { sendTo(q, jo(["t": "goto", "step": r.text])) }
        } else {
            let cue = jo(["t": "cue", "kind": r.kind, "text": r.text, "from": "rule"])
            for q in to { sendTo(q, cue) }
        }
        logEvent("regulă " + r.id + " (" + r.on + ") → " + r.to + " " + r.kind + (who != nil ? " [" + label(who!) + "]" : ""))
    }

    private func fireRules(_ on: String, _ who: Peer?, _ arg: Int, mk: String = "", v: Int = 0) {
        for r in rules where r.on == on {
            let ok: Bool
            switch on {
            case "solved": ok = true
            case "zone", "pollwin": ok = r.arg == arg
            case "meter": ok = r.meterKind == mk && v >= r.arg
            default: ok = false
            }
            if ok { fire(r, who) }
        }
    }

    private func meterJson(_ m: MeterState, _ open: Bool) -> JSONObject {
        let v = Array(m.vals.values)
        return jo(["t": "meterres", "id": m.id, "kind": m.kind, "avg": v.isEmpty ? 0 : v.reduce(0, +) / v.count, "max": v.max() ?? 0, "n": v.count, "of": m.rec.count, "open": open])
    }

    private func closeMeter(_ m: MeterState) {
        lock.lock(); defer { lock.unlock() }
        if meter !== m { return }
        meter = nil
        let j = meterJson(m, false)
        for q in plist() where isStaff(q) && q.online { sendTo(q, j) }
        if !m.vals.isEmpty { fireRules("meter", nil, 0, mk: m.kind, v: j.optInt("avg")) }
        logEvent("contor \(m.kind): medie \(j.optInt("avg")), max \(j.optInt("max")), \(m.vals.count)/\(m.rec.count)")
    }

    private func welcome(_ q: Peer) -> JSONObject {
        jo(["t": "welcome", "number": q.number, "name": q.name, "src": q.src, "hub": hubName, "role": q.role, "tok": q.tok, "group": q.group, "fx": q.fx])
    }

    private func sendTo(_ p: Peer, _ o: JSONObject) { p.conn?.sendJson(o) }

    private func partnerOf(_ p: Peer) -> Peer? {
        p.line == 0 ? nil : plist().first { $0 !== p && $0.line == p.line }
    }

    private func pushLine(_ p: Peer) {
        sendTo(p, jo(["t": "line", "line": p.line, "slot": p.slot, "partner": partnerOf(p)?.number ?? ""]))
    }

    private func targets(_ to: String, _ from: Peer?) -> [Peer] {
        let on = plist().filter { $0.online && $0 !== from }
        if to == "all" { return on }
        if to == "staff" { return on.filter { isStaff($0) } }
        if to == "spectators" { return on.filter { $0.role == "spectator" } }
        if to == "actors" { return on.filter { $0.role == "actor" } }
        if to.hasPrefix("num:") { return on.filter { $0.number == String(to.dropFirst(4)) } }
        if to.hasPrefix("id:") { return on.filter { $0.id == String(to.dropFirst(3)) } }
        if to.hasPrefix("grp:") { return on.filter { !$0.group.isEmpty && $0.group == String(to.dropFirst(4)) } }
        return []
    }

    private func peersJson() -> JSONObject {
        let arr: [Any] = plist().map { q in
            jo(["id": q.id, "name": q.name, "role": q.role, "number": q.number, "group": q.group, "line": q.line, "slot": q.slot,
                "online": q.online, "muted": q.muted, "bat": q.bat, "zone": q.zone, "fx": q.fx,
                "busy": q.callId != 0 || q.offhook, "mon": q.callId != 0 && calls[q.callId]?.connected == true])
        }
        return jo(["t": "peers", "list": arr, "listen": allowListen, "click": clickOnJoin, "peerMsg": peerMsg, "p2p": p2pMode])
    }

    private func broadcastPeers() {
        let j = peersJson()
        for q in plist() where isStaff(q) && q.online { sendTo(q, j) }
    }

    // ------------------------------------------------------------------ JSON messages

    private func strList(_ a: [Any]?, max: Int, len: Int) -> [String] {
        var out = [String]()
        for e in (a ?? []).prefix(max) {
            let s = cleanText((e as? String) ?? "", len)
            if !s.isEmpty { out.append(s) }
        }
        return out
    }

    private func onJson(_ p: Peer, _ m0: JSONObject) {
        var m = m0
        let reg = p.role == "regie"
        switch m.optString("t") {
        case "msg":
            var to = m.optString("to", "all")
            if !isStaff(p) && to == "all" { to = "staff" }
            let out = jo(["t": "msg", "from": p.name, "num": p.number, "role": p.role, "text": String(m.optString("text").prefix(500)), "to": to])
            p.nMsgs += 1
            var list = targets(to, p)
            if p.role == "spectator" && !peerMsg { list = list.filter { isStaff($0) } }
            for r in list { sendTo(r, out) }
        case "cue":
            if !reg { break }
            m["from"] = p.number
            let list = targets(m.optString("to", "all"), p)
            cueCount += 1
            let wave = min(max(m.optInt("wave", 0), 0), 3000)
            m["wave"] = nil
            if wave > 0 && list.count > 1 { waveSend(list, m, wave) } else { for r in list { sendTo(r, m) } }
            logEvent("cue " + m.optString("kind") + " → " + m.optString("to"))
            let cid = cleanText(m.optString("id"), 16)
            if !cid.isEmpty {
                sendTo(p, jo(["t": "cueack", "id": cid, "sent": list.count, "online": plist().filter { $0.online && $0 !== p }.count]))
            }
        case "media":
            if !reg { break }
            let mid = m.optString("mid")
            if Media.validId(mid) && Media.KINDS[m.optString("kind")] != nil && m.optString("b64").count <= Media.MAX_B64 && m.optInt("i", -1) >= 0 {
                var out = jo(["t": "media", "mid": mid, "kind": m.optString("kind"), "i": m.optInt("i"), "n": m.optInt("n"), "b64": m.optString("b64")])
                if m.optInt("i") == 0 { out["size"] = m.optLong("size"); out["sha"] = m.optString("sha") }
                for r in targets(m.optString("to", "all"), p) { sendTo(r, out) }
                if m.optInt("i") == 0 { logEvent("media " + m.optString("kind") + " " + mid + " → " + m.optString("to")) }
            }
        case "rules":
            if !reg { break }
            rules = Rules.fromJson(m.optArray("list"))
            fired.removeAll()
            sendTo(p, jo(["t": "rulesok", "n": rules.count]))
        case "zone":
            if reg { break }
            let z = m.optInt("z", -1)
            let now = nowMs()
            if z >= 0 && z <= 99 && z != p.zone && now - p.zoneAt > 2000 {
                p.zone = z; p.zoneAt = now; p.nZones += 1
                logEvent(label(p) + " → zona \(z)")
                broadcastPeers()
                if z > 0 { fireRules("zone", p, z) }
            }
        case "setfx":
            if !reg { break }
            let k = m.optString("k")
            if let q = peers[m.optString("id")], k.isEmpty || VoiceFx.KINDS.contains(k) {
                q.fx = k
                sendTo(q, jo(["t": "fx", "k": k]))
                broadcastPeers()
            }
        case "meter":
            if !reg { break }
            let kind = m.optString("kind")
            let secs = min(max(m.optInt("secs", 5), 1), 30)
            if kind == "noise" || kind == "motion" {
                let rec = targets(m.optString("to", "all"), p).filter { $0.role != "regie" }
                meterSeq += 1
                let mt = MeterState(id: "m\(meterSeq)", kind: kind, rec: Set(rec.map { $0.id }))
                meter = mt
                let req = jo(["t": "meterreq", "id": mt.id, "kind": kind, "secs": secs])
                for r in rec { sendTo(r, req) }
                spawn { [self] in
                    Thread.sleep(forTimeInterval: Double(secs) + 3)
                    closeMeter(mt)
                }
                sendTo(p, meterJson(mt, true))
            }
        case "meterval":
            let v = m.optInt("v", -1)
            if let mt = meter, !reg, m.optString("id") == mt.id, mt.rec.contains(p.id), v >= 0 && v <= 100 {
                mt.vals[p.id] = v
                let j = meterJson(mt, true)
                for q in plist() where isStaff(q) && q.online { sendTo(q, j) }
            }
        case "report":
            if reg { sendTo(p, jo(["t": "reportres", "md": reportMd()])) }
        case "chain":
            if !reg { break }
            let text = cleanText(m.optString("text"), 200)
            var ord = [String]()
            for e in (m.optArray("nums") ?? []).prefix(20) {
                let num = (e as? String) ?? ""
                if let q = plist().first(where: { $0.number == num && $0.role != "regie" }), !ord.contains(q.id) { ord.append(q.id) }
            }
            if !text.isEmpty && ord.count >= 2 {
                let c = Chain(id: "c" + String(nowMs() % 100000), orig: text, order: ord)
                chain = c
                chainStep(c)
                logEvent("telefon fără fir pornit: \(ord.count) persoane")
            }
        case "chainfwd":
            if let c = chain, c.hop < c.order.count, c.order[c.hop] == p.id, m.optString("id") == c.id {
                c.texts.append(cleanText(m.optString("text"), 200))
                c.hop += 1
                chainStep(c)
            }
        case "secret":
            if !reg { break }
            let phrase = cleanText(m.optString("phrase"), 120)
            let words = phrase.split(whereSeparator: { $0.isWhitespace }).map(String.init)
            let rec = targets(m.optString("to", "spectators"), p).filter { $0.role != "regie" }
            if words.count >= 2 && !rec.isEmpty && norm(phrase).count >= 4 {
                secret = Secret(phrase: phrase, rec: Set(rec.map { $0.id }))
                let sh = rec.sorted { (Int64($0.number) ?? Int64.max) < (Int64($1.number) ?? Int64.max) }
                for (i, r) in sh.enumerated() {
                    let k = i % words.count
                    sendTo(r, jo(["t": "cue", "kind": "text", "text": "Fragment \(k + 1)/\(words.count): \(words[k])  (găsiți restul și spuneți fraza întreagă)", "from": "secret"]))
                }
                logEvent("secret fragmentat: \(words.count) cuvinte la \(rec.count) telefoane")
            }
        case "guess":
            if let s = secret, !s.solved, !reg, s.rec.contains(p.id) {
                let n = (s.tries[p.id] ?? 0) + 1
                s.tries[p.id] = n
                if n <= 20 && norm(m.optString("text")) == norm(s.phrase) && !norm(s.phrase).isEmpty {
                    s.solved = true
                    sendTo(p, jo(["t": "guessres", "ok": true]))
                    for q in plist() where isStaff(q) && q.online { sendTo(q, jo(["t": "secretres", "by": label(p), "phrase": s.phrase])) }
                    logEvent("secret rezolvat de " + label(p))
                    fireRules("solved", p, 0)
                } else { sendTo(p, jo(["t": "guessres", "ok": false])) }
            }
        case "tape":
            if !reg { break }
            switch m.optString("a") {
            case "rec":
                if let q = peers[m.optString("id")], q.role != "regie" {
                    tapeFrames.removeAll(); tapeBytes = 0
                    tapeSrc = q.id
                    tapeUntil = nowMs() + Int64(min(max(m.optInt("secs", 60), 1), 90)) * 1000
                    logEvent("casetofon: înregistrez " + label(q))
                }
            case "stop":
                tapeSrc = nil
                logEvent("casetofon: oprit (\(tapeFrames.count) cadre)")
            case "play":
                let rec = targets(m.optString("to", "all"), p).filter { $0.role != "regie" }
                let frames = tapeFrames
                if !frames.isEmpty && !rec.isEmpty && !tapePlaying {
                    tapePlaying = true
                    spawn { [self] in
                        for f in frames {
                            var b = [UInt8](f.dropFirst())
                            if b.isEmpty { continue }
                            b[0] = UInt8(TAPE_SRC)
                            let ty = Int(f[f.startIndex]) == T_AUDIO_U ? T_AUDIO_U : T_AUDIO
                            for r in rec { r.conn?.send(Frame(type: ty, data: Data(b))) }
                            Thread.sleep(forTimeInterval: 0.02)
                        }
                        lock.lock(); tapePlaying = false; lock.unlock()
                    }
                    logEvent("casetofon: redau \(frames.count) cadre → \(rec.count)")
                }
            default: break
            }
        case "sync":
            sendTo(p, jo(["t": "sync", "c": m.optLong("c"), "s": nowMs()]))
        case "poll":
            if !reg { break }
            let opts = strList(m.optArray("opts"), max: 6, len: 40)
            let q = cleanText(m.optString("q"), 120)
            if opts.count >= 2 && !q.isEmpty {
                let rec = targets(m.optString("to", "all"), p).filter { !isStaff($0) || $0.role == "actor" }
                var id = cleanText(m.optString("id"), 16)
                if id.isEmpty { pollSeq += 1; id = "p\(pollSeq)" }
                let pl = Poll(id: id, q: q, opts: opts, rec: Set(rec.map { $0.id }))
                poll = pl
                let out = jo(["t": "poll", "id": id, "q": q, "opts": opts])
                for r in rec { sendTo(r, out) }
                pushPoll(pl, true)
                logEvent("sondaj \"\(q)\" → \(rec.count)")
            }
        case "vote":
            let o = m.optInt("o", -1)
            if let pl = poll, !reg, m.optString("id") == pl.id, pl.rec.contains(p.id), o >= 0 && o < pl.opts.count {
                pl.votes[p.id] = o
                pushPoll(pl, true)
            }
        case "pollend":
            if !reg { break }
            if let pl = poll {
                pushPoll(pl, false)
                logEvent("sondaj încheiat: \(pl.votes.count)/\(pl.rec.count)")
                var cnt = [Int](repeating: 0, count: pl.opts.count)
                for v in pl.votes.values where v >= 0 && v < cnt.count { cnt[v] += 1 }
                let mx = cnt.max() ?? 0
                if mx > 0 && cnt.filter({ $0 == mx }).count == 1, let wi = cnt.firstIndex(of: mx) { fireRules("pollwin", nil, wi) }
                poll = nil
            }
        case "offhook": offhook(p); broadcastPeers()
        case "onhook", "hangup": hangup(p); broadcastPeers()
        case "dial": dial(p, m.optString("number").filter { $0.isASCII && $0.isNumber }); broadcastPeers()
        case "answer": answer(p); broadcastPeers()
        case "intervene": if p.listening { p.talking = m.optBool("on") }
        case "monitor": if reg { monitor(p, m.optString("number")); broadcastPeers() }
        case "couple": if reg { couple(m.optString("a"), m.optString("b")) }
        case "uncouple": if reg { uncouple(m.optString("id")) }
        case "autocouple": if reg { autoCouple() }
        case "setnum": if reg { setNum(m.optString("id"), m.optString("number").filter { $0.isASCII && $0.isNumber }) }
        case "setgroup":
            if reg, let q = peers[m.optString("id")] {
                q.group = cleanText(m.optString("group"), 24)
                persist(); broadcastPeers()
            }
        case "mute":
            if reg, let q = peers[m.optString("id")] {
                q.muted = m.optBool("on")
                persist(); broadcastPeers()
            }
        case "forget": if reg { forget(m.optString("id")) }
        case "ping": sendTo(p, jo(["t": "pong"]))
        case "udp": let l = cleanText(m.optString("lan"), 64); p.lan = privateCand(l) ? l : ""
        case "sms": if isStaff(p) { smsOut(p, m) }
        case "smsin":
            if p.smsgw {
                let from = String(m.optString("from").filter { ($0.isASCII && $0.isNumber) || $0 == "+" }.prefix(20))
                let text = cleanText(m.optString("text"), 500)
                if !from.isEmpty && !text.isEmpty {
                    let out = jo(["t": "msg", "from": "SMS", "num": from, "role": "sms", "text": text, "to": "staff"])
                    for q in plist() where isStaff(q) && q.online { sendTo(q, out) }
                    logEvent("SMS ← " + from)
                }
            }
        case "stat":
            p.bat = m.optInt("bat", -1)
            broadcastPeers()
        case "getlog": if reg { for l in log { sendTo(p, jo(["t": "log", "line": l])) } }
        case "opt":
            if reg {
                allowListen = m.optBool("listen", true)
                let pm = min(max(m.optInt("p2p", p2pMode), 0), 2)
                if pm == 0 && p2pMode != 0 { for c in calls.values { stopP2p(c) } }
                p2pMode = pm
                clickOnJoin = m.optBool("click", true)
                peerMsg = m.optBool("peerMsg", true)
                persist(); broadcastPeers()
            }
        case "peers": if isStaff(p) { sendTo(p, peersJson()) }
        default: break
        }
    }

    // ------------------------------------------------------------------ telephony

    private func reset(_ x: Peer) {
        x.callId = 0; x.listening = false; x.talking = false; x.silent = false; x.offhook = false
    }

    private func joinListen(_ p: Peer, _ c: Call, _ silent: Bool) {
        p.callId = c.id; p.listening = true; p.talking = false; p.silent = silent; p.offhook = true
        c.listeners.append(p)
        sendTo(p, jo(["t": "listening"]))
        logEvent((silent ? "monitor " : "ascultă ") + label(p) + " @ " + label(c.a) + "↔" + label(c.b))
        if clickOnJoin && !silent { for x in c.members() { sendTo(x, jo(["t": "click"])) } }
    }

    /// Lifting the receiver: dial tone, busy, or - on a party line - listening in on the neighbour's call.
    private func offhook(_ p: Peer) {
        if p.callId != 0 || p.offhook { return }
        let pa = partnerOf(p)
        let c = pa.flatMap { calls[$0.callId] }
        if let pa = pa, let c = c, c.connected, allowListen, !pa.listening {
            joinListen(p, c, false)
        } else if let pa = pa, pa.callId != 0 || pa.offhook {
            sendTo(p, jo(["t": "busy"]))
        } else {
            p.offhook = true
            sendTo(p, jo(["t": "dialtone"]))
        }
    }

    private func dial(_ p: Peer, _ number: String) {
        p.nCalls += 1
        if number.isEmpty || number.count > 16 || p.callId != 0 { return }
        if !p.offhook {
            offhook(p)
            if !p.offhook || p.callId != 0 { return }
        }
        var dest = ""
        var t = plist().first { $0.number == number }
        if number.count > 1 && number.hasPrefix("0") {
            t = isStaff(p) ? plist().first { $0.role == "gateway" && $0.online } : nil
            dest = String(number.dropFirst())
        }
        guard let tt = t, tt.online, tt !== p else { sendTo(p, jo(["t": "unknown"])); return }
        let tp = partnerOf(tt)
        if tt.callId != 0 || tt.offhook || (tp != nil && (tp!.callId != 0 || tp!.offhook)) {
            sendTo(p, jo(["t": "busy"]))
            return
        }
        callSeq += 1
        let c = Call(id: callSeq, a: p, b: tt)
        calls[c.id] = c
        p.callId = c.id
        tt.callId = c.id
        sendTo(p, jo(["t": "ringback"]))
        sendTo(tt, jo(["t": "ring", "num": p.number, "name": p.name, "pat": tt.slot, "who": tt.number, "dest": dest]))
        if let tp = tp, tp.online {
            sendTo(tp, jo(["t": "ring", "num": p.number, "name": "", "pat": tt.slot, "who": tt.number, "party": true]))
        }
        logEvent("apel " + label(p) + " → " + label(tt))
    }

    private func smsOut(_ p: Peer, _ m: JSONObject) {
        let to = String(m.optString("to").filter { ($0.isASCII && $0.isNumber) || $0 == "+" })
        let text = cleanText(m.optString("text"), 320)
        let gw = plist().first { $0.smsgw && $0.online }
        var why = ""
        if !(5...16).contains(to.count) || text.isEmpty { why = "bad_number" }
        else if gw == nil { why = "no_gateway" }
        else {
            let now = nowMs()
            if now - smsWinStart > 60000 { smsWinStart = now; smsCount = 0 }
            smsCount += 1
            if smsCount > 20 { why = "limit" }
        }
        if !why.isEmpty { sendTo(p, jo(["t": "smsres", "ok": false, "why": why])); return }
        sendTo(gw!, jo(["t": "smsout", "to": to, "text": text]))
        sendTo(p, jo(["t": "smsres", "ok": true]))
        logEvent("SMS → \(to) (" + label(p) + ")")
    }

    private var rdv: UdpPort?

    /// UDP rendezvous: a client sends "CUPLAJP|id|tok"; the hub records the address it sees (after NAT) and answers CUPLAJP-OK.
    private func rendezvousLoop() {
        guard let d = UdpPort(bind: port + 2, reuse: true) else { return }
        lock.lock(); rdv = d; lock.unlock()
        while isRunning() {
            guard let (data, from) = d.receive() else { continue }
            let t = (String(data: data, encoding: .utf8) ?? "").components(separatedBy: "|")
            if t.count == 3 && t[0] == "CUPLAJP" {
                var ok = false
                lock.lock()
                if let q = peers[t[1]], q.online, !q.tok.isEmpty, q.tok == t[2] { q.udp = from; ok = true }
                lock.unlock()
                if ok { d.send(Data("CUPLAJP-OK".utf8), to: from) }
            }
        }
    }

    private func sameNet(_ a: sockaddr_in, _ b: sockaddr_in) -> Bool {
        let x = addrString(a).0, y = addrString(b).0
        return x == y || (isPrivateHost(x) && isPrivateHost(y))
    }

    private func tryP2p(_ c: Call) {
        if p2pMode == 0 || !c.listeners.isEmpty || c.p2pKey != nil { return }
        let a = c.a, b = c.b
        if !a.p2p || !b.p2p || a.local || b.local || a.muted || b.muted { return }
        guard let ua = a.udp, let ub = b.udp else { return }
        if p2pMode == 1 && !sameNet(ua, ub) { return }
        let key = Sec.randomBytes(16)
        c.p2pKey = key
        func cands(_ p: Peer, _ u: sockaddr_in) -> [String] {
            let (h, pt) = addrString(u)
            return (p.lan.isEmpty ? [] : [p.lan]) + ["\(h):\(pt)"]
        }
        sendTo(a, jo(["t": "p2p", "key": hex(key), "cand": cands(b, ub), "init": true]))
        sendTo(b, jo(["t": "p2p", "key": hex(key), "cand": cands(a, ua), "init": false]))
        logEvent("P2P " + label(a) + " ↔ " + label(b))
    }

    private func stopP2p(_ c: Call) {
        if c.p2pKey == nil { return }
        c.p2pKey = nil
        sendTo(c.a, jo(["t": "p2pstop"]))
        sendTo(c.b, jo(["t": "p2pstop"]))
        logEvent("P2P oprit " + label(c.a) + " ↔ " + label(c.b))
    }

    private func connect(_ c: Call, _ forNum: String) {
        c.connected = true
        c.tConn = nowMs()
        c.a.offhook = true
        c.b.offhook = true
        sendTo(c.a, jo(["t": "connected", "peer": c.b.number, "name": c.b.name]))
        sendTo(c.b, jo(["t": "connected", "peer": c.a.number, "name": c.a.name, "forNum": forNum]))
        if let pb = partnerOf(c.b), pb.callId == 0 { sendTo(pb, jo(["t": "stopring"])) }
        tryP2p(c)
        logEvent("conectat " + label(c.a) + " ↔ " + label(c.b) + (forNum.isEmpty ? "" : " (răspuns greșit, era pentru \(forNum))"))
    }

    private func answer(_ p: Peer) {
        if let c = calls[p.callId] {
            if c.b === p && !c.connected { connect(c, "") }
            return
        }
        if p.offhook { return }
        guard let pa = partnerOf(p), let c2 = calls[pa.callId] else { return }
        if c2.b === pa && !c2.connected {
            let old = c2.b
            c2.b = p
            old.callId = 0
            p.callId = c2.id
            sendTo(old, jo(["t": "stopring"]))
            connect(c2, old.number)
        }
    }

    private func endCall(_ c: Call, _ by: Peer?, _ why: String) {
        stopP2p(c)
        calls[c.id] = nil
        var all = [c.a, c.b]
        all.append(contentsOf: c.listeners)
        let pb = partnerOf(c.b)
        for x in all {
            reset(x)
            sendTo(x, jo(["t": "ended", "why": why, "by": by?.number ?? ""]))
        }
        if !c.connected, let pb = pb { sendTo(pb, jo(["t": "stopring"])) }
        let dur = c.connected ? (nowMs() - c.tConn) / 1000 : 0
        logEvent("încheiat " + label(c.a) + " ↔ " + label(c.b) + " [" + why + (c.connected ? ", \(dur)s" : "") + "]")
    }

    private func hangup(_ p: Peer) {
        guard let c = calls[p.callId] else { reset(p); return }
        if p.listening {
            c.listeners.removeAll { $0 === p }
            let wasSilent = p.silent
            reset(p)
            sendTo(p, jo(["t": "ended", "why": "left"]))
            if clickOnJoin && !wasSilent { for x in c.members() { sendTo(x, jo(["t": "click"])) } }
            return
        }
        let rejected = !c.connected && p === c.b
        endCall(c, p, rejected ? "rejected" : "hangup")
    }

    private func sweep() {
        let now = nowMs()
        for c in Array(calls.values) where !c.connected && now - c.t0 > ringTimeoutMs { endCall(c, nil, "noanswer") }
    }

    private func monitor(_ p: Peer, _ number: String) {
        if p.callId != 0 { return }
        guard let t = plist().first(where: { $0.number == number }), let c = calls[t.callId], c.connected else { return }
        stopP2p(c)
        joinListen(p, c, true)
    }

    // ------------------------------------------------------------------ party lines

    private func couple(_ a: String, _ b: String) {
        guard let pa = peers[a], let pb = peers[b], pa !== pb else { return }
        detach(pa, push: false)
        detach(pb, push: false)
        lineSeq += 1
        pa.line = lineSeq; pa.slot = 0
        pb.line = lineSeq; pb.slot = 1
        pushLine(pa); pushLine(pb)
        logEvent("cuplaj " + label(pa) + " ⇄ " + label(pb))
        persist()
        broadcastPeers()
    }

    private func detach(_ p: Peer, push: Bool = true) {
        let pa = partnerOf(p)
        p.line = 0; p.slot = 0
        if let pa = pa { pa.line = 0; pa.slot = 0; pushLine(pa) }
        if push { pushLine(p) }
    }

    private func uncouple(_ id: String) {
        guard let p = peers[id] else { return }
        detach(p)
        persist()
        broadcastPeers()
    }

    private func autoCouple() {
        let sp = plist().filter { $0.role == "spectator" }
        for x in plist() { x.line = 0; x.slot = 0 }
        var i = 0
        while i + 1 < sp.count {
            lineSeq += 1
            sp[i].line = lineSeq; sp[i].slot = 0
            sp[i + 1].line = lineSeq; sp[i + 1].slot = 1
            i += 2
        }
        for x in plist() { pushLine(x) }
        logEvent("auto-perechi: \(sp.count / 2)")
        persist()
        broadcastPeers()
    }

    private func setNum(_ id: String, _ number: String) {
        guard let p = peers[id], !number.isEmpty, number.count <= 12 else { return }
        if plist().contains(where: { $0 !== p && $0.number == number }) { return }
        p.number = number
        sendTo(p, welcome(p))
        persist()
        broadcastPeers()
    }

    private func forget(_ id: String) {
        guard let p = peers[id] else { return }
        if p.role == "regie" && p.online { return }
        hangup(p)
        if let pa = partnerOf(p) { pa.line = 0; pa.slot = 0; pushLine(pa) }
        peers[id] = nil
        order.removeAll { $0 == id }
        p.conn?.close()
        logEvent("eliminat " + label(p))
        persist()
        broadcastPeers()
    }

    // ------------------------------------------------------------------ audio

    private func onAudio(_ p: Peer, _ data: Data, _ type: Int) {
        if p.muted { return }
        if tapeSrc == p.id {
            if nowMs() > tapeUntil || tapeBytes > 6_000_000 { tapeSrc = nil }
            else { tapeFrames.append(Data([UInt8(type)]) + data); tapeBytes += data.count + 1 }
        }
        var b = [UInt8](data)
        let mode = Int(b[0])
        b[0] = UInt8(mode == 1 ? MEDIA_PA_SRC : p.src)
        let f = Frame(type: type, data: Data(b))
        var dest = [Peer]()
        switch mode {
        case 0:
            if let c = calls[p.callId], c.connected, !(p.listening && !p.talking) {
                dest = (c.members() + c.listeners).filter { $0 !== p }
            }
        case 1: if isStaff(p) { dest = plist().filter { $0 !== p } }
        case 2: dest = plist().filter { $0 !== p && isStaff($0) }
        default: break
        }
        for d in dest { d.conn?.send(f) }
    }
}
