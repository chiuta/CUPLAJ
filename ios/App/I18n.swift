import Foundation

/// Translations in 8 languages from i18n.tsv (the same file the Android app uses):
/// key TAB en TAB ro TAB fr TAB it TAB es TAB pt TAB de TAB hu.
enum L {
    static let codes = ["en", "ro", "fr", "it", "es", "pt", "de", "hu"]
    static let names = ["English", "Română", "Français", "Italiano", "Español", "Português", "Deutsch", "Magyar"]
    private static var tbl = [String: [String]]()
    static var lang = "en"

    static func load() {
        if !tbl.isEmpty { return }
        guard let url = Bundle.main.url(forResource: "i18n", withExtension: "tsv"),
              let text = try? String(contentsOf: url, encoding: .utf8) else { return }
        for line in text.components(separatedBy: "\n") {
            if line.trimmingCharacters(in: .whitespaces).isEmpty || line.hasPrefix("#") { continue }
            let p = line.components(separatedBy: "\t")
            if p.count >= 9 { tbl[p[0]] = (0..<8).map { p[$0 + 1].replacingOccurrences(of: "\\n", with: "\n") } }
        }
    }

    static func detect() -> String {
        let l = Locale.current.language.languageCode?.identifier ?? "en"
        return codes.contains(l) ? l : "en"
    }

    static func t(_ k: String) -> String {
        guard let a = tbl[k] else { return k }
        let i = max(0, codes.firstIndex(of: lang) ?? 0)
        return a[i].isEmpty ? a[0] : a[i]
    }

    /// %s and %d placeholders, filled in order (as on Android).
    static func t(_ k: String, _ args: Any...) -> String {
        let f = t(k)
        var out = ""
        var i = f.startIndex
        var n = 0
        while i < f.endIndex {
            let c = f[i]
            if c == "%", f.index(after: i) < f.endIndex {
                let d = f[f.index(after: i)]
                if d == "s" || d == "d" {
                    out += n < args.count ? "\(args[n])" : ""
                    n += 1
                    i = f.index(i, offsetBy: 2)
                    continue
                }
                if d == "%" { out += "%"; i = f.index(i, offsetBy: 2); continue }
            }
            out.append(c)
            i = f.index(after: i)
        }
        return out
    }

    /// For strings that exist only in the iOS app: the key's translation if present, else the English fallback.
    static func or(_ k: String, _ fallback: String) -> String { tbl[k] == nil ? fallback : t(k) }
}
