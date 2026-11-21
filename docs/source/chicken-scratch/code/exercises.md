# Exercises

Practice is how this codebase stops being scary. Work through these in
order; each level builds on the last. Do them on a **branch**
(`git checkout -b practice-yourname`), so nothing reaches the real app
until you mean it to.

Answers for Level 1 are at the bottom. For the other levels, "How to check"
tells you how to know you're right; the tests and screenshots are your
friends.

---

## Level 1: read and trace (no code changes)

Use Ctrl+Shift+F, F12 and Shift+F12 (see [How to read code](reading-code.md)).

1. **Robot condition.** On the Endgame page a scout picks "Did not move".
   What exact text is saved in the record's `data`? In which file is that
   decided?
2. **To do or Completed?** Which method decides whether a match appears
   under To do or Completed on the match list? What two places does it
   check?
3. **The passcode.** Trace what happens when someone taps the lock icon and
   types a number. Where is the number compared, and with what?
4. **Two scouts, one robot.** Tablet A saves team 217, match 12 at 10:00:05.
   Tablet B saves the same at 10:00:09. After Bluetooth sync, whose version
   does every tablet keep? Which function decides?
5. **The red page.** What makes the Auton page turn red, after how long, and
   where is that number written?
6. **Fragments.** What's the largest number of fragments one bundle can
   have, and why can a bundle not be any bigger? (Look in `transfer.dart`
   or `transfer.py`.)
7. **Who listens?** `ScoutDatabase` has two streams. Name them, and for each
   say which class listens and what it does when an event arrives.
8. **The laptop's memory.** How does the companion know to ask the tablet
   only for *new* records? Which table and column hold that memory?

---

## Level 2: small changes

### 2a. A shorter auton reminder

Change the Auton page to turn red after **15** seconds instead of 20.

- *Files:* `lib/pages/game_periods/auton_page.dart`.
- *How to check:* `flutter run`, open a match, swipe to Auton and wait.

### 2b. Wait for the save

`_submit` in `submit_data_page.dart` calls `writeFile()` without `await`
(see [Tablet: screens](tablet-screens.md)). Make the Submit button wait for
the save before leaving the page.

- *Hint:* the dialog button's `onPressed` can be an `async` function.
  After an `await`, check `mounted` before using `context`.
- *How to check:* `flutter test`; submit a match on a tablet and see it move
  to Completed.

### 2c. Fix the QR code's missing fields

Make the QR code include `collected_outpost` and `score_table_side`, like
the database copy does.

- *Files:* `_buildQrMap()` in `submit_data_page.dart`.
- *Better:* build the QR from `scoutData.toMap()` and only replace
  `auton_path` with its compact form, so it can never drift again.
- *How to check:* the QR's contents on a tablet, or print the map. Scan it
  with **Sync With Workers** on another tablet and check the record.

### 2d. A clearer label

Change the Teleop counter's label from "Teleop Fuel Scored" to
"Fuel scored in teleop".

- *How to check:* `$env:SCREEN_SHOTS="C:\temp\s"; flutter test test/screens_test.dart`
  and look at `scout_teleop.png`.

---

## Level 3: features

### 3a. A new stat: "Auton + climb"

Add a dashboard stat that is 1 if the robot climbed in auton **and**
reached any endgame level, else 0.

- *Files:* `lib/analysis/metrics.dart`. Use `MetricKind.flag`, `_flag`,
  `_climbLevel`.
- *How to check:* it appears in a tile's settings; add a test in
  `test/dashboard_test.dart` checking it for two hand-made records.
- Read [Add a metric or dashboard tile](../maintain/add-metric-or-tile.md).

### 3b. A new field end to end

Follow [Worked example: add a field](../maintain/add-a-field.md) to add
`teleop_passes`. Then check the companion shows it in the **Seasons** tab
after a sync: you shouldn't need to change any companion code. Why not?

### 3c. Companion: show coverage in the Seasons tab

The Seasons tab shows how many records have each field. Add a column
showing how many **different teams** have that field.

- *Files:* `companion/chicken_companion/ui/main_window.py`
  (`_show_season_fields`), maybe a new `Store` method in `db.py`.
- *How to check:* run against the Simulator; add a test in
  `companion/tests/`.

### 3d. Export one team

Add a button to the companion's Data tab that exports only the team typed
in the filter box.

- *Files:* `main_window.py` (`_data_tab`, `_export_csv`), `csv_export.py`.

---

## Level 4: go deeper

### 4a. Test the layout engine

Write tests for `compactLayout` and `resolveLayout` in
`test/dashboard_test.dart`: a tile dropped on another pushes it down; gaps
close; a 12-wide tile can't sit beside anything. Pure functions are the
easiest code to test.

### 4b. A new admin setting

Let the laptop set whether **Good robot mode** starts on. Follow
"Adding a new admin setting" in [Admin settings](../how/admin-settings.md):
laptop storage, Admin tab, payload, tablet `pushedSettings`, and tests on
both sides.

### 4c. A new protocol message

Add a `PING` (type `0x04`) that the tablet answers with `PONG` (`0x05`),
carrying the tablet's clock time. Use it in the companion to warn when a
tablet's clock is more than a minute off (which would affect
newest-edit-wins).

- *Files:* `framing.py` + `frame.dart` (types), `client.py` (send),
  `lora_responder.dart` + `responder.py` (answer),
  [Link protocol](../reference/protocol.md) (document it).
- *Also:* add an example to `companion/tests/make_vectors.py`, regenerate
  `protocol_vectors.json`, run **both** test suites, including the
  cross-language test.

---

## Level 1 answers

1. `'Did not move for the whole match'`, the `value` of that option in
   `_options` in `lib/widgets/robot_condition_widget.dart`. The *label* shown
   is "Did not move".
2. `getMatches` in `lib/pages/match_list_page.dart`. It checks the database
   (`ScoutDatabase.instance.record(...)`) and whether the per-match file
   exists.
3. The lock's `onPressed` in `_HomePageState.build`
   (`lib/pages/home_page.dart`) calls `_showPasswordInputDialog`, parses the
   text with `int.tryParse`, and compares it with `queenPassword` from
   `lib/models/globals.dart` (which `AdminService` may have replaced with the
   laptop's passcode).
4. Tablet B's (10:00:09 is newer). `_isNewer` in `ScoutDatabase`, called by
   `mergeRemote`.
5. A `Timer.periodic` started in `_AutonPageState.initState`
   (`auton_page.dart`), with `Duration(seconds: 20)`; it sets
   `changeBackground = true`.
6. 64 (`MAX_FRAGMENTS`). The fragment index and count are one byte each in
   the header, and the ACK bitmap is sized for 64; at 190 bytes each that's
   about 12 KB, which is plenty once compressed.
7. `localChanges`: `BluetoothSyncService` sends the new record to every
   connected tablet (`_onLocalSave`). `changes`: the dashboard page reloads
   its data half a second later.
8. The `tablets` table's `last_rev` column (one row per tablet database);
   `LinkWorker.sync` reads it and polls for changes after that number.
