# Differential Drivebase Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- Motors for left & right side of the drive. Minimum of one (1) per side.
    - [ ] Configure (idle mode, current limits).
    - [ ] Configure gear ratio.
- [ ] Encoder to track drivebase position (robot pose).
- [ ] Gyroscope to track robot heading (CTRE Pigeon 2.0).
- [ ] Odometry setup to track encoder & gyro to determine robot pose.
- [ ] speed controls:
    - [ ] Velocity setpoint (path planning / auton).
    - [ ] Manual operation.
- [ ] PID basic control. SVA (Static Friction, Velocity, Acceleration) or GVA (Gravity, Velocity, Acceleration) FeedForward Control Loop
- [ ] Logging via AdvantageKit.


## Advanced Functions
- [ ] Support vision pose estimation. 
- [ ] Motion Profiling (CTRE/Phoenix6 Motion Magic). MaxVelocity & MaxAcceleration.
- [ ] Java Units Classes (Distance, Angle, Velocity, Etc.).
- [ ] Simulation of subsystem.
- [ ] Integration with SysID.


## Butterfly Functions

- [ ] Initialize / Create four (4) double acting pneumatic solenoids.
- [ ] Map functionality to drive pneumatic state of drivebase. 
  - [ ] Traction / Omni / split modes.
- [ ] Method to return the state of modules.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
|            |        |                                     |
