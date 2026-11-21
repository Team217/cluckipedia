# Patterns and ideas

A handful of ideas appear again and again in this codebase. Once you
recognise them, most files become much easier to read. Each one below has:
what it is, where it's used, real code, and **why** it's there.

## 1. Identity from content: the record ID

**What:** every scouting record's ID is built from *what* was scouted, not
*who* or *when*:

```dart
// lib/data/scout_record.dart
static String makeId(String eventKey, int matchNumber, int teamNumber) =>
    '${eventKey}_Match${matchNumber}_$teamNumber';
```

**Where:** the tablet database, Bluetooth sync, the laptop database, file
names, legacy import (`record_id` in `legacy_import.py`).

**Why:** if two tablets both have "Troy 2, match 12, team 217", they must
agree it's the *same* record, with no central server handing out numbers.
An ID made from the content means every device calculates the same ID on
its own. The alternative, auto-numbered IDs (1, 2, 3…), would give the same
match different numbers on different tablets, and sync would create
duplicates.

**Trade-off:** two scouts can't both keep a separate record of the same
robot in the same match; the second save replaces the first (see next
pattern). For scouting that's what we want.

## 2. Last writer wins

**What:** when two copies of a record differ, keep the one edited most
recently. If they were edited in the same millisecond, the one from the
tablet with the "larger" device ID wins, so every device makes the
*same* choice.

```dart
// lib/data/scout_database.dart
static bool _isNewer(int aTime, String aDevice, int bTime, String bDevice) =>
    aTime > bTime || (aTime == bTime && aDevice.compareTo(bDevice) > 0);
```

```python
# companion/chicken_companion/db.py — the same rule in Python
def _newer(a_time: int, a_dev: str, b_time: int, b_dev: str) -> bool:
    return a_time > b_time or (a_time == b_time and a_dev > b_dev)
```

**Where:** `mergeRemote` (tablet), `Store.merge` (laptop),
`MemoryStore.put` and `MemoryLoraStore.put` (test copies).

**Why:** it's the simplest rule that (a) never loses the latest edit,
(b) needs no coordination, and (c) gives the same answer no matter what
order records arrive in. That last property is what makes sync safe to
repeat: receiving a record twice changes nothing. A rule with that property
is called **idempotent**.

**Trade-off:** it trusts each tablet's clock. A tablet with its clock set
wrong could win or lose unfairly. Tablets set their time from the network,
so this is rarely a problem.

## 3. A fixed envelope with a flexible payload

**What:** a record has a few fixed fields every season shares (event,
match, team, scout, station, device, edit time) and one `data` field holding
that season's measurements as JSON.

```dart
// lib/data/scout_record.dart
/// Only the identifying fields are real columns. The game-specific scouting
/// data lives in [data] as JSON, so next season's ScoutData can change its
/// fields without touching the database, sync, or CSV code.
final Map<String, dynamic> data;
```

**Where:** both databases, every sync message, CSV export (which builds its
columns from whatever is in `data`), and the laptop's season data
dictionary.

**Why:** the game changes every year. If every measurement were a database
column, each season would need database changes on every tablet and the
laptop, new sync code and new CSV code, and old seasons wouldn't fit the
new columns. With the envelope + payload design, a new season only changes
the screens and `ScoutData` (see
[Getting ready for a new season](../maintain/new-season.md)).

**Trade-off:** the database can't check that `data` has the right fields,
and queries need `json_extract`. The laptop's `season_fields` table brings
back some of that visibility.

## 4. Singletons: one service for the whole app

**What:** a class with exactly one instance, reached through `.instance`.

```dart
// lib/sync/bluetooth_sync_service.dart
class BluetoothSyncService extends ChangeNotifier {
  BluetoothSyncService._() { ... }                      // private constructor
  static final BluetoothSyncService instance = BluetoothSyncService._();
```

The private constructor (`._()`) means nobody else can make a second one.

