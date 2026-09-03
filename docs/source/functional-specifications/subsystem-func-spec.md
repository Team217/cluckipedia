# Subsystem Functional Specification

[toc]

## 1 Introduction

Subsystems are one of the primary building blocks we have that allow us to build modular & flexible FRC robot code.

## 1.1 Checklists

As a part of this functional specification, there is a set of accompanying checklists that are intended to serve as a quick reference guide for students to check their development progress on a particular subsystem. These checklists should serve as nothing more than an overview or reference. Refer to this specification document for support in developing a given subsystem.

<br>

## 2 Subsystem Component Overview

Subsystems at their core are a hardware interface layer for linking motors, actuators, and/or sensors to a set of commands that the higher level control schemes can use to produce complex robot operations. 

### 2.1 Motion Components

Robots move, we have a few primary means of making them do so. THis section covers the most common.

#### 2.1.1 Motors

Motors provide rotational motion, the specific mechanical design the motor gets integrated into defines how that motors rotational motion will translate to subsystem functionality. Motors a driven by a motor controller. There are two main vendors ThunderChickens uses; CTRE/WCP Kraken motors, REV Neo & Vortex motors.

#### 2.1.2  Pneumatics

Pneumatics are linear motion devices that have two states, extended and retracted. Pneumatics use compressed air to power them. We use unique hardware controllers to drive pneumatics. There are two types of solenoid, single action or double action. Single action use air pressure to extend out and a spring to retract. Double acting utilize air in both directions. A good way to remember them is single = spring.

#### 2.1.3 Servos

Servos are small form factor motors with some unique features and limitations. Servos come in two primary types; standard & continuous rotation.
- Standard servos have a limited rotation, +- 90 to 180 degrees depending on the type. These servos have internal control loops that drive them to their position & hold it there.
- Continuous rotation servos act like standard motors, they have a forward & backward control. Sometimes these servos are variable speed and some are fixed. 

### 2.2 Sensors

Sensors are critical for our group, they are what enable us to perform complex robot operations. Below are some of the common sensors we utilize to provide feedback to the subsystems & robot.

#### 2.2.1 Encoders

For mechanisms that don't naturally come to rest at their designated *home* position (zero (0) on the encoder). It is highly recommended to utilize an absolute encoder for reliable positioning. A CTRE CANCoder or a REV Through-bore would be suitable options for use. Often times with elevators gravity will bring them back to *home* because of this a relative encoder in the motor is sufficient.

#### 2.2.2 Limit switches

Whenever we have a physical mechanism that moves we often want to protect the larger robot structure from the subsystem moving beyond it's limits. We can utilize software limits on these mechanisms however in the event of an encoder issue we often like to add hardware limit switches as well. These limit switches provide multiple benefits. Firstly, they cut power to motors in the direction of travel when limit switch is depressed. Second, they provide a known location for re-seeding the encoder position. Finally, having multiple redundant limit switches allows for the mechanical or electrical failure of one without compromising the system integrity. For an elevator it is typical to run upper & lower limit switches.

#### 2.2.3 Cameras

We use cameras for two (2) primary means:
- AprilTag detection: This allows us to locate ourselves and field elements accurately on the field.
- ML/Color object detection: This aids us in navigating to game pieces scattered on the field.

#### 2.2.4 Inertial Measurement Unit (IMU)

IMU or Inertial Measurement Unit which is a collection of: Accelerometer, which measures linear acceleration in each axis, Gyroscopes, which measure rotational speed and changes in orientation, and finally Magnetometers or Compass, which measures magnetic fields to help determine absolute heading.


### 2.3 Misc.

For the rest of the hardware that doesn't fit into the usual boxes.

#### 2.3.1 Leds

Leds are visual indicators that help the drive team understand the current state of the robot. In addition to that we can use them to aid in debugging in the pits. The CANdle is the primary LED controller we use in FRC.

<br>

## 3 Common System Architecture

There's a handful of concepts that are common across all subsystems. While the function of the subsystem my differ each of them needs to support constants, logging, units, system characterization, and logging. This is part of keeping consistency across our ThunderChickens codebase.

### 3.1 Constants

