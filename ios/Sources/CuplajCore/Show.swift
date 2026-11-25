import Foundation

// ====================================================================== audio priority (ducking)

/// Gain for everything else while the host / PA speaks: drops fast, comes back slowly. One step per 20 ms frame.
public final class Ducker {
    public private(set) var gain = 1.0
    private var hold = 0
    private let level: Double
    private let holdFrames: Int
    public init(level: Double = 0.3, holdFrames: Int = 8) { self.level = level; self.holdFrames = holdFrames }

    public func step(_ priorityActive: Bool) -> Double {
        if priorityActive { hold = holdFrames } else if hold > 0 { hold -= 1 }
        let target = hold > 0 ? level : 1.0
        gain += (target - gain) * (target < gain ? 0.5 : 0.08)
        return gain
    }
}

// ====================================================================== OSC

public enum OscArg { case int(Int32), float(Float), str(String)
    var text: String {
        switch self {
        case .int(let i): return String(i)
        case .float(let f): return String(f)
        case .str(let s): return s
        }
    }
}

public enum Osc {
    public static let CUE_KIND_NAMES: Set<String> = ["text", "vibrate", "ring", "flash", "speaker", "contact", "era", "say", "sound", "blackout", "fakecall", "realcall", "panic", "color", "poll", "pollend", "image", "video", "clip", "caption", "dmx", "midi", "morse", "house"]

    private static func str(_ b: inout [UInt8], _ s: String) {
        b.append(contentsOf: Array(s.utf8)); b.append(0)
        while b.count % 4 != 0 { b.append(0) }
    }

    private static func be(_ v: UInt32) -> [UInt8] { [UInt8(v >> 24), UInt8((v >> 16) & 0xFF), UInt8((v >> 8) & 0xFF), UInt8(v & 0xFF)] }

    /// OSC 1.0 message with text, integer or float arguments.
    public static func encode(_ addr: String, _ args: [OscArg]) -> Data {
        var b = [UInt8]()
        str(&b, addr)
        var tags = ","
        for a in args {
            switch a { case .int: tags += "i"; case .float: tags += "f"; case .str: tags += "s" }
        }
        str(&b, tags)
        for a in args {
            switch a {
            case .int(let i): b += be(UInt32(bitPattern: i))
            case .float(let f): b += be(f.bitPattern)
            case .str(let s): str(&b, s)
            }
        }
        return Data(b)
    }

    public struct Msg { public let addr: String; public let args: [OscArg] }

    private static func readStr(_ d: [UInt8], _ p: inout Int) -> String? {
        var e = p
        while e < d.count && d[e] != 0 { e += 1 }
        if e >= d.count || e - p > 512 { return nil }
        let s = String(decoding: d[p..<e], as: UTF8.self)
        p = (e + 4) & ~3
        return p > d.count ? nil : s
    }

    /// Decodes one message (no bundles); nil if malformed. Never traps.
    public static func decode(_ data: Data) -> Msg? {
        let x = [UInt8](data)
        if x.count < 8 || x.count > 2048 { return nil }
        var p = 0
        guard let addr = readStr(x, &p), addr.hasPrefix("/") else { return nil }
        guard let tags = readStr(x, &p), tags.hasPrefix(","), tags.count <= 17 else { return nil }
        var out = [OscArg]()
        for t in tags.dropFirst() {
            switch t {
            case "i":
                if p + 4 > x.count { return nil }
                let v = (UInt32(x[p]) << 24) | (UInt32(x[p + 1]) << 16) | (UInt32(x[p + 2]) << 8) | UInt32(x[p + 3])
                out.append(.int(Int32(bitPattern: v))); p += 4
            case "f":
                if p + 4 > x.count { return nil }
                let v = (UInt32(x[p]) << 24) | (UInt32(x[p + 1]) << 16) | (UInt32(x[p + 2]) << 8) | UInt32(x[p + 3])
                out.append(.float(Float(bitPattern: v))); p += 4
            case "s":
                guard let s = readStr(x, &p) else { return nil }
                out.append(.str(s))
            default: return nil
            }
        }
        return Msg(addr: addr, args: out)
    }

