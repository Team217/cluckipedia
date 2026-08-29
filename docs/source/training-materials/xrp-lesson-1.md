# Lesson 1: Basic XRP Drive

This first lesson is going to take you through the simplest form of a command based drive using the XRP robot. This same structure and example can be applied to any full size FRC robot as well.

## Create XRP Drivebase Subsystem

```Java
public class ExampleSubsystem extends SubsystemBase {
  public ExampleSubsystem() {
	// Initialization of the subsystem happens here. 
	// Create motors, sensors, etc.
  }

  @Override
  public void periodic() {
	// Periodic acts like a while loop for each individual subsystem.
	// We use periodic to do math, log data, manage alerts, etc.
  }

  @Override
  public void simulationPeriodic() {
	// Simulation periodic works the same but only runs during simulation.
	// We do math and logging related to simulating movement.
  }
}
```

We're going to start by building a drivebase subsystem for the XRP. When creating a new subsystem, my recommendation usually is to copy the `ExampleSubsystem.java` file and rename it. I'm going to call mine XRPDriveSubsystem. There are 3 places you need change the name:
- The `ExampleSubsystem.java` filename.
- The class name `ExampleSubsystem`.
- The constructor of the class.

Once renamed it should look something like this:

```Java
public class XRPDriveSubsystem extends SubsystemBase {
  public XRPDriveSubsystem() {
  }

  ...
}
```

Subsystems are the layer of code that take inputs (joystick positions, speeds, etc.), do some modification or routing, and output the values to some sort of hardware. Building a drivebase subsystem the most important piece of hardware we need are the drive motors. Drivebase construction can vary wildly depending on how many wheels, what type of drivebase it is. For the XRP its about as simple as it can get. We have 2 motors one on the left side, one on the right. Each motor type / vendor has its own uniqe Java library to support the specific motor controller. The XRP motors use [`XRPMotor`](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/wpilibj/xrp/XRPMotor.html). Lets start by creating our two motors. 

```Java
import edu.wpi.first.wpilibj.xrp.XRPMotor;
import frc.robot.Constants.DriveConstants;

public class XRPDriveSubsystem extends SubsystemBase {
  // Set up the XRP hardware.
  private final XRPMotor m_rightMotor = new XRPMotor(DriveConstants.kRightMotorID);
  private final XRPMotor m_leftMotor = new XRPMotor(DriveConstants.kLeftMotorID);

  public XRPDriveSubsystem() {
  }

  ...
}
```

You'll notice when I created the XRPMotor I passed in an ID value. Instead of hardcoding the number I created some constants in our `Constants.java` file. We put any constants across the code base in this constants file so that we have a central location to find and change values. Makes it easier when tuning, testing, or in the pits at a competition to tweak the values.

```Java
public final class Constants {
  ...
  public final class DriveConstants {
	// The XRP has the left and right motors set to channels 0 and 1 respectively
	public static final int kLeftMotorID = 0;
	public static final int kRightMotorID = 1;
  }
  ...
}
```

Now that we have the motors created we have to configure them. Typically this would be setting inverison, current limits, maybe setting up a onboard control loop. With the XRPMotor the only thing we can configure is the inversion of the motor. When configuring hardware we always want to explicitly set each configuration reguardless if we want to change it from the default. If we ever swap a motor controller or reuse an old one, we can't assume the configuration is set how we want it.

```Java
  ...
  public XRPDriveSubsystem() {
	// Configure motors.
	m_leftMotor.setInverted(false);
    m_rightMotor.setInverted(false);
  }
  ...
```

Now that we have the motors created and configured we can put them together as a drivebase. WPILib has a [`DifferentialDrive`](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/wpilibj/drive/DifferentialDrive.html) class that provides some helper functions making it easier to create a "differential drive" robot. 

```Java
public class XRPDriveSubsystem extends SubsystemBase {
  // Set up the XRP hardware.
  private final XRPMotor m_rightMotor = new XRPMotor(DriveConstants.kRightMotorID);
  private final XRPMotor m_leftMotor = new XRPMotor(DriveConstants.kLeftMotorID);

  // Set up the differential drive controller.
  private final DifferentialDrive m_diffDrive =  new DifferentialDrive(m_leftMotor::set, m_rightMotor::set);
  
  public XRPDriveSubsystem() {
  ...
```

