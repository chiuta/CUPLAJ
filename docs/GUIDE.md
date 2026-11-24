# CUPLAJ — User guide

**English** · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Version: 1.7.0 · Last update: 2026-11-24_

## 1. Before the show

- Use one Wi‑Fi network for all phones, or the hotspot of the control phone. Disable *client isolation* on routers.
- Install the APK, grant microphone and notification permissions, and in the app's **Aa** menu choose *Keep the app alive* so the system does not put it to sleep.
- Charge all phones; the control shows battery levels and warns under 20 %.

## 2. Roles and numbers

- **Control** — the hub; sees everything, sends cues, couples phones. Number 100.
- **Actor** — needs the PIN; can broadcast to the crew and call anyone. Numbers 201 and up.
- **Spectator** — receives calls, messages and cues; can only speak to the crew. Six‑digit numbers.
- The control can change any number and assign groups (for example *balcony*, *cellar*).

## 3. Using the phone

- **1985** — lift the handset (button), wait for the tone, turn the dial; a number dials itself after a short pause.
- **1995** — lift, then use the keypad; the notebook lists numbers the control added.
- **2003** — type the number and press *Call*; messages are limited to 160 characters.
- A ringing phone can be answered from the notification, even on the lock screen.

## 4. Party line in practice

- Select two devices in the Control tab and press *Couple 2*, or press *Auto pairs* to pair all free phones.
- Use *Listen in* on the control to monitor a line without being heard.
- Options let you turn off neighbour listening, the click, or messages between phones.

## 5. Cues

- Choose a target (everyone, spectators, actors, selected devices, a group, one number), a kind and a text, then *Send cue*.
- Examples: `Mother;204455` adds a contact; `1;It is nearly midnight` turns the screen black with a faint text; `Dr. Voss;204455;Come to the cellar` makes a phantom call that speaks when answered.

## 6. Script

- Build the show as steps with a target, kind, text and delay. **GO** sends the next step; **PLAY** runs the rest automatically from the selected step.
- Export the script as JSON and import it on another control phone.

## 7. Troubleshooting

- *No hubs found* — check the same network, disable client isolation, or type the hub's address.
- *Wrong PIN / room code* — the control shows both on its start screen.
- *Port busy* — close any other app using port 8470.
- *Silent calls* — check the microphone permission and the loudspeaker toggle in the Voice tab.

## 8. Internet, VoIP and mobile network (1.1)

- **Network key** — on the start screen switch *Encrypted link* on; the control shows a key (xxxx‑xxxx‑xxxx‑xxxx). Type it in the *Network key* field on every other phone. Without the right key the hub refuses the connection. Use it whenever traffic leaves your local network.
- **From the internet** — forward TCP 8470 and UDP 8472 on the router to the control phone and give the public address (`host` or `host:port`) in the *Hub address* field. On mobile data nothing else is needed.
- **P2P** — in the Control tab the *Direct calls* option cycles off / local network only / anywhere. *Anywhere* lets two phones learn each other's public IP; the control cannot monitor, mute or break into a call while it is direct (it switches back to the relay automatically when it needs to).
- **SIP line** — Settings → *VoIP / SIP line…*: server, user, password, optional domain and the internal number for incoming calls (default 100). Restart the hub to apply. Staff dial `0` + external number; spectators cannot place outside calls. Each call is limited to 15 minutes. SIP and RTP are not encrypted.
- **SMS** — Settings → *SMS gateway* (needs a SIM and the SMS permissions). Staff pick *SMS to a real phone number* in the Messages tab; incoming SMS appear to staff as messages from *SMS*. Limit: 20 per minute. Operator charges apply.
- **Real call** — the *real phone call* cue opens the phone's dialler with the number. Android does not allow an app to route a cellular call into the party line, so GSM call audio cannot be bridged; use the SIP line for that.

## 9. Connecting without a router (1.2)

- **Same hub, any link** — calls, party line, cues, encryption and PIN work identically over every link; only the way the phone reaches the hub changes.
- **Hotspot, USB tethering, Ethernet** — any link that gives the phones an IP network works with no setup; use the address or let the hub be found.
- **Bluetooth** — on the control phone start the hub with *Bluetooth link: ON* and press *Make visible by Bluetooth*. On other phones press *Find via Bluetooth* and choose the control phone (🔵). Range is short and about 7 phones fit on one hub; voice is compressed automatically. Pairing is not required; access is still protected by the PIN, room code and network key.
- **Wi‑Fi Direct** — on the control phone start the hub with *Wi‑Fi Direct group: ON*. On other phones press *Find Wi‑Fi Direct* and choose the control phone (📡); the control phone may ask you to accept each phone. After joining, the hub appears in the list. Phones usually cannot stay on their normal Wi‑Fi while in a Wi‑Fi Direct group, and some models do not support the control phone being both.
- **mDNS** — the hub also announces itself by mDNS/DNS‑SD (🌐), which finds it on networks that block the usual broadcast.
- **Not included** — Wi‑Fi Aware (few phones support it) and Google's Nearby Connections (needs Google Play Services, which this trade‑free app avoids). Bluetooth Low Energy is too slow for voice.

## 10. Plug and play (1.3)

