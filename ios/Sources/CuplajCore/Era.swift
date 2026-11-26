import Foundation

/// Telephone eras. The era is a year; what matters is the kind it falls into:
/// 0 rotary (<1990), 1 keypad (<2000), 2 mobile (<2020), 3 smartphone (<2040),
/// 4 wearables (<2060, whispered calls, shake or tap to answer), 5 shared mind (2060+, no handset).
public enum Era {
    public static let all = [1985, 1995, 2003, 2025, 2045, 2080]
    public static let rotary = 0, keypad = 1, mobile = 2, smart = 3, wear = 4, mind = 5

    public static func kind(_ e: Int) -> Int {
        if e < 1990 { return rotary }
        if e < 2000 { return keypad }
        if e < 2020 { return mobile }
        if e < 2040 { return smart }
        if e < 2060 { return wear }
        return mind
    }

    public static func msgLimit(_ e: Int) -> Int {
        switch kind(e) {
        case rotary, keypad: return 500
        case mobile: return 160
        case smart: return 500
        case wear: return 200
        default: return 60
        }
    }

    /// Wearables and the shared mind call by choosing a name, not by dialling digits.
    public static func hasDigits(_ e: Int) -> Bool { kind(e) <= smart }

    public struct Sweep: Equatable {
        public let eras: [Int]
        public let stepMs: Int
    }

    /// "Time travel" cue text: `1985,1995,2003,2025,2045,2080;4000` steps through the eras, one every 4000 ms.
    /// Returns nil for a plain single era. Years 1900...2200, at most 12 steps, step 1.5 to 30 seconds (default 4 s).
    public static func parseSweep(_ text: String) -> Sweep? {
        let semi = text.firstIndex(of: ";")
        let list = semi.map { String(text[..<$0]) } ?? text
        if !list.contains(",") { return nil }
        let eras = list.split(separator: ",").compactMap { Int($0.trimmingCharacters(in: .whitespaces)) }.filter { $0 >= 1900 && $0 <= 2200 }.prefix(12)
        if eras.count < 2 { return nil }
        var step = 4000
        if let s = semi, let v = Int(String(text[text.index(after: s)...]).trimmingCharacters(in: .whitespaces)) { step = v }
        return Sweep(eras: Array(eras), stepMs: min(30000, max(1500, step)))
    }

    /// First era of a cue text, single or sweep; nil if it is not a valid era.
    public static func firstEra(_ text: String) -> Int? {
        if let s = parseSweep(text) { return s.eras[0] }
        if let v = Int(text.trimmingCharacters(in: .whitespaces)), v >= 1900 && v <= 2200 { return v }
        return nil
    }
}
