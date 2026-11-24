# Composing Commands

Most robot actions are combinations of simpler commands. Combining them is called **composition**.

## Decorators

| Method | Result |
|---|---|
| `a.andThen(b)` | Run `a`, then `b` (a sequence) |
| `a.alongWith(b)` | Run both; finish when **both** finish |
| `a.raceWith(b)` | Run both; finish when **either** finishes, and interrupt the other |
| `a.deadlineFor(b)` | Run both; finish when **`a`** finishes, and interrupt `b` |
| `a.until(cond)` | End `a` early when `cond` is true |
| `a.withTimeout(2.0)` | End `a` after 2 seconds |
| `a.onlyIf(cond)` | Skip `a` entirely if `cond` is false at schedule time |
| `a.unless(cond)` | The opposite of `onlyIf` |
| `a.repeatedly()` | Restart `a` every time it finishes |
| `a.finallyDo(action)` | Always run `action` when `a` ends, even if interrupted |
| `a.withName("Intake Piece")` | A readable name in logs and dashboards |

## Example: intake a game piece

```java
public Command intakePiece() {
  return pivot.deploy()                                   // 1. lower the intake
      .andThen(
          intake.intake()                                 // 2. spin rollers...
              .alongWith(feeder.feed())
              .until(feeder::hasPiece)                    //    ...until we have a piece
              .withTimeout(3.0))                          //    (but give up after 3 s)
      .andThen(pivot.stow())                              // 3. raise the intake
      .withName("IntakePiece");
}
```

## Example: shoot when ready

```java
public Command shoot() {
  return Commands.waitUntil(() -> shooter.atSpeed() && turret.onTarget())
      .andThen(feeder.feed().withTimeout(0.5))
      .deadlineFor(shooter.spinUp(), turret.aim())        // keep spinning and aiming the whole time
      .withName("Shoot");
}
```

## Rules for composition

- A command object can only be in **one** composition. Make a fresh one each time by calling the
  factory method again. That's one of the reasons we use factory methods.
- A composition **requires every subsystem** its parts require. If any part gets interrupted, the
  whole group is interrupted.
- Commands that coordinate several subsystems live in the container or in `commands/`, not inside one
  subsystem.

## Seeing commands run

The 2026 `Robot.java` logs every running command to AdvantageKit (`Commands/All/<name>`). Open
AdvantageScope and watch which commands start and stop. This is the fastest way to answer "why
didn't my command run?"

## 2027: composition in Commands v3

In [Commands v3](commands-v3.md#composition), a routine like `intakePiece()` is written as one
function that `await`s each step in order, and `awaitAll` / `awaitAny` replace `alongWith` /
`raceWith`. The decorators on this page have direct equivalents. See the table in
[Migrating from v2 to v3](v2-to-v3.md).
