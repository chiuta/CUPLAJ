import XCTest
@testable import CuplajCore

final class AirTests: XCTestCase {
    let key = Sec.deriveKey("abcd-efgh-jkmn-pqrs")

    func testPackUnpackAndTamper() throws {
        let p = AirPacket(type: Air.T_CUE, seq: 7, data: Air.cue(3, "salut"))
        let raw = try XCTUnwrap(Air.pack(p, key: key, macLen: 4))
        XCTAssertEqual(Air.unpack(raw, key: key, macLen: 4), p)
        XCTAssertNil(Air.unpack(raw, key: Sec.deriveKey("zzzz-zzzz-zzzz-zzzz"), macLen: 4))
        var bad = [UInt8](raw); bad[4] ^= 1
        XCTAssertNil(Air.unpack(Data(bad), key: key, macLen: 4))
    }

    func testReplayGuard() {
        let g = AirGuard(); let p = AirPacket(type: 1, seq: 9, data: Data())
        XCTAssertTrue(g.accept(p, now: 1000)); XCTAssertFalse(g.accept(p, now: 2000)); XCTAssertTrue(g.accept(p, now: 700_000))
    }

    func testLumenRoundTripWithClockSkew() throws {
        let p = AirPacket(type: 1, seq: 5, data: Air.cue(1, "ok"))
        let raw = try XCTUnwrap(Air.pack(p, key: key, macLen: 2))
        let slots = Lumen.encode(raw)
        // camera at ~30 fps, a 1.5 % slow clock, noise and an offset
        var s = [LSample](); var t = 0.0
        let total = Double(slots.count) * Lumen.SLOT_MS * 1.015 + 1500
        var seed: UInt64 = 7
        while t < total {
            let idx = Int((t - 400) / (Lumen.SLOT_MS * 1.015))
            let on = idx >= 0 && idx < slots.count && slots[idx]
            seed = seed &* 6364136223846793005 &+ 1442695040888963407
            let noise = Double(seed >> 60) - 7.5
            s.append(LSample(t: t, v: (on ? 180 : 40) + noise))
            t += 33.3
        }
        let got = Lumen.decode(s, key: key, macLen: 2, maxLen: 12)
        XCTAssertEqual(got, p)
    }

    func testSonusRoundTrip() throws {
        for ultra in [true, false] {
            let p = AirPacket(type: 3, seq: 2, data: Data([12]))
            let raw = try XCTUnwrap(Air.pack(p, key: key, macLen: 4))
            let pcm = Sonus.encode(raw, fs: 48000, ultra: ultra)
            let pad = [Int16](repeating: 0, count: 48000 * 2)
            let got = Sonus.decode(pad + pcm + pad, fs: 48000, ultra: ultra, key: key)
            XCTAssertEqual(got, p, "ultra=\(ultra)")
        }
    }

    func testTactusRoundTrip() throws {
        let p = AirPacket(type: 1, seq: 4, data: Data([2, 65]))
        let raw = try XCTUnwrap(Air.pack(p, key: key, macLen: 2))
        let d = Tactus.encode(raw)
        var s = [ASample](); var t = 0.0
        let total = d.reduce(0, +) + 1500
        // 100 Hz accelerometer; vibration = 25 Hz oscillation of 1 m/s^2 on top of gravity
        var phases = [(Double, Double)](); var at = 400.0
        for (i, x) in d.enumerated() { if i % 2 == 0 { phases.append((at, at + Double(x))) }; at += Double(x) }
        while t < Double(total) {
            let vib = phases.contains { t >= $0.0 && t < $0.1 }
            let z = 9.81 + (vib ? 1.2 * sin(2 * Double.pi * 25 * t / 1000) : 0)
            s.append(ASample(t: t, x: 0, y: 0, z: z))
            t += 10
        }
        XCTAssertEqual(Tactus.decode(s, key: key, macLen: 2, maxLen: 6), p)
    }
}
