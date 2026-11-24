# Changelog

Documentation language: English only.

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
