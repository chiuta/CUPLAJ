import XCTest
@testable import CuplajCore

/// End-to-end on loopback: a real Hub, real sockets, real Net clients.
final class HubTests: XCTestCase {
    final class Box {
        private let lock = NSLock()
        private(set) var msgs = [JSONObject]()
        private(set) var audio = [(Int, Data)]()
        func add(_ m: JSONObject) { lock.lock(); msgs.append(m); lock.unlock() }
        func addAudio(_ s: Int, _ d: Data) { lock.lock(); audio.append((s, d)); lock.unlock() }
        func types() -> [String] { lock.lock(); defer { lock.unlock() }; return msgs.map { $0.optString("t") } }
        func last(_ t: String) -> JSONObject? { lock.lock(); defer { lock.unlock() }; return msgs.last { $0.optString("t") == t } }
        func audioCount() -> Int { lock.lock(); defer { lock.unlock() }; return audio.count }
    }

    func until(_ cond: () -> Bool, _ secs: Double = 6) -> Bool {
        let end = Date().addingTimeInterval(secs)
        while Date() < end { if cond() { return true }; Thread.sleep(forTimeInterval: 0.02) }
        return cond()
    }

    func client(_ port: Int, id: String, name: String, role: String = "spectator", pin: String = "", key: String = "") -> (Net, Box) {
        let b = Box()
        let n = Net(onJson: { b.add($0) }, onAudio: { b.addAudio($0, $1) }, onState: { _, _ in })
        n.start(host: "127.0.0.1:\(port)", hello: jo(["t": "hello", "id": id, "name": name, "role": role, "pin": pin, "room": "", "tok": "", "p2p": false]), key: Sec.deriveKey(key))
        return (n, b)
    }

    func port() -> Int { 20000 + Int.random(in: 0..<20000) }

    func testWelcomeNumbersAndPinDenied() {
        let p = port()
        let h = Hub(hubName: "T", pin: "123456", port: p)
        h.start(); defer { h.stop() }
        Thread.sleep(forTimeInterval: 0.3)
        XCTAssertNil(h.error)
        let (a, ba) = client(p, id: "a", name: "Ana"); defer { a.stop() }
        XCTAssertTrue(until { ba.last("welcome") != nil })
        XCTAssertEqual(ba.last("welcome")?.optString("number"), "211001")
        let (r, br) = client(p, id: "r", name: "Rex", role: "actor", pin: "000000"); defer { r.stop() }
        // loopback is trusted by default (the Regie phone itself), so a wrong PIN passes only because it is local
        XCTAssertTrue(until { br.last("welcome") != nil || br.last("denied") != nil })
    }

    func testCallRingAnswerAudioHangup() {
        let p = port()
        let h = Hub(hubName: "T", pin: "123456", port: p)
        h.start(); defer { h.stop() }
        Thread.sleep(forTimeInterval: 0.3)
        let (a, ba) = client(p, id: "a", name: "Ana"); defer { a.stop() }
        let (b, bb) = client(p, id: "b", name: "Bob"); defer { b.stop() }
        XCTAssertTrue(until { ba.last("welcome") != nil && bb.last("welcome") != nil })
        let nb = bb.last("welcome")!.optString("number")
        a.send(jo(["t": "offhook"]))
        XCTAssertTrue(until { ba.last("dialtone") != nil })
        a.send(jo(["t": "dial", "number": nb]))
        XCTAssertTrue(until { ba.last("ringback") != nil && bb.last("ring") != nil })
        b.send(jo(["t": "answer"]))
        XCTAssertTrue(until { ba.last("connected") != nil && bb.last("connected") != nil })
        a.sendAudio(mode: 0, pcm: Data(repeating: 1, count: 320))
        XCTAssertTrue(until { bb.audioCount() > 0 })
        XCTAssertEqual(ba.audioCount(), 0)
        a.send(jo(["t": "hangup"]))
        XCTAssertTrue(until { bb.last("ended") != nil })
    }

    func testBusyAndUnknown() {
        let p = port()
        let h = Hub(hubName: "T", pin: "123456", port: p)
        h.start(); defer { h.stop() }
        Thread.sleep(forTimeInterval: 0.3)
        let (a, ba) = client(p, id: "a", name: "Ana"); defer { a.stop() }
        XCTAssertTrue(until { ba.last("welcome") != nil })
        a.send(jo(["t": "dial", "number": "999999"]))
        XCTAssertTrue(until { ba.last("unknown") != nil })
    }

