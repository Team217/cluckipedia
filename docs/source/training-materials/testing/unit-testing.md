# Unit Testing

A **unit test** is a small piece of code that runs part of the robot code and checks that it did the
right thing. Tests run on a laptop in seconds, with no robot. They catch the bugs that are hardest to
find on the field: a flipped sign, the wrong units, a button bound to the wrong command, or an
autonomous routine that references something that doesn't exist.

:::{admonition} Every test on this page was run against the 2026 robot code
:class: note
The tests below were compiled and run with WPILib 2026, Phoenix 6, AdvantageKit, and PathPlannerLib
in the 2026 robot repository, using the arm from [Building an IO Subsystem](../advantagekit/io-subsystem.md).
All of them pass except one that found a real bug (see [Checking the auto files](#checking-the-auto-files)).
:::

## What to test, and how

| Kind of test | What it runs | Catches | Speed |
|---|---|---|---|
| **Pure logic** | One class or method with no hardware (math, unit conversions, lookup tables, state machines) | Sign and unit mistakes, edge cases | Instant |
| **Subsystem with a fake IO** | The real subsystem and its commands, with an IO that the test controls | Command logic, limits, brake/coast, finishing conditions | Instant |
| **Subsystem with the sim IO** | The subsystem, the real motor configuration, and a physics model | Wrong gains, gear ratios, or gravity type; a mechanism that never arrives | A few seconds |
| **Teleop bindings** | Your real button bindings, with a simulated controller | Buttons bound to the wrong command, wrong stick direction, broken interlocks | Instant |
| **Auto files** | Every PathPlanner path and auto in `deploy/` | Paths that don't load, named commands that were never registered | About a second |
| **Auto paths** | A full path or auto with a simulated drivetrain, in simulated time | Paths that never finish, routines that get stuck | About a second each |

The [AdvantageKit IO layer](../advantagekit/why-io.md) is what makes most of these possible: because
a subsystem only talks to its IO interface, a test can hand it a fake IO or a sim IO instead of real
hardware.

## Setting up

Our robot projects already include JUnit 5. In `build.gradle`:

```groovy
testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
testRuntimeOnly 'org.junit.platform:junit-platform-launcher'

test {
    useJUnitPlatform()
    systemProperty 'junit.jupiter.extensions.autodetection.enabled', 'true'
}
```

Tests live in `src/test/java`. A test in the same package as the code it tests can also use that
code's package-private methods. The examples on this page all sit in `frc.robot`, so they can share the
helper and the fake IO.

**Running tests:**

- In VS Code: **WPILib: Test Robot Code** from the command palette, or the **Testing** panel (the
  flask icon) to run one test at a time.
- From a terminal: `./gradlew test`.
- `./gradlew build` runs the tests too, so a failing test fails the build. Run it before every pull
  request (see the [Team Git Workflow](../git/team-workflow.md#the-pull-request-process)).

After a run, `build/reports/tests/test/index.html` shows every test and the full message of any
failure.

## How a test is written

```java
class ArmCommandTest {
  @BeforeEach   // runs before every test
  void setUp() { ... }

  @Test         // each @Test method is one test
  void scoreSendsSetpointAndFinishesWhenArrived() {
    ...
    assertEquals(expected, actual, tolerance);   // fails the test if they differ
    assertTrue(condition, "message shown when it fails");
  }

  @AfterEach    // runs after every test, even a failed one
  void tearDown() { ... }
}
```

- **Name each test as a sentence** that says what should happen: `coastsWhileDisabled`, not `test2`.
- **Compare `double`s with a tolerance** (`assertEquals(1.5, x, 1e-9)`), never exactly.
- **Add a message** to `assertTrue` so a failure explains itself.
- **One behavior per test.** When it fails, the name tells you what broke.
- **Tests must not depend on each other.** JUnit doesn't promise an order. Reset everything in
  `@BeforeEach`.

## A test helper for robot code

Robot code expects the hardware layer, the Driver Station, and the command scheduler to exist. This
helper sets them up and gives the test control of time, so a test can run 3 seconds of robot loops
instantly:

```java
package frc.robot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/** Shared setup for robot code unit tests. */
public final class TestUtil {
  private TestUtil() {}

  /** Starts the simulated hardware layer and clears anything left over from another test. */
  public static void setUp() {
    assertTrue(HAL.initialize(500, 0));
    CommandScheduler.getInstance().cancelAll();
    CommandScheduler.getInstance().unregisterAllSubsystems();
    CommandScheduler.getInstance().getDefaultButtonLoop().clear();
    DriverStationSim.resetData();
    SimHooks.pauseTiming(); // the test controls time with step()
  }

  public static void tearDown() {
    CommandScheduler.getInstance().cancelAll();
    CommandScheduler.getInstance().unregisterAllSubsystems();
    CommandScheduler.getInstance().getDefaultButtonLoop().clear();
    SimHooks.resumeTiming();
  }

  /** Puts the simulated Driver Station in teleop (auto = false) or autonomous, enabled. */
  public static void enable(boolean autonomous) {
    DriverStationSim.setAutonomous(autonomous);
    DriverStationSim.setEnabled(true);
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();
  }

  public static void disable() {
    DriverStationSim.setEnabled(false);
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();
  }

  /** One robot loop: read controllers, run the scheduler, then advance time 20 ms. */
  public static void step() {
    DriverStation.refreshData();
    CommandScheduler.getInstance().run();
    SimHooks.stepTiming(0.02);
  }

  public static void run(double seconds) {
    for (int i = 0; i < Math.round(seconds / 0.02); i++) step();
  }
}
```

- `HAL.initialize` starts WPILib's simulated hardware layer.
- The command scheduler is a single shared object, so every test clears it, including the
  **button bindings** (`getDefaultButtonLoop().clear()`), or one test's bindings leak into the next.
- `SimHooks.pauseTiming()` freezes the robot clock. `step()` then advances it exactly 20 ms per loop,
  so timeouts, `WaitCommand`s, and path following all see consistent time.

## Testing a subsystem with a fake IO

A fake IO is a small class, inside the test, that implements the subsystem's IO interface. It
**records** what the subsystem asked for and **reports** whatever the test tells it to. This tests
the subsystem's logic and commands exactly, with no physics involved.

```java
package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.arm.ArmIO;
import frc.robot.Constants.ArmConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArmCommandTest {
  /** A fake IO: records what the subsystem asked for, and reports whatever the test sets. */
  static class FakeArmIO implements ArmIO {
    double positionRot = ArmConstants.kStartPositionRot;
    double lastSetpointRot = Double.NaN;
    double lastVolts = Double.NaN;
    Boolean brake = null;

    @Override
    public void updateInputs(ArmIOInputs inputs) {
      inputs.connected = true;
      inputs.positionRot = positionRot;
    }

    @Override
    public void setPosition(double positionRot) {
      lastSetpointRot = positionRot;
    }

    @Override
    public void setVoltage(double volts) {
      lastVolts = volts;
    }

    @Override
    public void setBrakeMode(boolean brake) {
      this.brake = brake;
    }
  }

  FakeArmIO io;
  Arm arm;

  @BeforeEach
  void setUp() {
    TestUtil.setUp();
    TestUtil.enable(false);
    io = new FakeArmIO();
    arm = new Arm(io);
  }

  @AfterEach
  void tearDown() {
    TestUtil.tearDown();
  }

  @Test
  void scoreSendsSetpointAndFinishesWhenArrived() {
    Command score = arm.score();
    CommandScheduler.getInstance().schedule(score);
    TestUtil.step();
    assertEquals(ArmConstants.kScoreRot, io.lastSetpointRot, 1e-9);
    assertTrue(score.isScheduled(), "still moving");

    io.positionRot = ArmConstants.kScoreRot; // pretend the arm got there
    TestUtil.step(); // periodic reads the new position
    TestUtil.step(); // the command sees atGoal() and ends
    assertFalse(score.isScheduled());
  }

  @Test
  void coastsWhileDisabledAndBrakesWhenEnabled() {
    TestUtil.disable();
    TestUtil.step();
    assertEquals(false, io.brake);
    TestUtil.enable(false);
    TestUtil.step();
    assertEquals(true, io.brake);
  }

  @Test
  void manualStopsTheMotorWhenItEnds() {
    Command manual = arm.manual(() -> 4.0);
    CommandScheduler.getInstance().schedule(manual);
    TestUtil.step();
    assertEquals(4.0, io.lastVolts, 1e-9);
    manual.cancel();
    assertEquals(0.0, io.lastVolts, 1e-9);
  }
}
```

Things worth testing on every subsystem this way:

- Each command sends the right setpoint and **finishes** when (and only when) it should.
- Limits: a setpoint past a software limit is clamped or refused.
- Brake mode while enabled, coast while disabled.
- Motors stop when a command ends or is interrupted.
- Alerts fire when the fake IO reports `connected = false`.

## Testing a subsystem with the sim IO

Running the real sim IO checks the whole chain: the TalonFX configuration, the Motion Magic gains,
the gear ratio, and the physics. If the arm can't reach its setpoint here, it won't on the robot
either.

```java
package frc.robot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.arm.ArmIOSim;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs the real TalonFX configuration and control loop against the arm physics sim. */
class ArmSimTest {
  @BeforeEach
  void setUp() {
    TestUtil.setUp();
    SimHooks.resumeTiming(); // CTRE's simulated devices run in real time
    TestUtil.enable(false);
    Timer.delay(0.1); // give the simulated TalonFX time to see the robot enabled
  }

  @AfterEach
  void tearDown() {
    TestUtil.tearDown();
  }

  @Test
  void armReachesScorePosition() {
    Arm arm = new Arm(new ArmIOSim());
    Command score = arm.score();
    CommandScheduler.getInstance().schedule(score);
    for (int i = 0; i < 150 && score.isScheduled(); i++) { // up to 3 s
      CommandScheduler.getInstance().run();
      Timer.delay(0.02);
    }
    assertFalse(score.isScheduled(), "score() should finish");
    assertTrue(arm.atGoal());
  }
}
```

Two differences from the fake IO test:

- CTRE's simulated devices run on their own clock in **real time**, so this test uses real time
  (`Timer.delay`) instead of paused time, and takes a few seconds.
- The simulated TalonFX only drives its output after it sees the robot enabled, so `setUp()` waits
  100 ms after enabling.

Keep sim tests few and focused on "does it get there": they are slower, and exact results depend on
the physics model.

## Testing teleop

Teleop bugs are usually in the **bindings**: the wrong button, the wrong stick, a sign flipped, or a
safety interlock that doesn't actually block anything. To test the real bindings, put them in a
method that takes the controllers and subsystems, and call that same method from both the robot
container and the test:

```java
package frc.robot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.wpilibj.simulation.XboxControllerSim;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.subsystems.arm.Arm;
import frc.robot.Constants.ArmConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeleopBindingTest {
  /** The same method the container calls, so the test checks the real bindings. */
  static void configureArmBindings(CommandXboxController operator, Arm arm) {
    arm.setDefaultCommand(arm.manual(() -> -operator.getLeftY() * 6.0));
    operator.a().onTrue(arm.score());
    operator.b().onTrue(arm.stow());
  }

  ArmCommandTest.FakeArmIO io;
  Arm arm;
  XboxControllerSim operatorSim;

  @BeforeEach
  void setUp() {
    TestUtil.setUp();
    TestUtil.enable(false);
    io = new ArmCommandTest.FakeArmIO();
    arm = new Arm(io);
    configureArmBindings(new CommandXboxController(1), arm);
    operatorSim = new XboxControllerSim(1);
  }

  @AfterEach
  void tearDown() {
    TestUtil.tearDown();
  }

  @Test
  void aButtonScores() {
    operatorSim.setAButton(true);
    operatorSim.notifyNewData();
    TestUtil.step();
    assertEquals(ArmConstants.kScoreRot, io.lastSetpointRot, 1e-9);
  }

  @Test
  void leftStickDrivesArmManually() {
    operatorSim.setLeftY(-0.5); // stick pushed forward is negative Y
    operatorSim.notifyNewData();
    TestUtil.step(); // default commands are scheduled at the end of a loop...
    TestUtil.step(); // ...and run on the next one
    assertEquals(3.0, io.lastVolts, 1e-9);
  }
}
```

- `XboxControllerSim` sets buttons and stick values on a simulated controller. Call
  `notifyNewData()` after changing them; the helper's `step()` refreshes the Driver Station data and
  runs the scheduler, which checks the triggers.
- **Default commands start one loop late.** The scheduler adds default commands at the end of a loop,
  so they first run on the next loop. That's why `leftStickDrivesArmManually` steps twice.
- WPILib sticks report **negative Y when pushed forward**, so this test also proves the manual
  control isn't inverted.

What to test for teleop:

- Every button does what the driver practice sheet says it does.
- Interlocks really block: for example the climb can't start before the endgame, or the intake
  won't run while stowed.
- Joystick shaping: deadbands, squaring, and max speed scaling (test these as pure functions).
- Field-relative driving flips correctly for the red alliance (set the alliance with
  `DriverStationSim.setAllianceStationId(...)`).

## Testing autonomous

### Checking the auto files

PathPlanner reads paths and autos from `src/main/deploy/pathplanner/` at runtime, so mistakes there
don't show up when the code compiles. This test reads every file and checks it against the code:

```java
package frc.robot;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.Filesystem;
import frc.robot.robotContainers.CompetitionContainer;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Checks every PathPlanner file in deploy/ against the code, without driving anything. */
class AutoFilesTest {
  static final File PATHPLANNER_DIR = new File(Filesystem.getDeployDirectory(), "pathplanner");

  @BeforeAll
  static void registerNamedCommands() {
    TestUtil.setUp();
    new CompetitionContainer(); // registers named commands and configures AutoBuilder
    TestUtil.tearDown();
  }

  static List<File> files(String folder, String ext) {
    File[] all = new File(PATHPLANNER_DIR, folder).listFiles((d, n) -> n.endsWith(ext));
    return all == null ? List.of() : List.of(all);
  }

  @Test
  void everyNamedCommandInAnAutoIsRegistered() throws Exception {
    Pattern named =
        Pattern.compile(
            "\"type\"\\s*:\\s*\"named\".*?\"name\"\\s*:\\s*\"([^\"]+)\"", Pattern.DOTALL);
    List<String> missing = new ArrayList<>();
    for (File auto : files("autos", ".auto")) {
      Matcher m = named.matcher(Files.readString(auto.toPath()));
      while (m.find()) {
        if (!NamedCommands.hasCommand(m.group(1))) missing.add(auto.getName() + ": " + m.group(1));
      }
    }
    assertTrue(missing.isEmpty(), "Named commands used in autos but never registered: " + missing);
  }

  @Test
  void everyPathLoadsAndStartsOnTheField() {
    List<String> problems = new ArrayList<>();
    for (File file : files("paths", ".path")) {
      String name = file.getName().replace(".path", "");
      try {
        PathPlannerPath path = PathPlannerPath.fromPathFile(name);
        Pose2d start = path.getPathPoses().get(0);
        if (start.getX() < 0 || start.getX() > 17.6 || start.getY() < 0 || start.getY() > 8.1) {
          problems.add(name + " starts off the field at " + start);
        }
      } catch (Exception e) {
        problems.add(name + " failed to load: " + e.getMessage());
      }
    }
    assertTrue(problems.isEmpty(), problems.toString());
  }
}
```

:::{admonition} This test found a real bug in the 2026 code
:class: warning
When this test ran against the 2026 robot code, it failed:

`Named commands used in autos but never registered: [MVPLeft.auto: Climb, SnowblowerLeft.auto: Climb, SnowblowerRight.auto: Climb]`

The climber had been commented out of the robot container, so nothing registered `Climb`.
PathPlanner doesn't crash in that case: it prints a warning and runs **nothing** in its place, so
those three autos would quietly skip the climb in a match. The 2026 code's own `AutonTests` only
builds each auto, so it didn't catch this.
:::

Use your project's real robot container to register the named commands, as this test does, so the
test checks exactly what the robot will run.

### Running a path in simulated time

To check that a path (or a whole auto) finishes and ends where it should, drive it with a simulated
drivetrain. The simplest is an **ideal drive** that moves exactly as commanded. It doesn't test the
drivetrain; it tests the path and the auto's logic, and runs 15 seconds of auto in about a second.

```java
package frc.robot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.path.PathPlannerPath;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Drives a PathPlanner path with a perfect simulated drivetrain, in simulated time. */
class AutoPathTest {
  /**
   * A drivetrain that moves exactly as commanded. Tests the path and the auto logic, not the drive.
   */
  static class IdealDrive extends SubsystemBase {
    Pose2d pose = Pose2d.kZero;
    ChassisSpeeds speeds = new ChassisSpeeds(); // robot-relative

    @Override
    public void periodic() {
      pose =
          pose.exp(
              new Twist2d(
                  speeds.vxMetersPerSecond * 0.02,
                  speeds.vyMetersPerSecond * 0.02,
                  speeds.omegaRadiansPerSecond * 0.02));
    }
  }

  IdealDrive drive;

  @BeforeEach
  void setUp() throws Exception {
    TestUtil.setUp();
    TestUtil.enable(true);
    drive = new IdealDrive();
    AutoBuilder.configure(
        () -> drive.pose,
        p -> drive.pose = p,
        () -> drive.speeds,
        (speeds, feedforwards) -> drive.speeds = speeds,
        new PPHolonomicDriveController(new PIDConstants(5.0, 0, 0), new PIDConstants(5.0, 0, 0)),
        RobotConfig.fromGUISettings(),
        () -> false, // blue alliance: no flipping
        drive);
  }

  @AfterEach
  void tearDown() {
    TestUtil.tearDown();
  }

  @Test
  void pathFinishesAtItsEndPoint() throws Exception {
    PathPlannerPath path = PathPlannerPath.fromPathFile("MVP-Mid");
    List<Pose2d> poses = path.getPathPoses();
    drive.pose = path.getStartingHolonomicPose().orElse(poses.get(0));

    Command follow = AutoBuilder.followPath(path);
    CommandScheduler.getInstance().schedule(follow);
    for (int i = 0; i < 15 * 50 && follow.isScheduled(); i++) TestUtil.step(); // up to 15 s

    assertFalse(follow.isScheduled(), "path should finish within 15 s");
    double error =
        drive.pose.getTranslation().getDistance(poses.get(poses.size() - 1).getTranslation());
    assertTrue(error < 0.10, "ended " + error + " m from the end of the path");
  }
}
```

- `AutoBuilder.configure` is given the ideal drive in place of the real one.
- The starting pose comes from the path, just like resetting odometry at the start of an auto.
- The path follower uses the robot clock, so paused, stepped time works.

With the same setup you can test whole autos (`AutoBuilder.buildAuto("Name")`) using fake-IO
subsystems for the mechanisms. Useful checks:

- The auto **finishes within 15 seconds**, even when a game piece sensor never triggers (every wait
  needs a timeout).
- Mechanisms are in the right state when the robot arrives (for example the intake is deployed before
  the pickup path ends).
- Autos for the red alliance end in the mirrored position (set the alliance, and pass a flipping
  supplier that checks it).

## What not to unit test

- **Tuning.** Tests check that a mechanism gets there, not that the gains are perfect. Tune on the
  robot ([Tuning Workflow](../control/tuning.md)).
- **Vendor libraries.** Don't test that Phoenix 6 or PathPlanner work; test **your** code that uses them.
- **Exact sim numbers.** A test that needs the arm at exactly 30.000° after 1.2 s breaks whenever the
  physics model changes. Test "arrives within tolerance within a time limit."

## 2027

- Imports change with the `org.wpilib` package rename; the testing ideas don't.
- Commands v3 uses its own scheduler (`Scheduler.getDefault()`). Check the 2027 WPILib docs for its
  test helpers before converting these tests.
- Re-run every test after updating vendor libraries: tests are the fastest way to find what an update
  broke.
