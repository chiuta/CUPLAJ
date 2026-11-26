import Foundation
import AVFoundation
import CoreHaptics
import CoreMIDI
import UIKit
import UserNotifications
import AudioToolbox
import CuplajCore
import CoreMotion
#if canImport(Darwin)
import Darwin
#endif

/// Vibration. iPhones cannot buzz for an arbitrary time with the old API, so Core Haptics is used (iPhone 8 and later);
/// older devices fall back to the system vibration, which has a fixed length.
final class Haptics {
    private var engine: CHHapticEngine?
    private var player: CHHapticPatternPlayer?
    private var fallback: Timer?

    private func ensure() -> CHHapticEngine? {
        if engine != nil { return engine }
        guard CHHapticEngine.capabilitiesForHardware().supportsHaptics else { return nil }
        do {
            let e = try CHHapticEngine()
            e.isAutoShutdownEnabled = true
            e.resetHandler = { [weak self] in try? self?.engine?.start() }
            try e.start()
            engine = e
        } catch { engine = nil }
        return engine
    }

    /// Pattern like Android's: [pause, buzz, pause, buzz ...] in ms. `repeatAll` loops it (used while ringing).
    func pattern(_ p: [Int64], repeatAll: Bool) {
        cancel()
        guard let e = ensure() else { systemFallback(p, repeatAll: repeatAll); return }
        var events = [CHHapticEvent]()
        var t = 0.0
        for (i, ms) in p.enumerated() {
            let d = Double(ms) / 1000
            if i % 2 == 1 && d > 0 {
                events.append(CHHapticEvent(eventType: .hapticContinuous,
                                            parameters: [CHHapticEventParameter(parameterID: .hapticIntensity, value: 1), CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.5)],
                                            relativeTime: t, duration: min(d, 30)))
            }
            t += d
        }
        if events.isEmpty { return }
        do {
            let pat = try CHHapticPattern(events: events, parameters: [])
            let pl = try e.makePlayer(with: pat)
            player = pl
            try pl.start(atTime: CHHapticTimeImmediate)
            if repeatAll {
                fallback = Timer.scheduledTimer(withTimeInterval: max(t, 1), repeats: true) { [weak self] _ in try? self?.player?.start(atTime: CHHapticTimeImmediate) }
            }
        } catch { systemFallback(p, repeatAll: repeatAll) }
    }

    func buzz(ms: Int64) { pattern([0, max(1, min(ms, 30_000))], repeatAll: false) }

    private func systemFallback(_ p: [Int64], repeatAll: Bool) {
        var t = 0.0
        for (i, ms) in p.enumerated() {
            if i % 2 == 1 { DispatchQueue.main.asyncAfter(deadline: .now() + t) { AudioServicesPlaySystemSound(kSystemSoundID_Vibrate) } }
            t += Double(ms) / 1000
        }
        if repeatAll { fallback = Timer.scheduledTimer(withTimeInterval: max(t, 1), repeats: true) { _ in AudioServicesPlaySystemSound(kSystemSoundID_Vibrate) } }
    }

    func cancel() {
        fallback?.invalidate(); fallback = nil
        try? player?.stop(atTime: CHHapticTimeImmediate)
        player = nil
    }
}

enum Torch {
    static func flash(ms: Int) {
        guard let d = AVCaptureDevice.default(for: .video), d.hasTorch else { return }
        do {
            try d.lockForConfiguration()
            try d.setTorchModeOn(level: 1.0)
            d.unlockForConfiguration()
        } catch { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + Double(max(50, min(ms, 60_000))) / 1000) {
            if (try? d.lockForConfiguration()) != nil { d.torchMode = .off; d.unlockForConfiguration() }
        }
    }
}

final class Speech: NSObject {
    private let synth = AVSpeechSynthesizer()
    func say(_ text: String, lang: String) {
        stop()
        let u = AVSpeechUtterance(string: text)
        let tags = ["en": "en-US", "ro": "ro-RO", "fr": "fr-FR", "it": "it-IT", "es": "es-ES", "pt": "pt-PT", "de": "de-DE", "hu": "hu-HU"]
        u.voice = AVSpeechSynthesisVoice(language: tags[lang] ?? "en-US")
        synth.speak(u)
    }
    func stop() { if synth.isSpeaking { synth.stopSpeaking(at: .immediate) } }
}

enum Notif {
    static func ask() { UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { _, _ in } }

    static func ring(title: String, who: String) {
        guard UIApplication.shared.applicationState != .active else { return }
        let c = UNMutableNotificationContent()
        c.title = title; c.body = who; c.sound = .defaultCritical
        UNUserNotificationCenter.current().add(UNNotificationRequest(identifier: "ring", content: c, trigger: nil))
    }

    static func cancel() { UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: ["ring"]) }
}

// ====================================================================== UDP helpers (OSC, Art-Net, beacons)

