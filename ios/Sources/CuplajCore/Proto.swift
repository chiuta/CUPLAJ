import Foundation
#if canImport(Darwin)
import Darwin
#endif

public let VERSION = "1.10.0"
public let PORT = 8470
public let BEACON_PORT = 8471
public let T_JSON = 1
public let T_AUDIO = 2
public let T_AUDIO_U = 3
public let T_CHAL = 4
public let FLAG_ENC = 0x80
public let MEDIA_PA_SRC = 254
public let TAPE_SRC = 253

public struct Frame {
    public var type: Int
    public var data: Data
    public var plain: Bool = false
    public var bad: Bool = false
    public init(type: Int, data: Data, plain: Bool = false, bad: Bool = false) {
        self.type = type; self.data = data; self.plain = plain; self.bad = bad
    }
}

public struct IOErr: Error { public let msg: String; public init(_ m: String) { msg = m } }

/// A byte channel to one device. Blocking reads, to be used from a dedicated thread.
public protocol Chan: AnyObject {
    func read(_ n: Int) throws -> Data
    func write(_ d: Data) throws
    func setIdleTimeout(ms: Int)
    var origin: String { get }
    var loopback: Bool { get }
    var remote: Bool { get }
    var lowBandwidth: Bool { get }
    func close()
}

func isPrivateHost(_ h: String) -> Bool {
    if h == "::1" || h.hasPrefix("fe80") || h.hasPrefix("fc") || h.hasPrefix("fd") { return true }
    let p = h.split(separator: ".").compactMap { Int($0) }
    if p.count != 4 { return false }
    return p[0] == 10 || p[0] == 127 || (p[0] == 172 && (16...31).contains(p[1])) || (p[0] == 192 && p[1] == 168) || (p[0] == 169 && p[1] == 254)
}

public final class TcpChan: Chan {
    private var fd: Int32
    private let lock = NSLock()
    public let origin: String
    public let loopback: Bool
    public let remote: Bool
    public var lowBandwidth: Bool { false }

    public init(fd: Int32) {
        self.fd = fd
        var one: Int32 = 1
        setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &one, socklen_t(MemoryLayout<Int32>.size))
        setsockopt(fd, SOL_SOCKET, SO_KEEPALIVE, &one, socklen_t(MemoryLayout<Int32>.size))
        setsockopt(fd, SOL_SOCKET, SO_NOSIGPIPE, &one, socklen_t(MemoryLayout<Int32>.size))
        var ss = sockaddr_storage()
        var len = socklen_t(MemoryLayout<sockaddr_storage>.size)
        var host = "?"
        let ok = withUnsafeMutablePointer(to: &ss) { p -> Bool in
            p.withMemoryRebound(to: sockaddr.self, capacity: 1) { sp in getpeername(fd, sp, &len) == 0 }
        }
        if ok {
            var buf = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            let r = withUnsafePointer(to: &ss) { p -> Int32 in
                p.withMemoryRebound(to: sockaddr.self, capacity: 1) { sp in
                    getnameinfo(sp, len, &buf, socklen_t(buf.count), nil, 0, NI_NUMERICHOST)
                }
            }
            if r == 0 { host = String(cString: buf) }
        }
        if host.hasPrefix("::ffff:") { host = String(host.dropFirst(7)) }
        origin = host
        loopback = host == "127.0.0.1" || host == "::1"
        remote = !isPrivateHost(host)
    }

    /// Connects with a timeout (non-blocking connect + poll).
    public static func connect(host: String, port: Int, timeoutMs: Int = 5000) throws -> TcpChan {
        var hints = addrinfo()
        hints.ai_family = AF_UNSPEC
        hints.ai_socktype = SOCK_STREAM
        var res: UnsafeMutablePointer<addrinfo>?
        if getaddrinfo(host, String(port), &hints, &res) != 0 || res == nil { throw IOErr("resolve") }
        defer { freeaddrinfo(res) }
        var p = res
        var last = "connect"
        while let ai = p {
            let s = socket(ai.pointee.ai_family, ai.pointee.ai_socktype, ai.pointee.ai_protocol)
            if s >= 0 {
                let fl = fcntl(s, F_GETFL, 0)
                _ = fcntl(s, F_SETFL, fl | O_NONBLOCK)
                var r = Darwin.connect(s, ai.pointee.ai_addr, ai.pointee.ai_addrlen)
                if r != 0 && errno == EINPROGRESS {
                    var pfd = pollfd(fd: s, events: Int16(POLLOUT), revents: 0)
                    let pr = poll(&pfd, 1, Int32(timeoutMs))
                    if pr > 0 {
                        var err: Int32 = 0
                        var l = socklen_t(MemoryLayout<Int32>.size)
                        getsockopt(s, SOL_SOCKET, SO_ERROR, &err, &l)
                        r = err == 0 ? 0 : -1
                    } else { r = -1; last = "timeout" }
                }
                if r == 0 {
                    _ = fcntl(s, F_SETFL, fl)
                    return TcpChan(fd: s)
                }
                Darwin.close(s)
            }
            p = ai.pointee.ai_next
        }
        throw IOErr(last)
    }

    public func setIdleTimeout(ms: Int) {
        var tv = timeval(tv_sec: ms / 1000, tv_usec: Int32((ms % 1000) * 1000))
        setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))
    }

    public func read(_ n: Int) throws -> Data {
        if n == 0 { return Data() }
        var buf = [UInt8](repeating: 0, count: n)
        var got = 0
        while got < n {
            let r = buf.withUnsafeMutableBytes { raw in recv(fd, raw.baseAddress! + got, n - got, 0) }
            if r > 0 { got += r; continue }
            if r == 0 { throw IOErr("eof") }
            if errno == EINTR { continue }
            throw IOErr("recv")
        }
        return Data(buf)
    }

    public func write(_ d: Data) throws {
        let b = [UInt8](d)
        var sent = 0
        while sent < b.count {
            let r = b.withUnsafeBytes { raw in send(fd, raw.baseAddress! + sent, b.count - sent, 0) }
            if r > 0 { sent += r; continue }
            if r < 0 && errno == EINTR { continue }
            throw IOErr("send")
        }
    }

    public func close() {
        lock.lock(); defer { lock.unlock() }
        if fd >= 0 {
            shutdown(fd, SHUT_RDWR)
            Darwin.close(fd)
            fd = -1
        }
    }
}

