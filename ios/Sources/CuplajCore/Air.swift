import Foundation
import CryptoKit

/*
 CUPLAJ-AIR: three own protocols for a link without a network, using other phone functions (same wire format as Android).
   LUMEN  - light: torch (or screen) -> camera. Manchester code in 100 ms slots, Barker-13 sync, decoder searching phase and clock skew.
   SONUS  - sound: speaker -> microphone. 8-FSK near-ultrasonic (17-20 kHz) or audible, 50 ms symbol (3 bits), zig-zag preamble, Goertzel.
   TACTUS - vibration: vibration motor -> accelerometer of another phone on the same table/stage. Burst length code (short 0, long 1).
 All three carry the same small packet, authenticated with HMAC (network key) and protected against replay.
*/

public struct AirPacket: Equatable {
    public var type: Int, seq: Int, data: Data
    public init(type: Int, seq: Int, data: Data) { self.type = type; self.seq = seq; self.data = data }
}

public enum Air {
    public static let T_CUE = 1, T_INVITE = 2, T_ZONE = 3
    private static let openKey = Data("CUPLAJ-AIR-OPEN".utf8)

    static func mac(_ key: Data?, _ msg: Data, _ n: Int) -> Data {
        let k = SymmetricKey(data: key ?? openKey)
        return Data(HMAC<SHA256>.authenticationCode(for: msg, using: k)).prefix(n)
    }

    /// Bytes: [ver<<4|type][seq][len][data...][mac...]. Without a network key the MAC is only a checksum.
    public static func pack(_ p: AirPacket, key: Data?, macLen: Int = 4) -> Data? {
        guard p.data.count <= 90, (1...15).contains(p.type) else { return nil }
        let body = Data([UInt8((1 << 4) | p.type), UInt8(p.seq & 0xFF), UInt8(p.data.count)]) + p.data
        return body + mac(key, body, macLen)
    }

    public static func unpack(_ raw: Data, key: Data?, macLen: Int = 4) -> AirPacket? {
        let b = [UInt8](raw)
        if b.count < 3 + macLen { return nil }
        if (b[0] >> 4) & 0xF != 1 { return nil }
        let len = Int(b[2])
        if len > 90 || b.count != 3 + len + macLen { return nil }
        let body = Data(b[0..<(3 + len)])
        let tag = Data(b[(3 + len)...])
        if !Sec.same(tag, mac(key, body, macLen)) { return nil }
        return AirPacket(type: Int(b[0] & 0xF), seq: Int(b[1]), data: Data(b[3..<(3 + len)]))
    }

    public static func bits(_ d: Data) -> [Int] {
        let b = [UInt8](d)
        return (0..<(b.count * 8)).map { Int((b[$0 / 8] >> UInt8(7 - $0 % 8)) & 1) }
    }

    public static func bytes(_ bits: [Int], from: Int, n: Int) -> Data {
        var o = [UInt8](repeating: 0, count: n)
        for i in 0..<(n * 8) where bits[from + i] == 1 { o[i / 8] |= UInt8(0x80 >> (i % 8)) }
        return Data(o)
    }

    /// Cue: [index in CUE_KINDS][UTF-8 text].
    public static func cue(_ kind: Int, _ text: String) -> Data { Data([UInt8(kind)]) + Data(text.utf8.prefix(60)) }
}

/// Anti-replay: refuses a (type, seq) seen in the last 10 minutes.
public final class AirGuard {
    private var seen = [Int: Int64]()
    private let lock = NSLock()
    public init() {}
    public func accept(_ p: AirPacket, now: Int64 = nowMs()) -> Bool {
        lock.lock(); defer { lock.unlock() }
        seen = seen.filter { now - $0.value <= 600_000 }
        let k = p.type * 256 + p.seq
        if seen[k] != nil { return false }
        seen[k] = now
        return true
    }
}

// ====================================================================== LUMEN

public struct LSample { public var t: Double, v: Double; public init(t: Double, v: Double) { self.t = t; self.v = v } }

public enum Lumen {
    public static let SLOT_MS = 100.0
    private static let barker = [1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1, 0, 1]

