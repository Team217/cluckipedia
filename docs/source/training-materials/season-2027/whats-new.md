# What's New for 2027

2027 is the biggest change to FRC software in years: a **new robot controller (SystemCore)**, a
**new Driver Station**, **Java 25**, **renamed packages**, and a **new command framework
(Commands v3)**. This page summarizes what changes for us and links to the wiki pages that cover
each part.

:::{admonition} Status: alpha software (as of November 2026)
:class: warning
Everything here comes from the **2027 alpha** releases (WPILib `2027.0.0-alpha-8` and matching
vendor alphas). Names and APIs have changed between alphas and will change again before kickoff
(January 9, 2027). Each item is labeled with how settled it is. After kickoff, check every item
against the official docs and update this page with a PR.

Official sources: [WPILib "New for 2027"](https://docs.wpilib.org/en/latest/docs/yearly-overview/yearly-changelog.html),
[WPILib 2027 docs](https://docs.wpilib.org/en/latest/),
[SystemCore testing repository](https://github.com/wpilibsuite/SystemcoreTesting).
:::

## At a glance: 2026 → 2027

| Area | 2026 (what the 2026 code uses) | 2027 |
|---|---|---|
| Robot controller | roboRIO | **SystemCore** (plus optional **MotionCore** expansion) |
| Driver Station | NI Driver Station (Windows only) | **2027 FIRST Driver Station** (Windows, macOS, Linux) |
| Java version | Java 17 | **Java 25** |
| WPILib packages | `edu.wpi.first...` | **`org.wpilib...`** |
| Constants and enum values | Mixed (`kBlue`, `Blue`) | **ALL_CAPS** |
| Robot startup | `robotInit()` or constructor | **Constructor only**; `robotInit()` is removed |
| "Test" mode | Test | Renamed **Utility** |
| Op modes | Auto / Teleop | Auto / Teleop **plus selectable OpModes** (FTC-style) |
| Command framework | Commands v2 | **Commands v3** (new, Java only). v2 is still available. |
| Dashboards | SmartDashboard, Shuffleboard, `SendableChooser` | **Removed.** Replaced by the **Telemetry** and **Tunables** APIs and `Selectable` |
| Controllers | `XboxController`, `PS4Controller`, `CommandXboxController` | Unified **`Gamepad`** classes (names still changing between alphas) |
| CAN in WPILib classes | Integer CAN IDs and bus | **`CANPort` enum** for SystemCore vs. MotionCore buses |
| Units library | Mutable and immutable units | **Immutable only** |
| AdvantageKit | v26 | **v27** alphas |

## SystemCore: the new robot controller

*Settled enough to plan around; connector details are still changing.*

SystemCore replaces the roboRIO. It's built by Limelight around a Raspberry Pi Compute Module 5.

| Feature | What it means for programmers |
|---|---|
| **5 CAN FD buses** (`can_s0` to `can_s4`) | Split devices across buses (for example drivetrain on one, mechanisms on another). Fewer reasons to need a CANivore, though CTRE devices still work with one. |
| **MotionCore** expansion | Adds more CAN buses (`can_d0`...) and I/O. WPILib's `CANPort` enum picks SystemCore vs. MotionCore buses. |
| **SmartIO ports** (6) | Each port can be configured as digital in/out, PWM, analog in, and more. There are no relay, analog output, or SPI ports, and no MXP. |
| **Built-in IMU** | A gyro on the controller itself, with simulation support. The Pigeon 2 is still the better choice for swerve heading. |
| **Web interface** at `http://robot.local` | Set the team number, update the OS image, install packages (AdvantageScope Lite, Elastic), and see status. Replaces the roboRIO imaging tool and team number setter. |
| **Storage** | 16 GB internal. AdvantageKit logs go to a USB drive (`/U`) by default, or to `/home/systemcore/logs` internally. |
| **Wi-Fi access point** (for the bench and pit) | Connect to it to program without the radio. At events, use the VH-109 radio as before. |
| **OLED screen** | Shows IP addresses and faults, which helps with "why can't I connect?" |
| **I2C pinout** | Swapped compared to the roboRIO. Old I2C cables need SCL/SDA swapped. |

Teams in the alpha program have hardware now. Price, sale date, and whether the roboRIO remains
legal for 2027 hadn't been announced as of November 2026.

More: [Robot Hardware](../frc-basics/hardware.md#systemcore-2027).

## The 2027 Driver Station

*Settled.* A rewritten FIRST Driver Station that runs on **Windows, macOS, and Linux**.

- "Practice" mode is renamed **Match Mode**.
- An **A-Stop** (autonomous stop) button sits next to E-Stop.
- **OpMode selection**: pick which op mode runs, like FTC.
- The NI Driver Station can still drive a SystemCore for basic use, but new features need the 2027 DS.

## Java and WPILib library changes

### Java 25 and the package rename

*Settled.*

- WPILib 2027 uses **Java 25**. It's bundled with the WPILib installer, so nothing to install separately.
- WPILib packages moved from `edu.wpi.first` to **`org.wpilib`**. For example,
  `edu.wpi.first.wpilibj2.command.Command` becomes a class under `org.wpilib`.
- Constants and enum values are **ALL_CAPS**.
- **Importing a 2026 project:** the 2027 WPILib VS Code includes an importer that converts a 2026
  project, including the package renames. Run it on a copy and review the diff in Git.

:::{note}
Code samples elsewhere in this wiki use the **2026** package names (`edu.wpi.first...`), because
they match the 2026 robot code you'll read. In 2027 code, let VS Code fix the imports
({kbd}`Ctrl+.`) and use autocomplete to find the new names.
:::

### Removed or changed APIs

| Change | Status | What to do |
|---|---|---|
| `robotInit()` removed | Settled | Put startup code in the `Robot` constructor (the 2026 code already does). |
| SmartDashboard, Shuffleboard, `SendableChooser`, `Sendable` removed | Settled | Use the **Telemetry** API for outputs, **Tunables** for live-tunable values, and **`Selectable`** for choosers. In AdvantageKit code, keep using `Logger.recordOutput` and AdvantageKit's choosers. |
| Motor controller `set()` → `setThrottle()`, `stopMotor()` → `disable()` | Settled | Affects WPILib's own motor controller classes. We use Phoenix 6 control requests, which don't change because of this. |
| `MotorControllerGroup` removed | Settled | Use a `Follower` request on the TalonFX. |
| Gamepad classes reorganized | **Changing** | Alpha 5 merged them into one `Gamepad`; alpha 7 brought back controller-specific classes and renamed face buttons to directions (`faceUp`/`faceDown`/`faceLeft`/`faceRight`). Use autocomplete. |
| CAN device classes take a `CANPort` enum | Settled for WPILib classes | Vendor libraries (Phoenix 6) use their own CAN bus classes. Check the vendor docs. |
| `Rotation2d` getters return **wrapped** angles | Settled | If you need an unwrapped angle (for example a turret past ±180°), store it as a `double` or `Angle` instead. |
| Mutable units removed | Settled | Use the immutable units (`Meters.of(1.0)`). |
| NetworkTables 3 removed | Settled | Only matters for very old dashboards. |
| Removed hardware classes | Settled | Relays, analog output, SPI, analog gyros, counters, interrupts, `Servo`, and old IMU classes are gone. |
| Shuffleboard, PathWeaver, RobotBuilder removed | Settled | Use Elastic, PathPlanner/Choreo, and this wiki. |

### New APIs worth knowing

- **OpModes** (`OpModeRobot`, `OpMode`, `PeriodicOpMode`): an alternative robot structure where each
  selectable mode is its own class. Optional. `TimedRobot` (and AdvantageKit's `LoggedRobot`) still
  work. AdvantageKit adds an optional `LoggedOpModeRobot`.
- **Telemetry** and **Tunables**: WPILib's built-in replacements for SmartDashboard-style publishing
  and live-tuned numbers.
- **Commands v3**: see below.

## Command-based programming

*Commands v3 is new and still changing between alphas. Commands v2 is stable.*

| | Commands v2 | Commands v3 |
|---|---|---|
| Status for 2027 | Still included and supported | New. Java only. |
| Subsystem type | `SubsystemBase` with `periodic()` | `Mechanism` interface, no `periodic()` |
| Command logic | `initialize` / `execute` / `isFinished` / `end`, or decorators | **One function** written top to bottom using a **coroutine** (`yield`, `wait`, `waitUntil`, `await`) |
| Conflicts | New command interrupts old (configurable) | **Priorities**: equal or higher priority interrupts, lower is rejected |
| Names | Optional | **Required** (`.named("...")`) |

2027 changes to **v2**: `Command.schedule()` is deprecated (schedule through the scheduler or a
trigger), `Subsystem.idle()` was added, and `RamseteCommand` and the swerve/mecanum controller
commands were removed.

Learn v3 here: [Commands v3](../command-based/commands-v3.md), then
[Migrating from v2 to v3](../command-based/v2-to-v3.md).

## Our libraries in 2027

*Changing quickly. This is the compatibility list from the SystemCore testing repository for
WPILib alpha 7/8, as of November 2026.*

| Library | 2027 status | Notes |
|---|---|---|
| CTRE Phoenix 6 | `v26.70.0-alpha-2` | Works on SystemCore's CAN buses. Tuner X and the swerve generator still apply. |
| AdvantageKit | `v27.0.0-alpha-5` to `alpha-7` | `LoggedDashboardChooser` → **`LoggedNetworkChooser`**. Mutable units support removed. Only the **skeleton** template exists for 2027 so far; the TalonFX swerve template hadn't been updated yet. |
| REVLib | `v2027.0.0-alpha-7` | For SPARK MAX/Flex where we still use REV. See [REV Motors](../rev/revlib.md). |
| ReduxLib | `v2027.0.0-alpha-7p1` | Redux sensors (Canandmag, Canandgyro, Canandcolor) |
| PathPlannerLib | `v2027.0.0-alpha-4.1` | Check whether its commands are v2 or v3 before choosing a command framework. |
| PhotonLib | Development builds only | Expect a release closer to kickoff. |
| AdvantageScope, Elastic | 2027 alphas | AdvantageScope Lite and Elastic can also run **on** SystemCore, installed from its web interface. |
| maple-sim | No 2027 release found | See [Simulation Strategy](../simulation/strategy.md). |

## What this means for training

- **Everything conceptual in this wiki still applies**: Java, Git, the 20 ms loop, the IO layer,
  Phoenix 6, PID and feedforward, simulation, swerve, and vision.
- **Learn Commands v3** after you're comfortable with v2 concepts. v2 is still how the 2026 code and
  most other teams' code work, and the ideas (requirements, triggers, composition) carry over.
- **Expect import and name changes.** When a sample doesn't compile in a 2027 project, the fix is
  almost always a renamed package, an ALL_CAPS enum value, or a renamed method. Use autocomplete
  and the importer.
- **Mentors decide** the team's 2027 command framework after kickoff, once vendor libraries have
  stable 2027 releases. See the recommendation in [Commands v3](../command-based/commands-v3.md#should-we-use-v3-in-2027).
