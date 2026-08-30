# Lesson 2: Intermediate XRP Drive

Now that we have the basic framework of a drive subsystem together now we're going to work on improving the capabilities of the drive, focusing on improved control of the motors via velocity setpoints. To do this we need to start by making some changes to how we drive the motors in the subsystem.

## Remove DifferentialDrive

In our previous example we used the `DifferentialDrive` class to handle the underlying logic that converts joystick movement to movement of the drive motors. We then set the motor output indirectly using `arcadeDrive` and `tankDrive` methods. Going forward we want to control the motor voltage directly. The motor classes all have a setVoltage function that lets us set the voltage output going to the motor. Since we're going to be controlling the motors directly now, we can get rid of the `DifferentialDrive` class initialization in our `XRPDriveSubsystem`.

```Java
 // private final DifferentialDrive m_diffDrive =  new DifferentialDrive(m_leftMotor::set, m_rightMotor::set);
```

You'll notice when that line is removed our `tankDrive` and `arcadeDrive` functions are throwing errors. We can swap the `m_diffDrive` methods for direct motor control. That looks something like this:

```Java
  public void arcadeDrive(double fwdSpeed, double rotSpeed) {
    m_leftMotor.set(fwdSpeed + rotSpeed);
    m_rightMotor.set(fwdSpeed - rotSpeed);
  }

  public void tankDrive(double leftSpeed, double rightSpeed) {
    m_leftMotor.set(leftSpeed);
    m_rightMotor.set(rightSpeed);
  }
```

