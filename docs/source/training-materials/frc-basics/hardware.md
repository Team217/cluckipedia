# Robot Hardware for Programmers

You don't need to wire the robot, but you do need to know what every box does, because most
"code bugs" at competition turn out to be hardware problems you diagnose from code and logs.

## The control system

| Component | What it does | Programmer's concern |
|---|---|---|
| **roboRIO** | The robot's computer. Runs our Java code. | Deploy target. Logs go to a USB stick plugged into it. |
| **Radio** | Wi-Fi link to the Driver Station | Must be programmed at each event. |
| **PDH** (Power Distribution Hub) | Distributes battery power, measures current per channel | We log its currents to spot stalled motors. |
| **CANivore** | A USB-to-CAN-FD adapter for CTRE devices | Faster CAN bus. Device constructors need the bus name. |
| **Kraken X60 / X44** | Brushless motors with a built-in **TalonFX** controller | We talk to them as `TalonFX` objects. |
| **CANcoder** | Absolute magnetic encoder | Knows the angle at power-on, so swerve modules and arms don't need to be zeroed. |
| **Pigeon 2** | Gyro/IMU | Robot heading for field-relative driving and odometry. |
| **Coprocessor** (e.g. Orange Pi) + cameras | Runs PhotonVision | Sends AprilTag pose estimates to the roboRIO. |
| **CANdle** | LED controller | Status lights for drivers. |

:::{admonition} 2027: SystemCore replaces the roboRIO
:class: important
See [SystemCore (2027)](#systemcore-2027) below. The concepts on this page stay the same.
:::

For a full guide to encoders, gyros, limit switches, beam breaks, distance and color sensors from
every manufacturer, see [Sensors Overview](../sensors/overview.md). For REV motor controllers, see
[REV Motors](../rev/revlib.md).

## Drivetrains

| Drivetrain | How it moves | Notes |
|---|---|---|
| **Differential** ("tank" or "skid steer") | Left and right wheel sets; turns by driving the sides at different speeds | Cheap, simple, strong. The XRP training robots use one. Can't move sideways. |
| **Swerve** | Each of 4 modules has a drive motor and a steering motor, so the robot can move in any direction while facing any direction | What we use most. More complex: 8 motors, 4 absolute encoders, and a gyro. See [CTRE Swerve](../swerve/ctre-swerve.md). |
| **Butterfly** | Each corner switches (with pneumatics) between a traction wheel and an omni wheel | A classic ThunderChickens design, last used in 2022 |

"Tank" and "arcade" are also names for two ways to **drive** a differential drivetrain: **tank**
control uses one joystick per side; **arcade** control uses one stick for forward speed and one for
turning. The [Functional Specifications](../../functional-specifications/index.rst) describe what our
drivetrain code must support.

## Status lights

Most devices have LEDs that tell you their state (powered, CAN OK, fault, brake/coast). When
something isn't working, check the lights before changing code. See
[Hardware Status LEDs](../../hardware-status-lights.md) for what each pattern means, and the
[CANdle LED Plan](../../candle-template.md) for the robot's own status lights.

## CAN bus

**CAN** is a two-wire network that daisy-chains every motor controller and sensor.

- Every device on a bus needs a **unique ID** for its device type. Set IDs in
  [Phoenix Tuner X](../ctre/tuner-x.md) and record them in `Constants.java`.
- A loose CAN wire takes down **every device after it** in the chain. If several devices drop out at
  once, look for a wiring break, not a code bug.
- Too much traffic causes delayed data. Phoenix 6 lets us lower the update rate of signals we don't
  need (see [Phoenix 6 Basics](../ctre/phoenix6.md)).

## Motors, gearboxes, and encoders

- A motor spins fast with little torque. A **gearbox** (or belt/chain reduction) trades speed for
  torque. A **12:1** reduction means 12 motor rotations make 1 mechanism rotation.
- The Kraken's built-in encoder measures **rotor** rotations. Tell the TalonFX the gear ratio
  (`SensorToMechanismRatio`) and it reports **mechanism** rotations instead.
- A **relative** encoder (the motor's own) starts at 0 at power-on. An **absolute** encoder
  (CANcoder) always knows its real angle.

## Current, voltage, and brownouts

- The battery is nominally 12 V, but it drops under load. When voltage falls below about 6.8 V, the
  roboRIO **browns out**: it disables motor outputs to protect itself.
- **Current limits** keep motors from drawing too much current, which prevents brownouts, tripped
  breakers, and melted motors. Every motor we configure gets a current limit.
- **Stator current** relates to torque (what the motor pushes). **Supply current** is what comes
  from the battery. Limit stator current to protect mechanisms and wheels; limit supply current to
  protect the battery and breakers.

## SystemCore (2027)

For 2027, FIRST is replacing the roboRIO with **SystemCore** (built by Limelight). What changes for
programmers:

| roboRIO | SystemCore |
|---|---|
| 1 CAN bus (plus a CANivore for CAN FD) | **5 CAN FD buses** (`can_s0`–`can_s4`). A **MotionCore** expansion adds more. |
| DIO, PWM, analog, relay, SPI, MXP ports | **6 SmartIO ports**, each configurable (digital, PWM, analog in, ...). No relay, SPI, or MXP. |
| No built-in gyro worth using | **Built-in IMU** (the Pigeon 2 is still better for swerve) |
| Imaging tool + team number setter | **Web interface** at `http://robot.local` (team number, OS updates, packages) |
| Logs on the USB stick | Logs on a USB stick (`/U`) or internal storage (`/home/systemcore/logs`) |
| Ethernet/USB only | Also has its own Wi-Fi access point for bench work |
| — | OLED screen showing IP addresses and faults |

The **I2C connector pinout is swapped** compared to the roboRIO, and connector details were still
changing in the alpha hardware. Details: [What's New for 2027](../season-2027/whats-new.md#systemcore-the-new-robot-controller).
