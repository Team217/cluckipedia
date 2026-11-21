# Testing

Automated tests are small programs that check the real code. Run them
**before every commit**. They take about a minute and catch most mistakes
before they reach a tablet.

## Running them

```powershell
# Tablet app (from the project folder)
flutter test

# One file, or tests whose name contains some text
flutter test test/admin_test.dart
flutter test --plain-name "merge keeps"

# Companion app
cd companion
.venv\Scripts\python -m pytest          # all
.venv\Scripts\python -m pytest -k tcp   # names containing "tcp"
.venv\Scripts\python -m pytest -x -q    # stop at the first failure, quiet
```

Expected right now: every tablet test passes except `test/widget_test.dart`
(a leftover template; see [Known issues](known-issues.md)), and every
companion test passes. The two `test_cross_language` / Dart TCP tests are
*skipped* if Dart isn't installed; with Flutter installed, they run.

## What's covered

| Area | Tests |
|---|---|
| Saving, merging, manifests, CSV | `test/scout_data_store_test.dart`, `companion/tests/test_store.py` |
| Database upgrades | `test/db_migration_test.dart`, `test_legacy_import.py`, `test_admin.py` |
| LoRa/TCP protocol | `test/lora_test.dart`, `test_protocol.py`, `test_tcp.py` |
| Python ⇄ Dart agree | `test_cross_language.py`, `test_tcp.py` (run the real Dart tablet code) |
| Admin settings | `test/admin_test.dart`, `test_admin.py` |
| Legacy import | `test_legacy_import.py` |
| Dashboards | `test/dashboard_test.dart` |
| Every screen builds (light and dark) | `test/screens_test.dart` |

## What isn't covered (test by hand)

- Bluetooth between real tablets.
- A real LoRa dongle and radio.
- Downloading from The Blue Alliance.
- Camera QR scanning.
- How it *feels* to use with fingers in a noisy arena.

Before every event, do a short practice run on the real tablets (see the
[Match day checklist](../use/match-day.md)).

## Seeing the screens

```powershell
$env:SCREEN_SHOTS = "C:\temp\shots"; flutter test test/screens_test.dart
$env:DASHBOARD_SHOTS = "C:\temp\dash"; flutter test test/dashboard_test.dart
```

writes a PNG of every screen and starter dashboard. Look at them after any
visual change.

## Writing a test

Copy a nearby test and change it. A Dart test looks like:

```dart
test('total fuel adds auton and teleop', () {
  final m = Metrics.get('total_fuel');
  expect(m.read({'auton': {'auton_fuel': 3}, 'teleop': {'teleop_fuel': 7}}), 10);
});
```

A Python test is a function starting with `test_` using `assert`:

```python
from chicken_companion.db import season_of

def test_season_comes_from_the_event_key():
    assert season_of("2026mitr2") == 2026
    assert season_of("practice") is None
```

`tmp_path` (Python) gives a fresh folder, handy for a throwaway database:
`Store(tmp_path / "t.db")`.

## When a test fails

1. Read the message: it says what was expected and what happened.
2. Run just that test while you fix it.
3. If *your* change made an old expectation wrong on purpose (e.g. a renamed
   field), update the test, and say so in your commit message.
4. Never delete or skip a test just to get a green run.
