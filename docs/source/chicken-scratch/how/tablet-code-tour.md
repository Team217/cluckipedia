# Tablet code tour

A walk through every folder of the tablet app. Files you'll change most are
**bold**.

```text
chicken_scratch/
├── lib/                 ← all the app's Dart code
├── test/                ← automated tests
├── assets/              ← pictures, fonts, default config files
├── android/             ← Android build settings + Kotlin Bluetooth code
├── tool/                ← a helper used by the companion's tests
├── pubspec.yaml         ← app name, version, packages, assets
├── companion/           ← the laptop app (see Companion code tour)
└── docs/               ← this documentation (published in Cluckipedia)
```

## `lib/`: the app

### `main.dart`
Starts the app: loads laptop settings, resumes the desktop link, applies the
theme, opens the home page.

### `pages/`: one file per screen

| File | Screen |
|---|---|
| `home_page.dart` | Home: top bar, passcode lock, menu; shows one of the two homes below. Also `writeMatchJson`, used by **Get Matches**. |
| `student_home_page.dart` | Tiles on a scouting tablet's home. |
| `admin_home_page.dart` | Tiles on the master's home. |
| `events_page.dart` | List of events (from `MatchSchedules/` files). |
| **`match_list_page.dart`** | To do / Completed matches for an event. |
| **`match_scout_page.dart`** | The swipeable scouting pages for one match, with the alliance-coloured app bar. |
| **`game_periods/prematch_page.dart`** | Scout name + starting position. |
| **`game_periods/auton_page.dart`** | Auton path, fuel, climb; turns red after 20 s. |
| **`game_periods/teleop_page.dart`** | Teleop fuel. |
| **`game_periods/endgame_page.dart`** | Climb level, outpost, robot condition, defense, driving. |
| **`game_periods/submit_data_page.dart`** | Comments, QR code, Submit. |
| `settings_page.dart` | Settings, including "Desktop settings". |
| `bluetooth_sync_page.dart` | Bluetooth sync screen; also the "pick master tablet" dialog. |
| `desktop_link_page.dart` | LoRa / TCP connection to the laptop. |
| `sync_with_workers_page.dart` | QR scanning on the master. |
| `dashboard_page.dart` | The dashboards screen (toolbar, palette, edit mode). |
| `export_csv.dart` | The CSV export flow. |
| `queen_home_page.dart` | *Unused* (old home screen). |

The game-specific pages (**bold** above) are what changes each season. See
[Getting ready for a new season](../maintain/new-season.md).

### `widgets/`: reusable pieces of screen

| File | What |
|---|---|
| **`fuel_counter_widget.dart`** | The big tap-to-count number with −/+ buttons. |
| **`auton_path_widget.dart`** | Draw-the-path field map, and `AutonPath` (with its compact QR encoding). |
| **`climb_selector_widget.dart`** | Climb level list, and the `ClimbLevel` values saved in the data. |
| **`robot_condition_widget.dart`** | Robot condition list (the saved text values live here). |
| **`defense_widget.dart`** | Defense / driving switches and star ratings. |
| `scout_ui.dart` | Shared look: `SectionHeader`, `ScoutCard`, `CardLabel`, `Caption`. |
| `home_tile.dart` | Home screen cards. |
| `match_tile.dart` | A row in the match list. |
| `counter_box_widget.dart` | *Unused* (old counter). |

### `models/`: data the screens work with

| File | What |
|---|---|
| **`scout_data_model.dart`** | `ScoutData`: every field for the match being scouted, `toMap()` (what gets saved), loading, and `writeFile()` (saves to the JSON file **and** the database). |
| `match_model.dart` | `FRCMatch`: one scheduled match. *Note the red/blue swap*, explained in [Files on the tablet](../database/files.md). |
| `events_model.dart` | An event from The Blue Alliance. |
| `tba.dart` | Downloads events and matches from The Blue Alliance. |
| `globals.dart` | The built-in scouter list and passcode (replaced by laptop settings when sent). |
| `score_event_model.dart`, `student.dart` | *Leftovers* from earlier seasons. |

