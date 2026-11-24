# CUPLAJ protocol (v1.3)

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

## v1.1 extensions

**Frame types.** `1` JSON, `2` PCM16 audio, `3` μ‑law audio (160 bytes = 20 ms). Audio payload: `[src][data]`. Bit `0x80` on the type byte (`FLAG_ENC`) marks an encrypted frame.

**Encryption.** Key = PBKDF2WithHmacSHA1(passphrase, salt `CUPLAJ-v1`, 10 000 iterations, 128 bit). Frame body = `nonce(12) | AES‑GCM(ciphertext+tag)`; nonce = 4‑byte prefix (first byte 1 = hub, 2 = client) + 8‑byte big‑endian counter. Receivers reject a wrong direction byte and non‑increasing counters. The hub may answer `denied bad_key` in clear.

**Keepalive.** Client sends `{"t":"ping"}` every 15 s, hub answers `{"t":"pong"}`; hub closes connections idle for 60 s.

**P2P.** UDP rendezvous on `PORT+2` (8472). Client sends `hello` with `p2p:true`, registers over UDP with `id` and session token, and reports `{"t":"udp","lan":"ip:port"}`. On a call the hub sends `{"t":"p2p","key":hex16,"cand":["lan","observed"],"init":bool}`; peers probe the candidates and send μ‑law audio encrypted with the per‑call key. `{"t":"p2pstop"}` returns audio to the relay. Control option `opt.p2p`: 0 off, 1 LAN only, 2 anywhere.

**Gateway role.** A client with role `gateway` (number 90) is a SIP bridge; it requires the PIN unless local. Staff dial `0`+number; the hub sends `ring` with `dest` to the gateway. `unknown` is returned to spectators.

**SMS.** Staff → hub `{"t":"sms","to","text"}`; hub → gateway `{"t":"smsout",...}`; hub → staff `{"t":"smsres","ok","why"}` (`bad_number`, `no_gateway`, `limit`). Gateway → hub `{"t":"smsin","from","text"}`, delivered to staff as `msg` with role `sms`. `hello.smsgw:true` registers a gateway.

## v1.2: transports

The framing and messages are identical on every transport; only the byte channel differs.

| Transport | How the client reaches the hub |
| --- | --- |
| TCP | `host[:port]`, default port 8470 (LAN, hotspot, USB tethering, Ethernet, Wi‑Fi Direct group at 192.168.49.1, internet) |
| Bluetooth | classic RFCOMM, insecure service record, UUID `7c6a0c5e-3d10-4f6e-9a55-2b1c8f0d4a71`; addressed as `bt:AA:BB:CC:DD:EE:FF` |

Discovery: UDP beacon `CUPLAJ1|name|port|room?|key?` every 2 s on port 8471; mDNS/DNS‑SD service `_cuplaj._tcp`; Bluetooth device scan; Wi‑Fi Direct peer discovery.

Idle limits apply per channel: 10 s to send `hello`, 60 s of silence afterwards (clients ping every 15 s). Bluetooth clients send μ‑law audio.

## v1.3: invitation link and Wi‑Fi Aware

**Invitation:** `cuplaj://join?r=<spectator|actor>&h=<host[:port],…>&k=<key>&p=<pin>&c=<room>&n=<hub name>` (all values URL‑encoded, every field except `r` optional, at most 8 hosts, 800 characters). `r=regie` is rejected. Without `h` the client starts an automatic search.

**Wi‑Fi Aware:** the hub publishes service `cuplaj` and listens on a TCP port inside the Aware data path; the client subscribes, sends a message to the publisher, receives the peer IPv6 address and port and opens an ordinary TCP channel (same framing). The data path is protected with a fixed passphrase; admission is still the hub's job.

**Auto‑join priority:** local network, mDNS, Wi‑Fi Direct group, Wi‑Fi Aware, Bluetooth.

## v1.4: session keys, limits and CUPLAJ‑AIR

**Handshake.** A keyed client first sends a plain `T_CHAL` frame with a 16‑byte nonce `cn`; a keyed hub answers `T_CHAL` with `sn`. Session key = first 16 bytes of HMAC‑SHA256(master, "CUPLAJ-sess" ‖ sn ‖ cn). Frames are AES‑128‑GCM with nonce = direction byte (1 hub, 2 client) ‖ counter; receivers reject reflected or non‑increasing counters. A wrong or missing key gets a plain `denied bad_key`; a keyed client on an unkeyed hub gets `denied no_key`. Master key: PBKDF2‑HMAC‑SHA1, 100 000 iterations, salt `CUPLAJ-v1`.

**Limits.** 2048 bytes per frame before login, 32768 after; 64 concurrent unauthenticated connections; 60 per origin; 6 failures in 60 s lock an origin for 60 s.

**Air packet.** `[ver(4)|type(4)] [seq] [len] [data…] [tag]`, tag = first 2 or 4 bytes of HMAC‑SHA256(network key, header ‖ data); invitations use a fixed public key. Type 1 = cue (`[kind index][text]`), type 2 = invitation (IPv4 ‖ 24‑bit PIN).

**LUMEN.** Slots of 100 ms: 2 off, Barker‑13 (1111100110101), then each bit as a Manchester pair (1 = on‑off), 2 off. Receiver averages the central 60 % of each slot of the camera's mean luminance, searches 31 clock skews (±6 %) and 10 phases, requires Barker match and contrast, reads bits differentially.

