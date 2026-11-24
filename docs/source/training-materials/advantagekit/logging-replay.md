# Logging, Replay & AdvantageScope

## How logging is set up

`Robot.java` extends `LoggedRobot` and configures AdvantageKit in its constructor based on
`Constants.SystemConstants.kDeployMode`:

| Mode | Where data goes |
|---|---|
| `REAL` | A `.wpilog` file on the USB stick in the roboRIO, and live over NetworkTables |
| `SIM` | Live over NetworkTables (connect AdvantageScope to the simulator) |
| `REPLAY` | Reads a log file, runs the code as fast as possible, and writes a new `_sim` log |

Each log also records **metadata**: Git SHA, branch, build date, and whether there were uncommitted
changes. That's how we know exactly which code ran in a match.

## What to log

| Log it | How |
|---|---|
| Every sensor and motor reading | Through IO `inputs` (`Logger.processInputs`) |
| Setpoints and goals | `Logger.recordOutput("Arm/GoalRot", goal)` |
| State machine states | `Logger.recordOutput("Intake/State", state.toString())` |
| Booleans that decide things | `AtGoal`, `HasPiece`, `ReadyToShoot` |
| Poses and trajectories | `Logger.recordOutput("Odometry/Robot", pose)` (AdvantageScope shows these on the field) |
| Running commands | Already done in `Robot.java` (`Commands/All/<name>`) |

A good rule: **if a decision depends on a value, log the value.** When a match goes wrong, the
question is always "why did the robot decide that?"

:::{admonition} Keep logging fast
:class: warning
Logging runs every loop. Don't build long strings or allocate big arrays each cycle. Don't log
the same thing in many places.
:::

## Using AdvantageScope

AdvantageScope is the viewer for live data and log files.

- **Line Graph:** positions, velocities, voltages, and currents over time. The tool for tuning.
- **Odometry / 3D Field:** robot pose, vision estimates, trajectories, and game pieces. The 2026
  code puts the articulated CAD robot model in the 3D view (see `ascope_assets/`).
- **Table:** raw values at a point in time.
- **Statistics:** for example, a histogram of loop times.
- **Mechanism:** `LoggedMechanism2d` drawings.

Docs: [AdvantageScope documentation](https://docs.advantagescope.org/) ([offline](../../reference-materials/offline/advantagekit-reference.md#advantagescope))

### Pulling logs from the robot

At competition this happens **every time the robot comes back to the pit** (see the competition
procedures doc). In AdvantageScope: **File → Download Logs**, connect to the robot, and save to the
team log archive. Name or organize logs by event and match.

## Debugging from a log: a worked routine

1. Open the log. Find the moment something went wrong (enabled time, driver report, a spike in
   current).
2. Check **Commands/All**: was the command you expected actually running?
3. Check the subsystem's **inputs**: did the sensor values make sense? Was the device `connected`?
4. Check the **outputs**: what goal was set, and did `AtGoal` ever become true?
5. Check **power**: battery voltage, brownouts, and current on that motor.

Most problems are found by step 3.

## Replay

Replay lets you rerun a real match through your code on your laptop.

1. In `Constants.java`, set `kSimMode = Mode.REPLAY`.
2. Run **WPILib: Simulate Robot Code**. AdvantageKit asks for a log file (or uses the one open in
   AdvantageScope).
3. It writes `<log>_sim.wpilog` with every output recomputed.

The useful part: **add new `recordOutput` calls, then replay.** You'll see values you never logged
during the match, because every input the code saw is in the log. You can also change logic (for
example a vision filter) and see what it *would* have done.

:::{note}
Replay only matches reality if every input goes through the IO layer. See the replay rule in
[Why an IO Layer](why-io.md).
:::

## 2027 changes

- AdvantageKit **v27** (alpha) supports SystemCore and Java 25.
- Logs go to a USB drive (`/U`) by default, or to SystemCore's internal storage at
  `/home/systemcore/logs`.
- `LoggedDashboardChooser` is replaced by **`LoggedNetworkChooser`**, because WPILib removed
  `SendableChooser`.
- WPILib removed SmartDashboard and Shuffleboard. Keep publishing with `Logger.recordOutput`;
  Elastic and AdvantageScope read it.
- New: automatic logging of system stats, CAN status, NetworkTables clients, the radio, and the
  console. There's also an optional `LoggedOpModeRobot` base class.
- Replay now refuses to run on the real robot by accident.

More: [What's New for 2027](../season-2027/whats-new.md#our-libraries-in-2027).
