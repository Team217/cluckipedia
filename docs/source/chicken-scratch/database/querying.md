# Querying the data

The laptop database holds every season, so it's the place to ask questions.
**Work on a copy** of `scouting.db` so you can't damage the real one.

## With DB Browser for SQLite

1. Copy `C:\Users\<you>\ChickenScratch\scouting.db` somewhere, e.g. your
   Desktop.
2. Open the copy in DB Browser for SQLite.
3. **Browse Data** to look at tables; **Execute SQL** to run queries like the
   ones below (press ▶ or Ctrl+Enter).

## Getting values out of `data`

`json_extract(data, '$.section.field')` pulls one field out of the JSON:

```sql
SELECT event_key, match_number, team_number,
       json_extract(data, '$.teleop.teleop_fuel') AS teleop_fuel
FROM scout_records
WHERE season = 2026
LIMIT 20;
```

Not sure what a season's fields are called? Ask the data dictionary:

```sql
SELECT path, value_type, records
FROM season_fields
WHERE season = 2026
ORDER BY position;
```

## Useful queries

**Average fuel per team at an event, best first:**

```sql
SELECT team_number,
       COUNT(*) AS matches,
       ROUND(AVG(json_extract(data, '$.auton.auton_fuel')
               + json_extract(data, '$.teleop.teleop_fuel')), 1) AS avg_total_fuel
FROM scout_records
WHERE event_key = '2026mitr2'
GROUP BY team_number
ORDER BY avg_total_fuel DESC;
```

**How often each team reached each climb level:**

```sql
SELECT team_number,
       json_extract(data, '$.endgame.climb_level') AS level,
       COUNT(*) AS times
FROM scout_records
WHERE event_key = '2026mitr2'
GROUP BY team_number, level
ORDER BY team_number, level;
```

**Breakdown rate (any condition other than "No"):**

```sql
SELECT team_number,
       ROUND(100.0 * SUM(json_extract(data, '$.endgame.robot_condition') NOT IN ('No', 'No.'))
             / COUNT(*)) AS pct_broke_down
FROM scout_records
WHERE season = 2026
GROUP BY team_number
ORDER BY pct_broke_down DESC;
```

**Every comment about one team, newest event first:**

```sql
SELECT event_key, match_number, scout_name,
       json_extract(data, '$.endgame.comments') AS comment
FROM scout_records
WHERE team_number = 217 AND json_extract(data, '$.endgame.comments') <> ''
ORDER BY event_key DESC, match_number;
```

**Which matches still have no data for a station's team?** (Easier on the
tablet: the *Scouting coverage* dashboard.) Count records per match:

```sql
SELECT match_number, COUNT(*) AS robots_scouted
FROM scout_records
WHERE event_key = '2026mitr2'
GROUP BY match_number
HAVING robots_scouted < 6
ORDER BY match_number;
```

**How much did each scout do?**

```sql
SELECT scout_name, COUNT(*) AS records
FROM scout_records WHERE season = 2026
GROUP BY scout_name ORDER BY records DESC;
```

## From Python

Inside `companion/`, with the virtual environment:

```python
from chicken_companion.db import Store

store = Store(r"C:\Users\you\Desktop\scouting-copy.db")
for r in store.records(event_key="2026mitr2"):
    print(r.team_number, r.data["teleop"]["teleop_fuel"])
```

## Into a spreadsheet

**Data → Export CSV…** in the companion app (one season at a time), or the
tablet's **Export CSV**. Columns are described in
[Scouting data by season](scouting-data.md).
