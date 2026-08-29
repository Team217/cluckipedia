# Turret Subsystem Functional Specification

## Core Turret Functionality
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

## Bonus Turret Functions
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
- Chinese Remainder Theorem (CRT) dual encoder position tracking.
