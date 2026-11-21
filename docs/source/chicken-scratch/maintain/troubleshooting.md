# Troubleshooting

## Building and running

| Problem | Try |
|---|---|
| `flutter` isn't recognised | Flutter's `bin` folder isn't on your PATH, or the terminal was opened before you added it. Open a new terminal. |
| `flutter doctor` shows ✗ Android toolchain | Install Android SDK Command-line Tools in Android Studio, then `flutter doctor --android-licenses`. |
| Build fails after adding/upgrading a package | The package may need newer Android build tools than this project uses (Android Gradle Plugin 8.3.1, Kotlin 1.8.22). Pin an older version in `pubspec.yaml` (like `share_plus: 10.1.4`) or upgrade the tools in `android/settings.gradle` and test carefully. |
| "App not installed" / signatures don't match | The APK was built on a different computer. See [Build and install](build-and-install.md). |
| `flutter run` doesn't see the tablet | USB debugging off, cable is charge-only, or you didn't accept the prompt on the tablet. Try `adb devices`. |
| Companion: `No module named PyQt6` | You ran plain `python` instead of `.venv\Scripts\python`, or haven't run `pip install -r requirements.txt`. |
| Companion window doesn't open on Linux | Install your distro's Qt/xcb libraries (e.g. `sudo apt install libxcb-cursor0`). |

## Scouting

| Problem | Try |
|---|---|
| **View Events** is empty | No schedules on this tablet. Push from the laptop (Admin → Send to tablets, then Bluetooth sync), or **Get Current Matches** with internet. |
| Can't swipe past Pre-match | Pick a scout name *and* tap the starting position. |
| Scout list is wrong or old | The laptop roster hasn't reached this tablet. Bluetooth sync with the master after **Send to tablets**. |
| Wrong team shown for a match | The tablet's **station** is wrong (Settings), or the schedule changed. Check Admin → Tablets on the laptop. |
| A match shows in **Completed** but wasn't scouted on this tablet | Another tablet scouted it and it arrived by sync. That's normal. |
| Field picture is upside down | Toggle **Scoring table side** in Settings. |

## Bluetooth

See the table in [Bluetooth sync](../how/bluetooth-sync.md#common-problems).
Quick checks: Bluetooth on, tablets paired, master is **hosting**,
**Nearby devices** permission granted.

## Laptop ⇄ tablet

| Problem | Try |
|---|---|
| "No answer from the tablet" (LoRa) | Both dongles written with the same **Radio settings**? Tablet's Desktop Link shows the dongle **connected**? Antennas on, within range? |
| Can't connect (TCP) | Tablet's Desktop Link → TCP → **Start listening**. Same network? Guest Wi-Fi often blocks devices talking to each other; use a phone hotspot or a USB cable. Firewall on the laptop? |
| Testing with the Android **emulator** (TCP): connects but "No answer from the tablet" | The emulator console's `redir add` accepts the connection but doesn't deliver data on newer emulators. Use adb instead: `adb forward tcp:5811 tcp:5810`, then connect the companion to `127.0.0.1` port `5811`. Re-run after restarting the emulator; `adb forward --list` shows active forwards. |
| Tablet dongle won't connect | Note its VID/PID on the Desktop Link page. It may use a USB chip not in `usb_device_filter.xml`. The app also tries the generic CDC driver. |
| "The master tablet didn't take the settings" | The tablet app is too old. Install the latest APK. |
| Sync seems to miss records | On the Sync tab, select the tablet, **Re-read everything from selected tablet**, then **Sync now**. |

## Data

| Problem | Try |
|---|---|
| Duplicate-looking records | Records are keyed by event/match/team, so true duplicates can't exist. Two scouts on the *same* robot overwrite each other (newest wins): check station assignments. |
| Old season's data "missing" in the companion | The Data tab shows one season at a time. Pick it in **Season**. |
| Legacy import lists problems | Read the reason next to each file. Usually broken JSON or a file name not in `<event>_Match<n>_<team>.json` form. |
| Need to fix a record by hand | Edit it on a tablet (open the match, change, Submit): the new edit wins everywhere after sync. Avoid editing the database files directly. |

Still stuck? Write down exactly what you did and what you saw (a photo of
the screen helps), and ask a mentor or open an issue on GitHub.
