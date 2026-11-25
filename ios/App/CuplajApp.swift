import SwiftUI
import CuplajCore

@main
struct CuplajApp: App {
    @StateObject private var model = AppModel()
    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .preferredColorScheme(.dark)
                .onOpenURL { url in
                    if model.applyInvite(url.absoluteString) { model.showToast(L.t("invite_applied")) } else { model.showToast(L.t("invite_unknown_hub")) }
                }
        }
    }
}

struct RootView: View {
    @EnvironmentObject var m: AppModel
    var body: some View {
        ZStack {
            Color(red: 0.07, green: 0.06, blue: 0.06).ignoresSafeArea()
            if m.inSession { MainView() } else { SetupView() }
            Overlays()
        }
    }
}

let gold = Color(red: 0.91, green: 0.76, blue: 0.43)
let curtain = Color(red: 0.55, green: 0.12, blue: 0.10)

// ====================================================================== setup

struct SetupView: View {
    @EnvironmentObject var m: AppModel
    @State private var inviteText = ""
    var body: some View {
        NavigationView {
            Form {
                Section {
                    Text(L.t("tagline")).font(.headline).foregroundColor(gold)
                    TextField(L.t("name_hint"), text: $m.name).textContentType(.name)
                    Picker(L.t("role"), selection: $m.role) {
                        Text(L.t("role_spectator")).tag("spectator")
                        Text(L.t("role_actor")).tag("actor")
                        Text(L.t("role_regie")).tag("regie")
                    }.pickerStyle(.segmented)
                }
                if m.role == "regie" { regieSection } else { guestSection }
                Section {
                    Picker(L.t("set_lang"), selection: $m.lang) {
                        ForEach(0..<L.codes.count, id: \.self) { Text(L.names[$0]).tag(L.codes[$0]) }
                    }
                }
                Section {
                    Button(m.role == "regie" ? L.t("btn_start_hub") : L.t("btn_connect")) { m.startSession() }
                        .frame(maxWidth: .infinity).foregroundColor(.white).padding(8).background(curtain).cornerRadius(10)
                        .listRowBackground(Color.clear)
                }
                Section { Text(L.or("ios_note", "iPhone: keep the app open on screen during the show. As host (Regie) the phone must stay awake and plugged in.")).font(.footnote).foregroundColor(.secondary) }
            }
            .navigationTitle("CUPLAJ")
            .onAppear { m.startDiscovery() }
        }
        .navigationViewStyle(.stack)
    }

    var guestSection: some View {
        Group {
            Section(header: Text(L.t("hubs_found"))) {
                if m.hubs.isEmpty { Text(L.t("hubs_none")).foregroundColor(.secondary) }
                ForEach(m.hubs) { h in
                    Button { m.host = h.host } label: {
                        HStack { Text(h.name); Spacer(); Text(h.host).font(.caption).foregroundColor(.secondary); if h.encrypted { Image(systemName: "lock.fill") } }
                    }
                }
                TextField(L.t("host_hint"), text: $m.host).keyboardType(.URL).autocapitalization(.none).disableAutocorrection(true)
            }
            Section {
                if m.role == "actor" { SecureField(L.t("actor_pin"), text: $m.pin).keyboardType(.numberPad) }
                TextField(L.t("room_hint"), text: $m.room).autocapitalization(.none)
                TextField(L.t("key_hint"), text: $m.joinKey).autocapitalization(.none).disableAutocorrection(true)
            }
            Section(header: Text(L.t("btn_auto_join"))) {
                TextField("cuplaj://join?…", text: $inviteText).autocapitalization(.none).disableAutocorrection(true)
                Button(L.t("invite_join")) { if m.applyInvite(inviteText) { m.showToast(L.t("invite_applied")); inviteText = "" } else { m.showToast(L.t("invite_unknown_hub")) } }
            }
        }
    }

    var regieSection: some View {
        Group {
            Section { Text(L.t("regie_explain")).font(.footnote) }
            Section(header: Text("PIN")) {
                HStack { Text(m.hubPin).font(.system(.title2, design: .monospaced)); Spacer(); Text(Hub.localAddresses().joined(separator: ", ")).font(.caption).foregroundColor(.secondary) }
                TextField(L.t("room_hint"), text: $m.room).autocapitalization(.none)
                Toggle(L.t("enc_on"), isOn: $m.encOn)
                if m.encOn { TextField(L.t("enc_key"), text: $m.netKey).autocapitalization(.none).disableAutocorrection(true) }
            }
        }
    }
}

// ====================================================================== overlays

struct Overlays: View {
    @EnvironmentObject var m: AppModel
    var body: some View {
        ZStack {
            if let cp = m.colorNow { ColorFlash(plan: cp, stamp: m.colorStamp) }
            if m.blackoutOn {
                Color.black.ignoresSafeArea()
                if !m.blackoutText.isEmpty { Text(m.blackoutText).font(.title2).foregroundColor(.white.opacity(0.7)).multilineTextAlignment(.center).padding() }
            }
            if let big = m.bigText {
                Text(big).font(.system(size: 34, weight: .semibold, design: .serif)).foregroundColor(.white).multilineTextAlignment(.center)
                    .padding(24).background(Color.black.opacity(0.88)).cornerRadius(16).padding().onTapGesture { m.bigText = nil }
            }
            if let md = m.mediaShow { MediaView(kind: md.kind, url: md.url).onTapGesture { m.mediaShow = nil } }
            VStack {
                Spacer()
                if !m.captionText.isEmpty {
                    Text(m.captionText).font(.title3).foregroundColor(.white).multilineTextAlignment(.center).padding(10)
                        .background(Color.black.opacity(0.85)).cornerRadius(10).padding(.bottom, 40)
                }
                if let t = m.toast { Text(t).font(.footnote).padding(10).background(Color(white: 0.2)).cornerRadius(8).padding(.bottom, 8) }
            }
            if m.blackoutOn || m.colorNow != nil { VStack { Spacer(); HStack { Spacer(); Button(L.or("panic_btn", "✕")) { m.panic() }.padding().foregroundColor(.white.opacity(0.25)) } } }
        }
        .allowsHitTesting(m.blackoutOn || m.colorNow != nil || m.mediaShow != nil || m.bigText != nil)
    }
}

/// Full-screen colour. The rate is capped at 2 Hz by colorPlan (photosensitivity).
struct ColorFlash: View {
    let plan: ColorPlan
    let stamp: Date
    var body: some View {
        TimelineView(.animation) { ctx in
            let on: Bool = {
                if plan.hz <= 0 { return true }
                let t = ctx.date.timeIntervalSince(stamp)
                return Int(t * plan.hz * 2) % 2 == 0
            }()
            Color(red: on ? plan.r : 0, green: on ? plan.g : 0, blue: on ? plan.b : 0).ignoresSafeArea()
        }
    }
}

import AVKit
struct MediaView: View {
    let kind: String
    let url: URL
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            if kind == "video" { VideoPlayer(player: AVPlayer(url: url)).ignoresSafeArea() }
            else if let d = try? Data(contentsOf: url), let ui = UIImage(data: d) { Image(uiImage: ui).resizable().scaledToFit() }
        }
    }
}
