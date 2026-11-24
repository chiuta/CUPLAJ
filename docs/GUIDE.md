# CUPLAJ — User guide

**English** · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Version: 1.0.0 · Last update: 2026-11-24_

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

---

[← README](../README.md)
