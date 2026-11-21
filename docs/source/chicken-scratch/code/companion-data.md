# Companion: data

The laptop's storage and data tools: `db.py`, `admin.py`,
`legacy_import.py`, `csv_export.py`, `radio_config.py`, `sim_data.py` and
`__main__.py`. Table-by-table details are in
[Companion database](../database/companion-database.md); this page is about
the code.

## SQLAlchemy in one minute

Each table is a Python class; each row is an object of that class:

```python
class Scout(Base):
    __tablename__ = "scouts"
    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String, unique=True)
```

You work with rows inside a **session**:

```python
with store.Session() as s:                  # read-only work
    sc = s.get(Scout, 3)                    # row with primary key 3
    names = s.scalars(select(Scout.name)).all()

with store.Session.begin() as s:            # writes, saved together at the end
    s.add(Scout(name="Doe, Jane"))
```

`Session.begin()` **commits** (saves) everything at the end of the `with`
block, or **rolls back** (saves nothing) if an error happens inside it. The
database is never left half-updated.

## `db.py`

### Module-level helpers

| Function | What it does |
|---|---|
| `season_of(event_key)` | The first four digits as a year, or `None`. |
| `_newer(a_time, a_dev, b_time, b_dev)` | The newest-wins rule ([Pattern 2](patterns.md)). |
| `_value_type(v)` | `bool`, `int`, `float`, `text` or `list`. `bool` is checked **before** `int`, because in Python `True` is also an `int`. |
| `field_paths(data)` | Every leaf in a nested dict as `("auton.auton_fuel", "int")`, in order. Recursive: it calls itself for nested dicts. |

### `Store`

