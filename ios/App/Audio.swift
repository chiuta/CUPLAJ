import Foundation
import AVFoundation
import CuplajCore

let RATE = 8000        // telephone quality: 8 kHz, 16-bit, mono
let FR = 160           // 20 ms

struct ToneSpec { let f1: Double; let f2: Double; let onMs: Int; let offMs: Int; var amp: Double = 0.22 }

/// Line tones (425 Hz, as on the Romanian telephone network).
enum Tones {
    static let dial = ToneSpec(f1: 425, f2: 0, onMs: 1000, offMs: 0)
    static let ringback = ToneSpec(f1: 425, f2: 0, onMs: 1000, offMs: 4000)
    static let busy = ToneSpec(f1: 425, f2: 0, onMs: 500, offMs: 500)
    static let unknown = ToneSpec(f1: 425, f2: 0, onMs: 200, offMs: 200)
}

/// Short effects: receiver click, rotary pulses, DTMF, beeps, cue presets.
enum Fx {
    private static func noise(_ seed: inout UInt64) -> Double {
        seed = seed &* 6364136223846793005 &+ 1442695040888963407
        return Double((seed >> 33) & 0xFFFF) / 32768.0 - 1.0
    }

    private static func env(_ i: Int, _ n: Int, _ edge: Int) -> Double { min(1.0, Double(min(i, n - i)) / Double(edge)) }
    private static func s16(_ v: Double) -> Int16 { Int16(max(-32767, min(32767, v)) ) }

    static func click(_ amp: Double = 0.5) -> [Int16] {
        var seed: UInt64 = 42
        return (0..<90).map { i in s16(noise(&seed) * amp * 32767 * exp(-Double(i) / 14.0)) }
    }

    /// Rotary dial: n pulses, 10 per second.
    static func pulses(_ n: Int) -> [Int16] {
        let per = RATE / 10
        var out = [Int16](repeating: 0, count: per * n + per)
        let c = click(0.7)
        for k in 0..<n { for (i, v) in c.enumerated() where k * per + i < out.count { out[k * per + i] = v } }
        return out
    }

    static func beep(_ f: Double, _ ms: Int, _ amp: Double = 0.3) -> [Int16] {
        let n = RATE * ms / 1000
        return (0..<n).map { i in s16(sin(2 * .pi * f * Double(i) / Double(RATE)) * amp * 32767 * env(i, n, 40)) }
    }

    static func dtmf(_ d: Character) -> [Int16] {
        guard let idx = Array("123456789*0#").firstIndex(of: d) else { return [] }
        let lo = [697.0, 770, 852, 941][idx / 3], hi = [1209.0, 1336, 1477][idx % 3]
        let n = RATE * 110 / 1000
        return (0..<n).map { i in
            s16((sin(2 * .pi * lo * Double(i) / Double(RATE)) + sin(2 * .pi * hi * Double(i) / Double(RATE))) * 0.18 * 32767 * env(i, n, 30))
        }
    }

    static func preset(_ name: String) -> [Int16] {
        let R = Double(RATE)
        var seed: UInt64 = 7
        switch name {
        case "bell":
            let n = RATE * 18 / 10
            return (0..<n).map { i in
                let t = Double(i), d = exp(-t / (R * 0.45))
                return s16((sin(2 * .pi * 880 * t / R) * 0.6 + sin(2 * .pi * 1760 * t / R) * 0.25 + sin(2 * .pi * 2349 * t / R) * 0.1) * d * 0.7 * 32767)
            }
        case "siren":
            let n = RATE * 3
            return (0..<n).map { i in
                let f = 800 + 400 * sin(2 * .pi * 0.8 * Double(i) / R)
                return s16(sin(2 * .pi * f * Double(i) / R) * 0.35 * 32767 * env(i, n, 400))
            }
        case "knock":
            let n = RATE * 17 / 10
            let starts = [0, RATE * 4 / 10, RATE * 8 / 10, RATE * 13 / 10]
            return (0..<n).map { i in
                var v = 0.0
                for s in starts {
                    let k = i - s
                    if k >= 0 && k < RATE / 8 {
                        v += sin(2 * .pi * 95 * Double(k) / R) * exp(-Double(k) / (R * 0.02)) + noise(&seed) * 0.15 * exp(-Double(k) / (R * 0.005))
                    }
                }
                return s16(v * 0.8 * 32767)
            }
        case "static":
            let n = RATE * 2
            var lp = 0.0
            return (0..<n).map { i in
                lp += 0.45 * (noise(&seed) - lp)
                return s16(lp * 0.5 * 32767 * env(i, n, 800))
            }
        default: return beep(1000, 300)
        }
    }
}

