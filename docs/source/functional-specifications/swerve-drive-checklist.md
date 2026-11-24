# Swerve Drive Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- [ ] Four (4) modules, each with a drive motor, steer motor, and absolute encoder (CANcoder).
    - [ ] Module positions, gear ratios, wheel radius, and encoder offsets from the Tuner X swerve generator (`TunerConstants`).
    - [ ] Configure motors (inversion, brake mode, current limits, gains). Drive stator limit below wheel slip.
    - [ ] Steer motors use the CANcoder as feedback (`FusedCANcoder` or `RemoteCANcoder`).
- [ ] Gyroscope to track robot heading (CTRE Pigeon 2.0).
- [ ] Kinematics & odometry with a `SwerveDrivePoseEstimator`.
- [ ] Field-relative teleop driving: deadbands, input shaping, max speed scaling, alliance flip.
- [ ] Robot-relative velocity control for autonomous; PathPlanner `AutoBuilder` configured.
- [ ] Re-zero heading from a driver button; reset pose.
- [ ] Logging via AdvantageKit.


## Advanced Functions

- [ ] Support vision pose estimation (timestamped measurements with standard deviations).
- [ ] Pose history: pose at a past timestamp (`sampleAt`).
- [ ] High frequency (250 Hz) synchronized odometry thread.
- [ ] Drive-at-angle / heading lock.
- [ ] X-lock.
- [ ] Integration with SysID (drive & steer) and wheel radius characterization.
- [ ] Simulation of subsystem.
- [ ] Gyro disconnect fallback & alert.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
| 11/23/2026 | GV     | Completed checklist                 |
|            |        |                                     |
