# CUPLAJ compared with similar tools

Written 2026‑11‑24 from the public store listings and articles named below. These products solve neighbouring problems, so the table shows what each one is for, not a ranking. Competitor facts come only from those pages and may have changed since; cells marked *Not checked* or *Not stated* were not verified.

| | CUPLAJ 1.8 | Unity Intercom | Zello | Augmented Theatre | QLab Remote / SOLOstudio / MiX16 | Livecube |
|---|---|---|---|---|---|---|
| Main purpose | Immersive theatre: party‑line phones + cues, on the audience's own phones | Production intercom over IP | Push‑to‑talk | Audience phones as stage design | Show‑control cue lists | Audience participation |
| Needs a server / dashboard | Control phone is the hub (Android) | Unity Intercom Server | Not checked | Dashboard for Mac / iPad | QLab on a Mac / the app itself | Local Wi‑Fi and a web address |
| Account needed | No | Not stated | Not checked | No (per its listing) | Not stated | No (web address) |
| Works with no Wi‑Fi router | Yes: hotspot, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware; even torch, sound or vibration (AIR) | Needs an IP network | Not checked | Not stated | Needs the Mac on the network | Local Wi‑Fi |
| Simulated telephone line, ringing, dial, coupled lines | Yes (core feature) | No | No | No | No | No |
| Light / sound / vibration / text cues on phones | Yes | Tally only | No | Yes (also video, stills) | Plays media from the operator device | Colours and content on phones |
| Cue list with delays, import / export | Yes | No | No | Dashboard | Yes (their core) | Not stated |
| PA priority (other sounds duck) | Yes (1.6) | "priority ducking" slider (store notes) | Not checked | Not stated | n/a | n/a |
| Image / video / audio cues delivered to phones | Yes (1.6, ≤ 8 MB, through the hub) | No | No | Yes | n/a | Content on phones |
| OSC / MIDI to lighting and sound desks | OSC in and out (1.6); MIDI and Art‑Net/DMX out (1.8, untested with hardware) | Not stated | No | Not stated | MIDI and OSC cues | Not stated |
| Location zones, rules, crowd meter, voice effects, per‑language captions | Yes (1.7; zones by sound, no GPS) | No | No | Not stated | No | Audience input only |
| Cues fire at one shared instant (clock sync) | Yes (1.5) | Not stated | No | Yes ("in step") | n/a | Not stated |
| Panic / stop everything | Yes (1.5), also over AIR | n/a | n/a | Not stated | Yes (SOLOstudio) | Not stated |
| Audience polls / votes | Yes (1.5) | No | No | Not stated | No | Yes (core) |
| Delivery confirmation (sent to N of M) | Yes (1.5) | n/a | Not checked | Not stated | n/a | Not stated |
| External PTT button | Headset button / volume key (1.5) | Bluetooth PTT button | Bluetooth devices | n/a | n/a | n/a |
| Phone calls / SMS / SIP bridge | SIP gateway, SMS gateway | External com system bridge | No | No | No | No |
| Audience games: whispering game, split secret, ghost tape, post‑show report (1.8) | Yes (not tested on phones) | Not stated | No | Not stated | No | Not stated |
| Platforms | Android | iOS (and server) | Android, iOS, PC, Mac | iOS | iOS, Mac | Web |
| Licence | MIT, free | Free app, paid server tiers | Not checked | Free app | Free + in‑app purchases | Not checked |

## What CUPLAJ does that the others do not

Fake party‑line telephony as theatre (coupled lines, distinct ringing, eavesdropping and intervening by the control phone), and cues that still work with no network (AIR: LUMEN, SONUS, TACTUS). Judged only by the pages listed below, none of the others offers zones that follow the audience by sound, a message that is deformed from phone to phone, or a secret split over phones — but this is absence from their public descriptions, not proof.

## What others do that CUPLAJ does not (yet)

* **iOS.** CUPLAJ is Android only.
* **Proven lighting and MIDI integration** (MiX16, SOLOstudio Pro, QLab): CUPLAJ sends Art‑Net / DMX and MIDI since 1.8, but only the packet formats are tested, not real desks or MIDI devices.
* **Program feed, many channels, tally from video switchers** (Unity Intercom: up to 128 channels, 63 users by default); CUPLAJ has groups, not independent channels.
* **Media that reaches phones joining late, and synchronised video playback** (Augmented Theatre); in CUPLAJ a file goes only to phones online when it is sent, and video starts when the cue arrives (use `@seconds;` for a shared start).
* **General push‑to‑talk messaging across PC, Mac and phones** (Zello, which its reviewer describes as walkie‑talkie style audio and text messages rather than a true intercom). CUPLAJ is not a general messenger.

## Sources

* Unity Intercom, App Store listing: https://apps.apple.com/us/app/id897632774
* Zello versus Unity, Media & Learning: https://media-and-learning.eu/type/tools/zello-intercom-over-ip/
* Augmented Theatre, App Store listing: https://apps.apple.com/app/id1135946050
* QLab Remote, App Store listing: https://apps.apple.com/us/app/qlab-remote/id642520727
* SOLOstudio, App Store listing: https://apps.apple.com/cd/app/solostudio/id1668634437
* MiX16 Show Cue Pro, App Store listing: https://apps.apple.com/app/id1617084964
* Livecube in *Theater au Lait* coverage (CNBC, Daily Dot): https://www.cnbc.com/2014/10/31/musical-theater-theres-an-app-for-that.html
* LAIT, University of Illinois: https://lait.ncsa.illinois.edu/?p=470
