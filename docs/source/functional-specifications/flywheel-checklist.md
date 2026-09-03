# Flywheel Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.


## Core Functions

- [ ] Motor(s) to spin the flywheel.
    - [ ] Configure motor basics (idle mode, current limits, inversion, etc.).
    - [ ] Configure gear ratio.
- [ ] Encoder to measure flywheel velocity. Relative encoder.
- [ ] Speed controls:
    - [ ] Set-points. Via tunable PID & Feedforward.
    - [ ] Manual operation.
- [ ] PID basic control. SVA (Static Friction, Velocity, Acceleration) FeedForward Control Loop
- [ ] Pass constants object into constructor
- [ ] Logging via AdvantageKit.
- [ ] Method to check if arm is at a setpoint. onTarget.


## Advanced Functions

- [ ] Support for single or dual motors.
- [ ] Support for external encoder REV through-bore or CTRE CANCoder.
- [ ] Filtering of velocity feedback. LinearFilter (WPI)
- [ ] Setup Setpoints to use Enum or data class. This reduces the risk of typos or "mystery constants/doubles" in code.
- [ ] Java Units Classes (Distance, Angle, Velocity, Etc.).
- [ ] Simulation of subsystem.
- [ ] Integration with SysID.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
|            |        |                                     |
