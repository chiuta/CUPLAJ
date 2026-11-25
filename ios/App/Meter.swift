import Foundation
import AVFoundation
import CoreMotion

/// Audience meter: measures noise or motion for a few seconds and reports ONLY a number 0-100 (no audio leaves the phone).
enum Meter {
    private static var recorder: AVAudioRecorder?
    private static let motionMgr = CMMotionManager()

    /// -60 dBFS -> 0, 0 dBFS -> 100.
    static func level(_ rms: Double) -> Int { Int(max(0, min(100, ((20 * log10(max(rms, 1e-6))) + 60) / 60 * 100))) }

    /// 0.05 m/s^2 of deviation ~ 0, 5 m/s^2 (shaken hard) ~ 100.
    static func motionLevel(_ sd: Double) -> Int { Int(max(0, min(100, (log10(max(sd, 0.05) / 0.05) / 2.0) * 100))) }

    static func measure(kind: String, secs: Int, done: @escaping (Int?) -> Void) {
        if kind == "noise" { noise(secs: secs, done: done) } else { motion(secs: secs, done: done) }
    }

    private static func noise(secs: Int, done: @escaping (Int?) -> Void) {
        AVAudioSession.sharedInstance().requestRecordPermission { ok in
            guard ok else { DispatchQueue.main.async { done(nil) }; return }
            DispatchQueue.main.async {
                let settings: [String: Any] = [AVFormatIDKey: kAudioFormatLinearPCM, AVSampleRateKey: 8000, AVNumberOfChannelsKey: 1, AVLinearPCMBitDepthKey: 16]
                guard let r = try? AVAudioRecorder(url: URL(fileURLWithPath: "/dev/null"), settings: settings) else { done(nil); return }
                r.isMeteringEnabled = true
                guard r.record() else { done(nil); return }
                recorder = r
                var sum = 0.0, n = 0
                let t = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { _ in
                    r.updateMeters()
                    sum += pow(10, Double(r.averagePower(forChannel: 0)) / 10)   // mean power (linear)
                    n += 1
                }
                DispatchQueue.main.asyncAfter(deadline: .now() + Double(secs)) {
                    t.invalidate(); r.stop(); recorder = nil
                    done(n == 0 ? nil : level((sum / Double(n)).squareRoot()))
                }
            }
        }
    }

    private static func motion(secs: Int, done: @escaping (Int?) -> Void) {
        guard motionMgr.isAccelerometerAvailable else { done(nil); return }
        var sum = 0.0, n = 0, mean = 9.81
        motionMgr.accelerometerUpdateInterval = 0.02
        motionMgr.startAccelerometerUpdates(to: .main) { d, _ in
            guard let a = d?.acceleration else { return }
            let m = (a.x * a.x + a.y * a.y + a.z * a.z).squareRoot() * 9.81
            mean += (m - mean) * 0.05
            let dev = m - mean
            sum += dev * dev; n += 1
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + Double(secs)) {
            motionMgr.stopAccelerometerUpdates()
            done(n == 0 ? nil : motionLevel((sum / Double(n)).squareRoot()))
        }
    }
}
