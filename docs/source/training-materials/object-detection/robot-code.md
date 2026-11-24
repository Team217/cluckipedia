# Object Detection: Robot Code

This page turns PhotonVision detections into **field positions of game pieces**, then uses them for
teleop assists and autonomous pickups. It follows the same structure as every other subsystem on this
wiki: an **IO layer** for the camera, a subsystem with the logic, and commands.

:::{admonition} Every class on this page was compiled and unit-tested
:class: note
The Java files on this page were compiled against our 2026 libraries (WPILib 2026.2.1, PhotonLib
v2026.3.2, AdvantageKit, PathPlannerLib 2026.1.2), and the unit tests at the bottom pass. The drive
methods they call come from the [AdvantageKit TalonFX swerve template](../swerve/ctre-swerve.md#using-it-with-advantagekit).
:::

## Architecture

```text
PhotonVision (coprocessor)
      │  detections: yaw, pitch, class, confidence, timestamp
      ▼
GamePieceVisionIO           ← PhotonVision on the robot, fake detections in sim (logged, replayable)
      ▼
GamePieceVision subsystem
  • PieceProjection: each detection → a field position, using the robot pose WHEN THE FRAME WAS TAKEN
  • PieceTracker: remembers pieces across frames (smoothing, confirmation, expiry)
  • logs observations and confirmed pieces for AdvantageScope
      │
      ├── DriveToPiece     (teleop + auto: drive onto the piece until the intake has it)
      ├── aimAssist        (teleop: robot points at the piece, driver translates)
      ├── triggers         (rumble, auto-deploy intake)
      └── auto patterns    (vision-finished pickup, interrupt-on-sight, dynamic multi-piece)
```

**Why track pieces in field coordinates instead of steering straight from `yaw`?**

- When the piece goes **under the intake**, it leaves the camera's view. The tracker remembers where
  it was, so the robot can finish the pickup "blind."
- One dropped frame doesn't make the robot jerk or switch targets.
- Autos can **choose** between pieces (nearest, on our side, not near an opponent).
- PathPlanner pathfinding needs a field position to drive to.

**Why split out `PieceProjection` and `PieceTracker`?** They are plain Java with no hardware and no
clock calls (timestamps are passed in). That makes them easy to unit-test on a laptop and means
they behave identically on the robot, in simulation, and in log replay.

## Constants

```java
package frc.robot.subsystems.gamepiece;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.util.Units;

public final class GamePieceVisionConstants {
  public static final String kCameraName = "intakeCam"; // must match the PhotonVision camera name

  // Camera position from robot center on the floor: x forward, y left, z up (meters).
  // Pitch: POSITIVE = tilted DOWN.
  public static final Transform3d kRobotToCamera =
      new Transform3d(
          new Translation3d(0.30, 0.0, 0.25),
          new Rotation3d(0.0, Units.degreesToRadians(25.0), 0.0));

  public static final double kPieceCenterHeightM = 0.05; // height of the piece's center
  public static final int kPieceClassId = 0; // index in the model's label list
  public static final double kMinConfidence = 0.55;
  public static final double kMaxRangeM = 5.0;

  // Tracker tuning
  public static final double kAssociationDistanceM = 0.35; // same piece if this close
  public static final double kTrackTimeoutSec = 0.75; // forget pieces not seen for this long
  public static final int kMinHitsToConfirm = 3; // sightings before a piece is "real"
  public static final double kPositionSmoothing = 0.3; // 0..1, higher = trust new data more

  // Which way the intake faces: kZero = robot front, kPi = robot back
  public static final Rotation2d kIntakeDirection = Rotation2d.kZero;

  // Update each season from the field drawings
  public static final double kFieldLengthM = 17.55;
  public static final double kFieldWidthM = 8.05;

  // Simulated camera field of view
  public static final double kSimHorizontalFovDeg = 70.0;
  public static final double kSimVerticalFovDeg = 55.0;

  private GamePieceVisionConstants() {}
}
```


**Measure the camera transform carefully.** Every centimeter or degree of error becomes position
error on the field. Re-measure after any hard hit. See [Units & Geometry](../frc-basics/units-geometry.md)
for `Transform3d` conventions.

## The IO layer

The inputs hold **raw detections**: one array entry per detection, from every frame since the last
loop. Logging the raw data (not the computed field positions) means you can change the tracker and
re-run it on a match log with [replay](../advantagekit/logging-replay.md).

```java
package frc.robot.subsystems.gamepiece;

import org.littletonrobotics.junction.AutoLog;

/** Raw detections from the game piece camera. One array entry per detection. */
public interface GamePieceVisionIO {
  @AutoLog
  public static class GamePieceVisionIOInputs {
    public boolean connected = false;
    public double[] timestampsSec = new double[] {}; // when each detection's frame was captured
    public double[] yawDeg = new double[] {}; // + = right
    public double[] pitchDeg = new double[] {}; // + = up
    public int[] classIds = new int[] {};
    public double[] confidences = new double[] {};
  }

  public default void updateInputs(GamePieceVisionIOInputs inputs) {}
}
```


### Real robot: PhotonVision

```java
package frc.robot.subsystems.gamepiece;

import java.util.ArrayList;
import java.util.List;
import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

public class GamePieceVisionIOPhotonVision implements GamePieceVisionIO {
  private final PhotonCamera camera;

  public GamePieceVisionIOPhotonVision(String cameraName) {
    camera = new PhotonCamera(cameraName);
  }

  @Override
  public void updateInputs(GamePieceVisionIOInputs inputs) {
    inputs.connected = camera.isConnected();

    List<Double> timestamps = new ArrayList<>();
    List<Double> yaws = new ArrayList<>();
    List<Double> pitches = new ArrayList<>();
    List<Integer> classIds = new ArrayList<>();
    List<Double> confidences = new ArrayList<>();

    // Every frame since the last loop, not just the newest one
    for (PhotonPipelineResult result : camera.getAllUnreadResults()) {
      for (PhotonTrackedTarget target : result.getTargets()) {
        timestamps.add(result.getTimestampSeconds());
        yaws.add(target.getYaw());
        pitches.add(target.getPitch());
        classIds.add(target.getDetectedObjectClassID());
        confidences.add((double) target.getDetectedObjectConfidence());
      }
    }

    inputs.timestampsSec = timestamps.stream().mapToDouble(Double::doubleValue).toArray();
    inputs.yawDeg = yaws.stream().mapToDouble(Double::doubleValue).toArray();
    inputs.pitchDeg = pitches.stream().mapToDouble(Double::doubleValue).toArray();
    inputs.classIds = classIds.stream().mapToInt(Integer::intValue).toArray();
    inputs.confidences = confidences.stream().mapToDouble(Double::doubleValue).toArray();
  }
}
```


PhotonLib timestamps are in the same timebase as `Timer.getTimestamp()`, so they can be used
directly to look up the robot's past pose.

### Simulation: fake detections

The sim IO works backwards: given where pieces are in the simulated world, it computes the yaw and
pitch PhotonVision would report, and drops pieces outside the camera's field of view.

```java
package frc.robot.subsystems.gamepiece;

import static frc.robot.subsystems.gamepiece.GamePieceVisionConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Fakes detections of pieces at known field positions, as seen from the simulated robot. */
public class GamePieceVisionIOSim implements GamePieceVisionIO {
  private final Supplier<Pose2d> robotPose;
  private final Supplier<List<Translation2d>> piecePositions;

  public GamePieceVisionIOSim(
      Supplier<Pose2d> robotPose, Supplier<List<Translation2d>> piecePositions) {
    this.robotPose = robotPose;
    this.piecePositions = piecePositions;
  }

  @Override
  public void updateInputs(GamePieceVisionIOInputs inputs) {
    inputs.connected = true;
    Pose2d pose = robotPose.get();
    List<double[]> seen = new ArrayList<>();

    for (Translation2d piece : piecePositions.get()) {
      if (piece.getDistance(pose.getTranslation()) > kMaxRangeM) continue;
      PieceProjection.toYawPitch(piece, pose, kRobotToCamera, kPieceCenterHeightM)
          .filter(a -> Math.abs(a[0]) < kSimHorizontalFovDeg / 2.0)
          .filter(a -> Math.abs(a[1]) < kSimVerticalFovDeg / 2.0)
          .ifPresent(seen::add);
    }

    int n = seen.size();
    inputs.timestampsSec = new double[n];
    inputs.yawDeg = new double[n];
    inputs.pitchDeg = new double[n];
    inputs.classIds = new int[n];
    inputs.confidences = new double[n];
    for (int i = 0; i < n; i++) {
      inputs.timestampsSec[i] = Timer.getTimestamp();
      inputs.yawDeg[i] = seen.get(i)[0];
      inputs.pitchDeg[i] = seen.get(i)[1];
      inputs.classIds[i] = kPieceClassId;
      inputs.confidences[i] = 0.9;
    }
  }
}
```


## Projection: detection → field position

The camera reports **angles**, not distances. To find where a piece is, cast a ray from the camera
through the detection and find where it hits the plane at the piece's center height. This works for
any camera tilt, as long as the piece sits on the floor.

```java
package frc.robot.subsystems.gamepiece;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import java.util.Optional;

/** Converts between camera angles and field positions. Pure math: no hardware, easy to test. */
public final class PieceProjection {
  private PieceProjection() {}

  /**
   * Field position of a detected piece, found by casting a ray from the camera through the
   * detection and intersecting it with the plane at the piece's center height.
   *
   * @return empty if the ray doesn't reach that plane, or the piece is farther than maxRangeM
   */
  public static Optional<Translation2d> toField(
      double yawDeg,
      double pitchDeg,
      Pose2d robotPose,
      Transform3d robotToCamera,
      double pieceHeightM,
      double maxRangeM) {
    // Ray in the camera frame (x forward, y left, z up). PhotonVision reports +yaw = right and
    // +pitch = up, each as atan(pixel offset / focal length), so the ray is (1, -tan yaw, tan
    // pitch).
    Translation3d rayCam =
        new Translation3d(
            1.0, -Math.tan(Math.toRadians(yawDeg)), Math.tan(Math.toRadians(pitchDeg)));

    // Into the robot frame
    Translation3d dir = rayCam.rotateBy(robotToCamera.getRotation());
    Translation3d origin = robotToCamera.getTranslation();

    double dz = pieceHeightM - origin.getZ();
    if (Math.abs(dir.getZ()) < 1e-6) return Optional.empty(); // ray parallel to the floor
    double s = dz / dir.getZ();
    if (s <= 0) return Optional.empty(); // plane is behind the camera along this ray

    Translation2d robotRelative = origin.plus(dir.times(s)).toTranslation2d();
    if (robotRelative.getNorm() > maxRangeM) return Optional.empty();

    return Optional.of(
        robotPose.transformBy(new Transform2d(robotRelative, Rotation2d.kZero)).getTranslation());
  }

  /**
   * The reverse: the yaw and pitch (degrees) PhotonVision would report for a piece. Used by the
   * simulation IO and by tests.
   *
   * @return {yawDeg, pitchDeg}, or empty if the piece is behind the camera
   */
  public static Optional<double[]> toYawPitch(
      Translation2d piece, Pose2d robotPose, Transform3d robotToCamera, double pieceHeightM) {
    Pose3d camera = new Pose3d(robotPose).transformBy(robotToCamera);
    Translation3d inCamera =
        new Translation3d(piece.getX(), piece.getY(), pieceHeightM)
            .minus(camera.getTranslation())
            .rotateBy(camera.getRotation().unaryMinus());
    if (inCamera.getX() <= 0.01) return Optional.empty();
    double yawDeg = -Math.toDegrees(Math.atan(inCamera.getY() / inCamera.getX()));
    double pitchDeg = Math.toDegrees(Math.atan(inCamera.getZ() / inCamera.getX()));
    return Optional.of(new double[] {yawDeg, pitchDeg});
  }
}
```


## The tracker

```java
package frc.robot.subsystems.gamepiece;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Remembers game pieces on the field across camera frames: smooths their positions, confirms them
 * after several sightings, and forgets them when they stop being seen. Plain Java with timestamps
 * passed in, so it runs the same on the robot, in simulation, in replay, and in unit tests.
 */
public class PieceTracker {
  public static class TrackedPiece {
    public Translation2d position;
    public double lastSeenSec;
    public int hits = 1;
    public double confidence;

    TrackedPiece(Translation2d position, double timeSec, double confidence) {
      this.position = position;
      this.lastSeenSec = timeSec;
      this.confidence = confidence;
    }
  }

  private final double associationDistanceM;
  private final double timeoutSec;
  private final int minHits;
  private final double smoothing;
  private final List<TrackedPiece> pieces = new ArrayList<>();

  public PieceTracker(
      double associationDistanceM, double timeoutSec, int minHits, double smoothing) {
    this.associationDistanceM = associationDistanceM;
    this.timeoutSec = timeoutSec;
    this.minHits = minHits;
    this.smoothing = smoothing;
  }

  /** Merges a sighting into the closest existing piece, or starts tracking a new one. */
  public void addObservation(Translation2d position, double timeSec, double confidence) {
    TrackedPiece closest = null;
    double closestDist = associationDistanceM;
    for (TrackedPiece p : pieces) {
      double d = p.position.getDistance(position);
      if (d < closestDist) {
        closestDist = d;
        closest = p;
      }
    }
    if (closest == null) {
      pieces.add(new TrackedPiece(position, timeSec, confidence));
    } else {
      closest.position = closest.position.interpolate(position, smoothing);
      closest.lastSeenSec = Math.max(closest.lastSeenSec, timeSec);
      closest.hits++;
      closest.confidence = confidence;
    }
  }

  /** Forgets pieces that haven't been seen recently. Call once per loop. */
  public void removeStale(double nowSec) {
    pieces.removeIf(p -> nowSec - p.lastSeenSec > timeoutSec);
  }

  public boolean isConfirmed(TrackedPiece p) {
    return p.hits >= minHits;
  }

  public List<TrackedPiece> getConfirmed() {
    return pieces.stream().filter(this::isConfirmed).toList();
  }

  /** Best piece = closest, plus 0.5 m of cost per radian the robot must turn to face it. */
  public Optional<TrackedPiece> getBest(
      Pose2d robotPose, Rotation2d intakeDirection, Predicate<Translation2d> allowed) {
    Rotation2d intakeHeading = robotPose.getRotation().plus(intakeDirection);
    return getConfirmed().stream()
        .filter(p -> allowed.test(p.position))
        .min(
            Comparator.comparingDouble(
                p -> {
                  Translation2d toPiece = p.position.minus(robotPose.getTranslation());
                  double turn = Math.abs(toPiece.getAngle().minus(intakeHeading).getRadians());
                  return toPiece.getNorm() + 0.5 * turn;
                }));
  }

  /** The tracked piece closest to a point, used to stay locked on the same piece. */
  public Optional<TrackedPiece> getNear(Translation2d point, double radiusM) {
    return pieces.stream()
        .filter(p -> p.position.getDistance(point) < radiusM)
        .min(Comparator.comparingDouble(p -> p.position.getDistance(point)));
  }
}
```


## Latency compensation: the robot's past pose

The frame was captured some time ago (often 30–60 ms). Projecting it from the robot's *current*
pose puts the piece in the wrong place whenever the robot is moving or turning. WPILib's pose
estimator keeps a short history, so add one method to the template's `Drive`:

```java
/** The estimated pose at a past timestamp (seconds, same timebase as Timer.getTimestamp()). */
public Optional<Pose2d> getPoseAtTime(double timestampSec) {
  return poseEstimator.sampleAt(timestampSec);
}
```

(CTRE's generated swerve has the same idea built in as `samplePoseAt(...)`, but its timestamps use
CTRE's timebase. If you use it, convert with `Utils.fpgaToCurrentTime(...)`.)

## The subsystem

```java
package frc.robot.subsystems.gamepiece;

import static frc.robot.subsystems.gamepiece.GamePieceVisionConstants.*;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.subsystems.gamepiece.PieceTracker.TrackedPiece;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.littletonrobotics.junction.Logger;

public class GamePieceVision extends SubsystemBase {
  private final GamePieceVisionIO io;
  private final GamePieceVisionIOInputsAutoLogged inputs = new GamePieceVisionIOInputsAutoLogged();
  private final Supplier<Pose2d> currentPose;
  private final DoubleFunction<Optional<Pose2d>> poseAtTime;
  private final PieceTracker tracker =
      new PieceTracker(
          kAssociationDistanceM, kTrackTimeoutSec, kMinHitsToConfirm, kPositionSmoothing);
  private final Alert disconnectedAlert =
      new Alert("Game piece camera disconnected", AlertType.kWarning);

  public GamePieceVision(
      GamePieceVisionIO io,
      Supplier<Pose2d> currentPose,
      DoubleFunction<Optional<Pose2d>> poseAtTime) {
    this.io = io;
    this.currentPose = currentPose;
    this.poseAtTime = poseAtTime;
  }

  @Override
  public void periodic() {
    io.updateInputs(inputs);
    Logger.processInputs("GamePieceVision", inputs);
    disconnectedAlert.set(!inputs.connected);

    List<Pose2d> accepted = new ArrayList<>();
    for (int i = 0; i < inputs.timestampsSec.length; i++) {
      if (inputs.classIds[i] != kPieceClassId || inputs.confidences[i] < kMinConfidence) continue;

      // Robot pose when the frame was CAPTURED, not now (latency compensation)
      double t = inputs.timestampsSec[i];
      Optional<Pose2d> robotPose = poseAtTime.apply(t);
      if (robotPose.isEmpty()) continue;

      double confidence = inputs.confidences[i];
      PieceProjection.toField(
              inputs.yawDeg[i],
              inputs.pitchDeg[i],
              robotPose.get(),
              kRobotToCamera,
              kPieceCenterHeightM,
              kMaxRangeM)
          .filter(GamePieceVision::isOnField)
          .ifPresent(
              p -> {
                tracker.addObservation(p, t, confidence);
                accepted.add(new Pose2d(p, Rotation2d.kZero));
              });
    }
    tracker.removeStale(Timer.getTimestamp());

    Logger.recordOutput("GamePieceVision/Observations", accepted.toArray(Pose2d[]::new));
    Logger.recordOutput(
        "GamePieceVision/ConfirmedPieces",
        tracker.getConfirmed().stream()
            .map(p -> new Pose2d(p.position, Rotation2d.kZero))
            .toArray(Pose2d[]::new));
  }

  private static boolean isOnField(Translation2d p) {
    return p.getX() > 0 && p.getX() < kFieldLengthM && p.getY() > 0 && p.getY() < kFieldWidthM;
  }

  // ---------------- Queries used by commands and triggers ----------------

  public boolean hasConfirmedPiece() {
    return !tracker.getConfirmed().isEmpty();
  }

  public boolean hasConfirmedPieceWithin(double meters) {
    Translation2d robot = currentPose.get().getTranslation();
    return tracker.getConfirmed().stream().anyMatch(p -> p.position.getDistance(robot) < meters);
  }

  public Optional<TrackedPiece> getBestPiece(Pose2d robotPose, Predicate<Translation2d> allowed) {
    return tracker.getBest(robotPose, kIntakeDirection, allowed);
  }

  public Optional<TrackedPiece> getBestPiece(Pose2d robotPose) {
    return getBestPiece(robotPose, p -> true);
  }

  /** Re-finds the piece we're locked onto, so the target doesn't jump between pieces. */
  public Optional<TrackedPiece> getPieceNear(Translation2d point, double radiusM) {
    return tracker.getNear(point, radiusM);
  }
}
```


In AdvantageScope, add `GamePieceVision/ConfirmedPieces` and `GamePieceVision/Observations` to a
2D or 3D field view next to the robot pose. You'll see exactly what the robot "thinks" is on the field.

## `DriveToPiece`: the core pickup command

Locks onto one piece, turns the intake toward it, drives at it, keeps going briefly after the piece
disappears under the intake, and ends when the intake's sensor sees the piece (a beam break, CANrange,
or current check; see [Presence sensors](../sensors/presence.md)). An optional `speedScale` lets a
driver control the speed with a trigger.

```java
package frc.robot.subsystems.gamepiece;

import static frc.robot.subsystems.gamepiece.GamePieceVisionConstants.kIntakeDirection;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.subsystems.gamepiece.PieceTracker.TrackedPiece;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Locks onto one piece, turns the intake toward it, and drives onto it. Keeps going briefly after
 * the piece disappears under the intake, and ends when the intake sensor has the piece.
 */
public class DriveToPiece extends Command {
  private static final double kMaxSpeed = 3.0; // m/s
  private static final double kMinSpeed = 0.6; // keep pushing into the piece at the end
  private static final double kDistanceP = 2.0; // m/s per meter of distance
  private static final double kLostTimeoutSec = 0.6; // keep driving this long after last sighting
  private static final double kAcquireTimeoutSec = 0.5; // give up if no piece found by then
  private static final double kHeadingFreezeM = 0.35; // stop re-aiming when this close

  private final Drive drive;
  private final GamePieceVision vision;
  private final BooleanSupplier hasPiece;
  private final DoubleSupplier speedScale;
  private final PIDController headingPid = new PIDController(5.0, 0.0, 0.2);

  private Translation2d target;
  private Rotation2d lockedHeading;
  private double lastSeenSec;
  private double startSec;

  /** speedScale (0..1) lets a driver control speed with a trigger; use () -> 1.0 for full auto. */
  public DriveToPiece(
      Drive drive, GamePieceVision vision, BooleanSupplier hasPiece, DoubleSupplier speedScale) {
    this.drive = drive;
    this.vision = vision;
    this.hasPiece = hasPiece;
    this.speedScale = speedScale;
    headingPid.enableContinuousInput(-Math.PI, Math.PI);
    addRequirements(drive);
    setName("DriveToPiece");
  }

  public DriveToPiece(Drive drive, GamePieceVision vision, BooleanSupplier hasPiece) {
    this(drive, vision, hasPiece, () -> 1.0);
  }

  @Override
  public void initialize() {
    startSec = Timer.getTimestamp();
    target = null;
    lockedHeading = null;
    headingPid.reset();
  }

  @Override
  public void execute() {
    Optional<TrackedPiece> piece =
        (target == null)
            ? vision.getBestPiece(drive.getPose())
            : vision.getPieceNear(target, 0.5); // stay locked on the same piece
    piece.ifPresent(
        p -> {
          target = p.position;
          lastSeenSec = p.lastSeenSec;
        });

    if (target == null) {
      drive.stop();
      return;
    }

    Pose2d pose = drive.getPose();
    Translation2d toPiece = target.minus(pose.getTranslation());
    double distance = toPiece.getNorm();

    // Point the intake at the piece; freeze the heading up close so the robot doesn't wobble
    if (distance > kHeadingFreezeM || lockedHeading == null) {
      lockedHeading = toPiece.getAngle().minus(kIntakeDirection);
    }
    double omega =
        headingPid.calculate(pose.getRotation().getRadians(), lockedHeading.getRadians());

    // Slow down while not yet facing the piece
    double headingError = Math.abs(pose.getRotation().minus(lockedHeading).getRadians());
    double speed =
        MathUtil.clamp(kDistanceP * distance, kMinSpeed, kMaxSpeed)
            * Math.max(0.0, Math.cos(headingError))
            * MathUtil.clamp(speedScale.getAsDouble(), 0.0, 1.0);

    // Field-frame velocity toward the piece, converted to robot-relative for runVelocity()
    Translation2d v = new Translation2d(speed, toPiece.getAngle());
    drive.runVelocity(
        ChassisSpeeds.fromFieldRelativeSpeeds(v.getX(), v.getY(), omega, pose.getRotation()));
  }

  @Override
  public boolean isFinished() {
    double now = Timer.getTimestamp();
    if (hasPiece.getAsBoolean()) return true;
    if (target == null) return now - startSec > kAcquireTimeoutSec;
    return now - lastSeenSec > kLostTimeoutSec;
  }

  @Override
  public void end(boolean interrupted) {
    drive.stop();
  }
}
```


**Tuning order:** heading PID first (robot standing still, piece off to the side), then `kDistanceP` and
`kMaxSpeed`, then `kMinSpeed` (it should push firmly into the piece without bulldozing it away), then
`kLostTimeoutSec` (just long enough to finish after the piece leaves view). Once it works, move the
gains to [`LoggedTunableNumber`s](../control/tuning.md) so you can tune without redeploying.

## Teleop

### Aim assist

The driver keeps full control of translation. While the button is held and a piece is confirmed, the
robot turns its intake toward the piece. It reuses the template's `DriveCommands.joystickDriveAtAngle`,
which already handles deadbands and alliance flipping; we only supply the angle.

```java
/** Driver translates; while a piece is visible the robot turns its intake toward it. */
public static Command aimAssist(
    Drive drive, GamePieceVision vision, DoubleSupplier xSupplier, DoubleSupplier ySupplier) {
  AtomicReference<Translation2d> locked = new AtomicReference<>(); // lambdas can't reassign locals
  return Commands.runOnce(() -> locked.set(null))
      .andThen(
          DriveCommands.joystickDriveAtAngle(
              drive,
              xSupplier,
              ySupplier,
              () -> {
                Pose2d pose = drive.getPose();
                var piece =
                    (locked.get() == null)
                        ? vision.getBestPiece(pose)
                        : vision.getPieceNear(locked.get(), 0.5);
                locked.set(piece.map(p -> p.position).orElse(null));
                return piece
                    .map(p -> p.position.minus(pose.getTranslation()).getAngle().minus(kIntakeDirection))
                    .orElse(drive.getRotation()); // no piece: hold the current heading
              }))
      .withName("AimAssist");
}
```

`AtomicReference` is just a box that a lambda can change. Java lambdas can't reassign local
variables (see [Interfaces & Lambdas](../java/interfaces-lambdas.md)).

### Bindings

In the container, after `drive`, `intake`, and `pieceVision` are created:

```java
// 1) AIM ASSIST: hold Left Bumper. Driver translates, robot points the intake at the piece.
driver.leftBumper().whileTrue(
    GamePieceCommands.aimAssist(drive, pieceVision, () -> -driver.getLeftY(), () -> -driver.getLeftX()));

// 2) FULL AUTO PICKUP: hold Right Bumper. Release at any time to cancel.
driver.rightBumper().whileTrue(
    new DriveToPiece(drive, pieceVision, intake::hasPiece)
        .deadlineFor(intake.intakeCommand())
        .andThen(rumble(0.3)));

// 3) SEMI-AUTO PICKUP: hold Right Trigger. Robot steers, trigger depth = speed.
driver.rightTrigger(0.1).whileTrue(
    new DriveToPiece(drive, pieceVision, intake::hasPiece, driver::getRightTriggerAxis)
        .deadlineFor(intake.intakeCommand()));

// 4) DRIVER FEEDBACK: short rumble when a piece is within 3 m and we're empty
new Trigger(() -> pieceVision.hasConfirmedPieceWithin(3.0) && !intake.hasPiece())
    .and(RobotModeTriggers.teleop())
    .onTrue(rumble(0.15));

// 5) OPTIONAL AUTO-DEPLOY: lower the intake when a piece is close and we're empty
new Trigger(() -> pieceVision.hasConfirmedPieceWithin(1.0) && !intake.hasPiece())
    .and(RobotModeTriggers.teleop())
    .whileTrue(intake.deployCommand());
```

```java
private Command rumble(double seconds) {
  return Commands.startEnd(
          () -> driver.getHID().setRumble(RumbleType.kBothRumble, 0.6),
          () -> driver.getHID().setRumble(RumbleType.kBothRumble, 0.0))
      .withTimeout(seconds);
}
```

LEDs work the same way: bind an LED command to the same triggers (for example green when a piece is
confirmed, white when one is in the intake). See [Triggers](../command-based/triggers.md).

## Autonomous

The pickup used by every pattern below. **Always add a timeout** so auto can't get stuck:

```java
Command visionPickup() {
  return new DriveToPiece(drive, pieceVision, intake::hasPiece)
      .withTimeout(2.0)
      .deadlineFor(intake.intakeCommand());
}
```

To use it from the PathPlanner GUI, register it **before** `AutoBuilder` builds any autos (see
[PathPlanner](../autonomous/pathplanner.md)):

```java
NamedCommands.registerCommand("VisionPickup", visionPickup());
NamedCommands.registerCommand(
    "WaitForPieceSeen", Commands.waitUntil(pieceVision::hasConfirmedPiece).withTimeout(0.5));
```

### Pattern A: path to an approach point, then vision finishes

In the PathPlanner GUI, end the path about **0.8–1.2 m before** the expected piece, facing it. After
the pickup, use pathfind-then-follow so the robot rejoins the plan from wherever it actually ended up:

```java
private final PathConstraints constraints =
    new PathConstraints(3.5, 3.0, Math.toRadians(540), Math.toRadians(720));

Command patternA() {
  try {
    PathPlannerPath toPiece = PathPlannerPath.fromPathFile("Start_To_Piece1_Approach");
    PathPlannerPath toScore = PathPlannerPath.fromPathFile("Piece1_To_Score");
    return Commands.sequence(
        AutoBuilder.followPath(toPiece),
        visionPickup(),
        AutoBuilder.pathfindThenFollowPath(toScore, constraints), // from wherever we ended up
        scoreCommand());
  } catch (Exception e) {
    DriverStation.reportError("Path load failed: " + e.getMessage(), e.getStackTrace());
    return Commands.none();
  }
}
```

### Pattern B: hand over to vision as soon as a piece is confirmed

Faster than Pattern A, because vision takes over early. If vision never sees the piece, the path
just finishes normally. Use it for pieces that may have been bumped, like midfield pieces.

```java
Command visionAwarePickup(String approachPathName) {
  try {
    PathPlannerPath path = PathPlannerPath.fromPathFile(approachPathName);
    return AutoBuilder.followPath(path)
        .until(() -> pieceVision.hasConfirmedPieceWithin(1.5))
        .andThen(visionPickup());
  } catch (Exception e) {
    DriverStation.reportError("Path load failed: " + e.getMessage(), e.getStackTrace());
    return Commands.none();
  }
}
```

### Pattern C: dynamic multi-piece auto

The robot decides **at runtime** which piece to get. If none is visible, it drives to the next search
waypoint and scans. If a pickup fails, it moves on. This needs PathPlanner pathfinding (`AutoBuilder`
configured, and a `navgrid.json` from the PathPlanner GUI).

```java
// Where to look when nothing is visible (blue-origin field coordinates), in priority order
private final List<Pose2d> searchWaypoints =
    List.of(
        new Pose2d(8.3, 7.4, Rotation2d.fromDegrees(-90)),
        new Pose2d(8.3, 5.8, Rotation2d.fromDegrees(-90)),
        new Pose2d(8.3, 4.1, Rotation2d.fromDegrees(-90)));
private int searchIndex = 0;

/** Only chase pieces where we're allowed to and want to go (example: our half + midline). */
private boolean allowedPiece(Translation2d p) {
  boolean red = DriverStation.getAlliance().orElse(Alliance.Blue) == Alliance.Red;
  return red ? p.getX() > 7.5 : p.getX() < 10.0;
}

private Command goToBestPieceOrSearch() {
  Pose2d pose = drive.getPose();
  var piece = pieceVision.getBestPiece(pose, this::allowedPiece);

  if (piece.isPresent()) {
    Translation2d p = piece.get().position;
    Rotation2d approachDir = p.minus(pose.getTranslation()).getAngle();
    Translation2d stopPoint = p.minus(new Translation2d(0.8, approachDir)); // 0.8 m short
    Pose2d approach = new Pose2d(stopPoint, approachDir.minus(kIntakeDirection));
    return AutoBuilder.pathfindToPose(approach, constraints, 1.0); // arrive at 1 m/s
  }

  if (searchIndex < searchWaypoints.size()) {
    Pose2d waypoint = searchWaypoints.get(searchIndex++);
    return AutoBuilder.pathfindToPose(waypoint, constraints)
        .until(pieceVision::hasConfirmedPiece)
        .andThen(scanInPlace());
  }
  return Commands.none(); // nothing left to search
}

private Command scanInPlace() {
  return Commands.run(() -> drive.runVelocity(new ChassisSpeeds(0, 0, 1.5)), drive)
      .until(pieceVision::hasConfirmedPiece)
      .withTimeout(1.5);
}

private Command oneCycle() {
  return Commands.sequence(
      Commands.defer(this::goToBestPieceOrSearch, Set.of(drive)), // decided when it starts
      visionPickup(),
      Commands.either(scoreCommand(), Commands.none(), intake::hasPiece));
}

public Command dynamicAuto(int cycles) {
  Command[] cycleCommands = new Command[cycles];
  for (int i = 0; i < cycles; i++) cycleCommands[i] = oneCycle();
  return Commands.sequence(
          Commands.runOnce(() -> searchIndex = 0),
          scorePreloadCommand(),
          Commands.sequence(cycleCommands))
      .withName("Dynamic Pickup Auto");
}
```

`scoreCommand()` and `scorePreloadCommand()` are your existing scoring routines. `Commands.defer` is
what makes the piece choice happen **when the cycle starts**, not when the robot boots (see
[Composition](../command-based/composition.md)).

## More integrations

- **Avoid contested pieces:** add a `robot` class to the model, track robots the same way, and skip
  pieces within about 0.7 m of an opponent in `allowedPiece`.
- **Held-piece check:** a second, cheap camera looking inside the intake with its own small model
  (`piece_ok` / `piece_misaligned`) can trigger an automatic eject-and-retry.

## Test it without a robot

### Unit tests

The projection and tracker are pure logic, so they get fast JUnit tests in `src/test/java`. Run them
with **WPILib: Test Robot Code** in VS Code, or `./gradlew test`. These tests catch sign mistakes
(is +yaw left or right?) that would otherwise send the robot the wrong way on the field. See
[Unit Testing](../testing/unit-testing.md) for testing the commands and autos too.

```java
package frc.robot.subsystems.gamepiece;

import static frc.robot.subsystems.gamepiece.GamePieceVisionConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;

class PieceVisionTest {
  @Test
  void centeredDetectionLandsWhereTheCameraPoints() {
    // Camera 0.25 m up, tilted 25 degrees down, 0.30 m forward. A piece centered in the image
    // (yaw 0, pitch 0) is where that center ray hits the piece-center height.
    double expectedX = 0.30 + (0.25 - kPieceCenterHeightM) / Math.tan(Math.toRadians(25.0));
    Translation2d p =
        PieceProjection.toField(0.0, 0.0, Pose2d.kZero, kRobotToCamera, kPieceCenterHeightM, 5.0)
            .orElseThrow();
    assertEquals(expectedX, p.getX(), 1e-9);
    assertEquals(0.0, p.getY(), 1e-9);
  }

  @Test
  void positiveYawIsToTheRight() {
    Translation2d p =
        PieceProjection.toField(10.0, 0.0, Pose2d.kZero, kRobotToCamera, kPieceCenterHeightM, 5.0)
            .orElseThrow();
    assertTrue(p.getY() < 0, "right of the robot is -y");
  }

  @Test
  void projectionRoundTrips() {
    Pose2d robot = new Pose2d(3.0, 2.0, Rotation2d.fromDegrees(30));
    Translation2d piece = new Translation2d(4.5, 2.6);
    double[] yp =
        PieceProjection.toYawPitch(piece, robot, kRobotToCamera, kPieceCenterHeightM).orElseThrow();
    Translation2d back =
        PieceProjection.toField(yp[0], yp[1], robot, kRobotToCamera, kPieceCenterHeightM, 5.0)
            .orElseThrow();
    assertEquals(0.0, back.getDistance(piece), 1e-9);
  }

  @Test
  void lookingAboveTheHorizonGivesNothing() {
    assertTrue(
        PieceProjection.toField(0.0, 40.0, Pose2d.kZero, kRobotToCamera, kPieceCenterHeightM, 5.0)
            .isEmpty());
  }

  @Test
  void trackerConfirmsAfterEnoughSightingsAndForgets() {
    PieceTracker tracker = new PieceTracker(0.35, 0.75, 3, 0.3);
    tracker.addObservation(new Translation2d(2.0, 1.0), 0.00, 0.9);
    tracker.addObservation(new Translation2d(2.05, 1.0), 0.02, 0.9);
    assertTrue(tracker.getConfirmed().isEmpty(), "2 sightings is not enough");
    tracker.addObservation(new Translation2d(2.02, 1.0), 0.04, 0.9);
    assertEquals(1, tracker.getConfirmed().size(), "close sightings merge into one piece");

    tracker.removeStale(0.5);
    assertEquals(1, tracker.getConfirmed().size());
    tracker.removeStale(1.0);
    assertTrue(tracker.getConfirmed().isEmpty(), "forgotten after the timeout");
  }

  @Test
  void bestPiecePrefersOneInFront() {
    PieceTracker tracker = new PieceTracker(0.35, 0.75, 1, 0.3);
    tracker.addObservation(new Translation2d(1.5, 0.0), 0, 0.9); // 1.5 m ahead
    tracker.addObservation(new Translation2d(-1.2, 0.0), 0, 0.9); // 1.2 m behind
    var best = tracker.getBest(Pose2d.kZero, Rotation2d.kZero, p -> true).orElseThrow();
    assertEquals(1.5, best.position.getX(), 1e-9);
    assertFalse(tracker.getBest(Pose2d.kZero, Rotation2d.kZero, p -> false).isPresent());
  }
}
```


### Simulation

Wire the sim IO in the container's `SIM` case, with a list of piece positions you control:

```java
List<Translation2d> simPieces =
    new ArrayList<>(List.of(new Translation2d(5.0, 4.0), new Translation2d(6.5, 2.5)));
pieceVision =
    new GamePieceVision(
        new GamePieceVisionIOSim(drive::getPose, () -> simPieces),
        drive::getPose,
        drive::getPoseAtTime);
```

In the intake's sim IO, set `hasPiece` when the intake is within about 0.25 m of a sim piece, and
remove that piece from the list. Then drive to pieces with the bindings above and run the autos in
simulation, watching `GamePieceVision/ConfirmedPieces` in AdvantageScope. (The 2026 code's `FuelSim`
already tracks game pieces and could supply the list instead.)
