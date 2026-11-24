# CUPLAJ

**English** · [Română](README.ro.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Español](README.es.md) · [Português](README.pt.md) · [Deutsch](README.de.md) · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Party-line telephony for immersive theatre**

_Version: 1.8.0 · Last update: 2026-11-24_

CUPLAJ turns a fleet of ordinary Android phones into a private telephone exchange for immersive theatre. Organisers and actors talk to spectators (and to each other) over the venue's local Wi‑Fi — no cloud, no accounts, no ads. Its signature feature is the *cuplaj*: the shared **party line** of fixed telephones from the 1980s to about 2006, where two subscribers shared one line, rang with different cadences, and could overhear each other.

## Features

- **Three telephone eras** — 1985 rotary dial (audible pulses), 1995 keypad (DTMF tones), 2003 mobile (SMS, 160 characters). The control can switch the era live.
- **Party line (cuplaj)** — two phones share one line; a call to one rings both with the called subscriber's cadence; the neighbour who picks up first takes the call; lifting the handset during a neighbour's call lets you listen in, and even break in.
- **No infrastructure** — one phone (the control) is the hub. Others join via Wi‑Fi or the hub's hotspot; hubs are found automatically; optional room code and PIN.
- **Voice** — real calls (8 kHz), push‑to‑talk public address from the control, backstage channel for the crew.
- **Cues** — full‑screen text, vibration, ring, flash, loudspeaker, era change, new contact, spoken voice (local text‑to‑speech), sound effects, black screen, phantom call.
- **Script** — a cue list with GO and PLAY (timed), reorder, import/export as JSON.
- **Control tools** — numbers, groups, couple/uncouple, automatic pairs, silent monitor, mute, forget, battery levels, event log, state that survives restarts, rate limiting.
- **8 languages and accessibility** — English, Romanian, French, Italian, Spanish, Portuguese, German, Hungarian; adjustable text size; answer from the lock screen; keeps running in the background.
- **Internet and mobile data (1.1)** — connect with `host` or `host:port`; optional AES‑128‑GCM network key; automatic G.711 compression off the local network; keepalive.
- **Direct P2P calls (1.1)** — audio goes phone‑to‑phone over UDP with hole punching, falling back to the hub relay; the control can set it off, local‑only (default) or anywhere.
- **VoIP / SIP gateway (1.1)** — the control phone registers with a SIP provider or PBX; the crew dials `0`+number to call out, incoming SIP calls ring an internal number.
- **Mobile network (1.1)** — SMS gateway through the control phone's SIM (send and receive), a *real call* cue that opens the dialler, and a Wi‑Fi / mobile indicator.
- **More ways to connect (1.2)** — Bluetooth (no router, about 7 phones), Wi‑Fi Direct (the control phone makes its own Wi‑Fi network) and mDNS discovery, in addition to Wi‑Fi, hotspot, USB tethering, Ethernet and mobile data.
- **Plug and play (1.3)** — the control phone opens every fitting link by itself; other phones tap *Find and join automatically*, or tap an invitation link. Adds Wi‑Fi Aware.
- **Hardened and cut off from the network (1.4)** — a hostile‑audience audit with fixes, and CUPLAJ‑AIR: three original protocols that carry cues and invitations with no network at all (torch → camera, speaker → microphone, vibration → accelerometer).
- **Show control (1.5)** — cues that fire at one shared instant, a PANIC cue, full‑screen colour cues, audience polls, delivery confirmation and a hardware PTT key. Compared with Unity Intercom, Zello, Augmented Theatre, QLab/SOLOstudio and Livecube in `docs/COMPARISON.md`.
- **Everything from the comparison that fits an Android‑only, offline app (1.6)** — PA priority (ducking), OSC in and out for lighting and sound desks, and image, video and audio‑clip cues sent through the hub.
- **Smart show (1.7)** — zones that follow the audience by sound, rules ("when someone enters zone 3, send them…"), a crowd meter, voice effects, captions, multilingual and personalised text, story branching.
- **Stage tools (1.8)** — Art‑Net/DMX lights, MIDI, cue waves, whispering game, split secret, ghost tape, house‑lights safety cue, Morse by vibration, post‑show report.

## Quick start

