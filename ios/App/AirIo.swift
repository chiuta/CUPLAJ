import Foundation
import AVFoundation
import CoreMotion
import CuplajCore

/// iOS emitters and receivers for LUMEN, SONUS and TACTUS. The decoding itself is in CuplajCore (tested on macOS CI).
enum AirIo {
    static let FS = 48000

    // ------------------------------------------------------------------ transmit

    /// LUMEN: the torch follows the slots. Returns false when there is no torch.
    static func sendLight(_ packet: Data, done: @escaping () -> Void) -> Bool {
        guard let d = AVCaptureDevice.default(for: .video), d.hasTorch else { return false }
        let slots = Lumen.encode(packet)
        let t = Thread {
            let t0 = DispatchTime.now().uptimeNanoseconds
            var last = false
            func set(_ on: Bool) {
                if (try? d.lockForConfiguration()) != nil {
                    if on { try? d.setTorchModeOn(level: 1.0) } else { d.torchMode = .off }
                    d.unlockForConfiguration()
                }
            }
            for (k, on) in slots.enumerated() {
                let target = t0 + UInt64(Double(k) * Lumen.SLOT_MS * 1e6)
                while DispatchTime.now().uptimeNanoseconds < target { Thread.sleep(forTimeInterval: 0.001) }
                if on != last { last = on; set(on) }
            }
            set(false)
            done()
        }
        t.start()
        return true
    }

    private static var txEngine: AVAudioEngine?