    public struct Cmd { public let kind: String; public var to = "all"; public var cue = ""; public var text = "" }

    /// Addresses must start with `/cuplaj/<PIN>/`: OSC has no authentication, the hub PIN keeps others on the network out.
    public static func command(_ m: Msg, pin: String) -> Cmd? {
        let pre = "/cuplaj/"
        guard m.addr.hasPrefix(pre), !pin.isEmpty else { return nil }
        let rest = String(m.addr.dropFirst(pre.count))
        guard let i = rest.firstIndex(of: "/") else { return nil }
        if !Sec.same(String(rest[..<i]), pin) { return nil }
        switch String(rest[rest.index(after: i)...]) {
        case "go": return Cmd(kind: "go")
        case "panic": return Cmd(kind: "panic")
        case "stop": return Cmd(kind: "stop")
        case "cue":
            let a = m.args.map { $0.text }
            if a.count < 2 || !CUE_KIND_NAMES.contains(a[1]) { return nil }
            return Cmd(kind: "cue", to: String(a[0].prefix(40)), cue: a[1], text: a.count > 2 ? String(a[2].prefix(200)) : "")
        default: return nil
        }
    }
}

// ====================================================================== media files through the hub

public enum Media {
    public static let MAX_BYTES = 8 * 1024 * 1024
    public static let CHUNK = 12000
    public static let MAX_B64 = 17000
    public static let KINDS = ["image": ".img", "video": ".vid", "clip": ".snd"]

    public static func validId(_ s: String) -> Bool {
        !s.isEmpty && s.count <= 16 && s.allSatisfy { $0.isASCII && ($0.isLowercase || $0.isNumber) }
    }

    /// (index, total, base64) chunks to send.
    public static func chunks(_ data: Data) -> [(Int, Int, String)] {
        let n = max(1, (data.count + CHUNK - 1) / CHUNK)
        return (0..<n).map { i in
            let lo = i * CHUNK, hi = min(data.count, (i + 1) * CHUNK)
            return (i, n, data.subdata(in: lo..<hi).base64EncodedString())
        }
    }
}

/// Receives chunks in order into a file inside `dir`; checks size and SHA-256 at the end. The path never depends on received text.
public final class MediaAsm {
    private final class Tr {
        let url: URL; let kind: String; let size: Int; let sha: String; let n: Int
        var next = 0; var data = Data()
        init(url: URL, kind: String, size: Int, sha: String, n: Int) { self.url = url; self.kind = kind; self.size = size; self.sha = sha; self.n = n }
    }
    private let dir: URL
    private var open = [String: Tr]()
    private let lock = NSLock()
    public init(dir: URL) { self.dir = dir }

    public func file(_ id: String, _ kind: String) -> URL? {
        guard Media.validId(id), let ext = Media.KINDS[kind] else { return nil }
        let u = dir.appendingPathComponent(id + ext)
        return FileManager.default.fileExists(atPath: u.path) ? u : nil
    }

    /// "ok" chunk received, "done" file complete and verified, otherwise the reason it was rejected.
    public func add(_ m: JSONObject) -> String {
        lock.lock(); defer { lock.unlock() }
        let id = m.optString("mid"), kind = m.optString("kind")
        guard Media.validId(id), let ext = Media.KINDS[kind] else { return "bad_id" }
        let i = m.optInt("i", -1)
        let b64 = m.optString("b64")
        if b64.count > Media.MAX_B64 { return "chunk_big" }
        var t = open[id]
        if t == nil {
            if i != 0 { return "order" }
            let size = Int(m.optLong("size", -1)), n = m.optInt("n", -1), sha = m.optString("sha")
            let hexOk = sha.count == 64 && sha.allSatisfy { $0.isHexDigit && !$0.isUppercase }
            if size <= 0 || size > Media.MAX_BYTES || n < 1 || n > 1000 || !hexOk { return "bad_meta" }
            if open.count >= 3 { return "busy" }
            try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
            t = Tr(url: dir.appendingPathComponent(id + ext), kind: kind, size: size, sha: sha, n: n)
            open[id] = t
        }
        guard let tr = t else { return "io" }
        if i != tr.next { open[id] = nil; return "order" }
        guard let bytes = Data(base64Encoded: b64) else { open[id] = nil; return "bad_data" }
        if tr.data.count + bytes.count > tr.size { open[id] = nil; return "overflow" }
        tr.data.append(bytes)
        tr.next += 1
        if tr.next < tr.n { return "ok" }
        open[id] = nil
        if tr.data.count != tr.size || Sec.sha256Hex(tr.data) != tr.sha { return "bad_hash" }
        do { try tr.data.write(to: tr.url, options: .atomic) } catch { return "io" }
        return "done"
    }