    /// Slot levels (true = on): 2 off, Barker-13, Manchester bits (1 = on-off), 2 off.
    public static func encode(_ packet: Data) -> [Bool] {
        var out = [false, false]
        out += barker.map { $0 == 1 }
        for bit in Air.bits(packet) { out.append(bit == 1); out.append(bit == 0) }
        out += [false, false]
        return out
    }

    private static func slotMean(_ s: [LSample], _ a: Double, _ b: Double) -> Double {
        var l = 0, h = s.count
        while l < h { let m = (l + h) / 2; if s[m].t < a { l = m + 1 } else { h = m } }
        var sum = 0.0, n = 0, i = l
        while i < s.count && s[i].t < b { sum += s[i].v; n += 1; i += 1 }
        return n == 0 ? Double.nan : sum / Double(n)
    }

    public static func decode(_ s: [LSample], key: Data?, macLen: Int = 2, maxLen: Int = 12, slotMs: Double = SLOT_MS) -> AirPacket? {
        if s.count < 40 { return nil }
        let t0 = s.first!.t, t1 = s.last!.t
        for sk in (-15...15).sorted(by: { abs($0) < abs($1) }) {
            let T = slotMs * (1.0 + Double(sk) * 0.004)
            for ph in 0..<10 {
                let start = t0 + Double(ph) * T / 10
                let nSlots = Int((t1 - start) / T)
                if nSlots < barker.count + 2 * (3 + macLen) * 8 { continue }
                let m = (0..<nSlots).map { k -> Double in
                    let a = start + Double(k) * T + 0.2 * T
                    return slotMean(s, a, a + 0.6 * T)
                }
                if let r = scan(m, key, macLen, maxLen) { return r }
            }
        }
        return nil
    }

    private static func scan(_ m: [Double], _ key: Data?, _ macLen: Int, _ maxLen: Int) -> AirPacket? {
        let nb = barker.count
        if m.count < nb + 16 { return nil }
        for k in 0...(m.count - nb - 16) {
            var on = 0.0, off = 0.0, non = 0, noff = 0, bad = false
            for i in 0..<nb {
                let v = m[k + i]
                if v.isNaN { bad = true; break }
                if barker[i] == 1 { on += v; non += 1 } else { off += v; noff += 1 }
            }
            if bad { continue }
            on /= Double(non); off /= Double(noff)
            if on - off < 6.0 { continue }
            let thr = (on + off) / 2
            var mism = 0
            for i in 0..<nb where (m[k + i] > thr) != (barker[i] == 1) { mism += 1 }
            if mism > 1 { continue }
            var p = k + nb
            func readByte() -> Int? {
                var v = 0
                for _ in 0..<8 {
                    if p + 1 >= m.count { return nil }
                    let a = m[p], b = m[p + 1]
                    if a.isNaN || b.isNaN { return nil }
                    if abs(a - b) < (on - off) * 0.3 { return nil }
                    v = (v << 1) | (a > b ? 1 : 0)
                    p += 2
                }
                return v
            }
            guard let h0 = readByte(), let h1 = readByte(), let h2 = readByte(), h2 <= maxLen else { continue }
            var buf = [UInt8(h0), UInt8(h1), UInt8(h2)]
            var ok = true
            for _ in 0..<(h2 + macLen) { if let v = readByte() { buf.append(UInt8(v)) } else { ok = false; break } }
            if !ok { continue }
            if let pk = Air.unpack(Data(buf), key: key, macLen: macLen) { return pk }
        }
        return nil
    }
}

// ====================================================================== SONUS

public enum Sonus {
    public static let SYM_MS = 50.0
    static let sync = [0, 7, 1, 6, 2, 5, 3, 4, 0, 7, 1, 6, 2, 5, 3, 4]

    public static func freq(_ i: Int, ultra: Bool) -> Double { ultra ? 17000.0 + 400.0 * Double(i) : 1800.0 + 300.0 * Double(i) }

