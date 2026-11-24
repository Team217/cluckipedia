# CTRE Reference

Local summary of the CTRE pages linked from [Phoenix 6 Basics](../../training-materials/ctre/phoenix6.md),
[Phoenix Tuner X](../../training-materials/ctre/tuner-x.md), [Motion Magic](../../training-materials/control/motion-magic.md),
[Feedforward](../../training-materials/control/feedforward.md), and [CTRE Swerve](../../training-materials/swerve/ctre-swerve.md).

Sources: [Phoenix 6 docs](https://v6.docs.ctr-electronics.com/en/stable/),
[Tuner X](https://v6.docs.ctr-electronics.com/en/stable/docs/tuner/index.html),
[Swerve generator](https://v6.docs.ctr-electronics.com/en/stable/docs/tuner/tuner-swerve/index.html),
[Motion Magic](https://v6.docs.ctr-electronics.com/en/stable/docs/api-reference/device-specific/talonfx/motion-magic.html),
[SysId integration](https://v6.docs.ctr-electronics.com/en/stable/docs/api-reference/wpilib-integration/sysid-integration/index.html),
[Phoenix 6 examples](https://github.com/CrossTheRoadElec/Phoenix6-Examples).

## Phoenix 6 in a project

Phoenix 6 is added as a **vendordep**: a JSON file in `vendordeps/` (ours is `Phoenix6-<version>.json`).
In VS Code, **WPILib: Manage Vendor Libraries** installs or updates it. That needs internet the first
time; see [Offline Reference](index.md#preparing-a-laptop-to-code-without-internet).

**Firmware and library versions should match the season.** Update device firmware in Tuner X at the
start of each season and after updating the vendordep.

## Device classes

| Class | Device |
|---|---|
| `TalonFX` | Kraken X60 / X44 and Falcon 500 motor controllers |
| `TalonFXS` | TalonFXS controller (for non-integrated motors such as Minion) |
| `CANcoder` | Absolute magnetic encoder |
| `Pigeon2` | IMU / gyro |
| `CANdle` | LED controller |
| `CANrange` | Time-of-flight distance sensor |
| `CANdi` | Digital input/encoder interface on CAN |
| `CANBus` | Represents a bus (`"rio"` or a CANivore's name). Can report bus utilization. |

## Configuration details

- Each device has a **configuration object** (`TalonFXConfiguration`, `CANcoderConfiguration`, ...)
  made of groups: `MotorOutput`, `CurrentLimits`, `Feedback`, `Slot0/1/2`, `MotionMagic`,
  `SoftwareLimitSwitch`, `HardwareLimitSwitch`, `Voltage`, `TorqueCurrent`, `ClosedLoopRamps`,
  `OpenLoopRamps`, and others.
- `getConfigurator().apply(fullConfig)` sets **every** value (defaults for anything you didn't set).
  `apply(config.Slot0)` changes only that group.
- `apply` blocks for up to a timeout (default about 0.1 s) waiting for the device to acknowledge.
  Apply configs **at startup**, not every loop. Changing gains at runtime (live tuning) is fine,
  but only when they change.
- Configs are saved on the device, but we treat the code as the source of truth by applying the full
  config at boot.

### Important config fields

| Field | Meaning |
|---|---|
| `MotorOutput.Inverted` | `CounterClockwise_Positive` (default) or `Clockwise_Positive`, looking at the motor face |
| `MotorOutput.NeutralMode` | `Brake` or `Coast` when output is zero |
| `CurrentLimits.StatorCurrentLimit(+Enable)` | Limits motor torque/heat |
| `CurrentLimits.SupplyCurrentLimit(+Enable)` | Limits battery draw; also has `SupplyCurrentLowerLimit` and `SupplyCurrentLowerTime` |
| `Feedback.SensorToMechanismRatio` | Sensor rotations per mechanism rotation |
| `Feedback.RotorToSensorRatio` | Rotor rotations per remote sensor rotation (when using a CANcoder) |
| `Feedback.FeedbackSensorSource` | `RotorSensor`, `RemoteCANcoder`, `FusedCANcoder` (Pro), `SyncCANcoder` (Pro) |
| `Slot0.kP/kI/kD/kS/kV/kA/kG` | Closed-loop gains |
| `Slot0.GravityType` | `Elevator_Static` or `Arm_Cosine` |
| `Slot0.StaticFeedforwardSign` | Whether kS follows the velocity sign or the error sign |
| `ClosedLoopGeneral.ContinuousWrap` | For mechanisms that spin forever, such as swerve steering: takes the shortest path across 0/1 rotation |

## Control requests in more detail

- Each request has `with...` methods that return the same object, for example
  `velocityRequest.withVelocity(50).withFeedForward(0.5).withSlot(1)`.
- `withEnableFOC(true)` uses Field-Oriented Commutation (Pro): roughly 15% more peak power and
  efficiency.
- `withLimitForwardMotion(boolean)` / `withLimitReverseMotion(boolean)` connect a limit-switch-like
  signal from code.
- `withUpdateFreqHz(...)` sets how often the request is resent. The default is fine.
- `Follower(leaderId, opposeMasterDirection)` makes a motor copy another. Set the follower once at
  startup.
- If the robot is disabled, every TalonFX goes to neutral regardless of the request.

## Status signals in more detail

- `getPosition()`, `getVelocity()`, `getAcceleration()`, `getMotorVoltage()`, `getSupplyCurrent()`,
  `getStatorCurrent()`, `getTorqueCurrent()`, `getDeviceTemp()`, `getClosedLoopReference()`,
  `getClosedLoopError()`, `getFault_*()` / `getStickyFault_*()` methods.
- `signal.getValueAsDouble()` returns the number. `signal.getValue()` returns a typed unit (Angle, etc.).
- `signal.getStatus()` tells you whether the last refresh worked.
- `BaseStatusSignal.refreshAll(...)` refreshes many signals in one call. On a CANivore, signals from
  devices can be time-synchronized with `BaseStatusSignal.waitForAll(timeout, ...)`, which the
  swerve odometry thread uses.
- `setUpdateFrequency(hz)` sets how often the device sends a signal. `optimizeBusUtilization()`
  turns off signals you haven't requested, which reduces CAN traffic.
- **Latency compensation:** `BaseStatusSignal.getLatencyCompensatedValue(position, velocity)`
  estimates the current position from the last sample plus velocity × age.

## Simulation API

Every device has a sim-state object: `talon.getSimState()`, `cancoder.getSimState()`,
`pigeon.getSimState()`.

| Method | Purpose |
|---|---|
| `setSupplyVoltage(volts)` | Battery voltage the simulated device sees |
| `getMotorVoltage()` | The voltage the simulated TalonFX is applying (feed into physics) |
| `setRawRotorPosition(rot)` / `setRotorVelocity(rps)` / `setRotorAcceleration(...)` | Feed physics results back to the TalonFX |
| `Orientation` | Must match the motor's inversion (`ChassisReference`) |
| `CANcoderSimState.setRawPosition(...)`, `setVelocity(...)` | Simulated absolute encoder |
| `Pigeon2SimState.setRawYaw(deg)` | Simulated gyro |

The simulated devices are only enabled while the robot is enabled, just like real ones.
See [Mechanism Simulation](../../training-materials/simulation/mechanisms.md).

## Phoenix Tuner X

- **Connect:** choose the roboRIO (team number / `10.2.17.2`) or a USB-connected CANivore as the
  target. On a robot, Phoenix's diagnostic server runs inside the robot program, so **robot code
  must be running** (deployed) for Tuner X to see devices through the roboRIO.
- **Devices page:** every device, with its ID, name, firmware version, and license status. Click a
  device to open its page.
- **Device page tabs (in general):**
  - *Configs:* view and edit every config. Remember the code overwrites these at boot.
  - *Control:* run the device directly (voltage, duty cycle, velocity, position). The robot must be
    enabled with control allowed; use carefully.
  - *Plot:* graph any signals live.
  - *Self Test Snapshot:* faults, current, temperature, firmware, and status in one view.
- **Firmware:** select one or many devices and use the firmware update action. Use the version
  matching the season's Phoenix 6 library.
- **IDs and names:** change on the device page. IDs are per device type, so a TalonFX 5 and a
  CANcoder 5 can coexist, but two TalonFXs can't both be 5.
- **Licensing:** Phoenix Pro licenses are activated per device or per CANivore ("season pass").
  The license status shows on each device.
- **Log extraction:** Tuner X can download `.hoot` log files from the robot and convert them to
  `.wpilog` for AdvantageScope or SysId.

## Swerve Project Generator

The generator in Tuner X (**Mechanisms → Swerve Project Generator**) walks through these stages.
It needs an all-CTRE drivetrain (8 TalonFX/TalonFXS, 4 CANcoders, a Pigeon 2) on one CAN bus.

1. **Drivetrain setup:** choose the CAN bus, gyro, module type (vendor and gear ratios), wheel
   radius, and the track width and wheelbase.
2. **Module setup:** for each corner, assign the drive motor, steer motor, and CANcoder by ID. Blink
   devices to confirm which physical module each one is.
3. **Verification:** the generator spins each steer and drive motor briefly so you can confirm
   direction and that the encoder matches. Fix inversions in the wizard if something is reversed.
4. **Encoder offsets:** align all wheels straight forward (bevel gears facing the same side on all
   four) and let the generator record each CANcoder offset.
5. **Test drive:** the generator can drive the robot from Tuner X with a controller, to sanity-check
   everything before generating code.
6. **Generate:** it produces a complete robot project, or only `TunerConstants.java`. For us, copy
   `TunerConstants.java` into the AdvantageKit swerve template (see
   [AdvantageKit Reference](advantagekit-reference.md#talonfx-swerve-template)).

`TunerConstants.java` holds everything: IDs, offsets, inversions, gear ratios, wheel radius, steer
and drive gains, the speed at 12 V, and slip current. Keep it in Git; regenerate it only when the
hardware changes.

## Motion Magic details

- **Profile limits** live in the `MotionMagic` config group: `MotionMagicCruiseVelocity`,
  `MotionMagicAcceleration`, `MotionMagicJerk` (0 means no jerk limit, giving a trapezoid; nonzero
  gives an S-curve).
- **Expo:** `MotionMagicExpo_kV` and `MotionMagicExpo_kA` describe how fast the mechanism can go
  and accelerate. With `MotionMagicExpoVoltage`, the profile follows the motor's natural
  exponential curve. If cruise velocity is 0, it uses the full speed `MotionMagicExpo_kV` allows.
- Motion Magic uses the **same slot gains** as other closed-loop modes. kV and kA matter more,
  because the profile supplies a target velocity and acceleration at each step.
- The profile is generated **on the device** at 1 kHz. Changing the target mid-move starts a new
  profile from the current state, so motion stays smooth.
- Plot `ClosedLoopReference` (the profile's position) against `Position` to judge tracking.

## SysId with Phoenix 6

CTRE's alternative to logging SysId data with AdvantageKit:

1. Write a `SysIdRoutine` whose mechanism applies `VoltageOut` to the TalonFX, and pass a
   `recordState` callback that writes the state with `SignalLogger.writeString("state", state.toString())`.
2. Call `SignalLogger.start()` before running the tests and `SignalLogger.stop()` after.
   `SignalLogger` records TalonFX signals at full rate to `.hoot` files on the robot.
3. Run the four tests.
4. In **Tuner X**, download the `.hoot` log and convert it to `.wpilog`.
5. Load it in **SysId** and select the state entry and the TalonFX's position, velocity, and motor
   voltage signals.

The full-rate signals often give a cleaner fit than 50 Hz robot-loop logging.

Note: the 2026 code turns off automatic CTRE signal logging (`SignalLogger.enableAutoLogging(false)`)
in `Robot.java` but still starts the logger. Check that before relying on `.hoot` files.

## What's in the Phoenix 6 examples

CTRE's example repository has small, complete projects for Java (and C++/Python). The useful ones
(folder names vary a little between seasons):

| Example | Shows |
|---|---|
| VelocityClosedLoop | `VelocityVoltage` and `VelocityTorqueCurrentFOC` on a TalonFX |
| PositionClosedLoop | `PositionVoltage` and `PositionTorqueCurrentFOC` |
| MotionMagic | Motion Magic configuration and requests |
| CANcoder | Reading a CANcoder, and using it as a remote sensor |
| Pigeon2 | Reading yaw/pitch/roll and angular rates |
| SwerveWithPathPlanner | Generated swerve project with PathPlanner autos |
| Simulation examples | `TalonFXSimState` paired with WPILib physics sims |
| Latency compensation / multiple signals | `refreshAll`, `waitForAll`, latency-compensated values |

The patterns from these examples already appear on our pages. Read the originals when you're online.