    /// Deletes the oldest files above `maxBytes` in total.
    public func trim(maxBytes: Int) {
        lock.lock(); defer { lock.unlock() }
        let fm = FileManager.default
        guard let urls = try? fm.contentsOfDirectory(at: dir, includingPropertiesForKeys: [.contentModificationDateKey, .fileSizeKey]) else { return }
        var items = urls.compactMap { u -> (URL, Date, Int)? in
            guard let v = try? u.resourceValues(forKeys: [.contentModificationDateKey, .fileSizeKey]) else { return nil }
            return (u, v.contentModificationDate ?? .distantPast, v.fileSize ?? 0)
        }.sorted { $0.1 < $1.1 }
        var tot = items.reduce(0) { $0 + $1.2 }
        while tot > maxBytes, !items.isEmpty {
            let f = items.removeFirst()
            tot -= f.2
            try? fm.removeItem(at: f.0)
        }
    }
}

// ====================================================================== multilingual and personalised text

/// "en=Hello|ro=Salut|fr=Bonjour" -> text in the phone's language (then en, then the first). Plain text is unchanged.
public func pickLang(_ text: String, _ lang: String) -> String {
    let parts = text.components(separatedBy: "|")
    func ok(_ s: String) -> Bool {
        let c = Array(s)
        return c.count >= 3 && c[0].isASCII && c[0].isLowercase && c[0].isLetter && c[1].isASCII && c[1].isLowercase && c[1].isLetter && c[2] == "="
    }
    if parts.count < 2 || !parts.allSatisfy(ok) { return text }
    var m = [String: String]()
    for p in parts { m[String(p.prefix(2))] = String(p.dropFirst(3)) }
    return m[lang] ?? m["en"] ?? String(parts[0].dropFirst(3))
}

/// {name} and {number} of the receiving phone; no other code, so text cannot leak other data.
public func expandTokens(_ text: String, name: String, number: String) -> String {
    let n = String(String.UnicodeScalarView(name.unicodeScalars.filter { $0.value >= 32 }).prefix(40))
    let d = String(number.filter { ($0.isASCII && $0.isNumber) || $0 == "+" }.prefix(20))
    return text.replacingOccurrences(of: "{name}", with: n).replacingOccurrences(of: "{number}", with: d)
}

// ====================================================================== voice effects (8 kHz, 20 ms frames)

public final class VoiceFx {
    public static let KINDS = ["robot", "radio", "echo", "deep", "high", "lofi"]
    private static let L = 400
    public let kind: String
    private var dl = [Float](repeating: 0, count: 4096)
    private var w = 0
    private var tap = 0.0
    private var ph = 0.0
    private var lp1: Float = 0, lp2: Float = 0, hp1: Float = 0, hpPrev: Float = 0
    private var ech = [Float](repeating: 0, count: 1440)
    private var ei = 0
    private var hold: Float = 0
    private var hc = 0

    public init(_ kind: String) { self.kind = kind }

    private func rd(_ p: Double) -> Float {
        let d = p * Double(VoiceFx.L) + 2
        let pos = Double(w) - d
        let i0 = Int(floor(pos))
        let fr = Float(pos - Double(i0))
        let n = dl.count
        let a = dl[((i0 % n) + n) % n]
        let b = dl[(((i0 + 1) % n) + n) % n]
        return a + (b - a) * fr
    }

