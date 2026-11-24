# Offline Reference

Everything in this wiki works **without internet**. Wherever a page links to an outside site
(WPILib, CTRE, AdvantageKit, PathPlanner, PhotonVision, and others), the key information from that
site is summarized in this section, in our own words. The external links stay in place for when
you're online and want the full official version.

:::{note}
These pages summarize the official docs as of the 2026 season. Official docs are the final word.
When you're back online after kickoff, check whether anything important changed, and update these
pages with a PR.
:::

## Where each external source is covered

| External source | Local page |
|---|---|
| WPILib installation, NI Game Tools, Driver Station, roboRIO imaging | [Installing the Tools Offline](tools-install.md) |
| Git, GitHub Desktop, Elastic, PathPlanner, and Tuner X installs | [Installing the Tools Offline](tools-install.md) |
| dev.java, Codecademy (Java) | [Java Reference](java-reference.md) |
| Learn Git Branching, GitHub Skills, WPILib Git docs | [Git Reference](git-reference.md) |
| WPILib advanced controls: PID intro, flywheel/arm/turret tuning, SysId, pose estimators | [Controls Reference](controls-reference.md) |
| *Controls Engineering in FRC* (Tyler Veness) | [Controls Reference](controls-reference.md#going-deeper-controls-engineering-in-frc) |
| CTRE Phoenix 6 docs, Tuner X docs, swerve generator, Motion Magic, CTRE SysId, Phoenix 6 examples | [CTRE Reference](ctre-reference.md) |
| AdvantageKit docs and template projects, AdvantageScope docs | [AdvantageKit & AdvantageScope Reference](advantagekit-reference.md) |
| PathPlanner docs | [PathPlanner Reference](pathplanner-reference.md) |
| PhotonVision docs | [PhotonVision Reference](photonvision-reference.md) |
| PhotonVision object detection, Ultralytics YOLO, BlenderProc | [Object Detection](../../training-materials/object-detection/overview.md) section ([references](../../training-materials/object-detection/testing-ops.md#references)) |
| WPILib "New for 2027", SystemCore testing repo, AdvantageKit v27 notes | [What's New for 2027](../../training-materials/season-2027/whats-new.md) |
| WPILib Commands v3 docs and migration guide | [Commands v3](../../training-materials/command-based/commands-v3.md), [Migrating from v2 to v3](../../training-materials/command-based/v2-to-v3.md) |
| REV docs (REVLib, SPARK MAX/Flex, REV Hardware Client, Through Bore, Color Sensor V3) | [REV Motors](../../training-materials/rev/revlib.md), [Sensors](../../training-materials/sensors/overview.md) |
| Sensor vendor docs (CTRE CANrange/CANdi, Redux, Studica navX, Grapple LaserCAN, PWF, ThriftyBot) | [Sensors](../../training-materials/sensors/overview.md) section |
| maple-sim releases | [Simulation Strategy](../../training-materials/simulation/strategy.md) (status summary) |

## Getting this wiki onto a laptop without internet

Pick whichever works for you:

1. **Download from Read the Docs.** While online, open Cluckipedia and use the version menu
   (bottom of the sidebar) to download the **HTML zip**, **PDF**, or **EPUB**. For the HTML zip,
   unzip it anywhere and open `index.html` in a browser. Search and navigation work from the files.
2. **Copy from a mentor's USB stick.** Mentors keep a built copy of the site
   (`docs/build/html` in the Cluckipedia repository). Copy the folder and open `index.html`.
3. **Build it yourself** from the Cluckipedia repository (needs Python; install the packages once
   while online, see the repository README):
   ```powershell
   .venv\Scripts\sphinx-build -b html docs/source docs/build/html
   ```

## Preparing a laptop to code without internet

Robot code needs some downloads the first time. Do these while you have internet (or get them from
a mentor's USB stick):

- [ ] Install everything in [Installing the Tools Offline](tools-install.md). Keep the installers
      on a USB stick; the WPILib installer can be run with no internet.
- [ ] Clone the robot repository.
- [ ] Run `./gradlew build` **once** in that repository. Gradle downloads every library the project
      uses (WPILib, Phoenix 6, AdvantageKit, PathPlannerLib, PhotonLib, ...) into a cache in your
      user folder (`%USERPROFILE%\.gradle`).
- [ ] Run **WPILib: Simulate Robot Code** once, so the simulation libraries are downloaded too.
- [ ] Test it: disconnect from the internet and run `./gradlew build --offline`. If it succeeds, you're
      ready.

After that, building, simulating, and deploying to the robot all work offline. (Deploying only
needs the robot's network, not the internet.)

:::{warning}
Changing library versions (updating a vendordep JSON in `vendordeps/`) needs internet again,
because Gradle has to download the new version. Do library updates at the shop, then build once
before going offline.
:::