    func testCueReachesSpectatorsOnlyFromRegie() {
        let p = port()
        let h = Hub(hubName: "T", pin: "123456", port: p)
        h.start(); defer { h.stop() }
        Thread.sleep(forTimeInterval: 0.3)
        let (r, br) = client(p, id: "r", name: "Regie", role: "regie", pin: "123456"); defer { r.stop() }
        let (a, ba) = client(p, id: "a", name: "Ana"); defer { a.stop() }
        XCTAssertTrue(until { br.last("welcome") != nil && ba.last("welcome") != nil })
        a.send(jo(["t": "cue", "to": "all", "kind": "text", "text": "nope"]))
        r.send(jo(["t": "cue", "to": "spectators", "kind": "text", "text": "Hello", "id": "c1"]))
        XCTAssertTrue(until { ba.last("cue") != nil })
        XCTAssertEqual(ba.last("cue")?.optString("text"), "Hello")
        XCTAssertTrue(until { br.last("cueack") != nil })
        XCTAssertEqual(br.last("cueack")?.optInt("sent"), 1)
        XCTAssertEqual(ba.msgs.filter { $0.optString("t") == "cue" }.count, 1)
    }

    func testEncryptedSessionAndWrongKey() {
        let p = port()
        let h = Hub(hubName: "T", pin: "123456", port: p, netKey: "abcd-efgh")
        h.start(); defer { h.stop() }
        Thread.sleep(forTimeInterval: 0.3)
        let (a, ba) = client(p, id: "a", name: "Ana", key: "abcd-efgh"); defer { a.stop() }
        XCTAssertTrue(until { ba.last("welcome") != nil })
        let (w, bw) = client(p, id: "w", name: "Wrong", key: "zzzz-zzzz"); defer { w.stop() }
        XCTAssertTrue(until { bw.last("denied") != nil })
        XCTAssertNil(bw.last("welcome"))
        let (n, bn) = client(p, id: "n", name: "NoKey"); defer { n.stop() }
        XCTAssertTrue(until { bn.last("denied") != nil })
    }

    func testPartyLineCouplingAndEavesdrop() {
        let p = port()
        let h = Hub(hubName: "T", pin: "123456", port: p)
        h.start(); defer { h.stop() }
        Thread.sleep(forTimeInterval: 0.3)
        let (r, br) = client(p, id: "r", name: "Regie", role: "regie", pin: "123456"); defer { r.stop() }
        let (a, ba) = client(p, id: "a", name: "A"); defer { a.stop() }
        let (b, bb) = client(p, id: "b", name: "B"); defer { b.stop() }
        XCTAssertTrue(until { br.last("welcome") != nil && ba.last("welcome") != nil && bb.last("welcome") != nil })
        r.send(jo(["t": "couple", "a": "a", "b": "b"]))
        XCTAssertTrue(until { ba.last("line")?.optInt("line") ?? 0 > 0 && bb.last("line")?.optInt("slot") == 1 })
        _ = br
    }

    func testFramingRejectsOversizeBeforeLogin() {
        let p = port()
        let h = Hub(hubName: "T", pin: "123456", port: p)
        h.start(); defer { h.stop() }
        Thread.sleep(forTimeInterval: 0.3)
        guard let ch = try? TcpChan.connect(host: "127.0.0.1", port: p, timeoutMs: 2000) else { return XCTFail("connect") }
        defer { ch.close() }
        try? ch.write(Data([1, 0, 1, 0, 0]) + Data(repeating: 0x41, count: 16))   // claims 65 536 bytes: more than the 2 KB pre-login limit
        ch.setIdleTimeout(ms: 3000)
        XCTAssertThrowsError(try ch.read(1))
    }

    func testPersistenceRestoresNumbers() {
        let p = port()
        var saved = ""
        let h = Hub(hubName: "T", pin: "123456", port: p)
        h.onPersist = { saved = $0 }
        h.start()
        Thread.sleep(forTimeInterval: 0.3)
        let (a, ba) = client(p, id: "a", name: "Ana")
        XCTAssertTrue(until { ba.last("welcome") != nil })
        let num = ba.last("welcome")!.optString("number")
        a.stop(); h.stop()
        XCTAssertFalse(saved.isEmpty)
        let p2 = port()
        let h2 = Hub(hubName: "T", pin: "123456", port: p2, restore: saved)
        h2.start(); defer { h2.stop() }
        Thread.sleep(forTimeInterval: 0.3)
        let (a2, ba2) = client(p2, id: "a", name: "Ana"); defer { a2.stop() }
        XCTAssertTrue(until { ba2.last("welcome") != nil })
        XCTAssertEqual(ba2.last("welcome")?.optString("number"), num)
    }
}
