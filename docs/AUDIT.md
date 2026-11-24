# Adversarial audit — CUPLAJ 1.0.0

Documentation language: English only. Date: 2026‑11‑24. Scope: the design and code were attacked from the viewpoint of a hostile participant, a faulty device and a sloppy operator; every item below led to a change in 1.0.0 unless marked open. Items marked **open** are known limits that were *not* fixed.

Method: code review of all sources, then JVM integration tests of the hub with simulated phones (about 55 checks, `app/src/test/…/HubTest.kt`) that reproduce each attack. Type‑checked against the Android 14 SDK. **Not** verified: behaviour on physical devices (audio latency, echo, OEM battery killers).

## A. Access and identity

| # | Attack / failure | Fix in 1.0.0 |
| --- | --- | --- |
| A1 | A spectator claims the role `regie` or `actor` | PIN required for `actor`/`regie` from non‑loopback; unknown roles degrade to spectator |
| A2 | Another phone connects with an existing device id and takes over its number | per‑device session token; mismatch → `denied: dup` |
| A3 | A neighbouring venue's phones join the show | optional room code (`bad_room`), hub beacon shows 🔒 |
| A4 | Spectator sends `cue`, `couple`, `setnum`, `monitor`… | all control messages are ignored unless sent by a `regie` peer |
| A5 | Spectator uses the public‑address audio mode | PA mode (1) accepted from staff only; spectators limited to modes 0 and 2 |
| A6 | Spoofed sender number/role in messages and cues | the hub stamps number and role itself (the display name is chosen by each device — see open item 8) |

## B. Availability

| # | Attack / failure | Fix |
|---|---|---|
| B1 | Message flood from one device | 40 JSON/s and 90 audio frames/s per device; excess dropped |
| B2 | Slow client stalls everyone (blocking writes) | per‑connection writer thread with a 400‑item queue; overflow disconnects the slow client |
| B3 | Oversized frame / garbage length | frames above 200 000 bytes close the connection |
| B4 | Unlimited device records | at most 400 records (`denied: full`) |
| B5 | Call rings forever | 40 s ring timeout → `ended/noanswer`, busy tone to the caller |
| B6 | Hub restart forgets numbers and couplings | state persisted on the control phone and restored |
| B7 | Port already used (second hub) | error surfaced to the operator |
| B8 | Phone sleeps / is killed in the background | foreground service with microphone + connected‑device types, wake and Wi‑Fi locks, battery‑optimisation prompt, automatic reconnect |
| B9 | Duplicate / ghost sessions after reconnect | old connection replaced, party line detached cleanly |

## C. Party‑line semantics

| # | Scenario | Fix / test |
|---|---|---|
| C1 | Both neighbours answer at once | first `answer` wins; the other receives `stopring` |
| C2 | Neighbour lifts during a call | becomes a listener; click to the parties; can be disabled |
| C3 | Caller dials own neighbour | busy |
| C4 | Called party's neighbour is in a call | caller hears busy; no ghost ring |
| C5 | Parties hang up while a listener is attached | listener is released and returns to idle |
| C6 | Uncoupling during a call | call ends cleanly, no stuck "busy" |
| C7 | Rejection by the ringing phone | caller hears busy (new `rejected` reason) |

## D. Client robustness

* UI thread safety: the discovery list is mutated on the UI thread only.
* Reconnecting clients re‑send `hello` with the stored token for that host.
* Ring accepted while idle, busy‑tone or unknown‑number tone (previously ignored).
* Microphone only runs while it is needed (PTT or a live call); a voice‑activity gate suppresses silence.
* Permissions: service restarted after the microphone permission is granted; notification actions answer or reject from the lock screen.

## E. Open items (honest list)

1. **Encryption is optional (since 1.1).** Without a network key traffic is plain; with it, every frame is AES‑128‑GCM with direction‑bound nonces and replay rejection. The key is derived from a passphrase with PBKDF2 (10 000 iterations); a weak or shared key is only as strong as its secrecy.
2. **Single point of failure:** if the control phone dies, all calls stop (state is restored on restart, calls are not).
3. **No device testing yet.** Audio mixing, echo cancellation on speakerphone, and latency on real hardware are unmeasured; a handful of phones should be rehearsed before a show.
4. **No load testing beyond simulation.** The 400‑device cap is a safety limit, not a claim of tested capacity.
5. **Release APK is signed with the debug key** (installable directly; not suitable for the Play Store).
6. The protocol document, audit and changelog are English only; the app, README and user guide are in 8 languages.
7. Android versions: minSdk 24; behaviour of full‑screen call notifications varies by vendor.
8. **Display names are self‑declared.** A spectator can type a name such as "Regie"; numbers and roles shown next to messages are hub‑stamped and trustworthy.

## F. Version 1.1 risks and mitigations

1. **P2P bypasses the hub.** Direct audio cannot be monitored, muted or broken into; the hub stops P2P (`p2pstop`) when it needs to, and keeps hub‑local phones and non‑P2P clients on the relay. Mode *anywhere* reveals each phone's public IP to its peer; default is local network only.
2. **SIP toll fraud.** Outbound calls are staff‑only, 15‑minute maximum, and inbound SIP is accepted only from the configured server's IP. SIP signalling and RTP are unencrypted; the provider password travels as a digest only.
3. **SIP NAT traversal is minimal** (rport and registration refresh only); providers needing STUN/ICE or SRTP are not supported.
4. **SMS cost and abuse.** Staff only, 20 per minute, number and length validation, text sanitised before display, only the registered gateway can inject incoming SMS.
5. **Cellular call audio cannot be bridged.** Stock Android gives apps no access to GSM call audio; the *real call* cue is only a dialler handoff.
6. **Not tested on real networks:** NAT types, mobile carriers (CGNAT may block P2P), SIP providers, SIM cards, battery cost of keepalive. Hub logic is tested on a JVM against a simulated SIP server.
7. Port forwarding (TCP 8470, UDP 8472) exposes the hub to the internet; always enable the network key and PIN in that case.

## G. Version 1.2 risks and mitigations

1. **Bluetooth uses the insecure RFCOMM profile** (no pairing), so any nearby phone can open a channel. Admission still needs the PIN, room code and, if enabled, the network key; with the key on, traffic is AES‑GCM encrypted. Bluetooth is only listening while the hub runs with the option on.
2. **Idle detection on non‑TCP channels** is done by a watchdog thread (no socket timeout exists); a silent channel is closed after the same limits as TCP (10 s before hello, 60 s afterwards).
3. **Wi‑Fi Direct group invitations** may need a manual accept on the control phone; many phones cannot use a normal Wi‑Fi connection and a Wi‑Fi Direct group at once. Not tested.
4. **Bluetooth capacity** is limited by the piconet (about 7 simultaneous links) and range; CUPLAJ does not fall back to another link automatically.
5. **mDNS** advertises the hub name on the local network; disable the option by not using the hub name for anything sensitive.
6. Permissions grew: Bluetooth, nearby Wi‑Fi devices and, on Android ≤ 12, location (Android requirement for scanning; CUPLAJ never reads the location).
7. **Not tested on real devices**; the channel layer is covered by JVM simulation only.
