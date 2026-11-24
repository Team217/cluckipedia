# Controls Reference

Local summary of the WPILib advanced-controls pages linked from [PID Explained](../../training-materials/control/pid.md),
[Feedforward](../../training-materials/control/feedforward.md), and [Odometry & Vision](../../training-materials/swerve/vision.md). The online
versions have interactive simulators; the walkthroughs below describe what you'd see in them, and
you can reproduce each one in our own simulation (see [Mechanism Simulation](../../training-materials/simulation/mechanisms.md)).

Sources: [Introduction to PID](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/introduction-to-pid.html),
[Advanced Controls introduction](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/index.html),
[Tuning a flywheel](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-flywheel.html),
[Tuning a vertical arm](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-vertical-arm.html),
[Tuning a turret](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-turret.html),
[SysId](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/system-identification/index.html),
[Pose estimators](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/state-space/state-space-pose-estimators.html).

## Vocabulary

| Term | Meaning |
|---|---|
| **Plant** | The thing being controlled (motor + mechanism) |
| **Setpoint / reference** | Where we want the plant to be |
| **Process variable / measurement** | Where the sensor says it is |
| **Error** | Setpoint minus measurement |
| **Control effort** | What the controller outputs (for us, volts) |
| **Step response** | How the system behaves when the setpoint suddenly jumps |
| **Rise time** | How long it takes to first get near the setpoint |
| **Overshoot** | How far it goes past the setpoint |
| **Settling time** | How long until it stays within tolerance |
| **Steady-state error** | Error that remains after everything settles |
| **Saturation** | The controller asks for more than 12 V; the output clips |

## The PID equation

```text
u(t) = kP·e(t) + kI·∫e(t)dt + kD·de(t)/dt
```

- **P** scales with the current error. On its own it leaves steady-state error whenever the plant
  needs a constant effort to stay put (gravity, friction, back-EMF), and it oscillates if set too high.
- **I** accumulates error over time and grows until the steady-state error disappears. It's slow,
  and it **winds up**: during long moves or saturation it builds a large value, which causes big
  overshoot. Use it sparingly, with an integrator limit (`setIntegratorRange`) or an i-zone
  (`setIZone`).
- **D** responds to how fast the error changes, opposing fast motion. It damps oscillation, but it
  amplifies sensor noise.

In practice, **feedforward does the bulk of the work and PID corrects what's left over**. This is
the main lesson of every WPILib tuning walkthrough.

## Walkthrough 1: flywheel (velocity control)

The setup: a flywheel spins up to a target speed. A game piece passing through slows it down
briefly. The goal is a fast spin-up and a fast recovery after each shot.

**Feedforward only (kV, plus kS).** Start with every gain at zero and raise kV until the
steady-state speed matches the setpoint. This alone gives a flywheel that reaches the right speed and
holds it, but it recovers slowly after a disturbance, because nothing reacts to error.

**Add kP.** A small kP makes the flywheel recover faster after the speed dips. Too much kP makes the
voltage chatter and can oscillate (worse with sensor delay). Stop raising kP once recovery is good
and the motor stays smooth.

**P only, no feedforward.** If you try kP alone, the flywheel always settles **below** the setpoint,
because a nonzero error is required to produce the voltage needed to hold speed. Raising kP shrinks
that error but brings oscillation. This is the demonstration of why feedforward matters.

Flywheels don't need kI or kD. A **bang-bang controller** (full power below the setpoint, off above
it) is another option for very high-inertia flywheels, but we rarely use it.

## Walkthrough 2: vertical arm (position control with gravity)

The setup: an arm rotates in a vertical plane, so gravity's pull changes with angle. It's strongest
when the arm is horizontal and zero when it's straight up or down.

**Gravity feedforward (kG · cos θ).** Raise kG until the arm holds still when horizontal without
help. With the cosine term, it then holds at any angle. This requires angle 0 to be horizontal.

**Add kV (and a motion profile).** Following a profile, kV supplies the voltage needed for the
profile's planned velocity. A trapezoidal profile (or Motion Magic) avoids slamming the arm with a
full-step error.

**Add kP, then a little kD if needed.** kP corrects the remaining tracking error. If the arm
overshoots or bounces at the end of a move, add a small kD.

**P only, no kG.** The arm sags below its setpoint, by different amounts at different angles. Adding
kI "fixes" the sag slowly and with overshoot. kG fixes it immediately. Use kG.

## Walkthrough 3: turret (position control without gravity)

The setup: a turret rotates in a horizontal plane, so gravity doesn't matter, but friction and
inertia do.

**Feedforward (kS, kV, kA) with a motion profile** gets the turret most of the way. kS overcomes
static friction; without it, small moves don't happen at all.

**kP, plus kD.** Because the turret has inertia and no gravity to settle against, it tends to
overshoot. kD damps that. Tune kP for a fast response, then kD to remove overshoot.

**Watch the limits.** Turrets usually can't spin forever (cables), so use soft limits and choose the
shortest *allowed* path to the target, not always the shortest angle.

## Motion profiles

A **motion profile** is a planned path for position and velocity over time that respects maximum
velocity and acceleration. Instead of jumping the setpoint, the controller follows the profile's
moving setpoint, so the error stays small throughout.

