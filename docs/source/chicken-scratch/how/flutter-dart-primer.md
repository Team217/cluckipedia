# Flutter and Dart crash course

Just enough Dart and Flutter to read and change this app. For more, the
official **dart.dev/language** tour and **docs.flutter.dev → Get started →
Learn** are excellent.

## Dart in five minutes

```dart
// Variables. `final` can't be reassigned; `var` can.
final team = 217;            // type worked out: int
var fuel = 0;
fuel = fuel + 1;
String name = 'Doe, Jane';   // or write the type

// A value that might be missing has a `?` on its type.
int? matchNumber;            // starts as null
final shown = matchNumber ?? 0;   // `??` = "or this if null"

// Lists and maps (a map is like a dictionary / JSON object).
final teams = [217, 33, 67];
final data = {'auton': {'auton_fuel': 6}, 'name': 'Scout 1'};

// Functions.
int total(int auton, int teleop) => auton + teleop;   // one-line form
String describe(int fuel) {
  if (fuel > 10) return 'great';
  return 'ok';
}

// Strings can include values with $ or ${…}.
print('Team $team scored ${total(3, 7)}');
```

### Classes

```dart
class ScoutRecord {
  final String id;
  final int teamNumber;
  const ScoutRecord({required this.id, required this.teamNumber});
}

final r = ScoutRecord(id: '2026x_Match1_217', teamNumber: 217);
```

Names starting with `_` (like `_save()`) are *private* to their file.

### Async: waiting for slow things

Reading a file, the database or Bluetooth takes time. Dart doesn't stop the
whole app to wait; such functions return a **`Future`**, and you `await` it
inside an `async` function:

```dart
Future<void> save() async {
  final db = await ScoutDatabase.instance.db;   // wait for the database
  await db.insert('scout_records', {...});      // wait for the write
}
```

If you forget `await`, the code carries on before the work is finished. A
common cause of "it didn't save" bugs.

### Imports

```dart
import 'package:flutter/material.dart';                 // Flutter itself
import 'package:chicken_scratch/data/scout_database.dart'; // our file: lib/data/scout_database.dart
```

`package:chicken_scratch/…` always means "the `lib/` folder of this project".

## Flutter in ten minutes

### Everything is a widget

A screen is a tree of widgets. Here's a real, small one from this project,
the big cards on the home screen (`lib/widgets/home_tile.dart`, simplified):

```dart
class HomeTile extends StatelessWidget {
  const HomeTile({super.key, required this.icon, required this.label, required this.onTap});

  final IconData icon;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Card(                          // a rounded surface
      child: InkWell(                     // makes it tappable
        onTap: onTap,
        child: Column(children: [         // things stacked vertically
          Icon(icon),
          Text(label),
        ]),
      ),
    );
  }
}
```

and it's used like `HomeTile(icon: Icons.event, label: 'View Events', onTap: …)`.

Common layout widgets: `Column` (vertical), `Row` (horizontal), `Padding`,
`SizedBox` (fixed size or a gap), `Expanded` (take the remaining space),
`Stack` (on top of each other), `ListView` (scrolling list), `Card`,
`Scaffold` (a page with an app bar).

### Stateless and stateful widgets

- A **StatelessWidget** only shows what it's given (like `HomeTile`).
- A **StatefulWidget** remembers things that change, like a counter. When
  the value changes you call **`setState`**, and Flutter re-runs `build` to
  redraw:

```dart
class _CounterState extends State<Counter> {
  int value = 0;

  @override
  Widget build(BuildContext context) => TextButton(
        onPressed: () => setState(() => value++),   // change + redraw
        child: Text('$value'),
      );
}
```

The scouting pages are stateful widgets that change a shared `ScoutData`
object and call `setState`.

### Services that tell the screen to redraw

Services like `BluetoothSyncService` extend **`ChangeNotifier`**. When
something changes they call `notifyListeners()`; pages listen
(`addListener`) and call `setState` to redraw. That's how the Bluetooth page
updates its log while syncing.

### Colours and text styles come from the theme

Don't write colours like `Colors.green` in pages. Ask the theme:

```dart
final scheme = Theme.of(context).colorScheme;
Text('Hi', style: TextStyle(color: scheme.primary));
```

so light and dark mode both work. See [Look and feel](theme.md).

### Navigation

Opening a page pushes it on a stack; Back pops it:

```dart
Navigator.push(context, MaterialPageRoute(builder: (_) => const DashboardPage()));
Navigator.pop(context);
```

### Hot reload

While `flutter run` is running, save your change and press **r** in the
terminal. The app updates in about a second and keeps its state. Press **R**
for a full restart if something looks stuck.

## Packages this app uses

From `pubspec.yaml`:

| Package | Used for |
|---|---|
| `sqflite` | The SQLite database on the tablet |
| `path_provider` | Finding the app's storage folder |
| `http` | Downloading from The Blue Alliance |
| `qr_flutter` | Drawing the QR code on the Submit page |
| `mobile_scanner` | Scanning QR codes (Sync With Workers) |
| `usb_serial` | Talking to the USB LoRa dongle |
| `share_plus` | The share menu after CSV export |
| `settings_ui` | The Settings page layout |
| `flutter_rating_bar` | Star ratings |
| `sqflite_common_ffi` *(tests only)* | Running the database in tests on a computer |

Some packages in `pubspec.yaml` (`googleapis`, `googleapis_auth`,
`google_sign_in`, `flutter_image_map`, `flutter_spinbox`, and
`number_inc_dec`, which only the unused counter box uses) are left over from
earlier seasons. See [Known issues](../maintain/known-issues.md).

To add a package: `flutter pub add <name>`, then check the APK still builds
([Build and install](../maintain/build-and-install.md)): some new package
versions need newer Android build tools than this project uses.