    /// SONUS: plays the packet on the speaker (media volume must be up).
    static func sendSound(_ packet: Data, ultra: Bool, done: @escaping () -> Void) {
        let pcm = Sonus.encode(packet, fs: FS, ultra: ultra, amp: 0.6)
        let eng = AVAudioEngine(), node = AVAudioPlayerNode()
        guard let fmt = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: Double(FS), channels: 1, interleaved: false),
              let buf = AVAudioPCMBuffer(pcmFormat: fmt, frameCapacity: AVAudioFrameCount(pcm.count)) else { done(); return }
        buf.frameLength = AVAudioFrameCount(pcm.count)
        for i in 0..<pcm.count { buf.floatChannelData![0][i] = Float(pcm[i]) / 32768 }
        eng.attach(node)
        eng.connect(node, to: eng.mainMixerNode, format: fmt)
        txEngine = eng
        do { try eng.start() } catch { done(); return }
        node.scheduleBuffer(buf, completionHandler: nil)
        node.play()
        DispatchQueue.global().asyncAfter(deadline: .now() + Double(pcm.count) / Double(FS) + 0.4) {
            node.stop(); eng.stop(); txEngine = nil
            done()
        }
    }

    /// TACTUS: the haptic engine follows the burst durations. Returns the duration in ms (0 = unavailable).
    static func sendVibe(_ packet: Data, haptics: Haptics) -> Int {
        let d = Tactus.encode(packet)
        haptics.pattern([0] + d.map { Int64($0) }, repeatAll: false)
        return d.reduce(0, +)
    }

    // ------------------------------------------------------------------ receive

    /// LUMEN: back camera (low resolution) -> mean brightness of the centre of the frame -> decoder.
    final class LightRx: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {
        private let key: Data?
        private let onPacket: (AirPacket) -> Void
        private let session = AVCaptureSession()
        private var buf = [LSample]()
        private let lock = NSLock()
        private var run = false
        private let q = DispatchQueue(label: "cuplaj-air-cam")

        init(key: Data?, onPacket: @escaping (AirPacket) -> Void) { self.key = key; self.onPacket = onPacket }

        func start() -> Bool {
            guard AVCaptureDevice.authorizationStatus(for: .video) != .denied,
                  let dev = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) ?? AVCaptureDevice.default(for: .video),
                  let input = try? AVCaptureDeviceInput(device: dev) else { return false }
            session.beginConfiguration()
            session.sessionPreset = .low
            if session.canAddInput(input) { session.addInput(input) } else { session.commitConfiguration(); return false }
            let out = AVCaptureVideoDataOutput()
            out.videoSettings = [kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_420YpCbCr8BiPlanarFullRange]
            out.alwaysDiscardsLateVideoFrames = true
            out.setSampleBufferDelegate(self, queue: q)
            if session.canAddOutput(out) { session.addOutput(out) } else { session.commitConfiguration(); return false }
            session.commitConfiguration()
            run = true
            q.async { [session] in session.startRunning() }
            Thread { [self] in
                while isRun() {
                    Thread.sleep(forTimeInterval: 1)
                    lock.lock()
                    if let last = buf.last?.t { buf.removeAll { $0.t < last - 32000 } }
                    let win = buf
                    lock.unlock()
                    if let pk = Lumen.decode(win, key: key) { lock.lock(); buf.removeAll(); lock.unlock(); onPacket(pk) }
                }
            }.start()
            return true
        }

        private func isRun() -> Bool { lock.lock(); defer { lock.unlock() }; return run }

        func captureOutput(_ output: AVCaptureOutput, didOutput sb: CMSampleBuffer, from connection: AVCaptureConnection) {
            guard let pb = CMSampleBufferGetImageBuffer(sb) else { return }
            CVPixelBufferLockBaseAddress(pb, .readOnly); defer { CVPixelBufferUnlockBaseAddress(pb, .readOnly) }
            guard let base = CVPixelBufferGetBaseAddressOfPlane(pb, 0) else { return }
            let w = CVPixelBufferGetWidthOfPlane(pb, 0), h = CVPixelBufferGetHeightOfPlane(pb, 0), stride = CVPixelBufferGetBytesPerRowOfPlane(pb, 0)
            let p = base.assumingMemoryBound(to: UInt8.self)
            var sum = 0, n = 0, y = h / 4
            while y < h * 3 / 4 { var x = w / 4; while x < w * 3 / 4 { sum += Int(p[y * stride + x]); n += 1; x += 3 }; y += 3 }
            if n > 0 {
                let t = CMTimeGetSeconds(CMSampleBufferGetPresentationTimeStamp(sb)) * 1000
                lock.lock(); buf.append(LSample(t: t, v: Double(sum) / Double(n))); lock.unlock()
            }
        }

        func stop() { lock.lock(); run = false; lock.unlock(); q.async { [session] in session.stopRunning() } }
    }

    /// SONUS: microphone at 48 kHz -> 12 s window -> decoder.
    final class SoundRx {
        private let ultra: Bool
        private let key: Data?
        private let onPacket: (AirPacket) -> Void
        private let eng = AVAudioEngine()
        private let lock = NSLock()
        private var ring = [Int16](repeating: 0, count: FS * 12)
        private var pos = 0, filled = 0
        private var lastTry = nowMs()
        private var run = false

        init(ultra: Bool, key: Data?, onPacket: @escaping (AirPacket) -> Void) { self.ultra = ultra; self.key = key; self.onPacket = onPacket }

        func start() -> Bool {
            let input = eng.inputNode
            let inFmt = input.outputFormat(forBus: 0)
            guard inFmt.sampleRate > 0, let outFmt = AVAudioFormat(commonFormat: .pcmFormatInt16, sampleRate: Double(FS), channels: 1, interleaved: true),
                  let conv = AVAudioConverter(from: inFmt, to: outFmt) else { return false }
            run = true
            input.installTap(onBus: 0, bufferSize: 4800, format: inFmt) { [weak self] b, _ in
                guard let self = self else { return }
                let cap = AVAudioFrameCount(Double(b.frameLength) * Double(FS) / inFmt.sampleRate) + 16
                guard let ob = AVAudioPCMBuffer(pcmFormat: outFmt, frameCapacity: cap) else { return }
                var fed = false
                conv.convert(to: ob, error: nil) { _, st in if fed { st.pointee = .noDataNow; return nil }; fed = true; st.pointee = .haveData; return b }
                guard let ch = ob.int16ChannelData else { return }
                self.push(UnsafeBufferPointer(start: ch[0], count: Int(ob.frameLength)))
            }
            do { try eng.start() } catch { input.removeTap(onBus: 0); return false }
            return true
        }

        private func push(_ s: UnsafeBufferPointer<Int16>) {
            lock.lock()
            for v in s { ring[pos] = v; pos = (pos + 1) % ring.count }
            filled = min(ring.count, filled + s.count)
            var win: [Int16]?
            if nowMs() - lastTry > 2000 && filled > FS * 4 {
                lastTry = nowMs()
                let start = (pos - filled + ring.count) % ring.count
                win = (0..<filled).map { ring[(start + $0) % ring.count] }
            }
            lock.unlock()
            if let w = win {
                DispatchQueue.global().async { [self] in
                    if let pk = Sonus.decode(w, fs: FS, ultra: ultra, key: key) { lock.lock(); filled = 0; lock.unlock(); onPacket(pk) }
                }
            }
        }

        func stop() { if run { eng.inputNode.removeTap(onBus: 0); eng.stop(); run = false } }
    }

    /// TACTUS: accelerometer at its maximum rate -> burst decoder.
    final class VibeRx {
        private let key: Data?
        private let onPacket: (AirPacket) -> Void
        private let mm = CMMotionManager()
        private var buf = [ASample]()
        private let lock = NSLock()
        private var run = false

        init(key: Data?, onPacket: @escaping (AirPacket) -> Void) { self.key = key; self.onPacket = onPacket }

        func start() -> Bool {
            guard mm.isAccelerometerAvailable else { return false }
            run = true
            mm.accelerometerUpdateInterval = 0.01
            mm.startAccelerometerUpdates(to: OperationQueue()) { [weak self] d, _ in
                guard let self = self, let a = d?.acceleration else { return }
                let g = 9.81
                self.lock.lock()
                self.buf.append(ASample(t: d!.timestamp * 1000, x: a.x * g, y: a.y * g, z: a.z * g))
                if let last = self.buf.last?.t, let first = self.buf.first?.t, last - first > 40000 { self.buf.removeAll { $0.t < last - 40000 } }
                self.lock.unlock()
            }
            Thread { [self] in
                while isRun() {
                    Thread.sleep(forTimeInterval: 1)
                    lock.lock(); let win = buf; lock.unlock()
                    if let pk = Tactus.decode(win, key: key) { lock.lock(); buf.removeAll(); lock.unlock(); onPacket(pk) }
                }
            }.start()
            return true
        }

        private func isRun() -> Bool { lock.lock(); defer { lock.unlock() }; return run }
        func stop() { lock.lock(); run = false; lock.unlock(); mm.stopAccelerometerUpdates() }
    }
}
