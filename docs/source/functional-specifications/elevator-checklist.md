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


## Climber Ratchet & Safety Functions

- [ ] Drive method refuses to drive against an engaged ratchet.
- [ ] Ratchet release only by explicit command; blocked while hanging.
- [ ] Endgame lockout (button combination or match time).
- [ ] Higher current limits & brake mode for climbing loads.
- [ ] Stop at the climbed position (limit switch or current detection).
- [ ] Log ratchet state & climb state.


## Telescoping Arm Functions

- [ ] Extension gravity feedforward scaled by sin(pivot angle).
- [ ] Maximum extension limited by pivot angle (frame perimeter / height).
- [ ] Retract before rotating (superstructure).
- [ ] Homing at power on.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
| 11/23/2026 | GV     | Added climber safety & telescoping arm |
|            |        |                                     |
