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

    /// Same model as the Kotlin test: a ~170 Hz motor sampled by a 100 Hz accelerometer (aliased), gravity and noise.
    func tactusSim(_ packet: Data, hz: Double, noise: Double, amp: Double) -> [ASample] {
        let d = Tactus.encode(packet)
        var segs = [(Double, Double, Bool)](); var t = 800.0
        for (i, ms) in d.enumerated() { segs.append((t, t + Double(ms), i % 2 == 0)); t += Double(ms) }
        let total = t + 800
        var seed: UInt64 = 99
        func rnd() -> Double { seed = seed &* 6364136223846793005 &+ 1442695040888963407; return Double(seed >> 11) / Double(1 << 53) - 0.5 }
        var out = [ASample](); var tt = 0.0
        while tt < total {
            var a = 0.0
            for sg in segs where sg.2 && tt >= sg.0 + 25 && tt < sg.1 + 15 { a = amp * sin(2 * Double.pi * 170.0 * tt / 1000) }
            out.append(ASample(t: tt, x: rnd() * noise, y: rnd() * noise, z: 9.81 + a + rnd() * noise))
            tt += 1000.0 / hz + rnd() * 0.8
        }
        return out
    }

    func testTactusRoundTrip() throws {
        let p = AirPacket(type: 1, seq: 4, data: Data([2]))
        let raw = try XCTUnwrap(Air.pack(p, key: key, macLen: 2))
        XCTAssertEqual(Tactus.decode(tactusSim(raw, hz: 100, noise: 0.05, amp: 1.5), key: key, macLen: 2, maxLen: 6), p)
        XCTAssertEqual(Tactus.decode(tactusSim(raw, hz: 200, noise: 0.04, amp: 1.5), key: key, macLen: 2, maxLen: 6), p)
        XCTAssertNil(Tactus.decode(tactusSim(raw, hz: 200, noise: 0.04, amp: 1.5), key: Sec.deriveKey("zzzz-zzzz-zzzz-zzzz"), macLen: 2, maxLen: 6))
    }
}
