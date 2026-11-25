# CUPLAJ 1.8 — an honest review

Written 2026‑11‑25 by the same assistant that wrote most of the code, so read it as a self‑audit, not as independent testing. Competitor facts come only from public store listings and articles (sources at the end) and may be out of date; where I could not verify something I say so.

## Verdict in five lines

* **Idea and scope: strong and genuinely unusual.** I found no other product that turns the audience's own phones into a *simulated party‑line telephone exchange* that a control phone can run, eavesdrop on and cue.
* **Engineering on paper: solid.** About 9,300 lines of Kotlin, an encrypted session (AES‑128‑GCM, PBKDF2 100 000, PIN), a hub with limits, more than 250 automated JVM checks, green CI.
* **Evidence on real hardware: none.** Not one feature has been run on a fleet of real phones. Audio latency, battery, Wi‑Fi Direct, Bluetooth, camera/torch, MIDI and Art‑Net with a real desk are all unproven.
* **Versus professional show control: not a replacement.** QLab, MiX16, SOLOstudio and Unity Intercom are mature, field‑proven and (mostly) iOS/Mac. CUPLAJ complements them (OSC, MIDI, Art‑Net out) rather than competing.
* **Biggest practical risks:** Android only; the hub is one phone (a single point of failure); no field test; translations unreviewed; feature count has outrun testing.

## What it is compared with

These products solve neighbouring problems. Nothing below is a ranking.

| Group | Examples | What they do | Overlap with CUPLAJ |
|---|---|---|---|
| Operator cue players | QLab (+ QLab Remote), Go Button, MiX16 Show Cue, SOLOstudio | Play audio/video/lighting cues from the operator's own device; MIDI/OSC out | Cue list, GO, panic, OSC/MIDI. But they play on *their* speakers, not on the audience's phones |
| Audience‑phone shows | Wiretapper, Augmented Theatre, Livecube | Audience installs an app; the show reaches headphones or screens | Light/sound/text cues on phones. No simulated telephone network |
| Intercoms | Unity Intercom, Zello | Push‑to‑talk and channels for crews | Voice groups. Not theatre cues |
| Assistive audio streaming | Sennheiser MobileConnect, LimeOnAir (named in a QLab forum thread) | Stream one audio feed to phones | One‑way audio only |

## Feature‑by‑feature, honestly

Legend: ✅ clearly better or unique · ➖ comparable · ❌ clearly weaker or missing · ❓ cannot judge without field testing.

| Area | CUPLAJ | Best alternative | My judgement |
|---|---|---|---|
| Party‑line telephony (ring, dial, coupled lines, eavesdrop) | Core feature, tested in software only | Nothing comparable found | ✅ unique, ❓ audio quality over a crowded Wi‑Fi unknown |
| Running with no router | Hotspot, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware, plus AIR (light/sound/vibration) | Others assume a normal network | ✅ ambitious, ❓ Bluetooth/Wi‑Fi Direct behaviour varies a lot by phone model and Android version |
| Cue breadth (24 kinds, timed `@sec;`, polls, zones, rules, wave, chain) | Very wide | Augmented Theatre, MiX16 (media‑centric) | ✅ breadth, ❌ no visual timeline editor; the cue list is text‑ and button‑driven |
| Audio/video/image cues | Via hub, ≤ 8 MB, only to phones online at send time | Augmented Theatre (stated to handle media well) | ➖ late‑joiners miss media |
| Shared‑instant timing | Clock sync, `@sec;` | QLab on one machine is trivially in sync; phone fleets are not | ❓ the real test is audible drift across 30+ phones, never measured |
| Lighting/sound desk integration | OSC in/out; Art‑Net/DMX and MIDI out | QLab, MiX16, SOLOstudio: mature MIDI/OSC | ❌ packet formats tested, no real desk; DMX/MIDI run only on the control phone |
| Scale | One phone as hub | Unity Intercom lists up to 128 channels / 63 users (store page) | ❓ no measured ceiling; Android hotspots often cap clients |
| Reliability model | Foreground service + wake lock | Desktop servers | ❌ if the hub phone dies, the show stops; no hot standby |
| Security | PIN + PBKDF2 + AES‑GCM, size limits | n/a | ➖ sensible for a closed event; AES‑128 and a 6‑digit PIN are fine for a venue, not for hostile networks. Not independently audited |
| Accessibility / captions | Per‑language captions, haptic and light cues | Little found elsewhere | ✅ good intent, ❓ not tested with real users |
| Audience games (whisper chain, split secret, ghost tape, report) | Present | Not found elsewhere | ✅ original, ❓ untested on phones; the "tape" and "chain" are the likeliest to show bugs |
| Platforms | Android only | Most competitors: iOS and/or Mac | ❌ large practical gap; many audiences are iPhone‑heavy |
| Onboarding | Invitation by light, sound, QR, link | Store apps with one tap install | ❌ a sideloaded APK is a real barrier for an audience; no Play Store listing |
| Licence / cost | MIT, free, no accounts | Mostly paid or in‑app purchases | ✅ |
| Documentation | 8 languages, guides, protocol spec, audit | Varies | ✅ unusually thorough; ❓ translations unreviewed by native speakers |
| Testing | 250+ JVM checks, CI green | Mature products have years of field use | ❌ no instrumented or device tests at all |

