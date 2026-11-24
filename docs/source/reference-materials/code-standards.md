# Code Standards

Reviewers check every PR against these.

## Formatting

- Run `./gradlew spotlessApply` before every commit. We use google-java-format (2-space indent,
  100-column lines). Don't hand-format.

## Naming

| Thing | Style | Example |
|---|---|---|
| Classes | `PascalCase` | `ArmIOTalonFX` |
| Methods, variables | `camelCase` with units | `positionRot`, `setGoal()` |
| Constants | `k` prefix | `kStowRot`, `kMotorId` |
| Command factories | Verb phrase, lowercase first letter | `intake()`, `goTo()`, `stow()` |
| Log keys | `Subsystem/Thing` | `Arm/GoalRot` |
| IO implementations | `<Name>IO<Hardware>` (the spec's *SubsystemIOHardware*) | `ArmIOTalonFX`, `ArmIOSim` |

(Some 2026 command methods start with a capital letter, such as `Deployed()`. Use lowercase for new code.)

## Constants and units

These follow the [Subsystem Functional Specification](../functional-specifications/subsystem-func-spec.md#3-common-system-architecture):

- Each subsystem has its own **constants class**, passed into the subsystem through its
  constructor and filled in for each robot in `Constants.java`. That lets the same code run on the
  competition and practice robots, or run two copies of a mechanism.
- Use **real mechanism units**, not motor rotations: meters for distance, and degrees, rotations, or
  radians for angles. Put the unit in the name (`heightMeters`, `positionRot`), or use the WPILib
  Java units classes (`Distance`, `Angle`).
- Setpoints go in an `enum` or constants, never as loose numbers in a command.

## Structure

- One package per mechanism: `subsystems/arm/` holds `Arm`, `ArmIO`, `ArmIOTalonFX`, `ArmIOSim`.
- **All hardware access goes through IO classes.** Subsystems never import Phoenix classes
  (except units/enums).
- Every subsystem has a sim IO.
- Multi-subsystem commands go in the container or `commands/`.
- No `static` state in subsystems.

## Control & hardware

- Every motor has a current limit and a defined neutral mode.
- Use voltage-based control requests (`VoltageOut`, `VelocityVoltage`, `MotionMagicVoltage`),
  not duty cycle.
- Configure devices fully in code and apply with a retry helper.
- Soft limits on every mechanism with a limited range of motion.

## Testing

See [Unit Testing](../training-materials/testing/unit-testing.md).

- Pure logic (math, conversions, state machines, target selection) gets unit tests.
- Every subsystem has at least a fake-IO test of its commands: right setpoint, finishes when it should,
  stops on end.
- Button bindings live in a method that tests can call.
- A test checks that every named command used by an auto is registered, and that every path loads.
- `./gradlew build` (which runs the tests) passes before a PR is opened.

## Logging

- Every sensor reading in inputs; every goal, state, and decision as an output.
- `connected` in every IO's inputs, with an `Alert` when false.

## Documentation

- Javadoc on every public class and method. Say the **units**.
- Comments explain **why**, not what. `// 20.5 rot is past the bumper, safe to run rollers` beats
  `// check position`.

## Commands v3 (2027)

If the team uses Commands v3:

- Every command is named with `.named(...)`. Use the same "Mechanism Action" style: `"Arm GoTo"`.
- Every `while` loop in a command body yields.
- Cleanup that must happen on cancel goes in `.whenCanceled(...)`.
- Default commands use `Command.LOWEST_PRIORITY`. Any other non-zero priority needs a comment
  explaining why.
- Each mechanism's `updateInputs()` is called from `Robot.robotPeriodic()` before the scheduler runs.

## Never

- `Thread.sleep`, blocking `while` loops, or file/network I/O in the main loop.
- Committing to `main` directly. See the [Team Git Workflow](../training-materials/git/team-workflow.md).
- Magic numbers in logic.
- Comparing `double`s with `==`.