    private func shift(_ x: Float, _ r: Double) -> Float {
        dl[w] = x
        let adv = (1.0 - r) / Double(VoiceFx.L)
        tap = (((tap + adv).truncatingRemainder(dividingBy: 1.0)) + 1.0).truncatingRemainder(dividingBy: 1.0)
        let p2 = (tap + 0.5).truncatingRemainder(dividingBy: 1.0)
        let s = sin(Double.pi * tap)
        let g1 = Float(s * s)
        let g2 = 1 - g1
        let y = rd(tap) * g1 + rd(p2) * g2
        w = (w + 1) % dl.count
        return y
    }

    public func process(_ f: [Int16]) -> [Int16] {
        var o = [Int16](repeating: 0, count: f.count)
        for i in 0..<f.count {
            let x = Float(f[i]) / 32768
            var y: Float
            switch kind {
            case "robot":
                ph += 2 * Double.pi * 55.0 / 8000
                if ph > 2 * Double.pi { ph -= 2 * Double.pi }
                y = x * Float(sin(ph)) * 1.6
            case "radio":
                hp1 += 0.31 * (x - hp1); let h1 = x - hp1
                hpPrev += 0.31 * (h1 - hpPrev); let hpv = h1 - hpPrev
                lp1 += 0.8 * (hpv - lp1); lp2 += 0.8 * (lp1 - lp2)
                y = Float(tanh(Double(lp2 * 3))) * 0.8
            case "echo":
                let d = ech[ei]; ech[ei] = x + d * 0.4; ei = (ei + 1) % ech.count; y = x + d * 0.5
            case "deep": y = shift(x, 0.75)
            case "high": y = shift(x, 1.5)
            case "lofi":
                if hc % 3 == 0 { hold = (x * 15).rounded() / 15 }
                hc += 1
                y = hold
            default: y = x
            }
            o[i] = Int16(max(-1, min(1, y)) * 32767)
        }
        return o
    }

    /// PCM16 little-endian frame from the microphone.
    public func processBytes(_ b: Data) -> Data {
        let a = [UInt8](b)
        let n = a.count / 2
        let s = (0..<n).map { Int16(bitPattern: UInt16(a[2 * $0]) | (UInt16(a[2 * $0 + 1]) << 8)) }
        let r = process(s)
        var out = [UInt8](repeating: 0, count: a.count)
        for i in 0..<n { let u = UInt16(bitPattern: r[i]); out[2 * i] = UInt8(u & 0xFF); out[2 * i + 1] = UInt8(u >> 8) }
        return Data(out)
    }
}

// ====================================================================== rules (zone, poll, meter -> reply)

public struct Rule {
    public let id: String, on: String, arg: Int, meterKind: String, to: String, kind: String, text: String, once: Bool
}

public enum Rules {
    public static let ALLOWED: Set<String> = Osc.CUE_KIND_NAMES.subtracting(["poll", "pollend", "realcall", "dmx", "midi"]).union(["goto"])

    static func validTarget(_ to: String) -> Bool {
        if ["all", "staff", "spectators", "actors", "self"].contains(to) { return true }
        for p in ["num:", "id:", "grp:"] where to.hasPrefix(p) {
            let v = to.dropFirst(p.count)
            return (1...40).contains(v.count) && v.allSatisfy { ($0.isASCII && ($0.isLetter || $0.isNumber)) || " _.-".contains($0) }
        }
        return false
    }

