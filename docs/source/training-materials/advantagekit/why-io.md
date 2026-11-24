# Why an IO Layer

**AdvantageKit** is a logging framework from Team 6328. It records **every input** the robot code
sees, so a log can be **replayed** later through the same code, producing every output and internal
value as if you'd logged them all. To make that work, hardware access has to be separated from robot
logic. That separation is the **IO layer**.

Docs: [docs.advantagekit.org](https://docs.advantagekit.org/) ([offline](../../reference-materials/offline/advantagekit-reference.md))

## The shape of every subsystem

```text
           ┌────────────────────────┐
           │  Pivot (subsystem)     │  ← logic: commands, setpoints, state, logging
           │  uses PivotIO          │
           └───────────┬────────────┘
                       │ interface
       ┌───────────────┼────────────────┐
       ▼               ▼                ▼
 PivotIOTalonFX    PivotIOSim        PivotIO (empty)
 real Kraken       physics model     replay: does nothing,
                                     inputs come from the log
```

- **`PivotIO`** is an interface. It has an `Inputs` class (everything the subsystem reads) and
  methods for everything it can command (`setPosition`, `setVoltage`, ...).
- **`PivotIOTalonFX`** talks to the real hardware with Phoenix 6.
- **`PivotIOSim`** uses a physics model so the code runs on a laptop.
- In **replay**, the subsystem gets an IO that does nothing, and AdvantageKit fills the inputs from
  the log file.

The subsystem **never knows which one it has**. Same logic, three environments.

## What this buys us

| Benefit | Example |
|---|---|
| Write code before the robot exists | Build and tune the pivot in sim during week 2 of build season. |
| Debug from logs | A match went wrong: replay the log, add new `recordOutput` calls, and see values you didn't log at the time. |
| Swap hardware | Changing from a SparkMax to a TalonFX only means writing a new IO class. The subsystem doesn't change. (The 2026 code has `RollerIOTalonFX` and REV versions.) |
| Consistent logging | Every mechanism logs the same kinds of data under the same names. |

## The two kinds of logged data

| Kind | How | What |
|---|---|---|
| **Inputs** | `io.updateInputs(inputs)` then `Logger.processInputs("Pivot", inputs)` | Anything read from the outside world: sensors, motor feedback, the Driver Station, vision. |
| **Outputs** | `Logger.recordOutput("Pivot/Setpoint", value)` | Anything the code calculates: setpoints, states, poses, "at target" flags. |

:::{admonition} The replay rule
:class: danger
Subsystem logic may only use data that came through `inputs`. If the subsystem reads a sensor
directly, or reads the clock with `System.currentTimeMillis()`, replay produces different results
than the real match did. Use `Timer.getFPGATimestamp()` (AdvantageKit logs it) and keep every
hardware call inside the IO classes.
:::

## Read the real example

Open these 2026 files side by side and find each part of the diagram above:

- `subsystems/intake/PivotIO.java`: the interface and the `@AutoLog` inputs class
- `subsystems/intake/PivotIOTalonFX.java`: the Phoenix 6 implementation
- `subsystems/intake/PivotIOSim.java`: an arm physics model with a PID that mimics the TalonFX's
- `subsystems/intake/KrakenPivot.java`: the subsystem
- `robotContainers/CompetitionContainer.java`: where real or sim IO is chosen

Then compare with `subsystems/intake/Pivot.java`, the older version that talks to SparkMax motors
directly. List three things you **can't** do with `Pivot` that you **can** do with `KrakenPivot`.
