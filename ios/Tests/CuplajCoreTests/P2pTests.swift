import XCTest
@testable import CuplajCore

final class P2pTests: XCTestCase {
    func testDirectAudioLoopback() throws {
        let got = expectation(description: "pcm")
        var received = Data()
        let a = try XCTUnwrap(P2pLink { _ in })
        let b = try XCTUnwrap(P2pLink { d in received = d; got.fulfill() })
        let key = Sec.randomBytes(16)
        a.begin(key: key, candidates: ["127.0.0.1:\(b.port)"], initiator: true)
        b.begin(key: key, candidates: ["127.0.0.1:\(a.port)"], initiator: false)
        var tries = 0
        while !(a.up && b.up) && tries < 40 { Thread.sleep(forTimeInterval: 0.1); tries += 1 }
        XCTAssertTrue(a.up && b.up)
        let pcm = Data((0..<320).map { UInt8($0 % 200) })
        XCTAssertTrue(a.sendPcm(pcm))
        wait(for: [got], timeout: 3)
        XCTAssertEqual(received.count, 320)
        a.close(); b.close()
    }

    func testWrongKeyIsIgnored() throws {
        let a = try XCTUnwrap(P2pLink { _ in })
        let b = try XCTUnwrap(P2pLink { _ in XCTFail("must not decode") })
        a.begin(key: Sec.randomBytes(16), candidates: ["127.0.0.1:\(b.port)"], initiator: true)
        b.begin(key: Sec.randomBytes(16), candidates: ["127.0.0.1:\(a.port)"], initiator: false)
        Thread.sleep(forTimeInterval: 1.0)
        XCTAssertFalse(a.up); XCTAssertFalse(b.up)
        a.close(); b.close()
    }
}
