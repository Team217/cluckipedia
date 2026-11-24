package frc.robot.subsystems;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkMaxConfig;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.networktables.DoubleSubscriber;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ClimberConstants;
import frc.robot.Constants.OperatorConstants;
import frc.robot.utilities.LoggedTunableNumber;
import frc.robot.utilities.Alerts.AlertHandler;
import frc.robot.utilities.Alerts.MotorAlerts;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import org.littletonrobotics.junction.Logger;

public class ClimberSubsystem extends SubsystemBase {
  public SparkMax climbMotor;
  private SparkMaxConfig climbMotorConfig;
  private RelativeEncoder climbEncoder;
  private PIDController climbPID;
  private boolean climbPIDEnabled;
  private double climbSetpoint;
  public DigitalInput leftCageDetect;
  public DigitalInput rightCageDetect;

  private LoggedTunableNumber tunableP;
  private LoggedTunableNumber tunableI;
  private LoggedTunableNumber tunableD;
  private LoggedTunableNumber climberSetpoint;

  public ClimberSubsystem() {
    leftCageDetect = new DigitalInput(ClimberConstants.kLeftCageDetect);
    rightCageDetect = new DigitalInput(ClimberConstants.kRightCageDetect);
    climbMotor = new SparkMax(ClimberConstants.kMotor, MotorType.kBrushless);

    tunableP = new LoggedTunableNumber(this.getName() + "/Controller/P", ClimberConstants.kP);
    tunableI = new LoggedTunableNumber(this.getName() + "/Controller/I", ClimberConstants.kI);
    tunableD = new LoggedTunableNumber(this.getName() + "/Controller/D", ClimberConstants.kD);
    climberSetpoint = new LoggedTunableNumber(getName() + "/Setpoints/Climber", ClimberConstants.kSetpoint);

    climbPID = new PIDController(tunableP.get(), tunableI.get(), tunableD.get());

    climbMotorConfig = new SparkMaxConfig();
    climbMotorConfig.inverted(false);
    climbMotorConfig.smartCurrentLimit(80);

    climbMotor.configure(climbMotorConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    climbEncoder = climbMotor.getEncoder();
    climbEncoder.setPosition(ClimberConstants.kHomeSetpoint);
    // AlertHandler.addAlerts(climbMotor);
    climbPIDEnabled = true;
  }
  ;

  public Command go() {
    return runOnce(
        () -> {
          // if (onCage().getAsBoolean()) {
            climbPID.setSetpoint(ClimberConstants.kHomeSetpoint);
          // }
        });
  }

  public Command manual(DoubleSupplier speed) {
    return run(
        () -> {
          climbMotor.set(MathUtil.applyDeadband(speed.getAsDouble(), OperatorConstants.kDeadband));
        });
  }

  public Command climbGo() {
    return runOnce(
        () -> {
          climbPID.setSetpoint(climberSetpoint.get());
          // if (this.isEmpty().getAsBoolean()) {
          //   climbPID.setSetpoint(climberSetpoint.get());
          // } else {
          //   climbPID.setSetpoint(0);
          // }
        });
  }

  public BooleanSupplier isEmpty() {
    return () -> (!this.getLeftCageDetect() && !this.getRightCageDetect());
  }

  public BooleanSupplier onCage() {
    return () -> (this.getLeftCageDetect() && this.getRightCageDetect());
  }

  public double getClimbPosition() {
    return climbEncoder.getPosition();
  }

  public boolean getPIDStatus() {
    return climbPIDEnabled;
  }

  public void setAngleVoltage(double voltage) {
    climbMotor.setVoltage(voltage);
  }

  public Command setCLimbPIDStatus(boolean state) {
    return runOnce(
        () -> {
          climbPIDEnabled = state;
        });
  }

  public boolean getLeftCageDetect() {
    // return !leftCageDetect.get();
    return true;
  }

  public boolean getRightCageDetect() {
    // return !rightCageDetect.get();
    return true;
  }

  @Override
  public void periodic() {
    // This method will be called once per scheduler run
    Logger.recordOutput(this.getName() + "/LeftCageDetect", this.getLeftCageDetect());
    Logger.recordOutput(this.getName() + "/RightCageDetect", this.getRightCageDetect());

    double climbVelocity = 1 * climbPID.calculate(this.getClimbPosition(), climbPID.getSetpoint());

    if (tunableP.hasChanged(hashCode())
        || tunableI.hasChanged(hashCode())
        || tunableD.hasChanged(hashCode())) {
      climbPID.setPID(tunableP.get(), tunableI.get(), tunableD.get());
    }

    if ((climbPIDEnabled == true) && (Math.abs(climbPID.getError()) > ClimberConstants.kTolerance)) {
      climbVelocity =
          MathUtil.clamp(climbVelocity, ClimberConstants.kMinVoltage, ClimberConstants.kMaxVoltage);
      this.setAngleVoltage(climbVelocity);
    } else {
      climbMotor.setVoltage(0);
    }

    Logger.recordOutput(this.getName() + "/ClimbVelocity", climbVelocity);
    Logger.recordOutput(this.getName() + "/ClimbPID", climbPIDEnabled);
    Logger.recordOutput(this.getName() + "/ClimbSetpoint", climbPID.getSetpoint());
    Logger.recordOutput(this.getName() + "/EncoderPosition", getClimbPosition());
    Logger.recordOutput(this.getName() + "/IsEmpty", this.isEmpty());
    Logger.recordOutput(this.getName() + "/OnCage", this.onCage());
    Logger.recordOutput(this.getName() + "/Error", climbPID.getError());

    
    AlertHandler.addAlerts(climbMotor); 
  }
  // Check PID values

  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
  }
}

