# Link protocol

The full specification of the laptop ⇄ master tablet link (version 1). For a
gentler explanation, read [Desktop link (LoRa and TCP)](../how/desktop-link.md)
first.

This page is the **only** copy of the specification. If you change the
protocol, change this page in the same commit as the code.

How the desktop companion app and the master tablet exchange scouting
records. It was designed for a pair of Waveshare USB-TO-LoRa (SX1262)
dongles in transparent "stream" mode, and runs unchanged over TCP when no
dongles are available (see *Transports* below). Both implementations
must match this page:

- Python: `companion/chicken_companion/protocol/`
- Dart: `lib/lora/`

`companion/protocol_vectors.json` holds byte-exact examples that both test
suites check.

## Ground rules

- **The desktop starts every exchange; the tablet only answers.** One radio
  channel is shared and the radios are half duplex, so only one side ever
  needs to decide when to talk.
- **Frames are small.** An encoded frame is at most 210 bytes, so it fits in
  one 240-byte LoRa packet, and a burst of frames fits in the dongle's
  960-byte buffer.
- **Senders pace themselves.** After writing a frame, wait about as long as it
  takes to send over the air (`bytes × 8 / air_bps`, plus margin) before
  writing the next. Both sides share the `air_bps` setting.

## Transports

| | LoRa | TCP |
|---|---|---|
| Carrier | USB serial to the dongle, 115200 8N1 | One TCP connection |
| Who connects | Both open their dongle | The tablet listens (default port 5810); the desktop connects |
| Pacing | Yes, by estimated airtime | None |
| Loss | Whole frames can be lost; retries recover | Never lost; retries never fire |

The bytes are identical on both: the same COBS frames, messages and
exchanges. Over TCP the tablet accepts one desktop at a time; a new
connection replaces the old one.

## Framing

On the serial line, each frame is

```
COBS( header | payload | crc16 ) 0x00
```

- **COBS** (Consistent Overhead Byte Stuffing) removes every `0x00`, so `0x00`
  marks the end of a frame. A receiver can start listening mid-stream, drop
  garbage, and resync at the next `0x00`.
- **crc16** is CRC-16/CCITT-FALSE (poly `0x1021`, init `0xFFFF`, no reflection,
  no final XOR) over `header | payload`, big-endian. Frames with a bad CRC are
  dropped silently.

Header (8 bytes, big-endian integers):

| Offset | Size | Field | Meaning |
|---|---|---|---|
| 0 | 1 | version | `1` |
| 1 | 1 | type | message type (below) |
| 2 | 2 | session | chosen by the desktop for each exchange; the tablet echoes it. Frames with another session are ignored. |
| 4 | 2 | transfer | transfer id for DATA / ACK / ACK_REQ, else 0 |
| 6 | 1 | index | fragment index for DATA, else 0 |
| 7 | 1 | count | fragment count for DATA, else 0 |

The payload is at most **190 bytes**.

## Message types

| Type | Name | Direction | Payload |
|---|---|---|---|
| `0x01` | POLL | desktop → tablet | JSON `{"since": <rev>, "db": "<tablet db id or empty>"}` |
| `0x02` | STATUS_REQ | desktop → tablet | empty |
| `0x03` | STATUS | tablet → desktop | JSON `{"db", "name", "rev", "records", "events": [..], "admin": <settings version>}` |
| `0x10` | DATA | either | one fragment of a bundle |
| `0x11` | ACK | receiver → sender | `count` byte, then a bitmap of received fragments (bit `i` of byte `i/8`, LSB first) |
| `0x12` | ACK_REQ | desktop → tablet | empty: "send me an ACK for this transfer" |
| `0x7F` | ERROR | tablet → desktop | JSON `{"message": ".."}` |

JSON payloads are UTF-8 with no extra spaces.

## Bundles

A bundle is a batch of records sent as one transfer:

```
bundle = zlib( JSON )            (zlib format, RFC 1950)
```

It's split into fragments of at most 190 bytes, with at most **64** fragments
per bundle (about 12 KB compressed). The JSON is one of:

- Poll reply: `{"kind":"records","db":"<tablet db id>","rev":<highest rev
  included>,"more":<bool>,"admin":<settings version>,"devices":[{"id",
  "name","station"}...],"records":[<record>...]}`. `devices` lists the
  tablet itself, then every tablet it has seen over Bluetooth.