/// Output mixer: voice from any number of sources + line tone + effects, rendered on demand at 8 kHz.
final class Playout {
    private final class SrcQ { var q = [[Int16]](); var primed = false }
    private final class Effect { let d: [Int16]; var pos = 0; init(_ d: [Int16]) { self.d = d } }

    private let lock = NSLock()
    private var srcs = [Int: SrcQ]()
    private var effects = [Effect]()
    private var tone: ToneSpec?
    private var lastTone: ToneSpec?
    private var toneKey = 0
    private var lastToneKey = -1
    private var tpos = 0
    private var ph1 = 0.0, ph2 = 0.0
    private let duck = Ducker()
    var duckOn = true
    private var pending = [Float]()

    func setTone(_ t: ToneSpec?) { lock.lock(); tone = t; toneKey += 1; lock.unlock() }
    func clearVoice() { lock.lock(); srcs.removeAll(); lock.unlock() }
    func clearEffects() { lock.lock(); effects.removeAll(); lock.unlock() }
    func effect(_ d: [Int16]) { if d.isEmpty { return }; lock.lock(); effects.append(Effect(d)); lock.unlock() }

    /// Feed one frame (PCM16 LE after the 1-byte source id).
    func feed(src: Int, data: Data) {
        let b = [UInt8](data.dropFirst())
        let n = b.count / 2
        if n <= 0 { return }
        let s = (0..<n).map { Int16(bitPattern: UInt16(b[2 * $0]) | (UInt16(b[2 * $0 + 1]) << 8)) }
        lock.lock(); defer { lock.unlock() }
        let sq = srcs[src] ?? SrcQ()
        srcs[src] = sq
        sq.q.append(s)
        while sq.q.count > 14 { sq.q.removeFirst() }
    }

    /// One 20 ms mixed frame (called with the lock held).
    private func mixFrame() -> [Float] {
        var mix = [Int](repeating: 0, count: FR)
        var pa = [Int](repeating: 0, count: FR)
        var paNow = false
        for (sid, sq) in srcs {
            if !sq.primed && sq.q.count >= 3 { sq.primed = true }
            if sq.primed {
                if sq.q.isEmpty { sq.primed = false; continue }
                let ch = sq.q.removeFirst()
                if sid == MEDIA_PA_SRC {
                    paNow = true
                    for i in 0..<min(FR, ch.count) { pa[i] += Int(ch[i]) }
                } else {
                    for i in 0..<min(FR, ch.count) { mix[i] += Int(ch[i]) }
                }
            }
        }
        if let tn = tone {
            if toneKey != lastToneKey { lastToneKey = toneKey; tpos = 0; ph1 = 0; ph2 = 0 }
            let spms = RATE / 1000
            let period = (tn.onMs + tn.offMs) * spms
            for i in 0..<FR {
                let inPeriod = period > 0 ? tpos % period : 0
                let on = tn.offMs == 0 || inPeriod < tn.onMs * spms
                tpos += 1
                if on {
                    var v = sin(ph1)
                    ph1 += 2 * .pi * tn.f1 / Double(RATE)
                    if ph1 > 2 * .pi { ph1 -= 2 * .pi }
                    if tn.f2 > 0 {
                        v += sin(ph2)
                        ph2 += 2 * .pi * tn.f2 / Double(RATE)
                        if ph2 > 2 * .pi { ph2 -= 2 * .pi }
                    }
                    mix[i] += Int(v * tn.amp * 32767)
                }
            }
        } else { lastToneKey = -1 }
        var keep = [Effect]()
        for e in effects {
            for i in 0..<FR where e.pos < e.d.count { mix[i] += Int(e.d[e.pos]); e.pos += 1 }
            if e.pos < e.d.count { keep.append(e) }
        }
        effects = keep
        let g = duckOn ? duck.step(paNow) : 1.0
        return (0..<FR).map { Float(max(-32768, min(32767, Int(Double(mix[$0]) * g) + pa[$0]))) / 32768 }
    }

