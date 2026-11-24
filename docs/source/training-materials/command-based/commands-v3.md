# Commands v3 (2027)

**Commands v3** is WPILib's new command framework for Java, introduced for 2027. It keeps the ideas
you learned in [Subsystems & Commands](subsystems-commands.md) (commands, requirements, triggers,
composition), but changes **how you write a command**: instead of splitting it across
`initialize`/`execute`/`isFinished`/`end`, you write **one function from top to bottom**, and it
pauses and resumes across loop cycles.

:::{admonition} Alpha API
:class: warning
Commands v3 is part of the 2027 WPILib **alpha**. Method names and imports on this page match the
WPILib docs as of November 2026 and may change before kickoff. If something doesn't compile, check
autocomplete and the [Commands v3 docs](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/index.html).
In the docs' examples the classes are imported from `org.wpilib.command3` (for example
`org.wpilib.command3.Command`, `org.wpilib.command3.Trigger`, `org.wpilib.command3.button.CommandGamepad`).
:::

## The big idea: coroutines

A **coroutine** is a function that can **pause** in the middle and **resume** later from the same
spot. In v3, every command body is a coroutine:

```java
public Command deploy() {
  return run(coroutine -> {
    io.setPosition(DEPLOYED_ROT);            // runs once, the first loop
    coroutine.waitUntil(this::atGoal);       // pauses here, loop after loop, until true
    Logger.recordOutput("Pivot/Deployed", true);  // runs once when it arrives
  }).named("Pivot Deploy");
}
```

Read it top to bottom: set the goal, wait until we arrive, log it. When the function reaches the end,
the command is **finished**.

Compare with v2, where the same logic is split up:

```java
// v2
return run(() -> io.setPosition(DEPLOYED_ROT))
    .until(this::atGoal)
    .finallyDo(() -> Logger.recordOutput("Pivot/Deployed", true))
    .withName("Pivot Deploy");
```

### Cooperative: you must yield

Coroutines are **cooperative**. While your command's code runs, nothing else in the robot runs.
Your command gives control back to the scheduler only when it calls a **yielding** method. That's
how the scheduler fits every command into each 20 ms loop.

| Coroutine method | What it does |
|---|---|
| `coroutine.yield()` | Pause until the next loop (one cycle) |
| `coroutine.wait(time)` | Pause for a duration, for example `Seconds.of(0.5)` |
| `coroutine.waitUntil(condition)` | Pause until a `BooleanSupplier` is true |
| `coroutine.waitUntil(condition, timeout)` | Same, with a timeout. The result tells you whether it timed out (`result.timedOut()`). |
| `coroutine.await(command)` | Start another command and pause until it finishes |
| `coroutine.awaitAll(a, b, ...)` | Start several and wait for **all** to finish (parallel) |
| `coroutine.awaitAny(a, b, ...)` | Start several and wait for the **first** to finish; the rest are cancelled (race) |
| `coroutine.fork(command)` | Start a command in the background and keep going immediately |
| `coroutine.park()` | Pause forever (until cancelled). Used by commands that only supervise children. |

:::{danger}
**Every loop inside a command must yield.** A `while` loop without `yield()` (or another yielding
call) freezes the entire robot, just like a blocking `while` loop in v2. WPILib's compiler plugin
reports a **compile error** for a `while` loop in a command body that doesn't yield.
:::

```java
// Follow a changing target until cancelled
return run(coroutine -> {
  while (true) {
    io.setPosition(target.getAsDouble());
    coroutine.yield();                  // REQUIRED: let the rest of the robot run
  }
}).named("Arm Track");
```

## Mechanisms (replace subsystems)

A v3 **`Mechanism`** is the equivalent of a v2 subsystem: it owns hardware, is a requirement, and
provides command factories.

```java
public class Pivot implements Mechanism {
  private final PivotIO io;
  private final PivotIOInputsAutoLogged inputs = new PivotIOInputsAutoLogged();

  public Pivot(PivotIO io) {
    this.io = io;
  }

  /** Read sensors and log. Called from Robot.robotPeriodic() every loop. */
  public void updateInputs() {
    io.updateInputs(inputs);
    Logger.processInputs("Pivot", inputs);
  }
  // ... command factories ...
}
```

**There's no `periodic()`.** In v3, mechanisms don't get a method called automatically every loop.
Call the update method yourself from `Robot.robotPeriodic()`, **before** running the scheduler, so
commands see fresh sensor data:

```java
@Override
public void robotPeriodic() {
  pivot.updateInputs();
  intake.updateInputs();
  drive.updateInputs();
  Scheduler.getDefault().run();   // the v3 scheduler (alpha name; check autocomplete)
}
```

This actually fits the AdvantageKit IO pattern well: reading inputs at the start of the loop is
exactly what `periodic()` did in our v2 subsystems.

### Command factories on a mechanism