- **Trapezoidal profile:** accelerate at max acceleration, cruise at max velocity, decelerate. WPILib
  has `TrapezoidProfile` and `ProfiledPIDController`; CTRE's version on the motor is
  [Motion Magic](../../training-materials/control/motion-magic.md).
- **Exponential profile:** shaped from the motor's physics (kV, kA), so it never asks for more
  than the motor can do. WPILib's `ExponentialProfile`; CTRE's `MotionMagicExpo`.

## SysId (system identification)

SysId measures kS, kV, kA (and kG) by driving the mechanism with known voltages and fitting a model
to the results.

**The four tests:**

| Test | What happens | Measures mostly |
|---|---|---|
| Quasistatic forward | Voltage ramps up slowly (for example 1 V/s) | kS, kV (and kG) |
| Quasistatic reverse | Same, backwards | kS, kV (and kG) |
| Dynamic forward | A sudden constant step (for example 7 V) | kA |
| Dynamic reverse | Same, backwards | kA |

**Writing the routine (WPILib `SysIdRoutine` with AdvantageKit logging):**

```java
private final SysIdRoutine sysId =
    new SysIdRoutine(
        new SysIdRoutine.Config(
            null,                       // ramp rate (default 1 V/s)
            Volts.of(4),                // dynamic step voltage. Keep it modest for arms.
            null,                       // timeout (default 10 s)
            state -> Logger.recordOutput("Arm/SysIdState", state.toString())),
        new SysIdRoutine.Mechanism(
            voltage -> io.setVoltage(voltage.in(Volts)),
            null,                       // AdvantageKit already logs the data
            this));

public Command sysIdQuasistatic(SysIdRoutine.Direction direction) {
  return sysId.quasistatic(direction);
}

public Command sysIdDynamic(SysIdRoutine.Direction direction) {
  return sysId.dynamic(direction);
}
```

**Running it:**

1. Bind each of the four commands to a button (hold to run). Give the mechanism room, and have
   someone ready to disable. For arms and elevators, stop each test before hitting a hard stop.
2. Run all four tests, in any order.
3. Get the `.wpilog` file off the robot. The `SysIdState` output marks which test was running.
   CTRE users can log with `SignalLogger` instead (see
   [CTRE Reference](ctre-reference.md#sysid-with-phoenix-6)).
4. Open **SysId** (WPILib tools) and load the `.wpilog` directly. Pick the test-state entry
   (`Arm/SysIdState`), then the position, velocity, and voltage entries (from `Arm/positionRot`,
   `Arm/velocityRotPerSec`, `Arm/appliedVolts`), and set the units (rotations). Choose the
   mechanism type: Simple, Elevator, or Arm.
5. Read the gains. Check the fit: the **r²** value should be close to 1. Poor fits mean noisy data,
   tests that hit limits, or wrong units.

SysId can also suggest PID gains from the measured model. Treat them as starting points.

## Pose estimators

`SwerveDrivePoseEstimator` (and the differential and mecanum versions) combine:

- **Odometry**: smooth, high-rate, but drifts over time.
- **Vision**: noisy, delayed, but doesn't drift.

It's a **Kalman-filter-style** fusion. Each source has **standard deviations**: three numbers
(x meters, y meters, heading radians) describing how much it's expected to be wrong.

- Odometry standard deviations are set once in the constructor (small values, for example 0.1 m).
- Vision standard deviations are passed with each measurement:
  `addVisionMeasurement(pose, timestampSeconds, VecBuilder.fill(xStd, yStd, thetaStd))`.
  A common approach scales them with distance squared and divides by tag count. Set the heading
  standard deviation very high (or `Double.MAX_VALUE`) if the gyro is better than vision.
- **Smaller standard deviation means more trust.** If vision makes the pose jitter, raise its standard
  deviations. If the pose drifts away from truth and corrects slowly, lower them.
- The timestamp must be the **image capture time**. The estimator keeps a short buffer of past
  odometry and applies the vision correction at that moment, then replays forward.

## Going deeper: *Controls Engineering in FRC*

Source: [Controls Engineering in FRC (PDF)](https://file.tavsys.net/control/controls-engineering-in-frc.pdf),
by Tyler Veness, the main author of WPILib's controls libraries. Mentors: keep the PDF on the USB stick.
It's free to download and is the deep reference. What it covers, in order:

1. **PID and feedforward**, with the physics behind them.
2. **Modeling**: deriving equations for DC motors, elevators, flywheels, arms, and drivetrains.
3. **State-space control**: representing a system as matrices (state, input, output), and
   designing controllers (LQR) mathematically instead of by trial and error.
4. **Estimation**: Kalman filters, the math behind the pose estimator.
5. **Discretization**: turning continuous physics into 20 ms loop updates.
6. **Nonlinear control and motion profiling.**

Most students won't need chapters 3+ to write good robot code. Students interested in engineering
or math in college will find it rewarding. WPILib implements these ideas in `LinearSystem`,
`LinearQuadraticRegulator`, `KalmanFilter`, and `LinearSystemLoop`.
