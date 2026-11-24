# Tour of the 2026 Code

The 2026 robot code is our worked example. It contains good patterns we want to keep and older
ones we're moving away from. Use this page to know which is which before you copy anything.

:::{admonition} 2026 code in a 2027 world
:class: note
The 2026 code targets WPILib 2026 (roboRIO, Java 17, `edu.wpi.first` packages, Commands v2). It's
still the best example of the patterns we use, but it won't build with WPILib 2027 without the
importer and some fixes. Porting one subsystem to 2027 and Commands v3 is a good advanced exercise
(see [Migrating from v2 to v3](../training-materials/command-based/v2-to-v3.md)).
:::

:::{admonition} Older example code
:class: note
The 2025 robot's subsystems (REV hardware, YAGSL swerve, no IO layer) are kept in the Cluckipedia
repository under `docs/source/reference-materials/example-code/2025-subsystems/` for comparison.
They show patterns we've since moved away from; use the 2026 code and the training pages instead.
:::

## Map

| Path | What it is | Status |
|---|---|---|
| `Robot.java` | `LoggedRobot`, AdvantageKit setup, command logging, container selection | **Keep the pattern** |
| `Constants.java` | All constants, `BotType`, `Mode` (REAL/SIM/REPLAY), `kDebug` | **Keep**. Consider splitting per-subsystem constants into each subsystem's package. |
| `robotContainers/` | One container per robot (`CompetitionContainer`, `PrototypeContainer`, demo, test, vision) | **Keep the idea**: real/sim/replay IO choice lives here. |
| `subsystems/intake/PivotIO*`, `KrakenPivot` | Kraken pivot with full IO layer | **Model example** of the IO pattern |
| `subsystems/rollers/RollerIO*` | Reusable open-loop roller IO, used by intake and hopper | **Model example** (simplest IO) |
| `subsystems/shooter/` | Flywheels, hood, and turret with IO layers | Good IO structure. Hardware is REV (SparkFlex/SparkMax, ServoHub); see [REV Motors](../training-materials/rev/revlib.md). |
| `subsystems/intake/Pivot.java`, `SparkMaxRoller`, `MaxFeeder`, etc. | Pre-IO-layer REV subsystems | **Legacy.** Read to compare, don't copy. |
| `subsystems/Swerve.java` + `src/main/java/swervelib/` | YAGSL swerve (vendored source) with maple-sim inside | **Being replaced** by CTRE swerve + AdvantageKit template |
| `subsystems/Vision.java`, `utilities/vision/` | PhotonVision cameras, ambiguity and distance rejection, vision sim | **Keep the filtering ideas**; restructure into a `VisionIO` layer |
| `utilities/logging/LoggedTunableNumber.java` | Live-tunable numbers (from 6328) | **Keep** |
| `utilities/HardwareHelper.java` | Retrying config apply with alerts | **Keep** (the TalonFX/CANcoder parts) |
| `utilities/pathplanner/` | PathPlanner setup, replay-safe pathfinding (`LocalADStarAK`) | **Keep** |
| `utilities/AllianceFlipUtil.java`, `TCMath.java`, `InterpolationTable.java` | Math helpers | **Keep** |
| `utilities/FuelSim.java`, `TrenchFuelCollisions.java` | 2026 game-piece simulation on maple-sim | Game-specific; would need porting off maple-sim |
| `utilities/RobotVisualizer.java`, `ascope_assets/` | Articulated CAD robot in AdvantageScope | **Keep the technique** |
| `src/main/deploy/` | PathPlanner paths, AprilTag layouts, shot tables, Elastic layout | **Keep** |

## Known issues in the 2026 code

- Several commands use `runOnce(...).until(...)`, which finishes after one loop no matter what
  the condition is (see [Subsystems & Commands](../training-materials/command-based/subsystems-commands.md)).
- `Pivot.java` has `atHardStop()` but never uses it.
- `MVPLeft`, `SnowblowerLeft`, and `SnowblowerRight` autos use a `Climb` named command that is
  never registered (the climber is commented out), so they skip the climb. Found by the auto file
  test in [Unit Testing](../training-materials/testing/unit-testing.md#checking-the-auto-files).
- Some mechanisms mix duty-cycle and voltage control. Voltage control is more consistent, because
  the same command gives the same result as the battery voltage drops.
