# Pivot Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.


## Core Functions

- [ ] Absolute encoder to track arm position. Seed absolute encoder to the motors relative encoder (at the least on startup).
- [ ] Motor(s) to drive the arm pivot.
    - [ ] Configure (idle mode, current limits).
    - [ ] Configure gear ratio.
- [ ] Limit switches. HW limits, SW limits (Encoder position, Min / Max setpoint value).
- [ ] Speed controls:
    - [ ] Set-points. Via tunable PID. (Rotations at a minimum ideally real units).
    - [ ] Manual operation.
- [ ] PID basic control. SVA (Static Friction, Velocity, Acceleration) or GVA (Gravity, Velocity, Acceleration) FeedForward Control Loop
- [ ] Pass constants object into constructor.
- [ ] Logging via AdvantageKit.
- [ ] Method to check if arm is at a setpoint. onTarget.


## Advanced Functions

- [ ] Support for single or dual motors.
- [ ] Motion Profiling (CTRE/Phoenix6 Motion Magic). MaxVelocity & MaxAcceleration.
- [ ] Optional HW limits. Enable / Disable this functionality.
    - [ ] Redundant limit switches (2 on top, 2 on bottom).
- [ ] Switch from Brake to Coast on disable and vise versa.
- [ ] Java Units Classes (Distance, Angle, Velocity, Etc.).
- [ ] Simulation of subsystem.
- [ ] Integration with SysID. Characterizes motor performance.


## Hood Functions

- [ ] Motor or servo to drive the hood.
    - [ ] If servo take in argument from constructor?
- [ ] Limit switch. Lower limit/home position.


## Turret Functions

- [ ] Sensor to track position. Seed absolute encoder to the motors relative encoder (at the least on startup).
    - [ ] Single absolute encoder.
    - [ ] Dual absolute encoder.
    - [ ] 10 turn potentiometer.
- [ ] Chinese Remainder Theorem (CRT) dual encoder position tracking.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
|            |        |                                     |
