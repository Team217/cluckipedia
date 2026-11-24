# Gyros & IMUs

A **gyro** measures rotation rate; integrating it gives the robot's **heading** (yaw). An **IMU**
(inertial measurement unit) combines a gyro with an accelerometer (and sometimes a magnetometer) and
also reports pitch, roll, and acceleration.

## How they work

- A **MEMS gyroscope** is a tiny vibrating structure on a chip. When the chip rotates, the Coriolis
  effect pushes on the vibration, and the chip measures that push as a rotation rate.
- The IMU's processor **integrates** rate over time into an angle and fuses it with the
  accelerometer to correct pitch and roll.
- **Drift:** small errors in the rate add up, so yaw slowly wanders (good FRC IMUs drift well under
  a degree per minute). Vision corrections in the pose estimator keep it in check.
- **Calibration:** most IMUs measure their bias while still at startup. **Don't move the robot while
  it boots**, or the heading will drift badly.

## When to use it

- **Field-relative driving and odometry:** the most important use. Swerve odometry needs an
  accurate heading.
- **Turning to an angle** (auto-aim, snapping to a heading).
- **Tilt detection:** pitch and roll for detecting tipping or a robot climbing onto something.

Mount the IMU **flat, rigidly, near the robot's center**, away from motors' magnetic fields if it uses
a magnetometer. Record its orientation in code.

## Conventions

WPILib and our code use **counterclockwise-positive** yaw (viewed from above). Some sensors report
clockwise-positive. **Always use the sensor's `getRotation2d()` (or equivalent)**, which converts to
WPILib's convention, instead of a raw angle getter.

## CTRE: Pigeon 2 (preferred)

```java
Pigeon2 pigeon = new Pigeon2(SwerveConstants.kPigeonId, bus);
Pigeon2Configuration cfg = new Pigeon2Configuration();
cfg.MountPose.MountPoseYaw = 0.0;     // degrees; set if mounted rotated
cfg.MountPose.MountPosePitch = 0.0;
cfg.MountPose.MountPoseRoll = 0.0;
pigeon.getConfigurator().apply(cfg);

StatusSignal<Angle> yaw = pigeon.getYaw();                          // CCW+, continuous (doesn't wrap)
StatusSignal<AngularVelocity> yawRate = pigeon.getAngularVelocityZWorld();
// In updateInputs: BaseStatusSignal.refreshAll(yaw, yawRate) ...
Rotation2d heading = pigeon.getRotation2d();
pigeon.setYaw(0.0);                                                 // re-zero
```

- On a CANivore (or a SystemCore CAN FD bus), the swerve odometry thread reads yaw at high rate and
  time-synchronized with the modules. The CTRE and AdvantageKit swerve code does this for you.
- **Mount pose** calibration is in Tuner X. It fixes a gyro mounted at an angle.
- **Sim:** `pigeon.getSimState().setRawYaw(degrees)`. The CTRE swerve sim sets it automatically.

## Studica: navX

```java
AHRS navx = new AHRS(AHRS.NavXComType.kMXP_SPI);   // or kUSB1, kI2C, ...
Rotation2d heading = navx.getRotation2d();          // converted to CCW+
double rateDegPerSec = navx.getRate();
boolean ok = navx.isConnected() && !navx.isCalibrating();
navx.zeroYaw();
```

- `getAngle()` and `getYaw()` are **clockwise-positive**. Use `getRotation2d()`.
- The classic navX plugs into the roboRIO's **MXP** port. **SystemCore has no MXP or SPI**, so on
  2027 robots a navX needs a USB connection or a newer CAN-based Studica product. Check Studica's
  2027 support.

## Redux: Canandgyro

```java
Canandgyro gyro = new Canandgyro(SwerveConstants.kCanandgyroId);
Rotation2d heading = gyro.getRotation2d();
boolean ok = gyro.isConnected() && !gyro.isCalibrating();
gyro.setYaw(0.0);
```

ReduxLib generally reports angles in **rotations**. Check the units before using raw getters like
`getYaw()` or `getMultiturnYaw()`.

## SystemCore built-in IMU (2027)

SystemCore includes an IMU that WPILib 2027 can read (with simulation support). It's convenient
for small robots and test benches, but mounting is fixed to wherever the controller sits, so a
Pigeon 2 placed flat near the center remains our choice for swerve.

## Using the gyro in code

- Put the gyro in its own `GyroIO` (`GyroIOPigeon2`, `GyroIOSim`). The AdvantageKit swerve template
  already does this.
- Log `connected`, `yawPosition`, and `yawVelocity`.
- **If the gyro disconnects**, the swerve template falls back to estimating heading from the wheel
  movement. Make sure drivers get an alert.
- Reset heading with a driver button (point the robot away from you, press, done). Better: reset
  the **pose** from vision, which fixes heading too.
