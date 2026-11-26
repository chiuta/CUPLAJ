# CUPLAJ for iOS

Client + Hub (Regie) for iPhone/iPad, speaking the same protocol as the Android app (see `PROTOCOL.md`), so Android and iOS phones can share one show.

- `Sources/CuplajCore` – protocol, AES-GCM crypto, hub, client, cues, rules, Art-Net, MIDI, OSC, direct P2P audio, SIP gateway, AIR codecs (Swift package; `swift test` runs on macOS CI).
- `App/` – SwiftUI app (phone, messages, voice/PA, Regie console, script, settings, AIR I/O).
- `project.yml` – XcodeGen spec: `cd ios && xcodegen generate`, then open `Cuplaj.xcodeproj`, set your signing team, run.

## What is ported
Everything that has a public iOS API: calls and party lines, cues (incl. timed/wave), polls, rules, meter, chain/secret, tape, report, media, PA with ducking, Art-Net, MIDI, OSC in/out, invite links, Bonjour + UDP beacon discovery, **direct P2P audio**, **SIP/VoIP gateway** (REGISTER, INVITE, digest auth, RTP PCMU), **CUPLAJ-AIR** (LUMEN torch/camera, SONUS speaker/mic incl. near-ultrasonic, TACTUS haptics/accelerometer, zone beacon, air invitations) and **background operation** (active audio session + `UIBackgroundModes=audio`, engine revived after interruptions).

## Honest status
- CI (macOS) runs the core tests (crypto vectors, hub integration over loopback, P2P over loopback, SIP registration against a fake server, AIR round trips on synthetic signals) and builds the app for the simulator.
- **Nothing has been tested on a real iPhone or against a real SIP provider.** Audio quality/echo, haptics, torch timing, camera/mic/accelerometer reception, ultrasonic reach (the voice-chat audio session may attenuate it; use the audible mode if the ultrasonic one fails), Bonjour and background survival are unverified on hardware.
- Cannot be ported (platform limits, not omissions): **Bluetooth RFCOMM** (no public API on iOS), **Wi-Fi Direct** (no API to create/join groups; use the iPhone Personal Hotspot or any shared Wi-Fi instead), **silent SMS gateway and silent phone calls** (iOS only lets the user confirm; the `realcall` cue opens the dialler). **Wi-Fi Aware** needs iOS 26 and cross-platform behaviour that cannot be verified here, so it is not included.
- Background operation is best effort: iOS may still suspend the app, especially a Regie phone; keep it open, plugged in and with the screen on during a show.
- No signed build or App Store/TestFlight pipeline yet (needs an Apple developer account).

## Eras (1.10)

All six eras (1985, 1995, 2003, 2025, 2045, 2080) exist on iOS: era‑specific phone screen, ringtones, message limits, the *time travel* cue and shake‑to‑answer for 2045 (CoreMotion). Logic is in `CuplajCore/Era.swift` with tests; the screens and shake detector are untested on a real iPhone.