## Where I think the weaknesses really are

1. **Everything physical is unproven.** The tests exercise logic (framing, crypto, rules, encoders, fader maths). They cannot say whether phones ring on time, whether audio stutters, or whether a venue's Wi‑Fi copes.
2. **Feature creep.** Version 1.8 added DMX, MIDI, Morse, tape, chain, secret and report. Each is small and tested in isolation, but the product now has more surface than any one rehearsal can cover. A director will use perhaps five cue kinds.
3. **Single hub, no failover.** Operator tools survive a dead laptop by having a second one. Here a dead or sleeping hub phone ends the show.
4. **Android‑only and sideloaded.** For many productions this alone decides adoption.
5. **Usability is unknown.** The UI is built programmatically and has not been put in front of a stage manager. Dense settings and a 24‑kind cue picker may overwhelm non‑technical hosts.
6. **Self‑review bias.** The audit and this review were done by the author of the code. They found real bugs (tape frame type, unbounded waves, silent no‑op rule actions), which is a hint that more remain.

## Where it is genuinely good

* A coherent, original concept executed end to end, from protocol to docs.
* Offline‑first and trade‑free: no accounts, no cloud, MIT.
* Careful limits and failure handling in the hub (frame caps, rate limits, bounded timers), and the habit of rejecting impossible actions rather than ignoring them.
* A written protocol (`docs/PROTOCOL.md`) that lets others build clients or bridges.

## Suggested next steps, in order of value

1. A **rehearsal with 10–20 real phones** on one hotspot, then on a router; record latency, drops, battery.
2. Test **one real desk** over Art‑Net and one MIDI device.
3. Put the control UI in front of **one stage manager** and watch.
4. Decide a **small default cue set** and hide the rest behind "advanced".
5. Native‑speaker review of the translations; consider an iOS companion or a web client for audience phones.

## Sources

* QLab Remote, App Store: https://apps.apple.com/us/app/qlab-remote/id642520727
* QLab overview: https://en.wikipedia.org/wiki/QLab
* Go Button (from the makers of QLab), App Store: https://apps.apple.com/mm/app/id454017315
* MiX16 Show Cue, App Store: https://apps.apple.com/us/app/mix16-show-cue/id1617084964
* SOLOstudio, App Store: https://apps.apple.com/app/id1668634437
* Wiretapper (audience app), Time Out and The Nudge: https://www.timeout.com/london/theatre/wiretapper , https://thenudge.com/?p=4424
* QLab forum thread on cued feeds to audience phones: https://groups.google.com/g/qlab/c/oUyhk3DP3eo
* Earlier comparison and further sources: `docs/COMPARISON.md`
