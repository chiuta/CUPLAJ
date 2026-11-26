import XCTest
@testable import CuplajCore

final class EraTests: XCTestCase {
    func testKinds() {
        XCTAssertEqual(Era.kind(1985), Era.rotary)
        XCTAssertEqual(Era.kind(1989), Era.rotary)
        XCTAssertEqual(Era.kind(1995), Era.keypad)
        XCTAssertEqual(Era.kind(2003), Era.mobile)
        XCTAssertEqual(Era.kind(2025), Era.smart)
        XCTAssertEqual(Era.kind(2045), Era.wear)
        XCTAssertEqual(Era.kind(2080), Era.mind)
        XCTAssertEqual(Set(Era.all.map { Era.kind($0) }).count, 6)
    }

    func testLimitsAndDigits() {
        XCTAssertEqual(Era.msgLimit(2003), 160)
        XCTAssertEqual(Era.msgLimit(2080), 60)
        XCTAssertTrue(Era.hasDigits(1985) && Era.hasDigits(2025))
        XCTAssertFalse(Era.hasDigits(2045) || Era.hasDigits(2080))
    }

    func testSweep() {
        XCTAssertNil(Era.parseSweep("2025"))
        let s = Era.parseSweep("1985,1995,2003,2025,2045,2080;3000")
        XCTAssertEqual(s, Era.Sweep(eras: [1985, 1995, 2003, 2025, 2045, 2080], stepMs: 3000))
        XCTAssertEqual(Era.parseSweep("1985,2080")?.stepMs, 4000)
        XCTAssertEqual(Era.parseSweep("1985,2080;10")?.stepMs, 1500)
        XCTAssertEqual(Era.parseSweep("1985,2080;999999")?.stepMs, 30000)
        XCTAssertEqual(Era.parseSweep("1985,abc,9999,2045")?.eras, [1985, 2045])
        XCTAssertNil(Era.parseSweep("abc,2045"))
        XCTAssertEqual(Era.parseSweep((1...30).map { String(1980 + $0) }.joined(separator: ","))?.eras.count, 12)
    }

    func testFirstEra() {
        XCTAssertEqual(Era.firstEra("2045"), 2045)
        XCTAssertEqual(Era.firstEra("2025,2080;2000"), 2025)
        XCTAssertNil(Era.firstEra("x"))
        XCTAssertNil(Era.firstEra("5"))
    }
}