We used this `::` method reference operator to pass in a reference to the function set. What this does is allow DifferentialDrive to call the set functions of the motors internally. You'll see this operator show up more in robot code in the future. 

Now need to create a function that will allow us to actually drive this robot. You'll notice the motors  & differential drive have the word `private` in front of them, this prevents outside access to them directly (safe to prevent unauthorized use of the motor/drive). So inorder to control these motors we need a function that we can call. We'll add our function in between the constructor and the periodic functions at the bottom of the file.

There are two common ways of controlling a "differential drive" robot. With tank drive the X axis of each joystick is mapped to the motors on their respective sides. For arcade drive the forward movement is mapped to the X axis and rotation is mapped to the Y axis. This is typically split across two joysticks but can be combined into one as well. Below is a diagram to further illustrate this concept.

![DriveControlStyles.jpg](:/e0671889891244e2b836e28a9d6bfaf3)

Transitioning back to our functions, the `DifferentialDrive` class supports both of these drive styles. The functions we're creating here are very simple, we pass arguments that match the `arcadeDrive` or `tankDrive` arguments of `DifferentialDrive`. Below is the implementation of these functions. You'll note they are `public` functions meaning that we can access them from outside the subsystem. This is the key to later controlling the robot.

```Java
public void arcadeDrive(double fwdSpeed, double rotSpeed) {
  m_diffDrive.arcadeDrive(fwdSpeed, rotSpeed);
}

public void tankDrive(double leftSpeed, double rightSpeed) {
  m_diffDrive.tankDrive(leftSpeed, rightSpeed);
}
```

Congratulations! You have successfully created a simple drivebase subsystem for the XRP! In the next section we'll create a command to route the controller inputs into the subsystem.

## Create XRP Drive Command

Commands are actions the robot performs, intake a object, raise an arm, score something. We write command logic that preforms the action by commanding the subsystem. For a drive command this is fairly simple, we take controller input and we feed it into the drive functions we made previously. As we get into more complex logic and functionality the commands get more complex. There are multiple ways to create a command but we're going to start by focusing on command java files. 

Below is an annotated example command to highlight the main functions that make up a `Command` Class.
```Java
public class ExampleCommand extends Command {
  @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
  // Commands typically include at least one subsystem.
  private final ExampleSubsystem m_subsystem;

  public ExampleCommand(ExampleSubsystem subsystem) {
    m_subsystem = subsystem;
    addRequirements(subsystem);
  }

  @Override
  public void initialize() {
    // Called when the command is initially scheduled.
  }

  @Override
  public void execute() {
    // Execute acts like a while loop for each individual command.
  }

  @Override
  public void end(boolean interrupted) {
	// End runs once isFinished returns true or the command is interrupted.	 	// Serves as a way to safely stop the command. (Stop motion).
  }

  @Override
  public boolean isFinished() {
    // isFinished is the logic that ends the execute loop.
	// Returning false is equivalent to an infinite loop.
    return false;
  }
}
```

Much like the subsystem, my recommendation is to copy the `ExampleCommand.java` file and rename it. I'm going to call mine XRPDriveCommand. There are 3 places you need change the name:
- The `ExampleCommand.java` filename.
- The class name `ExampleCommand`.
- The constructor of the class.

Once renamed, we need to change that ExampleSubsystem into our XRPDriveSubsystem. There are 3 places this change needs to occur:
- The import at the top of the file.
- The global variable `m_subsystem`.
- The argument in the constructor.

Once all these changes are made, it should look something like this:
```Java
import frc.robot.subsystems.XRPDriveSubsystem;

public class XRPDriveCommand extends Command {
  @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
  private final XRPDriveSubsystem m_subsystem;

  public XRPDriveCommand(XRPDriveSubsystem subsystem) {
    m_subsystem = subsystem;

    addRequirements(subsystem);
  }
  ...
}
```

