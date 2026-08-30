// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismLigament2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismRoot2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ElevatorConstants;
import frc.robot.Constants.OperatorConstants;
import frc.robot.Constants.ReefConstants;
import frc.robot.Constants.SystemConstants;
import frc.robot.Constants.ElevatorConstants.Setpoint;
import frc.robot.utilities.LoggedTunableNumber;
import frc.robot.utilities.Alerts.AlertHandler;
import frc.robot.utilities.Alerts.MotorAlerts;

import java.util.HashMap;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

import org.littletonrobotics.junction.Logger;

public class ElevatorSubsystem extends SubsystemBase {
  private SparkMax leftMotor;
  private SparkMax rightMotor;
  private SparkMax algaeMotor;
  private SparkMaxConfig leftMotorConfiguration;
  private SparkMaxConfig rightMotorConfiguration;

  // encoder measures the rotation amt, (part of the sparkmax motor)
  private RelativeEncoder leftEncoder;
  private RelativeEncoder rightEncoder;

  private DigitalInput upperLeftLimit;
  private DigitalInput upperRightLimit;
  private DigitalInput lowerLeftLimit;
  private DigitalInput lowerRightLimit;

  // the PID adjusts the positioning of the robot to go where we actually want to
  // go
  private PIDController pidController;
  private boolean PIDEnable;
  private LoggedTunableNumber tunableP;
  private LoggedTunableNumber tunableI;
  private LoggedTunableNumber tunableD;
  private LoggedTunableNumber l1Setpoint;
  private LoggedTunableNumber l2Setpoint;
  private LoggedTunableNumber l3Setpoint;
  private LoggedTunableNumber l4Setpoint;
  private LoggedTunableNumber a1Setpoint;
  private LoggedTunableNumber a2Setpoint;
  
  // the main mechanism object
  Mechanism2d mech = new Mechanism2d(SystemConstants.kLength, SystemConstants.kWidth);
  // the mechanism root node
  MechanismRoot2d root = mech.getRoot("base", Units.inchesToMeters(16.52), Units.inchesToMeters(7));
  MechanismLigament2d m_elevator = root.append(new MechanismLigament2d("elevator", Units.inchesToMeters(33.75), 90));

  // list for a method that checks to see if the elevator is at setpoint
  private HashMap<Setpoint, Double> setpointMap;
  private Setpoint targetSetpoint;

  