/// Thread-safe bounded queue with timed poll.
final class FrameQueue {
    private var items = [Frame]()
    private let cond = NSCondition()
    private let cap: Int
    init(cap: Int) { self.cap = cap }

    func offer(_ f: Frame) -> Bool {
        cond.lock(); defer { cond.unlock() }
        if items.count >= cap { return false }
        items.append(f)
        cond.signal()
        return true
    }

    func poll(timeout: TimeInterval) -> Frame? {
        cond.lock(); defer { cond.unlock() }
        if items.isEmpty { _ = cond.wait(until: Date(timeIntervalSinceNow: timeout)) }
        return items.isEmpty ? nil : items.removeFirst()
    }

    var isEmpty: Bool { cond.lock(); defer { cond.unlock() }; return items.isEmpty }
}

/// One framed connection: type(1) | length(4, big-endian) | payload. Optional AES-GCM with a per-connection session key.
public final class Conn {
    public let chan: Chan
    private let key: Data?
    private let server: Bool
    private var nPrefix = Data()
    private var nCtr: UInt64 = 0
    private var rxCtr: UInt64 = 0
    private var skey: Data?
    private var pending: Frame?
    private let q = FrameQueue(cap: 400)
    private let lock = NSLock()
    private var _open = true
    public var maxFrame: Int

    public init(chan: Chan, key: Data? = nil, server: Bool = false) {
        self.chan = chan
        self.key = key
        self.server = server
        var p = Sec.randomBytes(4)
        p[0] = server ? 1 : 2
        nPrefix = p
        maxFrame = server ? 2048 : 200_000
    }

    public var open: Bool { lock.lock(); defer { lock.unlock() }; return _open }

    private func be32(_ n: Int) -> Data { Data([UInt8((n >> 24) & 0xFF), UInt8((n >> 16) & 0xFF), UInt8((n >> 8) & 0xFF), UInt8(n & 0xFF)]) }

    private func readHeader() throws -> (Int, Int) {
        let h = [UInt8](try chan.read(5))
        let n = (Int(h[1]) << 24) | (Int(h[2]) << 16) | (Int(h[3]) << 8) | Int(h[4])
        return (Int(h[0]), n)
    }

    /// Client with a key: send our nonce, receive the hub's, derive the session key. Call before startWriter.
    public func clientHandshake() throws {
        guard let mk = key else { return }
        let cn = Sec.randomBytes(16)
        try chan.write(Data([UInt8(T_CHAL)]) + be32(16) + cn)
        let (t, n) = try readHeader()
        if n < 0 || n > 4096 { throw IOErr("bad frame") }
        let b = try chan.read(n)
        if t == T_CHAL && n == 16 {
            skey = Sec.session(master: mk, serverNonce: b, clientNonce: cn)
        } else {
            pending = Frame(type: t & ~FLAG_ENC, data: b, bad: true)
        }
    }