    /// Called from the audio render thread.
    func pull(_ frames: Int, into p: UnsafeMutablePointer<Float>) {
        lock.lock(); defer { lock.unlock() }
        while pending.count < frames { pending.append(contentsOf: mixFrame()) }
        for i in 0..<frames { p[i] = pending[i] }
        pending.removeFirst(frames)
    }
}

/// Microphone: 20 ms frames with a noise gate (silence is not sent).
final class Mic {
    private let onFrame: (Data) -> Void
    private var acc = [Float]()
    private var hang = 0
    private(set) var running = false
    init(onFrame: @escaping (Data) -> Void) { self.onFrame = onFrame }

    func process(_ samples: [Float]) {
        acc.append(contentsOf: samples)
        while acc.count >= FR {
            let f = Array(acc[0..<FR]); acc.removeFirst(FR)
            var e = 0.0
            for v in f { let x = Double(v) * 32768; e += x * x }
            let rms = (e / Double(FR)).squareRoot()
            if rms > 150 { hang = 20 } else if hang > 0 { hang -= 1 }
            if hang > 0 {
                var out = [UInt8](); out.reserveCapacity(FR * 2)
                for v in f {
                    let u = UInt16(bitPattern: Int16(max(-32768, min(32767, Int(v * 32768)))))
                    out.append(UInt8(u & 0xFF)); out.append(UInt8(u >> 8))
                }
                onFrame(Data(out))
            }
        }
    }
    func set(running r: Bool) { running = r; if !r { acc.removeAll(); hang = 0 } }
}

/// One AVAudioEngine for everything: playout (8 kHz source node), ringer (looping player), microphone tap.
final class AudioIO {
    let playout = Playout()
    private let engine = AVAudioEngine()
    private var src: AVAudioSourceNode?
    private let ringNode = AVAudioPlayerNode()
    private var started = false
    private var converter: AVAudioConverter?
    private var micOn = false
    let mic: Mic
    private let lock = NSLock()

    init(onMicFrame: @escaping (Data) -> Void) {
        mic = Mic(onFrame: onMicFrame)
    }

    func configureSession(speaker: Bool) {
        let s = AVAudioSession.sharedInstance()
        try? s.setCategory(.playAndRecord, mode: .voiceChat, options: [.allowBluetooth, .allowBluetoothA2DP])
        try? s.setPreferredSampleRate(16000)
        try? s.setActive(true)
        setSpeaker(speaker)
    }

    func setSpeaker(_ on: Bool) {
        wantSpeaker = on
        try? AVAudioSession.sharedInstance().overrideOutputAudioPort(on ? .speaker : .none)
    }

    private var observers = [NSObjectProtocol]()
    private var wantSpeaker = false

    /// Keeps the show alive in the background: the audio session stays active (UIBackgroundModes=audio) and the engine is restarted
    /// after calls, Siri, route changes or a media-services reset.
    private func watchSession() {
        if !observers.isEmpty { return }
        let nc = NotificationCenter.default
        let s = AVAudioSession.sharedInstance()
        func revive() { DispatchQueue.main.async { [weak self] in self?.revive() } }
        observers.append(nc.addObserver(forName: AVAudioSession.interruptionNotification, object: s, queue: nil) { n in
            if let v = n.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt, AVAudioSession.InterruptionType(rawValue: v) == .ended { revive() }
        })
        observers.append(nc.addObserver(forName: AVAudioSession.mediaServicesWereResetNotification, object: s, queue: nil) { _ in revive() })
        observers.append(nc.addObserver(forName: .AVAudioEngineConfigurationChange, object: engine, queue: nil) { _ in revive() })
    }

    private func revive() {
        lock.lock(); defer { lock.unlock() }
        if !started { return }
        configureSession(speaker: wantSpeaker)
        if !engine.isRunning { try? engine.start() }
    }

