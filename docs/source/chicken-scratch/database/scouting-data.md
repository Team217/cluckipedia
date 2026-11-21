# Scouting data by season

Every record has a `data` field holding that season's scouting data as JSON.
This page lists every field for each season, and the file formats older
data comes in.

The laptop keeps an automatic list of each season's fields too: the
**Seasons** tab, or the [`season_fields`](companion-database.md#season_fields)
table.

## How `data` is laid out

```json
{
  "auton":   { …fields scouted during auton… },
  "teleop":  { …during teleop… },
  "endgame": { …endgame and overall… },
  "name": "Doe, Jane"
}
```

The three sections and `name` have stayed the same across seasons; the
fields *inside* them change with the game. Field names use
`lower_case_with_underscores`.

## 2026: current tablet format

Written by `ScoutData.toMap()` in `lib/models/scout_data_model.dart`.

### `auton`

| Field | Type | Meaning | Values |
|---|---|---|---|
| `starting_pos` | list of 2 numbers | Where the scout tapped on the field picture, in screen pixels | e.g. `[504.7, 0.2]` |
| `starting_pos_normalized` | list of 2 numbers | The same point converted to field feet | e.g. `[-1.47, 29.34]` |
| `auton_path` | object | The path drawn: `{"points": [{"x": 0.50, "y": 0.29}, …]}`. `x` and `y` are 0–1 across the field picture; the first point is the start | |
| `auton_fuel` | whole number | Fuel scored in auton | 0+ |
| `auton_climb` | true/false | Climbed during auton | |
| `score_table_side` | true/false | Whether the scout's field picture was flipped (so paths can be drawn the right way round) | |

### `teleop`

| Field | Type | Meaning |
|---|---|---|
| `teleop_fuel` | whole number | Fuel scored in teleop |

### `endgame`

| Field | Type | Meaning | Values |
|---|---|---|---|
| `climb_level` | text | Highest climb | `none`, `level_1`, `level_2`, `level_3` |
| `collected_outpost` | true/false | Collected from the outpost | |
| `robot_condition` | text | How the robot held up | see below |
| `defense_played` | true/false | Played defense | |
| `defense_score` | whole number | Defense rating (meaningful only if played) | 0–3 |
| `defense_against` | true/false | Was defended against | |
| `defense_against_score` | whole number | How well it coped with defense | 0–3 |
| `driving` | whole number | Driving rating | 0–3 |
| `comments` | text | Free text | |

`robot_condition` values (from `robot_condition_widget.dart`):

| Stored text | Shown as |
|---|---|
| `No` | Working fine |
| `Partially working` | Partially working |
| `A mechanism broke (elaborate in comments)` | Mechanism broke |
| `Completely stopped for part of the match` | Stopped partially |
| `Did not move for the whole match` | Did not move |
| `Fell over :(` | Fell over |

!!! note "Changing stored values"
    The stored text is what analysis and the dashboards compare against (for
    example, "Broke down" is any value except `No`). If you reword a choice,
    change only how it's *shown*, not the stored `value`, or old and new
    data won't match.

### Example

```json
{
  "auton": {
    "starting_pos": [504.67, 0.17],
    "starting_pos_normalized": [-1.47, 29.34],
    "auton_path": {"points": [{"x": 0.502, "y": 0.298}, {"x": 0.490, "y": 0.294}]},
    "auton_fuel": 6, "auton_climb": false, "score_table_side": false
  },
  "teleop": {"teleop_fuel": 81},
  "endgame": {
    "climb_level": "none", "collected_outpost": false, "robot_condition": "No",
    "defense_played": false, "defense_score": 2, "defense_against": false,
    "defense_against_score": 2, "driving": 3, "comments": ""
  },
  "name": "Scout 1"
}
```

## 2025: legacy files

Imported from `ScoutingData/2025_Data/…/Raw_JSON`. These files don't contain
the event, match or team; those come from the file name.

### `auton`

| Field | Type | Meaning |
|---|---|---|
| `starting_pos`, `starting_pos_normalized` | list of 2 numbers | As in 2026 |
| `leave_start_line` | true/false | Left the starting line |
| `auto_coral_recieved` | whole number | Coral received *(spelling as stored)* |
| `auto_algae_removed` | whole number | Algae removed from the reef |
| `auto_algae_processor` | whole number | Algae scored in the processor |
| `auto_algae_net` | whole number | Algae scored in the net |
| `auto_reef` | 4 lists of 6 numbers | Coral scored on the reef: a 4 × 6 grid of counts (reef levels × positions) |

### `teleop`

`tele_coral_recieved`, `tele_algae_removed`, `tele_algae_processor`,
`tele_algae_net` (whole numbers) and `tele_reef` (4 × 6 grid), meaning the
same as their auton versions.

### `endgame`

| Field | Type | Values |
|---|---|---|
| `climb_status` | text | `Deep`, `Shallow`, `Park`, `Fail`, or empty |
| `robot_condition` | text | Like 2026, but some files end with a full stop (`No.`, `Partially working.`, `A mechanism broke. (elaborate in comments)`, `Fell over... :(`) |
| `defense_played`, `defense_against` | true/false | |
| `defense_score`, `defense_against_score`, `driving` | whole number | |
| `comments` | text | |

!!! tip "Comparing seasons"
    Old values are imported exactly as written, including small differences
    like `No` vs `No.`. If you compare across seasons, normalise in your
    query or spreadsheet (e.g. strip a trailing full stop).

## Legacy files

The companion's **Import legacy JSON…** reads three formats. Each file is
one team in one match.

| Format | Seen in | Identity from | Auton path |
|---|---|---|---|
| **2025 app** | 2025 | File name `<event>_Match<n>_<team>.json` | none |
| **2026 QR** | most 2026 files | `"event"`, `"match"`, `"team"` inside the file (checked against the file name) | compact base64 string |
| **2026 tablet** | some 2026 files | File name | `{"points": […]}` |

What the import changes, and nothing else:

1. `event`, `match` and `team` move from the data into the record's columns.
2. A compact base64 auton path is decoded into the `{"points": […]}` form:
   two bytes per point, each 0–255 scaled to 0–1. This is exact, so nothing
   is lost, and tablets can draw it.

Identical copies like `2025mimcc_Match32_1506(1).json` are skipped. If two
files for the same record *differ*, the newer file wins and the other is
reported.

### The QR format

The QR code on the Submit page (and what **Sync With Workers** scans) is the
2026 format plus identity, with the path compacted to fit in a QR code:

```json
{"match": 10, "team": 247, "event": "2026mitr2", "name": "Scout 1",
 "auton": {"starting_pos": […], "starting_pos_normalized": […],
           "auton_path": "hAiBCH4HfAd5…", "auton_fuel": 6, "auton_climb": false},
 "teleop": {"teleop_fuel": 81},
 "endgame": {"climb_level": "none", …}}
```

## CSV columns

Both apps export CSV with the same layout (`lib/data/csv_exporter.dart` and
`companion/chicken_companion/csv_export.py`):

1. Fixed columns: `event_key`, `match_number`, `team_number`, `station`,
   `scout_name`, `device_id`, `updated_at`.
2. Then every field in `data`, named `<section>_<field>`, in the order first
   seen. `name` is left out (it's `scout_name`).
    - A short list (up to 4 plain values), like `starting_pos`, becomes
      numbered columns: `auton_starting_pos_1`, `auton_starting_pos_2`.
    - Anything longer or nested (`auton_reef`, the path's `points`) becomes
      one column of JSON text.
    - Nested objects join their names: `auton_auton_path_points`.

Exporting one season at a time keeps the columns sensible, because each
season has different fields.
