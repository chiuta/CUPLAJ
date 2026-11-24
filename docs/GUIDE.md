# CUPLAJ — User guide

**English** · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Version: 1.2.0 · Last update: 2026-11-24_

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

---

[← README](../README.md)