We use a constants class tailored for each unique subsystem. The primary benefit of this is centralizing all our constants to one central location in the codebase. The constants can be found in `Constants.java`. Historically the constants were created & defined in this `Constants.java` file. Moving forward we have started defining a separate Java file that serves as a default constants template. This allows the constants required for the operation of the subsystem to travel with the subsystem code. It also supports implementing multiple instances of the same subsystem.

Below is an example of what a generic `SubsystemConstants.Java` file could look like:
```Java
public class SubsystemConstants {
  public int kLeaderCanID = -1;
  public String kCanBus = "";
  public Current kStatorLimit = Amps.of(40);
}
```
These constants can then be initialized in `Constants.java` for use in the specific instance of a subsystem.
```Java
public static final SubsystemConstants kDemoConstants = new SubsystemConstants();
static {
  kDemoConstants.kLeaderCanID = 1;
  kDemoConstants.kCanBus = "canivore";
  kDemoConstants.kStatorLimit = Amps.of(40);
  ...
  }
```

The constants for a subsystem need to be passed into the subsystem via the constructor. This is a new way of doing it for the ThunderChickens. With this change it provides us much needed flexibility when switching between competition & practice robots, or the ability to create multiple instances of a subsystem. Such as having multiple elevators on one robot.

### 3.2 Data / Units

It is strongly encouraged to do the conversion from "RPM" or "Rotations" to a meaningful unit that describes the actual real world movement of the mechanism. In the case of a system with a distance measure the primary unit should be meters. Angular units tend to be degrees as they are more human friendly, for math we often convert to radians. If you find working with inches or feet easier for a specific application you may convert from meters within that scope. For ThunderChickens codebase we tend to stick with Metric units. Bonus points if this is implemented through the use of the Java Units classes supported in WPILIB. This allows for very easy unit conversion and keeps the units consistent across the codebase.

### 3.3 Logging

Logging is the recording of telemetry data, such as motor, sensor, or robot state information to aid in the troubleshooting of the robot. When trying to understand why issues happened on the field, it is critical to have comprehensive logging to properly root cause the issues. The rule of thumb is to err on the side of more logging, we can always cut back or reduce logging in the future. To help get you thinking about what to long. Here's some of the primary data we may want to log:
- Relative encoder position
- Relative encoder velocity
- Control loop setpoint
- Limit switch states
- Absolute encoder position
- Absolute encoder velocity

