# Using the apps

- [Tablet app](tablet-app.md): every screen scouts and the master tablet use.
- [Dashboards](dashboards.md): the analysis screens for the strategy team.
- [Companion app](companion-app.md): the laptop program.
- [Match day checklist](match-day.md): what to do before, during and after an
  event.

How the screens fit together on the tablet:

```mermaid
flowchart TD
    Home["Home screen"]
    Home -->|scouting tablet| SH["View Events · Get Current Matches ·<br/>Dashboards · Bluetooth Sync · Export CSV"]
    Home -->|master tablet| MH["Get Matches · Sync With Workers · View Events ·<br/>Dashboards · Bluetooth Sync · Desktop Link · Export CSV"]
    Home -->|"lock icon + passcode,<br/>then ☰ menu"| Settings
    SH & MH --> Events["Events"] --> Matches["Match list<br/>(To do / Completed)"]
    Matches --> Scout["Scouting a match:<br/>Pre-match → Auton → Teleop → Endgame → Submit"]
    Scout -->|Submit| Matches
```

```{rubric} Table of contents
```

```{toctree}
:maxdepth: 1

tablet-app
dashboards
companion-app
match-day
```
