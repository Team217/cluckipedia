# Glossary

Mechanism and hardware terms (elevator, flywheel, homing, NEO models, and more) are also defined in
the [Subsystem Functional Specification glossary](../functional-specifications/subsystem-func-spec.md#7-glossary).

| Term | Meaning |
|---|---|
| **AdvantageKit (AK)** | Logging framework that records all inputs for log replay |
| **AdvantageScope** | Viewer for live robot data and log files |
| **Alliance flip** | Mirroring poses and paths so autos work on the red alliance |
| **AprilTag** | Square fiducial markers on the field used for vision localization |
| **Brownout** | The roboRIO cutting motor output when battery voltage drops too low |
| **CAN / CAN FD** | Robot device network. CAN FD (via CANivore) is faster. |
| **CANcoder** | CTRE absolute magnetic encoder |
| **CANivore** | CTRE USB CAN FD adapter |
| **Closed loop** | Control that uses sensor feedback (PID) |
| **Command** | An action the robot does, scheduled by the `CommandScheduler` |
| **Default command** | Command that runs when nothing else requires the subsystem |
| **Deadband** | Ignoring small joystick values near zero |
| **Feedforward** | Predicted output from a physics model (kS, kV, kA, kG) |
| **FOC** | Field-oriented commutation: a Phoenix Pro motor control mode with more power |
| **Gear ratio** | Motor rotations per mechanism rotation |
| **IO layer** | Interface separating hardware access from subsystem logic |
| **Inputs** | Values read from hardware, logged with `processInputs` |
| **Kinematics** | Converting between chassis speeds and wheel states |
| **Motion Magic** | CTRE's on-motor motion profiling |
| **MOI** | Moment of inertia: rotational "mass" used in simulation |
| **Named command** | A command registered for use in PathPlanner autos |
| **Odometry** | Estimating pose from wheel encoders and gyro |
| **Open loop** | Control without feedback (for example a fixed voltage) |
| **Outputs** | Values computed by code, logged with `recordOutput` |
| **Phoenix 6** | CTRE's library for TalonFX, CANcoder, Pigeon 2 |
| **PID** | Proportional-integral-derivative feedback control |
| **Pigeon 2** | CTRE gyro/IMU |
| **Pose** | Position and heading (`Pose2d`/`Pose3d`) |
| **Replay** | Running a recorded log back through the code |
| **Requirement** | A subsystem a command needs exclusive use of |
| **Setpoint / goal** | Where we want the mechanism to be |
| **Soft limit** | Configured position limit the motor won't drive past |
| **Stator / supply current** | Current in the motor windings / current drawn from the battery |
| **Std devs** | How much the pose estimator trusts a measurement |
| **Subsystem** | A robot mechanism that owns its hardware |
| **SysId** | WPILib tool that measures feedforward gains |
| **TalonFX** | The motor controller inside Kraken motors |
| **Trigger** | A condition (button or sensor) that schedules commands |
| **Tuner X** | CTRE's device setup and test app |
| **YAGSL** | "Yet Another Generic Swerve Library". Used through 2026, not going forward. |

## Testing

| Term | Meaning |
|---|---|
| **Unit test** | A small piece of code that runs part of the robot code on a laptop and checks the result |
| **JUnit** | The Java testing library our projects use |
| **Fake IO** | A test-only IO implementation that records commands and reports values the test sets |
| **Assertion** | A check in a test (`assertEquals`, `assertTrue`) that fails the test if it's false |

## Object detection

| Term | Meaning |
|---|---|
| **Bounding box** | Rectangle around a detected object in the image |
| **Confidence** | How sure the model is about a detection (0–1) |
| **Domain randomization** | Randomizing everything except the object in synthetic images, so the model learns only the object |
| **Fine-tuning** | Continuing training of an already-trained model on a smaller, more specific dataset |
| **Latency compensation** | Using the robot's pose from when a camera frame was captured, not the current pose |
| **NMS (non-maximum suppression)** | Removes duplicate boxes around the same object |
| **NPU** | Neural processing unit; the AI accelerator chip on the Orange Pi 5 / Rubik Pi 3 |
| **Quantization** | Converting a model to 8-bit integers so it runs fast on an NPU |
| **Synthetic data** | Training images rendered from a 3D model instead of photographed |
| **YOLO** | "You Only Look Once", the family of fast detection models we train |

## Sensors and REV hardware

| Term | Meaning |
|---|---|
| **Absolute encoder** | Knows its angle at power-on (within one rotation) |
| **Beam break** | IR emitter + receiver; an object blocking the beam is detected |
| **CANdi** | CTRE module that brings digital inputs and PWM/quadrature encoders onto CAN |
| **CANrange** | CTRE time-of-flight distance/proximity sensor |
| **Canandmag / Canandgyro / Canandcolor** | Redux Robotics CAN encoder, gyro, and color sensor |
| **Hall-effect sensor** | Detects a magnet with no contact |
| **Homing** | Finding a known position at startup (hard stop, limit switch, or current spike) |
| **LaserCAN** | Grapple Robotics CAN time-of-flight sensor |
| **MAXMotion** | REV's on-controller motion profiling (like Motion Magic) |
| **navX** | Studica IMU |
| **NEO / NEO Vortex / NEO 550** | REV brushless motors |
| **Normally closed (NC)** | A switch that opens when pressed; fails safe |
| **Quadrature** | Two-channel encoder signal that gives position and direction |
| **Relative encoder** | Counts change from where it started |
| **REV Hardware Client** | REV's device setup app |
| **SPARK MAX / SPARK Flex** | REV motor controllers |
| **Through Bore Encoder** | REV encoder with absolute and quadrature outputs |
| **Time of flight (ToF)** | Measures distance by timing reflected light |

## New for 2027

| Term | Meaning |
|---|---|
| **A-Stop** | Autonomous stop button on the 2027 Driver Station |
| **Commands v3** | WPILib's new Java command framework built on coroutines |
| **Coroutine** | A function that can pause (`yield`, `wait`, `await`) and resume on a later loop |
| **`CANPort`** | WPILib 2027 enum naming a SystemCore or MotionCore CAN bus |
| **Mechanism** | The Commands v3 replacement for a subsystem |
| **MotionCore** | Expansion module for SystemCore with extra CAN buses and I/O |
| **OpMode** | An FTC-style selectable robot mode, optional in WPILib 2027 |
| **Priority** | Commands v3 rule for which command wins a mechanism: equal or higher interrupts, lower is rejected |
| **Scope** | Where a Commands v3 binding or default command lives (global, OpMode, or command); removed when it ends |
| **SmartIO** | SystemCore's configurable I/O ports (digital, PWM, analog in, ...) |
| **SystemCore** | The FRC robot controller that replaces the roboRIO starting in 2027 |
| **Telemetry / Tunables** | WPILib 2027 APIs replacing SmartDashboard for publishing and live-tuned values |
| **Utility mode** | The 2027 name for Test mode |