For all ThunderChickens codebases, unless otherwise explicitly defined, logging is done through the [AdvantageKit](https://docs.advantagekit.org/) framework.

### 3.4 Simulation

Simulation provides a way for us to test our code before we have an actual robot to deploy to. With proper simulation support we can test core subsystem functionality, command sequencing, triggers, & more all before we have hardware in front of us. At a minimum every subsystem with at least one motor should be set up to simulate that motor. Utilizing the DCMotorSim object is a good starting point. Beyond that simulation of additional hardware, such as limit switches, external encoders, etc. is encouraged time permitting. Most if not all motor vendors have some level of simulation support baked into their software ecosystems.

#### 3.5 SysID Characterization:

SysID or System Identification is a process in which the mechanism runs through a predefined routine to quantify the PID & Feedforward constants for the system. AdvantageKit logging has compatibility with SysID (details [here](https://docs.advantagekit.org/data-flow/sysid-compatibility)). We need to setup the SysID commands & expose them in the subsystem so that we can execute the characterization at a future time when we have the physical mechanism present. Building these commands into the subsystem allows us to easily run SysID tests once we have the functional robot in front of us. 

<br>

## 4 Common System Interfaces

Subsystems implement SubsystemBase, this is a common java interface that all subsystems are built off of. Part of that means there's some methods we can extend to support our subsystem. This section goes into the common components across all subsystems defined in this specification.

### 4.1 Constructor

Constructors are the first block of code that runs when you create a java class object. This is where your initialization code is housed. In the case of subsystems this means some level of hardware initialization and configuration. Beyond hardware configuration we have to setup the subsystem to be ready to operate when the robot is enabled. This means setting initial setpoints, homing mechanisms, etc. Setup of simulation will also be included in the constructor, this is typically wrapped in a check to see if the robot is real or simulated.

### 4.2 Periodic

Periodic is a method called every cycle of the robot. This is typically a 20ms (0.02s) loop time. Think of the periodic function like an integrated `while(true)` loop. We typically put any reoccurring logging, checks, or calculations in this loop. **Caution** should be taken to not put anything computationally heavy in the periodic loop as that can cause overruns & slow the system response down. This periodic method runs on both real and simulated robots.

### 4.3 Simulation Periodic 

Simulation periodic is a method much like the above periodic but it only runs when the robot is being simulated. This section if solely for calculations related to simulated operation of the subsystem. Typically theres at least an `update` method to call.

<br>

## 5 Subsystem Specific Implementations

### 5.1 Drivebase

All robots need to move on the field. So every robot has some sort of drivebase or drivetrain to provide locomotion to the robot. In FRC these days the predominate drivebase design is Swerve, which provides the most dynamic control of the robot. Swerve is also the most complex drive system. For simple robots we often will utilize a differential drive which is more in line with what you would find on an RC car. There are some other flavors of differential drive robots that we'll touch on briefly in that section. 

#### 5.1.1 Swerve

A swerve drive system is typically made up of four (4) wheel modules mounted in each corner of the drivebase. These wheel modules are able to drive both the wheel itself and pivot the angle / heading of the wheel. This control of each wheels heading when working in tandem with the rest of the wheel modules enables the robot to move vary dynamically on the field & enables the "front" of the robot to become variable. Each wheel module is made up of two (2) motors. One for angle adjustment, and one for drive. Along with the motors, each angle motor uses an absolute encoder for positioning of the module angle. In addition to the 4 modules, the swerve subsystem also is equipped with an IMU / Gyroscope which provides the heading information needed to calculate the wheel positions. 

##### 5.1.1.1 Theory of Operation

##### 5.1.1.2 Architecture

##### 5.1.1.3 Interfaces

##### 5.1.1.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.1 is on all types of drivebase, it does cover swerve briefly but focuses on older drive styles.


#### 5.1.2 Differential Drive

Differential drive is your typical drive system. Two (2) rows of parallel wheels, left and right. They can be commanded forward, backward, or turn left and right. This drive system is cheap, reliable, and simple to execute. The XRP robots we use in training implement a simple differential drive system. These differential drive systems often chain or gear together multiple motors on each side to increase the torque or power behind the wheels.

##### 5.1.2.1 Theory of Operation

##### 5.1.2.2 Architecture

##### 5.1.2.3 Interfaces

##### 5.1.2.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.1 is on all types of drivebase, lots of different differential drivebase examples.

##### 5.1.2.5 Butterfly

Butterfly drive is a classic ThunderChickens drive system. Most recently we used it back in 2022.

This drive is a riff on the classic adding a pivoting wheel module in each corner. This wheel module integrates a traction wheel and an omni wheel into one module. With the control of a pneumatic solenoid we can switch between the traction and the omni balancing maneuverability and pushing power during a match. 

Additional configuration and methods are needed to setup the pneumatic system.


### 5.2 Motor Based Motion

#### 5.2.1 Elevator - Linear motion.

Elevators can be one of the most complicated mechanisms to program. The elevator subsystem from a hardware level has a few required hardware components as well as a variety of optional hardware to improve reliability and safety of the mechanism.

##### 5.2.1.1 Theory of Operation

Elevators are linear mechanisms, this means they move in one axis / direction. This is often vertical movement to pick up game elements from the ground and go score them. Elevators can have multiple stages, the more stages you have the higher maximum extension you can achieve, at the cost of a wider footprint. For FRC robots we typically have between 1 and 3 stages. The Elevator here is a single stage and has roughly 33" of vertical lift.

##### 5.2.1.2 Architecture

The core functionality of the elevator is as follows:
- Elevator mechanisms require motors to provide the lift. Depending on the demands of the game this could be one or two motors. The subsystem should be able to easily switch between the two different configurations. Motor type is typically determined in the first weeks of a competition season. For a *default* subsystem the recommendation would be to implement the system using the KrakenX60 motor controller the TalonFX.

- Motors need to be properly configured to work as we expect them to. Every motor should be explicitly defined in the subsystem. **Do not** assume a motor is pre-configured or in it's default configuration. Some of the key configurations to make are defined below:
  - Motor direction inversion.
  - Motor neutral mode.
  - Current limits.
  - Control loop configuration.
  - Feedback sensor configuration.

- The elevator requires some form of feedback to track its position. For most elevator applications where the elevator comes to rest at its *home* position (falls due to gravity) the motors integrated encoder is sufficient for feedback. The subsystem should be setup in such a way that allows for an external absolute encoder to be used in place of the relative encoder.

- Manual positioning of the elevator. We always want to provide a simple means of operation for the elevator. Typical application would be to map the direct movement of the elevator to a joystick axis. This can be useful for system failure on a competition field, or early prototype operation. hardware limit switches should be respected but software limits should be ignored.

- Control loop based positioning of the elevator. For any meaningful operation of the elevator we require precision control of the carriage. This is done through a conventional PID control loop. For ThunderChickens codebase we prefer to run using the integrated control loop on the motor controller. If there is a compelling reason to shift the control loop to the robot controller the standard WPILib `PIDController` can be used.

- User tunable PID constants, this is done through the use of the `LoggedTunableNumber` object. The goal here is to provide a quick and easy way for

- Software defined limits. The elevator subsystem needs to monitor the encoder feedback to ensure the mechanism is not extending beyond a software defined limit. Any attempt to extend past a limit should cut power to the motor in the direction of travel. The minimum and maximum software limits should be defined as constants.

- Minimal limit switch functionality needs to be present in the subsystem. For core operation of the elevator this would be a lower limit *home* position limit switch. This switch gives us the ability to home the elevator to a known location, allowing us to rely on our software limits for the upper extension limit. Limit switch functionality needs to be optional, with a way to enable or disable the checks.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Along with PID controllers we can also implement a FeedForward controller. These are important for implementing systems that want to control the velocity component of motion.

- Motion Profiling. This is a control method that defines maximum velocity and acceleration limits to the system. This is beneficial in that it allows for smoother operation of the mechanism. This reduces the maintenance required for the mechanism by reducing wear. Each motor supplier has their own implementation, along with WPILib proving options for on controller motion profiling. For CTRE/Phoenix6 its called Motion Magic while Rev calls it MaxMotion.

- Expanded hardware limit switches. Often for mission critical systems we want to bake in extra redundancy, limit switches are another area where we often implement redundancy. The subsystem should be able to support two (2) lower limits as well as an additional two (2) upper limits.

- Elevator homing sequence Command. On initialization the elevator should have the capability to home itself. Run it down at a slow power level until it either hits a lower limit or the current spikes indicating the elevator has hit a physical barrier.

##### 5.2.1.3 Interfaces

###### 5.2.1.3.1 Internal interfaces

- update PID/FF: Internal method to update the motor controller with the logged tunable values.
- drive functionality: Internal method to control the motor controller. This method should be the final step that checks HW/SW limits before commanding the motor. Commands should call this method to drive the mechanism, rather than attempting to command the motor directly.

###### 5.2.1.3.2 External interfaces

- Command for manual control of the elevator.
- Command for position setpoint control of the elevator.
- Commands for SysID.
- Command for homing the elevator.
- Getter if the elevator has reached its setpoint target.
- Getter for elevator limits.

##### 5.2.1.4 References

To improve your depth of knowledge on the construction or operation of elevator mechanisms in FIRST, the following resources provide a launching point.

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.2 is on elevator design & mechanical concepts.
- [Simple Elevator Visualizer](http://www.competitionrobotparts.com/elevator-visualizer/): Helpful in understanding the operating principles of an elevator.
- [WCP GrayT Elevator](https://wcproducts.com/collections/systems-structure/products/greyt-cascade-elevator): Product used in the competitive concept.
- [FIRST Education: Extenders](https://www.firstinspires.org/hubfs/lms/frc-guided-experience/m2/l4-extenders-lift-reach-climb-pdf.pdf?hsLang=en): Overview of different lift styles, including elevators.
- [Elevator Control Loop Tuning](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-elevator.html): Motion Profiler for Elevator applications.

##### 5.2.1.5 Climber - Linear with one way locking.

#### 5.2.2 Flywheel - Velocity control.

A flywheel is a rotational mechanism that spins fast & maintains energy. The most common application of a flywheel in FRC is for shooter wheels. Anytime we have a shooter that has to bring a game object up to speed very quickly a flywheel is the key to doing it. From a software perspective a flywheel is just a velocity controlled motor. 




##### 5.2.2.1 Theory of Operation

##### 5.2.2.2 Architecture

##### 5.2.2.3 Interfaces

##### 5.2.2.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.6.1 is on shooter flywheel design & mechanical concepts.

- Encoder to measure flywheel velocity. Relative encoder.
- Motor(s) to spin the flywheel.
    - Configure (idle mode, current limits).
    - Configure gear ratio.
- Speed controls:
    - Set-points. Via tunable PID & Feedforward.
    - Manual operation.
- Pass constants object into constructor
- Logging via AdvantageKit.
- Method to check if arm is at a setpoint. onTarget.
- Support for single or dual motors.
- Support for external encoder REV through-bore or CTRE CANCoder.
- PID basic control. SVA (Static Friction, Velocity, Acceleration) FeedForward Control Loop
- Filtering of velocity feedback. LinearFilter (WPI)
- Setup Setpoints to use Enum or data class. This reduces the risk of typos or "mystery constants/doubles" in code.
- Java Units Classes (Distance, Angle, Velocity, Etc.).
- Simulation of subsystem.
- Integration with SysID.


#### 5.2.3 Pivot - Angular positioning.

Pivots can be integrated into a wide variety of applications. From a software perspective a pivot is an angular / rotational position control. A motor rotates to & holds at the position. Think of an elbow or a railroad crossing gate.


##### 5.2.3.1 Theory of Operation

##### 5.2.3.2 Architecture

##### 5.2.3.3 Interfaces

##### 5.2.3.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.3 is on arm design & mechanical concepts.

- Absolute encoder to track arm position. Seed absolute encoder to the motors relative encoder (at the least on startup).
- Motor(s) to drive the arm pivot.
    - Configure (idle mode, current limits).
    - Configure gear ratio.
- Limit switches. HW limits, SW limits (Encoder position, Min / Max setpoint value).
- Speed controls:
    - Set-points. Via tunable PID. (Rotations at a minimum ideally real units).
    - Manual operation.
- Pass constants object into constructor.
- Logging via AdvantageKit.
- Method to check if arm is at a setpoint. onTarget.
- Support for single or dual motors.
- Motion Profiling (CTRE/Phoenix6 Motion Magic). MaxVelocity & MaxAcceleration.
- PID basic control. SVA (Static Friction, Velocity, Acceleration) or GVA (Gravity, Velocity, Acceleration) FeedForward Control Loop
- Optional HW limits. Enable / Disable this functionality.
    - Redundant limit switches (2 on top, 2 on bottom).
- Switch from Brake to Coast on disable and vise versa.
- Java Units Classes (Distance, Angle, Velocity, Etc.).
- Simulation of subsystem.
- Integration with SysID. Characterizes motor performance.

##### 5.2.3.5 Hood - Unique pivot.

- A hood is a variation on a pivot, hoods are used primarily for 


##### 5.2.3.6 Turret - Unique pivot

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.7 is on turret design & mechanical concepts.

- Absolute encoder to track position. Seed absolute encoder to the motors relative encoder (at the least on startup).
- Motor to drive the turret.
    - Configure (idle mode, current limits).
    - Configure gear ratio.
- Limit switches. HW limits, SW limits (Encoder position, Min / Max setpoint value).
- Speed controls:
    - Set-points. Via tunable PID. (Rotations at a minimum ideally real units).
    - Manual operation.
- Pass constants object into constructor.
- Logging via AdvantageKit.
- Method to check if arm is at a setpoint. onTarget.
- Motion Profiling (CTRE/Phoenix6 Motion Magic). MaxVelocity & MaxAcceleration.
- PID basic control. SVA (Static Friction, Velocity, Acceleration) or GVA (Gravity, Velocity, Acceleration) FeedForward Control Loop
- Optional HW limits. Enable / Disable this functionality.
    - Redundant limit switches (2 on top, 2 on bottom).
- Switch from Brake to Coast on disable and vise versa.
- Java Units Classes (Distance, Angle, Velocity, Etc.).
- Simulation of subsystem.
- Integration with SysID. Characterizes motor performance.
- Chinese Remainder Theorem (CRT) dual encoder position tracking.


#### 5.2.4 Roller - Open velocity control. 

A roller subsystem is one of the simplest mechanisms to program. It is simply a wheel, roller, belt, conveyor, etc. that runs in an open loop mode. Where we set the speed of the roller between -100% to 100%. This can be via applied output or voltage. We don't need feedback based control loops or any external sensors.

- Motor(s) to drive the roller.
    - Configure (idle mode, current limits).
    - Configure gear ratio.
- Speed controls:
    - Manual operation.
- Pass constants object into constructor.
- Logging via AdvantageKit.
- Support for single or dual motors.
- Java Units Classes (Distance, Angle, Velocity, Etc.).
- Simulation of subsystem.
- Integration with SysID. Characterizes motor performance.


##### 5.2.4.1 Theory of Operation

##### 5.2.4.2 Architecture

##### 5.2.4.3 Interfaces

##### 5.2.4.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.5 is on intake roller design & mechanical concepts.

### 5.3 Servo Based Motion

##### 5.3.1 Theory of Operation

##### 5.3.2 Architecture

##### 5.3.3 Interfaces

##### 5.3.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 4.2 is on servos.


##### 5.3.5 Pivot / Hood

### 5.4 Pneumatic based

##### 5.4.1 Theory of Operation

##### 5.4.2 Architecture

##### 5.4.3 Interfaces

##### 5.4.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 6.6 is on pneumatics.


##### 5.3.5 Gripper

### 5.5 Specialty

#### 5.5.1 Vision - Camera systems & pose estimation.

##### 5.5.1.1 Theory of Operation

##### 5.5.1.2 Architecture

##### 5.5.1.3 Interfaces

##### 5.5.1.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 6.4 is on cameras and vision concepts.
- [PhotonVision](https://photonvision.org/): Primary computer vision system utilized by the ThunderChickens.
- [Cluckipedia Vision Reference](https://cluck.thunderchickens.org/en/latest/reference-materials/vision-resources.html)


#### 5.5.2 Led - Visual feedback system, CANdle.

##### 5.5.2.1 Theory of Operation

##### 5.5.2.2 Architecture

##### 5.5.2.3 Interfaces

##### 5.5.2.4 References

- [CTRE CANdle Product Page](https://store.ctr-electronics.com/products/candle?srsltid=AfmBOor_ptYSzTNh7f-S45ZTQ4HopVHMj4pnIkfMbmXCX_2FiTsQ6D2e)
  - [User Manual](https://ctre.download/files/user-manual/CANdle%20User's%20Guide.pdf)
  - [Phoenix6 Documentation](https://v6.docs.ctr-electronics.com/en/latest/docs/hardware-reference/candle/index.html)
  - [Bring Up Guide](https://v5.docs.ctr-electronics.com/en/stable/ch12b_BringUpCANdle.html)
- [CANdle LED Plan Template](https://cluck.thunderchickens.org/en/latest/candle-template.html)


<br>

## 6 AdvantageKit IO Layer Framework

### 6.1 Subsystem: The base subsystem as is familiar to us.

### 6.2 Constants: The subsystem constants, again should be familiar.

### 6.3 SubsystemIO: The interface that defines the IO methods & data.

### 6.4 SubsystemIOSim: The simulation implementation of the above interface.

### 6.5 SubsystemIOHardware: The hardware implementation of the above interface.

### 6.6 References

- [AdvantageKit Website](https://docs.advantagekit.org/)
- [AdvantageScope Website](https://docs.advantagescope.org/)

<br>

## 7 Revision History


| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 06/13/2026 | AK     | Initial Release                     |
| 08/26/2026 | AK     | Expansion of document               |
| 08/30/2026 | AK     | Broaden to include all subsystems   |