    /// Hub: read the first frame. With a key it waits for T_CHAL and answers with its nonce. False = key mismatch.
    public func serverHandshake() throws -> Bool {
        let (t, n) = try readHeader()
        if n < 0 || n > maxFrame { throw IOErr("bad frame") }
        let b = try chan.read(n)
        guard let mk = key else {
            if t == T_CHAL { return false }
            pending = Frame(type: t & ~FLAG_ENC, data: b, bad: (t & FLAG_ENC) != 0)
            return true
        }
        if t != T_CHAL || n != 16 { return false }
        let sn = Sec.randomBytes(16)
        try chan.write(Data([UInt8(T_CHAL)]) + be32(16) + sn)
        skey = Sec.session(master: mk, serverNonce: sn, clientNonce: b)
        return true
    }

    public func startWriter() {
        let t = Thread { [self] in
            defer { close() }
            while open {
                guard let f = q.poll(timeout: 0.5) else { continue }
                if key != nil && !f.plain && skey == nil { continue }
                var out: Data
                if key != nil && !f.plain, let sk = skey {
                    nCtr += 1
                    var nonce = nPrefix
                    var c = nCtr
                    var ctr = [UInt8](repeating: 0, count: 8)
                    for i in stride(from: 7, through: 0, by: -1) { ctr[i] = UInt8(c & 0xFF); c >>= 8 }
                    nonce.append(contentsOf: ctr)
                    guard let ct = Sec.encrypt(key: sk, nonce: nonce, plain: f.data) else { break }
                    out = Data([UInt8(f.type | FLAG_ENC)]) + be32(12 + ct.count) + nonce + ct
                } else {
                    out = Data([UInt8(f.type)]) + be32(f.data.count) + f.data
                }
                do { try chan.write(out) } catch { break }
            }
        }
        t.start()
    }

    public func send(_ f: Frame) { if open { _ = q.offer(f) } }

    /// Control messages are never dropped: if the queue is full the connection closes and the client resyncs.
    public func sendJson(_ o: JSONObject) {
        guard open else { return }
        if !q.offer(Frame(type: T_JSON, data: jsonData(o))) { close() }
    }

    /// Plain even on an encrypted link (only for refusals, so the client learns why).
    public func sendJsonPlain(_ o: JSONObject) {
        if open { _ = q.offer(Frame(type: T_JSON, data: jsonData(o), plain: true)) }
    }

    public func readFrame() throws -> Frame {
        if let p = pending { pending = nil; return p }
        let (t, n) = try readHeader()
        if n < 0 || n > maxFrame { throw IOErr("bad frame") }
        let b = try chan.read(n)
        let enc = (t & FLAG_ENC) != 0
        let ty = t & ~FLAG_ENC
        guard key != nil else { return enc ? Frame(type: ty, data: Data(), bad: true) : Frame(type: ty, data: b) }
        guard let sk = skey else { return Frame(type: ty, data: Data(), bad: true) }
        if !enc { return Frame(type: ty, data: b, bad: true) }
        let bb = [UInt8](b)
        if n < 12 + 16 || ((bb[0] == 1) == server) { return Frame(type: ty, data: Data(), bad: true) }
        var ctr: UInt64 = 0
        for i in 4..<12 { ctr = (ctr << 8) | UInt64(bb[i]) }
        if ctr <= rxCtr { return Frame(type: ty, data: Data(), bad: true) }
        guard let pt = Sec.decrypt(key: sk, nonce: Data(bb[0..<12]), cipher: Data(bb[12..<n])) else {
            return Frame(type: ty, data: Data(), bad: true)
        }
        rxCtr = ctr
        return Frame(type: ty, data: pt)
    }

    public func close() {
        lock.lock()
        _open = false
        lock.unlock()
        chan.close()
    }
}

/// "host" or "host:port"; IPv6 is written [::1]:8470.
public func parseHostPort(_ s: String, defPort: Int = PORT) -> (String, Int) {
    let t = s.trimmingCharacters(in: .whitespaces)
    if t.hasPrefix("[") , let e = t.firstIndex(of: "]") {
        let host = String(t[t.index(after: t.startIndex)..<e])
        let rest = String(t[t.index(after: e)...]).replacingOccurrences(of: ":", with: "", options: .anchored)
        return (host, Int(rest) ?? defPort)
    }
    if let i = t.lastIndex(of: ":"), t.firstIndex(of: ":") == i {
        if let p = Int(t[t.index(after: i)...]), (1...65535).contains(p) { return (String(t[..<i]), p) }
    }
    return (t, defPort)
}

