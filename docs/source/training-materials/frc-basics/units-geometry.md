# Units, Geometry & Coordinates

Unit mistakes are the most common source of "it compiled, but the robot did something crazy."

## Our unit rules

1. **SI units in code**: meters, seconds, radians (or rotations, see below), kilograms, volts, amps.
2. **Rotations for CTRE motors.** Phoenix 6 reports positions in rotations and velocities in
   rotations per second, so mechanism code using TalonFX usually stays in rotations.
3. **Put the unit in the name**: `heightMeters`, `angleRad`, `velocityRotPerSec`. No bare `angle`.
4. **Convert at the edges.** Accept inches or degrees from CAD or drawings, convert once in
   `Constants`, and use SI everywhere else.

```java
import edu.wpi.first.math.util.Units;

public static final double kArmLengthMeters = Units.inchesToMeters(18.5);
public static final double kStowAngleRad = Units.degreesToRadians(95.0);
```

WPILib also has a typed units library (`edu.wpi.first.units.Units`), where values like
`Meters.of(1.2)` carry their unit. Phoenix 6 uses it for status signals (`StatusSignal<Angle>`).
Either style is fine; just don't mix units in one calculation.

## Motor rotations vs. mechanism rotations

```text
mechanismRot = motorRot / gearRatio
```

Two kinds of bugs come from this:

- Forgetting the gear ratio: the arm overshoots by a factor of the ratio.
- Applying it twice: once in `SensorToMechanismRatio` and again in your code.

**Decide in one place** whether a value is in motor or mechanism units. The 2026 `PivotIO` writes
"All positions are in motor rotations" in its Javadoc. Do that too.

## Field coordinates

WPILib uses the **blue alliance origin** convention:

- Origin `(0, 0)` is the corner of the field on the **blue alliance wall**, on the right side as
  seen from the blue driver station.
- **+X** points away from the blue alliance wall, toward the red alliance.
- **+Y** points to the left (as seen from the blue driver station).
- **Angles are counterclockwise-positive**, with 0 pointing along +X.

On the red alliance, our code **flips** poses so autos work from either side. See
`AllianceFlipUtil` in the 2026 code, and PathPlanner's built-in flipping.

## Robot coordinates

- **+X** is the robot's front, **+Y** is its left, and **+Z** is up.
- Rotation is counterclockwise-positive when viewed from above.

## Geometry classes

| Class | Meaning |
|---|---|
| `Translation2d` | An (x, y) point or vector, in meters |
| `Rotation2d` | An angle. Use `Rotation2d.fromDegrees(90)` and `.getRadians()` |
| `Pose2d` | Translation + rotation: where the robot is and which way it faces |
| `Transform2d` | A change in pose, for example camera position relative to robot center |
| `Pose3d`, `Transform3d` | 3D versions, used for cameras and game pieces |
| `ChassisSpeeds` | Robot velocity: vx, vy (m/s) and omega (rad/s) |

```java
Pose2d robot = new Pose2d(3.0, 4.0, Rotation2d.fromDegrees(90));
Translation2d target = new Translation2d(8.0, 4.0);
double distance = robot.getTranslation().getDistance(target); // 5.0 m
Rotation2d angleToTarget = target.minus(robot.getTranslation()).getAngle(); // 0°
```

## 2027 changes

- Geometry and units classes move from `edu.wpi.first.math...` / `edu.wpi.first.units...` to
  packages under **`org.wpilib`**. The classes and ideas are the same.
- **`Rotation2d` getters return wrapped angles** (within one turn). If you need to know a turret is
  at 200° rather than −160°, keep the angle as a `double` or `Angle`.
- **Mutable units are removed.** Use the normal immutable ones: `Meters.of(1.2)`, `Degrees.of(90)`.
- Enum values and constants in WPILib are **ALL_CAPS** (for example `Alliance.RED`).
