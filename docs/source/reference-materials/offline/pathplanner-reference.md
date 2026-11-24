# PathPlanner Reference

Local summary of the PathPlanner documentation linked from
[Autonomous with PathPlanner](../../training-materials/autonomous/pathplanner.md).
Source: [pathplanner.dev](https://pathplanner.dev/home.html)

## Opening the project

Open the PathPlanner app and choose the **robot project folder** (the folder with `build.gradle`).
PathPlanner reads and writes `src/main/deploy/pathplanner/`:

```text
src/main/deploy/pathplanner/
├── settings.json      ← robot config from the Settings page
├── paths/*.path       ← one file per path
└── autos/*.auto       ← one file per auto
```

These are text files, so commit them to Git like code. The 2026 project has many paths, such as
`Chaos-Top.path` and `MVP-Mid.path`.

## Settings (robot config)

Open **Settings** (gear icon). Accurate values here make path following work well:

| Setting | Notes |
|---|---|
| Robot mass (kg), MOI (kg·m²) | Include bumpers and battery. Get MOI from CAD or estimate. |
| Bumper size and offset | Draws the robot outline on the field |
| Holonomic mode | On, for swerve |
| Module locations / wheelbase and track width | Match `TunerConstants` |
| Wheel radius | Use the measured value from wheel-radius characterization |
| Drive gearing, true max drive speed | From `TunerConstants` and measurements |
| Wheel COF (coefficient of friction) | About 1.0–1.2 for typical tread; lower it if wheels slip |
| Drive motor type and current limit | Kraken X60 (FOC or not) and the slip current |
| Default constraints | Max velocity, acceleration, angular velocity, angular acceleration |

`RobotConfig.fromGUISettings()` in code reads this file, so the GUI and code always agree.

## Editing paths

- **Waypoints** (big dots) are positions the robot passes through. Drag to move. The smaller
  **control points** shape the curve's direction and tightness.
- **Rotation targets** set the robot's heading at a point along the path. Swerve can face any
  direction while moving.
- **Ideal starting state** sets the starting heading and velocity (for the preview and for
  resetting odometry).
- **Goal end state** sets the final heading and velocity. End velocity 0 means stop; non-zero lets
  the robot flow into the next path.
- **Global constraints** set max velocity, acceleration, and angular limits for the whole path.
- **Constraint zones** set slower limits over part of the path (for example near a game piece).
- **Point towards zones** make the robot face a field point over part of the path.
- **Event markers** run a named command at a position on the path, or over a range.
- **Reversed** (differential drive only) drives the path backwards.
- **Linked waypoints** share a position between paths, so moving it once updates every path
  that uses it. This keeps autos consistent.
- The **preview** animates the robot following the path, with the time shown.

## Building autos

An auto is a tree of commands built from:

| Block | Behavior |
|---|---|
| Path | Follow a path |
| Named command | Run a command registered in code |
| Wait | Wait some seconds |
| Sequential group | Run children in order |
| Parallel group | Run children together; finish when all finish |
| Deadline group | Finish when the first child finishes |
| Race group | Finish when any child finishes |

Set **Reset Odometry** on an auto so the robot's pose is set to the first path's start at the
beginning. Choose a **folder** to organize autos.

## PathPlannerLib API

| API | Purpose |
|---|---|
| `AutoBuilder.configure(...)` | One-time setup: pose supplier, reset, speeds, output, controller, config, flip, drive subsystem |
| `NamedCommands.registerCommand(name, cmd)` | Register before building autos |
| `new PathPlannerAuto("Auto Name")` | Load an auto as a command |
| `AutoBuilder.buildAutoChooser()` | A `SendableChooser` with every auto |
| `PathPlannerPath.fromPathFile("Path Name")` + `AutoBuilder.followPath(path)` | Follow one path from code |
| `AutoBuilder.pathfindToPose(pose, constraints)` | Generate a path on the fly to a pose, avoiding field obstacles |
| `AutoBuilder.pathfindThenFollowPath(path, constraints)` | Pathfind to a path's start, then follow it |
| `PathPlannerLogging.setLogActivePathCallback(...)` / `setLogTargetPoseCallback(...)` | Hand the active path and target pose to `Logger.recordOutput`, to see them in AdvantageScope |
| `Pathfinding.setPathfinder(new LocalADStarAK())` | Our replay-safe pathfinder (in `utilities/pathplanner/`) |
| `new EventTrigger("name")` | A `Trigger` that is true while inside an event marker's range |

**Alliance flipping:** paths are drawn for the blue alliance. When the flip supplier passed to
`AutoBuilder.configure` returns true (red alliance), every path is mirrored automatically.

## Troubleshooting

| Problem | Check |
|---|---|
| Auto chooser is empty | Autos are in `src/main/deploy/pathplanner/autos`; code deployed after saving |
| Named command does nothing | Registered **before** `buildAutoChooser()`, and the name matches exactly |
| Robot starts in the wrong place | Reset odometry is on; the robot is placed where the path starts; alliance flip is correct |
| Robot overshoots or lags paths | Robot config (mass, MOI, max speed) and translation PID; check odometry accuracy first |
| Robot wobbles in heading | Rotation PID too high, or gyro problems |
| Works in sim, not on robot | Wheel radius, wheel COF, or real max speed differs from settings |
