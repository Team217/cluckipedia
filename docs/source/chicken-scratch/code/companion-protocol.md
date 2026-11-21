# Companion: protocol

`companion/chicken_companion/protocol/` is the laptop's half of the link:
it turns "give me what's new" into frames on a wire, and back. It mirrors
the tablet's `lib/lora/` (see [Tablet: Bluetooth and the link](tablet-sync-and-link.md)),
which explains frames, COBS, bundles and bitmaps with worked examples. This
page covers the Python side.

| File | Holds |
|---|---|
| `framing.py` | Frames: `MsgType`, `Frame`, CRC, COBS, `encode`/`decode`, `FrameReader` |
| `transfer.py` | Bundles: compression, fragments, `fit_records`, bitmaps, `Outgoing`, `Incoming` |
| `link.py` | Ways to move frames: `SerialLink` (dongle), `TcpLink`, `SimulatedRadio` |
| `client.py` | The laptop's conversations: `status`, `poll`, `restore`, `push_admin` |
| `responder.py` | A pretend tablet: `MemoryStore` + `TabletResponder` (tests and simulator) |

## `framing.py`

| Item | What it is |
|---|---|
| `MsgType` | An `IntEnum` of message types: `POLL`, `STATUS_REQ`, `STATUS`, `DATA`, `ACK`, `ACK_REQ`, `ERROR`. An `IntEnum` behaves like a number but prints with a name. |
| `Frame` (`@dataclass`) | `type`, `session`, `transfer`, `index`, `count`, `payload`. `json()` reads the payload as JSON; `Frame.with_json(type, session, obj)` makes one. |
| `json_bytes(obj)` | Compact JSON as UTF-8 (`separators=(",", ":")`), byte-identical to the Dart side. |
| `crc16(data)` | CRC-16/CCITT-FALSE. |
| `cobs_encode` / `cobs_decode` | Remove / restore zero bytes. |
| `encode(frame)` | Header (packed with `struct`) + payload + CRC → COBS → trailing `0x00`. |
| `decode(packet)` | The reverse, or `None` if anything is wrong. Never raises: a damaged radio packet is normal, not an error. |
| `FrameReader.feed(data)` | Accumulates bytes, splits at zeros, returns the frames that decoded. |

`HEADER = struct.Struct(">BBHHBB")` describes the 8-byte header: `>`
means big-endian (most significant byte first), `B` = 1 byte, `H` = 2
bytes. The checksum is added with `struct.pack(">H", crc16(body))`.

## `transfer.py`

Same functions as `transfer.dart`: `pack_bundle`, `unpack_bundle`,
`split`, **`fit_records`**, `bitmap`, `parse_bitmap`, and the `Outgoing`
and `Incoming` classes.

**`fit_records(make_header, records)`**, in detail:

```python
lo, hi = 0, len(records)
best = build(0)
while lo < hi:                      # largest n that fits
    mid = (lo + hi + 1) // 2
    packed = build(mid)
    if len(packed) <= MAX_BUNDLE_BYTES:
        lo, best = mid, packed       # mid fits: try more
    else:
        hi = mid - 1                 # too big: try fewer
```

This is **binary search**: each try halves the range of possible answers.
`make_header(n)` is a *function* passed in, because the header depends on
how many records end up included (the poll reply's `rev` is the change
number of the last included record).

## `link.py`

**`Link`** is a `Protocol` (Python's interface): anything with
`send(frame)`, `recv(timeout)` and `close()`. `LinkClient` works with any of
them.

| Class | How it moves frames |
|---|---|
| `SerialLink(port, baud, air_bps)` | `pyserial`. A background thread (`_read_loop`) reads bytes into a `FrameReader` and puts frames on a `queue.Queue`; `recv` takes from the queue with a timeout. `send` waits for the previous frame's airtime (`airtime(n, bps)`), like the tablet does. |
| `TcpLink(host, port)` | A socket. Same reader-thread-plus-queue design; no pacing. Sets `TCP_NODELAY`. If the tablet closes the connection, `closed_by_peer` becomes true and `send` raises `ConnectionError`. |
| `SimulatedRadio(loss, seed, delay)` | Two in-memory endpoints (`desktop`, `tablet`). `_transmit` runs every frame through the real encoder and decoder, and drops a `loss` fraction of them using a seeded random generator, so a test with `seed=3` loses the *same* frames every run. An endpoint with a `handler` calls it directly (the simulated tablet). |

```{admonition} Why a queue?
:class: note
Bytes arrive whenever the radio delivers them; the client wants to wait for
"the next frame, or give up after 2 s". A reader thread plus a thread-safe
`queue.Queue` connects the two: the thread `put`s frames as they arrive,
and `recv` does `get(timeout=…)`.
```

## `client.py`: `LinkClient`

Created with a link, a receive timeout and a retry count. Every method
**blocks** (waits) until done, which is why the window runs it on the worker
thread. Problems raise **`LinkError`** with a message for the user.

| Method | What it does |
|---|---|
| `_session()` | A random session number for a new exchange, so stray frames from an earlier exchange are ignored. |
| `_drain()` | Throws away any frames already waiting. |
| `_recv(session, timeout)` | The next frame for *this* session; raises `LinkError` if the tablet sent `ERROR`. |
| `status()` | Send `STATUS_REQ` up to `retries` times until a `STATUS` arrives. Returns its JSON. |
| `poll(since, db, progress)` | Send `POLL`; `_collect` the `DATA` fragments; `_repair` the gaps; unpack → `PollResult(db, rev, more, records, admin, devices)`. Resend the poll if nothing came back. |
| `_collect(session, inc, progress)` | Receive fragments into an `Incoming` until the last index arrives or the line goes quiet. |
| `_repair(session, inc, progress)` | Send an `ACK` bitmap, collect resent fragments, repeat until complete; then a final `ACK` so the tablet knows. |
| `restore(records, progress)` | `fit_records` into as many `restore` bundles as needed and `_send_bundle` each. |
| `push_admin(config, schedules, progress)` | One `schedule` bundle per event, then one `admin` bundle. |
| `_send_bundle(data, progress)` | Send the fragments, then `ACK_REQ`; read the `ACK` bitmap; resend only what's missing; repeat until complete or out of retries. |

`progress(done, total)` is an optional callback; the worker passes a Qt
signal's `emit`, which moves the progress bar.

## `responder.py`: the pretend tablet

**`MemoryStore`**: records plus the change counter, in a dictionary; `put`
applies newest-wins; `since(rev)`; `status()`; and admin storage
(`receive_admin`, `receive_schedule`, `admin_version`, `device_list`).

**`TabletResponder(store, send)`**: the same request handling as the Dart
`LoraResponder`. It's never used with a real tablet; it exists so the
tests and **Simulator** mode have a tablet to talk to. The cross-language
tests then check that the *real* Dart responder behaves the same.
