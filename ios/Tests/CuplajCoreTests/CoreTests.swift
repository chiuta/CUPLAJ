import XCTest
@testable import CuplajCore

final class CoreTests: XCTestCase {
    // Vectors computed independently with Python (hashlib / hmac / cryptography), not with this code.
    func testKeyDerivationMatchesReference() {
        let k = Sec.deriveKey("abcd-efgh-jkmn-pqrs")
        XCTAssertEqual(hex(k ?? Data()), "4d92a6a2ad02748aa479f2ab799b52da")
        XCTAssertNil(Sec.deriveKey("   "))
    }

    func testSessionKeyMatchesReference() {
        let k = Sec.deriveKey("abcd-efgh-jkmn-pqrs")!
        let sn = Data(0..<16), cn = Data(16..<32)
        XCTAssertEqual(hex(Sec.session(master: k, serverNonce: sn, clientNonce: cn)), "e1e7e6d79d3dee337c37bca0a830000e")
    }

    func testAesGcmMatchesReferenceAndRoundTrips() {
        let s = unhex("e1e7e6d79d3dee337c37bca0a830000e")!
        let nonce = Data([1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1])
        let pt = Data("{\"t\":\"ping\"}".utf8)
        let ct = Sec.encrypt(key: s, nonce: nonce, plain: pt)!
        XCTAssertEqual(hex(ct), "8a3d40298e90ee83c503e7707b4325fb24a8a12bf8d0a02e21f82ab9")
        XCTAssertEqual(Sec.decrypt(key: s, nonce: nonce, cipher: ct), pt)
        var bad = ct; bad[0] ^= 1
        XCTAssertNil(Sec.decrypt(key: s, nonce: nonce, cipher: bad))
    }

    func testG711() {
        func pcm(_ v: [Int16]) -> Data { Data(v.flatMap { [UInt8(UInt16(bitPattern: $0) & 0xFF), UInt8(UInt16(bitPattern: $0) >> 8)] }) }
        XCTAssertEqual([UInt8](G711.encode(pcm([0, 1000, -1000, 32767, -32768, 100]))), [0xff, 0xce, 0x4e, 0x80, 0x00, 0xf2])
        let back = [UInt8](G711.decode(G711.encode(pcm([1000, -1000]))))
        let a = Int16(bitPattern: UInt16(back[0]) | UInt16(back[1]) << 8)
        XCTAssertLessThan(abs(Int(a) - 1000), 60)
    }

