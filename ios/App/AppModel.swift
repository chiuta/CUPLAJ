import Foundation
import SwiftUI
import AVFoundation
import UIKit
import CuplajCore

struct Msg: Identifiable { let id = UUID(); let from: String, num: String, text: String, mine: Bool, to: String }

struct PeerInfo: Identifiable {
    var id: String; var name: String; var role: String; var number: String; var group: String
    var line: Int; var slot: Int; var online: Bool; var busy: Bool; var mon: Bool; var muted: Bool; var bat: Int; var zone: Int; var fx: String
}

struct Step: Identifiable, Equatable {
    var id = UUID()
    var name: String; var to: String; var kind: String; var text: String; var delay: Int
}

struct Fake { let name: String, num: String, text: String }

let CUE_KINDS = ["text", "vibrate", "ring", "flash", "speaker", "contact", "era", "say", "sound", "blackout", "fakecall", "realcall", "panic", "color", "poll", "pollend", "image", "video", "clip", "caption", "dmx", "midi", "morse", "house"]

/// Everything the app does: the phone (calls, tones, ringing), cues received, and - on the Regie phone - the hub, script and consoles.
final class AppModel: ObservableObject {
    // ---- profile (persisted)
    @Published var name = ""
    @Published var role = "spectator"
    @Published var host = ""
    @Published var pin = ""
    @Published var room = ""
    @Published var joinKey = ""
    @Published var era = 1990 { didSet { UserDefaults.standard.set(era, forKey: "era") } }
    @Published var speaker = false
    @Published var lang = "en" { didSet { L.lang = lang; UserDefaults.standard.set(lang, forKey: "lang") } }
    @Published var scale: Double = 1.0 { didSet { UserDefaults.standard.set(scale, forKey: "scale") } }

    // ---- connection
    @Published var inSession = false
    @Published var connected = false
    @Published var status = ""
    @Published var number = ""
    @Published var line = 0
    @Published var slot = 0
    @Published var partnerNum = ""
    @Published var hubs: [HubInfo] = []
    @Published var toast: String?

    // ---- phone
    @Published var ph = "idle"
    @Published var dialed = ""
    @Published var ringNum = ""
    @Published var ringName = ""
    @Published var ringParty = false
    @Published var peerNumber = ""
    @Published var peerName = ""
    @Published var forNum = ""
    @Published var intervening = false
    @Published var pttMode = -1

    // ---- content
    @Published var msgs = [Msg]()
    @Published var log = [String]()
    @Published var peers = [PeerInfo]()
    @Published var optListen = true
    @Published var optClick = true
    @Published var optPeerMsg = true
    @Published var optP2p = 1
    @Published var pollCur: JSONObject?
    @Published var pollSummary = ""
    @Published var meterSummary = ""
    @Published var captionText = ""
    @Published var bigText: String?
    @Published var blackoutOn = false
    @Published var blackoutText = ""
    @Published var colorNow: ColorPlan?
    @Published var colorStamp = Date()
    @Published var mediaShow: (kind: String, url: URL, secs: Int)?
    @Published var chainCur: JSONObject?
    @Published var chainSummary = ""
    @Published var reportMd = ""
    @Published var contacts = [(String, String)]()
    @Published var p2pOn = false

    // ---- regie
    @Published var script = [Step]()
    @Published var cursor = 0
    @Published var playing = false
    @Published var rulesText = ""
    @Published var lastMediaId = ""
    @Published var oscOutHost = ""
    @Published var oscOutPort = 53000
    @Published var oscInOn = false
    @Published var oscInPort = 8473
    @Published var dmxHost = ""
    @Published var dmxOn = false
    @Published var encOn = true
    @Published var netKey = ""
    @Published var hubPin = ""
    @Published var duckOn = true { didSet { audio.playout.duckOn = duckOn } }
    @Published var invite = ""
    @Published var sipOn = false
    @Published var sipHost = ""
    @Published var sipPort = 5060
    @Published var sipUser = ""
    @Published var sipPass = ""
    @Published var sipDomain = ""
    @Published var sipInbound = "100"
    @Published var sipRegistered = false

