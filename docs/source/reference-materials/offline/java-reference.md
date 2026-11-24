# Java Reference

Local replacement for the online Java tutorials linked from [Java Basics](../../training-materials/java/basics.md)
([dev.java Learn](https://dev.java/learn/),
[Codecademy's Learn Java](https://www.codecademy.com/learn/learn-java)). It covers the Java topics those
courses teach beyond what the main Java pages cover. Try each snippet in a scratch project.

## Primitive types

| Type | Holds | Example |
|---|---|---|
| `int` | Whole numbers, about ±2.1 billion | `int id = 20;` |
| `long` | Bigger whole numbers | `long micros = 1_000_000L;` |
| `double` | Decimals (64-bit). Our default for any measurement. | `double volts = 11.8;` |
| `float` | Smaller decimals. We don't use it. | `float f = 1.5f;` |
| `boolean` | `true` / `false` | `boolean ready = false;` |
| `char` | One character | `char c = 'A';` |

**Casting** converts between them: `(int) 3.9` is `3` (it truncates, it doesn't round). Use
`Math.round(3.9)` to round.

## Strings

```java
String name = "Kraken";
name.length();                 // 6
name.toUpperCase();            // "KRAKEN"
name.equals("Kraken");         // true. ALWAYS use .equals for strings, never ==
name.contains("rak");          // true
name.substring(0, 3);          // "Kra"
String msg = "Volts: " + 12.0; // concatenation
String fmt = String.format("Pos %.2f rot", 1.23456); // "Pos 1.23 rot"
```

## The `Math` class

`Math.abs(x)`, `Math.max(a, b)`, `Math.min(a, b)`, `Math.sqrt(x)`, `Math.pow(x, 2)`,
`Math.sin/cos/tan(radians)`, `Math.atan2(y, x)`, `Math.hypot(x, y)`, `Math.PI`,
`Math.toRadians(deg)`, `Math.toDegrees(rad)`, `Math.signum(x)`, `Math.floor`, `Math.ceil`.

WPILib adds `MathUtil.clamp(value, low, high)`, `MathUtil.applyDeadband(value, deadband)`,
`MathUtil.angleModulus(radians)` (wraps an angle to −π…π), and `MathUtil.isNear(expected, actual, tolerance)`.

## Control flow details

```java
// Switch expression (modern Java): returns a value
double setpoint = switch (state) {
  case STOWED -> 0.0;
  case DEPLOYED -> 21.0;
  default -> 10.0;
};

// Ternary: a one-line if/else that returns a value
double volts = hasPiece ? 0.0 : 8.0;

// Loop control
for (int i = 0; i < 10; i++) {
  if (i == 3) continue; // skip to next iteration
  if (i == 7) break;    // leave the loop
}
```

## Collections

```java
List<Double> readings = new ArrayList<>();
readings.add(1.5);
readings.get(0);        // 1.5
readings.size();        // 1
readings.remove(0);

Map<String, Integer> canIds = new HashMap<>();
canIds.put("Pivot", 25);
canIds.get("Pivot");            // 25
canIds.getOrDefault("Arm", -1); // -1
canIds.containsKey("Pivot");    // true
for (Map.Entry<String, Integer> e : canIds.entrySet()) {
  System.out.println(e.getKey() + " = " + e.getValue());
}
```

Collections hold **objects**, so Java "boxes" `double` into `Double` automatically. A boxed value can
be `null`, and unboxing `null` throws a `NullPointerException`.

## Generics

The `<...>` part says what type a class holds: `List<String>`, `Supplier<Pose2d>`,
`StatusSignal<Angle>`. It lets the compiler catch mistakes like putting a `String` in a list of
poses.

## `Optional`

Some WPILib methods return `Optional<T>`, a box that may be empty. For example,
`DriverStation.getAlliance()` is empty until the Driver Station connects.

```java
Optional<Alliance> alliance = DriverStation.getAlliance();
boolean isRed = alliance.isPresent() && alliance.get() == Alliance.Red;
// or, shorter:
boolean isRed2 = alliance.orElse(Alliance.Blue) == Alliance.Red;
estimator.update(result).ifPresent(est -> drive.addVisionMeasurement(...));
```

Never call `.get()` without checking `isPresent()` first.

## Exceptions

```java
try {
  String text = Files.readString(path);
} catch (IOException e) {
  DriverStation.reportError("Couldn't read file: " + e.getMessage(), false);
}
```

- **Checked** exceptions (like `IOException`) must be caught or declared with `throws`.
- **Unchecked** exceptions (`NullPointerException`, `IndexOutOfBoundsException`,
  `ArithmeticException`) are bugs. Fix the cause instead of catching them.
- An uncaught exception in robot code **crashes the robot program**, which then restarts. During a
  match, the robot sits dead for several seconds. Read the stack trace in the Driver Station
  messages or the log.

## Records

A `record` is a compact class that only holds data:

```java
public record ShotParameters(double rpm, double hoodAngleDeg) {}

ShotParameters shot = new ShotParameters(3000, 25);
shot.rpm(); // 3000
```

## Access modifiers

| Modifier | Visible to |
|---|---|
| `public` | Everyone |
| `protected` | The same package and subclasses (used for `motor` in `ArmIOTalonFX` so `ArmIOSim` can reach it) |
| *(none)* | The same package only |
| `private` | The same class only |

## Debugging in VS Code

1. Click left of a line number to set a **breakpoint** (red dot).
2. Start with **WPILib: Simulate Robot Code** in debug mode, or press {kbd}`F5` for unit tests.
3. When execution stops, hover over variables to see values. Use the toolbar to
   **Step Over** (next line), **Step Into** (go inside a method call), and **Continue**.
4. The **Variables** and **Call Stack** panels show the current state and how you got there.

Don't use breakpoints on the real robot while it's enabled, since motors keep their last output.

## Practice ideas (no internet needed)

- Write a `Matrix2x2` class with `multiply`, `determinant`, and `toString`.
- Write a class that reads joystick-like values from an array and simulates a tank drive, printing
  left and right wheel speeds.
- Read any file in `src/main/java/frc/robot/utilities/` and write a one-sentence Javadoc for every
  method that's missing one.