Looking back at our subsystem we had two different control methods; tank and arcade. Each of these methods required two joystick axis. That means this command needs to have two inputs being passed into it. To pass numeric joystick values into a command we use something called a [`DoubleSupplier`](https://docs.oracle.com/javase/8/docs/api/java/util/function/DoubleSupplier.html). A `DoubleSupplier` is a little different than just passing a `double` into the class. When passing a standard `double` into a class or function, that value is fixed at the time of creation. Changing that value outside of the class wouldn't do anything to the value in the class. This poses an issue when we want to send updating joystick values into the command. Thats where `DoubleSupplier` comes in, instead of providing a value at the creation of the class, we're passing a reference to a value, so we can change the value external to the class and that value will update inside of the class.

We're going to create two DoubleSuppliers, one for each axis. They'll look something like this:

```Java
import frc.robot.subsystems.XRPDriveSubsystem;
import java.util.function.DoubleSupplier;

public class XRPDriveCommand extends Command {
  @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
  private final XRPDriveSubsystem m_subsystem;
  // If doing arcade choose a more appropriate name
  private final DoubleSupplier m_leftSupplier;
  private final DoubleSupplier m_rightSupplier;
  ...
}
```

Then we need to add some argurments to our constructor so that when we create the command we can pass in the joystick inputs. Passing them to the global suppliers we created above.

```Java
public XRPDriveCommand(XRPDriveSubsystem subsystem, DoubleSupplier leftSupplier, DoubleSupplier rightSupplier) {
  m_subsystem = subsystem;
  m_leftSupplier = leftSupplier;
  m_rightSupplier = rightSupplier;

  addRequirements(subsystem);
}
```

This allows us to now access the subsystem and our joystick suppliers from the rest of the command functions. Now all that's left to do is pass our joystick suppliers into our drive functions in the subsystem. To access the values in our suppliers we use the method `.getAsDouble()` This returns a double we can then use like we would any other double. Below are the two critical lines we need to add to this command for it to function.

```Java
 public class XRPDriveCommand extends Command {
  ...
  @Override
  public void initialize() {
  }

  @Override
  public void execute() {
    m_subsystem.tankDrive(m_leftSupplier.getAsDouble(), m_rightSupplier.getAsDouble());
  }

  @Override
  public void end(boolean interrupted) {
	// Stop the motors if command gets interrupted.
	m_subsystem.tankDrive(0,0);
  }

  @Override
  public boolean isFinished() {
    return false;
  }
}
```

With that our basic drive command is completed. We can now move on to setting up our subsystem and commands. We wrote the underlying code to support the drive base. We 

## Setup the RobotContainer

We can write any number of command and subsystems but they wont do anything until we set them up in `RobotContainer` class. It is this class in which our robot takes shape, we add all the subsystems, map all the controller bidings, and configure our autonomous in the `RobotContainer`. Below is the typical structure of our `RobotContainer`:

```Java
public class RobotContainer {

  public RobotContainer() {
    // Configure the controller bindings
    configureDriverBindings();
	configureOperatorBindings();
  }

  private void configureDriverBindings() {
	// Driver specific controller bindings.
  }

  private void configureOperatorBindings() {
    // Operator specific controller bindings.
  }

  public Command getAutonomousCommand() {
    return new PrintCommand("Hello World");
  }
}
```

We're going to do a couple things here to get this robot moving:
1. Create an xbox / ps4 controller.
2. Add our XRPDrive subsystem.
3. Add our XRPDrive command.
4. Schedule the command to run.

We're going to create a few global variables. controller, subsystem, command. Our controller is a custom class that we wrote a couple years ago that allows for easy switching between Xbox, PS4, or other potential controller options. On the left side we just create our abstract controller, on the right we specifiy which hardware version we want to use. The controller takes a port number, this is a value the DriverStation software assigns each controller and allows us to differentiate the different controllers in software. Next we add the subsystem, it takes no arguments. Finally the command we're going to create it and pass in our two joystick axis. We have `getLeftY` & `getRightY` methods we'll use for our tank drive. Much like our DifferentialDrive class, we're going to use the `::` method reference operator to pass in a reference to the function.

```Java
import frc.robot.utilities.controller.Controller;
import frc.robot.utilities.controller.XboxController;
import frc.robot.Constants.OperatorConstants;
import frc.robot.subsystems.XRPDriveSubsystem;
import frc.robot.commands.BasicArcadeDrive;

public class RobotContainer {
  // Drive Team Joysticks.
  private final Controller m_driverController = new XboxController(OperatorConstants.kDriverPort);

  // Driver Subsystems.
  private final XRPDriveSubsystem m_xrpDrive = new XRPDriveSubsystem();

  // Commands.
  private final XRPDriveCommand m_driveCommand = new XRPDriveCommand(m_xrpDrive, m_driverController::getLeftY, m_driverController::getRightY);
  public RobotContainer() {
    ...
```

Much like with the subsystem. Where we have constants in the code we're going to relocate them to our `Constants.java` file. In this case we have a seperate group for OperatorConstants (related to controllers). 

```Java
public final class Constants {
  ...
  public final class OperatorConstants {
    public static final int kDriverPort = 0;
  }
  ...
}
```

Finally now that we have everything created in the RobotContainer now we just need to schedule this command so it will run all the time, effectivly allowing us to control the robot drivebase. To do this we use an inheritded function in the subsystem. `.setDefaultCommand`. This method take a commmand as an argument and when no other command is running, the default command passed in will be executed.

```Java
public class RobotContainer {
  ...
  private void configureDriverBindings() {
	m_xrpDrive.setDefaultCommand(m_driveCommand);
  }
  ...
}
```

This was the final piece of the puzzle needed to complete our basic drive code for the XRP robot. We can now build the code to make sure it is free of errors. To build or deploy code we need to open the Command Palette in VS Code, `Ctrl+Shift+P` will open the Palette. Once open you'll see a box appear at the top of the window. You want to type in `WPILib: Build Robot Code` and press enter. The terminal will open up at the bottom of the window. If everything was done properly you should see this message appear:
```
  BUILD SUCCESSFUL in 53s
  6 actionable tasks: 5 executed, 1 up-to-date
   *  Terminal will be reused by tasks, press any key to close it.
```

## Test the Robot Code

When we run the code, what happens? When you try to drive forward you end up spinning in a circle! Can you take a guess as to why that happens? We set the same output to both motors -1 to 1 but on the robot you can see the motors are mirrored. To fix this we just need to invert the motor thats spinning backwards. We've already included a provision in the code to do this. In our `XRPDriveSubsystem` we can navigate to the constructor. We need to change the left motors inversion to `true`.

```Java
  ...
  public XRPDriveSubsystem() {
	// Configure motors.
	m_leftMotor.setInverted(true);
    m_rightMotor.setInverted(false);
  }
  ...
```

The other issue you may have experianced is when you let go of the joystick the robots wheels still spin slowly. This is due to the joysticks not settling back exactly to zero. To fix this we apply something called a deadband to the output. Effectly this creates a dead zone on the joystick where the code still processes the value as zero. a deadband of 0.5 would mean anything from -0.5 to 0.5 would be considered 0. We're going add this deadband to our `XRPDriveCommand`.

[MathUtil](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/math/MathUtil.html) is a useful set of value modifying utilities, within this class there is a function called `applyDeadband` that we can use to add our deadband. It takes two arguments, the input value and our deadband value. We're going to put our joystick raw input in and then create a constant for our deadband value. Typically we use a value of 0.1 on our deadband, however you can tweak the value to be higher or lower depending on what you're trying to do. 

```Java
  public void execute() {
    // A deadband on the joystick allows for slight noise on the joystick to be ignored, we typically cut the bottom 10% off.
    double leftOutput = MathUtil.applyDeadband(m_leftSupplier.getAsDouble(), OperatorConstants.kDeadband);
    double rightOutput = MathUtil.applyDeadband(m_rightSupplier.getAsDouble(), OperatorConstants.kDeadband);
    m_subsystem.tankDrive(leftOutput, rightOutput);
  }
```

 We can now build the code again to make sure it is free of errors. I highly recommend you build often during development as it can help you identify errors early as you develop your code.