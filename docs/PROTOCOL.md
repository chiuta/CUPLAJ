# CUPLAJ protocol (v1)

Documentation language: English only (the app UI, README and user guide are available in 8 languages).

## Transport

* **Discovery** — UDP broadcast on port **8471**, every 2 seconds from the hub: `CUPLAJ1|<hub name>|<tcp port>|<locked 0/1>`. Clients hold a `MulticastLock` while listening. Typing the hub address by hand always works.
* **Session** — one TCP connection per phone to the hub, port **8470**.
* **Frame** — `type:1 byte | length:4 bytes big‑endian | payload`. Maximum payload 200 000 bytes; larger frames close the connection.
  * `type 1` — JSON (UTF‑8 object with a `t` field).
  * `type 2` — audio.
* **Audio** — 8 kHz, 16‑bit mono PCM, 20 ms chunks (160 samples = 320 bytes). Client → hub: `[mode:1][pcm]`, mode `0` call, `1` public address (staff only), `2` to staff. Hub → client: `[src:1][pcm]`, where `src` is a per‑session source index used by the receiving mixer.

## Handshake

Client → hub: `{"t":"hello","id":<device id>,"name":…,"role":"spectator|actor|regie","pin":…,"room":…,"tok":<session token>}`

Hub → client: `{"t":"welcome","number":…,"group":…,"tok":…,"hub":…}` or `{"t":"denied","why":"bad_room|bad_pin|dup|full"}` followed by close.

* `room` is checked if the hub has a room code. `pin` is required for `actor`/`regie` from other devices (the control phone itself connects over loopback). Spectators need no PIN.
* The first welcome for a device id issues a random `tok`; reconnecting with the same id requires the same `tok`, otherwise `dup` — so another phone cannot take over an existing identity.
* The hub holds at most 400 device records.

## Client → hub messages

| `t` | Fields | Meaning |
|---|---|---|
| `offhook` / `onhook` | | lift / replace the handset |
| `dial` | `number` | place a call (also used while on a party line) |
| `answer` | | answer a ringing call |
| `hangup` | | end the call (same effect as `onhook`) |
| `intervene` | `on` | a listener starts/stops speaking into the call |
| `msg` | `to`, `text` | message to a target |
| `cue` | `to`, `kind`, `text` | **control only** — send a cue |
| `monitor` | `number` | **control only** — silent listening |
| `couple` / `uncouple` / `autocouple` | `a`,`b` / `id` | **control only** — party‑line management |
| `setnum` / `setgroup` / `mute` / `forget` | `id`, … | **control only** |
| `opt` | `listen`, `click`, `peerMsg` | **control only** — global options |
| `peers` / `getlog` | | **control only** — request roster / event log |
| `stat` | `bat` | battery level (every 60 s) |

Targets (`to`): `all`, `staff`, `spectators`, `actors`, `num:<number>`, `id:<device id>`, `grp:<group>`.

Cue kinds: `text`, `vibrate`, `ring`, `flash`, `speaker`, `era`, `contact`, `say`, `sound`, `blackout`, `fakecall`. Non‑control senders are ignored for `cue`.

## Hub → client messages

`welcome`, `denied`, `line` (`line`, `slot`, `partner`), `peers` (roster for the control), `log` (event line for the control), `msg`, `cue`, `dialtone`, `ringback`, `busy`, `unknown`, `ring` (`num`, `name`, `pat` 0|1, `party`), `stopring`, `connected` (`peer`, `name`, `forNum`), `listening`, `ended` (`why`: `hangup`, `rejected`, `noanswer`, …), `click`.

## Party‑line rules

1. A coupled pair shares `line` N with slots 0 and 1; each subscriber keeps their own number.
2. A call to number X rings X and, if idle, X's neighbour; the ring pattern follows the *called* number's slot (`pat` 0 = one long ring, 1 = two short).
3. Whoever sends `answer` takes the call (`forNum` tells them which number was called); the other ring stops (`stopring`).
4. While one subscriber is in a call, the neighbour's `offhook` makes them a *listener* (`listening`); parties receive a `click` if enabled.
5. A listener may `intervene`; their audio is mixed to both parties.
6. A call to a number whose line is in use gets `busy`. An unanswered call ends after 40 s (`ended`/`noanswer`).
7. The control may `monitor` any number: it listens without a click.

## Limits

* 40 JSON messages and 90 audio frames per second per device; excess is dropped.
* A per‑connection writer queue of 400 items; a client that cannot keep up is disconnected (JSON) or loses audio frames.
* Messages are capped (160 characters in the 2003 era is enforced by the client UI; the hub independently truncates to 500).

## Security model

Plain TCP on a trusted local network. The room code and PIN are access gates, not cryptography; the session token prevents identity takeover. Do not run the hub on an untrusted network.
