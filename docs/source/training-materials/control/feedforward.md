# Feedforward

**Feedforward** predicts the voltage a mechanism needs from physics, instead of waiting for error.
A well-tuned feedforward does about 90% of the work; PID handles the rest.

## The gains

```text
voltage = kS * sign(velocity) + kV * velocity + kA * acceleration + kG (* cos(angle) for arms)
```

| Gain | Overcomes | Units (TalonFX, mechanism units) | Needed for |
|---|---|---|---|
| **kS** (static) | Friction: the voltage needed just to start moving | V | Everything |
| **kV** (velocity) | Back-EMF: voltage needed to hold a speed | V per (rot/s) | Flywheels, anything that moves fast |
| **kA** (acceleration) | Inertia: voltage needed to speed up | V per (rot/s²) | Mechanisms with fast profiles |
| **kG** (gravity) | Gravity | V | Arms (`Arm_Cosine`) and elevators (`Elevator_Static`) |

On the TalonFX, these go in the same slot as the PID gains:

```java
config.Slot0.kS = 0.12;
config.Slot0.kV = 0.115;
config.Slot0.kA = 0.0;
config.Slot0.kG = 0.35;
config.Slot0.GravityType = GravityTypeValue.Arm_Cosine;   // or Elevator_Static
```

For `Arm_Cosine`, **position 0 must be horizontal**. The TalonFX multiplies kG by
cos(position), so the gravity term is largest when the arm is level and zero when it's vertical.

## Estimating kV before you test

A motor at 12 V spins at roughly its free speed. So:

```text
kV ≈ 12 V / (free speed in mechanism rot/s)
```

A Kraken X60 spins at about 100 rot/s (6000 RPM) with no load. Behind a 2:1 reduction, the flywheel
free speed is about 50 rot/s, so kV ≈ 0.24 V/(rot/s). This is a good starting value; SysId refines it.

## Finding kG and kS by hand

1. Command a slowly increasing voltage (`VoltageOut`) with a `LoggedTunableNumber`.
2. **Elevator:** the voltage where it just starts going up is kG + kS. The voltage where it just starts
   drifting down is kG − kS. Solve for both.
3. **Arm:** do the same with the arm horizontal.
4. **Flywheel/roller:** the voltage where it just starts turning is kS.

## SysId: measuring feedforward properly

WPILib's **SysId** tool runs the mechanism through test motions and fits kS, kV, kA, and kG for you.

1. Add a `SysIdRoutine` to the subsystem that drives the IO's `setVoltage` method and logs position
   and velocity. The AdvantageKit template projects show how to wire it with AdvantageKit logging.
2. Run the four tests (quasistatic forward/back, dynamic forward/back) on the robot with plenty of
   room. **Have someone ready to disable.**
3. Load the log into SysId and read off the gains.
4. CTRE alternative: Phoenix 6's `SignalLogger` can record the data directly. See
   [CTRE's SysId guide](https://v6.docs.ctr-electronics.com/en/stable/docs/api-reference/wpilib-integration/sysid-integration/index.html) ([offline](../../reference-materials/offline/ctre-reference.md#sysid-with-phoenix-6)).

Docs: [WPILib SysId](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/system-identification/index.html) ([offline](../../reference-materials/offline/controls-reference.md#sysid-system-identification))
