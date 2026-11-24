# REV Motors (SPARK MAX & SPARK Flex)

:::{admonition} CTRE first
:class: important
Our default hardware is CTRE (Kraken/TalonFX). Use REV only when a mechanism already has REV
hardware or a part needs it (for example a NEO 550 on a small roller). Everything else in this wiki,
including the IO layer, simulation, and tuning, applies to REV the same way. Only the IO
implementation changes.
:::

The 2026 robot still has REV hardware (see `FlywheelIOSparkFlex`, `TurretIOSparkMax`, and the
REV-based intake classes), so you need to be able to read and write REVLib code.

Code on this page matches **REVLib 2026** (`2026.0.5`, the version in our `vendordeps/`).

## The hardware

| Device | What it is | Notes |
|---|---|---|
| **NEO** | Brushless motor (~5,600 RPM free) | Needs a SPARK controller |
| **NEO Vortex** | Brushless motor (~6,700 RPM free) | Plugs directly into a **SPARK Flex** (dock style) |
| **NEO 550** | Small brushless motor (~11,000 RPM) | Overheats fast. **Always** use a low current limit (20–30 A). |
| **SPARK MAX** | Motor controller (brushless or brushed) | Encoder/limit/analog inputs on its **data port** |
| **SPARK Flex** | Motor controller, docks onto a Vortex | Separate external encoder port |
| **Through Bore Encoder** | Absolute + relative encoder | Wires to a SPARK data port or to roboRIO DIO. See [Encoders](../sensors/encoders.md). |
| **Servo Hub** | Controls up to 6 servos over CAN | The 2026 hood uses it (`HoodIOServoHub`) |

Unlike a Kraken, the motor and controller are **separate devices** (except SPARK Flex + Vortex,
which dock together). The controller reads the motor's built-in hall encoder.

## REV Hardware Client

The REV equivalent of Tuner X (Windows app). Connect to a SPARK by **USB-C**, or through one
SPARK to the whole CAN bus.

- **Update firmware** on every SPARK at the start of the season.
- **Set the CAN ID.** Every SPARK ships with ID 0, so set it before putting it on the bus. Two
  devices with the same ID cause both to behave strangely.
- **Set motor type** (brushless for NEO / Vortex / NEO 550). Running a brushless motor in brushed
  mode can damage it.
- **Run and plot** the motor for bench tests.
- As with CTRE, **code applies the real configuration at boot**. Don't rely on settings saved from
  the Hardware Client.

## Configuration

REVLib uses a **config object** that you build and then apply with `configure(...)`:

```java
import com.revrobotics.PersistMode;
import com.revrobotics.ResetMode;
import com.revrobotics.spark.FeedbackSensor;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;

private final SparkMax motor = new SparkMax(ArmConstants.kMotorId, MotorType.kBrushless);

public ArmIOSparkMax() {
  SparkMaxConfig config = new SparkMaxConfig();
  config.inverted(false);
  config.idleMode(IdleMode.kBrake);
  config.smartCurrentLimit(40);           // amps; NEO 550: 20–30
  config.voltageCompensation(12.0);       // consistent behavior as the battery sags

  // Encoder units: convert motor rotations to ARM rotations
  config.encoder
      .positionConversionFactor(1.0 / ArmConstants.kGearRatio)
      .velocityConversionFactor(1.0 / ArmConstants.kGearRatio / 60.0); // RPM → arm rot/s

  // Closed loop runs on the SPARK at 1 kHz
  config.closedLoop
      .feedbackSensor(FeedbackSensor.kPrimaryEncoder)
      .pid(ArmConstants.kP, 0.0, ArmConstants.kD)
      .outputRange(-1, 1);
  config.closedLoop.feedForward.kS(ArmConstants.kS).kV(ArmConstants.kV).kCos(ArmConstants.kG);
  config.closedLoop.maxMotion
      .cruiseVelocity(ArmConstants.kCruiseVelocityRotPerSec)
      .maxAcceleration(ArmConstants.kAccelerationRotPerSec2)
      .allowedProfileError(ArmConstants.kToleranceRot);

  config.softLimit
      .forwardSoftLimit(ArmConstants.kMaxPositionRot).forwardSoftLimitEnabled(true)
      .reverseSoftLimit(ArmConstants.kMinPositionRot).reverseSoftLimitEnabled(true);

  // Signal rates (like Phoenix update frequencies)
  config.signals.primaryEncoderPositionPeriodMs(20).primaryEncoderVelocityPeriodMs(20);

  HardwareHelper.configMotor(motor, config);   // retries, alerts on failure
}
```

### Reset and persist modes

`configure(config, ResetMode, PersistMode)` takes two options:

| Option | Meaning | Use |
|---|---|---|
| `ResetMode.kResetSafeParameters` | Reset everything to defaults first, then apply | **At startup.** Makes code the single source of truth. |
| `ResetMode.kNoResetSafeParameters` | Only change what's in this config | Runtime changes (new PID gains, brake/coast) |
| `PersistMode.kPersistParameters` | Save to flash so it survives power cycles | At startup only. Flash writes are slow and wear out. |
| `PersistMode.kNoPersistParameters` | Don't save | **Every runtime change** |

`HardwareHelper.configMotor` uses reset + persist at startup. For live tuning, build a small config
with only the changed values and apply it with `kNoResetSafeParameters, kNoPersistParameters`. The
2026 `TurretIOSparkMax.setPIDFF` does exactly this.

### Feedforward in REVLib 2026

