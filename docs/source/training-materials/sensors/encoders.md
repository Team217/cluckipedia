# Encoders

An **encoder** measures rotation: how far something has turned (**position**) and how fast
(**velocity**). It's the most important sensor on the robot.

## How they work

| Type | How it works | Knows position at power-on? |
|---|---|---|
| **Hall-effect motor encoder** (built into brushless motors) | Hall sensors detect the rotor's magnets as it spins and count steps | No: starts at 0 (relative) |
| **Magnetic absolute encoder** (CANcoder, Through Bore absolute, Canandmag) | A chip reads the angle of a diametric magnet on the shaft | **Yes**, within one rotation |
| **Quadrature encoder** (Through Bore relative output, older optical encoders) | Two square-wave channels (A/B) 90° apart; the order of edges gives direction | No (relative) |
| **Potentiometer** | A variable resistor; the voltage changes with angle | Yes, but noisy and limited to a few turns |

**Relative vs. absolute:**

- A **relative** encoder counts changes from wherever it started. It needs a known start position
  (a hard stop at boot, or a homing routine).
- An **absolute** encoder always knows its angle within one rotation. Mount it where the
  mechanism moves **less than one rotation** of the sensor (usually directly on the arm's or
  turret's output shaft), or it can't tell turns apart.

**Where to mount it:**

- **On the motor** (built-in): high resolution, but backlash in the gearbox hides small motions of the
  mechanism.
- **On the mechanism output** (CANcoder on the arm shaft): measures what you actually care about,
  absolute, and not fooled by backlash. Combine both with CTRE's Fused/Sync CANcoder (Phoenix Pro)
  or a remote sensor setting.

**Resolution and speed:** absolute magnetic encoders have plenty of resolution for FRC. The
limiting factors are usually **update rate** and **noise in velocity**, which is why shooter
velocity comes from the motor's own encoder.

## When to use what

| Mechanism | Recommended |
|---|---|
| Flywheel, roller, drivetrain wheel | Motor's built-in encoder |
| Elevator | Motor encoder + homing (hard stop or limit switch at the bottom) |
| Arm, wrist, pivot | **CANcoder on the pivot shaft** (absolute), fused with the motor |
| Turret | Absolute encoder on the turret ring gear or output. If the turret turns more than one sensor rotation, use two encoders with different ratios, or home it. |
| Swerve steering | CANcoder on each module (required by the Tuner X generator) |

## CTRE: CANcoder (preferred)

```java
CANcoder cancoder = new CANcoder(ArmConstants.kCancoderId, bus);
CANcoderConfiguration cc = new CANcoderConfiguration();
cc.MagnetSensor.SensorDirection = SensorDirectionValue.CounterClockwise_Positive;
cc.MagnetSensor.MagnetOffset = ArmConstants.kCancoderOffsetRot;    // from Tuner X
cc.MagnetSensor.AbsoluteSensorDiscontinuityPoint = 0.5;            // reads -0.5..0.5 rot
cancoder.getConfigurator().apply(cc);

// Let the TalonFX close the loop on the arm angle:
TalonFXConfiguration fx = new TalonFXConfiguration();
fx.Feedback.FeedbackRemoteSensorID = cancoder.getDeviceID();
fx.Feedback.FeedbackSensorSource = FeedbackSensorSourceValue.FusedCANcoder; // Pro; RemoteCANcoder otherwise
fx.Feedback.SensorToMechanismRatio = 1.0;              // CANcoder rotation = arm rotation
fx.Feedback.RotorToSensorRatio = ArmConstants.kGearRatio;
```

- **Offset:** rotate the mechanism to its zero (for an arm, horizontal), read **Absolute Position**
  in Tuner X, and set `MagnetOffset` to its negative.
- **Discontinuity point:** sets where the reading wraps. Choose it so the mechanism's range of
  motion never crosses the wrap.
- **Fused vs. Remote:** `RemoteCANcoder` uses only the CANcoder. `FusedCANcoder` combines CANcoder
  (absolute) with the rotor (fast, smooth) and needs Phoenix Pro.
- Read it directly with `cancoder.getAbsolutePosition()` / `getPosition()` / `getVelocity()`
  status signals.
- **CANdi** can also read a PWM (duty-cycle) or quadrature encoder, such as a Through Bore, onto CAN
  and feed it to a TalonFX (`FeedbackSensorSourceValue.RemoteCANdiPWM1`).

**Sim:** `cancoder.getSimState().setRawPosition(armRot)` and `.setVelocity(armRotPerSec)` from your
physics model.

## REV: Through Bore Encoder

A hex-shaft encoder with **both** an absolute (duty-cycle) output and a relative (quadrature)
output.

**On a SPARK data port** (closed loop on the SPARK):

```java
config.absoluteEncoder
    .positionConversionFactor(1.0)      // encoder rotation = arm rotation
    .zeroOffset(ArmConstants.kAbsOffsetRot)
    .inverted(false)
    .zeroCentered(true);                // reads -0.5..0.5 instead of 0..1
config.closedLoop.feedbackSensor(FeedbackSensor.kAbsoluteEncoder);

SparkAbsoluteEncoder abs = motor.getAbsoluteEncoder();
double armRot = abs.getPosition();
```

On a SPARK MAX this needs REV's absolute encoder adapter, plus
`config.absoluteEncoder.setSparkMaxDataPortConfig()`. A SPARK Flex has a separate external encoder
port.

**On a roboRIO DIO port** (read in robot code with WPILib):

```java
DutyCycleEncoder abs = new DutyCycleEncoder(ArmConstants.kDioChannel, 1.0, ArmConstants.kAbsOffsetRot);
double armRot = abs.get();               // 0..1 rotation (fullRange = 1.0)
boolean ok = abs.isConnected();
```

## Redux: Canandmag

A CAN absolute magnetic encoder (ReduxLib, already in our vendordeps).

```java
Canandmag enc = new Canandmag(ArmConstants.kCanandmagId);
double absRot = enc.getAbsPosition();    // absolute, 0..1 rotation
double rot = enc.getPosition();          // multi-turn, rotations
double rps = enc.getVelocity();          // rotations per second
boolean ok = enc.isConnected() && enc.magnetInRange();
```

It can't be a TalonFX remote sensor, so its value goes to the motor through robot code, or it's
used to **seed** the motor encoder at boot: `motorTalon.setPosition(enc.getAbsPosition())`. That's
a good pattern for any non-CTRE absolute encoder.

## ThriftyBot: Thrifty encoders

ThriftyBot's absolute encoder exists as an **analog** version (read with WPILib's `AnalogEncoder`)
and a **CAN** version (ThriftyLib's `ThriftyEncoder`). Check the units of `getPosition()` in
ThriftyLib's docs before using it; it returns raw integer counts.

## WPILib generic classes

| Class | Sensor |
|---|---|
| `Encoder(chA, chB)` | Quadrature encoder on two DIO ports. Call `setDistancePerPulse(...)`. |
| `DutyCycleEncoder(channel, fullRange, expectedZero)` | Absolute encoder's PWM output on DIO |
| `AnalogEncoder(channel, fullRange, expectedZero)` | Absolute encoder with analog output |
| `AnalogPotentiometer(channel, fullRange, offset)` | Potentiometer |

Each has a WPILib sim class (`EncoderSim`, `DutyCycleEncoderSim`, `AnalogEncoderSim`) for the
sim IO.

## Seeding: making a relative encoder absolute at boot

A common pattern when the motor's encoder runs the loop, but you have an absolute sensor elsewhere:

```java
// In the IO constructor, after configuring:
double absArmRot = absoluteEncoder.get();          // from any absolute encoder
motor.setPosition(absArmRot);                      // TalonFX now reports the true arm angle
```

Do it once at boot, or when the robot is disabled and still. Don't re-seed while moving.