- Restore: `{"kind":"restore","records":[<record>...]}`
- Schedule: `{"kind":"schedule","schedule":{"event":"2026mitr2","name":..,
  "hash":..,"matches":[{"level":"qm","n":1,"red":["frc217",..],
  "blue":[..]}...]}}`
- Admin settings: `{"kind":"admin","config":{"version":<ms>,
  "team_number":"217","season":"2026","current_event":"2026mitr2",
  "passcode":<int or null>,"scouts":["Last, First",..],
  "assignments":{"<device id>":{"station":"Red 1","score_table_side":false}},
  "events":[{"key","name","hash"}]}}`

`<record>` is the tablet's `ScoutRecord.toJson()`: `id`, `event_key`,
`match_number`, `team_number`, `scout_name`, `station`, `device_id`,
`updated_at` (ms since epoch), `data`.

## Change counter (`rev`)

Every time the tablet's database writes a record (a local save, a Bluetooth
merge or a restore), the record gets the next value of a counter that only
goes up. The desktop remembers, per tablet db id, the highest `rev` it has
received. Polling for `since` returns the records with a `rev` above `since`,
in `rev` order.

If the `db` in a POLL doesn't match the tablet's own id (a different tablet,
or a reinstalled app), the tablet treats `since` as 0 and sends everything.
Merging is newest-edit-wins on `updated_at`, with ties broken by `device_id`,
the same rule as Bluetooth sync, so receiving a record twice is harmless.

## Exchanges

### Poll (tablet → desktop)

1. Desktop: `POLL(session=S, since, db)`.
2. Tablet: picks the records with `rev > since`, as many as fit in one bundle,
   and sends DATA fragments `0..count-1` for a new transfer `T`.
3. Desktop: once it has the last fragment, or the line has been quiet for the
   receive timeout, it sends `ACK(T)` with its bitmap.
4. Tablet: resends any fragments the bitmap is missing. Repeat 3–4.
5. Once the desktop has every fragment, it sends a final complete `ACK`,
   decodes, merges, stores `rev` for that tablet, and polls again straight
   away if `more` is true.

If nothing arrives after a POLL, the desktop resends it (up to the retry
limit). The tablet treats a repeated POLL as a new request, which is safe.
An empty reply is a bundle with no records.

### Restore (desktop → tablet)

1. Desktop: DATA fragments of transfer `T`, then `ACK_REQ(T)`.
2. Tablet: `ACK(T)` with its bitmap.
3. Desktop: resends missing fragments, then `ACK_REQ` again, until the bitmap
   is complete.
4. Tablet: when a restore bundle completes, it merges the records once
   (newest edit wins) and keeps acknowledging repeats.

### Admin settings (desktop → tablet)

The desktop sends each event's schedule as its own bundle, then the admin
settings bundle, each exactly like a restore (DATA, ACK_REQ, ACK). The
tablet keeps a schedule if its `hash` differs from the one it has, and the
settings if their `version` is higher. It then asks for `STATUS` to confirm
the new `admin` version. A tablet without this feature acknowledges the
bundles but ignores them, so its `admin` stays 0.

The master passes settings and schedules on over Bluetooth: each `hello`
carries the sender's settings version, and a side with newer settings sends
`{"t":"admin","config":{..},"schedules":[..]}`.

On the tablets the desktop's values are defaults. Each tablet remembers the
values it last took from the desktop; a value changed on the tablet since
then is kept, until "Reset to desktop settings" in its Settings. The same
goes for a schedule file the tablet downloaded itself.

### Status

`STATUS_REQ` → `STATUS` with the tablet's db id, name, current `rev`, record
count and event keys. The desktop uses it to show what's connected.

## Radio settings

Both dongles must use the same settings. The desktop app's Radio Settings
dialog writes them over AT commands (`+++`, then `AT+...`, then `AT+EXIT`).
To set up the tablet's dongle, plug it into the computer and apply the same
settings.

Defaults used here: stream mode (`AT+MODE=1`), SF7 / 125 kHz / 4/5, 22 dBm,
NETID 0, channel 65 (915 MHz, the US ISM band; Waveshare's HF default is
channel 18, 868 MHz, which is the EU band). At these settings the usable rate
is roughly 4–5 kbps.
