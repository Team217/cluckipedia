# Tablet: admin settings

`lib/admin/admin_service.dart` receives the roster, team settings, station
assignments and schedules from the laptop, applies them, and remembers
enough to let a tablet keep its own changes. Read
[Admin settings](../how/admin-settings.md) for the overview; this page
goes through the code.

The file has two halves: the **`AdminService` class**, which deals with
storage and files, and **pure helper functions** at the bottom, which hold
the tricky logic and are easy to test ([Pattern 9](patterns.md)).

## What's stored where

| Kept in | Key | Holds |
|---|---|---|
| database `meta` | `admin_config` | The last settings received (JSON) |
| database `meta` | `admin_applied` | What the laptop last set in `settings.json`, e.g. `{"local.station": "Red 2"}` |
| database `meta` | `admin_schedule:<event>` | The schedule received for an event |
| database `meta` | `admin_schedule_file:<event>` | Fingerprint of the schedule file last *written* |
| database `meta` | `known_devices` | Tablets seen over Bluetooth |
| memory | `config` | The current settings (loaded at start-up) |
| memory | `_builtInScouts`, `_builtInPasscode` | The app's built-in values, captured when the service is created |

## `AdminService` methods

### Loading and receiving

| Method | What it does |
|---|---|
| `load()` | At start-up: read `admin_config`; if there is one, `_applyGlobals`. |
| `receiveConfig(incoming, {force})` | If the version isn't newer (and not `force`), return `false`. Otherwise save it, `_applyGlobals`, `_applySettings`, notify listeners, and add an event to `updates` (so Bluetooth passes it on). |
| `receiveSchedule(schedule, {force})` | Save it if its `hash` is new; then decide whether to (re)write the file (below). Returns whether the schedule was new. |
| `schedule(event)` / `schedules()` | One stored schedule / those for the events in the current settings. |
| `resetToDesktop()` | `_applySettings(force: true)` and rewrite every schedule with `force: true`. |
| `overrides()` | Keys whose current `settings.json` value differs from what the laptop last set. Shown on the Settings page. |

### Tablets

| Method | What it does |
|---|---|
| `noteDevice(id, name, station)` | Remember a tablet seen over Bluetooth (called on `hello`). |
| `knownDevices()` | The remembered tablets. |
| `deviceList(selfName)` | This tablet first, then the remembered ones, for the laptop's Tablets page. |

### Applying

| Method | What it does |
|---|---|
| `_applyGlobals(c)` | Replace `globals.studentList` with `"Select Student Name"` + the laptop's scouts (or the built-in list if the laptop sent none). Set `globals.queenPassword` (or the built-in one). |
| `_applySettings(c, {force})` | Read `settings.json` and `admin_applied`, call **`applyPushedSettings`**, write both results back. |
| `_scheduleFile(event)` | `MatchSchedules/<event>.json` in app storage. |

## The pure helpers

### `pushedSettings(config, deviceId)`

Turns the laptop's settings into `settings.json` keys **for this tablet**:

```dart
pushedSettings({
  'team_number': '217', 'season': '2026', 'current_event': '2026mitr2',
  'assignments': {'a1b2…': {'station': 'Red 2', 'score_table_side': false}},
}, 'a1b2…')
// → {'master.teamNumber': '217', 'master.year': '2026',
//    'master.currentCompId': '2026mitr2',
//    'local.station': 'Red 2', 'local.onScoreTableSide': false}
```

Empty or missing values are left out, so an empty field on the laptop
never blanks a tablet's setting. Only this tablet's assignment (by its
`deviceId`) is used.

### `applyPushedSettings(settings, applied, pushed, {force})`: the override rule

For each pushed key:

```text
current  = the value in settings.json now
last     = applied[key]  (what the laptop set last time, if ever)

if we've set this key before AND current != last AND not force:
    keep current            ← someone changed it on the tablet
else:
    write the pushed value, and remember it in applied
```

Worked example, the station key:

| Step | `settings.json` station | `applied` station | Laptop sends | Result |
|---|---|---|---|---|
| First push | `Blue 1` (set by hand long ago) | — | `Red 2` | Write `Red 2` (never set before) |
| Laptop changes it | `Red 2` | `Red 2` | `Red 3` | Write `Red 3` (unchanged on tablet) |
| Scout swaps on the tablet | `Blue 3` | `Red 3` | — | — |
| Laptop pushes again | `Blue 3` | `Red 3` | `Red 3` | **Keep `Blue 3`** (changed on tablet) |
| Reset to desktop settings | `Blue 3` | `Red 3` | `Red 3` (`force`) | Write `Red 3` |

It returns **new maps** rather than changing the ones passed in, so the
caller decides when to save, and tests can compare inputs and outputs.

### `scheduleFileContent(schedule)`

Writes a schedule in exactly the "Get Matches" file format: one JSON object
per match, joined by `,\n`, with **red and blue swapped** (see
[Files on the tablet](../database/files.md)). `test/admin_test.dart` checks
that the file reads back correctly through both the match list's parser and
the dashboards' parser.

### `fnv1a(text)`

A fast 64-bit fingerprint (the FNV-1a hash) of a file's contents, as 16 hex
characters. Dart integers are signed 64-bit, so the result is printed as
two unsigned 32-bit halves. (An early version printed a negative number;
the test with FNV's published reference values caught it.)

## Deciding whether to rewrite a schedule file

Inside `receiveSchedule`:

```text
changed     = the schedule's hash differs from the stored one
exists      = the file is there
lastWritten = fingerprint of what WE last wrote (meta)
overridden  = exists AND lastWritten known AND fingerprint(file now) != lastWritten

write the file if:  force  OR  not exists  OR  (changed AND not overridden)
```

So a schedule the tablet downloaded itself (with **Get Matches**) is left
alone, a missing file is always restored, and **Reset** always rewrites.

## Tests

`test/admin_test.dart` covers `applyPushedSettings` (first push,
overrides, reset), `pushedSettings`, `scheduleFileContent` round trips,
`fnv1a` reference values, and a full run of `AdminService` against a real
(temporary) database and settings file, including a locally replaced
schedule being kept until reset.