final class UdpSocket {
    let fd: Int32
    init?(port: Int? = nil, broadcast: Bool = false, reuse: Bool = false) {
        fd = socket(AF_INET, SOCK_DGRAM, 0)
        if fd < 0 { return nil }
        var one: Int32 = 1
        if broadcast { setsockopt(fd, SOL_SOCKET, SO_BROADCAST, &one, socklen_t(MemoryLayout<Int32>.size)) }
        if reuse { setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &one, socklen_t(MemoryLayout<Int32>.size)); setsockopt(fd, SOL_SOCKET, SO_REUSEPORT, &one, socklen_t(MemoryLayout<Int32>.size)) }
        if let p = port {
            var a = sockaddr_in()
            a.sin_family = sa_family_t(AF_INET)
            a.sin_port = in_port_t(UInt16(p).bigEndian)
            a.sin_addr = in_addr(s_addr: INADDR_ANY.bigEndian)
            let r = withUnsafePointer(to: &a) { $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { bind(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size)) } }
            if r != 0 { Darwin.close(fd); return nil }
            var tv = timeval(tv_sec: 1, tv_usec: 0)
            setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))
        }
    }

    func send(_ data: Data, host: String, port: Int) {
        var hints = addrinfo(); hints.ai_family = AF_INET; hints.ai_socktype = SOCK_DGRAM
        var res: UnsafeMutablePointer<addrinfo>?
        guard getaddrinfo(host, String(port), &hints, &res) == 0, let ai = res else { return }
        defer { freeaddrinfo(res) }
        let b = [UInt8](data)
        _ = b.withUnsafeBytes { sendto(fd, $0.baseAddress, b.count, 0, ai.pointee.ai_addr, ai.pointee.ai_addrlen) }
    }

    /// Blocks up to 1 s (receive timeout). Returns (data, sender ip).
    func receive() -> (Data, String)? {
        var buf = [UInt8](repeating: 0, count: 2048)
        var from = sockaddr_in()
        var len = socklen_t(MemoryLayout<sockaddr_in>.size)
        let n = withUnsafeMutablePointer(to: &from) { fp in
            fp.withMemoryRebound(to: sockaddr.self, capacity: 1) { recvfrom(fd, &buf, buf.count, 0, $0, &len) }
        }
        if n <= 0 { return nil }
        var ip = [CChar](repeating: 0, count: Int(INET_ADDRSTRLEN))
        var ad = from.sin_addr
        inet_ntop(AF_INET, &ad, &ip, socklen_t(INET_ADDRSTRLEN))
        return (Data(buf[0..<n]), String(cString: ip))
    }

    func close() { Darwin.close(fd) }
}

/// OSC out (every Regie cue also as /cuplaj/cue) and in (commands from a desk, protected by the PIN in the address).
final class OscOut {
    private let host: String, port: Int
    private let sock = UdpSocket()
    init(host: String, port: Int) { self.host = host; self.port = port }
    func send(_ d: Data) { sock?.send(d, host: host, port: port) }
    func close() { sock?.close() }
}

final class OscIn {
    private let port: Int
    private let pin: () -> String
    private let onCmd: (Osc.Cmd) -> Void
    private var sock: UdpSocket?
    private var running = false
    init(port: Int, pin: @escaping () -> String, onCmd: @escaping (Osc.Cmd) -> Void) { self.port = port; self.pin = pin; self.onCmd = onCmd }

    func start() -> Bool {
        guard let s = UdpSocket(port: port, reuse: true) else { return false }
        sock = s; running = true
        Thread { [self] in
            var winStart: Int64 = 0, winN = 0
            while running {
                guard let (d, _) = s.receive() else { continue }
                let now = nowMs()
                if now - winStart > 1000 { winStart = now; winN = 0 }
                winN += 1
                if winN > 20 { continue }
                guard let m = Osc.decode(d), let c = Osc.command(m, pin: pin()) else { continue }
                onCmd(c)
            }
        }.start()
        return true
    }

    func stop() { running = false; sock?.close(); sock = nil }
}

/// Art-Net toward the lighting desk: 25 Hz during fades, 1 Hz otherwise. Nothing is sent before the first cue
/// (a frame of zeros would turn the desk's lights off).
final class DmxOut {
    private let host: String
    private var faders = [Int: DmxFader]()
    private let lock = NSLock()
    private var seq = 0
    private var gen = 0
    private var running = true
    private let sock = UdpSocket(broadcast: true)
    private let queue = DispatchQueue(label: "cuplaj.dmx")

    init(host: String) { self.host = host }

    private func tick(_ my: Int) {
        lock.lock()
        if !running || my != gen { lock.unlock(); return }
        let now = nowMs()
        var fading = false
        let frames = faders.map { (u, f) -> (Int, [UInt8]) in if f.fading(now) { fading = true }; return (u, f.frame(now)) }
        lock.unlock()
        let dest = host.trimmingCharacters(in: .whitespaces).isEmpty ? "255.255.255.255" : host
        for (u, fr) in frames {
            seq = seq >= 255 ? 1 : seq + 1
            sock?.send(ArtNet.packet(universe: u, seq: seq, data: fr), host: dest, port: ArtNet.PORT)
        }
        queue.asyncAfter(deadline: .now() + (fading ? 0.04 : 1.0)) { [weak self] in self?.tick(my) }
    }