1. Get the APK from the *Actions* tab (artifact `CUPLAJ-apk`) or the *Releases* page and install it on every phone.
2. On the control phone, start the hotspot (or put all phones on the same Wi‑Fi).
3. On the control phone choose **Control** → *Start the hub*. Note the PIN for actors and, optionally, a room code.
4. On the other phones choose **Spectator** or **Actor**, pick the hub from the list (or type its address), and connect.
5. Every phone gets a number (control 100, actors 201…, spectators 6 digits). Lift the handset and call.

## The three eras

- **1985 · rotary** — a round dial you really turn; the pulses are audible; two long rings.
- **1995 · keypad** — DTMF tones, notebook of numbers, pager messages.
- **2003 · mobile** — names in the contact list, caller ID, SMS limited to 160 characters.

## How the party line works

<p align="center"><img src="docs/partyline.svg" width="620" alt="party line"></p>

1. Two phones are *coupled* by the control (or automatically in pairs). They share line N, slots 1 and 2.
2. A call to subscriber 1 rings both phones; subscriber 1 hears the cadence of their own number (one long ring), subscriber 2 hears the other (two short rings).
3. Whoever answers takes the call; the other phone stops ringing.
4. While the line is in use, the neighbour who lifts the handset hears the conversation and a faint click is heard by the parties.
5. A listener may *break in* and speak; a call to a coupled phone whose line is busy gets the busy tone.
6. The control can monitor any line silently (no click).

## Documentation

- [User guide](docs/GUIDE.md)
- [Protocol](docs/PROTOCOL.md) *(English)*
- [Adversarial audit](docs/AUDIT.md) *(English)*
- [Changelog](CHANGELOG.md) *(English)*
- [Presentation site](https://chiuta.github.io/CUPLAJ/)

## Honest status

The hub logic (calls, party line, permissions, limits, persistence) is covered by more than 250 automated checks that run on a JVM. The Android user interface and audio path have been type‑checked against the Android 14 SDK but **not yet tested on physical devices**; the APK is built by GitHub Actions. Test with a handful of phones before a show. Traffic on the local network is not encrypted (the PIN and room code keep honest strangers out, they are not cryptography). Version 1.1 networking (encryption, P2P, SIP, SMS) is covered by hub tests against a simulated SIP server, but has **not** been tried on real operators, NATs or SIM cards. Bluetooth, Wi‑Fi Direct and mDNS links (1.2) share the same transport layer, tested on the JVM with simulated channels, but have **not** been tried on real devices; behaviour differs between phone makers. Plug and play (auto‑join, invitations, Wi‑Fi Aware, 1.3) is **not** tested on devices; the invitation format is covered by JVM tests. The 1.4 audit fixes and the AIR codecs are covered by JVM tests (simulated noise, clock drift, wrong key, interference); the torch, camera, microphone, speaker, motor and accelerometer code is **not** tested on real phones, and real‑world range and speed will differ. The 1.5 features are covered by JVM tests (clock estimation, delays, poll rules, photosensitive limits); the screen overlays, headset‑button handling and real clock accuracy on phones are **not** tested on devices. The 1.6 features are covered by JVM tests (ducking curve, OSC parser fuzzing and flooding, file transfer integrity and path safety, hub relay); playing images, video and clips, the file picker and the settings dialogs are **not** tested on devices. Zones, rules, meter, voice effects and languages are covered by JVM tests (rule engine, hub rules, meter aggregation, voice filters measured by frequency); real sound beacons indoors, the microphone and accelerometer measurements and the voice quality on phones are **not** tested on devices. A recorded beacon can be replayed once per sequence number, so zones are for atmosphere, not for security. Art‑Net packets, DMX fades, MIDI messages, Morse patterns, the report and the hub logic for waves, whispering game, split secret and report are covered by JVM tests; the ghost tape, real lighting desks and MIDI devices, and everything on real phones are not tested.

## Build

Requires JDK 17. `./gradlew assembleRelease` produces `app/build/outputs/apk/release/app-release.apk` (signed with the debug key, installable directly). Unit tests: `./gradlew test`. The workflow in `.github/workflows` does both with Node ≥ 24 tooling on Ubuntu 26.04.

## Privacy

No analytics, no servers, no accounts. Audio and messages travel only inside your local network, between the phones and the hub. The hub keeps numbers, groups and couplings on the control phone so a restart does not lose them.

## Support the project

CUPLAJ is free and trade‑free. If it helps your show, you can buy the author a coffee: <https://buymeacoffee.com/echo.of.the.strings>.

## Licence

MIT © Alexandru‑Ionuț Chiuță (Alexio).
