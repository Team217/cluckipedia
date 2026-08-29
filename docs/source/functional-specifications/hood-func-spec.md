# Hood Subsystem Functional Specification

## Core Hood Functionality
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

## Bonus Hood Functions
### Easy Bonus
- Motion Profiling (CTRE/Phoenix6 Motion Magic). MaxVelocity & MaxAcceleration.
- PID basic control. SVA (Static Friction, Velocity, Acceleration) or GVA (Gravity, Velocity, Acceleration) FeedForward Control Loop
- Optional HW limits. Enable / Disable this functionality.
    - Redundant limit switches (2 on top, 2 on bottom).
- Switch from Brake to Coast on disable and vise versa.

### Hard Bonus
- Java Units Classes (Distance, Angle, Velocity, Etc.).
- Simulation of subsystem.
- Integration with SysID. Characterizes motor performance.
