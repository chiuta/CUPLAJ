import XCTest
@testable import CuplajCore

/// A tiny fake SIP server: answers REGISTER with 200 and replays an INVITE to the gateway.
final class SipTests: XCTestCase {
    func testRegisterAgainstFakeServer() throws {
        let server = try XCTUnwrap(UdpPort(bind: 0))
        var got = false
        let th = Thread {
            for _ in 0..<40 {
                guard let (d, from) = server.receive(), let t = String(data: d, encoding: .utf8), t.hasPrefix("REGISTER") else { continue }
                func h(_ n: String) -> String { t.components(separatedBy: "\r\n").first { $0.lowercased().hasPrefix(n.lowercased() + ":") }.map { String($0.dropFirst(n.count + 1)).trimmingCharacters(in: .whitespaces) } ?? "" }
                let r = "SIP/2.0 200 OK\r\nVia: \(h("Via"))\r\nFrom: \(h("From"))\r\nTo: \(h("To"));tag=x\r\nCall-ID: \(h("Call-ID"))\r\nCSeq: \(h("CSeq"))\r\nContent-Length: 0\r\n\r\n"
                server.send(Data(r.utf8), to: from)
                got = true
                return
            }
        }
        th.start()
        let gw = SipGateway(SipConfig(server: "127.0.0.1", port: server.port, user: "u", pass: "p"))
        XCTAssertTrue(gw.start())
        var n = 0
        while !gw.registered && n < 40 { Thread.sleep(forTimeInterval: 0.1); n += 1 }
        XCTAssertTrue(got)
        XCTAssertTrue(gw.registered)
        gw.stop()
        Thread.sleep(forTimeInterval: 0.6)
        server.close()
    }
}