**Where:** `ScoutDatabase`, `BluetoothSyncService`, `DesktopLinkService`,
`AdminService`.

**Why:** there's one database file, one Bluetooth radio and one link to the
laptop. A singleton keeps exactly one object in charge of each, and it
*outlives the screens*: leave the Bluetooth page and the connection keeps
going because the service still exists.

**Trade-off:** singletons are global, so any code can reach them, which can
make code harder to test. The project works around that with interfaces
(pattern 8).

## 5. Listeners: telling the screen something changed

**What:** a service extends `ChangeNotifier` and calls `notifyListeners()`
when its state changes. Pages call `addListener` and redraw.

```dart
// lib/pages/desktop_link_page.dart (simplified)
@override
void initState() {
  super.initState();
  link.addListener(_changed);          // start listening
}

@override
void dispose() {
  link.removeListener(_changed);       // stop when the page closes
  super.dispose();
}

void _changed() { if (mounted) setState(() {}); }   // redraw
```

**Where:** every service and the pages that show them.

**Why:** the service doesn't need to know which pages exist. It just
announces "something changed", and whoever cares redraws. That's what
keeps the arrows in the [layout diagram](index.md) pointing one way.

**Watch out:** always `removeListener` in `dispose`, or a closed page keeps
getting called, which causes errors about `setState` after dispose.
`if (mounted)` is a second safety net.

## 6. Streams: a flow of events

**What:** a `Stream` delivers a series of events over time; you `listen`
to it.

```dart
// lib/data/scout_database.dart
final _localChanges = StreamController<ScoutRecord>.broadcast();
Stream<ScoutRecord> get localChanges => _localChanges.stream;

// in saveLocal(): announce the new record
_localChanges.add(record);
```

```dart
// lib/sync/bluetooth_sync_service.dart: send each new local save to peers
ScoutDatabase.instance.localChanges.listen(_onLocalSave);
```

**Where:** `ScoutDatabase.localChanges` (Bluetooth sends new matches
instantly), `ScoutDatabase.changes` (dashboards refresh),
`AdminService.updates` (Bluetooth passes on new settings), Bluetooth events
from Kotlin, bytes from the USB dongle and TCP socket.

**Why:** like listeners, but each event carries a value (here, the record
that was saved). "Broadcast" means many listeners can subscribe.

## 7. Async and await: waiting without freezing

**What:** slow operations (files, the database, Bluetooth, network) return
a `Future` (Dart) and you `await` it. While waiting, the app keeps drawing
and responding.

```dart
Future<ScoutRecord?> record(String id) async {
  final rows = await (await db).query(_table, where: 'id = ?', whereArgs: [id]);
  return rows.isEmpty ? null : ScoutRecord.fromRow(rows.first);
}
```

The Python companion does slow work on a **background thread** instead
(pattern 13).

**Common bug:** forgetting `await`. The code carries on before the work is
finished.

## 8. Interfaces: depend on "what", not "which"

**What:** an *interface* (an `abstract class` in Dart, a `Protocol` in
Python) lists the methods something must have, without saying how they
work. Code is written against the interface, and you can plug in any
implementation.

```dart
// lib/lora/lora_responder.dart: what the responder needs from a database
abstract class LoraStore {
  Future<String> dbId();
  Future<int> currentRev();
  Future<List<(int, ScoutRecord)>> changesSince(int rev);
  Future<List<ScoutRecord>> merge(List<ScoutRecord> records);
  ...
}
```

| Interface | Real implementation | Test / simulator implementation |
|---|---|---|
| `LoraStore` (Dart) | `_DatabaseStore` in `desktop_link_service.dart` (the real database) | `MemoryLoraStore` (in memory) |
| `Link` (Python) | `SerialLink` (USB dongle), `TcpLink` (network) | `SimulatedRadio` endpoints (lossy, in memory) |

