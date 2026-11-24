# Java Basics

This page covers the parts of Java you'll use every day. It is not a complete Java course. For more
depth, use the free [Java tutorials from Oracle](https://dev.java/learn/) ([offline](../../reference-materials/offline/java-reference.md)) or
[Codecademy's Learn Java](https://www.codecademy.com/learn/learn-java) ([offline](../../reference-materials/offline/java-reference.md)) alongside these pages.

## Your first program

```java
public class Main {
  public static void main(String[] args) {
    System.out.println("Hello, ThunderChickens!");
  }
}
```

- Every Java file contains a **class** with the same name as the file (`Main.java` → `class Main`).
- `main` is where a plain Java program starts. Robot programs also have a `main`
  (`src/main/java/frc/robot/Main.java`), but you almost never touch it.
- Statements end with `;`. Blocks are wrapped in `{ }`.

## Variables and types

Java is **statically typed**: every variable has a type that never changes.

```java
int ballCount = 3;               // whole numbers
double motorVolts = 6.5;         // decimal numbers (we use double, not float)
boolean intakeDeployed = false;  // true or false
String name = "Kraken";          // text
final double kMaxVolts = 12.0;   // final = can't be reassigned
```

:::{admonition} Team naming conventions
:class: tip
- Variables and methods: `camelCase`. Include the unit in the name: `positionRot`,
  `velocityRadPerSec`, `heightMeters`.
- Constants: start with `k`, for example `kMaxVolts` or `kStowedSetpoint`.
- Classes: `PascalCase`, for example `KrakenPivot`.
:::

## Math and operators

```java
double average = (a + b) / 2.0;
int remainder = 7 % 3;           // 1
double clamped = Math.max(-12.0, Math.min(12.0, volts));
double angle = Math.atan2(y, x); // radians!
```

:::{admonition} Integer division
:class: warning
`7 / 2` is `3`, not `3.5`, because both numbers are `int`. Write `7 / 2.0` or `7.0 / 2` to get
a decimal result.
:::

## Conditionals

```java
if (velocity < kStallVelocity && current > kCurrentSpike) {
  System.out.println("At hard stop");
} else if (velocity > kMaxVelocity) {
  System.out.println("Too fast");
} else {
  System.out.println("Normal");
}
```

Comparison operators: `==`, `!=`, `<`, `<=`, `>`, `>=`. Logical operators: `&&` (and),
`||` (or), `!` (not).

:::{admonition} Never compare doubles with `==`
:class: danger
Floating-point math is not exact. Use a tolerance instead. The 2026 code has
`TCMath.isWithinTolerance(value, target, tolerance)` for exactly this.
:::

## Loops

```java
for (int i = 0; i < 4; i++) {
  System.out.println("Module " + i);
}

while (!isAtTarget()) {
  // ...
}
```

:::{admonition} No `while` loops waiting on the robot
:class: danger
Robot code runs in a 50 Hz loop (see [How a Robot Program Runs](../frc-basics/robot-program.md)).
A `while` loop that waits for a sensor **freezes the whole robot**. You will learn to use
commands instead.
:::

## Methods

```java
/** Converts motor rotations to arm degrees. */
public static double motorRotToArmDegrees(double motorRot, double gearRatio) {
  return motorRot / gearRatio * 360.0;
}
```

- `public`: who can call it. `static`: belongs to the class, not an object (more on this in
  [Classes & Objects](oop.md)). `double`: the return type (`void` means it returns nothing).
- The comment starting with `/**` is **Javadoc**. Write one for every public method.

## Arrays and lists

```java
double[] moduleAngles = {0.0, 90.0, 180.0, 270.0};
System.out.println(moduleAngles.length); // 4

import java.util.ArrayList;
import java.util.List;

List<String> autoNames = new ArrayList<>();
autoNames.add("Left");
autoNames.add("Center");
for (String auto : autoNames) {
  System.out.println(auto);
}
```

## Reading errors

When Java crashes, it prints a **stack trace**:

```text
Exception in thread "main" java.lang.NullPointerException: Cannot invoke "Intake.stop()" because "intake" is null
    at frc.robot.RobotContainer.configureBindings(RobotContainer.java:42)
    at frc.robot.RobotContainer.<init>(RobotContainer.java:20)
```

1. The first line tells you **what** went wrong.
2. The first `at` line inside **our** code (`frc.robot...`) tells you **where**.
3. Here, `intake` was used before it was created. Check the constructor order.

## 2027: Java 25

WPILib 2027 uses **Java 25** (2026 used Java 17). Everything on these pages works the same. Newer
Java adds conveniences you may see in examples, such as `_` for unused lambda parameters
(`_ -> gyro.reset()`), records, and pattern matching in `switch`. The JDK comes with the WPILib
installer.