Notice for arcade drive we have to do some math to make it work. For more information on the theory behind arcade drive check out [this page](http://xiaoxiae.github.io/Robotics-Simplified-Website/drivetrain-control/arcade-drive/) by Robotics Simplified. This now basically puts us in the same spot we were in at the beginning of the lesson. Build and Deploy the code to ensure you maintain the same level of functionality you had before.

## Adding Voltage Control

As previously mentioned the motor controller classes have a `setVoltage` method that takes a value in and sets the motor controllers output to that value. In FRC our robots run on a 12 volt system. The XRP robots run on 6 volts. When setting the motor controllers on the XRP -6 volts would be clockwise rotation, 0 volts would be off, 6 volts would be counter-clockwise. We're going to create a simple function called `setVoltage` like most setter functions we're not going to return anything.

```Java
  public void setVoltage(double leftVolt, double rightVolt) {
    m_leftMotor.setVoltage(leftVoltage);
    m_rightMotor.setVoltage(rightVoltage);
  }
```

The next thing we're going to do is add clamping to keep our output voltage value inline with the system voltage, this provides two features. It ensures anything we pass to the motors falls within our expected values and it provides an easy way to adjust the maximum output of the system. To add a clamp we're going to use the [`MathUtil`](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/math/MathUtil.html) class again. The clamp function take the input value, followed by the minimum value and the maximum value. Like most constants used on the robot we will store the max voltage in the constants file.

```Java
  public final class DriveConstants {
    ...
    public static final double kMaxVolt = 6;
  }
```

With the constant for our max voltage added we can now add it to our `setVoltage` method. The final method should look something like this.

```Java
 public void setVoltage(double leftVolt, double rightVolt) {
   // Clamp output to maximum voltage.
   double leftVoltage = MathUtil.clamp(leftVolt, -DriveConstants.kMaxVolt, DriveConstants.kMaxVolt);
   double rightVoltage = MathUtil.clamp(rightVolt, -DriveConstants.kMaxVolt, DriveConstants.kMaxVolt);  m_leftMotor.setVoltage(leftVoltage);
   m_rightMotor.setVoltage(rightVoltage);
 }
```

## Update XRPDriveCommand

To use and text this new function we can simply change the `m_subsystem.tankDrive` to use our new setVoltage function. We are going to tak our left and right output variables and scale them to our max voltage. We can do this by simply multiplying our leftOutput/rightOutput by our kMaxVolt constant.

```Java
  public void execute() {
    // A deadband on the joystick allows for slight noise on the joystick to be ignored, we typically cut the bottom 10% off.
    double leftOutput = MathUtil.applyDeadband(m_leftSupplier.getAsDouble(), OperatorConstants.kDeadband);
    double rightOutput = MathUtil.applyDeadband(m_rightSupplier.getAsDouble(), OperatorConstants.kDeadband);
    m_subsystem.setVoltage(leftOutput * DriveConstants.kMaxVolt, rightOutput * DriveConstants.kMaxVolt);
  }
```

This again sets the robot up to work the same it has previously, with the added benifit of setting the code up in a way to let us explore more complex motion control options. Build and Deploy the code to ensure you maintain the same level of functionality you had before.

## Adding Feedback to the System

When working with robot mechanisms and subsystems it is benificial to work in real world units, -1 to 1 or even -12 volts to 12 volts doesn't provide the user any insight into how the ouptut of the system is effecting the actual robot. So when desiging our subsystem code its best to build the system to work on real units. For our teams codebase we do everything in metric units. The next step then is to set the robot drivebase up to support driving based on a target velocity (meters per second).

Everything we've done up to this point we have recieved feedback from the drivers joysticks, modified those values and passed that to the motor controllers. This is definied as open loop control or non-feedback control. The robot and user have no idea how fast the robot is moving, just that fully forward on the joystick moves the robot at full speed. To drive this robot based on a target velocity we need to use a closed loop contol scheme or a feedback based controller. Most commonly used in robotics is a PID controller (A proportional–integral–derivative controller). A PID controller take a setpoint (a target velocity or position) and compares it to the feedback (the current position or velocity). Based on the error it then does some math to calculate what the output needs to be to hit that setpoint, by constantly repeating this process the robot can preciesly control its movement.

To setup a PID controller we first need to setup the source of feedback from the motors. Most motors used in FRC have a source of feedback integrated into them, a sensor called an encoder. Encoders are sensors that track the position the motor shaft, in knowing the position the encoder can also calculate the velocity of the motor output. To create an encoder for the XRP robot we're going to use the [`Encoder`](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/wpilibj/Encoder.html) class. The encoder requires two digital inputs to function. Much like our motor ID numbers we need to create a set of constants to store the digital inputs assigned to each encoder.

```Java
  public final class DriveConstants {
  	...
    // The XRP encoders are mapped to digital inputs.
    public static final int kLeftEncA = 4;
    public static final int kLeftEncB = 5;
    public static final int kRightEncA = 6;
    public static final int kRightEncB = 7;
    ...
  }
```

Finally in our `XRPDriveSubsystem` we can initialize our encoders in the space below the motors.

```Java
public class XRPDriveSubsystem extends SubsystemBase {
  ...
  // Encoders to provide position & velocity feedback from the drive motors.
  private final Encoder m_leftEncoder = new Encoder(DriveConstants.kLeftEncA, DriveConstants.kLeftEncB);
  private final Encoder m_rightEncoder = new Encoder(DriveConstants.kRightEncA, DriveConstants.kRightEncB);
  ...
}
```

Much like the motors our encoders also have some configuration settings. The main one we're going to focus on is `setDistancePerPulse`. This method allows us to specify for each pulse of the encoder what that translates to in real units. Like previously mentioned we're going to work in meters for our code. To figure out what our distance per pulse is we need to do some math. The diameter of the wheel is a known unit (0.06m), we need to determine what the perimeter is, We can use Pi \* D to calculate the diameter. Java has a `Math.PI` we can use. Next thing we need to know is how many pulses the motor has per full rotation of the motor. We can check the data sheet of the motor to find this value (585 counts per revolution). Finally we just need to divide the wheel perimeter by the counts per revolution to get our meters per pulse value needed for the encoder conversion. This math can be setup as a series of constants in our `Constants` file. Try to group similar constants together to make it easier to come back to in the future.

```Java
  public final class DriveConstants {
  	...
    // Physical properties.
    public static final double kWheelDiameter = 0.06; // Meters.
    public static final double kWheelPerimeter = kWheelDiameter * Math.PI; // Meters.
    public static final double kCountPerRev = 585;
    public static final double kMeterPerCount = kWheelPerimeter / kCountPerRev;
    ...
  }
```

Going back to our `XRPDriveSubsystem` we can now add the `setDistancePerPulse` method used our calculated value. Much like the motors we're also going to set the inversion setting to ensure they're configured how we'd expect them to be. Our encoder configuration should look something like this:

```Java
  public XRPDriveSubsystem() {
    ...
    // Configure encoders to go from counts to real units (meters & meters/second).
    m_leftEncoder.setDistancePerPulse(DriveConstants.kMeterPerCount);
    m_rightEncoder.setDistancePerPulse(DriveConstants.kMeterPerCount);
    m_leftEncoder.setReverseDirection(true);
    m_rightEncoder.setReverseDirection(true);
  }
```

Now that we have our encoders created we should verify that they're working as intended.

## Introduction to Logging

Logging and viewing data on the robot is a useful tool in helping to understand whats going on in your underlying code. There are two main ways we can handle viewing data on our robot. The first being [`SmartDashboard`](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/wpilibj/smartdashboard/SmartDashboard.html) This class allows us to publish data in key, value pairs to the robots network. This allows for live viewing of the data, however it does not log or save the data for future review. There is no setup required, you just call the static method that matches the data type you want to publish. The key is the first argument and is simply a text string to make the data more human readable. We typically organize our data by first putting the name of the class file that the data originates from then following that up with the datas key. `this.getName()` will return a string of the class name. The key can be divided into different "buckets" by splitting keys with a forward slash `/`. Updating our SmartDashboard data is something we want to do every cycle the code runs, this is a perfect use of our `periodic` method.

The encoder has two (2) methods that let us access the data from them. `getRate` returns the velocity and `getDistance` returns the current position in whatever units defined by our DistancePerPulse conversion factor. In this case our velocity is in meters per second and our position is meters.

```Java
  @Override
  public void periodic() {
    // Dashboard only, not logged to file.
    SmartDashboard.putNumber(this.getName() + "/LeftMotor/Velocity", m_leftEncoder.getRate());
    SmartDashboard.putNumber(this.getName() + "/LeftMotor/Position", m_leftEncoder.getDistance());
    SmartDashboard.putNumber(this.getName() + "/RightMotor/Velocity", m_rightEncoder.getRate());
    SmartDashboard.putNumber(this.getName() + "/RightMotor/Position", m_rightEncoder.getDistance());
  }
```

At this point we haven't made any changes to the actual drive functionality of our robot so lets go ahead and test the code to see if our encoders are working. Build and Deploy the code. We can use [AdvantageScope](https://docs.advantagescope.org/category/getting-started/) or [Elastic](https://frc-elastic.gitbook.io/docs).

TODO: How to use AdvantageScope or Elastic.

Publishing data to view in real time is useful for a lot of our debugging in the buildspace but falls short when we want to try to analyze what happend in a match. To do that we need to actaully log the data to a file for later viewing. There are a lot of logging frameworks availible in FRC, on our team we use a framework called [AdvantageKit](https://docs.advantagekit.org/category/getting-started/). There some advanced uses to AdvantageKit we'll cover in future lessons, but the improtant one for right now is `Logger.recordOutput` this is equivelent to the above SmartDashboard functionality, with the main difference being `recordOutput` handles any data type you pass into it, you don't need to specify a different method for each data type.

```Java
  @Override
  public void periodic() {
    ...
    // Live viewable and logged to file.
    Logger.recordOutput(this.getName() + "/LeftMotor/Velocity", m_leftEncoder.getRate());
    Logger.recordOutput(this.getName() + "/LeftMotor/Position", m_leftEncoder.getDistance());
    Logger.recordOutput(this.getName() + "/RightMotor/Velocity", m_rightEncoder.getRate());
    Logger.recordOutput(this.getName() + "/RightMotor/Position", m_rightEncoder.getDistance());
  }
```

The quick rule of thumb for when to use each one, `SmartDashboard` is analogous to a `System.out.println`, temporary statement for debugging purposes. `Logger.recordOuput` is good for critical data we may want to analyize later. When in doubt just use `Logger`.

## Implementing Velocity Control

As mentioned above to drive this chassis based on velocity setpoints we need to setup a PID controller. WPILib includes a class called [`PIDController`](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/math/controller/PIDController.html)  
this class takes 3 constants. a P value, I value and D value.These 3 values work together to modify the ouput going to the motor. We'll get into the specifics of what each of these values do to the ouput in a later section. For now we need to create some constants to hold these values. Remember we nreed a controller for each motor, so two of each constants.

```Java
  public final class DriveConstants {
  	...
    // PID constants for the drive motors.
    public static final double kLeftCtrlP = 3.4;
    public static final double kLeftCtrlI = 0;
    public static final double kLeftCtrlD = 0;
    public static final double kRightCtrlP = 3.4;
    public static final double kRightCtrlI = 0;
    public static final double kRightCtrlD = 0;
    ...
  }
```

Back in the `XRPDriveSubsystem` we can create and initilize our PID controllers/

```Java
public class XRPDriveSubsystem extends SubsystemBase {
  ...
  // PID controllers for drive motors.
  private final PIDController m_leftController = new PIDController(DriveConstants.kLeftCtrlP, DriveConstants.kLeftCtrlI, DriveConstants.kLeftCtrlD);
  private final PIDController m_rightController = new PIDController(DriveConstants.kRightCtrlP, DriveConstants.kRightCtrlI, DriveConstants.kRightCtrlD);
  ...
```

Now that we have the controllers defined we can build out a function that will let us drive the motors using these controllers. setVelocity is going to have two arguments, left velocity and right velocity. These values are going to get passed into the PID controller along with the velocuty of the motor. The PID controller has a function called `calculate` that performs the PID calculation. We're going to get the velocity from the encoder and pass that in as the feeback source. This value is going to get inverted and then we're going to pass it into our previously built `etVoltage`. The resulting function should look something like this:

```Java
  public void setVelocity(double leftVelocity, double rightVelocity) {
    // Velocity based PID control of the drive motors. 
    double leftVoltage = -1 * m_leftController.calculate(m_leftEncoder.getRate(), leftVelocity);
    double rightVoltage = -1 * m_rightController.calculate(m_rightEncoder.getRate(), rightVelocity);
    this.setVoltage(leftVoltage, rightVoltage);
  }
```

We can now modify our drive command to support our new velocity based approach.

## Closed Loop Drive

Much like our scaling to the voltage ouput we did with our setVoltage command in the XRPDriveCommand we can do the same thing for our velocity. We'll create a constant to define our max velocity. We can use the formula velocity = (perimeter \* RPM) / 60. We'll first create a RPM constant, checking the data sheet the RPM is 240.

```Java
  public final class DriveConstants {
    ...
    public static final double kRPM = 240;
    public static final double kMaxVelocity = (kWheelPerimeter * kRPM) / 60.0; // Meters / second.
    ```
  }
```

The XRP velocity calculates out to about 0.7536 m/s. We can now go to our `XRPDriveCommand` and update the execute method. Instead of multiplying the joystick magnatude by the max voltage we will now multiply it by our max velocity.

```Java
  @Override
  public void execute() {
    ...
    m_subsystem.setVelocity(leftOutput * DriveConstants.kMaxVelocity, rightOutput * DriveConstants.kMaxVelocity);
  }
```

With this change we can now build and deploy our robot code and see how the PID controllers drive the wheels to the target velocity.

## Tune PID Controller Performance

TODO: Tuning the PID controllers.  
Refer to the documentation WPILIB has on PID tuning for now. [Tuning PID Controller](https://docs.wpilib.org/en/2020/docs/software/advanced-control/introduction/tuning-pid-controller.html)

## Smooth Drive Accelleration

On larger robots, going from a stand still to full speed acceleration can pull a lot of current, and is really hard on the mechanical components of the robot. We can implement some simple strategies that keep the driver from accelerating too quickly reducing these negative impacts. In our `XRPDriveCommand` we can setup a Slew Rate Limiter. The slew rate is the maximum amount an output can change compared to the input signal. What this does is effectively gives us an acceleration curve. They're also really simple to set up. The only arguement the [`SlewRateLimiter`](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/math/filter/SlewRateLimiter.html) takes is the maximum rate. We're going to create another constant to hold this value. In our `DriveConstants` we'll add `kRateLimit`:

```Java
  public final class DriveConstants {
    ...
    // The rate of change of the drive motors in manual control. unit/second.
    public static final double kRateLimit = 0.8;
    ...
  }
```

In the `XRPDriveCommand` we're going add two slew rate limiter, one for each axis. We cannot use the same one for multiple input sources. We'll add this limiter decloration below the `DoubleSuppliers`.

```Java
public class XRPDriveCommand extends Command {
  ...
  private final SlewRateLimiter m_leftLimiter = new SlewRateLimiter(DriveConstants.kRateLimit);
  private final SlewRateLimiter m_rightLimiter = new SlewRateLimiter(DriveConstants.kRateLimit);
  ...
}
```

Finally in our `execute` method we just need to modify the output value by feeding it through our limiter. The complete method should look something like this:

```Java
  @Override
  public void execute() {
    // A deadband on the joystick allows for slight noise on the joystick to be ignored, we typically cut the bottom 10% off.
    double leftOutput = MathUtil.applyDeadband(m_leftSupplier.getAsDouble(), OperatorConstants.kDeadband);
    double rightOutput = MathUtil.applyDeadband(m_rightSupplier.getAsDouble(), OperatorConstants.kDeadband);
    // Slew rate limiter limits the rate of change, this gives a simple acceleration / deceleration curve to the drive.
    leftOutput = m_leftLimiter.calculate(leftOutput);
    rightOutput = m_rightLimiter.calculate(rightOutput);
    m_subsystem.setVelocity(leftOutput * DriveConstants.kMaxVelocity, rightOutput * DriveConstants.kMaxVelocity);
  }
```

We can do a final build and deploy for this lesson. Try driving and vary the rate on the limiter to see how it impacts your acceleration.