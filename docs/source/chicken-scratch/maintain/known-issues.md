# Known issues

Rough edges worth knowing about, and suggested fixes. Fixing any of these is
a good first project.

## Security

### API key and credentials in the repository

`lib/models/tba.dart` contains a The Blue Alliance API key, and
`assets/tablet_pull/` contains Google credential files (`token.json`,
`json_pull_cred.json`). Anyone who can read the repository can use them.

**Fix:** ask a mentor to revoke them on TBA and Google, remove them from the
code, and load the TBA key from settings instead (the companion already asks
for your own key and keeps it out of the repository). Changing them matters
more than deleting them, because Git history keeps old versions.

### Built-in passcode

The admin passcode built into `lib/models/globals.dart` is in the source
code. Set your own from the laptop (Admin → Team & settings), which replaces
it on every tablet.

## Release signing

Release APKs are signed with the build computer's debug key
(`android/app/build.gradle`), so updates must be built on the same computer
(see [Build and install](build-and-install.md)).

**Fix:** create a release keystore once (`keytool -genkey …`, see Flutter's
"Build and release an Android app" guide), store it safely *outside* the
repository, and point `build.gradle` at it through `key.properties`.
Changing the key on tablets that already have the app needs one uninstall
and reinstall, so back the data up to the laptop first.

## The QR code misses two fields

`_buildQrMap()` in `submit_data_page.dart` lists fields by hand and doesn't
include `collected_outpost` or `score_table_side`, so matches collected by
QR scan arrive without them.

**Fix:** build the QR from `ScoutData.toMap()` (compacting `auton_path`),
so it can never drift again.

## `test/widget_test.dart` always fails

It's Flutter's template "counter" test, which never matched this app.
**Fix:** delete it.

## Leftovers from earlier seasons

| What | Status |
|---|---|
| `lib/pages/queen_home_page.dart`, `lib/widgets/counter_box_widget.dart` | Unused |
| `lib/models/student.dart`, `lib/models/score_event_model.dart` | Unused (2024) |
| `assets/config/default_match.json` | A 2025 template; overwritten on first save |
| `assets/tablet_pull/`, `assets/transfer/` | Old file-copy tools (2023) |
| Packages `googleapis`, `googleapis_auth`, `google_sign_in`, `flutter_image_map`, `flutter_spinbox`, `number_inc_dec` | Not used by current code |
| Most fonts in `assets/fonts/` | Not used by the current design |

**Fix:** remove them in a branch, run the tests and build the APK to check
nothing needed them.

## Android build tools are old

The project pins Android Gradle Plugin 8.3.1 and Kotlin 1.8.22, so some new
package versions can't be used (e.g. `share_plus` is pinned to 10.1.4).
Upgrading the tools is a self-contained project: change
`android/settings.gradle` and the Gradle wrapper, build, and test Bluetooth
and USB on a real tablet.

## Things that need real hardware to test

Bluetooth sync between tablets, LoRa dongles, admin settings relayed from
the master to other tablets, The Blue Alliance downloads, and camera QR
scanning are only partly covered by automated tests. Do a practice run
before each event.

## The red/blue swap in schedule files

Not a bug, but a trap: see
[Files on the tablet](../database/files.md).
