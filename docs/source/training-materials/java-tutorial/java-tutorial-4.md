# Java Tutorial | Tutorial 4 Connecting Hardware

Welcome to the fourth ThunderChickens Java Tutorial.
This tutorial will answer these questions:

1. What hardware do we use?
2. How do we tune that hardware?

There's a lot to do, so let's get started!

## What hardware do we use?

We've already talked a bit about the hardware we use, but let's dig in a bit deeper... We'll talk about motors, motor controllers, gyroscopes, limit switches, banner sensors, encoders, and LEDs.

## Motors

The most common motor we use on our robots are the Neo Brushless motors. But what does "brushless" mean? First, we need to know what a brushed motor is. Brushed motors have physical brushes that make contact with a commutator (big boy motor word) to transfer electricity to the rotor (rotatey thingy), while brushless motors use electronic switching to control the magnetic field, eliminating the need for brushes. Brushless motors are significantly more efficient and have longer lifespans compared to brushed motors because they lack physical brushes, which eliminates friction and wear caused by contact with a commutator. This is how we program motors:

## Motor Controllers

So we have motors, but how do we control them? We use Spark MAXes to do this. Spark MAXes are motor controllers developed by REV Robotics. They control the speed, direction, and other aspects of motors, often used with the brushless motors, such as the NEO motors we talked about above.

```java
private SparkMax motor;
// For below, 0 is the motor ID, and the motor type is brushless.
motor = new SparkMax(0, MotorType.kBrushless);
```

## Gyroscopes

Short things short, a gyroscope allows us to drive the same direction no matter how the robot is rotated. This is useful because the functioning parts on our robot are typically on all sides, so we must rotate the robot for those functions to work. (Think back to the different subsystems). Instead of straining your eyes figuing out how the robot is rotated, you can just drive!

## Limit Switches

Limit switches are pieces of hardware that we use to limit other hardware, like motors. If you remember the elevator subsystem from the 2025 robot, you can see that we use limit switches to determine when the motors stop. We placed the limit switches at the top and bottom of our elevator so that we couldn't go past the floor or ceiling. This is only one example of how limit switches are used; the sky is the limit! This is how we code limit switches:

```java
private DigitalInput limitSwitch;
// For below, 0 is the limit switch ID.
limitSwitch = new DigitalInput(0);
```

## Banner Sensors

Using banner sensors is another way of limiting other hardware. They serve the same purpose, but function a bit differently. Instead of hitting a physical button, hardware intercepts a beam the sensor is outputing, and that sends a signal to software to trigger a function. Here's how we code banner sensors:

```java
private SparkLimitSwitch bannerSensor;
bannerSensor = motor.getForwardLimitSwitch();
```

## Encoders

Encoders work with the motors. But what do encoders do? Encoders take the physical motor properties and turn them into digital properties. The encoder allows us to track rotation speed, and position of the motor. This is useful for applications like our robot. According to this resource, "Encoders are devices used to measure motion (usually, the rotation of a shaft)." This is how we code encoders:

``` java
private AbsoluteEncoder motorEncoder = motor.getAbsoluteEncoder();
```

## LEDs

Party Time! 🎉🎊🎈 LED is an acronym for a light-emitting diode. LEDs have a few uses to them on a robot. First, they can relay critical information visually very quick. Second, pretty lights yippeeeeee! Candle 🕯️

## How do we tune that hardware?

We use PID controllers to tune the hardware mentioned above. What does tuning mean, though? A motor doesn't just move on it's own with code. You need PID(F) values to determine how fast and accurate the motor moves. PID is an incremental concept, meaning P has the most affect, then I, then D, and so on. That being said, we tune motors using P first, working our way down the line.

And there ya' go! You are all set to start learning about specialization! Proceed to the next tutorial to learn about specialization.

<br>
Made with ♡ by Jacob