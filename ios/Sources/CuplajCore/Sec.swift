import Foundation
import CryptoKit
import CommonCrypto

/// AES-128-GCM with a network key derived from the hub's phrase; byte-compatible with the Android app.
public enum Sec {
    /// PBKDF2-HMAC-SHA1, salt "CUPLAJ-v1", 100 000 iterations, 128 bits. Empty phrase = no encryption.
    public static func deriveKey(_ secret: String) -> Data? {
        let s = secret.trimmingCharacters(in: .whitespacesAndNewlines)
        if s.isEmpty { return nil }
        let pw = Array(s.utf8)
        let salt = Array("CUPLAJ-v1".utf8)
        var out = [UInt8](repeating: 0, count: 16)
        let rc = pw.withUnsafeBufferPointer { pp -> Int32 in
            pp.baseAddress!.withMemoryRebound(to: Int8.self, capacity: pw.count) { pc in
                CCKeyDerivationPBKDF(CCPBKDFAlgorithm(kCCPBKDF2), pc, pw.count, salt, salt.count,
                                     CCPseudoRandomAlgorithm(kCCPRFHmacAlgSHA1), 100_000, &out, 16)
            }
        }
        return rc == 0 ? Data(out) : nil
    }

    public static func newPassphrase() -> String {
        let abc = Array("abcdefghjkmnpqrstuvwxyz23456789")
        return (0..<4).map { _ in String((0..<4).map { _ in abc[Int.random(in: 0..<abc.count)] }) }.joined(separator: "-")
    }

    /// Fresh session key per connection: HMAC-SHA256(master, "CUPLAJ-sess" + hub nonce + client nonce), first 16 bytes.
    public static func session(master: Data, serverNonce: Data, clientNonce: Data) -> Data {
        var m = HMAC<SHA256>(key: SymmetricKey(data: master))
        m.update(data: Data("CUPLAJ-sess".utf8))
        m.update(data: serverNonce)
        m.update(data: clientNonce)
        return Data(m.finalize()).prefix(16)
    }

    public static func pin(_ digits: Int) -> String { (0..<digits).map { _ in String(Int.random(in: 0..<10)) }.joined() }

    public static func randomBytes(_ n: Int) -> Data { Data((0..<n).map { _ in UInt8.random(in: 0...255) }) }

    /// Returns ciphertext || 16-byte tag, as javax.crypto does.
    public static func encrypt(key: Data, nonce: Data, plain: Data) -> Data? {
        guard let n = try? AES.GCM.Nonce(data: nonce),
              let box = try? AES.GCM.seal(plain, using: SymmetricKey(data: key), nonce: n) else { return nil }
        return box.ciphertext + box.tag
    }

    public static func decrypt(key: Data, nonce: Data, cipher: Data) -> Data? {
        guard cipher.count >= 16, let n = try? AES.GCM.Nonce(data: nonce) else { return nil }
        let ct = cipher.prefix(cipher.count - 16)
        let tag = cipher.suffix(16)
        guard let box = try? AES.GCM.SealedBox(nonce: n, ciphertext: ct, tag: tag) else { return nil }
        return try? AES.GCM.open(box, using: SymmetricKey(data: key))
    }

    public static func sha256Hex(_ d: Data) -> String { hex(Data(SHA256.hash(data: d))) }

    /// Constant-time comparison of two strings.
    public static func same(_ a: String, _ b: String) -> Bool {
        let x = Array(a.utf8), y = Array(b.utf8)
        if x.count != y.count { return false }
        var r: UInt8 = 0
        for i in 0..<x.count { r |= x[i] ^ y[i] }
        return r == 0
    }
}

/// G.711 mu-law: 16-bit/8 kHz (320 bytes) <-> 8-bit (160 bytes).
public enum G711 {
    private static func encSample(_ s0: Int) -> UInt8 {
        var s = s0
        let sign = s < 0 ? 0x80 : 0
        if s < 0 { s = -s }
        if s > 32635 { s = 32635 }
        s += 0x84
        var e = 7
        var m = 0x4000
        while e > 0 && (s & m) == 0 { e -= 1; m >>= 1 }
        let mant = (s >> (e + 3)) & 0x0F
        return UInt8(truncatingIfNeeded: ~(sign | (e << 4) | mant))
    }

    private static let table: [Int] = (0..<256).map { i in
        let u = ~i & 0xFF
        let sign = u & 0x80
        let e = (u >> 4) & 7
        let m = u & 0x0F
        var v = ((m << 3) + 0x84) << e
        v -= 0x84
        return sign != 0 ? -v : v
    }

    /// PCM16 little-endian -> mu-law.
    public static func encode(_ pcm: Data) -> Data {
        let b = [UInt8](pcm)
        let n = b.count / 2
        var out = [UInt8](repeating: 0, count: n)
        for i in 0..<n {
            let s = Int(Int16(bitPattern: UInt16(b[2 * i]) | (UInt16(b[2 * i + 1]) << 8)))
            out[i] = encSample(s)
        }
        return Data(out)
    }

    public static func decode(_ u: Data) -> Data {
        var out = [UInt8]()
        out.reserveCapacity(u.count * 2)
        for x in u {
            let v = table[Int(x)]
            let w = UInt16(bitPattern: Int16(truncatingIfNeeded: v))
            out.append(UInt8(w & 0xFF))
            out.append(UInt8(w >> 8))
        }
        return Data(out)
    }
}
