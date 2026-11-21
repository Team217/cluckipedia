# Worked example: add a field

Let's say the strategy team wants to count how many times a robot **passes**
fuel to a partner during teleop. We'll add a field `teleop_passes`, end to
end: save it, show it on the Teleop page, put it in the QR code, and show it
on the dashboards.

Make a branch first: `git checkout -b add-teleop-passes`.

## 1. Store it in `ScoutData`

`lib/models/scout_data_model.dart`.

**Add a variable** near the other teleop field:

```dart
  // Teleop
  int teleopFuelScored = 0;
  int teleopPasses = 0;            // ← new
```

**Save it**: in `toMap()`, add it to the `teleop` section:

```dart
        "teleop": {
          "teleop_fuel": teleopFuelScored,
          "teleop_passes": teleopPasses,      // ← new
        },
```

**Load it**: in `_applyMap()`, inside the `teleopMap != null` block.
Note the `?? 0`: records saved before this change don't have the field, and
must still load.

```dart
      teleopPasses =
          (teleopMap['teleop_passes'] as num?)?.toInt() ?? 0;   // ← new
```

That's all it takes for the field to be saved in the database, synced over
Bluetooth, sent to the laptop and exported to CSV (as `teleop_teleop_passes`).
No database change is needed.

## 2. Show it on the Teleop page

`lib/pages/game_periods/teleop_page.dart` shows one counter in a card. Put
two side by side:

```dart
          Expanded(
            child: Center(
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SizedBox(
                    width: 320,
                    child: ScoutCard(
                      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
                      child: FuelCounterWidget(
                        scoutData: widget.scoutData,
                        label: 'Teleop Fuel Scored',
                        value: widget.scoutData.teleopFuelScored,
                        onChanged: (v) => setState(
                            () => widget.scoutData.setTeleopFuelScored = v),
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  SizedBox(
                    width: 320,
                    child: ScoutCard(
                      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
                      child: FuelCounterWidget(
                        scoutData: widget.scoutData,
                        label: 'Passes to a partner',
                        value: widget.scoutData.teleopPasses,
                        onChanged: (v) => setState(
                            () => widget.scoutData.teleopPasses = v),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
```

`setState` makes the page redraw with the new number. The value is saved
when the scout swipes to another page or submits.

Try it: `flutter run` on a tablet, or render the screens:

```powershell
$env:SCREEN_SHOTS = "C:\temp\shots"; flutter test test/screens_test.dart
```

and open `C:\temp\shots\scout_teleop.png`.

## 3. Put it in the QR code

`lib/pages/game_periods/submit_data_page.dart`, `_buildQrMap()`:

```dart
        "teleop": {
          "teleop_fuel": widget.scoutData.getTeleopFuelScored,
          "teleop_passes": widget.scoutData.teleopPasses,    // ← new
        },
```

## 4. Show it on the dashboards

`lib/analysis/metrics.dart`, add to `Metrics.all`:

```dart
    Metric('teleop_passes', 'Teleop passes', MetricKind.count,
        (d) => _num(_section(d, 'teleop')['teleop_passes'])),
```

Every tile's stat picker now offers **Teleop passes**: rankings, top teams,
trends, comparisons. Records from before the change return `null` for it and
are left out of averages, rather than counting as zero.

## 5. Test

Add a check to `test/scout_data_store_test.dart` or a new test, for example
that a `ScoutData` with `teleopPasses = 4` produces
`toMap()['teleop']['teleop_passes'] == 4`. Then run everything:

```powershell
flutter test
cd companion; .venv\Scripts\python -m pytest; cd ..
```

## 6. Document it

Add `teleop_passes` to the season's table in
[Scouting data by season](../database/scouting-data.md) and the stat list in
[Dashboards](../use/dashboards.md).

## 7. Share it

```powershell
git add -A
git commit -m "Count teleop passes"
git push -u origin add-teleop-passes
```

and open a pull request ([Git and teamwork](git-workflow.md)).

```{admonition} What you didn't have to touch
:class: tip
The database, Bluetooth sync, the LoRa/TCP link, CSV export and the whole
companion app. They all carry the `data` JSON as it is, which is why the
project stores game fields that way.
```
