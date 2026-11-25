import SwiftUI
import UniformTypeIdentifiers
import CuplajCore

struct MainView: View {
    @EnvironmentObject var m: AppModel
    @State private var sel = 0
    var body: some View {
        TabView(selection: $sel) {
            PhoneTab().tabItem { Label(L.t("tab_phone"), systemImage: "phone.fill") }.tag(0)
            MsgsTab().tabItem { Label(L.t("tab_msgs"), systemImage: "message.fill") }.tag(1)
            VoiceTab().tabItem { Label(L.t("tab_voice"), systemImage: "mic.fill") }.tag(2)
            if m.role == "regie" {
                RegieTab().tabItem { Label(L.t("tab_regie"), systemImage: "slider.horizontal.3") }.tag(3)
                ScriptTab().tabItem { Label(L.t("tab_script"), systemImage: "list.number") }.tag(4)
            }
            SettingsTab().tabItem { Label(L.or("tab_settings", "⚙︎"), systemImage: "gearshape") }.tag(5)
        }
        .accentColor(gold)
        .sheet(item: Binding(get: { m.pollCur.map { PollBox(m: $0) } }, set: { if $0 == nil { m.pollCur = nil } })) { PollSheet(box: $0) }
        .sheet(item: Binding(get: { m.chainCur.map { PollBox(m: $0) } }, set: { if $0 == nil { m.chainCur = nil } })) { ChainSheet(box: $0) }
    }
}

struct PollBox: Identifiable { let id = UUID(); let m: JSONObject }

struct PollSheet: View {
    @EnvironmentObject var m: AppModel
    let box: PollBox
    var body: some View {
        VStack(spacing: 14) {
            Text(box.m.optString("q")).font(.title2).multilineTextAlignment(.center).padding()
            ForEach(Array((box.m.optArray("opts") ?? []).enumerated()), id: \.offset) { i, o in
                Button((o as? String) ?? "") { m.vote(i) }.frame(maxWidth: .infinity).padding().background(curtain).foregroundColor(.white).cornerRadius(10)
            }
        }.padding()
    }
}

struct ChainSheet: View {
    @EnvironmentObject var m: AppModel
    let box: PollBox
    @State private var text = ""
    var body: some View {
        VStack(spacing: 14) {
            Text(L.t("chain_title")).font(.headline)
            Text(box.m.optString("text")).font(.title2).padding().background(Color(white: 0.15)).cornerRadius(10)
            TextField(L.t("chain_text"), text: $text).textFieldStyle(.roundedBorder)
            Button(L.t("chain_send")) { m.chainForward(text.isEmpty ? box.m.optString("text") : text) }.padding().background(curtain).foregroundColor(.white).cornerRadius(10)
        }.padding()
    }
}

// ====================================================================== phone

struct PhoneTab: View {
    @EnvironmentObject var m: AppModel
    private let keys: [[Character]] = [["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"], ["*", "0", "#"]]

    var phaseText: String {
        switch m.ph {
        case "idle": return m.line == 0 && m.era >= 2000 ? L.t("ph_idle") : (m.connected ? L.t("ph_idle") : L.t("ph_noline"))
        case "dialtone": return L.t("ph_dialtone")
        case "dialing": return L.t("ph_dialing")
        case "ringback": return L.t("ph_ringback")
        case "ringing": return (m.ringParty || m.era < 1990) ? L.t("ph_ringing_plain") : L.t("ph_ringing", m.ringName.isEmpty ? m.ringNum : m.ringName)
        case "talking": return L.t("ph_talking", m.peerName.isEmpty ? m.peerNumber : m.peerName) + (m.forNum.isEmpty ? "" : "  " + L.t("ph_for", m.forNum))
        case "listening": return L.t("ph_listening")
        case "busy": return L.t("ph_busy")
        case "unknown": return L.t("ph_unknown")
        default: return ""
        }
    }

