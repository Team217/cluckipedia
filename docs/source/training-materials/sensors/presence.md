# Limit, Presence & Homing Sensors

These sensors answer **yes/no** questions: "Is the elevator at the bottom?", "Is a game piece in the
intake?", "Is the arm at its hard stop?"

## Types and how they work

| Sensor | How it works | Good for | Watch out for |
|---|---|---|---|
| **Mechanical limit switch** | A lever closes or opens an electrical contact when pressed | End-of-travel detection | Gets bent or knocked loose by impacts |
| **Hall-effect sensor** | Detects a nearby magnet with no contact | End of travel, zeroing at a known position | Magnet placement and gap must be consistent |
| **Beam break** (IR emitter + receiver) | Light beam across a path; an object blocks it | Detecting game pieces passing a point | Alignment; sunlight and reflective pieces |
| **Reflective/photoelectric sensor** | Emitter and receiver in one housing, detects reflected light | Game piece present (one-sided mounting) | Color and finish of the target matter |
| **Proximity (time-of-flight) sensor** | Measures distance and compares to a threshold | Game piece present, with tunable range | See [Distance & Color Sensors](distance-color.md) |
| **Current spike (no sensor)** | The motor draws more current when stalled against something | Homing against a hard stop; detecting a grabbed piece | Must be tuned; false triggers during acceleration |

### Normally open vs. normally closed

- **Normally open (NO):** the circuit closes when pressed.
- **Normally closed (NC):** the circuit opens when pressed. **Safer for limits**: a broken wire reads
  as "pressed", so the mechanism stops instead of driving past its limit.

On the roboRIO, DIO inputs are **pulled up**: an open switch reads `true`, a closed switch reads
`false`. So with a NO switch wired to signal and ground, `get()` is `false` when pressed. Wrap it so
the rest of the code sees "pressed = true":

```java
inputs.atBottom = !bottomSwitch.get();
```

(SystemCore alpha units pulled inputs **down** instead. Check your controller's behavior.)

## Hardware limits vs. software checks

| Approach | How | When |
|---|---|---|
| **Hardware limit on the motor controller** | Switch is read by the TalonFX/SPARK, which refuses to drive past it | Safety limits: works even if robot code is slow or crashes |
| **Software check in robot code** | Read the sensor in `updateInputs`, act in commands | Game-piece detection, homing logic, anything that triggers commands |

Most robots use both: a hardware limit for safety, and the same signal logged and used for zeroing.

## Homing (zeroing a relative encoder)

When a mechanism uses a relative encoder, it needs a known position at startup:

1. **Start against a hard stop.** Simplest: the robot is always set up with the mechanism at its stop.
   Call `setPosition(...)` at boot. (The arm in [Building an IO Subsystem](../advantagekit/io-subsystem.md) does this.)
2. **Limit switch homing:** drive slowly toward the switch; when it triggers, set the position.
   CTRE can do this automatically: `ReverseLimitAutosetPositionEnable = true` sets the position the
   moment the switch closes.
3. **Current homing:** drive slowly into the hard stop at low voltage. When current is high and
   velocity is near zero for a short time, you're there. Set the position and back off.

```java
public Command home() {
  return run(() -> io.setVoltage(-1.5))
      .until(() -> inputs.statorCurrentAmps > 20.0 && Math.abs(inputs.velocityRotPerSec) < 0.05)
      .andThen(runOnce(() -> io.resetPosition(ElevatorConstants.kMinHeightRot)))
      .finallyDo(() -> io.setVoltage(0.0))
      .withName("Elevator Home");
}
```

Add a `Debouncer` (WPILib `edu.wpi.first.math.filter.Debouncer`) so a single noisy sample doesn't
count. The 2026 `Pivot.atHardStop()` is an unused start on this idea.

## CTRE

**Limit switch read by a TalonFX through a CANdi** (CTRE's CAN digital-input module), or from a
CANcoder or CANrange:

```java
CANdi candi = new CANdi(ElevatorConstants.kCandiId, bus);
// Configure CANdi S1 input behavior with CANdiConfiguration (pull/float, closed state) per CTRE docs.

TalonFXConfiguration fx = new TalonFXConfiguration();
fx.HardwareLimitSwitch.withReverseLimitRemoteCANdiS1(candi);          // source + ID in one call
fx.HardwareLimitSwitch.ReverseLimitEnable = true;
fx.HardwareLimitSwitch.ReverseLimitAutosetPositionEnable = true;      // re-zero on contact
fx.HardwareLimitSwitch.ReverseLimitAutosetPositionValue = 0.0;

StatusSignal<Boolean> s1Closed = candi.getS1Closed();                 // also log it
```

The same `withForwardLimitRemote...` / `withReverseLimitRemote...` methods accept a CANcoder,
CANrange, or another TalonFX as the source. A **CANrange** as a "limit" stops the motor when its
proximity detection triggers, which is useful for "stop the feeder when the game piece arrives."

Beam breaks and other on/off sensors can be wired to a CANdi input and read with `getS1Closed()`,
which keeps them on CAN instead of using controller ports.

## REV

Limit switches plug into the SPARK data port (a SPARK MAX needs REV's breakout or adapter board):

```java
config.limitSwitch
    .reverseLimitSwitchType(LimitSwitchConfig.Type.kNormallyClosed)
    .reverseLimitSwitchEnabled(true);   // the SPARK stops reverse motion when pressed
boolean atBottom = motor.getReverseLimitSwitch().isPressed();
```

The **REV Magnetic Limit Switch** is a hall-effect sensor with the same connector, used the same way.

## WPILib (DIO)

```java
DigitalInput beamBreak = new DigitalInput(FeederConstants.kBeamBreakChannel);
inputs.hasPiece = !beamBreak.get();     // beam broken pulls the line low on most sensors; check yours
```

**Sim:** `DIOSim` sets the value, or in the sim IO compute it directly, for example
`inputs.hasPiece = fuelSim.pieceInFeeder();`.