### `data/`: storage

| File | What |
|---|---|
| **`scout_database.dart`** | The SQLite database: save, merge, query, dashboards, small settings. See [Tablet database](../database/tablet-database.md). |
| `scout_record.dart` | `ScoutRecord`: one row of the database, plus JSON conversion. |
| `app_settings.dart` | Reads and updates `settings.json`. |
| `csv_exporter.dart` | Turns records into CSV text. |

### `sync/`, `lora/`, `admin/`

| File | What |
|---|---|
| `sync/bluetooth_sync_service.dart` | Tablet-to-tablet sync. See [Bluetooth sync](bluetooth-sync.md). |
| `lora/desktop_link_service.dart` | Connects the dongle or TCP listener; feeds bytes to the responder. |
| `lora/lora_responder.dart` | Answers the laptop's requests. |
| `lora/frame.dart`, `lora/transfer.dart` | The message format (framing, compression, chunking). |
| `lora/memory_store.dart` | A pretend database for tests. |
| `admin/admin_service.dart` | Applies roster, settings and schedules from the laptop. See [Admin settings](admin-settings.md). |

### `analysis/` and `dashboard/`

| File | What |
|---|---|
| **`analysis/metrics.dart`** | The list of stats and how each is read from a record. Changes each season. |
| `analysis/event_data.dart` | Loads an event's records and schedule; ranking maths. |
| `dashboard/dashboard_model.dart` | Tile types, layouts, the starter dashboards. |
| `dashboard/dashboard_board.dart` | The grid you drag tiles around on. |
| `dashboard/tiles.dart` | What each tile type draws. |
| `dashboard/tile_settings.dart` | A tile's settings dialog. |
| `dashboard/viz.dart` | Chart colours and small chart widgets. |

### `theme/app_theme.dart`
Colours and styles for the whole app. See [Look and feel](theme.md).

## `test/`

| File | Checks |
|---|---|
| `scout_data_store_test.dart` | Saving, merging, sync manifests, CSV. |
| `db_migration_test.dart` | Upgrading an old tablet database. |
| `lora_test.dart` | The LoRa message format and responder. |
| `admin_test.dart` | Laptop settings: override rule, schedule files, end to end. |
| `dashboard_test.dart` | Stats maths, layout, renders each starter dashboard. |
| `screens_test.dart` | Renders every main screen in light and dark. |
| `widget_test.dart` | Flutter's template test. Always fails; safe to delete. |

## `assets/`

| Folder | What |
|---|---|
| `images/` | Field pictures (`RedField.png`, `BlueField.png`), robot markers, team logo. **Replace the field pictures each season.** |
| `config/default_settings.json` | The first `settings.json` a new tablet gets. |
| `config/default_match.json` | Template for a new match file (out of date, but it's overwritten on first save). |
| `fonts/` | Fonts (mostly unused by the current design). |
| `tablet_pull/`, `transfer/` | *Old* tools and templates from earlier seasons (see [Known issues](../maintain/known-issues.md)). |

## `android/`

| Path | What |
|---|---|
| `app/src/main/AndroidManifest.xml` | Permissions (Bluetooth, internet, USB) and the USB dongle filter. |
| `app/src/main/res/xml/usb_device_filter.xml` | USB chips that open the app when plugged in. |
| `app/src/main/kotlin/.../MainActivity.kt` | Connects the Kotlin Bluetooth code to Flutter; asks for Bluetooth permission. |
| `app/src/main/kotlin/.../BluetoothSync.kt` | Bluetooth Classic: hosting, connecting, sending lines of text. |
| `app/build.gradle`, `settings.gradle` | Build settings and tool versions. |

## `tool/lora_stdio_responder.dart`

Runs the tablet's real responder code on a computer, talking through
stdin/stdout or TCP. The companion's tests use it to prove the Python and
Dart sides understand each other.
