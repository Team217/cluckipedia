# Tuning Workflow

A repeatable process for tuning any mechanism. Follow it in order, in simulation first, then on the
real mechanism.

## Tools

- **`LoggedTunableNumber`** (`utilities/logging/LoggedTunableNumber.java`): a number you can
  change from the dashboard while the robot runs. It only reads the dashboard when
  `SystemConstants.kDebug` is `true`; otherwise it uses the default from `Constants`. Values show up
  under `/Tuning/...` in NetworkTables.
- **AdvantageScope** line graphs of goal, reference, position, and voltage.
- **Phoenix Tuner X** plots, for quick checks on the bench.

## Safety first

- Tune with the mechanism **unloaded** first if possible, and with someone holding the disable key.
- Check soft limits and current limits **before** the first closed-loop test.
- Start with small gains. A kP that's 10× too high can break a mechanism in one move.

## The procedure

### 1. Verify the basics (open loop)

- [ ] Positive voltage moves the mechanism in the positive direction.
- [ ] Position units are right: move it a known amount and compare.
- [ ] Current limit and soft limits are set.

### 2. Feedforward

Set all PID gains to 0.

- [ ] **kG** (arms/elevators): find the voltage that holds the mechanism still
      (see [Feedforward](feedforward.md)).
- [ ] **kS**: find the voltage where it just starts to move.
- [ ] **kV**: estimate from free speed, or run SysId.
- [ ] Command a Motion Magic move with **no PID**. The mechanism should roughly follow the profile
      and stop near the goal. If it's way off, fix the feedforward before going on.

### 3. Proportional

- [ ] Raise kP until the mechanism reaches the goal quickly with a little overshoot.
- [ ] Back it off until there's no overshoot.

### 4. Derivative (if needed)

- [ ] If it still oscillates or overshoots at a good kP, add a small kD.
- [ ] If the motor buzzes or sounds rough, kD is too high.

### 5. Integral (rarely)

Almost never needed if feedforward is right. If a small steady error remains, first check kG/kS
again. If you do use kI, keep it tiny.

### 6. Record it

- [ ] Copy the final gains into `Constants.java`.
- [ ] Commit with a message saying what you tuned and on what (sim or which robot).
- [ ] Save an AdvantageScope screenshot of the final response in the PR.

## Velocity mechanisms (flywheels, rollers)

Same process with `VelocityVoltage`: kS, then kV (the big one), then a small kP. Judge by:

- **Spin-up time** to within tolerance of the target speed.
- **Recovery time** after a game piece passes through: how fast speed recovers.

Flywheels usually need no kD.

## Sim vs. real

Gains from simulation are a **starting point**, not the answer. The sim doesn't know about friction,
belt stretch, or the wiring harness pulling on the arm. Expect to change kS and kG on the real
mechanism, and maybe kP. If sim and real differ a lot, update the sim's mass, MOI, or gear ratio
so it's useful next time.

## Debugging bad behavior

| Symptom | Likely cause |
|---|---|
| Moves the wrong way, then hits the limit | Inversion wrong, or sensor and motor disagree |
| Doesn't move at all | Disabled, soft limit reached, wrong CAN ID, or `connected` false. Check inputs. |
| Sags below target | kG too low (or no gravity type set) |
| Oscillates around target | kP too high, kD too low |
| Slow and never quite gets there | Feedforward too low, or kP too low |
| Jumps violently at the start of a move | Not using Motion Magic, or acceleration too high |
| Works in sim, not on robot | Gear ratio, inversion, or zero offset differs. Compare inputs side by side. |

## 2027 note

WPILib 2027 adds its own **Tunables** API for live-tuned values, replacing SmartDashboard numbers.
Our `LoggedTunableNumber` (built on AdvantageKit's `LoggedNetworkNumber`) keeps working and is
logged for replay, so we keep using it.
