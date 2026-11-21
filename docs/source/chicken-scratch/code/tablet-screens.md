# Tablet: start-up and screens

This page goes through `lib/main.dart` and every screen in `lib/pages/`:
what state each keeps, what each method does, and why. First, a little
Flutter you need for the rest.

## How a screen works in Flutter

Most screens here are a pair of classes:

```dart
class MatchListPage extends StatefulWidget {          // 1. the "settings" of the screen
  final String? eventKey;                              //    (things passed in; never change)
  const MatchListPage({super.key, required this.eventKey});
  @override
  State<MatchListPage> createState() => _MatchListPageState();
}

class _MatchListPageState extends State<MatchListPage> {   // 2. the living part
  List<FRCMatch> hiveMatchListTODO = [];                    //    values that change
  ...
}
```

The **State** object lives as long as the screen is open. Its life cycle:

| Method | When Flutter calls it | Typical use here |
|---|---|---|
| `initState()` | Once, when the screen opens | Start loading data, subscribe to services |
| `build(context)` | Every time the screen must be drawn | Return the widgets; must be fast, no waiting |
| `setState(() { … })` | *You* call it after changing a value | Tells Flutter to call `build` again |
| `didUpdateWidget(old)` | The parent rebuilt with new settings | React to changed inputs (`PreMatchPage`) |
| `dispose()` | Once, when the screen closes | Unsubscribe, stop timers, free controllers |

`widget.xxx` inside the State reads the values passed in (e.g.
`widget.eventKey`). `mounted` is false once the screen has closed. Code
that finishes after an `await` checks `if (!mounted) return;` before
calling `setState`.

## `lib/main.dart`