  public ElevatorSubsystem() {
    m_elevator.setLineWeight(5);
    // motors
    leftMotor = new SparkMax(ElevatorConstants.kLeftMotor, MotorType.kBrushless);
    rightMotor = new SparkMax(ElevatorConstants.kRightMotor, MotorType.kBrushless);
    algaeMotor = new SparkMax(ElevatorConstants.kAlgaeMotor, MotorType.kBrushless);

    leftMotorConfiguration = new SparkMaxConfig();
    rightMotorConfiguration = new SparkMaxConfig();
    leftMotorConfiguration.follow(ElevatorConstants.kRightMotor, true);
    leftMotorConfiguration.smartCurrentLimit(ElevatorConstants.kMotorStallLimit);
    rightMotorConfiguration.smartCurrentLimit(ElevatorConstants.kMotorStallLimit);
    leftMotorConfiguration.idleMode(IdleMode.kBrake);
    rightMotorConfiguration.idleMode(IdleMode.kBrake);

    // leftMotorConfiguration.encoder.positionConversionFactor(-1);
    // leftMotorConfiguration.encoder..inverted(true);

    leftMotor.configure(
        leftMotorConfiguration, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
    rightMotor.configure(
        rightMotorConfiguration, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);

    // encoders
    leftEncoder = leftMotor.getEncoder();
    rightEncoder = rightMotor.getEncoder();

    // limit switches
    upperLeftLimit = new DigitalInput(ElevatorConstants.kUpperLeftLimit);
    upperRightLimit = new DigitalInput(ElevatorConstants.kUpperRightLimit);
    lowerLeftLimit = new DigitalInput(ElevatorConstants.kLowerLeftLimit);
    lowerRightLimit = new DigitalInput(ElevatorConstants.kLowerRightlimit);

    tunableP = new LoggedTunableNumber(this.getName() + "/Controller/P", ElevatorConstants.kP);
    tunableI = new LoggedTunableNumber(this.getName() + "/Controller/I", ElevatorConstants.kI);
    tunableD = new LoggedTunableNumber(this.getName() + "/Controller/D", ElevatorConstants.kD);

    // pid
    pidController = new PIDController(tunableP.get(), tunableI.get(), tunableD.get());
    PIDEnable = true;

    l1Setpoint = new LoggedTunableNumber(this.getName() + "/Setpoints/L1", ReefConstants.L1);
    l2Setpoint = new LoggedTunableNumber(this.getName() + "/Setpoints/L2", ReefConstants.L2);
    l3Setpoint = new LoggedTunableNumber(this.getName() + "/Setpoints/L3", ReefConstants.L3);
    l4Setpoint = new LoggedTunableNumber(this.getName() + "/Setpoints/L4", ReefConstants.L4);

    a1Setpoint = new LoggedTunableNumber(this.getName() + "/Setpoints/A1", ReefConstants.A1);
    a2Setpoint = new LoggedTunableNumber(this.getName() + "/Setpoints/A2", ReefConstants.A2);

    setpointMap = new HashMap<Setpoint, Double>();
    // list that is used latter on to check if at setpoint
    setpointMap.put(Setpoint.Home, ReefConstants.Home);
    setpointMap.put(Setpoint.L1, l1Setpoint.get());
    setpointMap.put(Setpoint.L2, l2Setpoint.get());
    setpointMap.put(Setpoint.L3, l3Setpoint.get());
    setpointMap.put(Setpoint.L4, l4Setpoint.get());
    setpointMap.put(Setpoint.A1, a1Setpoint.get());
    setpointMap.put(Setpoint.A2, a2Setpoint.get());

    targetSetpoint = Setpoint.Home;

    SmartDashboard.putData("MyMechanism", mech);
    
  }

  public void driveElevator(double power) {
    if ((power < 0) && ((!this.upperLeftLimit.get()) || (!this.upperRightLimit.get()))) {
      rightMotor.set(0);
    } else if ((power > 0) && ((!this.lowerLeftLimit.get()) || (!this.lowerRightLimit.get()))) {
      rightMotor.set(0);
    }
    rightMotor.setVoltage(power);
  }

  public Command elevatorSpeed(double power){
    return runOnce(
      () -> {
        rightMotor.setVoltage(power);
       
      });
  }

  public Command Set(Setpoint goal) {
    return run(
      ()-> {
        targetSetpoint = goal;
        pidController.setSetpoint(setpointMap.get(goal));
    }).until(()->this.atSetpoint(goal));
  }

  public Command Home() {
    return runOnce(
      () -> {
        targetSetpoint = Setpoint.Home;
        pidController.setSetpoint(ReefConstants.Home);
      });
  }

  public Command L1() {
    return runOnce(
      () -> {
        targetSetpoint = Setpoint.L1;
        pidController.setSetpoint(l1Setpoint.get());
      });
  }

  public Command A1() {
    return runOnce(
      () -> {
        targetSetpoint = Setpoint.A1;
        pidController.setSetpoint(a1Setpoint.get());
      });
  }

  public Command A2() {
    return runOnce(
      () -> {
        targetSetpoint = Setpoint.A2;
        pidController.setSetpoint(a2Setpoint.get());
      });
  }

  public Command L2() {
    return runOnce(
        () -> {
          targetSetpoint = Setpoint.L2;
          pidController.setSetpoint(l2Setpoint.get());
        });
  }

  public Command L3() {
    return runOnce(
        () -> {
          targetSetpoint = Setpoint.L3;
          pidController.setSetpoint(l3Setpoint.get());
        });
  }

  public Command L4() {
    return runOnce(
        () -> {          
          targetSetpoint = Setpoint.L4;
          pidController.setSetpoint(l4Setpoint.get());
        });
  }

  public Command setLift(double target) {
    return runOnce(
        () -> {
          pidController.setSetpoint(target);
        });
  }

  public Command setJoystick(DoubleSupplier joystickValue) {
    return runOnce(
        () -> {
          double setpoint = pidController.getSetpoint();
          pidController.setSetpoint(
              setpoint + (MathUtil.applyDeadband(joystickValue.getAsDouble(), OperatorConstants.kDeadband)) / 10);
        });
  }

  public Command setPIDEnable(boolean state) {
    return runOnce(
        () -> {
          PIDEnable = state;
        });
  }

  public boolean getPIDEnable() {
    return PIDEnable;
  }

  // you put in an int between 0 and 4, each int corrects to a setpoint
  // 0 is home, 1 is L1, 2 is L2, etc.
  public boolean atSetpoint() {
    return atSetpoint(targetSetpoint);
  }

  public boolean atSetpoint(Setpoint setpoint){
    if (Math.abs(rightEncoder.getPosition() - setpointMap.get(setpoint)) <= ElevatorConstants.kTolerance) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  // this has the PID controller loop
  public void periodic() {
    // This method will be called once per scheduler run

    double velocity = pidController.calculate(rightEncoder.getPosition(), pidController.getSetpoint());

    if ((this.getPIDEnable())
        && (Math.abs(pidController.getError())) > ElevatorConstants.kTolerance) {
      // constratins amt of volatage sent to motor
      velocity = MathUtil.clamp(velocity, ElevatorConstants.kMinVoltage, ElevatorConstants.kMaxVoltage); // AHHHHH
      Logger.recordOutput(getName()+"/Vel", velocity);                                                                                                   // VOLTAGE
      // spins the motor
      // motor1.setVoltage(velocity);
      this.driveElevator(velocity);
    } else {
      rightMotor.setVoltage(0);
    }

    if (!this.upperLeftLimit.get() == true || !this.upperRightLimit.get() == true) {
      rightEncoder.setPosition(ElevatorConstants.kMaxEncoderLimit);
      rightEncoder.setPosition(ElevatorConstants.kMaxEncoderLimit);
    } else if (!this.lowerLeftLimit.get() == true || !this.lowerRightLimit.get() == true) {
      rightEncoder.setPosition(ElevatorConstants.kMinEncoderLimit);
      rightEncoder.setPosition(ElevatorConstants.kMinEncoderLimit);
    }

    if(l1Setpoint.hasChanged(hashCode())) {
      setpointMap.replace(Setpoint.L1, l1Setpoint.get());
    }
    if(l2Setpoint.hasChanged(hashCode())) {
      setpointMap.replace(Setpoint.L2, l2Setpoint.get());
    }
    if(l3Setpoint.hasChanged(hashCode())) {
      setpointMap.replace(Setpoint.L3, l3Setpoint.get());
    }
    if(l4Setpoint.hasChanged(hashCode())) {
      setpointMap.replace(Setpoint.L4, l4Setpoint.get());
    }
    if(a1Setpoint.hasChanged(hashCode())) {
      setpointMap.replace(Setpoint.A1, a1Setpoint.get());
    }
    if(a2Setpoint.hasChanged(hashCode())) {
      setpointMap.replace(Setpoint.A2, a2Setpoint.get());
    }

    if (tunableP.hasChanged(hashCode())
        || tunableI.hasChanged(hashCode())
        || tunableD.hasChanged(hashCode())) {
      pidController.setPID(tunableP.get(), tunableI.get(), tunableD.get());
    }

    Logger.recordOutput(getName() + "/PIDerror", pidController.getError());
    Logger.recordOutput(getName() + "/Setpoint", pidController.getSetpoint());
    Logger.recordOutput(getName() + "/Position", rightEncoder.getPosition());
    Logger.recordOutput(getName() + "/AtSetpoint", atSetpoint());
    Logger.recordOutput(getName() + "/UpperLeftLimit", !upperLeftLimit.get());
    Logger.recordOutput(getName() + "/UpperRightLimit", !upperRightLimit.get());
    Logger.recordOutput(getName() + "/LowerLeftLimit", !lowerLeftLimit.get());
    Logger.recordOutput(getName() + "/LowerRightLimit", !lowerRightLimit.get());
    Logger.recordOutput(getName() + "/TargetSetpoint", targetSetpoint.name());
  }

  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
  }
}
