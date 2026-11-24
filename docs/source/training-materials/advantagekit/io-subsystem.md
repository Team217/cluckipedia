# Building an IO Subsystem (Step by Step)

:::{admonition} 2027
:class: note
The IO layer (Steps 1–3) is the same in 2027. Only the subsystem (Step 4) changes if the team uses
Commands v3: see the v3 version of this arm in
[Migrating from v2 to v3](../command-based/v2-to-v3.md#before-and-after-the-arm).
In a 2027 project, WPILib imports start with `org.wpilib` instead of `edu.wpi.first`.
:::

In this walkthrough you'll build an **arm** subsystem from scratch: one Kraken, a gearbox, and
position control with Motion Magic. It runs on real hardware and in simulation with the same
subsystem code. Every mechanism you write later (elevators, turrets, hoods, wrists) follows this
recipe.

Files you'll create in `src/main/java/frc/robot/subsystems/arm/`:

| File | Role |
|---|---|
| `ArmIO.java` | Interface + logged inputs |
| `ArmIOTalonFX.java` | Real hardware (Phoenix 6) |
| `ArmIOSim.java` | Simulation (physics model driving the Phoenix 6 sim) |
| `Arm.java` | The subsystem: logic, commands, logging |

Plus an `ArmConstants` class in `Constants.java`.

:::{admonition} Warm-up first
:class: tip
Before the arm, read the 2026 `subsystems/rollers/RollerIO.java`, `RollerIOTalonFX.java`,
and `RollerIOSim.java`. A roller is the simplest IO subsystem (open-loop only). Then write your
own roller from memory, and compare.
:::

## Step 0: Constants

Every number that describes the mechanism goes in one place.

```java
public static final class ArmConstants {
  public static final int kMotorId = 25;

  // Mechanism
  public static final double kGearRatio = 60.0;            // motor rotations per arm rotation
  public static final double kLengthMeters = Units.inchesToMeters(20.0);
  public static final double kMassKg = Units.lbsToKilograms(8.0);

  // Positions in ARM rotations. 0 = horizontal, positive = up (needed for Arm_Cosine gravity).
  public static final double kMinPositionRot = Units.degreesToRotations(-10.0);
  public static final double kMaxPositionRot = Units.degreesToRotations(95.0);
  public static final double kStartPositionRot = kMaxPositionRot;  // boots resting on the stow hard stop
  public static final double kStowRot = Units.degreesToRotations(90.0);
  public static final double kScoreRot = Units.degreesToRotations(30.0);
  public static final double kIntakeRot = Units.degreesToRotations(-5.0);
  public static final double kToleranceRot = Units.degreesToRotations(2.0);

  // Gains (volts per arm rotation of error, etc.). Starting points; tune them.
  public static final double kP = 40.0;
  public static final double kD = 0.0;
  public static final double kS = 0.1;
  public static final double kG = 0.3;
  public static final double kV = 7.0;   // V per (arm rot/s); about 12 V / free speed
  public static final double kCruiseVelocityRotPerSec = 1.0;
  public static final double kAccelerationRotPerSec2 = 3.0;

  public static final double kStatorCurrentLimitAmps = 60.0;
}
```

## Step 1: The IO interface

```java
package frc.robot.subsystems.arm;

import org.littletonrobotics.junction.AutoLog;

/** Hardware abstraction for the arm. All positions are ARM rotations, 0 = horizontal. */
public interface ArmIO {
  @AutoLog
  public static class ArmIOInputs {
    public boolean connected = false;
    public double positionRot = 0.0;
    public double velocityRotPerSec = 0.0;
    public double appliedVolts = 0.0;
    public double statorCurrentAmps = 0.0;
    public double tempCelsius = 0.0;
    public double closedLoopReferenceRot = 0.0;
  }

  /** Reads all sensors into the inputs object. Called once per loop. */
  public default void updateInputs(ArmIOInputs inputs) {}

  /** Motion Magic to a position (arm rotations). */
  public default void setPosition(double positionRot) {}

  /** Open-loop voltage, for manual control and SysId. */
  public default void setVoltage(double volts) {}

  /** Updates closed-loop gains at runtime (for live tuning). */
  public default void setGains(double kP, double kD, double kS, double kG) {}

  public default void setBrakeMode(boolean brake) {}
}
```

**What `@AutoLog` does:** at build time, AdvantageKit generates a class called
`ArmIOInputsAutoLogged` that knows how to write every field to the log and read it back in replay.
That's why the subsystem creates `ArmIOInputsAutoLogged`, not `ArmIOInputs`. If VS Code says
the class doesn't exist, run a build once.

**What goes in inputs:** anything you might want to look at after a match. Include `connected`,
temperature, and current, not just position. They are how you diagnose hardware failures from a log.

## Step 2: The real hardware implementation

```java
package frc.robot.subsystems.arm;

import com.ctre.phoenix6.BaseStatusSignal;
import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusSignal;
import com.ctre.phoenix6.configs.Slot0Configs;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.units.measure.*;
import frc.robot.Constants.ArmConstants;
import frc.robot.Constants.SystemConstants;
import frc.robot.utilities.HardwareHelper;

public class ArmIOTalonFX implements ArmIO {
  protected final TalonFX motor =
      new TalonFX(ArmConstants.kMotorId, new CANBus(SystemConstants.kCANivoreBus));
  private final TalonFXConfiguration config = new TalonFXConfiguration();

  // Control requests: create once, reuse every loop.
  private final MotionMagicVoltage positionRequest = new MotionMagicVoltage(0.0);
  private final VoltageOut voltageRequest = new VoltageOut(0.0);

  private final StatusSignal<Angle> position = motor.getPosition();
  private final StatusSignal<AngularVelocity> velocity = motor.getVelocity();
  private final StatusSignal<Voltage> appliedVolts = motor.getMotorVoltage();
  private final StatusSignal<Current> statorCurrent = motor.getStatorCurrent();
  private final StatusSignal<Temperature> temp = motor.getDeviceTemp();
  private final StatusSignal<Double> reference = motor.getClosedLoopReference();

  public ArmIOTalonFX() {
    config.MotorOutput.Inverted = InvertedValue.CounterClockwise_Positive;
    config.MotorOutput.NeutralMode = NeutralModeValue.Brake;
    config.CurrentLimits.StatorCurrentLimit = ArmConstants.kStatorCurrentLimitAmps;
    config.CurrentLimits.StatorCurrentLimitEnable = true;

    // The TalonFX divides by the gear ratio for us, so every position below is in ARM rotations.
    config.Feedback.SensorToMechanismRatio = ArmConstants.kGearRatio;

    config.Slot0.kP = ArmConstants.kP;
    config.Slot0.kD = ArmConstants.kD;
    config.Slot0.kS = ArmConstants.kS;
    config.Slot0.kG = ArmConstants.kG;
    config.Slot0.kV = ArmConstants.kV;
    config.Slot0.GravityType = GravityTypeValue.Arm_Cosine;

    config.MotionMagic.MotionMagicCruiseVelocity = ArmConstants.kCruiseVelocityRotPerSec;
    config.MotionMagic.MotionMagicAcceleration = ArmConstants.kAccelerationRotPerSec2;

    config.SoftwareLimitSwitch.ForwardSoftLimitThreshold = ArmConstants.kMaxPositionRot;
    config.SoftwareLimitSwitch.ForwardSoftLimitEnable = true;
    config.SoftwareLimitSwitch.ReverseSoftLimitThreshold = ArmConstants.kMinPositionRot;
    config.SoftwareLimitSwitch.ReverseSoftLimitEnable = true;

    HardwareHelper.configMotor(motor, config);

    // The motor's encoder reads 0 at power-on. The arm boots resting on its stow hard stop,
    // so tell the TalonFX where it is.
    motor.setPosition(ArmConstants.kStartPositionRot);

    BaseStatusSignal.setUpdateFrequencyForAll(
        50.0, position, velocity, appliedVolts, statorCurrent, temp, reference);
    motor.optimizeBusUtilization();
  }

  @Override
  public void updateInputs(ArmIOInputs inputs) {
    inputs.connected =
        BaseStatusSignal.refreshAll(
                position, velocity, appliedVolts, statorCurrent, temp, reference)
            .isOK();
    inputs.positionRot = position.getValueAsDouble();
    inputs.velocityRotPerSec = velocity.getValueAsDouble();
    inputs.appliedVolts = appliedVolts.getValueAsDouble();
    inputs.statorCurrentAmps = statorCurrent.getValueAsDouble();
    inputs.tempCelsius = temp.getValueAsDouble();
    inputs.closedLoopReferenceRot = reference.getValueAsDouble();
  }

  @Override
  public void setPosition(double positionRot) {
    motor.setControl(positionRequest.withPosition(positionRot));
  }

  @Override
  public void setVoltage(double volts) {
    motor.setControl(voltageRequest.withOutput(volts));
  }

  @Override
  public void setGains(double kP, double kD, double kS, double kG) {
    Slot0Configs slot0 = config.Slot0;
    slot0.kP = kP;
    slot0.kD = kD;
    slot0.kS = kS;
    slot0.kG = kG;
    motor.getConfigurator().apply(slot0);
  }

  @Override
  public void setBrakeMode(boolean brake) {
    config.MotorOutput.NeutralMode = brake ? NeutralModeValue.Brake : NeutralModeValue.Coast;
    motor.getConfigurator().apply(config.MotorOutput);
  }
}
```

:::{admonition} Why the CANcoder isn't here
:class: note
With only the motor encoder, the arm must be in a known position at boot. For a real arm,
prefer a CANcoder on the arm shaft (see [Phoenix 6 Basics](../ctre/phoenix6.md#cancoder-and-pigeon-2))
so it knows its angle no matter where it starts.
:::

## Step 3: The simulation implementation

The cleanest way to simulate CTRE hardware is to **keep the real TalonFX code and feed its
simulated sensor values from a physics model.** Phoenix 6 runs the motor controller's firmware,
including Motion Magic and the PID loop, inside the simulator. So in sim you are testing the
**same closed loop** that runs on the robot.

```java
package frc.robot.subsystems.arm;

import com.ctre.phoenix6.sim.TalonFXSimState;
import edu.wpi.first.math.system.plant.DCMotor;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.simulation.SingleJointedArmSim;
import frc.robot.Constants.ArmConstants;

/** Simulated arm: a WPILib physics model feeding the Phoenix 6 simulated TalonFX. */
public class ArmIOSim extends ArmIOTalonFX {
  private final SingleJointedArmSim armSim =
      new SingleJointedArmSim(
          DCMotor.getKrakenX60(1),
          ArmConstants.kGearRatio,
          SingleJointedArmSim.estimateMOI(ArmConstants.kLengthMeters, ArmConstants.kMassKg),
          ArmConstants.kLengthMeters,
          Units.rotationsToRadians(ArmConstants.kMinPositionRot),
          Units.rotationsToRadians(ArmConstants.kMaxPositionRot),
          true, // simulate gravity
          Units.rotationsToRadians(ArmConstants.kStartPositionRot));

  private final TalonFXSimState simState = motor.getSimState();

  @Override
  public void updateInputs(ArmIOInputs inputs) {
    // 1. Give the simulated TalonFX a battery voltage.
    simState.setSupplyVoltage(RobotController.getBatteryVoltage());

    // 2. Whatever voltage the TalonFX's firmware decided to apply drives the physics model.
    armSim.setInputVoltage(simState.getMotorVoltage());
    armSim.update(0.02);

    // 3. Feed the result back as ROTOR (motor-side) readings. The real encoder reads 0 at boot,
    //    so report the change since the start position.
    double armRot = Units.radiansToRotations(armSim.getAngleRads());
    double armRotPerSec = Units.radiansToRotations(armSim.getVelocityRadPerSec());
    simState.setRawRotorPosition(
        (armRot - ArmConstants.kStartPositionRot) * ArmConstants.kGearRatio);
    simState.setRotorVelocity(armRotPerSec * ArmConstants.kGearRatio);

    // 4. Read the TalonFX exactly the way the real implementation does.
    super.updateInputs(inputs);
  }
}
```

:::{admonition} Inversion in sim
:class: warning
`TalonFXSimState` works in the motor's own frame. If you set
`Inverted = Clockwise_Positive`, also set
`simState.Orientation = ChassisReference.Clockwise_Positive`, or the sim will run backwards.
:::

Compare this with the 2026 `PivotIOSim`, which re-implements the TalonFX PID with a WPILib
`PIDController`. That works, but gains tuned in it may not behave the same on the real
TalonFX. The approach above avoids that problem and is the one we use going forward.

:::{admonition} Using REV hardware instead?
:class: tip
Write an `ArmIOSparkMax` (and `ArmIOSparkMaxSim`) that implements the same `ArmIO`. The subsystem
doesn't change at all. See [REV Motors](../rev/revlib.md) for the SPARK version of this exact arm.
:::

## Step 4: The subsystem

```java
package frc.robot.subsystems.arm;

import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ArmConstants;
import frc.robot.utilities.logging.LoggedTunableNumber;
import java.util.function.DoubleSupplier;
import org.littletonrobotics.junction.AutoLogOutput;
import org.littletonrobotics.junction.Logger;

public class Arm extends SubsystemBase {
  private final ArmIO io;
  private final ArmIOInputsAutoLogged inputs = new ArmIOInputsAutoLogged();
  private final Alert disconnectedAlert = new Alert("Arm motor disconnected", AlertType.kError);

  private final LoggedTunableNumber kP = new LoggedTunableNumber("Arm/kP", ArmConstants.kP);
  private final LoggedTunableNumber kD = new LoggedTunableNumber("Arm/kD", ArmConstants.kD);
  private final LoggedTunableNumber kS = new LoggedTunableNumber("Arm/kS", ArmConstants.kS);
  private final LoggedTunableNumber kG = new LoggedTunableNumber("Arm/kG", ArmConstants.kG);

  @AutoLogOutput(key = "Arm/GoalRot")
  private double goalRot = ArmConstants.kStartPositionRot;

  private boolean brakeMode = true;

  public Arm(ArmIO io) {
    this.io = io;
  }

  @Override
  public void periodic() {
    // 1. Read and log inputs. Always first.
    io.updateInputs(inputs);
    Logger.processInputs("Arm", inputs);

    // 2. Health checks.
    disconnectedAlert.set(!inputs.connected);

    // 3. Live tuning: push new gains to the motor only when they change.
    LoggedTunableNumber.ifChanged(
        hashCode(), g -> io.setGains(g[0], g[1], g[2], g[3]), kP, kD, kS, kG);

    // 4. Coast while disabled so the pit crew can move the arm by hand.
    boolean shouldBrake = DriverStation.isEnabled();
    if (shouldBrake != brakeMode) {
      brakeMode = shouldBrake;
      io.setBrakeMode(brakeMode);
    }

    Logger.recordOutput("Arm/AtGoal", atGoal());
    Logger.recordOutput("Arm/ErrorDeg", (goalRot - inputs.positionRot) * 360.0);
  }

  /** True when the arm is within tolerance of its goal. */
  public boolean atGoal() {
    return Math.abs(goalRot - inputs.positionRot) < ArmConstants.kToleranceRot;
  }

  private void setGoal(double positionRot) {
    goalRot = positionRot;
    io.setPosition(positionRot);
  }

  // ---------------- Commands ----------------

  /** Moves to a position and finishes when it arrives. The motor keeps holding afterwards. */
  public Command goTo(double positionRot) {
    return run(() -> setGoal(positionRot)).until(this::atGoal).withName("Arm GoTo");
  }

  /** Follows a changing target (for example from a shot-angle table) and never finishes. */
  public Command track(DoubleSupplier positionRot) {
    return run(() -> setGoal(positionRot.getAsDouble())).withName("Arm Track");
  }

  public Command stow() {
    return goTo(ArmConstants.kStowRot).withName("Arm Stow");
  }

  public Command score() {
    return goTo(ArmConstants.kScoreRot).withName("Arm Score");
  }

  public Command intake() {
    return goTo(ArmConstants.kIntakeRot).withName("Arm Intake");
  }

  /** Manual open-loop control for testing. */
  public Command manual(DoubleSupplier volts) {
    return runEnd(() -> io.setVoltage(volts.getAsDouble()), () -> io.setVoltage(0.0))
        .withName("Arm Manual");
  }
}
```

:::{admonition} `@AutoLogOutput`
:class: tip
Putting `@AutoLogOutput` on a field or a no-argument getter logs it every loop automatically.
It saves writing `Logger.recordOutput` for simple values. For it to work, the object must be
reachable from your `Robot`/`RobotContainer` fields; see the AdvantageKit docs.
:::

## Step 5: Wire it up in the container

```java
private final Arm arm;

public CompetitionContainer() {
  switch (SystemConstants.kDeployMode) {
    case REAL -> arm = new Arm(new ArmIOTalonFX());
    case SIM -> arm = new Arm(new ArmIOSim());
    default -> arm = new Arm(new ArmIO() {});   // REPLAY: inputs come from the log
  }
  configureBindings();
}

private void configureBindings() {
  operator.a().onTrue(arm.intake());
  operator.b().onTrue(arm.score());
  operator.x().onTrue(arm.stow());
  operator.leftBumper().whileTrue(arm.manual(() -> -operator.getLeftY() * 4.0));
}
```

`new ArmIO() {}` creates an implementation with only the `default` no-op methods. That is what
replay needs.

## Step 6: Test it in simulation

1. Run **WPILib: Simulate Robot Code**.
2. Open AdvantageScope → **File → Connect to Simulator**.
3. Make a **Line Graph** with `Arm/positionRot`, `Arm/closedLoopReferenceRot`, and `Arm/GoalRot`.
4. Enable teleop and press the buttons. You should see the reference follow a smooth Motion Magic
   profile and the position follow the reference.
5. Open the **Tuning** table in NetworkTables (AdvantageScope or Elastic), change `Arm/kP`, and
   watch the response change live. See [Tuning Workflow](../control/tuning.md).

## Step 7: Visualize it (optional but recommended)

Log a `LoggedMechanism2d` or a `Pose3d` of the arm and view it in AdvantageScope's 2D Mechanism or
3D Field tab. The 2026 `utilities/RobotVisualizer.java` shows how the team articulated the CAD robot
model in AdvantageScope.
