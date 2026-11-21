# Database basics

If you've used a spreadsheet, you already understand most of this.

## Tables, rows and columns

A **database** holds **tables**. A table is like a spreadsheet tab: each
**row** is one thing (one scouted robot in one match) and each **column** is
one fact about it (team number, match number, …).

| id | event_key | match_number | team_number | scout_name |
|---|---|---|---|---|
| 2026mitr2_Match1_217 | 2026mitr2 | 1 | 217 | Doe, Jane |
| 2026mitr2_Match1_33 | 2026mitr2 | 1 | 33 | Roe, Rick |

Unlike a spreadsheet:

- every column has a **type** (text, whole number, decimal number, …),
- a **primary key** column (here `id`) is unique: no two rows can share it,
- programs can search millions of rows in a blink using **indexes** (like a
  book's index, built automatically on chosen columns).

## SQLite

We use **SQLite**: the whole database is a single file. There's no server to
run; the app opens the file directly. You can copy the file to back it up,
and open it with **DB Browser for SQLite** to look inside.

## SQL

**SQL** is the language for asking a database questions:

```sql
-- every record for team 217 at Troy 2, in match order
SELECT match_number, scout_name
FROM scout_records
WHERE event_key = '2026mitr2' AND team_number = 217
ORDER BY match_number;
```

More examples in [Querying the data](querying.md).

## JSON inside a column

The game changes every year: 2025 had coral and algae; 2026 has fuel. If
each scouting field were its own column, we'd redesign the database every
season and old seasons wouldn't fit.

So we split each record into:

- **columns** for what *every* season has: event, match, team, scout,
  station, who edited it and when;
- a **`data` column holding JSON** for that season's game-specific fields:

```text
{
  "auton":   {"auton_fuel": 6, "auton_climb": false, "starting_pos": [504.7, 0.2], …},
  "teleop":  {"teleop_fuel": 81},
  "endgame": {"climb_level": "level_2", "driving": 3, "comments": "fast cycles", …},
  "name": "Doe, Jane"
}
```

SQLite can still reach inside the JSON: `json_extract(data, '$.teleop.teleop_fuel')`.

## Schema and migrations

The **schema** is the list of tables and columns. When we change it (add a
column, add a table), old database files on tablets and laptops still have
the old schema. **Migration** code upgrades them in place, keeping all data.
Both apps store a **schema version** number so they know which upgrades a
file needs:

| Database | Current version | Where the upgrade code is |
|---|---|---|
| Tablet | 3 | `_open()` in `lib/data/scout_database.dart` |
| Laptop | 3 | `_migrate()` in `companion/chicken_companion/db.py` |

```{admonition} Never change a table without a migration
:class: danger
If you add a column but no upgrade code, every existing tablet or laptop
database breaks the moment the new app opens it. The database pages
explain how to add a migration safely.
```

## Diagrams in these pages

The database pages use **entity-relationship diagrams**: each box is a
table, its lines are columns (`PK` = primary key, `FK` = refers to another
table's key), and lines between boxes show how tables relate.
