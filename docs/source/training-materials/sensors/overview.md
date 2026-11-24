# Sensors Overview

A robot can only be as smart as what it can measure. This section covers the sensors used in FRC,
**regardless of manufacturer**: how each type works, when to use it, and how to use the common
products from CTRE, REV, Redux, Studica, Playing With Fusion, Grapple, and ThriftyBot, plus
WPILib's generic classes.

| Page | Sensors |
|---|---|
| [Encoders](encoders.md) | Built-in motor encoders, absolute encoders (CANcoder, Through Bore, Canandmag, Thrifty), quadrature encoders, potentiometers |
| [Gyros & IMUs](gyros.md) | Pigeon 2, navX, Canandgyro, SystemCore's built-in IMU |
| [Limit, Presence & Homing Sensors](presence.md) | Limit switches, hall-effect sensors, beam breaks, current-based homing |
| [Distance & Color Sensors](distance-color.md) | CANrange, LaserCAN, PWF Time of Flight, REV Color Sensor V3, Canandcolor |
| [Odometry & Vision](../swerve/vision.md) | Cameras and AprilTags |

## Choosing a sensor: the questions

1. **What do I need to know?** Position, angle, speed, "is something here", distance, color, heading?
2. **Do I need to know it at power-on?** Then you need an **absolute** sensor.
3. **How fast and how precise?** A shooter's speed needs fast, smooth data; a hopper's "is it full"
   doesn't.
4. **Where does it plug in?** CAN is easiest to wire and gives the most data. DIO, analog, and I2C
   use controller ports (which are limited, especially on SystemCore).
5. **Can it run the control loop on the motor controller?** A sensor the TalonFX or SPARK can read
   directly lets the closed loop run at 1 kHz with no robot-code delay.
6. **What's our standard?** Prefer **CTRE** (CANcoder, Pigeon 2, CANrange, CANdi) so everything is
   configured and logged the same way in Phoenix 6 and Tuner X.

## Quick chooser

| Need | First choice (CTRE) | Alternatives |
|---|---|---|
| Mechanism position, with a known starting position | Kraken built-in encoder | NEO/Vortex built-in encoder |
| Mechanism angle at power-on (arm, wrist, turret, swerve steering) | **CANcoder** | REV Through Bore (absolute), Redux Canandmag, Thrifty encoder |
| Robot heading | **Pigeon 2** | navX, Redux Canandgyro, SystemCore IMU |
| "Is the mechanism at its end of travel?" | Limit switch via **CANdi**, or current-based homing | Switch to SPARK data port or DIO; hall-effect sensor |
| "Is a game piece here?" | **CANrange** (proximity) or beam break via **CANdi** | LaserCAN, PWF ToF, Canandcolor proximity, beam break on DIO |
| Distance to something (up to ~2 m) | **CANrange** | LaserCAN, PWF Time of Flight |
| Game piece color | — | Redux Canandcolor, REV Color Sensor V3 |
| Robot position on the field | Cameras + AprilTags | — |

## How sensors connect

| Interface | How it works | Examples | Notes |
|---|---|---|---|
| **CAN** | Digital messages on the CAN bus | CANcoder, Pigeon 2, CANrange, CANdi, Canandmag, LaserCAN, PWF ToF | Most data, easy wiring, unique IDs required. Our preference. |
| **Motor controller port** | Plugs into the TalonFX/SPARK | Limit switches, Through Bore on a SPARK | The motor controller can act on it directly (limits, feedback) |
| **DIO** (digital input) | On/off (high/low voltage) | Limit switches, beam breaks, hall sensors | Also reads pulse-width (duty cycle) and quadrature encoders |
| **Analog input** | A voltage 0–5 V (roboRIO) | Potentiometers, some absolute encoders | Noisy; filter it |
| **I2C** | Short-distance digital bus | REV Color Sensor V3 | Long wires are unreliable. The roboRIO's onboard I2C port has known lockup problems. |
| **USB / Ethernet** | Computer connection | Cameras, some navX models | — |

:::{admonition} 2027: SystemCore ports
:class: note
SystemCore has **6 SmartIO ports** (each configurable as digital, PWM, analog in, ...), **2 I2C
ports** with a pinout swapped from the roboRIO, **no SPI and no MXP**, and **5 CAN FD buses**.
Digital inputs on alpha SystemCore units used a pull-*down* (switches had to connect to +3.3 V);
production units are planned to use a pull-*up* like the roboRIO. With fewer I/O ports, CAN sensors
matter even more. See [What's New for 2027](../season-2027/whats-new.md#systemcore-the-new-robot-controller).
:::

## The rules for every sensor in our code

1. **Sensors live in IO classes.** Read them in `updateInputs()` and put the values in `inputs`,
   never directly in subsystem logic. That's what makes replay and simulation work
   (see [Why an IO Layer](../advantagekit/why-io.md)).
2. **Log the raw value and a `connected` flag.** For a beam break, log the boolean; for a distance
   sensor, log the distance **and** whether it's valid.
3. **Name values with units**, as always: `distanceMeters`, `angleRot`.
4. **Simulate it.** In the sim IO, compute what the sensor *would* read from the physics model
   (for example, "beam broken" when the simulated game piece is in the intake).
5. **Plan for failure.** What does the robot do if this sensor is unplugged or reads garbage? A
   jammed limit switch shouldn't stop the whole robot; a disconnected encoder shouldn't send an arm
   into the floor.