    var body: some View {
        VStack(spacing: 12) {
            Text(m.status).font(.footnote).foregroundColor(.secondary)
            Text(L.t("hdr_num", m.number)).font(.system(.title, design: .monospaced)).foregroundColor(gold)
            Text(phaseText).font(.headline).multilineTextAlignment(.center).frame(minHeight: 44)
            Text(m.dialed.isEmpty ? " " : m.dialed).font(.system(size: 34, design: .monospaced))
            ForEach(0..<4, id: \.self) { r in
                HStack(spacing: 14) {
                    ForEach(keys[r], id: \.self) { k in
                        Button { m.digit(k) } label: { Text(String(k)).font(.title).frame(width: 76, height: 56).background(Color(white: 0.16)).cornerRadius(12) }
                    }
                }
            }
            HStack(spacing: 14) {
                Button { m.backspace() } label: { Image(systemName: "delete.left").frame(width: 56, height: 48) }
                if m.ph == "ringing" {
                    Button(L.t("btn_answer_big")) { m.lift() }.buttonStyle(CallStyle(color: .green))
                    Button(L.t("reject")) { m.hang() }.buttonStyle(CallStyle(color: .red))
                } else if m.ph == "idle" {
                    Button(L.t("btn_lift")) { m.dialed.isEmpty ? m.lift() : m.placeCall() }.buttonStyle(CallStyle(color: .green))
                } else if m.ph == "dialing" || m.ph == "dialtone" {
                    Button(L.t("btn_call")) { m.placeCall() }.buttonStyle(CallStyle(color: .green))
                    Button(L.t("btn_hang")) { m.hang() }.buttonStyle(CallStyle(color: .red))
                } else {
                    Button(L.t("btn_hang")) { m.hang() }.buttonStyle(CallStyle(color: .red))
                }
            }
            if m.ph == "listening" { Button(m.intervening ? L.t("btn_inter_off") : L.t("btn_inter")) { m.toggleIntervene() }.padding(8).background(Color(white: 0.2)).cornerRadius(8) }
            Toggle(m.speaker ? L.t("spk_on") : L.t("spk_off"), isOn: Binding(get: { m.speaker }, set: { m.changeSpeaker($0) })).padding(.horizontal, 30)
            Spacer()
        }.padding(.top, 8)
    }
}

struct CallStyle: ButtonStyle {
    let color: Color
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.headline).foregroundColor(.white).frame(minWidth: 120, minHeight: 48).background(color.opacity(configuration.isPressed ? 0.6 : 0.85)).cornerRadius(24)
    }
}

// ====================================================================== messages, voice

struct MsgsTab: View {
    @EnvironmentObject var m: AppModel
    @State private var text = ""
    @State private var to = "all"
    var body: some View {
        VStack {
            if m.role != "spectator" || true {
                Picker("", selection: $to) {
                    Text(L.t("tgt_all")).tag("all")
                    Text(L.t("tgt_staff")).tag("staff")
                    if m.role != "spectator" { Text(L.t("tgt_spectators")).tag("spectators"); Text(L.t("tgt_actors")).tag("actors") }
                }.pickerStyle(.segmented).padding(.horizontal)
            }
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 8) {
                    ForEach(m.msgs) { g in
                        VStack(alignment: g.mine ? .trailing : .leading) {
                            Text(g.from + (g.num.isEmpty ? "" : " · " + g.num)).font(.caption).foregroundColor(.secondary)
                            Text(g.text).padding(8).background(g.mine ? curtain : Color(white: 0.18)).cornerRadius(10)
                        }.frame(maxWidth: .infinity, alignment: g.mine ? .trailing : .leading)
                    }
                }.padding()
            }
            HStack {
                TextField(L.t("msg_hint"), text: $text).textFieldStyle(.roundedBorder)
                Button(L.t("btn_send")) { m.sendMsg(to: to, text: text); text = "" }
            }.padding()
        }
    }
}

struct PressButton: View {
    let title: String
    let color: Color
    let onChange: (Bool) -> Void
    @GestureState private var down = false
    var body: some View {
        Text(title).font(.headline).foregroundColor(.white).multilineTextAlignment(.center).frame(maxWidth: .infinity, minHeight: 90)
            .background(color.opacity(down ? 1 : 0.7)).cornerRadius(16)
            .gesture(DragGesture(minimumDistance: 0).updating($down) { _, s, _ in s = true }.onChanged { _ in onChange(true) }.onEnded { _ in onChange(false) })
    }
}

struct VoiceTab: View {
    @EnvironmentObject var m: AppModel
    var body: some View {
        VStack(spacing: 16) {
            Text(L.t("voice_title")).font(.title2)
            if m.role == "regie" || m.role == "actor" {
                Text(L.t("voice_hint_staff")).font(.footnote).foregroundColor(.secondary)
                PressButton(title: L.t("voice_pa_btn"), color: curtain) { m.ptt(1, down: $0) }
                PressButton(title: L.t("voice_sp_btn"), color: .blue) { m.ptt(2, down: $0) }
            } else {
                Text(L.t("voice_hint_sp")).font(.footnote).foregroundColor(.secondary)
            }
            Spacer()
        }.padding()
    }
}

// ====================================================================== Regie

