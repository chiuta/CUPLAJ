import Foundation
#if canImport(Darwin)
import Darwin
#endif

/// IPv4 UDP socket with a receive timeout (used for the P2P audio link and the hub rendezvous).
public final class UdpPort {
    public let fd: Int32
    public private(set) var port: Int = 0
    public init?(bind bindPort: Int, reuse: Bool = false) {
        fd = socket(AF_INET, SOCK_DGRAM, 0)
        if fd < 0 { return nil }
        var one: Int32 = 1
        if reuse { setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &one, socklen_t(MemoryLayout<Int32>.size)) }
        var a = sockaddr_in()
        a.sin_family = sa_family_t(AF_INET)
        a.sin_port = in_port_t(UInt16(bindPort).bigEndian)
        a.sin_addr = in_addr(s_addr: INADDR_ANY.bigEndian)
        let r = withUnsafePointer(to: &a) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { bind(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) } }
        if r != 0 { Darwin.close(fd); return nil }
        var tv = timeval(tv_sec: 0, tv_usec: 300_000)
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))
        var l = sockaddr_in(); var ll = socklen_t(MemoryLayout<sockaddr_in>.size)
        _ = withUnsafeMutablePointer(to: &l) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { getsockname(fd, $0, &ll) } }
        port = Int(UInt16(bigEndian: l.sin_port))
    }

    public func close() { Darwin.close(fd) }

    public static func resolve(_ host: String, _ port: Int) -> sockaddr_in? {
        var hints = addrinfo(); hints.ai_family = AF_INET; hints.ai_socktype = SOCK_DGRAM
        var res: UnsafeMutablePointer<addrinfo>?
        guard getaddrinfo(host, String(port), &hints, &res) == 0, let ai = res else { return nil }
        defer { freeaddrinfo(res) }
        return ai.pointee.ai_addr.withMemoryRebound(to: sockaddr_in.self, capacity: 1) { $0.pointee }
    }

    public func send(_ data: Data, to a: sockaddr_in) {
        var addr = a
        let b = [UInt8](data)
        _ = b.withUnsafeBytes { p in
            withUnsafePointer(to: &addr) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { sendto(fd, p.baseAddress, b.count, 0, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) } }
        }
    }

    public func receive() -> (Data, sockaddr_in)? {
        var buf = [UInt8](repeating: 0, count: 2048)
        var from = sockaddr_in(); var len = socklen_t(MemoryLayout<sockaddr_in>.size)
        let n = withUnsafeMutablePointer(to: &from) { fp in fp.withMemoryRebound(to: sockaddr.self, capacity: 1) { recvfrom(fd, &buf, buf.count, 0, $0, &len) } }
        if n <= 0 { return nil }
        return (Data(buf[0..<n]), from)
    }
}

public func addrString(_ a: sockaddr_in) -> (String, Int) {
    var ip = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN)); var ad = a.sin_addr
    inet_ntop(AF_INET, &ad, &ip, socklen_t(INET_ADDRSTRLEN))
    return (String(cString: ip), Int(UInt16(bigEndian: a.sin_port)))
}

private func sameAddr(_ a: sockaddr_in, _ b: sockaddr_in) -> Bool { a.sin_addr.s_addr == b.sin_addr.s_addr && a.sin_port == b.sin_port }

/// Direct encrypted audio between two phones over UDP (same wire format as the Android P2pLink):
/// packet = type(1) | nonce(12) | AES-GCM(data); types 1 probe, 2 confirm, 3 audio.
public final class P2pLink {
    private let sock: UdpPort
    private let onPcm: (Data) -> Void
    private let lock = NSLock()
    private var run = true
    private var alive = 2
    private var hubAddr: sockaddr_in?
    private var punch = Data()
    private var key: Data?
    private var cands = [sockaddr_in]()
    private var dir = 0
    private var _peer: sockaddr_in?
    private var ctr: UInt64 = 0
    private var rx: UInt64 = 0
    private var prefix = [UInt8](Sec.randomBytes(4))
    private var probeUntil: Int64 = 0
    public private(set) var hubSeen = false
    public private(set) var rxPackets = 0

    public init?(onPcm: @escaping (Data) -> Void) {
        guard let s = UdpPort(bind: 0) else { return nil }
        sock = s; self.onPcm = onPcm
        Thread { [self] in rxLoop() }.start()
        Thread { [self] in txLoop() }.start()
    }

