# Subsystem Functional Specification

[toc]

## 1 Introduction

Subsystems are one of the primary building blocks we have that allow us to build modular & flexible FRC robot code.

:::{admonition} How to build what this spec describes
:class: tip
This document says **what** a subsystem must do. The [Training Materials](../training-materials/index.md)
teach **how**: start with [Building an IO Subsystem](../training-materials/advantagekit/io-subsystem.md),
which builds a complete arm that meets most of the pivot requirements below.
:::

:::{admonition} 2027 season
:class: important
The requirements here don't change for 2027, but the code does: SystemCore replaces the roboRIO,
WPILib packages are renamed, and with Commands v3 a subsystem implements `Mechanism` instead of
extending `SubsystemBase`. See [What's New for 2027](../training-materials/season-2027/whats-new.md).
:::

## 1.1 Checklists

As a part of this functional specification, there is a set of accompanying checklists that are intended to serve as a quick reference guide for students to check their development progress on a particular subsystem. These checklists should serve as nothing more than an overview or reference. Refer to this specification document for support in developing a given subsystem.

<br>

## 2 Subsystem Component Overview

Subsystems at their core are a hardware interface layer for linking motors, actuators, and/or sensors to a set of commands that the higher level control schemes can use to produce complex robot operations. 

### 2.1 Motion Components

Robots move, we have a few primary means of making them do so. THis section covers the most common.

### 2.1.1 Motors

Motors provide rotational motion, the specific mechanical design the motor gets integrated into defines how that motors rotational motion will translate to subsystem functionality. Motors a driven by a motor controller. There are two main vendors ThunderChickens uses: CTRE/WCP Kraken motors (our default), and REV NEO & Vortex motors where a mechanism already uses them. See [Phoenix 6 Basics](../training-materials/ctre/phoenix6.md) and [REV Motors](../training-materials/rev/revlib.md).

### 2.1.2  Pneumatics

Pneumatics are linear motion devices that have two states, extended and retracted. Pneumatics use compressed air to power them. We use unique hardware controllers to drive pneumatics. There are two types of solenoid, single action or double action. Single action use air pressure to extend out and a spring to retract. Double acting utilize air in both directions. A good way to remember them is single = spring.

### 2.1.3 Servos

Servos are small form factor motors with some unique features and limitations. Servos come in two primary types; standard & continuous rotation.
- Standard servos have a limited rotation, +- 90 to 180 degrees depending on the type. These servos have internal control loops that drive them to their position & hold it there.
- Continuous rotation servos act like standard motors, they have a forward & backward control. Sometimes these servos are variable speed and some are fixed. 

### 2.2 Sensors

Sensors are critical for our group, they are what enable us to perform complex robot operations. Below are some of the common sensors we utilize to provide feedback to the subsystems & robot.

### 2.2.1 Encoders

For mechanisms that don't naturally come to rest at their designated *home* position (zero (0) on the encoder). It is highly recommended to utilize an absolute encoder for reliable positioning. A CTRE CANCoder or a REV Through-bore would be suitable options for use. Often times with elevators gravity will bring them back to *home* because of this a relative encoder in the motor is sufficient. See [Encoders](../training-materials/sensors/encoders.md) for how each type works and how to seed a motor encoder from an absolute one.

### 2.2.2 Limit switches

Whenever we have a physical mechanism that moves we often want to protect the larger robot structure from the subsystem moving beyond it's limits. We can utilize software limits on these mechanisms however in the event of an encoder issue we often like to add hardware limit switches as well. These limit switches provide multiple benefits. Firstly, they cut power to motors in the direction of travel when limit switch is depressed. Second, they provide a known location for re-seeding the encoder position. Finally, having multiple redundant limit switches allows for the mechanical or electrical failure of one without compromising the system integrity. For an elevator it is typical to run upper & lower limit switches. See [Limit, Presence & Homing](../training-materials/sensors/presence.md) for wiring switches as hardware limits (CANdi or SPARK) and homing routines.

### 2.2.3 Cameras

We use cameras for two (2) primary means:
- AprilTag detection: This allows us to locate ourselves and field elements accurately on the field.
- ML/Color object detection: This aids us in navigating to game pieces scattered on the field.

See [Odometry & Vision](../training-materials/swerve/vision.md), [Object Detection](../training-materials/object-detection/overview.md), and [Vision Resources](../reference-materials/vision-resources.md).

### 2.2.4 Inertial Measurement Unit (IMU)

IMU or Inertial Measurement Unit which is a collection of: Accelerometer, which measures linear acceleration in each axis, Gyroscopes, which measure rotational speed and changes in orientation, and finally Magnetometers or Compass, which measures magnetic fields to help determine absolute heading. See [Gyros & IMUs](../training-materials/sensors/gyros.md).


### 2.3 Misc.

For the rest of the hardware that doesn't fit into the usual boxes.

### 2.3.1 Leds

Leds are visual indicators that help the drive team understand the current state of the robot. In addition to that we can use them to aid in debugging in the pits. The CANdle is the primary LED controller we use in FRC. See the [CANdle LED Plan](../candle-template.md).

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

For all ThunderChickens codebases, unless otherwise explicitly defined, logging is done through the [AdvantageKit](https://docs.advantagekit.org/) framework. Every sensor reading goes in the IO layer's inputs, and every goal, state, and decision is recorded as an output. See [Logging & Replay](../training-materials/advantagekit/logging-replay.md).

### 3.4 Simulation

Simulation provides a way for us to test our code before we have an actual robot to deploy to. With proper simulation support we can test core subsystem functionality, command sequencing, triggers, & more all before we have hardware in front of us. At a minimum every subsystem with at least one motor should be set up to simulate that motor. The simulation lives in the subsystem's simulation IO class (see [section 6](#6-advantagekit-io-layer-framework)): it runs a WPILib physics model (`DCMotorSim`, `ElevatorSim`, `SingleJointedArmSim`, or `FlywheelSim`) and feeds the result into the motor vendor's simulation (the TalonFX `SimState`, or `SparkMaxSim` for REV), so the real motor configuration and control loop run in simulation too. Beyond that simulation of additional hardware, such as limit switches, external encoders, etc. is encouraged time permitting. See [Simulation Strategy](../training-materials/simulation/strategy.md) and [Simulating Mechanisms](../training-materials/simulation/mechanisms.md).

### 3.5 SysID Characterization:

SysID or System Identification is a process in which the mechanism runs through a predefined routine to quantify the PID & Feedforward constants for the system. AdvantageKit logging has compatibility with SysID (details [here](https://docs.advantagekit.org/data-flow/sysid-compatibility)). We need to setup the SysID commands & expose them in the subsystem so that we can execute the characterization at a future time when we have the physical mechanism present. Building these commands into the subsystem allows us to easily run SysID tests once we have the functional robot in front of us. See [Tuning on the Robot](../training-materials/control/tuning.md).

### 3.6 Testing

Logic that doesn't touch hardware (unit conversions, setpoint math, state machines, target selection) should be written so it can be tested with JUnit unit tests in `src/test/java`, which run with `./gradlew test` or **WPILib: Test Robot Code**. With a simulation IO, the whole subsystem can be tested the same way: create it with its sim IO, run `periodic()` for a few simulated seconds, and check that it reached its setpoint. Unit tests catch sign and unit mistakes before they reach the robot. See [Unit Testing](../training-materials/testing/unit-testing.md) for how to test subsystems, teleop bindings, and autonomous routines.

<br>

## 4 Common System Interfaces

Subsystems implement SubsystemBase, this is a common java interface that all subsystems are built off of. Part of that means there's some methods we can extend to support our subsystem. This section goes into the common components across all subsystems defined in this specification.

### 4.1 Constructor

Constructors are the first block of code that runs when you create a java class object. This is where your initialization code is housed. In the case of subsystems this means some level of hardware initialization and configuration. Beyond hardware configuration we have to setup the subsystem to be ready to operate when the robot is enabled. This means setting initial setpoints, homing mechanisms, etc. With the AdvantageKit IO layer, the subsystem constructor receives its IO object and its constants object, and does not create hardware itself. `RobotContainer` decides which IO to create (hardware, simulation, or an empty one for log replay) based on the robot's mode. Hardware configuration (current limits, inversion, gains) lives in the hardware IO class's constructor. See [Building an IO Subsystem](../training-materials/advantagekit/io-subsystem.md).

### 4.2 Periodic

Periodic is a method called every cycle of the robot. This is typically a 20ms (0.02s) loop time. Think of the periodic function like an integrated `while(true)` loop. We typically put any reoccurring logging, checks, or calculations in this loop. With the IO layer, `periodic()` always starts by calling `io.updateInputs(inputs)` and `Logger.processInputs(...)`, so everything after it works from logged data. **Caution** should be taken to not put anything computationally heavy in the periodic loop as that can cause overruns & slow the system response down. This periodic method runs on both real and simulated robots.

### 4.3 Simulation Periodic 

Simulation periodic is a method much like the above periodic but it only runs when the robot is being simulated. This section is solely for calculations related to simulated operation of the subsystem. With the IO layer, the physics update belongs in the simulation IO's `updateInputs()` instead, so most of our subsystems leave `simulationPeriodic()` empty.

<br>

## 5 Subsystem Specific Implementations

Each subsystem below follows the same layout as the elevator: **Theory of Operation** (how the mechanism works), **Architecture** (what the code must do, split into core and advanced functionality), **Interfaces** (the methods and commands the subsystem provides), and **References**. Variations on a subsystem, such as a hood or turret on a pivot, list only what is different from their parent. Everything in sections 3 and 4 (constants, units, logging, simulation, SysID, testing) applies to every subsystem and isn't repeated.

### 5.1 Drivebase

All robots need to move on the field. So every robot has some sort of drivebase or drivetrain to provide locomotion to the robot. In FRC these days the predominate drivebase design is Swerve, which provides the most dynamic control of the robot. Swerve is also the most complex drive system. For simple robots we often will utilize a differential drive which is more in line with what you would find on an RC car. There are some other flavors of differential drive robots that we'll touch on briefly in that section. 

### 5.1.1 Swerve Drive

A swerve drive system is typically made up of four (4) wheel modules mounted in each corner of the drivebase. These wheel modules are able to drive both the wheel itself and pivot the angle / heading of the wheel. This control of each wheels heading when working in tandem with the rest of the wheel modules enables the robot to move vary dynamically on the field & enables the "front" of the robot to become variable. Each wheel module is made up of two (2) motors. One for angle adjustment, and one for drive. Along with the motors, each angle motor uses an absolute encoder for positioning of the module angle. In addition to the 4 modules, the swerve subsystem also is equipped with an IMU / Gyroscope which provides the heading information needed to calculate the wheel positions. Going forward we build swerve from CTRE's Tuner X swerve generator and the AdvantageKit TalonFX swerve template, not YAGSL. See [CTRE Swerve](../training-materials/swerve/ctre-swerve.md).

#### 5.1.1.1 Theory of Operation

The driver (or an autonomous path) asks for a **chassis speed**: forward velocity, sideways velocity, and rotational velocity. Swerve **kinematics** converts that one request into a speed and an angle for each of the four modules, based on where each module sits relative to the center of the robot. Each module then runs two control loops: a position loop on the steer motor to point the wheel, and a velocity loop on the drive motor to spin it.

Two details make swerve work smoothly:
- **Module optimization:** a wheel never turns more than 90° to reach a new angle. If the target is more than 90° away, the module turns the shorter way and drives the wheel backwards.
- **Cosine compensation:** while a module is still turning toward its target angle, its drive speed is scaled down by how far off it is, so the robot doesn't lurch sideways.

The reverse calculation, **odometry**, combines each module's measured wheel distance and angle with the gyro heading to track where the robot is on the field. Odometry drifts over time (wheels slip), so a **pose estimator** blends it with vision measurements (see 5.5.1). With CTRE hardware on a CAN FD bus, odometry is sampled at a high rate (250 Hz) on a separate thread for accuracy.

Teleop driving is usually **field relative**: pushing the stick away from the driver moves the robot away from the driver no matter which way the robot faces. This requires the gyro heading, and must be flipped for the red alliance because field coordinates always start at the blue alliance wall.

#### 5.1.1.2 Architecture

The core functionality of the swerve drive is as follows:
- Four (4) modules, each with a drive motor, a steer motor, and an absolute encoder. The default hardware is two (2) KrakenX60 (TalonFX) motors and a CTRE CANcoder per module, on a CANivore or CAN FD bus. Module positions, gear ratios, wheel radius, and encoder offsets are generated by the Tuner X swerve generator and kept in `TunerConstants`.

- Motors need to be explicitly configured, the same as any other mechanism: inversion, neutral mode (brake), current limits, and closed-loop gains. The drive motor's stator current limit should be set below the point where the wheels slip on carpet.

- The steer motor uses the CANcoder as its feedback sensor (`FusedCANcoder` with Phoenix Pro, `RemoteCANcoder` otherwise) so module angles are correct at power on without any homing.

- A gyroscope (CTRE Pigeon 2) mounted flat near the center of the robot provides heading.

- Kinematics and odometry. The subsystem tracks the robot pose using a `SwerveDrivePoseEstimator` and exposes it to the rest of the code.

- Field-relative teleop driving with joystick deadbands, input shaping (squared inputs), maximum speed scaling, and alliance flipping.

- Robot-relative velocity control for autonomous. PathPlanner's `AutoBuilder` must be configured with the subsystem's pose, reset, speed, and drive methods.

- A way to re-zero the heading from a driver button, and to reset the full pose.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Vision pose estimation. Accept timestamped vision measurements with standard deviations and fuse them into the pose estimator (see 5.5.1).

- Pose history. Provide the robot's estimated pose at a past timestamp (`sampleAt`) for latency compensation in vision and object detection.

- High frequency, time-synchronized odometry (250 Hz on CAN FD) on a separate thread, as implemented in the AdvantageKit TalonFX swerve template.

- Drive-at-angle / heading lock. The driver controls translation while the robot holds or turns to a heading (for example to aim at a target).

- X-lock. Turn all modules inward to resist being pushed while stopped.

- Characterization commands: SysID for the drive and steer motors, and a wheel radius characterization routine (spin in place and compare the gyro to wheel distance).

- Simulation of the drivebase, including module physics and a simulated gyro.

- Gyro disconnect fallback. If the gyro stops reporting, estimate heading from module motion and raise an alert.

#### 5.1.1.3 Interfaces

##### 5.1.1.3.1 Internal interfaces

- run velocity: Internal method that takes a robot-relative chassis speed, converts it to module states, optimizes them, and commands each module. Every drive command ends up here.
- update odometry: Internal method, called from periodic, that reads each module and the gyro and updates the pose estimator.

##### 5.1.1.3.2 External interfaces

- Command for field-relative joystick driving (default command).
- Command for driving at a heading while the driver translates.
- Command to stop and command to X-lock the modules.
- Command to re-zero the heading.
- Commands for SysID and wheel radius characterization.
- Getter for the robot pose, heading, and chassis speeds.
- Getter for the pose at a past timestamp.
- Method to reset the pose.
- Method to add a vision measurement.

#### 5.1.1.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.1 is on all types of drivebase, it does cover swerve briefly but focuses on older drive styles.
- [CTRE Swerve](../training-materials/swerve/ctre-swerve.md): our guide to the Tuner X generator and the AdvantageKit swerve template.
- [AdvantageKit TalonFX Swerve Template](https://docs.advantagekit.org/getting-started/template-projects/talonfx-swerve-template/): the base project for our drive code.
- [WPILib Swerve Drive Kinematics](https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/swerve-drive-kinematics.html)


### 5.1.2 Differential Drive

Differential drive is your typical drive system. Two (2) rows of parallel wheels, left and right. They can be commanded forward, backward, or turn left and right. This drive system is cheap, reliable, and simple to execute. The XRP robots we use in training implement a simple differential drive system. These differential drive systems often chain or gear together multiple motors on each side to increase the torque or power behind the wheels.

#### 5.1.2.1 Theory of Operation

A differential drive turns by driving its two sides at different speeds: equal speeds drive straight, opposite speeds spin in place, and anything in between drives an arc. It cannot move sideways. Differential kinematics converts a requested forward and rotational speed into a left and right wheel speed using the track width (the distance between the left and right wheels).

There are three common ways to map a controller to the drive:
- **Tank:** each joystick's Y axis drives one side.
- **Arcade:** one stick's Y axis sets forward speed and another axis sets turning.
- **Curvature:** like arcade, but the turn input sets how sharp an arc to drive, which feels more like a car at high speed.

Odometry works like swerve: wheel distance from each side's encoder plus the gyro heading gives the robot pose.

#### 5.1.2.2 Architecture

The core functionality of the differential drive is as follows:
- Motors for the left and right sides, a minimum of one (1) per side. Additional motors on a side follow the leader. Motors must be explicitly configured: inversion (one side is mirrored), neutral mode, current limits, and gear ratio.

- Encoders to measure each side's distance and velocity, in meters. The motors' integrated encoders are sufficient.

- A gyroscope (CTRE Pigeon 2) to track robot heading.

- Odometry using the encoders and gyro to track the robot pose.

- Manual driving with arcade control as the default, and tank or curvature as options. Joystick inputs need deadbands and input shaping.

- Velocity control of each side using PID and SVA feedforward, for autonomous paths.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Vision pose estimation using a `DifferentialDrivePoseEstimator`.

- Motion profiling of the wheel velocities to limit acceleration and reduce tipping.

- PathPlanner support configured for a differential drive.

- Simulation using `DifferentialDrivetrainSim`.

- Integration with SysID for left and right feedforward gains.

#### 5.1.2.3 Interfaces

##### 5.1.2.3.1 Internal interfaces

- drive wheel speeds: Internal method that takes left and right velocities and commands both sides. Every drive command ends up here.
- update odometry: Internal method, called from periodic, that updates the pose from the encoders and gyro.

##### 5.1.2.3.2 External interfaces

- Command for arcade driving (default command), and optionally tank and curvature.
- Command to follow robot-relative chassis speeds (for autonomous).
- Commands for SysID.
- Getter for the robot pose, heading, and wheel speeds.
- Method to reset the pose.

#### 5.1.2.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.1 is on all types of drivebase, lots of different differential drivebase examples.
- [XRP Lessons](../training-materials/xrp-training/xrp-lesson-1.md): our differential drive training robot.
- [WPILib Differential Drive Kinematics](https://docs.wpilib.org/en/stable/docs/software/kinematics-and-odometry/differential-drive-kinematics.html)

#### 5.1.2.5 Butterfly

Butterfly drive is a classic ThunderChickens drive system. Most recently we used it back in 2022.

This drive is a riff on the classic adding a pivoting wheel module in each corner. This wheel module integrates a traction wheel and an omni wheel into one module. With the control of a pneumatic solenoid we can switch between the traction and the omni balancing maneuverability and pushing power during a match. 

Additional configuration and methods are needed to setup the pneumatic system. In addition to the differential drive requirements above, a butterfly drive needs:
- Four (4) double-acting solenoids, one per corner module (see 5.4).
- A mode for the whole drivebase: **traction** (all traction wheels down, best for pushing), **omni** (all omni wheels down, easiest turning), and **split** (traction on one end, omni on the other).
- Commands to switch modes, and a getter for the current mode.
- Odometry that accounts for the mode: omni wheels slip sideways, so trust the gyro more and the encoders less while in omni.


### 5.2 Motor Based Motion

### 5.2.1 Elevator - Linear motion.

Elevators can be one of the most complicated mechanisms to program. The elevator subsystem from a hardware level has a few required hardware components as well as a variety of optional hardware to improve reliability and safety of the mechanism.

#### 5.2.1.1 Theory of Operation

Elevators are linear mechanisms, this means they move in one axis / direction. This is often vertical movement to pick up game elements from the ground and go score them. Elevators can have multiple stages, the more stages you have the higher maximum extension you can achieve, at the cost of a wider footprint. For FRC robots we typically have between 1 and 3 stages. The Elevator here is a single stage and has roughly 33" of vertical lift.

#### 5.2.1.2 Architecture

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

- Motion Profiling. This is a control method that defines maximum velocity and acceleration limits to the system. This is beneficial in that it allows for smoother operation of the mechanism. This reduces the maintenance required for the mechanism by reducing wear. Each motor supplier has their own implementation, along with WPILib proving options for on controller motion profiling. For CTRE/Phoenix6 its called Motion Magic while Rev calls it MaxMotion. See [Motion Magic](../training-materials/control/motion-magic.md).

- Expanded hardware limit switches. Often for mission critical systems we want to bake in extra redundancy, limit switches are another area where we often implement redundancy. The subsystem should be able to support two (2) lower limits as well as an additional two (2) upper limits.

- Elevator homing sequence Command. On initialization the elevator should have the capability to home itself. Run it down at a slow power level until it either hits a lower limit or the current spikes indicating the elevator has hit a physical barrier. See [Homing](../training-materials/sensors/presence.md#homing-zeroing-a-relative-encoder) for an example command.

#### 5.2.1.3 Interfaces

##### 5.2.1.3.1 Internal interfaces

- update PID/FF: Internal method to update the motor controller with the logged tunable values.
- drive functionality: Internal method to control the motor controller. This method should be the final step that checks HW/SW limits before commanding the motor. Commands should call this method to drive the mechanism, rather than attempting to command the motor directly.

##### 5.2.1.3.2 External interfaces

- Command for manual control of the elevator.
- Command for position setpoint control of the elevator.
- Commands for SysID.
- Command for homing the elevator.
- Getter if the elevator has reached its setpoint target.
- Getter for elevator limits.

#### 5.2.1.4 References

To improve your depth of knowledge on the construction or operation of elevator mechanisms in FIRST, the following resources provide a launching point.

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.2 is on elevator design & mechanical concepts.
- [Simple Elevator Visualizer](http://www.competitionrobotparts.com/elevator-visualizer/): Helpful in understanding the operating principles of an elevator.
- [WCP GrayT Elevator](https://wcproducts.com/collections/systems-structure/products/greyt-cascade-elevator): Product used in the competitive concept.
- [FIRST Education: Extenders](https://www.firstinspires.org/hubfs/lms/frc-guided-experience/m2/l4-extenders-lift-reach-climb-pdf.pdf?hsLang=en): Overview of different lift styles, including elevators.
- [Elevator Control Loop Tuning](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-elevator.html): Motion Profiler for Elevator applications.

#### 5.2.1.5 Climber - Linear with one way locking.

A climber pulls the robot up off the floor at the end of a match, and then has to hold it there after the robot is disabled. Most climbers are a winch, an elevator, or a telescoping tube driven by a motor, with a **ratchet** that lets the mechanism move in the lifting direction but locks it from moving back. That locking is what keeps the robot hanging when motor power is cut at the end of the match.

From a software perspective a climber is an elevator (5.2.1) with these differences:
- An actuator to engage and release the ratchet pawl: a servo (see 5.3) or a pneumatic solenoid (see 5.4). Which actuator is used is passed in through the constants object.
- While the ratchet is engaged, the motor must only drive in the lifting direction. The drive method must refuse to drive against the pawl, which would only stall the motor or damage the ratchet.
- Releasing the ratchet is never automatic. It needs an explicit command, and should be blocked while the robot is hanging.
- Climbing loads are much higher than elevator loads: use higher current limits, brake mode, and position or voltage control tuned under the robot's full weight.
- A lockout so the climb can't be started by accident before the endgame: for example, it requires two buttons together, or the remaining match time to be below a threshold.
- Limit switches or current detection at the fully-retracted (climbed) position so the motor stops when the robot is up.
- Logging of the ratchet state and the climb state (stowed, extended, climbing, locked).

#### 5.2.1.6 Telescoping Arm - Linear motion on a pivot.

A telescoping arm is an extension mounted on a pivot: the pivot sets the angle (see 5.2.3) and the extension sets the length. From a software perspective it is an elevator whose direction of travel changes, so it has these differences:
- Gravity feedforward depends on the pivot angle. Gravity pulls along the extension by `sin(angle)` (full force when vertical, none when horizontal), so the extension's kG is scaled by the sine of the pivot angle. The pivot's own gravity feedforward also grows as the arm extends.
- The maximum allowed extension can depend on the pivot angle: for example to stay inside the frame perimeter and height limits. The limit check needs the current pivot angle.
- Motion between positions has to be ordered: retract before rotating, then extend at the new angle. This belongs in a superstructure (see 5.6.1) when the pivot and extension are separate subsystems.
- The extension usually needs homing at power on (see 5.2.1), since it does not come to rest at a known position like a gravity-loaded elevator.

### 5.2.2 Flywheel - Velocity control.

A flywheel is a rotational mechanism that spins fast & maintains energy. The most common application of a flywheel in FRC is for shooter wheels. Anytime we have a shooter that has to bring a game object up to speed very quickly a flywheel is the key to doing it. From a software perspective a flywheel is just a velocity controlled motor. 

#### 5.2.2.1 Theory of Operation

A flywheel's job is to reach a target speed, hold it, and recover quickly after each game piece takes energy out of it. Because it has no position to hold and no gravity to fight, almost all of the control comes from **feedforward**: kS (the voltage to overcome friction) plus kV times the target velocity gets the wheel very close to the target speed. A small kP corrects the remaining error and speeds up recovery after a shot. kA helps the wheel follow quick changes in setpoint.

Velocity readings from an encoder are noisy, so the "at speed" check should use a filtered velocity and a tolerance, and often require the wheel to stay in tolerance for a short time before reporting ready. Shooters usually choose their speed from the distance to the target using a lookup table (`InterpolatingDoubleTreeMap`) built from testing.

Flywheels are usually left in **coast** mode so they spin down freely instead of braking hard.

#### 5.2.2.2 Architecture

The core functionality of the flywheel is as follows:
- Motor(s) to spin the flywheel. Support for single or dual motors (a follower, or two independent wheels for spin). For a *default* subsystem use the KrakenX60 (TalonFX).

- Motors must be explicitly configured: inversion, neutral mode (coast), current limits, and gear ratio.

- A relative encoder to measure flywheel velocity. The motor's integrated encoder is sufficient.

- Velocity setpoint control using PID and SVA (Static Friction, Velocity, Acceleration) feedforward, running on the motor controller (`VelocityVoltage` for CTRE). PID and feedforward constants are user tunable with `LoggedTunableNumber`.

- Manual operation: open-loop voltage control for testing and for failures on the field.

- A method to check if the flywheel is at its setpoint (onTarget), using a filtered velocity and a tolerance.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Support for an external encoder (REV Through Bore or CTRE CANcoder) when the motor is not directly coupled to the wheel.

- Filtering of the velocity feedback with a WPILib `LinearFilter`, and requiring the speed to be in tolerance for a set time before reporting ready.

- Setpoints defined in an enum or data class. This reduces the risk of typos or "mystery constants/doubles" in code.

- A distance-to-speed lookup table for shooters, with the table in constants.

- An idle speed between shots to shorten spin-up time, if the game and battery allow.

- Torque-based control (`TorqueCurrentFOC` with Phoenix Pro) for faster recovery after each shot.

#### 5.2.2.3 Interfaces

##### 5.2.2.3.1 Internal interfaces

- update PID/FF: Internal method to update the motor controller with the logged tunable values.
- drive functionality: Internal method to command the motor's velocity or voltage. Commands call this rather than the motor directly.

##### 5.2.2.3.2 External interfaces

- Command to spin to a velocity setpoint and keep spinning.
- Command to spin to a speed chosen from a supplier (for example a distance table).
- Command to stop (coast down).
- Command for manual control.
- Commands for SysID.
- Getter for the current velocity and whether it is at its setpoint.

#### 5.2.2.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.6.1 is on shooter flywheel design & mechanical concepts.
- [Feedforward](../training-materials/control/feedforward.md) and [PID Control](../training-materials/control/pid.md)
- [WPILib Flywheel Tuning](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-flywheel.html)


### 5.2.3 Pivot - Angular positioning.

Pivots can be integrated into a wide variety of applications. From a software perspective a pivot is an angular / rotational position control. A motor rotates to & holds at the position. Think of an elbow or a railroad crossing gate.

#### 5.2.3.1 Theory of Operation

A pivot (an arm, wrist, or intake deploy) rotates to an angle and holds it. Unlike an elevator, the force gravity applies changes with the angle: it is greatest when the arm is horizontal and zero when it points straight up or down. The gravity feedforward is therefore **kG × cos(angle)**, measured from horizontal. With CTRE this is the `Arm_Cosine` gravity type, which needs the motor to report the mechanism angle with 0 at horizontal.

Most pivots do not come to rest at a known position, so they need an absolute encoder (or a known start position against a hard stop) to know their angle at power on. The absolute encoder's position is used to seed the motor's relative encoder at startup.

Motion profiling (Motion Magic) is recommended for every pivot: it limits velocity and acceleration so the arm moves smoothly instead of jumping to the full output of the PID.

#### 5.2.3.2 Architecture

The core functionality of the pivot is as follows:
- Motor(s) to drive the pivot. Support for single or dual motors. For a *default* subsystem use the KrakenX60 (TalonFX).

- Motors must be explicitly configured: inversion, neutral mode, current limits, gear ratio (`SensorToMechanismRatio`), and the gravity type.

- An absolute encoder to track the arm position (CTRE CANcoder or REV Through Bore), used to seed the motor's relative encoder at least at startup. The subsystem should also support starting against a known hard stop when no absolute encoder is present.

- Position setpoint control with PID and GVA (Gravity, Velocity, Acceleration) feedforward running on the motor controller, with user tunable gains. Setpoints are in mechanism units (rotations at a minimum, ideally degrees through the Java units classes).

- Software limits (minimum and maximum angle) defined as constants. Any attempt to move past a limit cuts power in that direction.

- Manual operation for testing and field failures. Hardware limits are respected; software limits may be ignored.

- A method to check if the pivot is at its setpoint (onTarget).

- Brake mode while enabled and coast mode while disabled, so the pit crew can move the arm by hand.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Motion profiling (Motion Magic), with MaxVelocity and MaxAcceleration as tunable constants.

- Optional hardware limit switches with a way to enable or disable the checks, including redundant switches (two (2) at each end).

- Fused absolute encoder feedback (`FusedCANcoder` with Phoenix Pro) instead of seeding only at startup.

- A homing command against a hard stop using current detection, for pivots without an absolute encoder.

#### 5.2.3.3 Interfaces

##### 5.2.3.3.1 Internal interfaces

- update PID/FF: Internal method to update the motor controller with the logged tunable values.
- drive functionality: Internal method that checks the hardware and software limits and then commands the motor. Commands call this rather than the motor directly.
- seed encoder: Internal method that sets the motor's position from the absolute encoder.

##### 5.2.3.3.2 External interfaces

- Command for manual control of the pivot.
- Command for position setpoint control of the pivot (move and finish when there).
- Command to follow a changing angle from a supplier.
- Commands for SysID.
- Command for homing (if used).
- Getter if the pivot has reached its setpoint target.
- Getter for the current angle and the pivot limits.

#### 5.2.3.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.3 is on arm design & mechanical concepts.
- [Building an IO Subsystem](../training-materials/advantagekit/io-subsystem.md): a complete pivot (arm) subsystem that meets the core requirements above.
- [Motion Magic](../training-materials/control/motion-magic.md)
- [WPILib Arm Tuning](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/tuning-vertical-arm.html)

#### 5.2.3.5 Hood - Unique pivot.

A hood is a variation on a pivot used primarily to set the launch angle of a shooter: the game piece rides along the hood as it leaves the flywheel, so the hood angle sets how steep the shot is. Hoods have a small range of motion and low inertia, and are usually set from the same distance lookup table as the flywheel speed.

Differences from a pivot:
- Driven by a motor or a servo (see 5.3.5). If a servo is supported, the actuator type is passed in through the constants object.
- Usually no absolute encoder. The hood homes to a lower limit switch or a hard stop (by current detection) at startup and its encoder is zeroed there.
- Gravity has little effect, so a simple position PID (with kS) is usually enough. Motion profiling is optional.
- Software limits are critical: the range is small, and driving past it can jam the shooter.
- A method to set the angle from a supplier, so the hood can follow a distance-based shot table continuously.

#### 5.2.3.6 Turret - Unique pivot

A turret rotates a mechanism (usually a shooter) around a vertical axis so it can aim without turning the whole robot. Turrets have limited travel because of wiring and mechanical stops, typically less than one full turn, or a little more than one turn with a cable chain.

Differences from a pivot:
- No gravity feedforward: the turret rotates horizontally.
- Position tracking must be correct at power on and through the whole range. Options are a single absolute encoder (works if the turret turns less than one encoder rotation), two absolute encoders on gears with different tooth counts using the Chinese Remainder Theorem (CRT) to find a unique position over several turns, or a 10-turn potentiometer.
- Choosing a target within the limits. When aiming at a field angle, the turret must choose between equivalent angles (for example +170° and -190°) the one that is inside its travel. If neither is reachable, it parks at the nearest limit and reports that it is **not** on target.
- Field-relative aiming. The turret angle is the target's field angle minus the robot heading, so the turret must also counter the robot's rotation. Feeding the robot's angular velocity forward makes tracking much tighter while the robot turns.
- Logging of the requested field angle, the chosen turret angle, and whether the target was reachable.

References:

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.7 is on turret design & mechanical concepts.
- [Encoders](../training-materials/sensors/encoders.md): absolute encoders and seeding.


### 5.2.4 Roller - Open velocity control. 

A roller subsystem is one of the simplest mechanisms to program. It is simply a wheel, roller, belt, conveyor, etc. that runs in an open loop mode. Where we set the speed of the roller between -100% to 100%. This can be via applied output or voltage. We don't need feedback based control loops or any external sensors.

#### 5.2.4.1 Theory of Operation

A roller only needs to spin in one direction to take a game piece in and the other direction to push it out. Exact speed rarely matters, so open-loop **voltage** control is used: a voltage gives the same speed whether the battery is full or sagging, which plain duty-cycle (percent output) does not.

Even without sensors, the motor's current tells us a lot: current rises sharply when the roller grabs a game piece or jams. Current detection is the cheapest way to know a roller has a piece.

#### 5.2.4.2 Architecture

The core functionality of the roller is as follows:
- Motor(s) to drive the roller. Support for single or dual motors. For a *default* subsystem use the KrakenX44 or KrakenX60 (TalonFX).

- Motors must be explicitly configured: inversion, neutral mode, current limits, and gear ratio.

- Open-loop voltage control with named speeds (intake, eject, hold) defined as constants or an enum.

- Manual operation for testing.

- Logging of applied voltage, current, and velocity.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Game piece detection from motor current (and velocity dropping) with a debounce, for rollers without a sensor.

- Velocity control when the speed matters (for example feeding a shooter at a consistent speed).

- A low "hold" voltage to keep a game piece from sliding out.

- Integration with SysID when velocity control is used.

#### 5.2.4.3 Interfaces

##### 5.2.4.3.1 Internal interfaces

- drive functionality: Internal method to command the motor's voltage. Commands call this rather than the motor directly.

##### 5.2.4.3.2 External interfaces

- Command to run the roller at a named speed while the command runs, then stop.
- Command for manual control.
- Getter for whether the roller has a game piece (if detection is used).

#### 5.2.4.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 5.5 is on intake roller design & mechanical concepts.

#### 5.2.4.5 Intake - Roller with game piece detection.

An intake picks game pieces up off the floor. It is a roller (5.2.4), often mounted on a deploy mechanism that lowers it outside the frame perimeter: a pivot (5.2.3) or a pneumatic cylinder (5.4). The intake and its deploy may be one subsystem or two coordinated by a superstructure (5.6.1).

Differences from a roller:
- A game piece sensor so the intake knows when it has a piece: a beam break, a CTRE CANrange (time of flight), or current detection. See [Limit, Presence & Homing](../training-materials/sensors/presence.md).
- The intake command ends, or the roller drops to a hold speed, as soon as a piece is detected.
- Intaking is blocked when the robot is already holding the maximum number of game pieces allowed.
- If there is a deploy mechanism, the rollers only run while deployed, and the intake stows when the intake command ends.
- An eject command that runs the rollers in reverse.
- Logging of the game piece sensor and the deploy state.
- Optionally, trigger a short controller rumble or LED pattern when a piece is acquired (see 5.5.2).

#### 5.2.4.6 Indexer / Conveyor - Game piece staging.

An indexer (or conveyor, hopper, feeder) moves game pieces from the intake to the scoring mechanism and holds them in between. It is one or more rollers or belts with sensors at key positions.

Differences from a roller:
- Game piece sensors at the positions that matter, typically where pieces enter and where they wait before the shooter.
- Staging: pieces are moved to the waiting position and stopped there, so they don't touch the flywheel until it's time to shoot.
- Feeding only when the scoring mechanism is ready (for example the flywheel and hood are at their setpoints). This check belongs in the command that feeds, or in a superstructure (5.6.1).
- Counting the number of pieces held, when the game allows more than one.
- Jam detection: high current with low velocity for a set time. Respond by reversing briefly, then trying again.
- Logging of each sensor, the piece count, and the indexer state.


### 5.3 Servo Based Motion

Servos are small motors with built-in control, introduced in section 2.1.3. They are useful for light, low-force jobs: releasing a latch, flipping a small flap, or setting a hood angle.

#### 5.3.1 Theory of Operation

A **standard servo** receives a target position as a pulse signal and drives itself there using its own internal control loop. The robot code only commands the position; standard servos do not report where they actually are. A **continuous rotation servo** instead treats the same signal as a speed and direction.

Because there is no feedback, the code has to **assume** where the servo is. A command that moves a servo should wait long enough for it to arrive, based on the servo's speed and how far it has to go. Servos are also weak: if the mechanism binds or carries load, the servo stalls short of its target without the code knowing.

Servos are controlled from a PWM port, or over CAN through a REV Servo Hub. Servo positions should be calibrated once on the real mechanism (the commanded value that matches each end of travel) and stored as constants.

:::{admonition} 2027
:class: important
WPILib 2027 removes the `Servo` class, and SystemCore has no dedicated PWM header (its SmartIO ports can be configured for PWM). Plan on CAN servo controllers such as the REV Servo Hub, and check the 2027 WPILib docs.
:::

#### 5.3.2 Architecture

The core functionality of the servo subsystem is as follows:
- A servo on a PWM port or a Servo Hub channel, with the port/channel in the constants object.

- Named positions (for example locked and released) defined as calibrated constants or an enum.

- An estimate of when the servo has arrived, based on the commanded change and a travel time constant. This acts as the "onTarget" check.

- Logging of the commanded position and the assumed state.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Support for continuous rotation servos with speed commands.

- Position feedback where accuracy matters: a servo with a feedback output, or a limit switch at the important position.

- Simulation that moves the simulated position toward the command at the servo's rated speed.

#### 5.3.3 Interfaces

##### 5.3.3.1 Internal interfaces

- set position: Internal method that commands the servo and records the time and target, for the arrival estimate.

##### 5.3.3.2 External interfaces

- Command to move to a named position and finish once it has had time to arrive.
- Getter for the commanded position and whether it has arrived.

#### 5.3.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 4.2 is on servos.

#### 5.3.5 Pivot / Hood

A small hood or flap can be driven directly by a servo instead of a motor (see 5.2.3.5). Differences from a motor-driven hood:
- There is no encoder or PID; the servo holds its own position.
- The commanded value is mapped to an angle with a calibrated linear conversion (the commanded value at each end of travel), so the rest of the code still works in degrees.
- The angle is limited in software to the calibrated range.
- Use a servo only if the hood carries little load. If shots move the hood, use a motor.

### 5.4 Pneumatic based

Pneumatics, introduced in section 2.1.2, give fast, strong, two-position motion: extended or retracted. They are a good fit for mechanisms that only need two states, at the cost of the weight of a compressor, tank, and tubing.

#### 5.4.1 Theory of Operation

A **compressor** fills a storage tank with air, and a **pneumatics hub** (REV Pneumatic Hub, or CTRE Pneumatics Control Module) switches the compressor and the solenoid valves. Each **solenoid valve** directs air to a cylinder:
- A **single-acting** (single solenoid) setup uses air to extend and a spring to retract. When power or air is lost, it returns to its spring position.
- A **double-acting** (double solenoid) setup uses air both ways and stays where it was last put when power is lost.

The compressor runs automatically to keep the tank pressure in range. With an analog pressure sensor on the REV Pneumatic Hub, the code can read the actual pressure and set the on/off pressure thresholds. Cylinders don't report their position unless a sensor (such as a magnetic reed switch on the cylinder) is added.

#### 5.4.2 Architecture

The core functionality of the pneumatic subsystem is as follows:
- Solenoid(s) created on the correct hub type and channels, with the hub type and channels in the constants object.

- A defined state for each mechanism (for example extended or retracted) set explicitly at startup.

- Methods to set and to toggle the state, and a getter for the commanded state.

- Compressor control left in its automatic (closed-loop) mode, with the pressure logged if a sensor is present.

- Logging of each solenoid's commanded state.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Analog pressure sensor support, with pressure thresholds as constants and an alert when pressure is too low to actuate.

- Position feedback from reed switches, so commands can wait until the cylinder has actually moved.

- Disabling the compressor during critical moments (for example while climbing) to save battery voltage.

- A choice of fail-safe state: decide what the mechanism should do when the robot is disabled or loses air, and pick single- or double-acting to match.

- Simulation using WPILib's pneumatics simulation classes, with a delay to model travel time.

#### 5.4.3 Interfaces

##### 5.4.3.1 Internal interfaces

- set state: Internal method that commands the solenoid. Commands call this rather than the solenoid directly.

##### 5.4.3.2 External interfaces

- Command to extend, command to retract, and command to toggle.
- Getter for the commanded state (and the sensed state, if a sensor is present).
- Getter for the system pressure (if a sensor is present).

#### 5.4.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 6.6 is on pneumatics.
- [WPILib Pneumatics](https://docs.wpilib.org/en/stable/docs/software/hardware-apis/pneumatics/index.html)

#### 5.4.5 Gripper

A gripper (claw) grabs and releases a game piece with a pneumatic cylinder. Differences from the general pneumatic subsystem:
- A game piece sensor so the code knows when to close, and whether it is holding a piece.
- An optional automatic close when a piece is detected, with a short delay so the piece is fully inside.
- A defined fail-safe state. Usually the gripper should keep holding a piece when the robot is disabled or loses air, which favors a double-acting cylinder or a single-acting cylinder whose spring closes it.
- Commands to grab and release, and a getter for whether a piece is held.

### 5.5 Specialty

### 5.5.1 Vision - Camera systems & pose estimation.

Vision uses cameras to find AprilTags on the field and work out where the robot is. Cameras connect to a coprocessor running PhotonVision, which does the image processing and sends results to the robot code over the network. See [Odometry & Vision](../training-materials/swerve/vision.md).

#### 5.5.1.1 Theory of Operation

Each AprilTag is at a known, published position on the field. When a camera sees a tag, PhotonVision can calculate where the camera must be to see the tag that way. With several tags in view at once (**multi-tag**), the result is much more accurate. Knowing exactly where the camera is mounted on the robot (the **robot-to-camera transform**) turns the camera's position into the robot's position.

Vision measurements are not perfect. A single tag can match two different poses (**ambiguity**), far-away tags are less accurate, and a frame is a few tens of milliseconds old by the time the robot receives it. The robot's pose estimator (see 5.1.1) blends each vision measurement with odometry, weighted by how much it trusts the measurement (its **standard deviations**), at the timestamp the frame was captured.

#### 5.5.1.2 Architecture

The core functionality of the vision subsystem is as follows:
- One or more cameras, each with a name matching PhotonVision and a robot-to-camera transform, defined in the constants object. Transforms come from CAD and are verified on the robot.

- Pose estimation for each camera using PhotonLib's pose estimator, with multi-tag estimation on the coprocessor as the primary strategy.

- Every unread result is processed each loop, not just the newest one.

- Filtering that rejects bad measurements: high ambiguity on single-tag estimates, tags beyond a maximum distance, and poses outside the field or off the floor.

- Each accepted measurement is sent to the drivebase's pose estimator with its timestamp and standard deviations.

- An alert when a camera is disconnected.

- Logging of every accepted and rejected pose separately, along with the tags used.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Standard deviations that scale with distance and the number of tags, so close multi-tag estimates are trusted more.

- Simulation using PhotonLib's `VisionSystemSim` to generate detections from the simulated robot pose.

- Targeting data for aiming (the angle to a specific tag) in addition to pose estimation.

- Using the coprocessor's object detection pipeline (see 5.5.1.5).

#### 5.5.1.3 Interfaces

##### 5.5.1.3.1 Internal interfaces

- process results: Internal method, called from periodic, that turns each camera's unread results into filtered pose measurements.

##### 5.5.1.3.2 External interfaces

- A consumer passed in through the constructor that receives each accepted measurement (pose, timestamp, standard deviations), usually the drivebase's add vision measurement method.
- Getter for whether each camera is connected.
- Getter for the latest target information, if targeting is used.

#### 5.5.1.4 References

- [NASA Robotics Design Guide](https://robotics.nasa.gov/nasa-rap-robotics-design-guide/): Section 6.4 is on cameras and vision concepts.
- [PhotonVision](https://photonvision.org/): Primary computer vision system utilized by the ThunderChickens.
- [Cluckipedia Vision Reference](https://cluck.thunderchickens.org/en/latest/reference-materials/vision-resources.html)
- [Odometry & Vision](../training-materials/swerve/vision.md)

#### 5.5.1.5 Object Detection - Game piece tracking.

Object detection uses a neural network on the coprocessor's NPU to find game pieces in a color camera's image. PhotonVision reports each detection's angles, class, and confidence. See the [Object Detection](../training-materials/object-detection/overview.md) section for training the model and the full robot code.

Differences from AprilTag vision:
- A color camera, usually mounted low and angled down toward the intake.
- Detections give angles, not positions. Each detection is projected onto the floor using the camera transform and the robot's pose **at the frame's timestamp** to get the piece's field position.
- A tracker that remembers pieces across frames: it smooths their positions, confirms a piece only after several sightings, and forgets pieces that haven't been seen recently.
- Queries for commands: whether a piece is confirmed, whether one is within a distance, the best piece to collect, and the piece nearest a point (to stay locked on one target).
- Logging of the raw detections, so tracker changes can be tested with log replay, and of the confirmed piece positions for display in AdvantageScope.
- Unit tests for the projection and tracker logic.

### 5.5.2 Led - Visual feedback system, CANdle.

LEDs tell the drive team what the robot is doing at a glance, and help the pit crew see problems quickly. The CANdle is our primary LED controller. Our standard colors and patterns are defined in the [CANdle LED Plan](../candle-template.md).

#### 5.5.2.1 Theory of Operation

The CANdle drives a strip of addressable LEDs over CAN: each LED can be set to its own color, and the CANdle can run built-in animations (strobe, rainbow, fade, and more) on its own. Groups of LEDs can be addressed as segments, so different parts of the strip can show different things.

At any moment several things may want to control the LEDs: a fault, the robot being disabled, a game piece being held, the robot lining up to score. The LED subsystem decides which one wins using a **priority**: for example faults first, then driver-requested signals, then game state, then an idle pattern. Each state only says what it wants; the LED subsystem chooses what to show.

#### 5.5.2.2 Architecture

The core functionality of the LED subsystem is as follows:
- A CANdle with its CAN ID, LED count, and segment layout in the constants object.

- The CANdle explicitly configured: LED strip type, brightness, and what to show when the robot code is not running.

- Named states (an enum) mapped to colors and patterns from the CANdle LED Plan.

- A priority order for the states, so the highest-priority active state is shown.

- Status shown while disabled: for example whether the robot is ready to enable and which alliance it is on.

- Logging of the state being shown.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Segments that show different information at the same time (for example a fault on one side and game state on the other).

- Triggers that set LED states automatically from other subsystems (game piece held, at target, vision connected).

- Reduced brightness when battery voltage is low.

- Simulation that logs the state so it can be viewed in AdvantageScope.

#### 5.5.2.3 Interfaces

##### 5.5.2.3.1 Internal interfaces

- apply state: Internal method that sends the colors or animation for the chosen state to the CANdle, only when the state changes.

##### 5.5.2.3.2 External interfaces

- Command to request a state while the command runs (for example while a button is held).
- Command to request a state for a set time (for example a short flash when a piece is acquired).
- Getter for the state being shown.

#### 5.5.2.4 References

- [CTRE CANdle Product Page](https://store.ctr-electronics.com/products/candle?srsltid=AfmBOor_ptYSzTNh7f-S45ZTQ4HopVHMj4pnIkfMbmXCX_2FiTsQ6D2e)
  - [User Manual](https://ctre.download/files/user-manual/CANdle%20User's%20Guide.pdf)
  - [Phoenix6 Documentation](https://v6.docs.ctr-electronics.com/en/latest/docs/hardware-reference/candle/index.html)
  - [Bring Up Guide](https://v5.docs.ctr-electronics.com/en/stable/ch12b_BringUpCANdle.html)
- [CANdle LED Plan Template](https://cluck.thunderchickens.org/en/latest/candle-template.html)
- [Hardware Status LEDs](../hardware-status-lights.md)

### 5.6 Robot Coordination

### 5.6.1 Superstructure - Coordinating multiple mechanisms.

A superstructure coordinates several mechanisms that have to move together, such as an elevator, an arm on the elevator, and an intake. Each mechanism is still its own subsystem with its own spec; the superstructure decides **what order** they move in and **when** it is safe to do the next step.

#### 5.6.1.1 Theory of Operation

Mechanisms that share space can collide: an arm may hit the bumpers if it swings down while the elevator is low, or an intake may hit the arm if both deploy at once. A superstructure avoids this with **states**: named configurations of the whole robot (stowed, intaking, scoring high, climbing). Moving from one state to another follows a defined sequence of steps, each waiting until the previous mechanism is safely in place.

The superstructure owns these transitions so that driver buttons and autonomous routines can ask for a state ("score high") without knowing how to get there safely.

#### 5.6.1.2 Architecture

The core functionality of the superstructure is as follows:
- An enum of robot states, each with the setpoint of every mechanism involved.

- Commands that move between states in a safe order, requiring every subsystem they move.

- Safety rules defined as constants (for example the minimum elevator height before the arm may rotate below horizontal), checked before each step.

- A getter for the current state and the goal state.

- Logging of the goal state, the current state, and each transition.

To expand the capabilities and usability of our subsystems there are a number of improvements we can make beyond the core functionality. They are as follows:

- Shortest safe path between any two states, instead of always passing through stow.

- Interrupting a transition partway through and continuing from wherever the mechanisms are.

- Readiness checks for scoring (for example the flywheel and hood are on target and the robot is aligned), used by triggers, LEDs, and autonomous.

- Visualization of all mechanisms together in AdvantageScope (a `Mechanism2d` or an articulated 3D robot model).

- Unit tests of the transition logic and safety rules, using each subsystem's simulation IO.

#### 5.6.1.3 Interfaces

##### 5.6.1.3.1 Internal interfaces

- is safe: Internal method that checks the safety rules for the next step against the mechanisms' measured positions.

##### 5.6.1.3.2 External interfaces

- Command to go to a state, finishing when every mechanism has arrived.
- Getter for the current state, the goal state, and whether the robot is ready to score.

#### 5.6.1.4 References

- [Command Composition](../training-materials/command-based/composition.md): sequences, parallel groups, and deadlines used to build transitions.
- [Triggers](../training-materials/command-based/triggers.md)

<br>

## 6 AdvantageKit IO Layer Framework

Every ThunderChickens subsystem is split into the files below. The subsystem's logic only talks to the `SubsystemIO` interface, so the same subsystem runs on the robot, in simulation, and in log replay. The full walkthrough is [Building an IO Subsystem](../training-materials/advantagekit/io-subsystem.md), and [Why an IO Layer?](../training-materials/advantagekit/why-io.md) explains the reasons.

### 6.1 Subsystem: The base subsystem as is familiar to us.

The logic: state, setpoints, safety checks, and command factories. Its `periodic()` starts with `io.updateInputs(inputs)` and `Logger.processInputs("Name", inputs)`. It never imports vendor hardware classes.

### 6.2 Constants: The subsystem constants, again should be familiar.

The constants class from [section 3.1](#31-constants), passed in through the constructor.

### 6.3 SubsystemIO: The interface that defines the IO methods & data.

An interface with an `@AutoLog` inputs class (every sensor reading, plus `connected`, current, and temperature) and `default` methods for each output (`setPosition`, `setVoltage`, `setBrakeMode`, ...). AdvantageKit generates a `SubsystemIOInputsAutoLogged` class from it at build time.

### 6.4 SubsystemIOSim: The simulation implementation of the above interface.

Runs a physics model and fills in the same inputs. For CTRE hardware, the simulation IO usually extends the hardware IO and drives the TalonFX simulation state, so the real configuration and control loop are tested too.

### 6.5 SubsystemIOHardware: The hardware implementation of the above interface.

Creates and configures the real devices and reads them into the inputs. Name it after the hardware it uses, for example `ArmIOTalonFX` or `ArmIOSparkMax`, so a mechanism can have more than one. See the [Code Standards](../reference-materials/code-standards.md#naming).

### 6.6 References

- [AdvantageKit Website](https://docs.advantagekit.org/)
- [AdvantageScope Website](https://docs.advantagescope.org/)

<br>

## 7 Glossary

[**A**](#a) | [**B**](#b) | [**C**](#c) | [**D**](#d) | [**E**](#e) | [**F**](#f) | [**G**](#g) | [**H**](#h) | [**I**](#i) | [**J**](#j) | [**K**](#k) | [**L**](#l) | [**M**](#m) | [**N**](#n) | [**O**](#o) | [**P**](#p) | Q | [**R**](#r) | [**S**](#s) | [**T**](#t) | [**U**](#u) | [**V**](#v) | [**W**](#w) | X | Y | Z

### A

#### Absolute Encoder
An encoder that remembers its position after loss of power. Record an initial zero (0) reference point. [CANCoder](#cancoder) and [Through-bore](#through-bore-encoder) are both common types of absolute encoders.

#### AdvantageKit
A logging and telemetry framework developed and popularized by Mechanical Advantage. Primarily used by ThunderChickens for the logging utilities. [AdvantageKit Website](https://docs.advantagekit.org/)

#### AdvantageScope
A visualization and analysis tool used to review data recorded by AdvantageKit & other logging methods. [AdvantageScope Website](https://docs.advantagescope.org/)

#### AprilTag
A visual fiducial marker used by robot vision systems for localization and field positioning. [More AprilTag information.](https://docs.wpilib.org/en/stable/docs/software/vision-processing/apriltag/apriltag-intro.html)

### B

#### Beam Break
A sensor made of a light emitter and receiver. An object passing between them blocks the beam and is detected. Commonly used to detect game pieces in an [Intake](#intake) or [Indexer](#indexer).

### C

#### CAN Bus
A communication network used by robot devices such as motor controllers, sensors, and LED controllers.

#### CANCoder
A [CTRE](#ctre) absolute encoder commonly used for rotational position feedback. This is a [CAN](#can-bus) based sensor.

#### CANdle
A [CTRE](#ctre) LED controller used to manage robot lighting and visual feedback systems. This is a [CAN](#can-bus) based sensor.

#### Characterization
The process of measuring a mechanism's behavior to derive control-loop parameters such as PID or Feedforward constants.

#### Chinese Remainder Theorem (CRT)
A math technique that finds a unique position over several turns from two [Absolute Encoders](#absolute-encoder) geared at different ratios. Used to track a [Turret](#turret) through more than one encoder rotation.

#### Co-processor
A secondary computer used for some purpose built compute task on the robot. Typically for vision processing.

#### Compressor
The pump that fills a pneumatic system's storage tank with compressed air. See [Pneumatics](#pneumatics).

#### Constructor
The initialization method of a Java class where subsystem hardware configuration, setup, and initialization occur.

#### Continuous Rotation Servo
A servo that behaves like a standard motor, rotating continuously in either direction rather than holding a specific position.

#### CTRE
[Cross The Road Electronics](https://store.ctr-electronics.com/), a manufacturer of FRC control system components including TalonFX controllers, CANcoders, and CANdles.

### D

#### DCMotorSim
A WPILib simulation utility used to model motor behavior in robot simulations.

#### Differential Drive
A drivetrain consisting of independently controlled left and right wheel sets used for basic robot movement. Think tank drive or RC car.

#### Double-Acting Solenoid
A pneumatic actuator configuration that uses air pressure for both extension and retraction.

#### Driver Station
The FRC Driver Station is a combination of laptop, game controllers, and software. When combined together to control the robot. For software information see [Driver Station](https://docs.wpilib.org/en/stable/docs/software/driverstation/index.html) on the WPILib documentation.

### E

#### Elevator
A linear motion subsystem that moves linearly, commonly used to lift or position game elements.

#### Encoder
A sensor that measures rotational position and velocity. Motors have an integrated [Relative Encoder](#relative-encoder).

### F

#### Feedforward Controller
A control strategy that predicts the output required to achieve desired motion, often combined with PID control.

#### Flywheel
A rotational mechanism used to store kinetic energy and provide consistent velocity, commonly used in shooter systems.

#### FRC
[FIRST Robotics Competition](https://www.firstinspires.org/programs/frc/), the robotics program for which the subsystem architecture is intended.

### G

#### Gear Ratio
The relationship between motor rotation and mechanism movement. Programming uses this value for unit conversion to go from [Rotations](#rotations) to a real world unit.

#### Gripper
A mechanism that grabs and releases a game piece, often with a pneumatic cylinder.

#### Gyroscope
A sensor that measures rotational velocity and orientation changes. We use these to track the heading of the robot.

### H

#### Hardware Limit Switch
A physical switch used to prevent a mechanism from moving beyond safe operating limits.

#### Home Position
A known reference position used to establish subsystem zero (0).

#### Homing
The process of moving a mechanism to a known position to establish accurate position tracking. Typically encoders are zeroed after homing.

#### Hood
A small-range [Pivot](#pivot) that sets the launch angle of a shooter.

### I

#### IMU (Inertial Measurement Unit)
A sensor package consisting of accelerometers, gyroscopes, and often magnetometers used to track robot orientation and motion.

#### Indexer
A mechanism of rollers or belts that moves game pieces from the [Intake](#intake) to the scoring mechanism and holds them in between. Also called a conveyor, hopper, or feeder.

#### Intake
A mechanism that picks game pieces up off the floor, usually rollers on a deploy mechanism.

#### Interface
A software component defining common methods and data interactions between subsystem layers.

### J

#### Java Units Classes
WPILib-provided classes that enforce consistent engineering units such as meters, degrees, and velocities.

### K

#### Kinematics
The math that converts between the robot's overall motion (chassis speeds) and each wheel's speed and angle.

#### KrakenX44
High-performance motors developed by [WestCoast Products](#westcoast-products). Powered by the [TalonFX](#talonfx). See [product page](https://wcproducts.com/products/kraken) for more information. 

#### KrakenX60
High-performance motors developed by [WestCoast Products](#westcoast-products). Powered by the [TalonFX](#talonfx). See [product page](https://wcproducts.com/products/kraken) for more information.

### L

#### LED
Light Emitting Diode devices used for driver feedback, robot diagnostics, and status indication.

#### Limit Switch
A sensor that detects a mechanism reaching a predefined physical boundary. See [Hardware Limit Switch](#hardware-limit-switch).

#### Logging
The process of recording robot telemetry and operational data for analysis and debugging.

#### LoggedTunableNumber
A utility that allows a double to be modified over [NetworkTables](#networktables) often via [AdvantageScope](#advantagescope). The changes to these values is logged for future analysis.

### M

#### Magnetometer
A sensor used to measure magnetic fields, can be used to determine an accurate heading.

#### MaxMotion
REV Robotics motion profiling implementation. See [MAXMotion Position Control](https://docs.revrobotics.com/revlib/spark/closed-loop/maxmotion-position-control) for more information.

#### Motion Magic
CTRE's onboard motion profiling control mode. See [Motion Magic Controls](https://v6.docs.ctr-electronics.com/en/2025/docs/api-reference/device-specific/talonfx/motion-magic.html) for more information.

#### Motion Profiling
A control technique that constrains velocity and acceleration to achieve smoother movement.

#### Motor Controller
An electronic device that regulates motor operation, speed, position, or torque.

### N

#### Neo 1.1
A first generation brushless motor produced by REV Robotics. Good mid-tier motor for medium torque application. See the REV [product page](https://www.revrobotics.com/rev-21-1650/) for more information on the motor. This motor requires a separate [Motor Controller](#motor-controller) to operate. ThunderChickens typically uses a REV [SparkMax](#sparkmax) as our preferred motor controller.

#### Neo 2.0
A second generation brushless motor produced by REV Robotics. Good mid-tier motor for medium torque application. See the REV [product page](https://www.revrobotics.com/rev-21-1653) for more information on the motor. This motor requires a separate [Motor Controller](#motor-controller) to operate. ThunderChickens typically uses a REV [SparkMax](#sparkmax) as our preferred motor controller.

#### Neo 550
A small form factor brushless motor produced by REV Robotics. Tiny low torque, high speed motor useful for weight savings and space saving applications. See the REV [product page](https://www.revrobotics.com/rev-21-1651/) for more information on the motor. This motor requires a separate [Motor Controller](#motor-controller) to operate. ThunderChickens typically uses a REV [SparkMax](#sparkmax) as our preferred motor controller.

#### NetworkTables
NetworkTables is a publish-subscribe communication system built over Ethernet to share data between the robot, [Co-processor](#co-processor), [Driver Station](#driver-station) and more. See [What is NetworkTables](https://docs.wpilib.org/en/stable/docs/software/networktables/networktables-intro.html) for more information.

### O

#### Odometry
Tracking the robot's position on the field from its wheel encoders and [Gyroscope](#gyroscope). Drifts over time, so it is combined with vision in a [Pose Estimator](#pose-estimator).

### P

#### Periodic
A subsystem method called every robot cycle, typically every 20 milliseconds.

#### PhotonVision
A robot vision platform used for camera processing and pose estimation. See [PhotonVision](https://photonvision.org/) website for more information.

#### PID Controller
A feedback control algorithm using Proportional, Integral, and Derivative terms to regulate system performance. See [Introduction to PID](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/introduction/introduction-to-pid.html) for more information.

#### Pivot
A rotational positioning subsystem that controls angular movement.

#### Pneumatics
A motion system powered by compressed air, providing strictly linear movement. Use is often discouraged due to the weight & components required to setup a pneumatic system.

#### Pose Estimator
A WPILib class that combines [Odometry](#odometry) with timestamped vision measurements to estimate the robot's position on the field.

#### Position Control
A control strategy that drives a mechanism to a desired target position.

### R

#### Ratchet
A mechanism that allows motion in one direction and locks against the other, using a pawl. Used on climbers so the robot keeps hanging after power is cut.

#### Relative Encoder
An encoder that tracks movement relative to a starting point but does not retain absolute position after power cycling. Starts at a zero (0) position, which is whatever position the motor is in when the robot is turned on.

#### REV Robotics
[REV Robotics](https://www.revrobotics.com/) is a robotics supplier providing motors, motor controllers, encoders, and related control hardware.

#### Robot Controller
The central computing device that executes robot code and coordinates subsystem behavior. Historically it has been the NI RoboRio. Starting with the 2027 season it will be the Limelight SystemCore.

#### Rotations
The native unit of motor and encoder position: one full turn of the shaft. Motor rotations are converted to mechanism units (meters, degrees) using the [Gear Ratio](#gear-ratio).

### S

#### Sensor
A device that provides feedback about robot or mechanism state. [Encoders](#encoder), [Limit Switches](#limit-switch), or [Inertial Measurement Units](#imu-inertial-measurement-unit) are common sensors in FRC. 

#### Servo
A compact actuator designed for position or continuous rotation control. Good for low speed, low torque applications. Shooter hoods, climber ratchet pawls are some examples.

#### Simulation
A software environment for testing robot code without physical hardware. [WPILIB](#wpilib) supports an integrated simulation environment via VS Code.

#### Simulation Periodic
A subsystem method executed only during robot simulation.

#### Single-Acting Solenoid
A pneumatic actuator that uses air pressure in one direction and a spring for return motion.

#### Software Limit
A motion boundary enforced through software typically using encoder feedback.

#### SparkMax

A [Motor Controller](#motor-controller) designed by [REV Robotics](#rev-robotics). This is ThunderChickens preferred motor controller. See [product page](https://www.revrobotics.com/rev-11-2158) for more information.

#### Subsystem
A modular hardware and software component that encapsulates a specific robot function. Such as Elevator, Pivot, Roller.

#### SubsystemBase
The WPILib base class used as the foundation for robot subsystems.

#### Superstructure
Code that coordinates several mechanisms so they move in a safe order, using named robot states.

#### Swerve Drive
A drivetrain in which each wheel module independently controls wheel speed and steering angle. Fast becoming the most popular drivebase, but also one of the most complicated steering methods.

#### SysID
WPILib's System Identification framework used for mechanism [Characterization](#characterization) and control tuning.

### T

#### TalonFX
A [CTRE](#ctre) [motor controller](#motor-controller) physically integrated into Kraken motors. Previously used in the Falcon 500 motors produced by VEX Robotics. See [product page](https://wcproducts.com/products/kraken) for more information.

#### Telescoping Arm
An extending arm mounted on a [Pivot](#pivot); an elevator whose direction of travel changes with the pivot angle.

#### Through-Bore Encoder
A type of encode that fits directly on a hex shaft. Typically a type of [Absolute Encoder](#absolute-encoder).

#### Tunable Constant
A configurable value that can be adjusted live during robot operation. See [LoggedTunableNumber](#loggedtunablenumber) for ThunderChickens typical implementation.

#### Turret
A mechanism that rotates around a vertical axis to aim, usually a shooter, without turning the whole robot.

### U

#### Units Conversion
The process of translating raw sensor values into meaningful engineering units such as meters or degrees.

### V

#### Velocity Control
A control mode that regulates the speed of a mechanism.

#### Vision System
A camera-based subsystem used for localization, target tracking, and object detection.

#### Vortex
A brushless motor produced by [REV Robotics](#rev-robotics). This motor is unique in that we can change the shaft type or put a hex shaft through the motor. See [product page](https://www.revrobotics.com/rev-21-1652) for more information.

### W

#### WestCoast Products
[WestCoast Products](https://wcproducts.com/) is a supplier of mechanical, structural, and electrical components tailored for FIRST Robotics Competition (FRC) teams.

#### WPILib
The primary software library and framework used for FRC robot programming. See [WPILib](https://docs.wpilib.org/en/stable/index.html) website for more information.

## 8 Revision History


| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 06/13/2026 | AK     | Initial Release                     |
| 08/26/2026 | AK     | Expansion of document               |
| 08/30/2026 | AK     | Broaden to include all subsystems   |
| 09/05/2026 | AK     | Add glossary of common terms        |
| 11/23/2026 | GV     | Complete section 5 subsystem specs  |
