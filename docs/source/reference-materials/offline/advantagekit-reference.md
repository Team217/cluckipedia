# AdvantageKit & AdvantageScope Reference

Local summary of the AdvantageKit and AdvantageScope documentation linked from
[Why an IO Layer](../../training-materials/advantagekit/why-io.md), [Logging, Replay & AdvantageScope](../../training-materials/advantagekit/logging-replay.md),
[Simulation Strategy](../../training-materials/simulation/strategy.md), and [CTRE Swerve](../../training-materials/swerve/ctre-swerve.md).

Sources: [AdvantageKit docs](https://docs.advantagekit.org/),
[template projects](https://docs.advantagekit.org/getting-started/template-projects/),
[TalonFX(S) swerve template](https://docs.advantagekit.org/getting-started/template-projects/talonfx-swerve-template/),
[AdvantageScope docs](https://docs.advantagescope.org/).

## Adding AdvantageKit to a project

The robot project already has this. Here's what it took, for a new project:

1. **Vendordep:** install `AdvantageKit.json` with **WPILib: Manage Vendor Libraries** (needs internet once).
2. **`build.gradle`:** add the annotation processor that generates `...AutoLogged` classes, and the
   replay-watch task. From our `build.gradle`:
   ```groovy
   task(replayWatch, type: JavaExec) {
       mainClass = "org.littletonrobotics.junction.ReplayWatch"
       classpath = sourceSets.main.runtimeClasspath
   }

   dependencies {
       // ...
       def akitJson = new groovy.json.JsonSlurper().parseText(
           new File(projectDir.getAbsolutePath() + "/vendordeps/AdvantageKit.json").text)
       annotationProcessor "org.littletonrobotics.akit:akit-autolog:$akitJson.version"
   }
   ```
3. **Git metadata:** the `gversion` plugin generates `BuildConstants.java` (Git SHA, branch, date,
   dirty flag) at every build, so it can be recorded in logs.
4. **`Robot` extends `LoggedRobot`**, and its constructor sets up metadata, data receivers, and
   `Logger.start()` (see `Robot.java`).

## Core API

| API | Purpose |
|---|---|
| `Logger.processInputs("Name", inputs)` | Logs an inputs object (real/sim) or fills it from the log (replay) |
| `@AutoLog` on an inputs class | Generates `XxxAutoLogged` with the logging code |
| `Logger.recordOutput("Key", value)` | Logs a computed value. Supports numbers, booleans, strings, arrays, enums, geometry (`Pose2d`, `Pose3d`, `Translation2d`, ...), `SwerveModuleState[]`, `ChassisSpeeds`, and other WPILib struct types. |
| `@AutoLogOutput(key = "...")` | Logs a field or getter automatically every loop |
| `Logger.recordMetadata("Key", "value")` | Logs one-time info (Git SHA, robot name) |
| `Logger.getTimestamp()` | Current timestamp that is consistent during replay |
| `LoggedNetworkNumber` / `LoggedNetworkBoolean` / `LoggedNetworkString` | Dashboard inputs that are logged, so replay sees the same values |
| `LoggedDashboardChooser<T>` | A logged `SendableChooser` (auto selection) |
| `LoggedMechanism2d` | A 2D mechanism drawing that can be logged |
| `Alert` (WPILib) | Persistent dashboard alerts; logged automatically |

## Data receivers and replay

- `WPILOGWriter` writes `.wpilog` files: to the USB stick on the roboRIO (`/U/logs`), or next to
  the source log in replay.
- `NT4Publisher` publishes everything live over NetworkTables, for AdvantageScope and dashboards.
- **Replay:** `Logger.setReplaySource(new WPILOGReader(path))` feeds inputs from a log instead of
  hardware. `setUseTiming(false)` runs as fast as possible.
- **Replay watch** (`./gradlew replayWatch`) re-runs replay automatically every time you save code
  changes, for a fast edit → see results loop.

### Rules for deterministic replay

Replay must produce exactly the same result as the real run, so robot logic must depend **only**
on logged inputs:

- All hardware reads go through IO `updateInputs` and `processInputs`.
- No `System.currentTimeMillis()` / `System.nanoTime()` in logic. Use `Timer.getFPGATimestamp()` or
  `Logger.getTimestamp()`, which replay correctly.
- Dashboard inputs use the `Logged...` classes.
- Be careful with multithreading. Data from other threads (like a high-rate odometry thread) must be
  passed in through inputs. The swerve template does this.
- Random numbers, file reads, and network reads in logic all break replay unless logged as inputs.

## Template projects

AdvantageKit publishes ready-made projects. Each season, download them while online and keep them
on the USB stick.

| Template | Use |
|---|---|
| Skeleton | Bare `LoggedRobot` setup |
| Diff drive | Tank-drive robot with IO layers |
| Spark swerve | Swerve on REV Spark motors |
| **TalonFX(S) swerve** | **Swerve on CTRE hardware. Our base going forward.** |
| Vision | PhotonVision / Limelight vision with an IO layer |

### TalonFX swerve template

**Requirements (default):** 4 TalonFX (or TalonFXS) drive motors, 4 TalonFX(S) steer motors, 4
CANcoders, and a Pigeon 2 (or NavX).

**Structure (`subsystems/drive/`):**

| File | Role |
|---|---|
| `Drive.java` | Subsystem: kinematics, pose estimator, PathPlanner setup, SysId, logging |
| `Module.java` | One swerve module's logic (optimization, states) |
| `ModuleIO.java` | Module IO interface and inputs |
| `ModuleIOTalonFX.java` | Real module using Phoenix 6 |
| `ModuleIOSim.java` | Simulated module |
| `GyroIO.java`, `GyroIOPigeon2.java` | Gyro IO |
| `PhoenixOdometryThread.java` | Reads odometry signals at high rate (250 Hz on CAN FD) and passes them in as inputs, replay-safe |
| `generated/TunerConstants.java` | From the Tuner X generator; replace with yours |
| `commands/DriveCommands.java` | Joystick drive, drive-at-angle, and characterization commands |

**Setup steps:**

1. Generate `TunerConstants.java` with the Tuner X swerve generator (see
   [CTRE Reference](ctre-reference.md#swerve-project-generator)) and replace the template's copy.
2. Check the template docs' notes on changes to the generated file. The template applies the
   drive gear ratio on the motor (`SensorToMechanismRatio`), so its drive feedforward units differ
   from CTRE's generated defaults. Use the template's characterization to find them.
3. Deploy and test driving at low speed.
4. **Feedforward characterization** command: ramps drive voltage and prints kS and kV.
5. **Wheel radius characterization** command: spins the robot in place and compares gyro rotation
   with wheel travel to compute the real wheel radius. Wheels wear, so redo this at events.
6. Tune steer and drive PID if needed.
7. Configure PathPlanner settings (mass, MOI, wheel COF) to match the robot.

## AdvantageScope

AdvantageScope comes with WPILib. Open it from the WPILib tools.

### Opening data

| Data source | How |
|---|---|
| A log file | **File → Open Log(s)**, or drag the `.wpilog` onto the window |
| Live robot | **File → Connect to Robot** (uses the team number from preferences) |
| Simulation | **File → Connect to Simulator** |
| Robot's USB log files | **File → Download Logs**, connect, and choose files to save |

Set the team number (217) and other options in AdvantageScope's **Preferences** (in the Help menu on Windows, the app menu on macOS).

### Navigating

- The **sidebar** lists every logged key in a tree. Drag keys into tabs.
- The **timeline** at the top shows enabled/auto/teleop periods. Click to set the time; drag to
  select a range; scroll to zoom.
- Add tabs with **+**. Each tab type views data differently.

### Tab types

| Tab | Use |
|---|---|
| **Line Graph** | Numbers over time (left/right axes), plus discrete fields such as booleans and states |
| **2D Field** | Robot pose, vision poses, and trajectories on a field image |
| **3D Field** | Robot model, mechanism components, game pieces, camera poses, in 3D |
| **Table** | Raw values at the selected time |
| **Console** | Printed messages |
| **Statistics** | Histograms and stats for numbers, for example loop time |
| **Video** | Synced match video |
| **Joysticks** | Controller inputs |
| **Swerve** | Module states: measured vs. setpoint arrows |
| **Mechanism** | `LoggedMechanism2d` drawings |
| **Points** | 2D point sets |
| **Metadata** | Log metadata (Git SHA, branch, dirty flag) |

### Layouts

**File → Export Layout** saves your tabs and graphs to a JSON file; **Import Layout** loads it.
Commit useful layouts to the repo so everyone uses the same views.

### Custom robot models

AdvantageScope can show a CAD model of our robot in the 3D Field. Each model is a folder (for
example `ascope_assets/assets/Robot_TC26CompMKiiiSim/`) with:

- `config.json`: name, rotation/position offsets, camera positions, and **components**
- `model.glb`: the base model, plus `model_0.glb`, `model_1.glb`, ... for each moving component

Point AdvantageScope at the assets folder with its **custom assets folder** option (same menu as Preferences). Moving components
are driven by logging an array of `Pose3d` (one per component). The 2026
`utilities/RobotVisualizer.java` shows how.
