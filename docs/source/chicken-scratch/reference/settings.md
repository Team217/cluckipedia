# Settings reference

## Tablet: `settings.json`

Stored in the app's storage folder (see
[Files on the tablet](../database/files.md)). "Set from laptop"
means the companion's Admin settings can set it (and the tablet can
override; see [Admin settings](../how/admin-settings.md)).

### `master` section

| Key | Type | Meaning | Changed by | Set from laptop |
|---|---|---|---|---|
| `isMaster` | true/false | This tablet is the master | Settings | no |
| `teamNumber` | text | Our team number, e.g. `"217"` | default file | yes |
| `year` | text | Season, e.g. `"2026"` | default file | yes |
| `currentCompId` | text | Current event key, e.g. `"2026mitr2"` | Settings | yes |

### `local` section

| Key | Type | Meaning | Changed by | Set from laptop |
|---|---|---|---|---|
| `station` | text | `Red 1` … `Blue 3` | Settings | yes (per tablet) |
| `onScoreTableSide` | true/false | Flip the field picture | Settings | yes (per tablet) |
| `masterBtName`, `masterBtAddress` | text | The master tablet to connect to over Bluetooth | Bluetooth Sync / Settings | no |
| `studentName` | text | Older field. Passed to the scouting page, but the scout's name actually comes from the Pre-match page | Settings | no |
| `playoffs` | text | Older field, not used | — | no |
| `desktopLinkMode` | `"lora"` or `"tcp"` | Desktop Link mode | Desktop Link | no |
| `tcpPort` | number | TCP port to listen on (default 5810) | Desktop Link | no |
| `tcpListening` | true/false | Resume TCP listening when the app starts | Desktop Link | no |
| `loraVid`, `loraPid` | number | The USB dongle to reconnect to automatically | Desktop Link | no |
| `loraAirBps` | number | Radio speed used to pace sending (default 4400) | Desktop Link | no |

Defaults for a brand-new tablet: `assets/config/default_settings.json`.

## Tablet: values from the laptop (`meta` table)

See [Tablet database → meta](../database/tablet-database.md).

## Companion: command line

```text
python -m chicken_companion [--db PATH]
python -m chicken_companion --import-json FOLDER [--replace] [--db PATH]
```

| Option | Meaning |
|---|---|
| `--db PATH` | Database file. Default `~/ChickenScratch/scouting.db`. |
| `--import-json FOLDER` | Import legacy raw JSON files from FOLDER and exit (no window). |
| `--replace` | With `--import-json`: replace records already in the database. |

## Companion: remembered choices

Saved per user by Qt (on Windows in the registry under
`HKEY_CURRENT_USER\Software\ChickenScratch\Companion`; on Linux in
`~/.config/ChickenScratch/Companion.conf`). Delete them to reset.

| Key | Meaning |
|---|---|
| `mode` | Connect over: `lora` or `tcp` |
| `port` | Last serial port (or the simulator) |
| `tcp_host`, `tcp_port` | Last tablet address and port |
| `radio` | Radio settings (JSON) |
| `auto_secs` | Auto-sync interval |
| `import_dir` | Last legacy import folder |
| `tba_key` | Your The Blue Alliance API key |

## Companion: database settings

Team number, season, current event, passcode and the settings version are
in the database's `admin_settings` table. See
[Companion database](../database/companion-database.md).