struct RegieTab: View {
    @EnvironmentObject var m: AppModel
    @State private var kind = "text"
    @State private var to = "all"
    @State private var text = ""
    @State private var showTools = false
    @State private var picking: String?
    @State private var pickTarget = "all"
    var body: some View {
        NavigationView {
            List {
                Section(header: Text(L.t("sec_cues"))) {
                    Picker(L.t("sec_cues"), selection: $kind) { ForEach(CUE_KINDS, id: \.self) { Text(L.t("cue_" + $0)).tag($0) } }
                    TextField(L.t("grp_hint"), text: $to).autocapitalization(.none).disableAutocorrection(true)
                    TextField(L.t("hint_" + kind), text: $text).autocapitalization(.none)
                    HStack {
                        Button(L.t("btn_send_cue")) { m.sendCue(to: to, kind: kind, text: text) }.buttonStyle(.borderedProminent).tint(curtain)
                        Spacer()
                        Button("PANIC") { m.sendCue(to: "all", kind: "panic", text: "") }.buttonStyle(.borderedProminent).tint(.red)
                    }
                    if Media.KINDS[kind] != nil { Button(L.t("media_send")) { picking = kind; pickTarget = to } }
                }
                Section(header: Text(L.t("tools18"))) {
                    Button(L.t("tool_chain")) { showTools = true }
                    HStack {
                        Button(L.t("meter_noise_btn")) { m.sendMeter("noise", to: to) }
                        Spacer()
                        Button(L.t("meter_motion_btn")) { m.sendMeter("motion", to: to) }
                    }.buttonStyle(.bordered)
                    HStack {
                        Button(L.t("tape_rec")) { m.tape("rec", id: to.hasPrefix("id:") ? String(to.dropFirst(3)) : "") }
                        Button(L.t("tape_stop")) { m.tape("stop") }
                        Button(L.t("tape_play")) { m.tape("play", to: to) }
                    }.buttonStyle(.bordered)
                    Button(L.t("report_btn")) { m.requestReport() }
                    if !m.reportMd.isEmpty { ShareLink(item: m.reportMd) { Label("Report (Markdown)", systemImage: "square.and.arrow.up") } }
                    if !m.chainSummary.isEmpty { Text(m.chainSummary).font(.footnote) }
                    if !m.pollSummary.isEmpty { Text(m.pollSummary).font(.footnote) }
                }
                Section(header: Text(L.t("sec_devices"))) {
                    HStack {
                        Button(L.t("btn_auto")) { m.autoCouple() }
                        Spacer()
                        ShareLink(item: m.makeInvite(role: "spectator")) { Label(L.t("btn_invite_sp"), systemImage: "person.badge.plus") }
                        ShareLink(item: m.makeInvite(role: "actor")) { Label(L.t("btn_invite_ac"), systemImage: "theatermasks") }
                    }.buttonStyle(.bordered).labelStyle(.iconOnly)
                    if m.peers.isEmpty { Text(L.t("no_devices")).foregroundColor(.secondary) }
                    ForEach(m.peers) { p in PeerRow(p: p, onSelect: { to = "id:" + p.id }) }
                }
                if !m.log.isEmpty {
                    Section(header: Text(L.t("btn_journal"))) {
                        ForEach(Array(m.log.suffix(40).enumerated()), id: \.offset) { _, l in Text(l).font(.system(.caption, design: .monospaced)) }
                    }
                }
            }
            .navigationTitle(L.t("tab_regie"))
            .sheet(isPresented: $showTools) { ChainSetup() }
            .fileImporter(isPresented: Binding(get: { picking != nil }, set: { if !$0 { picking = nil } }), allowedContentTypes: [.image, .movie, .audio]) { r in
                if case .success(let u) = r, let k = picking {
                    let ok = u.startAccessingSecurityScopedResource()
                    defer { if ok { u.stopAccessingSecurityScopedResource() } }
                    if let d = try? Data(contentsOf: u) { m.pushMedia(kind: k, data: d, to: [pickTarget]) }
                }
                picking = nil
            }
        }.navigationViewStyle(.stack)
    }
}