**Why:** the responder's logic (answering polls, handling bundles) can be
tested on a computer with an in-memory store, and the laptop can talk to a
simulated radio, all without touching the real code being tested. Passing
the implementation in (rather than the code creating it) is called
**dependency injection**.

## 9. Pure functions: easy to test

**What:** a function whose answer depends only on its inputs, with no
files, database or screen involved.

```dart
// lib/admin/admin_service.dart
(Map<String, dynamic>, Map<String, dynamic>) applyPushedSettings(
  Map<String, dynamic> settings,      // current settings.json
  Map<String, dynamic> applied,       // what the desktop set last time
  Map<String, dynamic> pushed,        // what the desktop sends now
  {bool force = false},
)
```

**Where:** `applyPushedSettings`, `pushedSettings`, `scheduleFileContent`,
`resolveLayout`, `compactLayout`, `Agg.apply`, `parseSchedule`,
`decode_compact_path`, `parse_file`, `schedule_hash`, `crc16`,
`cobsEncode`, and more.

**Why:** the tricky logic is pulled out of the code that touches files and
screens, so a test can call it with made-up inputs and check the answer in
milliseconds. `AdminService._applySettings` just reads the files, calls
`applyPushedSettings`, and writes the result.

## 10. One at a time: a queue made of futures

**What:** making sure messages are handled one after another, in order,
even though each one involves waiting.

```dart
// lib/lora/desktop_link_service.dart
Future<void> _queue = Future.value();

void _onBytes(Uint8List data) {
  for (final f in _reader.feed(data)) {
    // One request at a time, in order.
    _queue = _queue.then((_) => _responder?.handle(f)).catchError(...);
  }
}
```

Each new job is chained onto the end of the previous one with `.then`, so
job 2 can't start until job 1 has finished.

**Where:** `DesktopLinkService._onBytes`, `BluetoothSyncService` (its
event stream handler), and `tool/lora_stdio_responder.dart`.

**Why:** without it, two messages arriving close together would be
processed at the same time. For Bluetooth sync, a `done` message could be
handled before the records sent just ahead of it were saved, and the tablet
would announce "synced" too early.

## 11. The change counter: "what's new since…?"

**What:** every write to the tablet's database gets the next number from a
counter that only goes up (`local_rev`). A reader remembers the highest
number it has seen and asks for everything above it.

```dart
Future<List<(int, ScoutRecord)>> changesSince(int rev) async {
  final rows = await (await db).query(_table,
      where: 'local_rev > ?', whereArgs: [rev], orderBy: 'local_rev');
  ...
}
```

**Where:** the tablet database; the laptop remembers the number per tablet
(`tablets.last_rev`).

**Why:** LoRa is slow (about 5 kbit/s), so the laptop can't afford to ask
"send me everything" each time. With the counter it asks "anything after
#431?" and gets only the new or edited records. Database systems call this
a *log sequence number*.

## 12. Manifests: "here's what I have"

**What:** when two tablets connect, each sends a small list of every record
ID with its edit time (a **manifest**). Each side compares that with its own
database and sends only what the other lacks or has older.

```dart
Future<List<ScoutRecord>> recordsNewerThan(Map<String, dynamic> peerManifest) async {
  final all = await records();
  return all.where((r) {
    final theirs = peerManifest[r.id];
    if (theirs is! List || theirs.length < 2) return true;   // they don't have it
    return _isNewer(r.updatedAt, r.deviceId, (theirs[0] as num).toInt(), theirs[1].toString());
  }).toList();
}
```

**Why not use the change counter here too?** Bluetooth pairs change all the
time (tablet A meets the master, then tablet B), and each tablet's counter
is its own. Comparing manifests works between *any* two tablets with no
memory of past syncs. It's cheap over Bluetooth, which is fast enough to
send a few kilobytes of IDs.

## 13. Background threads and signals (companion)

**What:** the companion's window runs on one thread and does slow radio
work on another. They communicate only through Qt **signals**.