    public static func symbols(_ packet: Data) -> [Int] {
        let bits = Air.bits(packet)
        let n = (bits.count + 2) / 3
        let d = (0..<n).map { s -> Int in
            var v = 0
            for j in 0..<3 { let ix = s * 3 + j; v = (v << 1) | (ix < bits.count ? bits[ix] : 0) }
            return v
        }
        return sync + d
    }

    /// PCM 16-bit mono, each symbol with a 5 ms cosine ramp so the speaker does not click.
    public static func encode(_ packet: Data, fs: Int, ultra: Bool, amp: Double = 0.5) -> [Int16] {
        let sy = symbols(packet)
        let L = Int(Double(fs) * SYM_MS / 1000), ramp = Int(Double(fs) * 0.005), lead = L
        var out = [Int16](repeating: 0, count: lead + sy.count * L + L)
        for (si, s) in sy.enumerated() {
            let f = freq(s, ultra: ultra)
            for n in 0..<L {
                var a = amp
                if n < ramp { a *= 0.5 * (1 - cos(Double.pi * Double(n) / Double(ramp))) }
                if n >= L - ramp { a *= 0.5 * (1 - cos(Double.pi * Double(L - 1 - n) / Double(ramp))) }
                out[lead + si * L + n] = Int16(a * 32767 * sin(2 * Double.pi * f * Double(n) / Double(fs)))
            }
        }
        return out
    }

    private static func goertzel(_ x: [Float], _ from: Int, _ n: Int, _ f: Double, _ fs: Int) -> Double {
        let w = 2 * Double.pi * f / Double(fs), c = 2 * cos(w)
        var s1 = 0.0, s2 = 0.0
        for i in 0..<n { let s0 = Double(x[from + i]) + c * s1 - s2; s2 = s1; s1 = s0 }
        return s1 * s1 + s2 * s2 - c * s1 * s2
    }

    public static func decode(_ pcm: [Int16], fs: Int, ultra: Bool, key: Data?, macLen: Int = 4, maxLen: Int = 90) -> AirPacket? {
        let L = Int(Double(fs) * SYM_MS / 1000), hop = L / 8
        if pcm.count < L * (sync.count + 8) { return nil }
        let x = pcm.map { Float($0) / 32768 }
        let steps = (pcm.count - L) / hop
        let freqs = (0..<8).map { freq($0, ultra: ultra) }
        let e = (0..<steps).map { st in freqs.map { goertzel(x, st * hop, L, $0, fs) } }
        let need = sync.count
        var cands = [(Int, Double)]()
        if steps - need * 8 > 0 {
            for st in 0..<(steps - need * 8) {
                var sc = 0.0, ok = true
                for j in 0..<need {
                    let row = e[st + j * 8]
                    let tot = row.reduce(0, +) + 1e-12
                    let r = row[sync[j]] / tot
                    if r < 0.35 { ok = false; break }
                    sc += r
                }
                if ok { cands.append((st, sc / Double(need))) }
            }
        }
        for (st, _) in cands.sorted(by: { $0.1 > $1.1 }).prefix(12) {
            if let r = readSymbols(e, st + need * 8, macLen, maxLen, key) { return r }
        }
        return nil
    }

    private static func readSymbols(_ e: [[Double]], _ from: Int, _ macLen: Int, _ maxLen: Int, _ key: Data?) -> AirPacket? {
        func sym(_ j: Int) -> Int? {
            let st = from + j * 8
            if st >= e.count { return nil }
            let row = e[st]
            var b = 0
            for i in 1..<8 where row[i] > row[b] { b = i }
            return b
        }
        var bits = [Int](), j = 0
        func fill(_ n: Int) -> Bool {
            while bits.count < n * 8 {
                guard let s = sym(j) else { return false }
                j += 1
                bits += [(s >> 2) & 1, (s >> 1) & 1, s & 1]
            }
            return true
        }
        if !fill(3) { return nil }
        let head = [UInt8](Air.bytes(bits, from: 0, n: 3))
        let len = Int(head[2])
        if len > maxLen { return nil }
        let total = 3 + len + macLen
        if !fill(total) { return nil }
        return Air.unpack(Air.bytes(bits, from: 0, n: total), key: key, macLen: macLen)
    }
}