    func start(speaker: Bool) {
        lock.lock(); defer { lock.unlock() }
        if started { return }
        wantSpeaker = speaker
        watchSession()
        configureSession(speaker: speaker)
        let fmt = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: Double(RATE), channels: 1, interleaved: false)!
        let po = playout
        let node = AVAudioSourceNode(format: fmt) { _, _, frames, abl -> OSStatus in
            let list = UnsafeMutableAudioBufferListPointer(abl)
            if let p = list[0].mData?.assumingMemoryBound(to: Float.self) { po.pull(Int(frames), into: p) }
            return noErr
        }
        src = node
        engine.attach(node)
        engine.connect(node, to: engine.mainMixerNode, format: fmt)
        let rfmt = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: 16000, channels: 1, interleaved: false)!
        engine.attach(ringNode)
        engine.connect(ringNode, to: engine.mainMixerNode, format: rfmt)
        engine.prepare()
        do { try engine.start(); started = true } catch { started = false }
    }

    func stop() {
        lock.lock(); defer { lock.unlock() }
        stopRinger()
        setMic(false)
        if started { engine.stop() }
        if let n = src { engine.detach(n); src = nil }
        engine.detach(ringNode)
        started = false
        for o in observers { NotificationCenter.default.removeObserver(o) }
        observers.removeAll()
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    // ---- microphone

    func setMic(_ on: Bool) {
        if on == micOn { return }
        micOn = on
        let input = engine.inputNode
        if on {
            let inFmt = input.outputFormat(forBus: 0)
            guard inFmt.sampleRate > 0, let target = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: Double(RATE), channels: 1, interleaved: false),
                  let conv = AVAudioConverter(from: inFmt, to: target) else { micOn = false; return }
            converter = conv
            mic.set(running: true)
            input.installTap(onBus: 0, bufferSize: 1024, format: inFmt) { [weak self] buf, _ in
                guard let self = self, let conv = self.converter else { return }
                let ratio = Double(RATE) / inFmt.sampleRate
                let cap = AVAudioFrameCount(Double(buf.frameLength) * ratio) + 16
                guard let out = AVAudioPCMBuffer(pcmFormat: target, frameCapacity: cap) else { return }
                var fed = false
                var err: NSError?
                conv.convert(to: out, error: &err) { _, status in
                    if fed { status.pointee = .noDataNow; return nil }
                    fed = true; status.pointee = .haveData; return buf
                }
                if err == nil, let ch = out.floatChannelData {
                    self.mic.process(Array(UnsafeBufferPointer(start: ch[0], count: Int(out.frameLength))))
                }
            }
        } else {
            input.removeTap(onBus: 0)
            mic.set(running: false)
            converter = nil
        }
    }

    // ---- ringer (looping, era-dependent timbre)

    func startRinger(era: Int, pat: Int) {
        stopRinger()
        let sr = 16000
        let data = Ringer.render(era: era, pat: pat, sr: sr)
        guard let fmt = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: Double(sr), channels: 1, interleaved: false),
              let buf = AVAudioPCMBuffer(pcmFormat: fmt, frameCapacity: AVAudioFrameCount(data.count)) else { return }
        buf.frameLength = AVAudioFrameCount(data.count)
        for i in 0..<data.count { buf.floatChannelData![0][i] = data[i] }
        if !started { return }
        ringNode.scheduleBuffer(buf, at: nil, options: .loops, completionHandler: nil)
        ringNode.play()
    }

    func stopRinger() { if ringNode.isPlaying { ringNode.stop() } }
}

enum Ringer {
    /// 5 s cycle. The cadence tells the two subscribers of a party line apart: pat 0 = one long ring, pat 1 = two short rings.
    /// Timbre depends on the era: mechanical bell (1980s), electronic trill (1990s), arpeggio (2000s).
    static func render(era: Int, pat: Int, sr: Int) -> [Float] {
        let total = 5 * sr
        var out = [Float](repeating: 0, count: total)
        let segs = pat == 1 ? [(0, 400), (600, 1000)] : [(0, 1300)]
        let notes = [523.25, 659.25, 783.99, 659.25]
        for (a, b) in segs {
            let s0 = a * sr / 1000, s1 = min(total, b * sr / 1000)
            if s0 >= s1 { continue }
            for i in s0..<s1 {
                let t = Double(i) / Double(sr), local = Double(i - s0) / Double(sr)
                let env = min(1.0, min(local / 0.01, Double(s1 - i) / (0.02 * Double(sr))))
                let v: Double
                if era < 1990 { v = (sin(2 * .pi * 1400 * t) * 0.5 + sin(2 * .pi * 2000 * t) * 0.3) * (0.55 + 0.45 * sin(2 * .pi * 22 * t)) }
                else if era < 2000 { v = sin(2 * .pi * ((Int(local * 20) % 2 == 0) ? 880.0 : 1175.0) * t) * 0.6 }
                else { v = sin(2 * .pi * notes[Int(local / 0.12) % 4] * t) * 0.6 }
                out[i] = Float(v * env * 0.95)
            }
        }
        return out
    }
}
