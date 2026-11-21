# Build and install

## Build the APK

From the project folder:

```powershell
flutter pub get
flutter test                 # make sure nothing is broken first
flutter build apk --release
```

The APK is at `build\app\outputs\flutter-apk\app-release.apk` (about 33 MB).
The first build takes several minutes; later ones are faster.

Before building a version for an event, bump `version:` in `pubspec.yaml`
(e.g. `0.1.0` → `0.2.0`) so you can tell builds apart.

!!! danger "Build from the same computer every time"
    Release builds are currently signed with the **debug key of the computer
    that builds them** (see `android/app/build.gradle`). Android only lets an
    update install over an app signed with the same key. An APK built on a
    *different* computer fails with "App not installed" or "signatures do not
    match". The only way round that is **uninstalling, which deletes the
    tablet's data**.

    So: pick one "build computer", or set up a shared release key (see
    [Known issues](known-issues.md#release-signing)). If you must uninstall,
    **sync the tablet to the laptop first** and restore afterwards.

## Install on a tablet

=== "USB cable (recommended)"

    With USB debugging on (see [Your first day](../start/first-day.md#5-run-the-tablet-app)):

    ```powershell
    flutter install
    ```

    or, with the Android SDK's `adb`:

    ```powershell
    adb install -r build\app\outputs\flutter-apk\app-release.apk
    ```

    `-r` replaces the existing app and **keeps its data**.

=== "Copy the file"

    1. Copy the APK to the tablet (USB file transfer, Google Drive, …).
    2. Open it in the tablet's **Files** app.
    3. Allow "install unknown apps" for Files when Android asks.
    4. **Update**. Data is kept.

Repeat on all six tablets. Then open the app on each and check the version
under **Settings → Apps → Chicken Scratch**.

## After installing

- The first launch after an update upgrades the tablet's database if needed
  (automatic, keeps data).
- Grant **Nearby devices** (Bluetooth) when asked, and **USB access** for a
  LoRa dongle.

## Getting files off a tablet

```powershell
adb pull /sdcard/Android/data/com.example.chicken_scratch/files/ .\tablet-files
```

copies `settings.json`, schedules and per-match JSON files to your computer
(see [Files on the tablet](../database/files.md)).

## BlueStacks (testing on a PC)

The APK installs on BlueStacks (drag the APK onto its window). Most screens
work, but there's **no Bluetooth** and no USB dongle, so you can't test
sync or LoRa there. TCP mode works if BlueStacks's network can reach the
laptop.

## Build the companion app?

There's nothing to build: it runs from the source with Python. To set it up
on another laptop, copy (or `git clone`) the project and follow
[Your first day](../start/first-day.md#3-set-up-and-test-the-companion-app).
Copy `scouting.db` across too if you want the existing data.
