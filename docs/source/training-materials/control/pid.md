# PID Explained

**Closed-loop control** means measuring where a mechanism is and correcting the output to get it
where you want it. **PID** is the most common way to do that.

## The idea

```text
error = setpoint - measurement
output = kP * error  +  kI * (sum of error over time)  +  kD * (rate of change of error)
```

| Term | Reacts to | Effect | Too much |
|---|---|---|---|
| **P** (proportional) | How far off we are **now** | Pushes toward the target, harder when farther | Overshoot and oscillation |
| **I** (integral) | How long we've been off | Removes small, steady errors | Slow, wobbly overshoot ("windup") |
| **D** (derivative) | How fast the error is changing | Brakes as we approach; damps oscillation | Jittery, noisy, buzzing motor |

A useful picture is a **spring and a shock absorber**: P is the spring pulling the mechanism to the
target, and D is the shock absorber that keeps it from bouncing.

## Units matter

On a TalonFX with `PositionVoltage`, kP is in **volts per rotation of error**. With
`SensorToMechanismRatio` set, that means mechanism rotations. So `kP = 40` means "apply 40 V per
full rotation of error," which is 0.4 V for a 0.01-rotation (3.6°) error. Always know your gains'
units, because it's the only way to sanity-check them.

With `VelocityVoltage`, kP is in **volts per (rotation per second) of error**.

## Why PID alone isn't enough

PID only reacts to error. It has to *be wrong* before it does anything. For an arm fighting gravity,
the motor needs some voltage just to hold still, which means plain PID settles with a constant
error (sag). For a flywheel, holding speed needs constant voltage, so P alone always runs a bit slow.

The fix is **feedforward**: predict the voltage the mechanism needs and add it, so PID only cleans up
small errors. **Do feedforward first, then PID.** See [Feedforward](feedforward.md).

## Where PID runs

| Where | Rate | When to use |
|---|---|---|
| **On the TalonFX** (`Slot0` gains + `PositionVoltage`/`VelocityVoltage`/`MotionMagic`) | 1000 Hz | Almost always for single-motor mechanisms |
| On the roboRIO (WPILib `PIDController`, `ProfiledPIDController`) | 50 Hz | Control from a sensor the motor can't see (for example robot heading from vision, or driving to a pose) |

## Build intuition first

Before touching robot code, use WPILib's interactive tuning demos. They simulate a flywheel and an
arm and let you change gains with sliders. Without internet, use the written walkthroughs
(the **offline** links) and reproduce them in our own simulation:

- [Tuning a flywheel velocity controller](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-flywheel.html) ([offline](../../reference-materials/offline/controls-reference.md#walkthrough-1-flywheel-velocity-control))
- [Tuning a vertical arm position controller](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-vertical-arm.html) ([offline](../../reference-materials/offline/controls-reference.md#walkthrough-2-vertical-arm-position-control-with-gravity))
- [Tuning a turret position controller](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-turret.html) ([offline](../../reference-materials/offline/controls-reference.md#walkthrough-3-turret-position-control-without-gravity))
- [Introduction to PID](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/introduction-to-pid.html) ([offline](../../reference-materials/offline/controls-reference.md#the-pid-equation))

**Exercise:** In each demo, first try P only, then feedforward only, then both. Write down what you
saw each time.
