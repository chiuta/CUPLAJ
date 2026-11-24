# Changelog

Documentation language: English only.

## 1.8.0 — 2026‑11‑24

* **Art‑Net / DMX out** (cue kind *dmx*, `universe;1=255,2-4=128;fadeMs`, linear fades, 25 Hz while fading, 1 Hz otherwise) and **MIDI out** (cue kind *midi*: note, off, cc, pc, MMC; Android MidiManager, first USB/Bluetooth device). Both run on the control phone only and do not pass through the hub.
* **Cue wave**: text starting `~150;` makes the hub send the cue to phones one after another in number order.
* **Whispering game** (`chain`): a message passes through chosen phones, each may alter it; the control phone receives the original and every version.
* **Split secret** (`secret`/`guess`): a phrase is split word by word over the phones; the first correct guess (case, accents and punctuation ignored, 20 tries per phone) is reported and fires rule trigger `solved`.
* **Ghost tape** (`tape`): the hub records up to 60 s of one phone's audio (6 MB cap) and replays it to a group on reserved source 253.
* **House‑lights cue** (*house*: PANIC + white screen + message + vibration) and **Morse cue** (*morse*, text → vibration pattern, 60 characters).
* **Post‑show report** (`report`): Markdown with totals, per‑phone calls, messages, zone entries, battery, and the timeline; shared through the system share sheet.
* Not tested: ghost tape, real lighting desks, MIDI devices, everything on real phones. Not done: iOS, field test on real phones, program feed.

## 1.7.0 — 2026‑11‑24

* **Show zones**: a phone left in a room plays an authenticated near‑ultrasonic beacon (AIR type 3); audience phones that hear it report the zone to the hub; the control phone sees 📍zone next to each name. No GPS, Bluetooth or Wi‑Fi scan needed.
* **Rules engine** on the hub: `zone 3 once > self | text | Bun venit, {name}`, `pollwin 0 > …`, `meter noise 70 > …`; targets include *self* (the person who triggered). Action `goto` jumps the control phone's script to a step (story branching by poll result).
* **Crowd meter**: noise or movement over 5 s; phones send only a number 0–100 (never audio); average, peak and answer count shown to the control phone and usable in rules.
* **Voice effects** assigned by the control phone to a phone: robot, old radio, echo, deep, high, lo‑fi; applied on the speaker's phone before sending, so every listener hears it.
* **Captions** (cue kind *caption*) and multilingual text: `en=Hello|ro=Salut` shows each phone its own language (also for text, say, blackout, fake call); `{name}` and `{number}` personalise a cue per phone.

## 1.6.0 — 2026‑11‑24

* PA priority (ducking): PA announcements travel on a reserved audio source; other sounds drop to 30 % while it plays and return smoothly (setting).
* OSC out: every cue is also sent as `/cuplaj/cue to kind text` over UDP; OSC in: `/cuplaj/<hub PIN>/go|panic|stop|cue` with the PIN in the address, 20 messages per second at most.
* Media cues: image, video and audio‑clip files (≤ 8 MB) sent through the hub in checked pieces (size, SHA‑256, order, path‑safe names), at most 3 transfers at a time, cache trimmed to 48 MB; new cue kinds *image*, *video*, *clip*.
* Not done: iOS, MIDI, program feed, more channels than groups.

## 1.5.0 — 2026‑11‑24

* Comparison with Unity Intercom, Zello, Augmented Theatre, QLab Remote / SOLOstudio / MiX16 and Livecube: `docs/COMPARISON.md`.
* Shared clock: phones estimate the hub's clock (lowest round trip of the last 8 samples); a cue text starting `@3.5;` fires 3.5 s later on every phone at once; cues more than 10 s late are dropped.
* PANIC cue: stops ringing, vibration, speech, sound effects, black and colour screens, fake calls and pending timed cues; also accepted over AIR.
* Full‑screen colour cue (`#RRGGBB;seconds;pulses per second`), pulses capped at 2 Hz, long press closes it.
* Audience polls (2–6 options, one changeable vote per phone, result to the control phone); poll and end‑poll are cue kinds, so they work in scripts.
* Delivery confirmation after each cue (sent to N of M online phones).
* Optional hardware PTT key: headset button and volume‑up.

## 1.4.0 — 2026‑11‑24