// ====================================================================== TACTUS

public struct ASample { public var t: Double, x: Double, y: Double, z: Double; public init(t: Double, x: Double, y: Double, z: Double) { self.t = t; self.x = x; self.y = y; self.z = z } }

public enum Tactus {
    public static let SHORT_MS = 100, LONG_MS = 240, GAP_MS = 100

    /// Durations (ms) alternating vibrate, pause, ... (starts with a vibration). Preamble: long, long, long, short.
    public static func encode(_ packet: Data) -> [Int] {
        var out = [Int]()
        func burst(_ long: Bool, _ last: Bool) { out.append(long ? LONG_MS : SHORT_MS); if !last { out.append(GAP_MS) } }
        burst(true, false); burst(true, false); burst(true, false); burst(false, false)
        let bits = Air.bits(packet)
        for (i, b) in bits.enumerated() { burst(b == 1, i == bits.count - 1) }
        return out
    }

    static func bursts(_ s: [ASample]) -> [(Double, Double)] {
        if s.count < 30 { return [] }
        let mag = s.map { ($0.x * $0.x + $0.y * $0.y + $0.z * $0.z).squareRoot() }
        var hp = [Double](repeating: 0, count: s.count)
        var ema = mag[0]
        for i in mag.indices { ema += (mag[i] - ema) * 0.05; hp[i] = abs(mag[i] - ema) }
        let dt = (s.last!.t - s.first!.t) / Double(s.count - 1)
        let w = max(2, Int(40.0 / dt))
        var env = [Double](repeating: 0, count: s.count)
        for i in env.indices { var mx = 0.0; for j in max(0, i - w + 1)...i where hp[j] > mx { mx = hp[j] }; env[i] = mx }
        let sorted = env.sorted()
        let floorV = sorted[Int(Double(sorted.count) * 0.4)]
        let peak = sorted[min(sorted.count - 1, Int(Double(sorted.count) * 0.98))]
        if peak - floorV < 0.15 { return [] }
        let on = floorV + 0.35 * (peak - floorV), off = floorV + 0.2 * (peak - floorV)
        var out = [(Double, Double)]()
        var inB = false, t0 = 0.0
        for i in env.indices {
            if !inB && env[i] > on { inB = true; t0 = s[i].t }
            else if inB && env[i] < off { inB = false; out.append((t0, s[i].t - t0)) }
        }
        return out
    }

    public static func decode(_ s: [ASample], key: Data?, macLen: Int = 2, maxLen: Int = 6) -> AirPacket? {
        let b = bursts(s)
        if b.count < 4 + 3 * 8 + macLen * 8 { return nil }
        for k in 0...(b.count - 4) {
            let l1 = b[k].1, l2 = b[k + 1].1, l3 = b[k + 2].1, sh = b[k + 3].1
            let L = (l1 + l2 + l3) / 3
            if L < sh * 1.5 || abs(l1 - l2) > L * 0.35 || abs(l2 - l3) > L * 0.35 { continue }
            let gaps = (0..<3).map { b[k + $0 + 1].0 - (b[k + $0].0 + b[k + $0].1) }
            if gaps.contains(where: { $0 < Double(GAP_MS) * 0.4 || $0 > Double(GAP_MS) * 2.2 }) { continue }
            let bound = (L + sh) / 2
            var idx = k + 4
            func readByte() -> Int? {
                if idx + 8 > b.count { return nil }
                var v = 0
                for _ in 0..<8 { v = (v << 1) | (b[idx].1 > bound ? 1 : 0); idx += 1 }
                return v
            }
            guard let h0 = readByte(), let h1 = readByte(), let h2 = readByte(), h2 <= maxLen else { continue }
            var buf = [UInt8(h0), UInt8(h1), UInt8(h2)]
            var ok = true
            for _ in 0..<(h2 + macLen) { if let v = readByte() { buf.append(UInt8(v)) } else { ok = false; break } }
            if !ok { continue }
            if let pk = Air.unpack(Data(buf), key: key, macLen: macLen) { return pk }
        }
        return nil
    }
}