| Method | What it does | Why |
|---|---|---|
| `__init__(path)` | Make the engine (the connection to the file), run `_migrate`, make the `Session` factory. | `check_same_thread=False` lets the worker thread use its own `Store` safely. |
| `_migrate()` | Create missing tables; add the `season`/`source` columns to old databases (`ALTER TABLE`); fill in missing seasons; write the schema version; rebuild the data dictionary if needed. | New *tables* are easy (SQLAlchemy's `create_all`), but new *columns* on existing tables need explicit SQL. |
| `schema_version()` | The stored version number. | |
| `_apply(row, record, source)` | Copy a record dict onto a row object (including `season`). | Shared by `merge` and `import_records`. |
| `merge(records, source)` | Newest-wins merge of tablet records; returns how many were new or newer; refreshes the data dictionary for the seasons touched. | |
| `import_records(records, replace)` | Add missing records; replace existing ones only if `replace`. Returns `(added, replaced)`. | Legacy files have no trustworthy edit time, so "newest wins" doesn't apply; the user decides. |
| `existing_ids(ids)` | Which of these IDs are already stored (asked 500 at a time). | SQLite limits how many values one `IN (…)` query may hold. |
| `records(event_key, season)`, `events(season)`, `seasons()`, `count()` | Queries for the Data and Seasons tabs. | |
| `refresh_fields(seasons)` | Rebuild `season_fields` for the given seasons: walk every record's `data` with `field_paths`, count each path, note its types, keep first-seen order. | Always matches what's stored, because it's recalculated rather than updated piece by piece. |
| `fields(season)` | That season's dictionary rows, in order. | |
| `tablet(db_id)`, `tablets()`, `record_poll(...)`, `forget_tablet(db_id)` | How far the laptop has read from each tablet. | |

## `admin.py`

| Item | What it does |
|---|---|
| `schedule_hash(matches)` | SHA-1 of the schedule as sorted, compact JSON, first 16 characters. Same schedule → same fingerprint, so tablets can skip unchanged ones. |
| `sort_matches(matches)` | Quals first, then playoffs, by number. |
| `parse_tablet_schedule(text)` | Read a tablet's `MatchSchedules` file, **swapping red and blue back**. |
| `_tba_get(path, key)` / `tba_team_events` / `tba_event_schedule` / `tba_event_name` | The Blue Alliance's web API with Python's built-in `urllib`. Errors become `TbaError` with a readable message (bad key, no such event, offline). |
| `TeamSettings` (`@dataclass`) | Team number, season, current event, passcode. |
| `Admin(store)` | Wraps the admin tables. |

**`Admin` methods:**

| Method | What it does |
|---|---|
| `_get` / `_set` | Read / write an `admin_settings` row. |
| `_bump(s)` | Raise the settings version to `max(now in ms, old + 1)` ([Pattern 14](patterns.md)). Every change calls it. |
| `version`, `ensure_version()`, `pushed_version`, `note_tablet_version(v)` | The laptop's version, and the one the master last reported. |
| `team()` / `set_team(t)` | Read / save team settings (checking digits); only bumps if something changed. |
| `scouts`, `add_scouts`, `rename_scout`, `set_scout_active`, `remove_scouts`, `sort_scouts`, `names_in_data` | The roster. `add_scouts` tidies spaces and ignores names already there (case doesn't matter). |
| `events`, `event`, `upsert_event`, `set_schedule`, `set_event_send`, `remove_event` | Events and schedules. "Upsert" = update if it exists, insert if not. `set_schedule` returns `False` if nothing changed. |
| `devices`, `note_devices`, `note_devices_from_records`, `set_device`, `forget_device` | Tablets and assignments. `set_device`'s `score_table_side` defaults to the text `"keep"` because `None` already means "leave as set on the tablet". |
| `payload()` | Build `(config, schedules)` to push: settings, active scouts, assignments, and the ticked events with schedules. |

## `legacy_import.py`

| Item | What it does |
|---|---|
| `NAME_RE`, `COPY_RE` | Regular expressions for `<event>_Match<n>_<team>.json` and for a copy suffix like `(1)`. |
| `record_id(...)` | Same ID scheme as the tablet. |
| `decode_compact_path(text)` | Base64 → bytes → points (÷255). |
| `parse_file(path)` | One file → one record dict, or `ValueError` with the reason. |
| `scan(root, store)` | Read everything into an `ImportPlan` (records, duplicates, problems, per-event summary) **without writing anything**. |
| `run(plan, store, replace, events)` | Write the plan (optionally only chosen events). |
| `ImportPlan`, `EventSummary`, `FileProblem` | `@dataclass`es holding the results. |

Separating `scan` from `run` is what lets the dialog show "what would
happen" first (a *dry run*). Sorting so originals come before their `(1)`
copies matters because Python sorts `(` before `.`; see the comment in
`scan` (a test caught this).

## `csv_export.py`

`flatten` and `build_rows` match the tablet's `CsvExporter` exactly (same
column names and order), `to_csv` writes with Python's `csv` module (which
handles quoting), and `export(records, path)` saves the file.

## `radio_config.py`

**`RadioSettings`** (`@dataclass`): band, channel, spreading factor,
bandwidth, coding rate, power, network ID, address.

| Member | What it does |
|---|---|
| `frequency_mhz` | 850 + channel (HF dongle) or 410 + channel (LF). Channel 65 → 915 MHz. |
| `approx_bps()` | The LoRa bit rate formula: SF × bandwidth ÷ 2^SF × 4 ÷ (4 + CR). SF7, 125 kHz, CR 4/5 → about 5,470 bit/s. The window uses 80% of this to pace sending. |
| `commands()` | The `AT+…` commands that set the dongle. |
| `to_dict` / `from_dict` | Save in the user settings. `from_dict` ignores unknown keys, so old saved settings still load. |
| `apply_settings(port, settings, log)` | Open the port; send `+++` (enter command mode); each command; `AT+EXIT`; log every reply. |

## `sim_data.py`

`fake_store(matches, seed)` builds a `MemoryStore` with a made-up event
(`2026sim`), six "tablets" and records shaped like real ones. A fixed `seed`
makes it the same every run.

## `__main__.py`

Runs when you type `python -m chicken_companion`. `main()` reads the
command-line options with `argparse` (`--db`, `--import-json`,
`--replace`); `default_db_path()` makes `~/ChickenScratch`; `import_json`
prints a plan and imports without opening a window. PyQt is imported only
when the window is needed, so the command-line import works even where Qt
can't start (e.g. over SSH).
