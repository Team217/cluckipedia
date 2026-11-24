# Autonomous with PathPlanner

**PathPlanner** is a GUI for drawing robot paths and building autonomous routines, plus a library
(PathPlannerLib) that follows them in code.

Docs: [pathplanner.dev](https://pathplanner.dev/home.html) ([offline](../../reference-materials/offline/pathplanner-reference.md))

## Concepts

| Term | Meaning |
|---|---|
| **Path** | A smooth curve with a start and end pose, with velocity and rotation along it |
| **Auto** | A sequence of paths and commands, built in the PathPlanner GUI |
| **Named command** | A command registered in code under a name, used inside autos (for example `"Intake"`) |
| **Event marker** | Triggers a named command partway along a path |
| **Robot config** | Mass, MOI, wheel and module data, set in the GUI's settings |

Paths and autos are saved as JSON files in `src/main/deploy/pathplanner/` and deployed with the code.

## Setting up the library

Configure `AutoBuilder` once, in the drive subsystem's constructor:

```java
RobotConfig config = RobotConfig.fromGUISettings();   // reads the GUI's robot config
AutoBuilder.configure(
    this::getPose,                 // Supplier<Pose2d>
    this::resetPose,               // Consumer<Pose2d>
    this::getChassisSpeeds,        // robot-relative speeds
    (speeds, ff) -> runVelocity(speeds),
    new PPHolonomicDriveController(
        new PIDConstants(5.0, 0.0, 0.0),   // translation
        new PIDConstants(5.0, 0.0, 0.0)),  // rotation
    config,
    () -> DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red, // flip paths for red
    this);
```

The AdvantageKit swerve template already includes this. The 2026 code's
`utilities/pathplanner/` also adds `LocalADStarAK`, which makes PathPlanner's on-the-fly
pathfinding replay-safe.

## Named commands

Register named commands **before** building any autos:

```java
NamedCommands.registerCommand("DeployIntake", intake.deploy());
NamedCommands.registerCommand("Intake", intakePiece());
NamedCommands.registerCommand("Shoot", shoot());
```

In the GUI, add them to an auto between paths, or as event markers along a path.

## Auto chooser

```java
private final LoggedDashboardChooser<Command> autoChooser =
    new LoggedDashboardChooser<>("Auto Choices", AutoBuilder.buildAutoChooser());

public Command getAutonomousCommand() {
  return autoChooser.get();
}
```

`LoggedDashboardChooser` (from AdvantageKit) records which auto was selected, so it works in
replay.

## Workflow

1. Draw a path in the GUI. Keep rotation targets and constraints reasonable.
2. Build an auto from paths and named commands.
3. **Run it in simulation.** Watch the robot pose and the target path in AdvantageScope.
4. Run it on the practice field at low speed, then full speed.
5. If the robot ends up in the wrong place, check odometry first (wheel radius, gyro), then the path
   follower PID.

## Testing autos

A typo in a named command doesn't break the build: PathPlanner prints a warning and runs nothing in its
place. Add the auto tests from [Unit Testing](../testing/unit-testing.md#testing-autonomous), which
check every path and named command, and run paths in simulated time.

## 2027 changes

- PathPlannerLib has 2027 alpha builds. The GUI, paths, and autos work the same way.
- WPILib removed `SendableChooser`, so use AdvantageKit's **`LoggedNetworkChooser`** (or WPILib's
  `Selectable`) for the auto chooser.
- If the team uses **Commands v3**, check whether PathPlannerLib's commands are v2 or v3. See
  [Should we use v3 in 2027?](../command-based/commands-v3.md#should-we-use-v3-in-2027)
