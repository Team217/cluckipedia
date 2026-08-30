# Subsystem Functional Specification

## 1 Introduction

Subsystems are one of the primary building blocks we have that allow us to build modular & flexible FRC robot code.

<br>

## 2 Subsystem Component Overview

Subsystems at their core are a hardware interface layer for linking motors, actuators, and/or sensors to a set of commands that the higher level control schemes can use to produce complex robot operations. 

### 2.1 Motion Components

Robots move, we have a few primary means of making them do so. THis section covers the most common.

#### 2.1.1 Motors

Motors provide rotational motion, the specific mechanical design the motor gets integrated into defines how that motors rotational motion will translate to subsystem functionality. Motors a driven by a motor controller. There are two main vendors ThunderChickens uses; CTRE/WCP Kraken motors, REV Neo & Vortex motors.

#### 2.1.2  Pneumatics

Pneumatics are linear motion devices that have two states, extended and retracted. Pneumatics use compressed air to power them. We use unique hardware controllers to drive pneumatics.

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

### 2.3 Misc.

#### 2.3.1 Leds

Leds are visual indicators that help the drive team understand the current state of the robot. In addition to that we can use them to aid in debugging in the pits. The CANdle is the primary LED controller we use in FRC.

<br>

## 3 Common System Architecture

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

It is strongly encouraged to do the conversion from "RPM" or "Rotations" to a meaningful unit that describes the actual real world movement of the mechanism. In the case of an elevator Meters/Feet/Inches would be acceptable. For ThunderChickens codebase we tend to stick with Metric units. Bonus points if this is implemented through the use of the Java Units classes supported in WPILIB. This allows for very easy unit conversion and keeps the units consistent across the codebase.

### 3.3 Logging

Logging is the recording of telemetry data, such as motor, sensor, or robot state information to aid in the troubleshooting of the robot. When trying to understand why issues happened on the field, it is critical to have comprehensive logging to properly root cause the issues. The rule of thumb is to err on the side of more logging, we can always cut back or reduce logging in the future. To help get you thinking about what to long. Here's some of the primary data we want to log:
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

SysID or System Identification is a process in which the mechanism runs through a predefined routine to quantify the PID & Feedforward constants for the system. AdvantageKit logging has compatibility with SysID (details [here](https://docs.advantagekit.org/data-flow/sysid-compatibility)). We need to setup the SysID commands & expose them in the subsystem so that we can execute the characterization at a future time when we have the physical mechanism present.

<br>

## 4 Common System Interfaces

### 4.1 Constructor

Constructors 

### 4.2 Periodic

<br>

## 5 Subsystem Specific Implementations

### 5.1 Drivebase

#### 5.1.1 Swerve

##### 5.1.1.1 Theory of Operation

##### 5.1.1.2 Architecture

##### 5.1.1.3 Interfaces

##### 5.1.1.4 References

#### 5.1.2 Differential Drive

##### 5.1.2.1 Theory of Operation

##### 5.1.2.2 Architecture

##### 5.1.2.3 Interfaces

##### 5.1.2.4 References

### 5.2 Motor Based Motion

#### 5.2.1 Elevator - Linear motion.

The elevator subsystem from a hardware level has a few required hardware components as well as a variety of optional hardware to improve reliability and safety of the mechanism.

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

##### 5.2.2.1 Theory of Operation

##### 5.2.2.2 Architecture

##### 5.2.2.3 Interfaces

##### 5.2.2.4 References

#### 5.2.3 Pivot - Angular positioning.
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


##### 5.2.3.1 Theory of Operation

##### 5.2.3.2 Architecture

##### 5.2.3.3 Interfaces

##### 5.2.3.4 References

##### 5.2.3.5 Hood - Unique pivot.
- Absolute encoder or potentiometer to track hood position.
- Motor or servo to drive the hood.
    - Configure (idle mode, current limits).
    - Configure gear ratio.
    - If servo take in argument from constructor?
- Limit switch. Lower limit/home position.
- Speed controls:
    - Set-points. Via tunable PID. (Rotations at a minimum ideally real units).
    - Manual operation.
- Pass constants object into constructor.
- Logging via AdvantageKit.
- Method to check if hood is at a setpoint. onTarget.
- Motion Profiling (CTRE/Phoenix6 Motion Magic). MaxVelocity & MaxAcceleration.
- PID basic control. SVA (Static Friction, Velocity, Acceleration) or GVA (Gravity, Velocity, Acceleration) FeedForward Control Loop
- Optional HW limits. Enable / Disable this functionality.
    - Redundant limit switches (2 on top, 2 on bottom).
- Switch from Brake to Coast on disable and vise versa.
- Java Units Classes (Distance, Angle, Velocity, Etc.).
- Simulation of subsystem.
- Integration with SysID. Characterizes motor performance.


##### 5.2.3.6 Turret - Unique pivot
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

### 5.3 Servo Based Motion

##### 5.3.1 Theory of Operation

##### 5.3.2 Architecture

##### 5.3.3 Interfaces

##### 5.3.4 References

##### 5.3.5 Pivot / Hood

### 5.4 Pneumatic based

##### 5.4.1 Theory of Operation

##### 5.4.2 Architecture

##### 5.4.3 Interfaces

##### 5.4.4 References

##### 5.3.5 Gripper

### 5.5 Specialty

#### 5.5.1 Vision - Camera systems & pose estimation.

##### 5.5.1.1 Theory of Operation

##### 5.5.1.2 Architecture

##### 5.5.1.3 Interfaces

##### 5.5.1.4 References

#### 5.5.2 Led - Visual feedback system, CANdle.

##### 5.5.2.1 Theory of Operation

##### 5.5.2.2 Architecture

##### 5.5.2.3 Interfaces

##### 5.5.2.4 References

<br>

## 6 AdvantageKit IO Layer Framework

### 6.1 Subsystem: The base subsystem as is familiar to us.

### 6.2 Constants: The subsystem constants, again should be familiar.

### 6.3 SubsystemIO: The interface that defines the IO methods & data.

### 6.4 SubsystemIOSim: The simulation implementation of the above interface.

### 6.5 SubsystemIOHardware: The hardware implementation of the above interface.

<br>

## 7 Revision History


| Date     | Author | Description             |
| ---------- | ------ | ----------------------------------- |
| 06/13/2026 | AK   | Initial Release           |
| 08/26/2026 | AK   | Expansion of document         |
| 08/30/2026 | AK   | Broaden to include all subsystems   |