/// Only local addresses (RFC 1918 / link-local) are accepted as P2P candidates.
public func privateCand(_ s: String) -> Bool {
    let (h, p) = parseHostPort(s, defPort: -1)
    if !(1024...65535).contains(p) { return false }
    let parts = h.split(separator: ".", omittingEmptySubsequences: false).map { Int($0) }
    if parts.count != 4 || parts.contains(where: { $0 == nil || $0! < 0 || $0! > 255 }) { return false }
    return isPrivateHost(h)
}

/// Digits and a leading "+": no USSD codes (*...#) or call-forwarding codes.
public func safeNumber(_ n: String) -> String {
    let d = n.filter { $0.isNumber && $0.isASCII || $0 == "+" }
    let plus = d.hasPrefix("+")
    return (plus ? "+" : "") + String(d.filter { $0 != "+" }.prefix(20))
}

// ------------------------------------------------------------------ shared clock, timed cues, colour

/// The hub's clock as seen by the client: keep the estimate with the lowest RTT among the last 8.
public final class ClockSync {
    private var rtts = [Int64](repeating: Int64.max, count: 8)
    private var offs = [Int64](repeating: 0, count: 8)
    private var n = 0
    private let lock = NSLock()
    public init() {}

    public func add(t0: Int64, hub: Int64, t1: Int64) {
        lock.lock(); defer { lock.unlock() }
        let rtt = t1 - t0
        if rtt < 0 || rtt > 5000 { return }
        let i = n % 8
        n += 1
        rtts[i] = rtt
        offs[i] = hub - (t0 + t1) / 2
    }

    public func offset() -> Int64 {
        lock.lock(); defer { lock.unlock() }
        var b = -1
        for i in 0..<8 where rtts[i] != Int64.max && (b < 0 || rtts[i] < rtts[b]) { b = i }
        return b < 0 ? 0 : offs[b]
    }

    public func ready() -> Bool { lock.lock(); defer { lock.unlock() }; return rtts.contains { $0 != Int64.max } }
    public func hubNow(_ local: Int64) -> Int64 { local + offset() }

    /// How long to wait (ms) for a cue stamped `at` in hub time; nil = too far ahead or more than 10 s late.
    public func delayFor(at: Int64, local: Int64) -> Int64? {
        let d = at - hubNow(local)
        return (d > 600_000 || d < -10_000) ? nil : max(0, d)
    }
}

/// "@3.5;text" -> (3500 ms, "text"); no prefix -> (0, text).
public func parseAt(_ text: String) -> (Int64, String) {
    guard text.hasPrefix("@"), let semi = text.firstIndex(of: ";") else { return (0, text) }
    let head = String(text[text.index(after: text.startIndex)..<semi])
    let parts = head.split(separator: ".", omittingEmptySubsequences: false)
    guard parts.count >= 1, parts.count <= 2, let sec = Int(parts[0]), parts[0].count <= 3, parts[0].allSatisfy({ $0.isASCII && $0.isNumber }) else { return (0, text) }
    var tenth = 0
    if parts.count == 2 {
        guard parts[1].count == 1, let t = Int(parts[1]) else { return (0, text) }
        tenth = t
    }
    let ms = min(Int64(sec) * 1000 + Int64(tenth) * 100, 600_000)
    return (ms, String(text[text.index(after: semi)...]))
}

/// Full-screen colour cue. Rate is capped at 2 Hz (photosensitivity), duration between 0.2 s and 10 min.
public struct ColorPlan {
    public let r: Double, g: Double, b: Double
    public let ms: Int64
    public let hz: Double
}

public func colorPlan(_ text: String) -> ColorPlan? {
    let p = text.split(separator: ";", omittingEmptySubsequences: false).map { $0.trimmingCharacters(in: .whitespaces) }
    guard var c = p.first else { return nil }
    if c.hasPrefix("#") { c.removeFirst() }
    guard c.count == 6, let v = UInt32(c, radix: 16) else { return nil }
    let secs = p.count > 1 ? (Double(p[1]) ?? 3.0) : 3.0
    let hz = min(max(p.count > 2 ? (Double(p[2]) ?? 0.0) : 0.0, 0.0), 2.0)
    let ms = min(max(Int64(secs * 1000), 200), 600_000)
    return ColorPlan(r: Double((v >> 16) & 0xFF) / 255, g: Double((v >> 8) & 0xFF) / 255, b: Double(v & 0xFF) / 255, ms: ms, hz: hz)
}
