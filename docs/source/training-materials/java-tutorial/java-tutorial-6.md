# Java Tutorial | Above & Beyond 1 Programming

Welcome to the first ThunderChickens A&B Java Tutorial.
This tutorial will answer these questions:

1. How do we program?

There's a lot to do, so let's get started!

## How do we program?

As we've gone over in the past tutorials, we program in subsystems, meaning we have many files for different parts of the robot. Here's how we code one of them:

Robot.java
```Java
// This file is called Robot.java. This is where we initialize our Robot Container (Where we initialize our subsystems) and other functions.

package frc.robot; // FRC stuff

// Here, we create the Robot class where all of our initializing will be.
public class Robot extends LoggedRobot {
 // We create the variable for the robot container. We will initialize it further below.
 private final RobotContainer m_robotContainer;

 // This is our main function in the Robot.java file. This is run every time we give code to the robot.
 public Robot() {
  m_robotContainer = new RobotContainer();
 }
}
```

Once we have initialized our Robot Container file, let's move to RobotContainer.java to see what we're initializing!

RobotContainer.java
```java
// This file is called RobotContainer.java. This is where we initialize our subsystems and map commands to the controller.

package frc.robot; // FRC stuff
import frc.robot.subsystems.ElevatorSubsystem; // Our elevator subsystem
private final Controller m_operatorController = new XboxController(0); // The controller on port 0

public class RobotContainer {
 private final ElevatorSubsystem m_elevator = new ElevatorSubsystem();

 // This is our main function in the RobotContainer.java file.
 public RobotContainer() {
  // This is where we set our default command and configure our bindings.
  m_elevator.setDefaultCommand(m_elevator.manual(m_operatorController::getRightY));
  // Whenever the A button is pressed, the elevator will go up.
  m_operatorController.upButton().onTrue(m_elevator.goUp());
 }
}
```

This is great! We know what RobotContainer does! The elevator subsystem looks just like this one, except with more functions and features. Great job.

And there ya' go! You are all set to go to A&B 2!

<br>
Made with ♡ by Jacob