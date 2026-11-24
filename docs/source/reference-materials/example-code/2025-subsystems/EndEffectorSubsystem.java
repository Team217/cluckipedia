// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkLimitSwitch;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.EndEffectorConstants;
import frc.robot.Constants.OperatorConstants;
import frc.robot.utilities.LoggedTunableNumber;
import frc.robot.utilities.Alerts.AlertHandler;
import frc.robot.utilities.Alerts.MotorAlerts;

import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import org.littletonrobotics.junction.Logger;

public class EndEffectorSubsystem extends SubsystemBase {
  /** Creates a new ExampleSubsystem. */
  private SparkMax seaMotor;

  private SparkMaxConfig seaMotorConfig;
  private SparkLimitSwitch frontBanner;
  private SparkLimitSwitch rearBanner;

  private LoggedTunableNumber feedSpeed;
  private LoggedTunableNumber troughSpeed;
  private LoggedTunableNumber horizontalBranchSpeed;
  private LoggedTunableNumber verticalBranchSpeed;

  public EndEffectorSubsystem() {
    seaMotor = new SparkMax(EndEffectorConstants.kMotor, MotorType.kBrushless);
    frontBanner = seaMotor.getForwardLimitSwitch();
    rearBanner = seaMotor.getReverseLimitSwitch();

    seaMotorConfig = new SparkMaxConfig();
    seaMotorConfig.idleMode(IdleMode.kBrake);
    seaMotorConfig.smartCurrentLimit(EndEffectorConstants.kMotorStallLimit);
    seaMotorConfig.inverted(true);
    seaMotorConfig.limitSwitch.forwardLimitSwitchEnabled(false);
    seaMotorConfig.limitSwitch.reverseLimitSwitchEnabled(false);
    seaMotor.configure(seaMotorConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    feedSpeed = new LoggedTunableNumber(getName() + "/Speeds/Feed", 0.12);
    troughSpeed = new LoggedTunableNumber(getName() + "/Speeds/Trough", 0.4);
    horizontalBranchSpeed = new LoggedTunableNumber(getName() + "/Speeds/Horiz", 0.4);
    verticalBranchSpeed = new LoggedTunableNumber(getName() + "/Speeds/Vert", 0.4);
  }

  public Command load() {
    return run(
        () -> {
          if (this.inHopper().getAsBoolean()) {
            seaMotor.set(feedSpeed.get());
          } else {
            seaMotor.set(0.0);
          }
        });
  }

  public Command manual(DoubleSupplier speed) {
    return run(
        () -> {
          seaMotor.set(MathUtil.applyDeadband(speed.getAsDouble(), OperatorConstants.kDeadband));
        });
  }

  // inGo :)
  public Command control(boolean reverse) {
    return run(
        () -> {
          if (!reverse && this.inWheel().getAsBoolean()) {
            this.seaMotor.set(troughSpeed.get());
          } else if (reverse) {
            this.seaMotor.set(-1 * troughSpeed.get());
          } else {
            this.seaMotor.set(0.0);
          }
        });
  }

  public Command score(Double speed){
    return run(
        () -> {
          if(notInHopper().getAsBoolean() == true && inWheel().getAsBoolean() == true ){
            seaMotor.set(speed);
          }
          else{
            seaMotor.set(0);
          }
        });
  }
  // public boolean getbanner (DigitalInput banner){
  // return banner.get();
  // }
  public BooleanSupplier inHopper() {
    return () -> this.getRearBanner();
  }

  public BooleanSupplier notInHopper() {
    return () -> !this.getRearBanner();
  }


  public BooleanSupplier inWheel() {
    return () -> (!this.getRearBanner() && this.getFrontBanner());
  }

  public BooleanSupplier isEmpty() {
    return () -> (!this.getRearBanner() && !this.getFrontBanner());
  }

  public boolean getRearBanner() {
    return !frontBanner.isPressed();
  }

  public boolean getFrontBanner() {
    return !rearBanner.isPressed();
  }

  @Override
  public void periodic() {
    Logger.recordOutput(this.getName() + "/FrontBanner", this.getFrontBanner());
    Logger.recordOutput(this.getName() + "/Rearbanner", this.getRearBanner());
    Logger.recordOutput(this.getName() + "/InWheel", this.inWheel().getAsBoolean());
    Logger.recordOutput(this.getName() + "/InHopper", this.inHopper().getAsBoolean());
    Logger.recordOutput(this.getName() + "/IsEmpty", this.isEmpty().getAsBoolean());
    // Logger.recordOutput(this.getName() + "/Speed", motor1.get());
  }

  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
  }
}