**SONUS.** 8‑FSK, 3 bits per 50 ms symbol; tones 17.0–19.8 kHz in steps of 400 Hz (near‑ultrasonic) or 1.8–3.9 kHz in steps of 300 Hz (audible); preamble is a zig‑zag of all eight tones twice; 5 ms cosine ramps. Receiver runs Goertzel on a sliding window (hop = 1/8 symbol), scores the preamble, then reads symbols.

**TACTUS.** Vibration bursts: long ≈ 240 ms = 1, short ≈ 100 ms = 0, gap ≈ 100 ms; preamble long, long, long, short. Receiver removes gravity with a moving average, takes a 40 ms envelope, applies hysteresis thresholds, measures burst lengths relative to the preamble.

## v1.5: shared clock, polls, acknowledgements

* `sync`: client sends `{"t":"sync","c":<client ms>}`, hub answers `{"t":"sync","c":<same>,"s":<hub ms>}`. Offset = s − (t0+t1)/2 of the sample with the lowest round trip among the last 8 (samples above 5 s are ignored). Sent five times after login and every 60 s.
* `cue` (control only) may carry `at` (hub ms) and `id` (≤ 16 chars). The hub relays it unchanged and answers the sender `{"t":"cueack","id","sent","online"}`. A client waits until `at`; later than 10 s or more than 10 min ahead → dropped.
* `poll` (control only): `{"to","q"≤120,"opts"[2..6 × ≤40],"id"?}`. The hub sends it to the non‑staff recipients and actors, one poll at a time. `vote`: `{"id","o"}`, accepted only from a recipient, for the open poll, with a valid option; last vote counts. The hub pushes `pollres` `{id,q,opts,counts,n,of,open}` to staff after each change and when `pollend` closes it.
* New cue kinds: `panic`, `color` (`#RRGGBB;seconds;hz`, hz ≤ 2), `poll`, `pollend`.

## v1.6: media, OSC, PA source

* `media` (control only): `{"to","mid"[a-z0-9]{1,16},"kind":"image|video|clip","i","n","b64"≤17000 chars}`, first piece also `size` (≤ 8 MB) and `sha` (SHA‑256 hex). The hub relays pieces unchanged to the targets and keeps nothing. Clients accept pieces strictly in order, at most 3 open transfers, verify size and hash, store as `<mid>.img|.vid|.snd`; a bad piece drops the transfer. Cues `image`, `video`, `clip` carry `mid[;seconds]`.
* PA audio (mode 1) is forwarded with source byte 254; receivers scale all other sources by 0.3 (fast attack, 160 ms hold, slow release).
* OSC 1.0 over UDP (string, int, float): out `/cuplaj/cue to kind text`; in `/cuplaj/<hub PIN>/go`, `/panic`, `/stop`, `/cue to kind text`; the PIN is compared in constant time, bundles are ignored, packets over 2 KB are dropped.

## v1.8: stage tools

* `cue` may carry `wave` (ms, 0–3000); the hub sends it to the recipients in number order with that spacing and removes the field. Client side: text prefix `~ms;` after `@sec;`.
* `chain` (control only): `{text≤200, nums:[≤20 numbers]}` → first phone gets `chainmsg {id,text,hop,of}`, answers `chainfwd {id,text}`; only the phone whose turn it is is accepted; offline phones are skipped; at the end control phones get `chainres {id,orig,hops[]}`.
* `secret` (control only): `{phrase≤120 (≥2 words), to}` → each recipient gets one word as a `text` cue (`from:"secret"`); `guess {text}` from a recipient → `guessres {ok}`; first correct (normalised: lower case, ASCII letters and digits only) → `secretres {by,phrase}` to staff, rule trigger `solved`; 20 guesses per phone.
* `tape` (control only): `{a:"rec",id,secs 1..90}`, `{a:"stop"}`, `{a:"play",to}`; recorded audio frames are replayed 20 ms apart with source byte 253.
* `report` (control only) → `reportres {md}`.
* Local cue kinds (never sent to the hub): `dmx`, `midi`. Relayed kinds added: `morse`, `house`. Art‑Net: `Art-Net\0`, OpDmx 0x5000, protocol 14, sequence, universe (15 bits), length (even).

## v1.7: zones, rules, meter, voice effects, languages

* AIR type 3 (`T_ZONE`): data = 1 byte zone id (1–99), 4‑byte tag with the network key; emitted every 7 s over SONUS by a beacon phone. Receivers send `{"t":"zone","z":n}` when the zone changes (hub accepts one change per 2 s per phone, 0..99) and `z:0` after 30 s of silence. The hub zeroes a phone's zone when it disconnects.
* `rules` (control only; answered with `rulesok {n}`): `{"list":[{id,on:"zone|pollwin|meter|solved",arg,mk,to,kind,text,once}]}`, ≤ 50; kinds exclude `poll`, `pollend`, `realcall`; targets all/staff/spectators/actors/self/num:/id:/grp:. A rule fires at most once per 3 s per (rule, phone), or once ever with `once`. `pollwin` fires only for a unique winning option. `goto` sends `{"t":"goto","step"}` to the control phone.
* `meter` (control only): `{kind:"noise|motion",secs 1..30,to}` → recipients get `meterreq`, answer `meterval {id,v 0..100}`; the hub pushes `meterres {avg,max,n,of,open}` and closes after secs + 3 s, then evaluates `meter` rules on the average.
* `setfx` (control only): `{id,k}`, k in robot, radio, echo, deep, high, lofi or empty; the hub sends `fx` to that phone and includes it in `welcome` and the peer list.
* Cue text: `pickLang` (all parts must be `xx=…`), then `{name}` and `{number}` are replaced locally.