| Factory | Use |
|---|---|
| `run(coroutine -> { ... })` | The general form: any staged logic. Requires this mechanism. |
| `runRepeatedly(() -> ...)` | Run a simple action every loop forever (like v2's `run`) |
| `idle()` | Do nothing, but hold the mechanism |

Every factory ends with **`.named("...")`**. A command must have a name, and the builder won't give
you a `Command` until you name it. Options like `.withPriority(...)` and `.whenCanceled(...)` go
before `.named(...)`.

For commands that don't own a mechanism (for example a routine that only coordinates other commands),
use **`Command.noRequirements(coroutine -> { ... }).named("...")`**.

### One-shot commands

A body that never yields finishes in the same loop it starts, which is the v3 version of `runOnce`:

```java
public Command zero() {
  return run(coroutine -> io.resetPosition(0.0)).named("Pivot Zero");
}
```

### Cleanup when cancelled

If a command can be cancelled (button released, interrupted by a higher-priority command), put cleanup
in `whenCanceled`. Cleanup at the **end** of the body only runs if the body finishes normally.

```java
public Command manual(DoubleSupplier volts) {
  return run(coroutine -> {
        while (true) {
          io.setVoltage(volts.getAsDouble());
          coroutine.yield();
        }
      })
      .whenCanceled(() -> io.setVoltage(0.0))
      .named("Pivot Manual");
}
```

Keep `whenCanceled` short. It runs once and must not loop.

## Default commands

```java
drive.setDefaultCommand(
    drive.runRepeatedly(() -> drive.driveFieldRelative(speedsFromJoysticks()))
        .withPriority(Command.LOWEST_PRIORITY)
        .named("Joystick Drive"));
```

- Runs whenever nothing else owns the mechanism.
- Must require only its own mechanism.
- Give it **`Command.LOWEST_PRIORITY`**. Otherwise it may block driver commands from taking over
  (see below).
- If you don't set one, a mechanism sits idle.

## Priorities (replace interrupt behavior)

Every command has an integer **priority** (default 0). When a new command needs a mechanism that's
in use:

- New priority **≥** running priority: the running command is **interrupted**, and the new one starts.
- New priority **<** running priority: the new command is **rejected** and never starts.

```java
public Command climb() {
  return run(coroutine -> { /* ... */ })
      .withPriority(10)      // nothing at normal priority can interrupt the climb
      .named("Climb");
}
```

This replaces v2's `withInterruptBehavior(kCancelIncoming)`.

## Composition

### Inside a coroutine (recommended for routines)

```java
public Command intakePiece() {
  return Command.noRequirements(coroutine -> {
    coroutine.await(pivot.deploy());                                     // 1. lower the intake
    coroutine.await(intake.intakeUntilPiece().withTimeout(Seconds.of(3))); // 2. intake (3 s max)
    coroutine.await(pivot.stow());                                       // 3. raise it
  }).named("Intake Piece");
}

public Command shoot() {
  return Command.noRequirements(coroutine -> {
    coroutine.awaitAll(shooter.spinUp(), turret.aim());   // both in parallel, wait for both
    coroutine.await(feeder.feed().withTimeout(Seconds.of(0.5)));
  }).named("Shoot");
}
```

Child commands own their mechanisms **only while they run**, so a long routine doesn't lock every
mechanism for its whole duration. A child can also use a mechanism its parent requires without
interrupting the parent.

### Built-in groups

For simple cases there are still group builders: `Command.sequence(...)` / `a.andThen(b)`,
`Command.parallel(...)` / `a.alongWith(b)`, `Command.race(...)` / `a.raceWith(b)`, and
`Command.waitFor(time)`. Built-in groups hold **all** their children's mechanisms for the whole
group. Names are generated automatically (`A -> B` for sequences, `A | B` for parallel).

## Triggers and bindings

Bindings work like v2:

```java
CommandGamepad operator = new CommandGamepad(1);
operator.a().onTrue(arm.score());         // face-button names have changed between alphas;
operator.rightBumper().whileTrue(intake.intake());  // use autocomplete
new Trigger(feeder::hasPiece).onTrue(leds.flashGreen());
```

| Binding | Behavior |
|---|---|
| `onTrue` / `onFalse` | Start on the edge; run to completion |
| `whileTrue` / `whileFalse` | Run while the condition holds; cancel when it changes |
| `toggleOnTrue` / `toggleOnFalse` | Each edge starts or cancels |
| **`retryWhileTrue` / `retryWhileFalse`** | **New:** like `whileTrue`, but restarts the command if it finishes while the condition is still true |

Trigger helpers: `and`, `or`, `negate`, `debounce(time)`, `risingEdge()`, `fallingEdge()`, and
`multiPress(count, time)` (for example a double-tap).

## Scopes

Bindings and default commands belong to the **scope** where you create them:

- **Global**: created in the `Robot` constructor. Active all the time.
- **OpMode**: created inside an OpMode (if the team uses OpModes). Removed when that mode ends.
- **Command**: created inside a running command. Removed when the command ends.

When a scope ends, its bindings are removed, default-command overrides revert, and the commands it
scheduled are cancelled. This makes it easy to have, for example, different button mappings in
autonomous and teleop without manual cleanup.

## Logging commands

The v3 scheduler publishes structured telemetry about every queued and running command: name, ID,
parent, priority, requirements, and timing. You can see the full parent/child tree of a routine. Log
it with the team's logging setup so it's in every match log, like the `Commands/All/...` outputs in
the 2026 code.

## Should we use v3 in 2027?

This is a mentor decision after kickoff. The things to check then:

- [ ] Is Commands v3 out of alpha (a stable 2027 release)?
- [ ] Do **PathPlannerLib**, **AdvantageKit**, and our other libraries support v3 commands? For example, does PathPlanner's auto builder produce v2 or v3 commands?
- [ ] Can v2 and v3 run in the same project, if we need a library that only supports one?
- [ ] Are the students who'll write most of the code comfortable with coroutines?

Either way, **learn both**: v2 because most existing FRC code (including ours from 2026) uses it,
and v3 because it's where WPILib is going.
