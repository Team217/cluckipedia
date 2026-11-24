# Phoenix Tuner X

**Phoenix Tuner X** is CTRE's app for setting up and testing devices. Use it on the test bench and
during robot bring-up. It's not a place to store configuration: **the code applies the real
configuration at every boot.**

Docs: [Tuner X documentation](https://v6.docs.ctr-electronics.com/en/stable/docs/tuner/index.html) ([offline](../../reference-materials/offline/ctre-reference.md#phoenix-tuner-x))

## Connecting

- Connect your laptop to the robot (USB, Ethernet, or Wi-Fi), or plug the CANivore's USB cable
  directly into your laptop for bench work.
- In Tuner X, set the target to the roboRIO's address (`10.2.17.2`) or choose the local CANivore.
- The **Devices** page lists every CTRE device on each CAN bus.

## Bring-up checklist for a new mechanism

1. **Update firmware** on every device to the version that matches our Phoenix 6 vendordep.
2. **Set the CAN ID** and give it a name (for example "Pivot"). IDs must match `Constants.java`.
3. **Blink** the device (the button in Tuner X) to confirm which physical motor has which ID.
4. **Control → Voltage** at a small value (1–2 V). Check that the mechanism moves the direction you
   expect. If not, fix `Inverted` in code.
5. **Plot** position and velocity. Check that position increases in the "positive" direction and
   that the gear ratio matches the real motion. For example: rotate the arm 90° by hand and confirm
   the reading changes by `0.25 × gearRatio` rotor rotations.
6. For CANcoders, rotate the mechanism to its zero position and read the **absolute position**.
   That becomes your `MagnetOffset` (negated).
7. Write everything you found (IDs, inversions, offsets, ratios) into `Constants.java` and commit it.

## Self Test Snapshot

**Self Test Snapshot** shows a device's faults, current, temperature, and firmware in one place.
At competition, if a motor misbehaves, run a snapshot before you change any code.

## Plotting while tuning

Tuner X can plot any signal (position, velocity, closed-loop reference, closed-loop error) at high
rate. It's useful for quick checks on the bench. For anything you want to keep or compare, use
AdvantageScope with logged data instead.