| Item | What it does |
|---|---|
| `main()` | Prepares Flutter (`WidgetsFlutterBinding.ensureInitialized`), loads laptop settings (`AdminService.load`, inside `try` so failure can't stop the app), resumes the desktop link (`DesktopLinkService.resume`, not awaited), then `runApp`. |
| `MyApp` | The app itself: a `MaterialApp` with the light and dark themes, `themeMode: ThemeMode.system`, and `HomePage` at route `/`. `lightTheme` / `darkTheme` are static so the tests can use the exact same themes. |

## Home: `home_page.dart`, `student_home_page.dart`, `admin_home_page.dart`

**`_HomePageState`** holds `masterToggle` (is this the master?), `year`,
`teamNumber` and `isEnabled` (has the passcode been entered?).

| Method | What it does | Why |
|---|---|---|
| `readJson()` | Reads `settings.json` through the file-level getter `_localFile`, which first copies `assets/config/default_settings.json` if the file doesn't exist. | A new tablet has no settings yet. |
| `updateHome()` | Re-reads settings. Used by the ↻ button and pull-to-refresh. | After changing Settings, the home screen may need to switch between scouting and master. |
| `build()` | App bar with refresh and lock, the drawer (☰) with Settings, and the body: `AdminHome` or `studentHome`. | |
| `_showPasswordInputDialog()` | Asks for the passcode; compares with `globals.queenPassword`. Right: `isEnabled = true` unlocks Settings in the drawer. | Stops scouts changing settings by accident. |
| `writeMatchJson(key, json)` (top-level) | Writes a downloaded schedule to `MatchSchedules/<key>.json`. Used by **Get Matches**. | |

**`studentHome(context, isEnabled)`** is a plain *function* returning a
widget (not a class), the older style. It builds a `HomeTileGrid` of
`HomeTile`s; **Get Current Matches** calls `fetchMatches` (The Blue
Alliance) for `master.currentCompId` and writes the file.

**`_AdminHomeState`** has `isLoading` for the spinner on **Get Matches**,
which calls `fetchEvents('frc<team>', year)` then `fetchMatches` for each
event and `writeMatchJson` for each. The tiles open other pages with
`Navigator.push`.

## `events_page.dart`

`getEvents()` lists file names in `MatchSchedules/` (each file is one
event) and shows them as `ListTile`s. Tapping one pushes `MatchListPage`.
There's no database involved: an event "exists" on a tablet when its
schedule file does.

## `match_list_page.dart`

State: `hiveMatchListTODO`, `hiveMatchListComplete` (the two tabs; the
"hive" in the name is from an old database library), and `studentName`,
`station` and `onScoreTableSide` from settings.

| Method | What it does |
|---|---|
| `readJson()` | Reads this tablet's station etc. from `settings.json`. |
| `getMatches(eventKey)` | Parses the schedule file (see [Walkthroughs](walkthroughs.md), section 2), keeps qualification matches, sorts them, then sorts each into To do or Completed by checking the database and the match file. |
| `_myTeam(match)` | The team number this tablet scouts in a match, from its station: `"Red 2"` → `match.redAlliance[1]`. Used to outline that team's chip. |
| `_refreshPage()` | Re-runs `getMatches`. Pull-to-refresh and returning from a match call it. |
| `build()` | Two tabs of `MatchTile`s. Tapping a To do match asks to confirm; Completed matches open straight away for editing. |

```{admonition} Why are To do and Completed worked out per tablet?
:class: note
"Completed" means *this tablet's station's* robot has a record for that
match, whether this tablet scouted it or received it by sync. So after a
sync, a match scouted on another tablet for the same station also shows as
done. That's intended: it stops two people scouting the same robot.
```

## `match_scout_page.dart`: the scouting container

State: `scoutData` (the one `ScoutData` shared by all five pages),
`alliance`, `team`, `stationNum`, `_currentPage`, and a `PageController`.

| Member | What it does | Why |
|---|---|---|
| `initState()` | Works out alliance and team from the station and match; makes sure the match file exists (`getMatchFile`); creates `ScoutData`, which loads any saved data. | Everything the pages need is ready before they build. |
| `_isPreMatchLocked` | True on page 0 until a name is picked *and* a start position tapped. | Forces the two things every record needs. |
| `build()` | App bar in the alliance colour with the station chip; a `PopScope` that asks before leaving; a `PageView` of the five pages. `physics` locks swiping while `_isPreMatchLocked`; `onPageChanged` saves (`writeFile`). | Saving on every swipe means a crash or dead battery loses at most one page of input. |
| `refresh()` | `setState(() {})`. Passed to child pages as `notifyParent`. | Children change `ScoutData`, but only the parent decides whether swiping is locked, so children ask it to re-check. |
| `_onWillPop()` | "Are you sure?" dialog on Back. | Accidental Back mid-match. |

## `game_periods/prematch_page.dart`

State: `_studentName`, `_tapLocal` (where the scout tapped, in screen
pixels).

| Member | What it does |
|---|---|
| `_onTap(details, imageSize)` | Clamps the tap to the image, stores it in `ScoutData.startingPos` (pixels, for drawing it again) and `startingPosNormalized` (field feet, for analysis), then calls `notifyParent`. |
| `_multY`, `_offsetY`, `_multXRed`, … | Calibration numbers: feet = pixels × mult + offset. Red and blue use mirrored X. When **Scoring table side** is on, Y is flipped (`posY * -1 + 26`). **Re-measure them when the field picture changes.** |
| `_safeStudentName(name)` | Returns the name only if it's in the current scout list, else "Select Student Name". | 
| `initState()` / `didUpdateWidget()` | Restore name and position from `ScoutData` (e.g. when reopening a match). |
| `_buildLeftPanel()` | Scout dropdown, alliance badge, the instruction card. |
| `_buildFieldSection(flipped)` | The field image (rotated 180° on the scoring-table side), its alliance-coloured border, the robot marker, and the "Select your name first" cover. |
| `_StyledDropdown`, `_AllianceBadge`, `_InstructionCard`, `_RobotMarker` | Small private widgets used only by this page (the `_` keeps them private to the file). |

```{admonition} Why check the name against the list?
:class: tip
A `DropdownButton` crashes if its value isn't one of its items. If the
roster changed (a scout was removed), an old record's name wouldn't be in
the list. `_safeStudentName` turns that into "Select Student Name" instead
of a crash.
```

## `game_periods/auton_page.dart`

State: `changeBackground` and a `Timer`.

- `initState()` starts a 20-second timer; when it fires, `changeBackground`
  becomes true and the page fades to the theme's error colour: a reminder
  that auton is over. `dispose()` cancels the timer, which matters: a timer
  firing after the page closed would call `setState` on a dead page.
- `build()` lays out `AutonPathWidget` (the field) beside a column of
  cards: Good robot mode (`scoutData.showFiveButtons`, which makes the fuel
  counter show ±5), the `FuelCounterWidget`, and Climbed.

Drawing on the field doesn't make the page swipe sideways, because the path
widget claims the drag itself (see
[Tablet: widgets](tablet-widgets.md)). (`onDrawingStateChanged` exists for
a parent that wants to lock swiping while drawing, but `MatchScoutPage`
doesn't need it.)

## `game_periods/teleop_page.dart` and `endgame_page.dart`

**Teleop** is one `FuelCounterWidget` in a card.

**Endgame** puts three cards side by side, each made to the same height
with `IntrinsicHeight`: `ClimbSelectorWidget` plus the outpost switch,
`RobotCondition`, and `Defense`. Each widget gets the `scoutData` or a
value and an `onChanged`, and the page wraps changes in `setState`.

## `game_periods/submit_data_page.dart`

State: `commentController` (the comment text box), `_qrData` (the QR text,
null while being made), `_debounce`.

| Method | What it does | Why |
|---|---|---|
| `_buildQrMap()` | Builds the QR's data: identity plus the three sections, with the auton path compacted (`toCompactBase64`). | QR codes hold little; full path JSON would be too big. **Keep it in step with `ScoutData.toMap()`.** It currently leaves out `collected_outpost` and `score_table_side` (see [Known issues](../maintain/known-issues.md)). |
| `_generateQr()` | Turns the map into text with `compute(...)` on a background isolate, then `setState`. | Encoding on the main thread made typing stutter. |
| `_onCommentChanged(v)` | Stores the comment; regenerates the QR 0.8 s after typing stops. | Debounce ([Pattern 19](patterns.md)). |
| `_submit()` | Confirm dialog; then `writeFile()` and `Navigator.pop` back to the match list. | |

```{admonition} Something to notice
:class: note
`_submit` calls `widget.scoutData.writeFile()` without `await` before
`Navigator.pop`. The save carries on in the background and normally
finishes first, but the match list could, rarely, refresh before the save
has landed. Adding `await` (and making the button handler `async`) would
make the order certain. It's a nice small first fix.
```

## `settings_page.dart`

State: one field per setting (`masterToggle`, `station`, `scoreTable`,
`currentCompId`, …) and `_overrides` (settings changed on this tablet
since the laptop set them).

| Method | What it does |
|---|---|
| `readJson()` / `writeJson()` | Load and save `settings.json`. `writeJson` uses `AppSettings.update`, which **merges** keys into a section instead of overwriting the file, so keys saved by other pages (the Bluetooth master, the link mode) survive. |
| `_loadAdmin()` | Asks `AdminService.overrides()` which values differ from the laptop's. |
| `_resetToDesktop()` | Confirm, then `AdminService.resetToDesktop()`, then reload. |
| `_adminSummary()` | "Sent 3/14 10:22 · 18 scouts", or "None received yet". |
| `_settingsTheme(scheme)` | Colours for the `settings_ui` package, taken from the app theme. |
| `_showStationInputDialog`, `_showCurrentCompIdInputDialog` | Text-entry dialogs. |

## `bluetooth_sync_page.dart`

| Member | What it does |
|---|---|
| `pickMasterTablet(context)` (top-level) | Lists paired devices (`bondedDevices`) and saves the chosen one as `local.masterBtName` / `masterBtAddress`. Also used by Settings. |
| `_load()` | Reads whether this is the master, the station, the saved master device, the record count. |
| `_onSyncChanged()` | Listener on `BluetoothSyncService`: updates the record count and redraws. |
| `_ready()` | `ensureReady()`; shows the error in a snackbar if Bluetooth isn't usable. |
| `_toggleHosting()` / `_toggleConnection()` | Start/stop hosting (master) or connect/disconnect (scouting tablet). |
| `_controlPanel()` / `_statusPanel()` | The left and right halves of the screen. |

The page only *shows* the service. Leaving it doesn't disconnect, because
the service is a singleton ([Pattern 4](patterns.md)).

## `desktop_link_page.dart`

Shows `DesktopLinkService` (listens to it): the LoRa/TCP switch, USB devices
or network addresses, connect / start listening, counts and the activity
log. `_refresh()` reloads the USB list and addresses
(`DesktopLinkService.localAddresses`). `_saveAirBps()` saves the radio
pacing speed.

## `sync_with_workers_page.dart` (QR collection on the master)

| Method | What it does |
|---|---|
| `_onDetect(capture)` | Called by the camera scanner for each QR it sees. Parses the JSON, builds the key `<event>_Match<n>_<team>`, ignores repeats ("Already scanned"), stores it, and stops scanning. |
| `_saveToDevice()` | Writes each scanned match as a file (same naming as scouting tablets), converts each with `_recordFromQr`, and merges them into the database (`mergeRemote`), so they join Bluetooth sync and exports. |
| `_recordFromQr(qr)` | QR JSON → `ScoutRecord`: expands the compact auton path (`AutonPath.fromCompactBase64`) and uses the scan time as `updatedAt` (the QR has no edit time). |

## `dashboard_page.dart`

Covered in [Tablet: dashboards](tablet-dashboards.md).

## `export_csv.dart`

`exportCsvFlow(context)`: a dialog to pick an event (or all), then
`CsvExporter.export(eventKey: …)` writes the file, then a dialog offers
**Share** (`Share.shareXFiles`, Android's share menu).

## Unused: `queen_home_page.dart`

An older home screen, no longer used anywhere. Safe to delete (see
[Known issues](../maintain/known-issues.md)).
