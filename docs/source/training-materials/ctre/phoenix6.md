# Phoenix 6 Basics

**Phoenix 6** is CTRE's library for TalonFX (Kraken) motor controllers, CANcoders, the Pigeon 2, and
the CANivore. It's the core of our hardware code going forward.

Official docs: [v6.docs.ctr-electronics.com](https://v6.docs.ctr-electronics.com/en/stable/) ([offline](../../reference-materials/offline/ctre-reference.md))

## The three things you do with a device

1. **Configure** it once at startup (configs).
2. **Command** it every loop (control requests).
3. **Read** it every loop (status signals).

## 1. Configuration

```java
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

// The bus name must match the CANivore's name in Tuner X ("Canivore" on the 2026 robot).
// Leave the bus out to use the roboRIO's built-in CAN bus.
private final TalonFX motor = new TalonFX(20, new CANBus(SystemConstants.kCANivoreBus));

public ArmIOTalonFX() {
  TalonFXConfiguration config = new TalonFXConfiguration();

  // Direction and idle behavior
  config.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
  config.MotorOutput.NeutralMode = NeutralModeValue.Brake;

  // Current limits: always set these
  config.CurrentLimits.StatorCurrentLimit = 60.0;
  config.CurrentLimits.StatorCurrentLimitEnable = true;
  config.CurrentLimits.SupplyCurrentLimit = 40.0;
  config.CurrentLimits.SupplyCurrentLimitEnable = true;

  // Report mechanism rotations instead of rotor rotations
  config.Feedback.SensorToMechanismRatio = 48.0;

  // Closed-loop gains (see the Control section)
  config.Slot0.kP = 30.0;
  config.Slot0.kD = 0.5;
  config.Slot0.kS = 0.15;
  config.Slot0.kG = 0.4;
  config.Slot0.GravityType = GravityTypeValue.Arm_Cosine;

  // Soft limits keep the mechanism off its hard stops
  config.SoftwareLimitSwitch.ForwardSoftLimitThreshold = 0.30; // mechanism rotations
  config.SoftwareLimitSwitch.ForwardSoftLimitEnable = true;
  config.SoftwareLimitSwitch.ReverseSoftLimitThreshold = 0.0;
  config.SoftwareLimitSwitch.ReverseSoftLimitEnable = true;

  // Apply, retrying a few times in case the CAN bus is busy at boot
  HardwareHelper.configMotor(motor, config);
}
```

:::{admonition} Apply the whole config
:class: tip
Applying a full `TalonFXConfiguration` **resets every setting you didn't set** to its default.
This is good: the robot's behavior is defined entirely by code, not by whatever someone last
clicked in Tuner X. To change just one group at runtime (for example new PID gains), apply only
that group: `motor.getConfigurator().apply(config.Slot0)`.
:::

The 2026 code's `HardwareHelper.configMotor(TalonFX, TalonFXConfiguration)` retries the apply and
raises a dashboard alert if it fails. Keep using a helper like that.

## 2. Control requests

You tell a TalonFX what to do by sending a **control request** object. Create the request once and
update it each loop. Don't create a new one every loop.

```java
private final VoltageOut voltageRequest = new VoltageOut(0.0);
private final VelocityVoltage velocityRequest = new VelocityVoltage(0.0).withSlot(0);
private final PositionVoltage positionRequest = new PositionVoltage(0.0).withSlot(0);
private final MotionMagicVoltage motionMagicRequest = new MotionMagicVoltage(0.0);
private final NeutralOut neutralRequest = new NeutralOut();

motor.setControl(voltageRequest.withOutput(6.0));               // open loop, 6 V
motor.setControl(velocityRequest.withVelocity(50.0));           // 50 rot/s, closed loop
motor.setControl(positionRequest.withPosition(0.25));           // 0.25 rot, closed loop
motor.setControl(motionMagicRequest.withPosition(0.25));        // profiled move to 0.25 rot
motor.setControl(neutralRequest);                               // coast/brake per NeutralMode
```

| Request | Use for |
|---|---|
| `VoltageOut` | Rollers, simple open-loop mechanisms, SysId |
| `DutyCycleOut` | Like `VoltageOut` but as a fraction of battery voltage. Prefer volts, which stay consistent as the battery drains. |
| `VelocityVoltage` | Flywheels, rollers that need a consistent speed |
| `PositionVoltage` | Simple position holds |
| `MotionMagicVoltage` | Arms, elevators, turrets: smooth, profiled moves ([Motion Magic](../control/motion-magic.md)) |
| `Follower` | A second motor that copies a leader |
| `TorqueCurrentFOC` variants | Advanced torque control. Requires Phoenix Pro. |

**The PID loop runs on the TalonFX itself at 1 kHz**, much faster than our 50 Hz robot loop. That's
why we prefer on-motor control to running PID on the roboRIO.

## 3. Status signals

Every value you can read is a `StatusSignal`. Grab the signal objects once, then refresh them all
together each loop:

```java
private final StatusSignal<Angle> position = motor.getPosition();
private final StatusSignal<AngularVelocity> velocity = motor.getVelocity();
private final StatusSignal<Voltage> appliedVolts = motor.getMotorVoltage();
private final StatusSignal<Current> statorCurrent = motor.getStatorCurrent();

// In the constructor, after configuring:
BaseStatusSignal.setUpdateFrequencyForAll(50.0, position, velocity, appliedVolts, statorCurrent);
motor.optimizeBusUtilization(); // stop sending signals we didn't ask for

// Every loop:
boolean connected =
    BaseStatusSignal.refreshAll(position, velocity, appliedVolts, statorCurrent).isOK();
double positionRot = position.getValueAsDouble();
```

`refreshAll(...).isOK()` doubles as a **"device is connected"** check. Log it, and alert the drivers
if it goes false.

This is exactly what the 2026 `PivotIOTalonFX.updateInputs()` does. Read it now.

## CANcoder and Pigeon 2

```java
CANcoder cancoder = new CANcoder(30, bus);
CANcoderConfiguration ccConfig = new CANcoderConfiguration();
ccConfig.MagnetSensor.MagnetOffset = -0.137; // set so "zero" is where you want it (rotations)
ccConfig.MagnetSensor.SensorDirection = SensorDirectionValue.CounterClockwise_Positive;
cancoder.getConfigurator().apply(ccConfig);

// Use the CANcoder as the TalonFX's feedback sensor:
config.Feedback.FeedbackRemoteSensorID = cancoder.getDeviceID();
config.Feedback.FeedbackSensorSource = FeedbackSensorSourceValue.FusedCANcoder; // Pro
// or RemoteCANcoder without Pro
config.Feedback.RotorToSensorRatio = 48.0;  // motor rotations per CANcoder rotation
config.Feedback.SensorToMechanismRatio = 1.0;

Pigeon2 pigeon = new Pigeon2(1, bus);
StatusSignal<Angle> yaw = pigeon.getYaw();
```

## Other CTRE sensors

**CANrange** (distance/proximity) and **CANdi** (digital inputs and PWM/quadrature encoders on CAN)
follow the same configure / signal pattern. Both can act as hardware limits for a TalonFX. See
[Limit, Presence & Homing](../sensors/presence.md) and [Distance & Color](../sensors/distance-color.md).

## Phoenix Pro

Some features (FOC commutation for about 15% more power, `FusedCANcoder`, torque-current control)
need a **Phoenix Pro** license, either on the device or on the CANivore. Check with a mentor which
devices are licensed before relying on these features.

## 2027 changes

- Phoenix 6 has 2027 alpha builds for SystemCore. The ideas on this page (configs, control
  requests, status signals) don't change.
- **SystemCore has five CAN FD buses** built in, so a CANivore is no longer the only way to get
  CAN FD. Ask a mentor how devices will be split across buses. Bus names, and how a `CANBus` refers
  to a SystemCore bus, come from CTRE's 2027 docs; check them when the 2027 release is out.
- WPILib's own CAN classes take a `CANPort` enum in 2027. That's WPILib, not Phoenix.

More: [What's New for 2027](../season-2027/whats-new.md#our-libraries-in-2027).