struct PeerRow: View {
    @EnvironmentObject var m: AppModel
    let p: PeerInfo
    let onSelect: () -> Void
    @State private var coupleWith = false
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Circle().fill(p.online ? Color.green : Color.gray).frame(width: 9, height: 9)
                Text(p.name).bold()
                Text(p.number).font(.system(.footnote, design: .monospaced)).foregroundColor(gold)
                if !p.group.isEmpty { Text("[" + p.group + "]").font(.caption) }
                Spacer()
                if p.bat >= 0 { Text("\(p.bat)%").font(.caption).foregroundColor(p.bat < 20 ? .red : .secondary) }
            }
            Text(p.role + (p.line > 0 ? " · line \(p.line)" : "") + (p.muted ? " · muted" : "") + (p.busy ? " · busy" : "") + (p.fx.isEmpty ? "" : " · fx " + p.fx)).font(.caption).foregroundColor(.secondary)
        }
        .contentShape(Rectangle()).onTapGesture(perform: onSelect)
        .contextMenu {
            Button(L.t("btn_call_sel")) { m.callNumber(p.number) }
            if p.mon { Button(L.t("btn_listen")) { m.monitor(p.number) } }
            Button(p.muted ? "Unmute" : L.t("btn_mute")) { m.mute(p.id, !p.muted) }
            if p.line > 0 { Button(L.t("btn_uncouple")) { m.uncouple(p.id) } }
            Menu(L.t("fx_btn", "")) {
                Button(L.t("fx_none")) { m.setFx(p.id, "") }
                ForEach(VoiceFx.KINDS, id: \.self) { k in Button(L.t("fx_" + k)) { m.setFx(p.id, k) } }
            }
            Button(L.t("btn_forget"), role: .destructive) { m.forget(p.id) }
        }
    }
}

struct ChainSetup: View {
    @EnvironmentObject var m: AppModel
    @Environment(\.dismiss) var dismiss
    @State private var text = ""
    @State private var nums = ""
    @State private var phrase = ""
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text(L.t("tool_chain"))) {
                    TextField(L.t("chain_text"), text: $text)
                    TextField(L.t("chain_nums"), text: $nums).keyboardType(.numbersAndPunctuation)
                    Button(L.t("chain_send")) {
                        m.startChain(text, nums.split(whereSeparator: { !$0.isNumber }).map(String.init)); dismiss()
                    }
                }
                Section(header: Text(L.t("tool_secret"))) {
                    TextField(L.t("secret_phrase"), text: $phrase)
                    Text(L.t("secret_hint")).font(.footnote)
                    Button(L.t("chain_send")) { m.startSecret(phrase, to: "spectators"); dismiss() }
                }
            }.navigationTitle(L.t("tools18"))
        }
    }
}

// ====================================================================== script

struct ScriptTab: View {
    @EnvironmentObject var m: AppModel
    @State private var name = ""
    @State private var to = "all"
    @State private var kind = "text"
    @State private var text = ""
    @State private var delay = "0"
    @State private var exportText = ""
    @State private var importing = false
    var body: some View {
        NavigationView {
            List {
                Section {
                    HStack {
                        Button(L.t("btn_go")) { m.goNext() }.buttonStyle(.borderedProminent).tint(curtain)
                        Button(m.playing ? L.t("btn_stop") : L.t("btn_play")) { m.playing ? m.stopScript() : m.playFrom(m.cursor) }.buttonStyle(.bordered)
                        Button(L.t("btn_rewind")) { m.cursor = 0 }.buttonStyle(.bordered)
                    }
                    if m.script.isEmpty { Text(L.t("script_empty")).foregroundColor(.secondary) }
                    ForEach(Array(m.script.enumerated()), id: \.element.id) { i, s in
                        HStack {
                            Text(i == m.cursor ? "▶" : "\(i + 1)").frame(width: 28)
                            VStack(alignment: .leading) {
                                Text(s.name.isEmpty ? L.t("cue_" + s.kind) : s.name).bold()
                                Text("\(s.to) · \(s.kind) · \(s.text)").font(.caption).foregroundColor(.secondary).lineLimit(2)
                            }
                            Spacer()
                            Text("\(s.delay)s").font(.caption)
                        }
                        .contentShape(Rectangle()).onTapGesture { m.cursor = i }
                        .swipeActions { Button(L.t("btn_del"), role: .destructive) { m.removeStep(i) }; Button("↑") { m.moveStep(i, -1) }.tint(.gray); Button("↓") { m.moveStep(i, 1) }.tint(.blue) }
                    }
                }
                Section(header: Text(L.t("sec_newstep"))) {
                    TextField(L.t("step_name_hint"), text: $name)
                    TextField(L.t("grp_or_num_hint"), text: $to).autocapitalization(.none)
                    Picker(L.t("sec_cues"), selection: $kind) { Text(L.t("kind_msg")).tag("msg"); ForEach(CUE_KINDS, id: \.self) { Text(L.t("cue_" + $0)).tag($0) } }
                    TextField(kind == "msg" ? L.t("hint_msg") : L.t("hint_" + kind), text: $text).autocapitalization(.none)
                    TextField(L.t("step_delay_hint"), text: $delay).keyboardType(.numberPad)
                    Button(L.t("btn_add_step")) { m.addStep(Step(name: name, to: to, kind: kind, text: text, delay: Int(delay) ?? 0)); name = ""; text = "" }
                }
                Section {
                    ShareLink(item: m.scriptJson()) { Label(L.t("btn_export"), systemImage: "square.and.arrow.up") }
                    Button(L.t("btn_import")) { importing = true }
                }
            }
            .navigationTitle(L.t("tab_script"))
            .fileImporter(isPresented: $importing, allowedContentTypes: [.json, .plainText]) { r in
                if case .success(let u) = r {
                    let ok = u.startAccessingSecurityScopedResource()
                    defer { if ok { u.stopAccessingSecurityScopedResource() } }
                    if let s = try? String(contentsOf: u, encoding: .utf8) { m.showToast(L.t(m.importScript(s) ? "script_imported" : "script_bad")) }
                }
            }
        }.navigationViewStyle(.stack)
    }
}