    /// Lines: `zone 3 once > self | text | Welcome, {name}`; `pollwin 0 > all | color | #00ff00;3`; `meter noise 70 > all | blackout | 1;`.
    public static func parse(_ text: String) -> [Rule] {
        var out = [Rule]()
        for (n, raw) in text.components(separatedBy: "\n").enumerated() {
            let line = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            if line.isEmpty || line.hasPrefix("#") { continue }
            guard let gt = line.firstIndex(of: ">") else { continue }
            var left = line[..<gt].split(whereSeparator: { $0.isWhitespace }).map(String.init)
            let right = line[line.index(after: gt)...].split(separator: "|", maxSplits: 2, omittingEmptySubsequences: false).map { $0.trimmingCharacters(in: .whitespaces) }
            if right.count < 2 { continue }
            var once = false
            if let ix = left.firstIndex(of: "once") { once = true; left.remove(at: ix) }
            guard let on = left.first else { continue }
            var arg = 0
            var mk = ""
            switch on {
            case "solved": break
            case "zone", "pollwin":
                guard left.count > 1, let a = Int(left[1]) else { continue }
                arg = a
            case "meter":
                guard left.count > 2, left[1] == "noise" || left[1] == "motion", let a = Int(left[2]) else { continue }
                mk = left[1]; arg = min(max(a, 1), 100)
            default: continue
            }
            let to = right[0], kind = right[1]
            if !ALLOWED.contains(kind) || !validTarget(to) { continue }
            out.append(Rule(id: "r\(n + 1)", on: on, arg: arg, meterKind: mk, to: to, kind: kind, text: String((right.count > 2 ? right[2] : "").prefix(200)), once: once))
            if out.count >= 50 { break }
        }
        return out
    }

    public static func toJson(_ rs: [Rule]) -> [Any] {
        rs.map { jo(["id": $0.id, "on": $0.on, "arg": $0.arg, "mk": $0.meterKind, "to": $0.to, "kind": $0.kind, "text": $0.text, "once": $0.once]) }
    }

    public static func fromJson(_ a: [Any]?) -> [Rule] {
        var out = [Rule]()
        guard let a = a else { return out }
        for (i, e) in a.prefix(50).enumerated() {
            guard let o = e as? JSONObject else { continue }
            let kind = o.optString("kind"), on = o.optString("on"), to = o.optString("to")
            if !ALLOWED.contains(kind) || !["zone", "pollwin", "meter", "solved"].contains(on) || !validTarget(to) { continue }
            var id = String(o.optString("id").filter { $0.isLetter || $0.isNumber }.prefix(8))
            if id.isEmpty { id = "r\(i)" }
            let text = String(String.UnicodeScalarView(o.optString("text").unicodeScalars.filter { $0.value >= 32 }).prefix(200))
            out.append(Rule(id: id, on: on, arg: o.optInt("arg"), meterKind: o.optString("mk"), to: to, kind: kind, text: text, once: o.optBool("once")))
        }
        return out
    }
}

// ====================================================================== Art-Net / DMX, MIDI, Morse, report

public enum ArtNet {
    public static let PORT = 6454

    public static func packet(universe: Int, seq: Int, data: [UInt8]) -> Data {
        let len = (data.count + 1) & ~1
        var b = [UInt8](repeating: 0, count: 18 + len)
        let id = Array("Art-Net".utf8)
        for i in 0..<id.count { b[i] = id[i] }
        b[8] = 0x00; b[9] = 0x50
        b[10] = 0; b[11] = 14
        b[12] = UInt8(seq & 0xFF); b[13] = 0
        b[14] = UInt8(universe & 0xFF); b[15] = UInt8((universe >> 8) & 0x7F)
        b[16] = UInt8((len >> 8) & 0xFF); b[17] = UInt8(len & 0xFF)
        for i in 0..<data.count { b[18 + i] = data[i] }
        return Data(b)
    }
}

/// "0;1=255,2-4=128;2000" = universe 0, channel 1 at 255, channels 2-4 at 128, 2 s fade.
public struct DmxCmd {
    public let universe: Int
    public let target: [Int: Int]
    public let fadeMs: Int64

