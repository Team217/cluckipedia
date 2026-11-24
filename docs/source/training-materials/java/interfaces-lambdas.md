# Interfaces, Lambdas & Suppliers

These three ideas show up on almost every line of command-based robot code and in the AdvantageKit IO
layer. Take your time with this page.

## Interfaces: a contract

An **interface** lists methods a class promises to have, without saying how they work.

```java
public interface RollerIO {
  void setVoltage(double volts);
  void stop();
}
```

```java
public class RollerIOTalonFX implements RollerIO {
  private final TalonFX motor = new TalonFX(20);

  @Override
  public void setVoltage(double volts) {
    motor.setControl(new VoltageOut(volts));
  }

  @Override
  public void stop() {
    motor.stopMotor();
  }
}

public class RollerIOSim implements RollerIO {
  private double volts = 0.0;

  @Override
  public void setVoltage(double volts) { this.volts = volts; }

  @Override
  public void stop() { volts = 0.0; }
}
```

Code that only knows about `RollerIO` works with **either** implementation:

```java
RollerIO io = Robot.isReal() ? new RollerIOTalonFX() : new RollerIOSim();
io.setVoltage(6.0); // works the same either way
```

This is the entire idea behind the [AdvantageKit IO layer](../advantagekit/why-io.md).

### `default` methods

An interface can provide a do-nothing default, so implementations only override what they need:

```java
public interface PivotIO {
  public default void setBrakeMode(boolean brake) {}
}
```

The 2026 `PivotIO` does this. `PivotIOSim` doesn't care about brake mode, so it doesn't override it.

## Lambdas: a method you can pass around

A **lambda** is a small, nameless function. It is written `(parameters) -> expression`.

```java
Runnable sayHi = () -> System.out.println("hi");   // takes nothing, returns nothing
sayHi.run();                                       // prints "hi"

DoubleSupplier half = () -> 6.0 / 2;               // takes nothing, returns a double
System.out.println(half.getAsDouble());            // 3.0

DoubleUnaryOperator square = x -> x * x;           // takes a double, returns a double
System.out.println(square.applyAsDouble(4));       // 16.0
```

The key idea: **writing a lambda does not run it.** It packages up code to run *later*, possibly
many times.

```java
intake.stop();          // runs stop() RIGHT NOW, once
() -> intake.stop()     // a Runnable that will call stop() WHENEVER someone calls run()
intake::stop            // "method reference": shorthand for the same lambda
```

## The functional interfaces you'll see

| Interface | Signature | Robot use |
|---|---|---|
| `Runnable` | `() -> void` | The action inside `runOnce(...)` and `run(...)` |
| `BooleanSupplier` | `() -> boolean` | `until(() -> atSetpoint())`, `new Trigger(() -> sensor.get())` |
| `DoubleSupplier` | `() -> double` | Joystick axes: `() -> -controller.getLeftY()` |
| `Supplier<T>` | `() -> T` | `Supplier<Pose2d>` for the robot pose |
| `Consumer<T>` | `T -> void` | PathPlanner's "reset pose" callback |

## Why suppliers matter: the joystick bug

```java
// WRONG: reads the joystick ONCE when the command is created, then uses that value forever.
drive.driveCommand(controller.getLeftY());

// RIGHT: passes a function that re-reads the joystick every loop.
drive.driveCommand(() -> controller.getLeftY());
```

This is the most common beginner bug in command-based code. If a value can change while the command
is running, pass a **supplier**, not a value.

You can see this in the 2026 `KrakenPivot`:

```java
public Command SetPosition(DoubleSupplier position) {
  return runOnce(() -> this.setPosition(position.getAsDouble()))
      .until(() -> isWithinTolerance(position.getAsDouble(), 1));
}
```

## Capturing variables

A lambda can use variables from around it, but only if they are **effectively final** (never
reassigned):

```java
double target = 21.0;
Command c = pivot.runOnce(() -> pivot.setPosition(target)); // OK

double t = 0;
t = 5;
Command bad = pivot.runOnce(() -> pivot.setPosition(t));    // compile error
```
