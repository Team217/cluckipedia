# Motion Magic

`PositionVoltage` jumps the setpoint straight to the target, so the PID sees a huge error and
slams the mechanism. **Motion Magic** generates a smooth **motion profile** on the TalonFX: it
accelerates, cruises, and decelerates, while the PID and feedforward follow the moving profile.

```text
velocity
  ▲      ______________
  │     /              \        ← trapezoid: accelerate, cruise, decelerate
  │    /                \
  └───/──────────────────\────▶ time
```

## Configuration

```java
config.MotionMagic.MotionMagicCruiseVelocity = 1.0;   // mechanism rot/s
config.MotionMagic.MotionMagicAcceleration = 3.0;     // rot/s²
config.MotionMagic.MotionMagicJerk = 30.0;            // rot/s³ (optional; smooths the corners)

motor.setControl(new MotionMagicVoltage(0.0).withPosition(target));
```

Because the profile tells the TalonFX the velocity and acceleration it *should* have at every
moment, **kV and kA feedforward work during the move**, not just kG/kS. That's why a well-tuned
Motion Magic mechanism tracks its profile very closely.

| Variant | Use |
|---|---|
| `MotionMagicVoltage` | Arms, elevators, turrets (the default choice) |
| `MotionMagicVelocityVoltage` | Flywheels that should ramp smoothly |
| `MotionMagicExpoVoltage` | Uses kV/kA to make a profile the motor can actually follow. Good when you don't know safe limits. |
| `DynamicMotionMagicVoltage` | Changes cruise/accel limits per request (for example slower when carrying a game piece) |

## Choosing the limits

1. Start slow: cruise velocity at about 25% of free speed, acceleration so it reaches cruise in
   about 0.25 s.
2. Tune kS/kG/kV/kP until position follows the **profile reference**
   (`ClosedLoopReference`) tightly.
3. Raise the limits until the mechanism can't keep up (the error grows during the move), or until
   it's as fast as the mechanical team says is safe. Then back off 10–20%.

## What to graph

In AdvantageScope, plot together:

- `closedLoopReferenceRot`: where the profile says we should be right now
- `positionRot`: where we are
- `GoalRot`: the final target

Position lagging the reference during the move means kV/kA are too low or the limits are too
aggressive. Overshooting the goal at the end means kP is too high or kD too low.

Docs: [CTRE Motion Magic](https://v6.docs.ctr-electronics.com/en/stable/docs/api-reference/device-specific/talonfx/motion-magic.html) ([offline](../../reference-materials/offline/ctre-reference.md#motion-magic-details))