    public static func parse(_ text: String) -> DmxCmd? {
        let p = text.components(separatedBy: ";").map { $0.trimmingCharacters(in: .whitespaces) }
        guard let u = Int(p[0]), (0...32767).contains(u), p.count > 1 else { return nil }
        var t = [Int: Int]()
        for item in p[1].components(separatedBy: ",") {
            let kv = item.components(separatedBy: "=")
            guard kv.count == 2, let v0 = Int(kv[1].trimmingCharacters(in: .whitespaces)) else { return nil }
            let v = min(max(v0, 0), 255)
            let r = kv[0].trimmingCharacters(in: .whitespaces).components(separatedBy: "-")
            guard r.count <= 2, let a = Int(r[0]) else { return nil }
            var b = a
            if r.count == 2 { guard let bb = Int(r[1]) else { return nil }; b = bb }
            if a < 1 || b > 512 || b < a { return nil }
            for c in a...b { t[c] = v }
        }
        if t.isEmpty { return nil }
        let fade = p.count > 2 ? (Int64(p[2]) ?? 0) : 0
        return DmxCmd(universe: u, target: t, fadeMs: min(max(fade, 0), 600_000))
    }
}

/// 512 channels with linear fades.
public final class DmxFader {
    private var from = [Double](repeating: 0, count: 512)
    private var to = [Double](repeating: 0, count: 512)
    private var t0: Int64 = 0
    private var dur: Int64 = 0
    private let lock = NSLock()
    public init() {}

    private func frameD(_ now: Int64) -> [Double] {
        let k = dur <= 0 ? 1.0 : min(max(Double(now - t0) / Double(dur), 0), 1)
        return (0..<512).map { from[$0] + (to[$0] - from[$0]) * k }
    }

    public func go(_ c: DmxCmd, now: Int64) {
        lock.lock(); defer { lock.unlock() }
        let f = frameD(now)
        from = f
        to = f
        for (ch, v) in c.target { to[ch - 1] = Double(v) }
        t0 = now
        dur = c.fadeMs
    }

    public func frame(_ now: Int64) -> [UInt8] {
        lock.lock(); defer { lock.unlock() }
        return frameD(now).map { UInt8(min(max(Int($0.rounded()), 0), 255)) }
    }

    public func fading(_ now: Int64) -> Bool { lock.lock(); defer { lock.unlock() }; return dur > 0 && now - t0 < dur }
}

/// MIDI from text: `note 60 127 [channel]`, `off 60 [ch]`, `cc 7 100 [ch]`, `pc 5 [ch]`, `mmc play|stop|record`.
public enum Midi {
    public static func encode(_ text: String) -> [UInt8]? {
        let p = text.trimmingCharacters(in: .whitespacesAndNewlines).lowercased().split(whereSeparator: { $0.isWhitespace }).map(String.init)
        guard let head = p.first else { return nil }
        func n(_ i: Int, _ lo: Int, _ hi: Int) -> Int? { i < p.count ? Int(p[i]).flatMap { (lo...hi).contains($0) ? $0 : nil } : nil }
        func ch(_ i: Int) -> Int { ((i < p.count ? Int(p[i]).flatMap { (1...16).contains($0) ? $0 : nil } : nil) ?? 1) - 1 }
        switch head {
        case "note": guard let a = n(1, 0, 127), let v = n(2, 0, 127) else { return nil }; return [UInt8(0x90 | ch(3)), UInt8(a), UInt8(v)]
        case "off": guard let a = n(1, 0, 127) else { return nil }; return [UInt8(0x80 | ch(2)), UInt8(a), 0]
        case "cc": guard let a = n(1, 0, 127), let v = n(2, 0, 127) else { return nil }; return [UInt8(0xB0 | ch(3)), UInt8(a), UInt8(v)]
        case "pc": guard let a = n(1, 0, 127) else { return nil }; return [UInt8(0xC0 | ch(2)), UInt8(a)]
        case "mmc":
            let c: UInt8
            switch p.count > 1 ? p[1] : "" { case "stop": c = 1; case "play": c = 2; case "record": c = 6; default: return nil }
            return [0xF0, 0x7F, 0x7F, 0x06, c, 0xF7]
        default: return nil
        }
    }
}