REVLib 2026 puts feedforward on the SPARK: `closedLoop.feedForward.kS(...)`, `kV(...)`, `kA(...)`,
`kG(...)` (constant, for elevators), and `kCos(...)` (cosine, for arms, with `kCosRatio` to convert
the encoder position into a mechanism angle). There are shortcuts like `sva(kS, kV, kA)`. These are
in **volts**, so they behave like the CTRE gains.

## Commanding a SPARK

```java
private final SparkClosedLoopController controller = motor.getClosedLoopController();

motor.setVoltage(6.0);                                                // open loop
controller.setSetpoint(rpm, ControlType.kVelocity);                   // velocity PID
controller.setSetpoint(positionRot, ControlType.kPosition);           // position PID
controller.setSetpoint(positionRot, ControlType.kMAXMotionPositionControl); // profiled, like Motion Magic
```

**MAXMotion** is REV's on-controller motion profiling: trapezoidal, configured with `cruiseVelocity`
and `maxAcceleration`, in your conversion-factor units.

**Followers:** `followerConfig.follow(leadMotor, true)` (the `true` inverts the follower). Apply it
once at startup.

## Reading a SPARK

REVLib getters return plain `double`s. There's no `refreshAll`; each value comes from the latest
periodic CAN frame. Signal rates are set with `config.signals`.

```java
private final RelativeEncoder encoder = motor.getEncoder();

@Override
public void updateInputs(ArmIOInputs inputs) {
  inputs.positionRot = encoder.getPosition();          // conversion factor applied
  inputs.velocityRotPerSec = encoder.getVelocity();
  inputs.appliedVolts = motor.getAppliedOutput() * motor.getBusVoltage();
  inputs.statorCurrentAmps = motor.getOutputCurrent();
  inputs.tempCelsius = motor.getMotorTemperature();
  inputs.closedLoopReferenceRot = controller.getSetpoint();
  inputs.connected = motor.getLastError() == REVLibError.kOk;  // see note below
}
```

:::{note}
REVLib has no single "connected" flag like Phoenix's `refreshAll(...).isOK()`. The example checks
`getLastError()` after reading. Log `motor.hasActiveFault()` as a separate input too, since a
connected SPARK can still be faulted. Use the same approach in every REV IO class. Note that the 2026 `TurretIOSparkMax` just sets
`connected = true`, which hides disconnects.
:::

## Data port and attached sensors

The SPARK's data port can read sensors directly, so the closed loop can use them:

| Sensor | Access | Use as feedback |
|---|---|---|
| Built-in motor encoder | `motor.getEncoder()` | `FeedbackSensor.kPrimaryEncoder` |
| Absolute encoder (Through Bore) | `motor.getAbsoluteEncoder()`; configure with `config.absoluteEncoder...` (`zeroOffset`, `inverted`, conversion factors) | `FeedbackSensor.kAbsoluteEncoder` |
| Limit switches | `motor.getForwardLimitSwitch().isPressed()`; configure with `config.limitSwitch...` | Stops the motor directly, with no code in the loop |
| Analog sensor | `motor.getAnalog().getVoltage()` | `FeedbackSensor.kAnalogSensor` |

On a SPARK MAX, an absolute encoder and limit switches share the data port. Use REV's adapter
boards, and call `setSparkMaxDataPortConfig()` on the absolute encoder or limit switch config.

## Simulation

REVLib includes simulation classes that work like Phoenix 6's sim state:

```java
public class ArmIOSparkMaxSim extends ArmIOSparkMax {
  private final DCMotor gearbox = DCMotor.getNEO(1);
  private final SparkMaxSim sparkSim = new SparkMaxSim(motor, gearbox);   // motor is protected in the parent
  private final SingleJointedArmSim armSim = /* same as ArmIOSim */;

  @Override
  public void updateInputs(ArmIOInputs inputs) {
    armSim.setInputVoltage(sparkSim.getAppliedOutput() * RoboRioSim.getVInVoltage());
    armSim.update(0.02);
    double armRotPerSec = Units.radiansToRotations(armSim.getVelocityRadPerSec());
    // iterate(velocity in conversion-factor units, bus voltage, dt): runs the SPARK's simulated firmware
    sparkSim.iterate(armRotPerSec, RoboRioSim.getVInVoltage(), 0.02);
    super.updateInputs(inputs);
  }
}
```

`iterate` steps the simulated SPARK, including its closed loop, given the mechanism's velocity. Use
`SparkFlexSim` for a SPARK Flex. `sparkSim.getRelativeEncoderSim()`,
`getAbsoluteEncoderSim()`, and `getForwardLimitSwitchSim()` let you set sensor values directly.

## REV vs. CTRE quick comparison

| | CTRE Phoenix 6 | REVLib |
|---|---|---|
| Config | `TalonFXConfiguration` groups, `getConfigurator().apply()` | `SparkMaxConfig` builder, `configure(config, reset, persist)` |
| Units | Rotations, rotations/s; `SensorToMechanismRatio` | Rotations and **RPM** by default; conversion factors |
| Commands | Control request objects | `setSetpoint(value, ControlType)` |
| Profiling | Motion Magic | MAXMotion |
| Reading | `StatusSignal` + `refreshAll` | Plain getters; rates via `config.signals` |
| Connected check | `refreshAll(...).isOK()` | `getLastError()` / faults |
| Sim | `TalonFXSimState` + physics model | `SparkMaxSim` / `SparkFlexSim` + physics model |
| Setup tool | Tuner X | REV Hardware Client |

## 2027

REVLib has 2027 alpha builds for SystemCore (`v2027.0.0-alpha-7` for WPILib alpha 7/8 as of
November 2026). Expect the same structure with the `org.wpilib` package changes on the WPILib side.
