# Migrating from Commands v2 to v3

How each v2 idea maps to [Commands v3](commands-v3.md), then a complete before/after of the arm
subsystem from [Building an IO Subsystem](../advantagekit/io-subsystem.md).

Source: [WPILib "Migrating from Commands v2"](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/migration-guide.html)
(alpha; names may change before kickoff).

## Concept map

| Commands v2 | Commands v3 |
|---|---|
| `class X extends SubsystemBase` | `class X implements Mechanism` |
| `periodic()` (called automatically) | Your own `updateInputs()` method, called from `Robot.robotPeriodic()` before the scheduler runs |
| `CommandScheduler.getInstance().run()` | `Scheduler.getDefault().run()` |
| Command class with `initialize` / `execute` / `isFinished` / `end` | One `run(coroutine -> { setup; loop with yield; cleanup })` body |
| `runOnce(action)`, `InstantCommand` | `run(coroutine -> action)` with no yield, or `Command.noRequirements(c -> action)` |
| `run(action)` (repeat forever) | `runRepeatedly(action)` |
| `startEnd(start, end)` / `runEnd(run, end)` | `run(...)` with a yielding loop, plus `.whenCanceled(end)` |
| `.until(condition)` | `coroutine.waitUntil(condition)` inside the body |
| `.withTimeout(seconds)` | `.withTimeout(time)` on the command, or `coroutine.wait(time)` / `waitUntil(condition, timeout)` |
| `.finallyDo(action)` | Code after the wait (normal finish) and/or `.whenCanceled(action)` (cancelled) |
| `Commands.waitSeconds(s)` | `coroutine.wait(Seconds.of(s))` or `Command.waitFor(time)` |
| `sequence(a, b)` / `a.andThen(b)` | `coroutine.await(a); coroutine.await(b);`, or `Command.sequence` / `andThen` |
| `parallel(a, b)` / `a.alongWith(b)` | `coroutine.awaitAll(a, b)`, or `Command.parallel` / `alongWith` |
| `race(a, b)` / `a.raceWith(b)` | `coroutine.awaitAny(a, b)`, or `Command.race` / `raceWith` |
| `deadline(a, b)` / `a.deadlineFor(b)` | `coroutine.fork(b); coroutine.await(a);` (forked children are cancelled when the parent ends) |
| `.asProxy()` | Not needed. Children awaited from a coroutine only own their mechanisms while they run. |
| `withInterruptBehavior(kCancelIncoming)` | `.withPriority(higher number)` |
| Default `kCancelSelf` behavior | Default priority (0) |
| Default command | `setDefaultCommand(...)` with `.withPriority(Command.LOWEST_PRIORITY)` |
| `.withName("X")` (optional) | `.named("X")` (**required**) |
| `onTrue`, `whileTrue`, `toggleOnTrue` | Same names. New: `retryWhileTrue`. |
| `CommandXboxController` | `CommandGamepad` |

## Before and after: the arm

### v2 (from Building an IO Subsystem)

```java
public class Arm extends SubsystemBase {
  // ... io, inputs, tunables, alert ...

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("Arm", inputs);
    disconnectedAlert.set(!inputs.connected);
    LoggedTunableNumber.ifChanged(hashCode(), g -> io.setGains(g[0], g[1], g[2], g[3]), kP, kD, kS, kG);
    // ... brake mode, outputs ...
  }

  public Command goTo(double positionRot) {
    return run(() -> setGoal(positionRot)).until(this::atGoal).withName("Arm GoTo");
  }

  public Command track(DoubleSupplier positionRot) {
    return run(() -> setGoal(positionRot.getAsDouble())).withName("Arm Track");
  }

  public Command manual(DoubleSupplier volts) {
    return runEnd(() -> io.setVoltage(volts.getAsDouble()), () -> io.setVoltage(0.0))
        .withName("Arm Manual");
  }
}
```

### v3

```java
public class Arm implements Mechanism {
  // ... io, inputs, tunables, alert: unchanged ...

  /** Reads inputs, logs, and handles tuning. Call from Robot.robotPeriodic() before the scheduler. */
  public void updateInputs() {
    io.updateInputs(inputs);
    Logger.processInputs("Arm", inputs);
    disconnectedAlert.set(!inputs.connected);
    LoggedTunableNumber.ifChanged(hashCode(), g -> io.setGains(g[0], g[1], g[2], g[3]), kP, kD, kS, kG);
    // ... brake mode, outputs: unchanged ...
  }

  /** Moves to a position and finishes when it arrives. The motor keeps holding afterwards. */
  public Command goTo(double positionRot) {
    return run(coroutine -> {
      setGoal(positionRot);
      coroutine.waitUntil(this::atGoal);
    }).named("Arm GoTo");
  }

  /** Follows a changing target and never finishes on its own. */
  public Command track(DoubleSupplier positionRot) {
    return runRepeatedly(() -> setGoal(positionRot.getAsDouble())).named("Arm Track");
  }

  /** Manual open-loop control; stops the motor when cancelled. */
  public Command manual(DoubleSupplier volts) {
    return run(coroutine -> {
          while (true) {
            io.setVoltage(volts.getAsDouble());
            coroutine.yield();
          }
        })
        .whenCanceled(() -> io.setVoltage(0.0))
        .named("Arm Manual");
  }
}
```

And in `Robot`:

```java
@Override
public void robotPeriodic() {
  arm.updateInputs();          // every mechanism, before the scheduler
  Scheduler.getDefault().run();
}
```

**What didn't change:** `ArmIO`, `ArmIOTalonFX`, and `ArmIOSim`. The IO layer has nothing to do with
the command framework, which is one more reason to keep hardware behind it.

## Migration checklist

- [ ] `extends SubsystemBase` → `implements Mechanism`; rename `periodic()` and call it from
      `robotPeriodic()`.
- [ ] Every command factory ends with `.named(...)`.
- [ ] Every `while` loop in a command body yields.
- [ ] Commands that need cleanup on cancel have `.whenCanceled(...)`.
- [ ] Default commands use `Command.LOWEST_PRIORITY`.
- [ ] `withInterruptBehavior` replaced by priorities.
- [ ] Controller classes switched to the v3 versions (`CommandGamepad`).
- [ ] Autos still work with the auto library's command type (see
      [Should we use v3 in 2027?](commands-v3.md#should-we-use-v3-in-2027)).
- [ ] Test every binding in simulation. Watch the command telemetry for unexpected cancels.
