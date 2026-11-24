# Servo Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- [ ] Servo on a PWM port or Servo Hub channel, passed in via the constants object.
- [ ] Named positions as calibrated constants or an enum.
- [ ] Arrival estimate from travel time (onTarget).
- [ ] Pass constants object into constructor.
- [ ] Logging via AdvantageKit (commanded position & assumed state).


## Advanced Functions

- [ ] Support for continuous rotation servos (speed control).
- [ ] Position feedback where accuracy matters (feedback servo or limit switch).
- [ ] Simulation of subsystem (rate-limited position).


## Servo Hood Functions

- [ ] Calibrated linear conversion from commanded value to angle.
- [ ] Software limits to the calibrated range.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
| 11/23/2026 | GV     | Completed checklist                 |
|            |        |                                     |
