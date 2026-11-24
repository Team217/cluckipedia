# LED Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- [ ] CANdle CAN ID, LED count, and segment layout in the constants object.
- [ ] Configure CANdle (strip type, brightness, behavior when robot code isn't running).
- [ ] Named states (enum) mapped to colors & patterns from the CANdle LED Plan.
- [ ] Priority order for states.
- [ ] Disabled status display (ready to enable, alliance).
- [ ] Only update the CANdle when the state changes.
- [ ] Logging via AdvantageKit.


## Advanced Functions

- [ ] Segments showing different information at the same time.
- [ ] Triggers from other subsystems set states automatically.
- [ ] Reduced brightness on low battery voltage.
- [ ] Simulation of subsystem (log state for AdvantageScope).


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
| 11/23/2026 | GV     | Completed checklist                 |
|            |        |                                     |