    func go(_ c: DmxCmd) {
        lock.lock()
        if faders.count >= 8 && faders[c.universe] == nil { lock.unlock(); return }
        let f = faders[c.universe] ?? DmxFader()
        faders[c.universe] = f
        f.go(c, now: nowMs())
        gen += 1
        let my = gen
        lock.unlock()
        queue.async { [weak self] in self?.tick(my) }
    }

    func close() { lock.lock(); running = false; lock.unlock(); sock?.close() }
}

/// MIDI out through CoreMIDI: every destination (USB, Bluetooth LE, network session) plus a virtual source.
final class MidiOut {
    private var client = MIDIClientRef()
    private var port = MIDIPortRef()
    private var source = MIDIEndpointRef()
    private var ready = false

    init() {
        if MIDIClientCreate("CUPLAJ" as CFString, nil, nil, &client) == noErr,
           MIDIOutputPortCreate(client, "out" as CFString, &port) == noErr {
            MIDISourceCreate(client, "CUPLAJ" as CFString, &source)
            ready = true
        }
    }

    func send(_ bytes: [UInt8]) -> Bool {
        guard ready, !bytes.isEmpty else { return false }
        var buf = [UInt8](repeating: 0, count: 1024)
        var ok = false
        buf.withUnsafeMutableBytes { raw in
            let list = raw.baseAddress!.assumingMemoryBound(to: MIDIPacketList.self)
            var pkt = MIDIPacketListInit(list)
            pkt = MIDIPacketListAdd(list, 1024, pkt, 0, bytes.count, bytes)
            let n = MIDIGetNumberOfDestinations()
            for i in 0..<n {
                let d = MIDIGetDestination(i)
                if MIDISend(port, d, list) == noErr { ok = true }
            }
            if source != 0 { MIDIReceived(source, list); ok = true }
        }
        return ok
    }
}

/// Listens for hub beacons ("CUPLAJ1|name|port|room|enc") on UDP 8471; Android and iOS hubs both send them.
struct HubInfo: Identifiable, Equatable {
    var id: String { host }
    let host: String, name: String, needsRoom: Bool, encrypted: Bool
}

final class BeaconListener {
    private var sock: UdpSocket?
    private var running = false
    private var found = [String: (HubInfo, Date)]()
    private let lock = NSLock()
    var onChange: (([HubInfo]) -> Void)?

    func start() {
        if running { return }
        guard let s = UdpSocket(port: BEACON_PORT, broadcast: true, reuse: true) else { return }
        sock = s; running = true
        Thread { [self] in
            while running {
                if let (d, ip) = s.receive(), let t = String(data: d, encoding: .utf8)?.split(separator: "|", omittingEmptySubsequences: false).map(String.init),
                   t.count >= 5, t[0] == "CUPLAJ1", let p = Int(t[2]) {
                    let h = HubInfo(host: ip + ":" + String(p), name: String(t[1].prefix(40)), needsRoom: t[3] == "1", encrypted: t[4] == "1")
                    lock.lock(); found[h.host] = (h, Date()); lock.unlock()
                }
                lock.lock()
                let cutoff = Date().addingTimeInterval(-7)
                found = found.filter { $0.value.1 > cutoff }
                let list = found.values.map { $0.0 }.sorted { $0.name < $1.name }
                lock.unlock()
                DispatchQueue.main.async { self.onChange?(list) }
            }
        }.start()
    }

    func stop() { running = false; sock?.close(); sock = nil }
}

/// Publishes the hub on Bonjour (_cuplaj._tcp) so Android's NSD discovery finds it.
final class Bonjour {
    private var svc: NetService?
    func publish(name: String, port: Int) {
        let s = NetService(domain: "local.", type: "_cuplaj._tcp.", name: String(name.prefix(40)), port: Int32(port))
        s.publish()
        svc = s
    }
    func stop() { svc?.stop(); svc = nil }
}


/// Shake the phone to answer or hang up (2045 wearables era). Two strong jolts within 700 ms.
final class ShakeDetector: ObservableObject {
    private let mm = CMMotionManager()
    private var last = Date.distantPast
    var onShake: () -> Void = {}

    func start() {
        guard mm.isAccelerometerAvailable, !mm.isAccelerometerActive else { return }
        mm.accelerometerUpdateInterval = 0.02
        mm.startAccelerometerUpdates(to: OperationQueue.main) { [weak self] d, _ in
            guard let self = self, let a = d?.acceleration else { return }
            let g = (a.x * a.x + a.y * a.y + a.z * a.z).squareRoot()
            if g < 2.6 { return }
            let now = Date(), dt = now.timeIntervalSince(self.last)
            if dt > 0.12 && dt < 0.7 { self.last = .distantPast; self.onShake() } else if dt >= 0.7 { self.last = now }
        }
    }

    func stop() { mm.stopAccelerometerUpdates() }
}
