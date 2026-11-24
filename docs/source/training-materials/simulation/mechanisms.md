# Mechanism Simulation

Every mechanism sim follows the same loop, every 20 ms:

```text
TalonFX sim state ──(motor voltage)──▶ WPILib physics model
        ▲                                      │
        └──(rotor position & velocity)─────────┘
```

1. Give the simulated TalonFX a supply voltage.
2. Read the voltage its firmware is applying, and feed it into the physics model.
3. Step the physics model by 0.02 s.
4. Write the model's position and velocity back to the TalonFX as **rotor** values (multiply by the
   gear ratio).
5. Read the TalonFX normally.

You saw the full arm version in [Building an IO Subsystem](../advantagekit/io-subsystem.md#step-3-the-simulation-implementation).
Here are the other common mechanisms.

## Picking the physics model

| Mechanism | WPILib class | Key parameters |
|---|---|---|
| Roller, intake, feeder, turret (no gravity) | `DCMotorSim` | Gear ratio, moment of inertia (MOI) |
| Flywheel | `FlywheelSim` | Gear ratio, MOI |
| Arm, wrist, pivot | `SingleJointedArmSim` | Gear ratio, MOI, length, limits, gravity |
| Elevator | `ElevatorSim` | Gear ratio, carriage mass, drum radius, limits, gravity |

Get mass and MOI from the CAD team. Onshape and SolidWorks can calculate MOI around an axis. A rough
estimate is fine to start.

## Roller / flywheel (`DCMotorSim`)

```java
public class RollerIOSim extends RollerIOTalonFX {
  private static final DCMotor kGearbox = DCMotor.getKrakenX60(1);
  private final DCMotorSim sim =
      new DCMotorSim(
          LinearSystemId.createDCMotorSystem(kGearbox, RollerConstants.kMOI, RollerConstants.kGearRatio),
          kGearbox);
  private final TalonFXSimState simState = motor.getSimState();

  @Override
  public void updateInputs(RollerIOInputs inputs) {
    simState.setSupplyVoltage(RobotController.getBatteryVoltage());
    sim.setInputVoltage(simState.getMotorVoltage());
    sim.update(0.02);
    simState.setRawRotorPosition(sim.getAngularPositionRotations() * RollerConstants.kGearRatio);
    simState.setRotorVelocity(
        Units.radiansToRotations(sim.getAngularVelocityRadPerSec()) * RollerConstants.kGearRatio);
    super.updateInputs(inputs);
  }
}
```

## Elevator (`ElevatorSim`)

The elevator reports **meters**, but the TalonFX needs **rotor rotations**. Convert with the drum:

```text
mechanismRot = heightMeters / (2π × drumRadiusMeters)
rotorRot     = mechanismRot × gearRatio
```

```java
private final ElevatorSim sim =
    new ElevatorSim(
        DCMotor.getKrakenX60(2), ElevatorConstants.kGearRatio, ElevatorConstants.kCarriageMassKg,
        ElevatorConstants.kDrumRadiusMeters, ElevatorConstants.kMinHeightMeters,
        ElevatorConstants.kMaxHeightMeters, true, ElevatorConstants.kMinHeightMeters);

// in updateInputs:
double drumCircumference = 2 * Math.PI * ElevatorConstants.kDrumRadiusMeters;
simState.setRawRotorPosition(
    (sim.getPositionMeters() - ElevatorConstants.kMinHeightMeters)
        / drumCircumference * ElevatorConstants.kGearRatio);
simState.setRotorVelocity(
    sim.getVelocityMetersPerSecond() / drumCircumference * ElevatorConstants.kGearRatio);
```

For a two-motor elevator, the follower doesn't need its own physics. Give it the same rotor
values (watch the inversion) or leave it alone if your code never reads it.

## CANcoders and the Pigeon in sim

- `CANcoderSimState.setRawPosition(mechanismRot)` and `setVelocity(...)`. Update these if your
  TalonFX uses the CANcoder as its feedback sensor.
- `Pigeon2SimState.setRawYaw(degrees)`. The CTRE swerve sim does this for you.

## Battery sag (optional)

To include the effect of current draw on battery voltage:

```java
RoboRioSim.setVInVoltage(
    BatterySim.calculateDefaultBatteryLoadedVoltage(sim.getCurrentDrawAmps()));
```

Do this once in a central place (for example `Robot.simulationPeriodic()`) with the total current
of all simulated mechanisms, not separately in each subsystem.

## Checking a sim automatically

Once a mechanism works in simulation, a unit test can run the same sim and check that it still
reaches its setpoints after every change. See
[Testing a subsystem with the sim IO](../testing/unit-testing.md#testing-a-subsystem-with-the-sim-io).