    public var port: Int { sock.port }
    public var up: Bool { lock.lock(); defer { lock.unlock() }; return _peer != nil && key != nil }

    public func lanCandidate(_ ips: [String]) -> String { ips.first(where: { !$0.isEmpty }).map { "\($0):\(port)" } ?? "" }

    public func registerAt(host: String, port p: Int, id: String, tok: String) {
        guard let a = UdpPort.resolve(host, p) else { return }
        lock.lock(); hubAddr = a; punch = Data("CUPLAJP|\(id)|\(tok)".utf8); lock.unlock()
    }

    public func begin(key k: Data, candidates: [String], initiator: Bool) {
        lock.lock()
        key = k; dir = initiator ? 1 : 2; prefix[0] = UInt8(dir)
        cands = candidates.compactMap { s in
            let (h, p) = parseHostPort(s, defPort: -1)
            return p > 0 && !h.isEmpty ? UdpPort.resolve(h, p) : nil
        }
        _peer = nil; rx = 0
        probeUntil = nowMs() + 4000
        lock.unlock()
    }

    public func end() { lock.lock(); key = nil; _peer = nil; cands = []; lock.unlock() }
    public func close() { lock.lock(); run = false; lock.unlock() }

    private func loopDone() { lock.lock(); alive -= 1; let last = alive == 0; lock.unlock(); if last { sock.close() } }

    private func pack(_ type: UInt8, _ data: Data) -> Data? {
        lock.lock(); defer { lock.unlock() }
        guard let k = key else { return nil }
        ctr += 1
        var n = Data(prefix)
        var c = ctr
        var tail = [UInt8](repeating: 0, count: 8)
        for i in (0..<8).reversed() { tail[i] = UInt8(c & 0xFF); c >>= 8 }
        n.append(contentsOf: tail)
        guard let ct = Sec.encrypt(key: k, nonce: n, plain: data) else { return nil }
        return Data([type]) + n + ct
    }

    /// Sends 320 bytes of PCM straight to the partner; false if the direct path is not up.
    public func sendPcm(_ pcm: Data) -> Bool {
        lock.lock(); let p = _peer; lock.unlock()
        guard let to = p, let b = pack(3, G711.encode(pcm)) else { return false }
        sock.send(b, to: to)
        return true
    }

    private func isRunning() -> Bool { lock.lock(); defer { lock.unlock() }; return run }

    private func txLoop() {
        defer { loopDone() }
        var tick = 0
        while isRunning() {
            Thread.sleep(forTimeInterval: 0.2)
            lock.lock(); let h = hubAddr; let pu = punch; let seen = hubSeen; let k = key; let pr = _peer; let cs = cands; let until = probeUntil; lock.unlock()
            if let h = h, !pu.isEmpty, tick % 25 == 0 || (!seen && tick % 5 == 0) { sock.send(pu, to: h) }
            tick += 1
            if k != nil, pr == nil, nowMs() < until, let p = pack(1, Data([1])) { for c in cs { sock.send(p, to: c) } }
        }
    }

    private func rxLoop() {
        defer { loopDone() }
        while isRunning() {
            guard let (buf, from) = sock.receive() else { continue }
            if buf.count == 10, String(data: buf, encoding: .utf8) == "CUPLAJP-OK" { lock.lock(); hubSeen = true; lock.unlock(); continue }
            lock.lock(); let k = key; let d = dir; lock.unlock()
            guard let kk = k, buf.count >= 1 + 12 + 16 else { continue }
            let b = [UInt8](buf)
            let type = b[0]
            let n = Data(b[1..<13])
            if Int(n[n.startIndex]) == d { continue }   // reflection
            var c: UInt64 = 0
            for i in 4..<12 { c = (c << 8) | UInt64(n[n.startIndex + i]) }
            guard let pt = Sec.decrypt(key: kk, nonce: n, cipher: Data(b[13...])) else { continue }
            switch type {
            case 1:
                lock.lock(); if _peer == nil { _peer = from }; lock.unlock()
                if let a = pack(2, Data([1])) { sock.send(a, to: from) }
            case 2:
                lock.lock(); if _peer == nil { _peer = from }; lock.unlock()
            case 3:
                lock.lock()
                if c <= rx { lock.unlock(); continue }
                rx = c; rxPackets += 1
                if _peer == nil { _peer = from }
                lock.unlock()
                onPcm(G711.decode(pt))
            default: break
            }
        }
    }
}