/// Morse as a vibration pattern (ms: pause, buzz, pause, ...); starts with pause 0.
public enum Morse {
    private static let T: [Character: String] = [
        "A": ".-", "B": "-...", "C": "-.-.", "D": "-..", "E": ".", "F": "..-.", "G": "--.", "H": "....", "I": "..",
        "J": ".---", "K": "-.-", "L": ".-..", "M": "--", "N": "-.", "O": "---", "P": ".--.", "Q": "--.-", "R": ".-.",
        "S": "...", "T": "-", "U": "..-", "V": "...-", "W": ".--", "X": "-..-", "Y": "-.--", "Z": "--..",
        "0": "-----", "1": ".----", "2": "..---", "3": "...--", "4": "....-", "5": ".....", "6": "-....", "7": "--...", "8": "---..", "9": "----.",
    ]

    public static func pattern(_ text: String, unit: Int64 = 120) -> [Int64] {
        var out = [Int64]()
        let words = String(text.uppercased().prefix(60)).split(whereSeparator: { $0.isWhitespace })
        for w in words {
            var inWord = false
            for ch in w {
                guard let code = T[ch] else { continue }
                for (i, s) in code.enumerated() {
                    if out.reduce(0, +) > 45_000 { return out }
                    let pause: Int64 = i > 0 ? unit : (inWord ? unit * 3 : (!out.isEmpty ? unit * 7 : 0))
                    out.append(pause)
                    out.append(s == "." ? unit : unit * 3)
                }
                inWord = true
            }
        }
        if out.isEmpty { out.append(0) }
        return out
    }
}

public struct RPeer {
    public let name: String, role: String, number: String
    public let online: Bool
    public let bat: Int, zone: Int, calls: Int, msgs: Int, zones: Int
    public init(name: String, role: String, number: String, online: Bool, bat: Int, zone: Int, calls: Int, msgs: Int, zones: Int) {
        self.name = name; self.role = role; self.number = number; self.online = online; self.bat = bat; self.zone = zone; self.calls = calls; self.msgs = msgs; self.zones = zones
    }
}

public enum Report {
    private static func clean(_ s: String) -> String { s.replacingOccurrences(of: "|", with: "/").replacingOccurrences(of: "\n", with: " ") }

    public static func markdown(hub: String, startMs: Int64, nowMs: Int64, peers: [RPeer], events: [String], counters: [(String, Int)]) -> String {
        var sb = ""
        let mins = (nowMs - startMs) / 60000
        sb += "# " + clean(hub.isEmpty ? "CUPLAJ" : hub) + " — show report\n\n"
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm"
        sb += "Generated: " + f.string(from: Date(timeIntervalSince1970: Double(nowMs) / 1000)) + " · hub up \(mins) min\n\n"
        sb += "## Totals\n\n"
        for (k, v) in counters { sb += "* \(k): \(v)\n" }
        let on = peers.filter { $0.online }.count
        sb += "* phones known: \(peers.count) (online now: \(on))\n"
        let low = peers.filter { $0.bat >= 0 && $0.bat < 20 }
        if !low.isEmpty { sb += "* low battery (<20 %): " + low.map { $0.name }.joined(separator: ", ") + "\n" }
        sb += "\n## Phones\n\n| Name | Role | Number | Calls | Messages | Zone entries | Battery |\n|---|---|---|---|---|---|---|\n"
        for p in peers.prefix(150) {
            sb += "| \(clean(p.name)) | \(p.role) | \(p.number) | \(p.calls) | \(p.msgs) | \(p.zones) | " + (p.bat >= 0 ? "\(p.bat) %" : "–") + " |\n"
        }
        if peers.count > 150 { sb += "\n(\(peers.count - 150) more not listed)\n" }
        sb += "\n## Timeline\n\n"
        for e in events.suffix(400) { sb += "* " + clean(e) + "\n" }
        return sb
    }
}

public extension Rules {
    /// Whether `t` is an acceptable cue target (all, staff, spectators, actors, or num:/id:/grp:).
    static func validTargetPublic(_ t: String) -> Bool { t != "self" && validTarget(t) }
}