* Adversarial audit with fixes: random 6‑digit hub PIN; per‑connection session key (HMAC of both nonces) and 100 000 PBKDF2 iterations; plain `denied bad_key` / `no_key`; frame limits (2 KB before login, 32 KB after); pre‑login and per‑origin connection caps; lockout after repeated bad PINs; constant‑time secret comparison; eviction of offline spectators when full; loopback no longer trusted; P2P candidates limited to private addresses; dialled numbers stripped of USSD characters; invitation links need confirmation.
* CUPLAJ‑AIR, three original no‑network protocols: **LUMEN** (torch → camera, Manchester slots with Barker sync), **SONUS** (8‑FSK, near‑ultrasonic or audible, Goertzel detection), **TACTUS** (vibration pulse lengths → accelerometer). Shared authenticated packet (HMAC tag, replay guard). Carry cues and a hub invitation.
* New permission: camera (receive LUMEN). Settings → *Air links*.

## 1.3.0 — 2026‑11‑24

* Plug and play: *Plug & play* mode on the control phone opens every fitting link on its own (Wi‑Fi/hotspot, mDNS, Bluetooth, Wi‑Fi Aware, and a Wi‑Fi Direct group when there is no Wi‑Fi); one permission prompt.
* *Find and join automatically* on other phones: searches every link at once and connects to the best hub (local network, mDNS, Wi‑Fi Direct, Wi‑Fi Aware, Bluetooth, in that order).
* Invitation links (`cuplaj://join?…`) configure address, key, PIN and room in one tap; they can only create spectators or actors.
* Wi‑Fi Aware (NAN) transport for phones that support it.
* Bluetooth scan lists only phones that actually run a CUPLAJ hub (SDP check); Wi‑Fi Direct scan lists only phones.

## 1.2.0 — 2026‑11‑24

* Generic transport layer: the hub accepts any byte channel, not only TCP (`Chan`, `StreamChan`, `Hub.adopt`); covered by JVM tests with simulated channels, including mixed TCP and channel calls.
* Bluetooth (classic RFCOMM, no pairing required): control phone listens, other phones scan and connect; audio is compressed automatically.
* Wi‑Fi Direct: the control phone creates a group, other phones find and join it; the hub is then found as on any local network.
* mDNS/DNS‑SD announcement and discovery (`_cuplaj._tcp`) alongside the UDP beacon.
* New permissions: Bluetooth (connect, scan, advertise), nearby Wi‑Fi devices, location on Android 12 and below (required by Android for scanning).

## 1.1.0 — 2026‑11‑24

* Internet and mobile data: `host:port` addressing, optional AES‑128‑GCM network key, G.711 μ‑law compression off the LAN, keepalive and idle timeout.
* Direct P2P audio over UDP with hole punching and relay fallback; control option off / local / anywhere.
* VoIP: SIP gateway on the control phone (digest auth, PCMU RTP, inbound and outbound, staff‑only outbound, call length limit).
* Mobile network: SMS gateway (send and receive), *real call* cue (dialler), Wi‑Fi / mobile indicator.
* Protocol v1.1 (backward compatible: plain clients still work on unencrypted hubs). 8‑language UI, README and guide updated.

## 1.0.0 — 2026‑11‑24

First public release; a rewrite of the 0.x prototype after an adversarial audit (see `docs/AUDIT.md`).

* Hub hardened: PIN, room code, session tokens, role enforcement, rate limits, bounded queues, device cap, ring timeout, persistence.
* Party line rules completed (neighbour listening, intervening, busy handling, silent monitor, click, rejection and no‑answer endings).
* New: groups, mute, forget, automatic pairs, battery levels, event log, hub memory reset.
* New cues: spoken voice (local TTS), sound effects, black screen, phantom call.
* New: script (GO / PLAY with delays, JSON import/export).
* New: answer/reject from the notification and lock screen.
* New: user interface, README and user guide in 8 languages (EN, RO, FR, IT, ES, PT, DE, HU); text size control.
* New: presentation site with an interactive party‑line demo; protocol and audit documents.
* CI: build APK and run hub tests on Ubuntu 26.04; GitHub Pages; manual release workflow (draft pre‑release).

## 0.x — prototype

Initial single‑hub prototype with three telephone eras and basic coupling.
