# How a Robot Program Runs

## The loop

Robot code is not a script that runs from top to bottom and ends. It is a **loop that runs every
20 ms (50 times per second)** for as long as the robot is on.

```text
constructor (once)
   │
   ▼
┌───────────────── every 20 ms ─────────────────┐
│ robotPeriodic()        ← always                │
│   └─ CommandScheduler.run()                    │
│        ├─ subsystem.periodic() for each        │
│        └─ execute() each scheduled command     │
│ <mode>Periodic()       ← disabled/auto/teleop  │
└────────────────────────────────────────────────┘
```

Every piece of code you write gets a small slice of that 20 ms. If your code takes too long, you'll
see **"Loop time of 0.02s overrun"** warnings and the robot will respond sluggishly. That's why we
never use `while` loops that wait or `Thread.sleep()` in robot code.

## Robot modes

| Mode | When | Methods called |
|---|---|---|
| Disabled | Robot on, not enabled | `disabledInit()` once, then `disabledPeriodic()` |
| Autonomous | First 15 s (or so) of a match | `autonomousInit()`, `autonomousPeriodic()` |
| Teleop | Driver-controlled period | `teleopInit()`, `teleopPeriodic()` |
| Test | Only from the Driver Station test mode | `testInit()`, `testPeriodic()` |
| Simulation | When running on your laptop | `simulationPeriodic()` in addition to the above |

The Driver Station (or the sim GUI) chooses the mode. Motors **cannot** move while disabled. The
hardware enforces that, not our code.

## Where things live in our project

```text
src/main/java/frc/robot/
├── Main.java              ← starts the program. Don't touch.
├── Robot.java             ← the loop and modes; sets up AdvantageKit logging
├── Constants.java         ← every tunable number, CAN ID, and gear ratio
├── robotContainers/       ← creates subsystems and binds controls
├── subsystems/            ← one folder per mechanism
├── commands/              ← commands that coordinate several subsystems
└── utilities/             ← shared helpers
src/main/deploy/           ← files copied to the robot (PathPlanner paths, etc.)
vendordeps/                ← third-party library definitions (Phoenix 6, AdvantageKit, ...)
```

In the 2026 code, `Robot.java` extends `LoggedRobot` (AdvantageKit's version of `TimedRobot`) and
picks a `RobotContainer` based on `Constants.SystemConstants.kActiveBotType`. This lets one codebase
run the competition robot, a prototype, or a demo bot.

### `RobotContainer`

`RobotContainer` is where the robot gets assembled:

1. Create each subsystem, choosing the real or sim IO implementation.
2. Set default commands (for example, drive with joysticks).
3. Bind controller buttons to commands.
4. Build the autonomous chooser.

`Robot.java` asks the container for the selected autonomous command in `autonomousInit()`.

## Deploying

1. Connect to the robot over Wi-Fi, USB, or Ethernet.
2. {kbd}`Ctrl+Shift+P` → **WPILib: Deploy Robot Code** (or run `./gradlew deploy`).
3. Open the Driver Station and check for a green **Robot Code** indicator.

Code deploys to the roboRIO and starts automatically every time the robot powers on.

## Simulating

{kbd}`Ctrl+Shift+P` → **WPILib: Simulate Robot Code** → **Sim GUI**. In the Sim GUI:

- Use **Robot State** to switch between Disabled, Autonomous, and Teleop.
- Drag a keyboard or a connected controller onto **Joysticks** slot 0.
- Open AdvantageScope and connect to the simulator (**File → Connect to Simulator**) to see
  everything the code logs.

## 2027 changes

- **SystemCore** replaces the roboRIO. Deploying still uses **WPILib: Deploy Robot Code**. Set the
  team number on SystemCore's web page (`http://robot.local`).
- **`robotInit()` is removed.** Startup code goes in the `Robot` constructor (the 2026 code already
  does this).
- **Test mode is renamed Utility mode.** Check autocomplete for the matching method names.
- **OpModes:** WPILib adds an optional FTC-style structure (`OpModeRobot`) where each selectable mode
  is its own class, chosen on the new Driver Station. `TimedRobot` / `LoggedRobot` still work.
- **New Driver Station** for Windows, macOS, and Linux, with an A-Stop button and OpMode selection.
- With **Commands v3**, the loop runs `Scheduler.getDefault().run()`, and mechanisms have no
  `periodic()`. Call each mechanism's update method from `robotPeriodic()` first.
  See [Commands v3](../command-based/commands-v3.md).

More: [What's New for 2027](../season-2027/whats-new.md).
