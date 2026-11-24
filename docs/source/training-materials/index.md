(training-materials)=
# Training Materials

These pages take you from "I have never written Java" to "I can write, simulate, tune, and debug a
robot subsystem by myself." They teach the way the ThunderChickens write robot code, using our
2026 robot code as a worked example.

:::{admonition} How to use these pages
:class: note
New programmers: read the sections in the order below, since later pages build on earlier ones.
Everyone else: jump straight to the topic you need.
:::

## Learning path

| Step | Section | What you learn |
|---|---|---|
| 0 | [Setup](setup/dev-environment.md) | Install the tools, build and simulate a project |
| 1 | [Java](java/basics.md) | Variables, classes, interfaces, lambdas |
| 2 | [Git](git/basics.md) | Commits, branches, pull requests, team rules |
| 3 | [FRC Fundamentals](frc-basics/robot-program.md) | The robot loop, hardware, units and geometry |
| 4 | [Command-Based Programming](command-based/subsystems-commands.md) | Subsystems, commands, triggers, Commands v3 |
| 4½ | [XRP Lessons](xrp-training/xrp-resources.md) | Hands-on practice on a small robot: drive, voltage control, PID |
| 5 | [Motors & Motor Controllers](ctre/phoenix6.md) | Phoenix 6, Tuner X, REV |
| 5 | [Sensors](sensors/overview.md) | Encoders, gyros, limit switches, distance and color sensors |
| 6 | [AdvantageKit](advantagekit/why-io.md) | The IO layer, logging, replay |
| 7 | [Control](control/pid.md) | PID, feedforward, Motion Magic, tuning |
| 8 | [Simulation](simulation/strategy.md) | Physics sims for every mechanism |
| 9 | [Unit Testing](testing/unit-testing.md) | Test subsystems, teleop bindings, and autos on a laptop |
| 10 | [Swerve & Vision](swerve/ctre-swerve.md) | CTRE swerve, odometry, PhotonVision |
| 11 | [Autonomous](autonomous/pathplanner.md) | PathPlanner paths and autos |
| — | [What's New for 2027](season-2027/whats-new.md) | SystemCore, Java 25, Commands v3. Read it after step 4. |
| Advanced | [Object Detection](object-detection/overview.md) | Train a model to find game pieces and drive to them |

The [Functional Specifications](../functional-specifications/index.rst) say **what** each kind of
subsystem must do. These pages teach **how** to build it.

## The tech stack we teach

| Area | Tool | Notes |
|---|---|---|
| Language | Java | Java 17 in 2026, **Java 25** in 2027. |
| Robot framework | WPILib, command-based | Commands v2 (2026 code) and **Commands v3** (new for 2027). |
| Motors & sensors | **CTRE Phoenix 6** first; REV when needed | Kraken X60/X44 (TalonFX), CANcoder, Pigeon 2, CANrange, CANdi. REV SPARK/NEO where a mechanism already uses them. |
| Swerve | **CTRE swerve** (Tuner X generator) | We are **not** using YAGSL going forward. |
| Logging | **AdvantageKit** + AdvantageScope | IO-layer pattern, log replay. |
| Simulation | WPILib physics sims + Phoenix 6 sim | See [Simulation Strategy](simulation/strategy.md). |
| Autonomous | PathPlanner | Paths and autos built in a GUI. |
| Vision | PhotonVision | AprilTag pose estimation and object detection. |
| Dashboard | Elastic, AdvantageScope | |

:::{admonition} About the 2026 code
:class: note
The 2026 robot code is used throughout as a **worked example**, not as a template to copy
blindly. It contains REV hardware, a vendored copy of YAGSL, and some older patterns we are
moving away from. The [Tour of the 2026 Code](../reference-materials/code-tour-2026.md) labels
which parts show the patterns we want and which parts are legacy.
:::

:::{admonition} Heading into 2027
:class: important
2027 brings a new robot controller (SystemCore), Java 25, renamed WPILib packages, and Commands v3.
Read [What's New for 2027](season-2027/whats-new.md) once you've finished Command-Based
Programming. Pages throughout have **2027** notes where something changes.
:::

## No internet? No problem

Every link to an outside site has an **(offline)** link next to it that goes to a local summary in
the [Offline Reference](../reference-materials/offline/index.md). That section also explains how to
get this wiki and the robot code tools onto a laptop without internet.

## Find your specialty

Once you've finished the core path, pick an area to go deeper in. There's always more to do than
people to do it, so follow what you enjoy:

| Area | What it involves | Start here |
|---|---|---|
| **Mechanisms** | Arms, elevators, shooters, intakes: the subsystems that score | [Functional Specifications](../functional-specifications/index.rst), [Control](control/pid.md) |
| **Swerve & autonomous** | Driving, odometry, paths, auto routines | [Swerve & Vision](swerve/ctre-swerve.md), [Autonomous](autonomous/pathplanner.md) |
| **Vision** | Cameras, AprilTags, object detection | [Odometry & Vision](swerve/vision.md), [Vision Resources](../reference-materials/vision-resources.md), [Object Detection](object-detection/overview.md) |
| **LEDs & driver feedback** | Status lights the drive team can read at a glance | [CANdle LED Plan](../candle-template.md) |
| **Tools & infrastructure** | Simulation, logging, dashboards, scouting | [Simulation](simulation/strategy.md), [Chicken Scratch](../chicken-scratch/index.md) |

## A few things every new programmer should know

- **Coding is hard, and that's normal.** Everyone gets stuck. Ask the team: explaining your problem
  out loud (even to a rubber duck) often solves it.
- **Get to know your team.** You'll be working with these people all season. The best way to stay
  motivated is to make friends with them.
- **Write it well the first time.** Clear names, comments that explain *why*, and units on every
  value save someone else from rewriting your code later. See the
  [Code Standards](../reference-materials/code-standards.md).

## Other tutorials

If an explanation here doesn't click, try another one:

- [WPILib: Zero to Robot](https://docs.wpilib.org/en/stable/docs/zero-to-robot/introduction.html)
- [FRC Team 3255 Java tutorial](https://frcteam3255.github.io/FRC-Java-Tutorial/index.html)
- [FIRST: Programming the robot (PDF)](https://www.firstinspires.org/sites/default/files/uploads/resource_library/frc/team-resources/tca/tca-programming.pdf)

These pages build on the 2025 Java Tutorial series written by Jacob Mitchel, whose installation
guide and advice are folded in throughout.
