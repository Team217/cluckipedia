# Flywheel Subsystem Functional Specification

## Core Flywheel Functionality
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

## Bonus Flywheel Functions
### Easy Bonus
- Support for single or dual motors.
- Support for external encoder REV through-bore or CTRE CANCoder.
- PID basic control. SVA (Static Friction, Velocity, Acceleration) FeedForward Control Loop
- Filtering of velocity feedback. LinearFilter (WPI)
- Setup Setpoints to use Enum or data class. This reduces the risk of typos or "mystery constants/doubles" in code.

### Hard Bonus
- Java Units Classes (Distance, Angle, Velocity, Etc.).
- Simulation of subsystem.
- Integration with SysID.
