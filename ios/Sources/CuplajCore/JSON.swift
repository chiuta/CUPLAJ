import Foundation

/// Loose JSON object, like org.json.JSONObject on Android: missing or mistyped fields read as defaults.
public typealias JSONObject = [String: Any]

public extension Dictionary where Key == String, Value == Any {
    func optString(_ k: String, _ d: String = "") -> String {
        guard let v = self[k], !(v is NSNull) else { return d }
        if let s = v as? String { return s }
        if let n = v as? NSNumber {
            if CFGetTypeID(n) == CFBooleanGetTypeID() { return n.boolValue ? "true" : "false" }
            return n.stringValue
        }
        return d
    }

    func optInt(_ k: String, _ d: Int = 0) -> Int {
        guard let v = self[k] else { return d }
        if let n = v as? NSNumber { return n.intValue }
        if let s = v as? String, let i = Int(s.trimmingCharacters(in: .whitespaces)) { return i }
        return d
    }

    func optLong(_ k: String, _ d: Int64 = 0) -> Int64 {
        guard let v = self[k] else { return d }
        if let n = v as? NSNumber { return n.int64Value }
        if let s = v as? String, let i = Int64(s.trimmingCharacters(in: .whitespaces)) { return i }
        return d
    }

    func optBool(_ k: String, _ d: Bool = false) -> Bool {
        guard let v = self[k] else { return d }
        if let n = v as? NSNumber { return n.boolValue }
        if let s = v as? String { return s.lowercased() == "true" ? true : (s.lowercased() == "false" ? false : d) }
        return d
    }

    func optArray(_ k: String) -> [Any]? { self[k] as? [Any] }
    func optObject(_ k: String) -> JSONObject? { self[k] as? JSONObject }
    func has(_ k: String) -> Bool { self[k] != nil && !(self[k] is NSNull) }
}

public func jo(_ pairs: KeyValuePairs<String, Any?>) -> JSONObject {
    var o = JSONObject()
    for (k, v) in pairs { o[k] = v ?? NSNull() }
    return o
}

public func jsonData(_ o: JSONObject) -> Data {
    (try? JSONSerialization.data(withJSONObject: o, options: [])) ?? Data("{}".utf8)
}

public func jsonString(_ o: JSONObject) -> String { String(data: jsonData(o), encoding: .utf8) ?? "{}" }

public func parseJSON(_ d: Data) -> JSONObject? {
    (try? JSONSerialization.jsonObject(with: d, options: [])) as? JSONObject
}

public func parseJSONArray(_ s: String) -> [Any]? {
    (try? JSONSerialization.jsonObject(with: Data(s.utf8), options: [])) as? [Any]
}

public func nowMs() -> Int64 { Int64((Date().timeIntervalSince1970 * 1000).rounded()) }

/// Keep only printable characters (code >= 32), trim, limit length: the same "clean" as the Android hub.
public func cleanText(_ s: String, _ n: Int) -> String {
    let f = String(String.UnicodeScalarView(s.unicodeScalars.filter { $0.value >= 32 }))
    return String(f.trimmingCharacters(in: .whitespacesAndNewlines).prefix(n))
}

public func hex(_ d: Data) -> String { d.map { String(format: "%02x", $0) }.joined() }

public func unhex(_ s: String) -> Data? {
    var out = Data()
    var i = s.startIndex
    while i < s.endIndex {
        guard let j = s.index(i, offsetBy: 2, limitedBy: s.endIndex), let b = UInt8(s[i..<j], radix: 16) else { return nil }
        out.append(b)
        i = j
    }
    return out
}
