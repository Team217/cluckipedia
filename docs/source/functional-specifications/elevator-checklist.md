# Elevator Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- [ ] Encoder to track elevator position (Possibly absolute).
- [ ] Motor(s) to move the elevator up and down.
    - [ ] Configure (idle mode, current limits).
    - [ ] Configure gear ratio.
- [ ] Limit switches. HW limits, SW limits (Encoder position, Min / Max setpoint value).
- [ ] speed controls:
    - [ ] Set-points. Via tunable PID.
    - [ ] Manual operation.
- [ ] PID basic control. SVA (Static Friction, Velocity, Acceleration) or GVA (Gravity, Velocity, Acceleration) FeedForward Control Loop
- [ ] Pass constants object into constructor
- [ ] Logging via AdvantageKit.
- [ ] Method to check if arm is at a setpoint. onTarget.


## Advanced Functions

- [ ] Support for single or dual motors.
- [ ] Motion Profiling (CTRE/Phoenix6 Motion Magic). MaxVelocity & MaxAcceleration.
- [ ] Optional HW limits. Enable / Disable this functionality.
    - [ ] Redundant limit switches (2 on top, 2 on bottom).
- [ ] Java Units Classes (Distance, Angle, Velocity, Etc.).
- [ ] Simulation of subsystem.
- [ ] Integration with SysID.


## Climber Functions

- [ ] Actuator to drive the ratchet pawl.
    - [ ] Servo take in argument from constructor
    - [ ] Pneumatic solenoid. 


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
|            |        |                                     |