// ====================================================================== settings

struct SettingsTab: View {
    @EnvironmentObject var m: AppModel
    @State private var confirmExit = false
    var body: some View {
        NavigationView {
            Form {
                Section {
                    Picker(L.t("set_lang"), selection: $m.lang) { ForEach(0..<L.codes.count, id: \.self) { Text(L.names[$0]).tag(L.codes[$0]) } }
                    Picker(L.t("era_pick"), selection: $m.era) { Text("1980").tag(1980); Text("1990").tag(1990); Text("2000").tag(2000) }.pickerStyle(.segmented)
                    Toggle(L.t("duck_on"), isOn: $m.duckOn)
                }
                if m.role == "regie" {
                    Section(header: Text(L.t("hub_info"))) {
                        Text("PIN: " + m.hubPin).font(.system(.body, design: .monospaced))
                        Text(Hub.localAddresses().map { $0 + ":" + String(PORT) }.joined(separator: "\n")).font(.caption)
                        Toggle(L.t("opt_listen"), isOn: $m.optListen).onChange(of: m.optListen) { _ in m.sendOpts() }
                        Toggle(L.t("opt_click"), isOn: $m.optClick).onChange(of: m.optClick) { _ in m.sendOpts() }
                        Toggle(L.t("opt_peermsg"), isOn: $m.optPeerMsg).onChange(of: m.optPeerMsg) { _ in m.sendOpts() }
                    }
                    Section(header: Text(L.t("set_rules"))) {
                        Text(L.t("rules_hint")).font(.caption)
                        TextEditor(text: $m.rulesText).font(.system(.footnote, design: .monospaced)).frame(minHeight: 120).autocapitalization(.none)
                        Button(L.t("btn_send")) { m.saveRules() }
                    }
                    Section(header: Text(L.t("set_osc"))) {
                        Text(L.t("osc_hint", m.hubPin)).font(.caption)
                        TextField(L.t("osc_out_host"), text: $m.oscOutHost).autocapitalization(.none)
                        Stepper(L.t("osc_out_port") + ": \(m.oscOutPort)", value: $m.oscOutPort, in: 1...65535)
                        Toggle(L.t("osc_in_on"), isOn: $m.oscInOn)
                        Stepper(L.t("osc_in_port") + ": \(m.oscInPort)", value: $m.oscInPort, in: 1024...65535)
                        Button(L.t("btn_send")) { m.saveOsc() }
                    }
                    Section(header: Text(L.t("set_dmx"))) {
                        Text(L.t("dmx_hint")).font(.caption)
                        TextField(L.t("dmx_host"), text: $m.dmxHost).autocapitalization(.none)
                        Toggle(L.t("dmx_on"), isOn: $m.dmxOn)
                        Button(L.t("btn_send")) { m.saveDmx() }
                    }
                }
                Section(header: Text(L.t("set_about"))) {
                    Text(L.t("about_text")).font(.footnote)
                    Text("CUPLAJ iOS · protocol " + VERSION).font(.caption).foregroundColor(.secondary)
                }
                Section { Button(L.t("btn_exit"), role: .destructive) { confirmExit = true } }
            }
            .navigationTitle("CUPLAJ")
            .alert(L.t("confirm_exit"), isPresented: $confirmExit) {
                Button(L.t("yes"), role: .destructive) { m.stopSession() }
                Button(L.t("no"), role: .cancel) {}
            }
        }.navigationViewStyle(.stack)
    }
}
