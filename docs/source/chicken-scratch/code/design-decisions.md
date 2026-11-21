# Design decisions

Every program is the result of choices. This page records the big ones so
future maintainers know what was considered and why, and can tell when a
reason no longer applies. (Professionals call these *architecture decision
records*.)

Each entry has: **the problem**, **the options**, **the choice**, and
**the consequences**: what it costs and what to watch.

---

## D1. A database on each tablet, not just files

**Problem:** the original app saved each match as a JSON file. Syncing,
exporting and analysing meant reading hundreds of files.

**Options:** keep files only · a database only · a database plus the files.

**Choice:** a SQLite database (`sqflite`) as the source of truth, **plus**
the per-match file as a human-readable backup.

**Consequences:** queries ("all records for this event", "what changed
since #431") are fast and simple. The files keep older tools and habits
working. `ScoutData.writeFile()` writes both, and `readFile()` prefers the
database, because a synced copy may be newer than the local file.

---

## D2. Bluetooth star, not Wi-Fi or a mesh

**Problem:** six tablets must share data at venues with no usable network,
and FRC rules don't allow teams to run Wi-Fi networks.

**Options:** Wi-Fi Direct or a hotspot (not allowed) · Bluetooth Low Energy
(small, slow messages, complex) · Bluetooth Classic, as a mesh or a star ·
QR codes only.

**Choice:** **Bluetooth Classic (RFCOMM) in a star**: the master hosts and
the others connect. QR codes remain as a backup.

**Consequences:** Android supports a star with about 7 connections, but not a
mesh, so the master relays everything. If the master is out of range, the
others can't sync with each other. Flutter has no good Bluetooth Classic
package, so a small Kotlin file (`BluetoothSync.kt`) does the radio part and
Dart does all the logic (decision D3).

---

## D3. Keep native (Kotlin) code as small as possible

**Problem:** Bluetooth needs Android-specific code, but most of the team
reads Dart, not Kotlin.

**Choice:** Kotlin only moves lines of text between tablets (open, accept,
connect, send line, receive line). All the sync rules (manifests, merging,
relaying) are in Dart (`bluetooth_sync_service.dart`).

**Consequences:** the logic is testable in Dart and readable by students who
know one language. The Kotlin file rarely needs to change.

---

## D4. Record IDs from content; last writer wins

See [Patterns 1 and 2](patterns.md).

**Options considered:** a central server assigning IDs (impossible offline)
· random IDs (duplicates when two tablets scout the same robot) · version
vectors or full conflict resolution (complex, and scouting doesn't need to
keep two conflicting versions).

**Consequences:** simple and repeatable sync. If two scouts genuinely
scout the same robot, only the later save survives. Station assignments
exist to prevent that.

---

## D5. Game data as a JSON payload

See [Pattern 3](patterns.md).

**Options considered:** a column per field (database changes every
season) · a table of (record, field, value) rows (flexible but awkward to
query and slow) · JSON in a column.

**Consequences:** new seasons need no database changes, and old seasons
keep their own fields. The laptop's `season_fields` table and SQLite's
`json_extract` cover the "what fields exist?" and "query a field" needs.

---

## D6. A laptop companion app, in Python

**Problem:** data needs a long-term home across seasons, a place to manage
rosters and schedules, and a way to get data off the tablets without a
network.

**Options:** a web app (needs a server and network) · another Flutter app
(desktop Flutter needs extra tooling on Windows) · a Python desktop app.

**Choice:** **Python with PyQt6** for the window, **SQLAlchemy** for the
database, **pyserial** for the radio.

**Consequences:** Python is widely taught and quick to change; the
companion runs on Windows and Linux from source with no build step. The
price is that the link protocol is written twice (D8).

---

## D7. LoRa radio, with TCP as an alternative

**Problem:** the laptop is often in the pits, far from the stands, with no
network between them.

**Choice:** cheap **USB LoRa dongles** (long range, low speed). Later,
**TCP** was added carrying exactly the same bytes, for testing and for when
no dongles are available.

**Consequences:** LoRa's low speed (about 5 kbit/s) shaped the protocol:
small frames, compression, change counters, retries (D9). Because TCP
reuses everything, it costs very little code (`TcpLink`, `startTcp`).

---

## D8. One protocol, two implementations, kept honest by tests

**Choice:** a written spec ([Link protocol](../reference/protocol.md)),
shared byte-exact examples (`protocol_vectors.json`), and tests that run the
real Python client against the real Dart responder over a lossy connection.

**Consequences:** changing the protocol means changing both sides and
regenerating the vectors. The tests make it obvious when they disagree.

---

## D9. The laptop asks, the tablet only answers

**Problem:** LoRa radios share one channel and can't send and receive at the
same time. If both sides talked whenever they liked, they'd collide.

**Choice:** the laptop starts every exchange; the tablet never transmits
unless asked.

**Consequences:** no collisions, and the tablet code stays simple (it's a
loop of "receive request, send answer"). Anything the tablet wants to tell
the laptop, like its known tablets or settings version, rides along in
answers (`STATUS` and the poll reply).

---

## D10. Reliability: checksums, fragments, selective resend

**Problem:** radio packets get lost or corrupted.

**Choice:** each frame has a CRC-16 checksum (damaged frames are dropped).
Data is compressed and split into up to 64 fragments; the receiver replies
with a bitmap of what arrived, and only missing fragments are resent.

**Consequences:** a lost fragment costs one resend, not the whole batch. The
tests run with 20–25% of messages dropped on purpose.

---

## D11. Laptop settings are defaults; tablets can override

**Problem:** the laptop should set up all tablets at once, but people at the
event sometimes need to change a tablet on the spot (a scout swaps
stations).

**Options:** the laptop always wins · the newest edit wins · tablets can
override until reset.

**Choice:** **tablets can override**: chosen by the team. See
[Tablet: admin settings](tablet-admin.md).

**Consequences:** each tablet remembers what the laptop last set, to detect
local changes. A **Reset to desktop settings** button undoes overrides.

---

## D12. Dashboards computed on the tablet, saved per tablet

**Problem:** the strategy team needs live numbers in the stands with no
network.

**Choice:** all statistics are computed on the tablet from its own records
(`EventData`). Each tablet keeps its own dashboard layouts; they aren't
synced.

**Consequences:** works offline; any synced tablet can run them. Different
people can arrange dashboards their own way. Stats are defined in one
list (`Metrics.all`), so adding a stat updates every tile.

---

## D13. Material 3 theme, colours never hard-coded

**Choice:** one theme (`lib/theme/app_theme.dart`) generated from the team
green; pages ask the theme for colours; alliance and status colours come
from `AppColors`. Charts use a separate validated chart palette (`Viz`).

**Consequences:** dark mode works everywhere for free; restyling the whole
app means editing one file. Charts stay readable for colour-blind users.

---

## D14. Laptop database: season-aware, upgraded in place

**Choice:** a `season` column on every record, a `season_fields` data
dictionary rebuilt automatically, and a schema version with migration code
(`Store._migrate`).

**Consequences:** the laptop can hold every season side by side, and older
laptop databases upgrade themselves when opened.

---

## D15. Documentation next to the code, published in Cluckipedia

**Choice:** docs live in `docs/` in this repository, so a change to code
and its docs go in the same commit. They're copied into Cluckipedia (the
team's knowledge base), which publishes them.

**Consequences:** the docs must use Cluckipedia's format (Sphinx + MyST).
See [Updating these docs](../maintain/updating-docs.md).
