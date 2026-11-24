# Pneumatic Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- [ ] Solenoid(s) on the correct hub type (REV PH / CTRE PCM) and channels, from the constants object.
    - [ ] Single acting or double acting, matching the required fail-safe state.
- [ ] Explicit state set at startup.
- [ ] Methods to set and toggle state; getter for commanded state.
- [ ] Compressor left in automatic (closed-loop) control.
- [ ] Pass constants object into constructor.
- [ ] Logging via AdvantageKit.


## Advanced Functions

- [ ] Analog pressure sensor support, thresholds as constants, low pressure alert.
- [ ] Position feedback from reed switches.
- [ ] Disable compressor during critical moments (e.g. climbing).
- [ ] Simulation of subsystem.


## Gripper Functions

- [ ] Game piece sensor (holding / not holding).
- [ ] Optional automatic close on detection, with a short delay.
- [ ] Defined fail-safe state when disabled or out of air.
- [ ] Grab & release commands.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
| 11/23/2026 | GV     | Completed checklist                 |
|            |        |                                     |
