# Triggers & Controller Bindings

A **Trigger** is a condition that can start and stop commands. Controller buttons are triggers, and
so is anything you can write as a `BooleanSupplier`.

## Controller buttons

```java
private final CommandXboxController driver = new CommandXboxController(0);
private final CommandXboxController operator = new CommandXboxController(1);

private void configureBindings() {
  driver.a().onTrue(pivot.deploy());
  driver.rightBumper().whileTrue(intake.intake());
  operator.b().toggleOnTrue(shooter.spinUp());
}
```

| Binding | Behavior |
|---|---|
| `onTrue(cmd)` | Schedule when the button is pressed. Runs until it finishes or is interrupted. |
| `onFalse(cmd)` | Schedule when the button is released. |
| `whileTrue(cmd)` | Schedule on press, **cancel on release**. |
| `toggleOnTrue(cmd)` | Each press starts or cancels the command. |

Pick the right one: `whileTrue` for "hold to intake," `onTrue` for "press to deploy."

## Joystick axes

Axes are `double` values from −1 to 1. Pass them as **suppliers** so they're read every loop:

```java
drive.setDefaultCommand(
    drive.joystickDrive(
        () -> -driver.getLeftY(),    // forward is negative Y on the stick
        () -> -driver.getLeftX(),
        () -> -driver.getRightX()));
```

Apply a **deadband** so a stick resting slightly off-center doesn't creep:
`MathUtil.applyDeadband(value, 0.1)`.

Triggers can also be axes: `driver.rightTrigger(0.5)` is true when the trigger is pressed past halfway.

## Custom triggers

```java
Trigger hasGamePiece = new Trigger(() -> feeder.hasPiece());
hasGamePiece.onTrue(leds.flashGreen());
hasGamePiece.and(driver.rightBumper()).whileTrue(shooter.shoot());

new Trigger(DriverStation::isEnabled).onTrue(pivot.home());
```

Combine triggers with `.and(...)`, `.or(...)`, and `.negate()`. Use `.debounce(0.1)` for noisy
sensors.

## Where bindings go

Bindings go in the container's `configureBindings()` method, which runs once at startup. The 2026
code wraps controllers in its own `Controller` interface (`utilities/controller/`) so Xbox and PS4
controllers can be swapped. That's a nice example of an interface used for a real purpose.

## Testing bindings

Bindings can be tested on a laptop with a simulated controller. See
[Testing teleop](../testing/unit-testing.md#testing-teleop).

## 2027 changes

- Controller classes are being reorganized, and they're still changing between alphas. An early
  alpha merged them into one `Gamepad` class; a later one brought back controller-specific classes
  and renamed face buttons by direction (`faceUp`, `faceDown`, `faceLeft`, `faceRight`). Commands v3
  uses `CommandGamepad`. Use autocomplete to find the current names.
- Commands v3 adds `retryWhileTrue`, `multiPress`, `risingEdge`, and `fallingEdge`.
  See [Commands v3](commands-v3.md#triggers-and-bindings).
