# CUPLAJ for iOS

Client + Hub (Regie) for iPhone/iPad, speaking the same protocol as the Android app (see `PROTOCOL.md`), so Android and iOS phones can share one show.

- `Sources/CuplajCore` – protocol, AES-GCM crypto, hub, client, cues, rules, Art-Net, MIDI, OSC (Swift package; `swift test` runs on macOS CI).
- `App/` – SwiftUI app (phone, messages, voice/PA, Regie console, script, settings).
- `project.yml` – XcodeGen spec: `cd ios && xcodegen generate`, then open `Cuplaj.xcodeproj`, set your signing team, run.

## Honest status
- Core tests and the simulator build pass in CI. **Nothing has been tested on a real iPhone**: audio, echo, haptics, torch, MIDI and Bonjour behaviour on hardware are unverified.
- Not ported: AIR (light/sound/haptic modem), SIP gateway, real SMS/phone-call gateway, Bluetooth, Wi-Fi Direct/Aware, P2P audio (calls are always relayed), and background operation – the app must stay open and the screen awake, especially as Regie.
- No signed build or App Store/TestFlight pipeline yet (needs an Apple developer account).