```python
# ui/main_window.py
self._sync.connect(self.worker.sync)          # UI → worker
self.worker.synced.connect(self._on_synced)   # worker → UI
...
self._sync.emit()                             # "please sync"
```

**Why:** a radio transfer takes minutes; if the window waited, it would
freeze. Qt only allows the UI thread to touch windows and buttons, so the
worker can't update the screen directly. Signals carry messages safely
between threads. See [Companion: window](companion-ui.md).

## 14. Versions that only go up

**What:** admin settings carry a version number: the time of the last change
in milliseconds, always increased. A tablet accepts settings only if their
version is higher than what it has.

```python
# companion/chicken_companion/admin.py
def _bump(self, s) -> None:
    ...
    self._set(s, "version", str(max(int(time.time() * 1000), old + 1)))
```

`max(now, old + 1)` guarantees it goes up even if the clock went backwards.

**Why:** settings can reach a tablet by several routes (laptop → master →
tablet, or tablet → tablet). With a version that only goes up, an old copy
arriving late can never overwrite a newer one, and passing settings around
in circles stops by itself (each receiver ignores what isn't newer).

## 15. Remembering what you set: detecting local changes

**What:** to let a tablet override a laptop setting, the tablet remembers
what the laptop last set. If the current value still matches that, nobody
changed it; if it doesn't, someone did.

**Where:** `applyPushedSettings` (settings) and `receiveSchedule` (schedule
files, compared by a fingerprint, `fnv1a`). Full explanation:
[Tablet: admin settings](tablet-admin.md).

**Why:** the alternative, adding an "edited locally" flag to every settings
screen, would mean changing every place settings are edited. Comparing
with the last-known laptop value needs no changes to those screens at all.

## 16. Two implementations, one specification

**What:** the link protocol exists twice: Dart on the tablet, Python on the
laptop. They're kept in step by a shared written spec
([Link protocol](../reference/protocol.md)), shared example bytes
(`companion/protocol_vectors.json`) that both test suites check, and tests
that run the real Python code against the real Dart code.

**Why:** two languages can't share code. Without these checks, a small
difference (say, the order of two bytes) would only show up as "it doesn't
work" at an event.

## 17. Fakes and simulators

**What:** stand-ins that behave like the real thing: `SimulatedRadio`
(drops a chosen fraction of messages), `TabletResponder` + `MemoryStore` (a
pretend tablet in Python), `MemoryLoraStore` (a pretend database in Dart),
`fake_store()` (a made-up event), and the fake storage paths in the tests.

**Why:** you can't test with six tablets and two radios every time you
change a line. The simulator also lets anyone learn the companion app
without hardware.

## 18. Migrations: upgrading data in place

**What:** code that changes an existing database file to a newer design
without losing data, guided by a stored version number.

```dart
// lib/data/scout_database.dart
openDatabase(path, version: 3,
  onUpgrade: (db, oldVersion, newVersion) async {
    if (oldVersion < 2) await _createDashboardsTable(db);
    if (oldVersion < 3) await _addChangeCounter(db);
  },
  ...
```

Each step runs only if the file is older than that step, in order, so a
version 1 file gets both, and a version 2 file gets only the second.

**Why:** tablets and the laptop hold real data from earlier versions of the
app. Deleting it on upgrade isn't an option.

## 19. Debouncing: wait for things to settle

**What:** when many events arrive close together, wait until they stop
before reacting once.

```dart
// lib/pages/dashboard_page.dart
_sub = ScoutDatabase.instance.changes.listen((_) {
  _debounce?.cancel();
  _debounce = Timer(const Duration(milliseconds: 500), _reload);
});
```

**Where:** dashboard refresh (a Bluetooth sync can deliver hundreds of
records in a burst), and regenerating the QR code while typing comments
(`_onCommentChanged` in `submit_data_page.dart`).

**Why:** reloading all the dashboard maths for each of 300 records would
make the tablet stutter; once after the burst is enough.