- **Control phone** — choose *Control*, then *Start the hub*. With *Plug & play* on (default) CUPLAJ opens Wi‑Fi / hotspot, mDNS, Bluetooth and Wi‑Fi Aware, and creates a Wi‑Fi Direct group when there is no Wi‑Fi network. Accept the single permission prompt. Settings shows the active links.
- **Other phones, easiest** — Settings → *Share invitation for spectators* (or *actors*) on the control phone; send the link by any app. Whoever taps it with CUPLAJ installed is configured (address, key, PIN, room) and connected.
- **Other phones, without a link** — choose a role and tap *Find and join automatically*. CUPLAJ searches every link at once (Wi‑Fi, mDNS, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware) and connects to the best hub it finds: local network first, Bluetooth last.
- **Wi‑Fi Aware** — phones that support it find each other with no router and no setup. Many phones do not support it; nothing breaks if yours does not.
- **Safety** — an invitation can only make a spectator or an actor, never a control phone. Links contain the key and PIN, so send them only to people you invite.

## 11. Hardened + AIR (1.4)

- **Safer hub** — 6‑digit random PIN, a fresh key for every connection, limits on unauthenticated connections and message size, lockout after repeated wrong PINs, and a confirmation before any invitation link is applied.
- **LUMEN (light)** — the control phone blinks its torch; another phone watches with its rear camera. Dim the room, 1–3 m, point the torch at the camera. Slow (a few seconds) but works through any wall of noise.
- **SONUS (sound)** — the speaker plays a tone code, near‑ultrasonic (inaudible to most adults) or audible; the other phone listens with its microphone. Carries cues and a hub invitation (address and PIN).
- **TACTUS (vibration)** — the vibration motor sends short and long pulses; another phone resting on the same surface feels them with its accelerometer. Works in silence and darkness, carries short cues.
- **Using it** — Settings → *Air links*: switch on the listeners you want (camera / microphone / accelerometer; the phone asks for permission), then on the control phone press a send button. Packets carry a short authentication tag from the network key and cannot be replayed.

## 12. Show control (1.5)

- **Shared instant** — begin the cue text with `@3.5;` to fire it 3.5 s from now on every phone at once. Phones sync their clock with the hub on connect and every minute; a cue more than 10 s late is dropped instead of firing out of date.
- **PANIC** — cue kind *PANIC: stop everything* silences ringing, vibration, speech, sound effects, black and colour screens, fake calls and pending timed cues. Also works over AIR.
- **Colour** — `#RRGGBB;seconds;pulses per second` fills the screen. Pulses are capped at 2 per second for photosensitive audience members; a long press on the screen closes it.
- **Poll** — `question;option 1;option 2;…` asks spectators and actors; each phone gets one vote (changeable). *End poll* shows the result to the control phone. Polls can be steps in the script.
- **Delivery** — after each cue the control phone shows *sent to N of M phones online*. **Hardware PTT** — Settings → *Hardware PTT key* turns the headset button and volume‑up into push‑to‑talk while the app is in front.

## 13. Media, OSC, priority (1.6)

- **PA priority** — while the control phone (or an actor) speaks to everyone, every other sound on the phones drops to 30 % and returns smoothly. Settings → *PA priority* switches it off.
- **Media cues** — on the control phone, Cue tab → *Send a file…* picks an image, video or audio file (up to 8 MB) and sends it in pieces to the selected phones; each phone checks size and checksum and ignores a damaged file. Then use the cue kinds *Image on screen*, *Video on screen* or *Audio clip* (leave the text empty to use the last file, or `code;seconds`). Long‑press closes an image or video. Phones that join later do not have the file: send it again.
- **OSC out** — Settings → *OSC*: every cue is also sent as `/cuplaj/cue <to> <kind> <text>` over UDP to a lighting or sound desk, QLab or Companion.
- **OSC in** — the desk can send `/cuplaj/<hub PIN>/go`, `/panic`, `/stop` or `/cue <to> <kind> <text>` to the control phone (default port 8473). The PIN in the address is the only protection: keep the show network private. At most 20 messages per second are accepted.
- **Not included** — iOS (Android only), MIDI, a program feed, and more than the existing group channels.

## 14. Smart show (1.7)

- **Zones** — put a phone in a room (or hold it in a character's pocket), Settings → *Air links* → *Zone beacon* and tap until it shows the zone number 1–9: it plays an inaudible‑to‑most near‑ultrasonic code every 7 s. Audience phones with *Listen: SONUS* on report which zone they hear; the control phone shows 📍 next to their name. Silence for 30 s means they left.
- **Rules** — control phone, Settings → *Rules*, one per line: `zone 3 once > self | text | Welcome, {name}`, `pollwin 1 > all | color | #00ff00;3`, `meter noise 70 > all | blackout | 1;`, `pollwin 2 > all | goto | Ending B`. *self* is the person who triggered the rule; `goto` jumps the script to a step by name or number, so a poll can choose the story.
- **Crowd meter** — in the cue tab: *Measure noise* or *movement* for 5 s on the selected phones. Each phone sends only a number 0–100, never sound. You see average, peak and how many answered, and rules can react.
- **Voice effects** — select phones in the control list, *Voice effect…*: robot, old radio, echo, deep, high, lo‑fi. The effect is applied on that phone before sending, so everyone who hears it gets the same voice.
- **Captions and languages** — cue kind *Caption* shows large subtitles; any text cue can carry `en=Hello|ro=Salut|fr=Bonjour` and each phone uses its own language (then English, then the first). `{name}` and `{number}` become the receiving phone's own name and number.

---

[← README](../README.md)
