import Foundation

/// The client: keeps the connection to the hub (auto-reconnect, heartbeat, optional encryption) and carries JSON and audio.
public final class Net {
    private let onJson: (JSONObject) -> Void
    private let onAudio: (Int, Data) -> Void
    private let onState: (String, String) -> Void   // ("connecting", hostPort) | ("lost", "")
    private let lock = NSLock()
    private var conn: Conn?
    private var run = false
    private var gen = 0

    /// true = send mu-law audio (less traffic, for internet / mobile data).
    public var compress = false
    /// nil = automatic (compress when the hub is not on the local network).
    public var forceCompress: Bool?
    public private(set) var remote = false

    public init(onJson: @escaping (JSONObject) -> Void, onAudio: @escaping (Int, Data) -> Void, onState: @escaping (String, String) -> Void) {
        self.onJson = onJson; self.onAudio = onAudio; self.onState = onState
    }

    public func start(host: String, hello: JSONObject, key: Data? = nil) {
        stop()
        lock.lock(); run = true; gen += 1; let my = gen; lock.unlock()
        let t = Thread { [self] in loop(my, host, hello, key) }
        t.start()
    }

    public func stop() {
        lock.lock(); run = false; gen += 1; let c = conn; conn = nil; lock.unlock()
        c?.close()
    }

    private func alive(_ my: Int) -> Bool { lock.lock(); defer { lock.unlock() }; return run && gen == my }

    private func loop(_ my: Int, _ hostPort: String, _ hello: JSONObject, _ key: Data?) {
        let (host, port) = parseHostPort(hostPort)
        var delay: TimeInterval = 1
        while alive(my) {
            do {
                onState("connecting", hostPort)
                let ch = try TcpChan.connect(host: host, port: port, timeoutMs: 5000)
                ch.setIdleTimeout(ms: 45000) // the hub answers pings every 15 s; 45 s of silence = dead link
                remote = ch.remote
                compress = forceCompress ?? (ch.remote || ch.lowBandwidth)
                let c = Conn(chan: ch, key: key, server: false)
                lock.lock(); conn = c; lock.unlock()
                try c.clientHandshake()
                c.startWriter()
                c.sendJson(hello)
                delay = 1
                let pinger = Thread { [self] in
                    while alive(my) && c.open {
                        Thread.sleep(forTimeInterval: 15)
                        c.sendJson(jo(["t": "ping"]))
                    }
                }
                pinger.start()
                while alive(my) {
                    let f = try c.readFrame()
                    if f.bad {
                        // hub without / with another key: the only message accepted in clear is the refusal
                        if f.type == T_JSON, !f.data.isEmpty, let m = parseJSON(f.data), m.optString("t") == "denied" { onJson(m) }
                        break
                    }
                    if f.type == T_JSON {
                        if let m = parseJSON(f.data) { onJson(m) }
                    } else if f.type == T_AUDIO && f.data.count > 1 {
                        onAudio(Int(f.data[f.data.startIndex]), f.data)
                    } else if f.type == T_AUDIO_U && f.data.count > 1 {
                        let pcm = G711.decode(f.data.dropFirst())
                        onAudio(Int(f.data[f.data.startIndex]), Data([f.data[f.data.startIndex]]) + pcm)
                    }
                }
            } catch {
                // connection failed or ended
            }
            lock.lock(); let c = conn; lock.unlock()
            c?.close()
            if alive(my) {
                onState("lost", "")
                Thread.sleep(forTimeInterval: delay)
                delay = min(delay * 2, 5)
            }
        }
    }

    public func send(_ o: JSONObject) {
        lock.lock(); let c = conn; lock.unlock()
        c?.sendJson(o)
    }

    /// `mode`: 0 = call, 1 = PA (host to everyone), 2 = to staff. `pcm` is 16-bit little-endian, 8 kHz mono, 160 samples.
    public func sendAudio(mode: Int, pcm: Data) {
        let payload = compress ? G711.encode(pcm) : pcm
        lock.lock(); let c = conn; lock.unlock()
        c?.send(Frame(type: compress ? T_AUDIO_U : T_AUDIO, data: Data([UInt8(mode)]) + payload))
    }
}
