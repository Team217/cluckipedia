# Subsystems & Commands

:::{admonition} 2027: Commands v2 and v3
:class: note
This page teaches **Commands v2**, which the 2026 code and most FRC code use, and which is still
included in 2027. WPILib 2027 also adds **[Commands v3](commands-v3.md)**. Learn v2 first: the ideas
(requirements, triggers, composition) carry straight over.
:::

WPILib's **command-based** framework organizes robot code into **subsystems** (the parts of the
robot) and **commands** (the things the robot does).

## Subsystems

A subsystem owns one mechanism's hardware and is the **only** code allowed to touch it.

```java
public class Intake extends SubsystemBase {
  private final RollerIO io;
  private final RollerIOInputsAutoLogged inputs = new RollerIOInputsAutoLogged();

  public Intake(RollerIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);                 // read sensors, once per loop
    Logger.processInputs("Intake", inputs);  // log them
  }

  // ---- Command factories: the public API of the subsystem ----
  public Command intake() {
    return startEnd(() -> io.setVoltage(8.0), () -> io.setVoltage(0.0));
  }

  public Command eject() {
    return startEnd(() -> io.setVoltage(-6.0), () -> io.setVoltage(0.0));
  }
}
```

The rules:

- Hardware objects are `private`. Nobody outside the subsystem calls `motor.set(...)`.
- `periodic()` runs every loop, even when no command is using the subsystem. Use it to read
  sensors and log. Don't use it to decide what the mechanism should do; that's the command's job.
- Expose behavior through **command factory methods** (methods that return a `Command`).

## Commands

A command has four parts, which the scheduler calls in this order:

```text
initialize()   once, when scheduled
execute()      every loop while running
isFinished()   checked every loop; return true to end
end(interrupted) once, when finished or interrupted
```

You rarely write a command class by hand. Subsystems have built-in factories that create commands
which automatically **require** the subsystem:

| Factory | Behavior |
|---|---|
| `runOnce(action)` | Runs `action` once and finishes immediately |
| `run(action)` | Runs `action` every loop, never finishes on its own |
| `startEnd(start, end)` | Runs `start` once, and `end` when interrupted |
| `runEnd(run, end)` | Runs `run` every loop, and `end` when interrupted |

The static `Commands` class has more: `Commands.waitSeconds(1.0)`, `Commands.none()`,
`Commands.print("hi")`, and `Commands.sequence(...)`, `Commands.parallel(...)`.

## Requirements

Every command declares which subsystems it **requires**. The scheduler guarantees that **only one
command uses a subsystem at a time**. If a new command needs a subsystem that's busy, the old command
is **interrupted** (its `end(true)` runs) and the new one starts.

This is what keeps two buttons from fighting over the same motor. Factory methods like
`run(...)` add the requirement automatically. If you use `Commands.run(...)` instead, you must pass
the subsystem yourself: `Commands.run(() -> ..., intake)`.

## Default commands

A **default command** runs whenever no other command needs the subsystem:

```java
drive.setDefaultCommand(drive.joystickDrive(() -> -controller.getLeftY(), ...));
intake.setDefaultCommand(intake.run(() -> io.setVoltage(0.0)));  // idle when not in use
```

## Example: commands that wait for a mechanism

```java
public Command deploy() {
  return run(() -> io.setPosition(PivotConstants.kDeploySetpoint))
      .until(() -> isWithinTolerance(PivotConstants.kDeploySetpoint));
}
```

`until(...)` adds an end condition. The 2026 `KrakenPivot.Deployed()` command works this way. Read
its comment for a real bug the team found: the original end condition only finished while the
pivot was *passing through* a narrow range, so it never ended if the intake was already deployed.

:::{admonition} `runOnce(...).until(...)` doesn't wait
:class: warning
`runOnce` finishes after its first loop, so `.until(...)` has nothing to wait on. If you want
the command to stay running until the mechanism arrives, use `run(...).until(...)`, or follow
`runOnce` with `Commands.waitUntil(...)`. The 2026 `Stowed()` and `SetPosition()` methods use
the `runOnce` form, so they finish immediately.
:::

## 2027 changes to v2

- `Command.schedule()` is deprecated. Schedule commands through triggers or the scheduler.
- `Subsystem.idle()` was added: a command that requires the subsystem and does nothing.
- `RamseteCommand` and the swerve/mecanum controller commands were removed. Path following comes
  from PathPlanner (or Choreo) anyway.
- Packages move from `edu.wpi.first.wpilibj2.command` to `org.wpilib`. Use autocomplete.
