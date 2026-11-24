# Classes & Objects

Robot code is built out of objects: a `TalonFX` object for each motor, an `Intake` object for the
intake subsystem, a `Command` object for each action. This page explains how to read and write them.

## A class is a blueprint, an object is a thing

```java
public class Motor {
  // Fields: data each Motor object holds
  private final int canId;
  private double appliedVolts = 0.0;

  // Constructor: runs once when the object is created
  public Motor(int canId) {
    this.canId = canId;
  }

  // Methods: things the object can do
  public void setVoltage(double volts) {
    appliedVolts = Math.max(-12.0, Math.min(12.0, volts));
  }

  public double getAppliedVolts() {
    return appliedVolts;
  }
}
```

```java
Motor left = new Motor(1);
Motor right = new Motor(2);
left.setVoltage(6.0);
System.out.println(left.getAppliedVolts());  // 6.0
System.out.println(right.getAppliedVolts()); // 0.0. Each object has its own fields.
```

- `this.canId` means "the field `canId` of this object." You need `this` when a parameter has the
  same name as a field.
- `private` fields can only be touched inside the class. Other code has to go through methods. This
  is called **encapsulation**, and it is why subsystems hide their motors.

## `static` and `final`

| Keyword | Meaning | Robot example |
|---|---|---|
| `final` | Can only be assigned once | `private final TalonFX motor = new TalonFX(10);` |
| `static` | Belongs to the class, shared by all objects | `public static final double kP = 0.5;` |
| `static final` | A constant | Everything in `Constants.java` |

:::{warning}
If the compiler says *"non-static method cannot be referenced from a static context,"* the fix
is almost never to add `static`. You're trying to call a method without an object. Find or
create the object instead.
:::

## Inheritance (`extends`)

A class can **extend** another class to reuse and change its behavior.

```java
public class Intake extends SubsystemBase {
  @Override
  public void periodic() {
    // runs every 20 ms. SubsystemBase calls this for us.
  }
}
```

- `SubsystemBase` (from WPILib) already knows how to register with the command scheduler. By
  extending it, `Intake` gets that for free.
- `@Override` tells the compiler, "I am replacing a method from the parent class." If you misspell
  the method name, the compiler catches it.

## Enums

An `enum` is a fixed list of named values. They're great for states and modes.

```java
public enum IntakeState {
  STOWED,
  DEPLOYED,
  INTAKING,
  EJECTING
}

switch (state) {
  case STOWED -> pivot.setPosition(0.0);
  case DEPLOYED, INTAKING -> pivot.setPosition(21.0);
  case EJECTING -> pivot.setPosition(10.0);
}
```

The 2026 code uses enums to choose the robot (`BotType`) and the logging mode (`Mode.REAL`,
`Mode.SIM`, `Mode.REPLAY`). See `Constants.java` and `Robot.java`.

## Packages and imports

- A **package** is a folder of related classes, such as `frc.robot.subsystems.intake`.
- To use a class from another package, `import` it:

```java
import com.ctre.phoenix6.hardware.TalonFX;
import frc.robot.Constants.PivotConstants;
```

VS Code adds imports for you. Use {kbd}`Ctrl+.` on a red underline and pick the right one.
Be careful: there are several classes named `Rotation2d`, `Units`, and `Timer`. Make sure you import
the one from `edu.wpi.first...` (or `org.wpilib...` in 2027, after WPILib's package rename).
