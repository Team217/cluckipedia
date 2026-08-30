package frc.robot.subsystems;

import com.revrobotics.AbsoluteEncoder;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.AlgaeConstants;
import frc.robot.Constants.OperatorConstants;
import frc.robot.utilities.LoggedTunableNumber;
import java.util.function.DoubleSupplier;
import org.littletonrobotics.junction.Logger;

import frc.robot.utilities.Alerts.AlertHandler;
import frc.robot.utilities.Alerts.MotorAlerts;

public class AlgaeSubsystem extends SubsystemBase {
  private SparkMax intakeMotor;
  private SparkMax angleMotor;
  private SparkMaxConfig intakeMotorConfiguration;
  private SparkMaxConfig angleMotorConfiguration;
  private AbsoluteEncoder angleEncoder;
  private PIDController anglePID;
  private boolean anglePIDEnabled;

  private LoggedTunableNumber tunableP;
  private LoggedTunableNumber tunableI;
  private LoggedTunableNumber tunableD;
  private LoggedTunableNumber angleSetpoint;

  public AlgaeSubsystem() {
    intakeMotor = new SparkMax(AlgaeConstants.intakeMotor, MotorType.kBrushless);
    angleMotor = new SparkMax(AlgaeConstants.angleMotor, MotorType.kBrushless);

    tunableP = new LoggedTunableNumber(this.getName() + "P", AlgaeConstants.kP);
    tunableI = new LoggedTunableNumber(this.getName() + "I", AlgaeConstants.kI);
    tunableD = new LoggedTunableNumber(this.getName() + "D", AlgaeConstants.kD);
    angleSetpoint = new LoggedTunableNumber(getName() + "/Setpoints/Angle" + AlgaeConstants.angleSetpoint);

    anglePID = new PIDController(tunableP.get(), tunableI.get(), tunableD.get());

    intakeMotorConfiguration = new SparkMaxConfig();
    angleMotorConfiguration = new SparkMaxConfig();

    intakeMotorConfiguration.inverted(false);
    angleMotorConfiguration.inverted(false);
    intakeMotorConfiguration.smartCurrentLimit(AlgaeConstants.kAngleStallLimit);
    angleMotorConfiguration.smartCurrentLimit(AlgaeConstants.kAngleStallLimit);
    intakeMotorConfiguration.idleMode(IdleMode.kBrake);
    angleMotorConfiguration.idleMode(IdleMode.kBrake);

    intakeMotor.configure(
        intakeMotorConfiguration, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    angleMotor.configure(
        angleMotorConfiguration, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    angleEncoder = angleMotor.getAbsoluteEncoder();
    // angleEncoder = angleMotor.getEncoder();
    anglePIDEnabled = true;
    // angleSetpoint = angleEncoder.getPosition();
    // AlertHandler.addAlerts(intakeMotor);
    // AlertHandler.addAlerts(angleMotor);
  }

  public Command intake(DoubleSupplier speed) {
    return run(
        () -> {
          if (isExtended() && speed.getAsDouble() > 0.0) {
            this.intakeMotor.set(speed.getAsDouble());
          } else {
            this.intakeMotor.set(0.0);
          }
        });
  }

  public Command algaeStop() {
    return runOnce(
        () -> {
          this.intakeMotor.set(0.0);
        });
  }

  public Command algaeOut() {
    return runOnce(
        () -> {
          anglePID.setSetpoint(angleSetpoint.get());
        });
  }

  public Command algaeSpit() {
    return runOnce(
        () -> {
          this.intakeMotor.set(-0.4);
        });
  }

  public Command algaeHome() {
    return runOnce(
        () -> {
          this.intakeMotor.set(0.0);
          anglePID.setSetpoint(AlgaeConstants.kHomeSetpoint);
        });
  }

  public Command manual(DoubleSupplier intakeSpeed, DoubleSupplier armPosition) {
    return run(
        () -> {
          intakeMotor.set(MathUtil.applyDeadband(intakeSpeed.getAsDouble(), OperatorConstants.kDeadband));
          double setpoint = angleEncoder.getPosition();
          angleMotor.set(
              setpoint + (MathUtil.applyDeadband(armPosition.getAsDouble(), OperatorConstants.kDeadband)) / 10);
          
        });
      }
      
  public double getAnglePosition() {
    return angleEncoder.getPosition();
  }

  public double getIntakePosition() {
    return 0; // intakeEncoder.getPosition();
  }

  public boolean getPIDStatus() {
    return anglePIDEnabled;
  }

  public void setAngleVoltage(double voltage) {
    angleMotor.setVoltage(voltage);
  }

  public boolean isExtended() {
    return (this.getAnglePosition() >= (angleSetpoint.get() - AlgaeConstants.kAngleTolerance))
        && (this.getAnglePosition() < (angleSetpoint.get() + AlgaeConstants.kAngleTolerance));
  }

  public Command setAlgaePIDStatus(boolean state) {
    return runOnce(
        () -> {
          anglePIDEnabled = state;
        });
  }

  @Override
  public void periodic() {
    
    

    double algaeAngle = 1 * anglePID.calculate(this.getAnglePosition(), angleSetpoint.get());

    if (tunableP.hasChanged(hashCode())
        || tunableI.hasChanged(hashCode())
        || tunableD.hasChanged(hashCode())) {
      anglePID.setPID(tunableP.get(), tunableI.get(), tunableD.get());
    }

    if ((anglePIDEnabled == true)
        && (Math.abs(anglePID.getError()) > AlgaeConstants.kAngleTolerance)) {
      algaeAngle = MathUtil.clamp(
          algaeAngle,
          Constants.AlgaeConstants.kMinVoltage,
          Constants.AlgaeConstants.kMaxVoltage);
      this.setAngleVoltage(algaeAngle);
    } else {
      angleMotor.setVoltage(0);
    }

    // if (angleSetpoint == angleInSetpoint) {
    // intakeMotor.setVoltage(0);
    // }

    // Logger.recordOutput(this.getName() + "/ClimbVelocity", algaeAngle);
    Logger.recordOutput(this.getName() + "/AnglePID", anglePIDEnabled);
    Logger.recordOutput(this.getName() + "/AngleSetpoint", angleSetpoint);
    Logger.recordOutput(this.getName() + "/EncoderPosition", getAnglePosition());
    Logger.recordOutput(this.getName() + "/Error", anglePID.getError());
    Logger.recordOutput(this.getName() + "/Extended", this.isExtended());
    
    AlertHandler.addAlerts(angleMotor); 
    AlertHandler.addAlerts(intakeMotor); 
    
  }

  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
  }
}
