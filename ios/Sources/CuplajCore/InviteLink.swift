import Foundation

/// An invitation: one link that configures the guest phone (addresses, key, PIN, room, role).
public struct Invite {
    public var hosts: [String] = []
    public var key = ""
    public var pin = ""
    public var room = ""
    public var hub = ""
    public var role = "spectator"
    public init() {}
}

public enum InviteLink {
    private static let unreserved = CharacterSet(charactersIn: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~")
    private static func enc(_ s: String) -> String { s.addingPercentEncoding(withAllowedCharacters: unreserved) ?? "" }

    public static func build(_ i: Invite) -> String {
        var s = "cuplaj://join?r=" + enc(i.role)
        if !i.hosts.isEmpty { s += "&h=" + enc(i.hosts.joined(separator: ",")) }
        if !i.key.isEmpty { s += "&k=" + enc(i.key) }
        if !i.pin.isEmpty { s += "&p=" + enc(i.pin) }
        if !i.room.isEmpty { s += "&c=" + enc(i.room) }
        if !i.hub.isEmpty { s += "&n=" + enc(i.hub) }
        return s
    }

    /// nil for anything that is not a valid invitation; values are cleaned and limited.
    public static func parse(_ link: String?) -> Invite? {
        guard let link = link, link.count <= 800 else { return nil }
        let t = link.trimmingCharacters(in: .whitespacesAndNewlines)
        guard t.hasPrefix("cuplaj://join") else { return nil }
        let q = t.firstIndex(of: "?").map { String(t[t.index(after: $0)...]) } ?? ""
        var m = [String: String]()
        for part in q.split(separator: "&") {
            let kv = part.split(separator: "=", maxSplits: 1, omittingEmptySubsequences: false)
            let k = String(kv[0])
            let v = kv.count > 1 ? (String(kv[1]).replacingOccurrences(of: "+", with: " ").removingPercentEncoding ?? "") : ""
            m[k] = String(String.UnicodeScalarView(v.unicodeScalars.filter { $0.value >= 32 }).prefix(200))
        }
        let role = m["r"] ?? "spectator"
        if role != "spectator" && role != "actor" { return nil } // nobody becomes Regie from a link
        var inv = Invite()
        inv.role = role
        inv.hosts = Array((m["h"] ?? "").split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty && $0.count <= 100 && $0.allSatisfy { $0.isLetter || $0.isNumber || ".-:[]_".contains($0) } }.prefix(8))
        inv.pin = String((m["p"] ?? "").filter { $0.isASCII && $0.isNumber }.prefix(8))
        inv.key = String((m["k"] ?? "").prefix(64))
        inv.room = String((m["c"] ?? "").prefix(32))
        inv.hub = String((m["n"] ?? "").prefix(40))
        return inv
    }
}