    // ---- internals
    private var net: Net?
    private var sipGw: SipGateway?
    private var p2pLink: P2pLink?
    private var hub: Hub?
    private let bonjour = Bonjour()
    private let beacons = BeaconListener()
    private lazy var audio = AudioIO(onMicFrame: { [weak self] d in self?.micFrame(d) })
    private let haptics = Haptics()
    private let speech = Speech()
    private let clock = ClockSync()
    private var myFx: VoiceFx?
    private var myGroup = ""
    private var curTarget = ""
    private var fake: Fake?
    private var micMode = 0
    private var pending = [DispatchWorkItem]()
    private var statTimer: Timer?
    private var syncTimer: Timer?
    private var autoDialItem: DispatchWorkItem?
    private var fakeItem: DispatchWorkItem?
    private var scriptToken = 0
    private var nextStep = 0
    private var oscOut: OscOut?
    private var oscIn: OscIn?
    private var dmx: DmxOut?
    private var midi: MidiOut?
    private var clipPlayer: AVAudioPlayer?
    private lazy var media = MediaAsm(dir: FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0].appendingPathComponent("media"))
    private var deviceId = ""
    private let ud = UserDefaults.standard

    // ====================================================================== setup

    init() {
        L.load()
        let d = ud
        lang = d.string(forKey: "lang") ?? L.detect()
        L.lang = lang
        name = d.string(forKey: "name") ?? UIDevice.current.name
        role = d.string(forKey: "role") ?? "spectator"
        host = d.string(forKey: "host") ?? ""
        pin = d.string(forKey: "pin") ?? ""
        room = d.string(forKey: "room") ?? ""
        joinKey = d.string(forKey: "joinkey") ?? ""
        era = d.object(forKey: "era") as? Int ?? 1990
        speaker = d.bool(forKey: "speaker")
        scale = d.object(forKey: "scale") as? Double ?? 1.0
        rulesText = d.string(forKey: "rules") ?? ""
        oscOutHost = d.string(forKey: "oscoh") ?? ""
        oscOutPort = d.object(forKey: "oscop") as? Int ?? 53000
        oscInOn = d.bool(forKey: "oscin")
        oscInPort = d.object(forKey: "oscip") as? Int ?? 8473
        dmxHost = d.string(forKey: "dmxh") ?? ""
        dmxOn = d.bool(forKey: "dmxon")
        encOn = d.object(forKey: "encon") as? Bool ?? true
        netKey = d.string(forKey: "netkey") ?? ""
        if netKey.isEmpty { netKey = Sec.newPassphrase(); d.set(netKey, forKey: "netkey") }
        sipOn = d.bool(forKey: "sipon"); sipHost = d.string(forKey: "siph") ?? ""; sipPort = d.object(forKey: "sipp") as? Int ?? 5060
        sipUser = d.string(forKey: "sipu") ?? ""; sipPass = d.string(forKey: "sipw") ?? ""; sipDomain = d.string(forKey: "sipd") ?? ""
        sipInbound = d.string(forKey: "sipi") ?? "100"
        hubPin = d.string(forKey: "hubpin") ?? ""
        if hubPin.isEmpty { hubPin = Sec.pin(6); d.set(hubPin, forKey: "hubpin") }
        deviceId = d.string(forKey: "devid") ?? ""
        if deviceId.isEmpty { deviceId = UUID().uuidString.lowercased(); d.set(deviceId, forKey: "devid") }
        if let s = d.string(forKey: "script") { _ = loadScript(s) }
        loadContacts()
        beacons.onChange = { [weak self] l in self?.hubs = l }
    }

    func saveProfile() {
        ud.set(name, forKey: "name"); ud.set(role, forKey: "role"); ud.set(host, forKey: "host"); ud.set(pin, forKey: "pin")
        ud.set(room, forKey: "room"); ud.set(joinKey, forKey: "joinkey"); ud.set(speaker, forKey: "speaker")
        ud.set(encOn, forKey: "encon"); ud.set(netKey, forKey: "netkey")
    }

    func startDiscovery() { beacons.start() }
    func showToast(_ s: String) {
        toast = s
        DispatchQueue.main.asyncAfter(deadline: .now() + 3) { [weak self] in if self?.toast == s { self?.toast = nil } }
    }

    private func after(_ s: Double, _ f: @escaping () -> Void) -> DispatchWorkItem {
        let w = DispatchWorkItem(block: f)
        DispatchQueue.main.asyncAfter(deadline: .now() + s, execute: w)
        return w
    }

    // ====================================================================== session

    func startSession() {
        let n = name.trimmingCharacters(in: .whitespaces)
        if n.isEmpty { showToast(L.t("err_name")); return }
        if role != "regie" && host.trimmingCharacters(in: .whitespaces).isEmpty { showToast(L.t("err_host")); return }
        saveProfile()
        stopSession()
        UIApplication.shared.isIdleTimerDisabled = true
        Notif.ask()
        audio.playout.duckOn = duckOn
        audio.start(speaker: speaker)
        var target = host.trimmingCharacters(in: .whitespaces)
        let keyStr = role == "regie" ? (encOn ? netKey : "") : joinKey.trimmingCharacters(in: .whitespaces)
        if role == "regie" {
            let h = Hub(hubName: n, pin: hubPin, room: room, port: PORT, restore: ud.string(forKey: "hubstate"), trustLoopback: true, netKey: keyStr)
            h.onPersist = { [weak self] s in self?.ud.set(s, forKey: "hubstate") }
            h.start()
            hub = h
            bonjour.publish(name: n, port: PORT)
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in
                if let e = self?.hub?.error { self?.showToast(L.t("err_port") + " (" + e + ")") }
            }
            target = "127.0.0.1"
            loadDmx()
            startSip(keyStr)
            startOsc()
            invite = makeInvite(role: "spectator")
        }
        curTarget = target
        let tok = ud.string(forKey: "tok_" + target) ?? ""
        let nt = Net(onJson: { [weak self] m in DispatchQueue.main.async { self?.handle(m) } },
                     onAudio: { [weak self] _, d in self?.audio.playout.feed(src: Int(d[d.startIndex]), data: d) },
                     onState: { [weak self] code, arg in
                         DispatchQueue.main.async {
                             guard let s = self else { return }
                             if code == "connecting" { s.status = L.t("net_connecting", arg) }
                             else {
                                 s.status = L.t("net_lost"); s.connected = false; s.stopP2p(false)
                                 if s.ph != "idle" { s.resetPhone() }
                             }
                         }
                     })
        net = nt
        inSession = true
        nt.start(host: target, hello: jo(["t": "hello", "id": deviceId, "name": n, "role": role, "pin": role == "regie" ? hubPin : pin, "room": room, "tok": tok, "p2p": role != "regie", "smsgw": false]),
                 key: Sec.deriveKey(keyStr))
        UIDevice.current.isBatteryMonitoringEnabled = true
    }

    private func setupP2p(_ tok: String) {
        if role == "regie" || tok.isEmpty { return }
        if p2pLink == nil {
            p2pLink = P2pLink { [weak self] pcm in self?.audio.playout.feed(src: 255, data: Data([255]) + pcm) }
        }
        guard let l = p2pLink else { return }
        let (h, p) = parseHostPort(curTarget)
        DispatchQueue.global().async { [weak self] in
            l.registerAt(host: h, port: p + 2, id: self?.deviceId ?? "", tok: tok)
            let lan = l.lanCandidate(Hub.localAddresses())
            DispatchQueue.main.async { self?.send(jo(["t": "udp", "lan": lan])) }
        }
    }

    func stopP2p(_ close: Bool) {
        p2pOn = false
        p2pLink?.end()
        if close { p2pLink?.close(); p2pLink = nil }
    }

    func stopSession() {
        stopP2p(true)
        statTimer?.invalidate(); syncTimer?.invalidate()
        stopScript()
        net?.stop(); net = nil
        hub?.stop(); hub = nil
        bonjour.stop()
        stopOsc()
        stopSip()
        dmx?.close(); dmx = nil
        resetPhone()
        audio.stop()
        connected = false; status = ""; number = ""; line = 0; slot = 0
        blackoutOn = false; colorNow = nil; captionText = ""; bigText = nil; mediaShow = nil
        inSession = false
        UIApplication.shared.isIdleTimerDisabled = false
    }

    func send(_ o: JSONObject) { net?.send(o) }

    func makeInvite(role r: String) -> String {
        var i = Invite()
        i.role = r
        i.hosts = Hub.localAddresses().map { $0 + ":" + String(PORT) }
        i.key = encOn ? netKey : ""
        i.pin = r == "actor" ? hubPin : ""
        i.room = room
        i.hub = name
        return InviteLink.build(i)
    }

    func applyInvite(_ link: String) -> Bool {
        guard let i = InviteLink.parse(link) else { return false }
        role = i.role
        if let h = i.hosts.first { host = h }
        joinKey = i.key; pin = i.pin; room = i.room
        saveProfile()
        return true
    }

    // ====================================================================== messages from the hub

    private func handle(_ m: JSONObject) {
        switch m.optString("t") {
        case "welcome":
            number = m.optString("number")
            let fx = m.optString("fx")
            myFx = VoiceFx.KINDS.contains(fx) ? VoiceFx(fx) : nil
            if role == "regie" { _ = after(0.8) { [weak self] in self?.sendRules() } }
            myGroup = m.optString("group")
            connected = true
            status = L.t("connected_to", m.optString("hub"))
            let tk = m.optString("tok")
            if !tk.isEmpty { ud.set(tk, forKey: "tok_" + curTarget) }
            setupP2p(tk.isEmpty ? (ud.string(forKey: "tok_" + curTarget) ?? "") : tk)
            statTimer?.invalidate()
            statTimer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in self?.sendStat() }
            _ = after(1.5) { [weak self] in self?.sendStat() }
            syncTimer?.invalidate()
            for i in 0..<5 { _ = after(0.2 + Double(i) * 0.3) { [weak self] in self?.send(jo(["t": "sync", "c": nowMs()])) } }
            syncTimer = Timer.scheduledTimer(withTimeInterval: 60, repeats: true) { [weak self] _ in self?.send(jo(["t": "sync", "c": nowMs()])) }
        case "denied":
            let why = m.optString("why")
            let key = "denied_" + why
            status = L.t(key) == key ? L.t("denied_other") : L.t(key)
            net?.stop(); connected = false
        case "line":
            line = m.optInt("line"); slot = m.optInt("slot"); partnerNum = m.optString("partner")
        case "peers":
            peers = (m.optArray("list") ?? []).compactMap { $0 as? JSONObject }.map {
                PeerInfo(id: $0.optString("id"), name: $0.optString("name"), role: $0.optString("role"), number: $0.optString("number"), group: $0.optString("group"),
                         line: $0.optInt("line"), slot: $0.optInt("slot"), online: $0.optBool("online"), busy: $0.optBool("busy"), mon: $0.optBool("mon"),
                         muted: $0.optBool("muted"), bat: $0.optInt("bat", -1), zone: $0.optInt("zone"), fx: $0.optString("fx"))
            }
            optListen = m.optBool("listen", true); optClick = m.optBool("click", true); optPeerMsg = m.optBool("peerMsg", true); optP2p = m.optInt("p2p", 1)
        case "log":
            log.append(m.optString("line")); if log.count > 500 { log.removeFirst() }
        case "msg":
            msgs.append(Msg(from: m.optString("from"), num: m.optString("num"), text: m.optString("text"), mine: false, to: m.optString("to")))
            audio.playout.effect(Fx.beep(1000, 90)); haptics.buzz(ms: 200)
        case "cue": cueTimed(m)
        case "p2p":
            if let l = p2pLink, case let k = m.optString("key"), k.count == 32, let kd = unhex(k), let arr = m.optArray("cand") {
                l.begin(key: kd, candidates: arr.compactMap { $0 as? String }, initiator: m.optBool("init"))
                for d in [1.5, 3.5] { _ = after(d) { [weak self] in self?.p2pOn = l.up } }
            }
        case "p2pstop": stopP2p(false)
        case "pong": break
        case "sync": clock.add(t0: m.optLong("c"), hub: m.optLong("s"), t1: nowMs())
        case "cueack": showToast(L.t("cue_sent_n", m.optInt("sent"), m.optInt("online")))
        case "poll":
            pollCur = m
            audio.playout.effect(Fx.beep(900, 120)); haptics.buzz(ms: 300)
        case "pollres": pollResult(m)
        case "media":
            switch media.add(m) {
            case "done": showToast(L.t("media_ready"))
            case "bad_hash", "overflow", "bad_meta", "bad_data": showToast(L.t("media_bad"))
            default: break
            }
        case "fx":
            let k = m.optString("k")
            myFx = VoiceFx.KINDS.contains(k) ? VoiceFx(k) : nil
        case "goto": gotoStep(m.optString("step"))
        case "meterreq":
            let id = m.optString("id"), kind = m.optString("kind"), secs = min(max(m.optInt("secs", 5), 1), 30)
            Meter.measure(kind: kind, secs: secs) { [weak self] v in
                if let v = v { self?.send(jo(["t": "meterval", "id": id, "v": v])) }
            }
        case "meterres":
            if !m.optBool("open", true) {
                meterSummary = L.t("meter_result", L.t("meter_" + m.optString("kind")), m.optInt("avg"), m.optInt("max"), m.optInt("n"), m.optInt("of"))
                log.append(meterSummary)
                showToast(meterSummary)
            }
        case "rulesok": showToast(L.t("rules_saved", m.optInt("n")))
        case "chainmsg":
            chainCur = m
            audio.playout.effect(Fx.beep(700, 120)); haptics.buzz(ms: 300)
        case "chainres":
            var s = L.t("chain_result_h") + "\n" + m.optString("orig")
            for h in m.optArray("hops") ?? [] { s += "\n→ " + ((h as? String) ?? "") }
            chainSummary = s
            log.append(s.replacingOccurrences(of: "\n", with: " "))
        case "guessres": showToast(L.t(m.optBool("ok") ? "guess_ok" : "guess_no"))
        case "secretres":
            chainSummary = L.t("secret_solved", m.optString("by"), m.optString("phrase"))
            log.append(chainSummary)
        case "reportres": reportMd = m.optString("md")
        case "smsres": showToast(m.optBool("ok") ? L.t("sms_sent") : L.t("sms_err_" + m.optString("why")))
        case "dialtone": if dialed.isEmpty { enter("dialtone") }
        case "ringback": enter("ringback")
        case "busy": enter("busy")
        case "unknown": enter("unknown")
        case "listening": intervening = false; enter("listening")
        case "ring":
            if ph == "idle" || ph == "busy" || ph == "unknown" {
                ringNum = m.optString("num"); ringName = m.optString("name"); ringParty = m.optBool("party")
                fake = nil
                startRinging(m.optInt("pat"))
            }
        case "stopring": if ph == "ringing" && fake == nil { enter("idle") }
        case "connected":
            peerNumber = m.optString("peer"); peerName = m.optString("name"); forNum = m.optString("forNum")
            dialed = ""; intervening = false
            enter("talking")
        case "ended":
            audio.playout.clearVoice()
            dialed = ""; intervening = false
            let why = m.optString("why")
            enter((why == "rejected" || why == "noanswer") && ph == "ringback" ? "busy" : "idle")
        case "click": audio.playout.effect(Fx.click(0.35))
        default: break
        }
    }

    private func sendStat() {
        let lvl = UIDevice.current.batteryLevel
        send(jo(["t": "stat", "bat": lvl < 0 ? -1 : Int(lvl * 100)]))
    }

    // ====================================================================== phone

    func fmtNum(_ n: String) -> String { n }

    func enter(_ s: String) {
        ph = s
        switch s {
        case "dialtone": audio.playout.setTone(Tones.dial)
        case "ringback": audio.playout.setTone(Tones.ringback)
        case "busy": audio.playout.setTone(Tones.busy)
        case "unknown": audio.playout.setTone(Tones.unknown)
        default: audio.playout.setTone(nil)
        }
        if s != "ringing" {
            audio.stopRinger(); haptics.cancel(); Notif.cancel()
            audio.setSpeaker(speaker)
        }
        if s == "idle" { fake = nil; speech.stop() }
        updateMic()
    }

    func resetPhone() {
        autoDialItem?.cancel(); fakeItem?.cancel()
        ph = "idle"; fake = nil; dialed = ""; intervening = false; pttMode = -1
        audio.playout.setTone(nil)
        audio.stopRinger(); haptics.cancel(); speech.stop(); Notif.cancel()
        audio.setMic(false)
        audio.playout.clearVoice()
    }

    private func startRinging(_ pat: Int) {
        enter("ringing")
        audio.setSpeaker(true)
        audio.startRinger(era: era, pat: pat)
        haptics.pattern(pat == 1 ? [0, 400, 200, 400, 4000] : [0, 1300, 3700], repeatAll: true)
        Notif.ring(title: L.t("notif_call"), who: ringParty || era < 1990 ? L.t("ring_plain") : (ringName.isEmpty ? ringNum : ringName))
    }

    private func updateMic() {
        let want: Int
        if pttMode > 0 { want = pttMode }
        else if fake == nil && (ph == "talking" || (ph == "listening" && intervening)) { want = 0 }
        else { want = -1 }
        if want < 0 { audio.setMic(false); return }
        micMode = want
        AVAudioSession.sharedInstance().requestRecordPermission { [weak self] ok in
            DispatchQueue.main.async {
                guard let s = self else { return }
                if ok { s.audio.setMic(true) } else { s.showToast(L.t("err_mic")) }
            }
        }
    }

    private func micFrame(_ d: Data) {
        let out = myFx?.processBytes(d) ?? d
        if micMode == 0, p2pOn, let l = p2pLink, l.sendPcm(out) { return }
        net?.sendAudio(mode: micMode, pcm: out)
    }

    func ptt(_ mode: Int, down: Bool) { pttMode = down ? mode : -1; updateMic() }

    /// The receiver: answers if ringing, lifts if on the hook, otherwise hangs up.
    func lift() {
        if ph == "ringing", let f = fake {
            fakeItem?.cancel()
            send(jo(["t": "offhook"]))
            peerNumber = f.num; peerName = f.name; forNum = ""
            enter("talking")
            if !f.text.isEmpty { speech.say(f.text, lang: lang) }
            return
        }
        if !connected { showToast(L.t("err_conn")); return }
        switch ph {
        case "ringing": send(jo(["t": "answer"]))
        case "idle": send(jo(["t": "offhook"]))
        default: hang()
        }
    }

    func hang() {
        autoDialItem?.cancel(); fakeItem?.cancel()
        send(jo(["t": "onhook"]))
        dialed = ""; intervening = false
        audio.playout.clearVoice()
        enter("idle")
    }

    func digit(_ ch: Character) {
        let ok = ph == "dialtone" || ph == "dialing" || (era >= 2000 && ph == "idle")
        if !ok || dialed.count >= 12 { return }
        if ph == "dialtone" { audio.playout.setTone(nil); ph = "dialing" }
        dialed.append(ch)
        if era >= 1990 && era <= 1999 { audio.playout.effect(Fx.dtmf(ch)) }
        else if era >= 2000 { audio.playout.effect(Fx.beep(800, 40)) }
        else { audio.playout.effect(Fx.pulses(ch == "0" ? 10 : Int(String(ch)) ?? 1)) }
        autoDialItem?.cancel()
        if era < 2000 { autoDialItem = after(3.5) { [weak self] in if self?.ph == "dialing" { self?.placeCall() } } }
    }

    func backspace() { if !dialed.isEmpty && (ph == "dialing" || ph == "idle") { dialed.removeLast() } }

    func placeCall() {
        autoDialItem?.cancel()
        let n = dialed
        if n.isEmpty { return }
        if !connected { showToast(L.t("err_conn")); return }
        if ph == "idle" { send(jo(["t": "offhook"])) }
        else if ph != "dialtone" && ph != "dialing" { return }
        send(jo(["t": "dial", "number": n]))
        audio.playout.setTone(nil)
        ph = "dialing"
    }

    func callNumber(_ n: String) {
        if ph == "idle" || ph == "dialtone" || ph == "dialing" { dialed = n; placeCall() }
        else { showToast(L.t("err_busy_line")) }
    }

    func toggleIntervene() {
        intervening.toggle()
        send(jo(["t": "intervene", "on": intervening]))
        updateMic()
    }

    func changeSpeaker(_ on: Bool) { speaker = on; audio.setSpeaker(on); saveProfile() }

    // ====================================================================== messages, contacts

    func sendMsg(to: String, text: String) {
        if text.trimmingCharacters(in: .whitespaces).isEmpty { return }
        send(jo(["t": "msg", "to": to, "text": text]))
        msgs.append(Msg(from: name, num: number, text: text, mine: true, to: to))
    }

    func loadContacts() {
        let a = ud.stringArray(forKey: "contacts") ?? []
        contacts = a.compactMap { s in let p = s.components(separatedBy: "\t"); return p.count == 2 ? (p[0], p[1]) : nil }
    }
    func addContact(_ n: String, _ num: String) {
        if n.trimmingCharacters(in: .whitespaces).isEmpty || num.isEmpty { return }
        contacts.removeAll { $0.1 == num }
        contacts.append((n, num))
        ud.set(contacts.map { $0.0 + "\t" + $0.1 }, forKey: "contacts")
    }

    // ====================================================================== cues

    private func cueTimed(_ m: JSONObject) {
        let at = m.optLong("at", 0)
        if at <= 0 { cue(m); return }
        guard let d = clock.delayFor(at: at, local: nowMs()) else { return }
        if d == 0 { cue(m); return }
        var item: DispatchWorkItem!
        item = after(Double(d) / 1000) { [weak self] in self?.pending.removeAll { $0 === item }; self?.cue(m) }
        pending.append(item)
    }

    func panic() {
        for p in pending { p.cancel() }
        pending.removeAll()
        audio.stopRinger(); haptics.cancel(); speech.stop()
        audio.playout.clearEffects()
        captionText = ""; blackoutOn = false; colorNow = nil; mediaShow = nil; bigText = nil
        clipPlayer?.stop(); clipPlayer = nil
        if ph == "ringing" { enter("idle") }
    }

    private func cue(_ m: JSONObject) {
        let text = expandTokens(pickLang(m.optString("text"), lang), name: name, number: number)
        switch m.optString("kind") {
        case "caption":
            captionText = text
            let t = text
            _ = after(min(3 + Double(t.count) * 0.07, 20)) { [weak self] in if self?.captionText == t { self?.captionText = "" } }
        case "image", "video", "clip": mediaPlay(m.optString("kind"), text)
        case "morse": haptics.pattern(Morse.pattern(text), repeatAll: false)
        case "house":
            panic()
            haptics.pattern([0, 600, 200, 600, 200, 600], repeatAll: false)
            colorNow = colorPlan("#ffffff;300"); colorStamp = Date()
            captionText = text.trimmingCharacters(in: .whitespaces).isEmpty ? L.t("house_default") : text
        case "panic": panic()
        case "color":
            let cp = colorPlan(text)
            colorNow = cp; colorStamp = Date()
            if let cp = cp { let st = colorStamp; _ = after(Double(cp.ms) / 1000) { [weak self] in if self?.colorStamp == st { self?.colorNow = nil } } }
        case "text":
            msgs.append(Msg(from: L.t("role_regie"), num: m.optString("from"), text: text, mine: false, to: "all"))
            haptics.buzz(ms: 500)
            bigText = text
            _ = after(6) { [weak self] in if self?.bigText == text { self?.bigText = nil } }
        case "vibrate": haptics.buzz(ms: Int64(text) ?? 800)
        case "ring":
            audio.setSpeaker(true); audio.startRinger(era: era, pat: 0)
            haptics.pattern([0, 1300, 3700], repeatAll: true)
            _ = after(8) { [weak self] in
                if self?.ph != "ringing" { self?.audio.stopRinger(); self?.haptics.cancel(); self?.audio.setSpeaker(self?.speaker ?? false) }
            }
        case "flash": Torch.flash(ms: Int(text) ?? 1500)
        case "speaker": changeSpeaker(text == "1")
        case "era": if let e = Int(text) { era = e }
        case "contact":
            if let i = text.firstIndex(of: ";") {
                addContact(String(text[..<i]).trimmingCharacters(in: .whitespaces), String(text[text.index(after: i)...].filter { $0.isASCII && $0.isNumber }))
            }
        case "say": speech.say(text, lang: lang)
        case "realcall":
            // iOS asks the person before dialling; it cannot place the call silently.
            let num = safeNumber(text.trimmingCharacters(in: .whitespaces))
            if !num.isEmpty, let u = URL(string: "tel://" + num) { UIApplication.shared.open(u) }
        case "sound": audio.playout.effect(Fx.preset(text.trimmingCharacters(in: .whitespaces)))
        case "blackout":
            let i = text.firstIndex(of: ";")
            blackoutOn = (i.map { String(text[..<$0]) } ?? text).trimmingCharacters(in: .whitespaces) == "1"
            blackoutText = i.map { String(text[text.index(after: $0)...]) } ?? ""
        case "fakecall":
            if ph != "idle" { return }
            let p = text.components(separatedBy: ";")
            let nm = (p.first ?? "?").trimmingCharacters(in: .whitespaces)
            let num = String((p.count > 1 ? p[1] : "").filter { $0.isASCII && $0.isNumber })
            let say = p.dropFirst(2).joined(separator: ";").trimmingCharacters(in: .whitespaces)
            fake = Fake(name: nm, num: num, text: say)
            ringNum = num; ringName = nm; ringParty = false
            startRinging(0)
            fakeItem?.cancel()
            fakeItem = after(30) { [weak self] in if self?.ph == "ringing" && self?.fake != nil { self?.fake = nil; self?.enter("idle") } }
        default: break
        }
    }

    private func mediaPlay(_ kind: String, _ text: String) {
        let p = text.components(separatedBy: ";")
        guard let f = media.file(p[0].trimmingCharacters(in: .whitespaces), kind) else { return }
        let secs = min(max(p.count > 1 ? (Int(p[1].trimmingCharacters(in: .whitespaces)) ?? 0) : 0, 0), 3600)
        if kind == "clip" {
            clipPlayer?.stop()
            if let pl = try? AVAudioPlayer(contentsOf: f) {
                clipPlayer = pl; pl.play()
                if secs > 0 { _ = after(Double(secs)) { [weak self] in if self?.clipPlayer === pl { pl.stop(); self?.clipPlayer = nil } } }
            }
            return
        }
        mediaShow = (kind, f, secs)
        if secs > 0 { let u = f; _ = after(Double(secs)) { [weak self] in if self?.mediaShow?.url == u { self?.mediaShow = nil } } }
    }

    private func pollResult(_ m: JSONObject) {
        let opts = (m.optArray("opts") ?? []).map { ($0 as? String) ?? "" }
        let counts = (m.optArray("counts") ?? []).map { ($0 as? NSNumber)?.intValue ?? 0 }
        var s = m.optString("q") + "\n"
        for (i, o) in opts.enumerated() { s += o + ": " + String(i < counts.count ? counts[i] : 0) + "\n" }
        s += "\(m.optInt("n"))/\(m.optInt("of"))"
        pollSummary = s
        if !m.optBool("open", true) { log.append(s.replacingOccurrences(of: "\n", with: " · ")) }
    }

    func vote(_ opt: Int) { if let id = pollCur?.optString("id") { send(jo(["t": "vote", "id": id, "o": opt])); pollCur = nil } }

    // ====================================================================== Regie: sending cues

    func validTarget(_ t: String) -> Bool { Rules.validTargetPublic(t) }

    func sendCue(to: String, kind: String, text: String) {
        if kind == "poll" {
            let p = text.components(separatedBy: ";").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
            if p.count >= 3 { send(jo(["t": "poll", "to": to, "q": p[0], "opts": Array(p.dropFirst().prefix(6))])) }
            return
        }
        if kind == "pollend" { send(jo(["t": "pollend"])); return }
        let (delay, body1) = parseAt(text)
        var wave = 0
        var body0 = body1
        if body1.hasPrefix("~"), let semi = body1.firstIndex(of: ";"), let w = Int(body1[body1.index(after: body1.startIndex)..<semi]), body1[body1.index(after: body1.startIndex)..<semi].count <= 4 {
            wave = w; body0 = String(body1[body1.index(after: semi)...])
        }
        if kind == "dmx" || kind == "midi" {
            var ok = false
            if kind == "dmx" { if let c = DmxCmd.parse(body0), let d = dmx { d.go(c); ok = true } }
            else if let b = Midi.encode(body0) { if midi == nil { midi = MidiOut() }; ok = midi?.send(b) ?? false }
            showToast(L.t(ok ? "out_ok" : "out_fail"))
            return
        }
        let body = (Media.KINDS[kind] != nil && body0.trimmingCharacters(in: .whitespaces).isEmpty) ? lastMediaId : body0
        oscOut?.send(Osc.encode("/cuplaj/cue", [.str(to), .str(kind), .str(body)]))
        var o = jo(["t": "cue", "to": to, "kind": kind, "text": body, "id": String(UInt32.random(in: 0..<0xFFFFFF), radix: 16)])
        if delay > 0 { o["at"] = clock.hubNow(nowMs()) + delay }
        if wave > 0 { o["wave"] = wave }
        send(o)
        if kind == "panic" { panic() }
    }

    // ====================================================================== script

    func scriptJson() -> String {
        let a: [Any] = script.map { jo(["name": $0.name, "to": $0.to, "kind": $0.kind, "text": $0.text, "delay": $0.delay]) }
        let d = (try? JSONSerialization.data(withJSONObject: a, options: [.prettyPrinted])) ?? Data("[]".utf8)
        return String(data: d, encoding: .utf8) ?? "[]"
    }

    func loadScript(_ s: String) -> Bool {
        guard let a = parseJSONArray(s) else { return false }
        script = a.compactMap { $0 as? JSONObject }.map { Step(name: $0.optString("name"), to: $0.optString("to", "all"), kind: $0.optString("kind", "text"), text: $0.optString("text"), delay: $0.optInt("delay")) }
        cursor = 0
        return true
    }

    func saveScript() { ud.set(scriptJson(), forKey: "script") }
    func importScript(_ s: String) -> Bool { let ok = loadScript(s); if ok { saveScript() }; return ok }
    func addStep(_ s: Step) { script.append(s); saveScript() }
    func removeStep(_ i: Int) { if script.indices.contains(i) { script.remove(at: i); cursor = min(cursor, script.count); saveScript() } }
    func moveStep(_ i: Int, _ d: Int) {
        let j = i + d
        if script.indices.contains(i) && script.indices.contains(j) { script.swapAt(i, j); saveScript() }
    }

    func runStep(_ s: Step) {
        if s.kind == "msg" { sendMsg(to: s.to, text: s.text) }
        else {
            sendCue(to: s.to, kind: s.kind, text: s.text)
            if s.kind == "era" && s.to == "all", let e = Int(s.text) { era = e }
        }
    }

    func goNext() { if script.indices.contains(cursor) { runStep(script[cursor]); cursor += 1 } }

    private func gotoStep(_ s: String) {
        let t = s.trimmingCharacters(in: .whitespaces)
        let i = (Int(t).map { $0 - 1 }) ?? script.firstIndex { $0.name.caseInsensitiveCompare(t) == .orderedSame }
        if let i = i, script.indices.contains(i) { playFrom(i) }
    }

    func playFrom(_ i: Int) {
        if !script.indices.contains(i) { return }
        playing = true
        scriptToken += 1
        stepTimer(i, scriptToken)
    }

    private func stepTimer(_ i: Int, _ tok: Int) {
        if tok != scriptToken || !playing || !script.indices.contains(i) { playing = false; return }
        runStep(script[i])
        cursor = i + 1
        if cursor >= script.count { playing = false; return }
        nextStep = cursor
        let d = Double(script[cursor].delay)
        DispatchQueue.main.asyncAfter(deadline: .now() + d) { [weak self] in self?.stepTimer(self?.nextStep ?? 0, tok) }
    }

    func stopScript() { playing = false; scriptToken += 1 }

    // ====================================================================== Regie: consoles

    func sendRules() {
        if role != "regie" || net == nil { return }
        send(jo(["t": "rules", "list": Rules.toJson(Rules.parse(rulesText))]))
    }
    func saveRules() { ud.set(rulesText, forKey: "rules"); sendRules() }
    func sendMeter(_ kind: String, to: String) { send(jo(["t": "meter", "kind": kind, "secs": 5, "to": to])) }
    func startChain(_ text: String, _ nums: [String]) { send(jo(["t": "chain", "text": text, "nums": nums])) }
    func startSecret(_ phrase: String, to: String) { send(jo(["t": "secret", "phrase": phrase, "to": to])) }
    func guess(_ text: String) { send(jo(["t": "guess", "text": String(text.prefix(120))])) }
    func chainForward(_ text: String) {
        guard let id = chainCur?.optString("id") else { return }
        send(jo(["t": "chainfwd", "id": id, "text": String(text.prefix(200))]))
        chainCur = nil
    }
    func requestReport() { send(jo(["t": "report"])) }
    func tape(_ a: String, id: String = "", to: String = "all", secs: Int = 60) { send(jo(["t": "tape", "a": a, "id": id, "to": to, "secs": secs])) }
    func couple(_ a: String, _ b: String) { send(jo(["t": "couple", "a": a, "b": b])) }
    func uncouple(_ id: String) { send(jo(["t": "uncouple", "id": id])) }
    func autoCouple() { send(jo(["t": "autocouple"])) }
    func setNum(_ id: String, _ n: String) { send(jo(["t": "setnum", "id": id, "number": n])) }
    func setGroup(_ id: String, _ g: String) { send(jo(["t": "setgroup", "id": id, "group": g])) }
    func mute(_ id: String, _ on: Bool) { send(jo(["t": "mute", "id": id, "on": on])) }
    func forget(_ id: String) { send(jo(["t": "forget", "id": id])) }
    func monitor(_ number: String) { send(jo(["t": "monitor", "number": number])) }
    func setFx(_ id: String, _ k: String) { send(jo(["t": "setfx", "id": id, "k": k])) }
    func sendOpts() { send(jo(["t": "opt", "listen": optListen, "click": optClick, "peerMsg": optPeerMsg, "p2p": optP2p])) }

    func pushMedia(kind: String, data: Data, to: [String]) {
        if Media.KINDS[kind] == nil || data.isEmpty || data.count > Media.MAX_BYTES { showToast(L.t("media_big")); return }
        let id = String(UInt64.random(in: 0..<(1 << 40)), radix: 36).prefix(12).lowercased()
        let sha = Sec.sha256Hex(data)
        showToast(L.t("media_sending"))
        DispatchQueue.global().async { [weak self] in
            let ch = Media.chunks(data)
            for t in to {
                for (i, n, b) in ch {
                    var o = jo(["t": "media", "to": t, "mid": String(id), "kind": kind, "i": i, "n": n, "b64": b])
                    if i == 0 { o["size"] = data.count; o["sha"] = sha }
                    self?.send(o)
                    Thread.sleep(forTimeInterval: 0.012)
                }
            }
            DispatchQueue.main.async { self?.lastMediaId = String(id); self?.showToast(L.t("media_sent", String(id))) }
        }
    }

    // ====================================================================== OSC, DMX settings

    func saveOsc() {
        ud.set(oscOutHost, forKey: "oscoh"); ud.set(oscOutPort, forKey: "oscop"); ud.set(oscInOn, forKey: "oscin"); ud.set(oscInPort, forKey: "oscip")
        if inSession && role == "regie" { startOsc() }
    }

    private func startOsc() {
        stopOsc()
        if !oscOutHost.trimmingCharacters(in: .whitespaces).isEmpty { oscOut = OscOut(host: oscOutHost.trimmingCharacters(in: .whitespaces), port: oscOutPort) }
        if oscInOn {
            let i = OscIn(port: oscInPort, pin: { [weak self] in self?.hubPin ?? "" }, onCmd: { [weak self] c in DispatchQueue.main.async { self?.oscCmd(c) } })
            if i.start() { oscIn = i } else { showToast(L.t("err_port")) }
        }
    }

    private func stopOsc() { oscOut?.close(); oscOut = nil; oscIn?.stop(); oscIn = nil }

    private func oscCmd(_ c: Osc.Cmd) {
        if role != "regie" || net == nil { return }
        switch c.kind {
        case "go": goNext()
        case "stop": stopScript()
        case "panic": sendCue(to: "all", kind: "panic", text: "")
        case "cue": if validTarget(c.to) { sendCue(to: c.to, kind: c.cue, text: c.text) }
        default: break
        }
    }

    func saveSip() {
        ud.set(sipOn, forKey: "sipon"); ud.set(sipHost, forKey: "siph"); ud.set(sipPort, forKey: "sipp"); ud.set(sipUser, forKey: "sipu")
        ud.set(sipPass, forKey: "sipw"); ud.set(sipDomain, forKey: "sipd"); ud.set(sipInbound, forKey: "sipi")
        if inSession && role == "regie" { stopSip(); startSip(encOn ? netKey : "") }
    }

    private func startSip(_ keyStr: String) {
        if !sipOn || sipHost.trimmingCharacters(in: .whitespaces).isEmpty { return }
        let cfg = SipConfig(server: sipHost.trimmingCharacters(in: .whitespaces), port: sipPort, user: sipUser.trimmingCharacters(in: .whitespaces), pass: sipPass,
                            domain: sipDomain.trimmingCharacters(in: .whitespaces), inbound: sipInbound.trimmingCharacters(in: .whitespaces).isEmpty ? "100" : sipInbound.trimmingCharacters(in: .whitespaces),
                            hubPort: PORT, hubPin: hubPin, hubKey: keyStr, hubRoom: room)
        let g = SipGateway(cfg, log: { [weak self] s in DispatchQueue.main.async { self?.log.append("SIP: " + s) } })
        if !g.start() { showToast(L.t("err_sip")); return }
        sipGw = g
        // show the registration state after a few seconds
        _ = after(4) { [weak self] in self?.sipRegistered = self?.sipGw?.registered ?? false }
    }

    private func stopSip() { sipGw?.stop(); sipGw = nil; sipRegistered = false }

    func saveDmx() {
        ud.set(dmxHost, forKey: "dmxh"); ud.set(dmxOn, forKey: "dmxon")
        dmx?.close(); dmx = nil
        if dmxOn { dmx = DmxOut(host: dmxHost) }
    }
    private func loadDmx() { if dmxOn && dmx == nil { dmx = DmxOut(host: dmxHost) } }
}