    func testSha256() { XCTAssertEqual(Sec.sha256Hex(Data("abc".utf8)), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad") }

    func testHostPort() {
        XCTAssertTrue(parseHostPort("10.0.0.5") == ("10.0.0.5", 8470))
        XCTAssertTrue(parseHostPort("10.0.0.5:9000") == ("10.0.0.5", 9000))
        XCTAssertTrue(parseHostPort("[::1]:8471") == ("::1", 8471))
        XCTAssertTrue(privateCand("192.168.1.5:5000"))
        XCTAssertFalse(privateCand("8.8.8.8:5000"))
        XCTAssertEqual(safeNumber("*21*+40 721 #"), "2140721")   // same as the Kotlin test: no USSD/forwarding codes survive as such
        XCTAssertEqual(safeNumber("+40 721-000.111"), "+40721000111")
    }

    func testParseAtAndColor() {
        XCTAssertTrue(parseAt("@3.5;hi") == (3500, "hi"))
        XCTAssertTrue(parseAt("hi") == (0, "hi"))
        XCTAssertTrue(parseAt("@x;hi") == (0, "@x;hi"))
        XCTAssertEqual(colorPlan("#ff0000;2;9")?.hz, 2.0)
        XCTAssertNil(colorPlan("red"))
    }

    func testClockSync() {
        let c = ClockSync()
        XCTAssertFalse(c.ready())
        c.add(t0: 1000, hub: 5050, t1: 1100)
        XCTAssertEqual(c.offset(), 4000)
        XCTAssertNil(c.delayFor(at: 6000_0000, local: 1000))
        XCTAssertEqual(c.delayFor(at: 5600, local: 1000), 600 - 0)
    }

    func testOscRoundTripAndPin() {
        let d = Osc.encode("/cuplaj/1234/cue", [.str("all"), .str("text"), .str("Hello"), .int(7), .float(1.5)])
        let m = Osc.decode(d)
        XCTAssertEqual(m?.addr, "/cuplaj/1234/cue")
        XCTAssertEqual(m?.args.count, 5)
        let ok = Osc.command(Osc.decode(Osc.encode("/cuplaj/1234/cue", [.str("all"), .str("text"), .str("Hi")]))!, pin: "1234")
        XCTAssertEqual(ok?.kind, "cue")
        XCTAssertNil(Osc.command(Osc.decode(Osc.encode("/cuplaj/9999/go", []))!, pin: "1234"))
        XCTAssertEqual(Osc.command(Osc.decode(Osc.encode("/cuplaj/1234/panic", []))!, pin: "1234")?.kind, "panic")
        XCTAssertNil(Osc.decode(Data([1, 2, 3])))
    }

    func testPickLangAndTokens() {
        XCTAssertEqual(pickLang("en=Hello|ro=Salut", "ro"), "Salut")
        XCTAssertEqual(pickLang("en=Hello|ro=Salut", "de"), "Hello")
        XCTAssertEqual(pickLang("a|b", "ro"), "a|b")
        XCTAssertEqual(expandTokens("Hi {name} {number}", name: "Ana", number: "2101"), "Hi Ana 2101")
    }

    func testRules() {
        let r = Rules.parse("zone 3 once > self | text | Welcome {name}\nmeter noise 70 > all | blackout | 1;\nzone 1 > all | dmx | 0;1=1\nbad")
        XCTAssertEqual(r.count, 2)   // dmx is not allowed as a rule action
        XCTAssertTrue(r[0].once)
        XCTAssertEqual(r[1].arg, 70)
        XCTAssertEqual(Rules.fromJson(Rules.toJson(r)).count, 2)
    }

    func testArtNetDmxMidiMorse() {
        let p = [UInt8](ArtNet.packet(universe: 1, seq: 5, data: [255, 128, 7]))
        XCTAssertEqual(p.count, 18 + 4)
        XCTAssertEqual(Array(p[0..<7]), Array("Art-Net".utf8))
        XCTAssertEqual(p[9], 0x50); XCTAssertEqual(p[11], 14); XCTAssertEqual(p[12], 5); XCTAssertEqual(p[14], 1); XCTAssertEqual(p[17], 4)
        let c = DmxCmd.parse("0;1=255,2-4=128;2000")!
        XCTAssertEqual(c.target.count, 4); XCTAssertEqual(c.fadeMs, 2000)
        XCTAssertNil(DmxCmd.parse("0;600=1"))
        let f = DmxFader()
        f.go(c, now: 0)
        XCTAssertEqual(f.frame(0)[0], 0)
        XCTAssertEqual(f.frame(1000)[0], 128)
        XCTAssertEqual(f.frame(2000)[0], 255)
        XCTAssertTrue(f.fading(1000)); XCTAssertFalse(f.fading(2000))
        XCTAssertEqual(Midi.encode("note 60 127 2"), [0x91, 60, 127])
        XCTAssertEqual(Midi.encode("mmc play"), [0xF0, 0x7F, 0x7F, 0x06, 2, 0xF7])
        XCTAssertNil(Midi.encode("note 200 1"))
        let mo = Morse.pattern("SOS")
        XCTAssertEqual(mo.count, 18); XCTAssertEqual(mo[0], 0); XCTAssertEqual(mo[1], 120)
        XCTAssertLessThanOrEqual(Morse.pattern(String(repeating: "0 ", count: 30)).reduce(0, +), 45_000 + 7 * 120 + 360)
    }

    func testInviteLink() {
        var i = Invite(); i.hosts = ["10.0.0.2:8470"]; i.pin = "123456"; i.key = "abcd-efgh"; i.hub = "Hall A"
        let l = InviteLink.build(i)
        let p = InviteLink.parse(l)
        XCTAssertEqual(p?.hosts, ["10.0.0.2:8470"]); XCTAssertEqual(p?.pin, "123456"); XCTAssertEqual(p?.hub, "Hall A")
        XCTAssertNil(InviteLink.parse("cuplaj://join?r=regie"))
        XCTAssertNil(InviteLink.parse("https://x"))
    }

    func testMediaAssemblyRejectsBadHash() throws {
        let dir = FileManager.default.temporaryDirectory.appendingPathComponent("cuplaj-media-" + UUID().uuidString)
        let asm = MediaAsm(dir: dir)
        let data = Data((0..<30000).map { UInt8($0 % 251) })
        let ch = Media.chunks(data)
        var last = ""
        for (i, n, b) in ch {
            var m = jo(["mid": "abc123", "kind": "image", "i": i, "n": n, "b64": b])
            if i == 0 { m["size"] = data.count; m["sha"] = Sec.sha256Hex(data) }
            last = asm.add(m)
        }
        XCTAssertEqual(last, "done")
        XCTAssertNotNil(asm.file("abc123", "image"))
        var m = jo(["mid": "zzz", "kind": "image", "i": 0, "n": 1, "b64": data.prefix(10).base64EncodedString()])
        m["size"] = 10; m["sha"] = String(repeating: "0", count: 64)
        XCTAssertEqual(asm.add(m), "bad_hash")
        XCTAssertEqual(asm.add(jo(["mid": "../x", "kind": "image", "i": 0])), "bad_id")
        try? FileManager.default.removeItem(at: dir)
    }

    func testReport() {
        let md = Report.markdown(hub: "Hall|A", startMs: 0, nowMs: 120_000, peers: [RPeer(name: "A|B", role: "spectator", number: "212001", online: true, bat: 10, zone: 0, calls: 1, msgs: 2, zones: 0)], events: ["x"], counters: [("cues sent", 3)])
        XCTAssertTrue(md.contains("Hall/A"))
        XCTAssertTrue(md.contains("low battery"))
        XCTAssertTrue(md.contains("A/B"))
    }

    func testDucker() {
        let d = Ducker()
        for _ in 0..<10 { _ = d.step(true) }
        XCTAssertLessThan(d.gain, 0.35)
        for _ in 0..<80 { _ = d.step(false) }
        XCTAssertGreaterThan(d.gain, 0.95)
    }
}
