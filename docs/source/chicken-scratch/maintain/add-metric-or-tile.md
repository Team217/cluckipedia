# Add a metric or dashboard tile

Read [Dashboards inside](../how/dashboards-internals.md) first.

## Add a metric (a stat)

All metrics are in `Metrics.all`, in `lib/analysis/metrics.dart`. A metric
has:

```dart
Metric(
  'climb_level',            // key: stored in dashboard settings, never change it
  'Climb level',            // label shown to people
  MetricKind.level,         // count, rating, level or flag
  _climbLevel,              // function: record data → number (or null)
  higherIsBetter: true,     // false for bad things, like breakdowns
)
```

The `read` function gets the record's `data` map and returns a number, or
`null` if that match shouldn't count. Helpers at the top of the file:

| Helper | Returns |
|---|---|
| `_section(d, 'teleop')` | The `teleop` part of the data (empty if missing) |
| `_num(value)` | The number, or null if it isn't one |
| `_flag(value)` | 1 for true, 0 for false, null otherwise |

### Examples

A count straight from a field:

```dart
Metric('teleop_passes', 'Teleop passes', MetricKind.count,
    (d) => _num(_section(d, 'teleop')['teleop_passes'])),
```

A yes/no:

```dart
Metric('auton_climb', 'Auton climb', MetricKind.flag,
    (d) => _flag(_section(d, 'auton')['auton_climb'])),
```

Something worked out from several fields:

```dart
Metric('total_fuel', 'Total fuel', MetricKind.count, (d) {
  final a = _num(_section(d, 'auton')['auton_fuel']);
  final t = _num(_section(d, 'teleop')['teleop_fuel']);
  if (a == null && t == null) return null;
  return (a ?? 0) + (t ?? 0);
}),
```

A choice turned into a number (see `_climbLevel` and `_brokeDown` in the
file).

After adding one, it appears in every tile's settings. Add a test to
`test/dashboard_test.dart` that checks its value for a known record.

!!! warning "Keys are saved"
    Dashboards on tablets save metric *keys* in their settings. If you
    rename or remove a key, tiles using it fall back to the first metric.
    Prefer adding new metrics over renaming old ones.

## Add a tile type

1. **Declare it**: add a value to `enum TileType` in
   `lib/dashboard/dashboard_model.dart`, with its label, description, icon,
   default size and minimum size (in grid cells):

    ```dart
    scatter('Scatter plot', 'Two stats against each other',
        Icons.scatter_plot, 6, 5, 4, 3),
    ```

2. **Default settings**: in `defaultSettings`, add a `case` returning the
   settings a new tile starts with, e.g.
   `{'metric': 'auton_fuel', 'metric2': 'teleop_fuel', 'agg': 'avg'}`.

3. **Draw it**: in `lib/dashboard/tiles.dart`, write a widget class that
   takes `(TileConfig t, DashContext ctx)`, and add a `case` for it in
   `buildTile`. Get numbers from `ctx.data` (`stat`, `ranked`, `values`,
   `teams`, …), get colours from `Viz` (`Viz.ink`, `Viz.series[0]`, …), and
   call `ctx.selectTeam(team)` when someone taps a team. Copy the structure
   of a similar tile: `TopTeamsTile` is a good small one.

4. **Settings dialog**: if it needs options, add them in
   `lib/dashboard/tile_settings.dart`.

5. **Test**: `test/dashboard_test.dart` renders the starter dashboards. Add
   your tile to a test layout and check it builds without errors.

The palette panel in edit mode lists every `TileType` automatically.

## Change the starter dashboards

`defaultDashboards()` at the bottom of `dashboard_model.dart`. Each tile is
`_t(TileType.x, column, row, width, height, {settings})` on the 12-column
grid. Existing tablets keep their saved dashboards; **⋮ → Add the starter
dashboards** adds the new versions.
