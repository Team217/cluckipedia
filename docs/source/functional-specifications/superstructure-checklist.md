# Superstructure Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- [ ] Enum of robot states, each with the setpoint of every mechanism.
- [ ] Transition commands that move mechanisms in a safe order and require every subsystem they move.
- [ ] Safety rules as constants, checked before each step.
- [ ] Getter for current state & goal state.
- [ ] Logging via AdvantageKit (goal, current state, transitions).


## Advanced Functions

- [ ] Shortest safe path between states.
- [ ] Interruptible transitions that continue from the current positions.
- [ ] Ready-to-score checks for triggers, LEDs, and autonomous.
- [ ] Visualization of all mechanisms together (Mechanism2d or 3D model).
- [ ] Unit tests of transitions & safety rules using simulation IO.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 11/23/2026 | GV     | Initial Release                     |
|            |        |                                     |
