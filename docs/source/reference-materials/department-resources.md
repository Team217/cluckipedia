# Department Resources

Links to the tools, documentation, and community projects the programming department uses. Where a
link has an **(offline)** link next to it, the key information is summarized in this wiki.

:::{admonition} Update every season
:class: note
After kickoff, update this page with the season's library versions and check that the wiki's
code samples still compile.
:::

## Development tools

Every student doing robot code needs these. Install steps are in
[Development Environment](../training-materials/setup/dev-environment.md).

- [WPILib VS Code](https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/wpilib-setup.html) ([offline](offline/tools-install.md#wpilib)):
  the editor we write robot code in, plus the JDK, simulation, SysId, and AdvantageScope.
- [NI FRC Game Tools](https://www.ni.com/en/support/downloads/drivers/download.frc-game-tools.html) ([offline](offline/tools-install.md#ni-frc-game-tools-windows-only)):
  the Driver Station and roboRIO imaging tool. **2027:** SystemCore uses the new 2027 FIRST Driver
  Station instead; see [What's New for 2027](../training-materials/season-2027/whats-new.md#the-2027-driver-station).

## Vendor hardware tools

These are less critical for personal laptops. The programming department laptops have all of them.

- [CTRE Phoenix Tuner X](https://v6.docs.ctr-electronics.com/en/stable/docs/tuner/index.html) ([offline](offline/ctre-reference.md#phoenix-tuner-x)):
  configure, update, and test CTRE devices. See [Phoenix Tuner X](../training-materials/ctre/tuner-x.md).
- [REV Hardware Client](https://docs.revrobotics.com/rev-hardware-client): configure and update REV
  devices. See [REV Motors](../training-materials/rev/revlib.md).
- [Redux Alchemist](https://docs.reduxrobotics.com/alchemist/installation): configure Redux sensors.
- [Swyft Servo Programmer](https://swyftrobotics.com/products/swyft-servos): program Swyft servos.
- [Studica navX2-UI](https://pdocs.kauailabs.com/navx-micro/software/navx-ui/): configure a navX gyro.

## Official documentation

- [WPILib documentation](https://docs.wpilib.org/en/stable/index.html) ([offline](offline/index.md)):
  if you don't know something robot-code related, start here.
- [WPILib Java API reference](https://github.wpilib.org/allwpilib/docs/release/java/index.html):
  if you don't know a method or class, look here. (In VS Code, {kbd}`F12` on a class name opens it.)
- [WPILib repository](https://github.com/wpilibsuite/allwpilib): WPILib's source code.
- [CTRE Phoenix 6 documentation](https://v6.docs.ctr-electronics.com/en/stable/) ([offline](offline/ctre-reference.md))
- [CTRE Phoenix 6 examples](https://github.com/CrossTheRoadElec/Phoenix6-Examples) ([offline](offline/ctre-reference.md#whats-in-the-phoenix-6-examples))
- [AdvantageKit documentation](https://docs.advantagekit.org/) ([offline](offline/advantagekit-reference.md))
- [AdvantageScope documentation](https://docs.advantagescope.org/) ([offline](offline/advantagekit-reference.md#advantagescope))
- [PathPlanner documentation](https://pathplanner.dev/home.html) ([offline](offline/pathplanner-reference.md))
- [PhotonVision documentation](https://docs.photonvision.org/) ([offline](offline/photonvision-reference.md))

## 2027 season

- [What's New for 2027](../training-materials/season-2027/whats-new.md): our summary
- [WPILib "New for 2027"](https://docs.wpilib.org/en/latest/docs/yearly-overview/yearly-changelog.html)
- [WPILib 2027 docs (latest)](https://docs.wpilib.org/en/latest/)
- [Commands v3 docs](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/index.html)
  and [migration guide](https://docs.wpilib.org/en/latest/docs/software/commandbased/commands-v3/migration-guide.html)
- [SystemCore testing repository](https://github.com/wpilibsuite/SystemcoreTesting): hardware notes,
  known issues, and the vendor library compatibility table
- [AdvantageKit releases](https://github.com/Mechanical-Advantage/AdvantageKit/releases): v27 alpha notes

## Templates to start from

- [AdvantageKit TalonFX(S) swerve template](https://docs.advantagekit.org/getting-started/template-projects/talonfx-swerve-template/) ([offline](offline/advantagekit-reference.md#talonfx-swerve-template)):
  the base for our future drive code
- [AdvantageKit template projects](https://docs.advantagekit.org/getting-started/template-projects/) ([offline](offline/advantagekit-reference.md#template-projects))

## Learning

- [Training Materials](../training-materials/index.md): our own learning path, from Java basics to
  a complete subsystem. (This replaces the old external "Jacob's Tutorials" site.)
- [dev.java Learn](https://dev.java/learn/) ([offline](offline/java-reference.md)): official Java tutorials
- [Learn Git Branching](https://learngitbranching.js.org/) ([offline](offline/git-reference.md#practice-repository-offline))
- [WPILib Advanced Controls introduction](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/index.html) ([offline](offline/controls-reference.md)):
  interactive PID and feedforward demos
- [Controls Engineering in FRC](https://file.tavsys.net/control/controls-engineering-in-frc.pdf) ([offline](offline/controls-reference.md#going-deeper-controls-engineering-in-frc))
  (Tyler Veness): deep dive, for students who want more

## FRC community projects

| Project | What it is | Our status |
|---|---|---|
| [AdvantageKit](https://docs.advantagekit.org/getting-started/what-is-advantagekit/) | Logging built around IO layers and log replay | **Standard.** See [AdvantageKit](../training-materials/advantagekit/why-io.md). |
| [AdvantageScope](https://docs.advantagescope.org/) | Log viewer and analysis tool | **Standard** |
| [PathPlanner](https://pathplanner.dev/home.html) | Path and auto builder with a GUI | **Standard** for autos |
| [PhotonVision](https://photonvision.org/) | Vision processing OS for coprocessors | **Standard** |
| [Elastic](https://frc-elastic.gitbook.io/docs) | Drag-and-drop driver dashboard (replaces SmartDashboard/Shuffleboard) | **Standard** |
| [Choreo](https://choreo.autos/) | Time-optimized trajectory planner | Alternative to PathPlanner for advanced paths |
| [Limelight](https://limelightvision.io/) | Off-the-shelf vision system | Alternative to PhotonVision |
| [QuestNav](https://questnav.gg/) | Robot odometry using a Meta Quest 3S VR headset | Worth watching |
| [DogLog](https://doglog.dev/) | Simplified logging | Alternative to AdvantageKit; we use AdvantageKit |
| [FRC Web Components](https://github.com/frc-web-components/frc-web-components) | Web-based dashboard | Alternative to Elastic |
| [maple-sim](https://shenzhen-robotics-alliance.github.io/maple-sim/) | Physics engine for whole-robot simulation | Used in 2026; its 2026 release was beta and no 2027 release was available as of late 2026. See [Simulation Strategy](../training-materials/simulation/strategy.md#what-about-maplesim). |
| [YAGSL](https://docs.yagsl.com/) | JSON-configured swerve library | **Legacy.** Used through 2026; we're moving to CTRE swerve. |

## Teams whose open-source code is worth reading

- FRC 6328 Mechanical Advantage (creators of AdvantageKit)
- FRC 254 The Cheesy Poofs
- FRC 3847 Spectrum
- FRC 1678 Citrus Circuits

Read their code to see patterns, not to copy it. Every pattern you bring in should be one you can explain.

## Scouting app development

- [Flutter](https://docs.flutter.dev/get-started/install/windows/mobile): [Chicken Scratch](../chicken-scratch/index.md),
  our scouting app, is built with Flutter.
- [Android Studio](https://developer.android.com/studio): Android development tools and emulators.

## Text formatting

- [GitHub Markdown guide](github-markdown-guide.md) (in this wiki)
- [GitHub Flavored Markdown](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax)
- [Discord Markdown](https://support.discord.com/hc/en-us/articles/210298617-Markdown-Text-101-Chat-Formatting-Bold-Italic-Underline)
- [GitLab Flavored Markdown](https://docs.gitlab.com/user/markdown/)

## Event data and other tools

- [Chief Delphi](https://www.chiefdelphi.com/): forum for all things FIRST. Great for programming questions.
- [The Blue Alliance](https://www.thebluealliance.com/): all FRC event data (matches, scores, awards).
  It has a [REST API](https://www.thebluealliance.com/apidocs) for match and event data.
- [Statbotics](https://www.statbotics.io/): FRC analytics and predictions, with a
  [Python API](https://www.statbotics.io/docs/python) and a [REST API](https://www.statbotics.io/docs/rest).
- [Cheesy Arena](https://github.com/Team254/cheesy-arena): open-source field management system we
  use to run our practice field. It's written in [Go](https://go.dev/dl/).
