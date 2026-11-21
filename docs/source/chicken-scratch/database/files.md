# Files on the tablet

Besides its database, the tablet app keeps plain files in its **app storage
folder**:

```text
/sdcard/Android/data/com.example.chicken_scratch/files/
├── settings.json                      ← this tablet's settings
├── MatchSchedules/
│   ├── 2026mitr2.json                 ← one schedule per event
│   └── 2026mibro.json
└── 2026mitr2/                         ← one folder per event
    ├── 2026mitr2_Match1_217.json      ← one file per robot per match
    └── 2026mitr2_Match1_33.json
```

You can see these by plugging the tablet into a computer (USB file transfer)
and browsing to **Android → data → com.example.chicken_scratch → files**,
or with `adb pull` (see [Build and install](../maintain/build-and-install.md)).
Uninstalling the app deletes them.

## `settings.json`

Two sections: `master` holds team-wide settings, `local` holds this
tablet's. A new tablet starts from `assets/config/default_settings.json`. Code
reads and writes it through `AppSettings` (`lib/data/app_settings.dart`),
which only changes the keys it's given.

```json
{
  "master": {"isMaster": false, "teamNumber": "217", "year": "2026",
             "currentCompId": "2026mitr2"},
  "local":  {"studentName": "", "playoffs": "", "station": "Red 1",
             "onScoreTableSide": false, "masterBtName": "Galaxy Tab 6",
             "masterBtAddress": "AA:BB:CC:DD:EE:FF",
             "desktopLinkMode": "tcp", "tcpPort": 5810, "tcpListening": true}
}
```

Every key is described in [Settings reference](../reference/settings.md).

## Per-match files: `<event>/<event>_Match<n>_<team>.json`

A copy of each record's `data` (exactly the JSON in the database's `data`
column), written every time the scout swipes or submits. They're a
human-readable backup and are what the old file-based tools read; the app
itself works from the database (it prefers the database copy when opening a
match). Files from older seasons in this format are what the companion
imports.

## `MatchSchedules/`

One file per event, written by **Get Matches**, **Get Current Matches**, or
a schedule pushed from the laptop. The **Events** page lists these files,
and the match list and dashboards read them.

The format is a list of JSON objects separated by `,` + newline, **without**
the surrounding `[` `]`:

```text
{"event_key":"2026mitr2","match_number":1,"blue_alliance":["frc217","frc33","frc67"],"red_alliance":["frc1114","frc2056","frc254"],"comp_level":"qm"},
{"event_key":"2026mitr2","match_number":2,…}
```

```{admonition} Red and blue are swapped in this file
:class: warning
In this file, **`blue_alliance` holds the red alliance** and
**`red_alliance` holds the blue one**. (Above, 217/33/67 are *red*.)

That's historical: `FRCMatch.fromJson` (reading The Blue Alliance) swaps
them, `toJson` writes them swapped, and `FRCMatch.fromProcessedJson`
(reading the file) swaps them back. Everything that reads the file
expects this, so **don't "fix" one side**. If you ever change it, change
`match_model.dart`, `admin_service.dart` (`scheduleFileContent`), the
companion's `admin.py` (`parse_tablet_schedule`) and their tests
together.
```

## Other storage

| What | Where |
|---|---|
| The database | `/data/data/com.example.chicken_scratch/databases/chicken_scratch.db` (private) |
| CSV exports | A temporary file handed to Android's share menu |
| Dashboards | In the database (`dashboards` table) |
