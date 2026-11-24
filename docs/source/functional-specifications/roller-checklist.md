# Roller Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- [ ] Motor(s) to drive the roller.
    - [ ] Configure (idle mode, current limits).
    - [ ] Configure gear ratio.
- [ ] Speed controls:
    - [ ] Manual operation.
- [ ] Pass constants object into constructor.
- [ ] Logging via AdvantageKit.


## Advanced Functions

- [ ] Support for single or dual motors.
- [ ] Java Units Classes (Distance, Angle, Velocity, Etc.).
- [ ] Simulation of subsystem.
- [ ] Integration with SysID. Characterizes motor performance.


## Intake Functions

- [ ] Game piece sensor (beam break, CANrange, or current detection).
- [ ] End intake command or drop to hold speed on detection.
- [ ] Block intaking when at the game piece limit.
- [ ] Rollers only run while deployed; stow when the command ends.
- [ ] Eject command.
- [ ] Log sensor & deploy state.


## Indexer Functions

- [ ] Game piece sensors at entry & staging positions.
- [ ] Stage pieces before the shooter.
- [ ] Feed only when the scoring mechanism is ready.
- [ ] Count pieces held.
- [ ] Jam detection & reverse.
- [ ] Log sensors, piece count, and state.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
| 11/23/2026 | GV     | Added intake & indexer functions    |
|            |        |                                     |
