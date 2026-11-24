# Distance & Color Sensors

## Time-of-flight distance sensors

### How they work

A **time-of-flight (ToF)** sensor sends out pulses of infrared laser light and measures how long they
take to bounce back. Light travels about 30 cm per nanosecond, so the sensor times reflections very
precisely to get distance. FRC ToF sensors read from a few centimeters out to about 1–4 m.

- **Field of view (FOV) / region of interest:** the sensor sees a cone, not a point. Narrow it to
  ignore things off to the side.
- **Ranging mode / timing budget:** longer measurements are more accurate and reach farther, but
  update more slowly. Short mode is usually best for game pieces.
- **Signal strength / validity:** dark, shiny, or angled surfaces return weak signals. Always check
  the sensor's "valid" flag before using a distance.
- **Ambient light:** bright light (sunlight, stage lights) adds noise.

**Ultrasonic** sensors (sound echoes) were used historically; they're slower, wider, and WPILib's
ultrasonic class is removed in 2027. Use ToF.

### When to use them

- **Game piece detection** with an adjustable threshold ("piece present if closer than 8 cm").
- **Game piece position** inside a mechanism (how far in is it?).
- **Distance to a wall or field element**, at close range.

For robot position on the field, use vision instead.

### CTRE: CANrange (preferred)

```java
CANrange range = new CANrange(FeederConstants.kCanrangeId, bus);
CANrangeConfiguration cfg = new CANrangeConfiguration();
cfg.ProximityParams.ProximityThreshold = 0.08;                // meters: "detected" when closer
cfg.ProximityParams.ProximityHysteresis = 0.01;               // avoid flicker at the threshold
cfg.ProximityParams.MinSignalStrengthForValidMeasurement = 2500;
cfg.ToFParams.UpdateMode = UpdateModeValue.ShortRange100Hz;   // check available modes in Tuner X
range.getConfigurator().apply(cfg);

StatusSignal<Distance> distance = range.getDistance();
StatusSignal<Boolean> detected = range.getIsDetected();
StatusSignal<Double> strength = range.getSignalStrength();
```

- `getIsDetected()` applies the threshold **on the sensor**, so you get a clean boolean.
- A CANrange can be a TalonFX **hardware limit** (`withForwardLimitRemoteCANrange(range)`), so the
  feeder stops itself the instant a piece arrives. See [Presence sensors](presence.md#ctre).
- `FovParams` narrows the field of view.

### Grapple Robotics: LaserCAN

```java
LaserCan laser = new LaserCan(FeederConstants.kLaserCanId);
try {
  laser.setRangingMode(LaserCan.RangingMode.SHORT);
  laser.setRegionOfInterest(new LaserCan.RegionOfInterest(8, 8, 16, 16));
  laser.setTimingBudget(LaserCan.TimingBudget.TIMING_BUDGET_33MS);
} catch (ConfigurationFailedException e) {
  DriverStation.reportError("LaserCAN config failed: " + e.getMessage(), false);
}

LaserCan.Measurement m = laser.getMeasurement();
boolean valid = m != null && m.status == LaserCan.LASERCAN_STATUS_VALID_MEASUREMENT;
double distanceMeters = valid ? m.distance_mm / 1000.0 : Double.NaN;
```

LaserCAN needs Grapple's vendordep, which isn't in the 2026 project.

### Playing With Fusion: Time of Flight

PWF's library is already in our vendordeps (the 2026 code uses PWF's battery fuel gauge).

```java
TimeOfFlight tof = new TimeOfFlight(FeederConstants.kTofId);
tof.setRangingMode(TimeOfFlight.RangingMode.Short, 24);  // sample time in ms
tof.setRangeOfInterest(8, 8, 12, 12);

boolean valid = tof.isRangeValid();
double distanceMeters = tof.getRange() / 1000.0;          // getRange() is millimeters
```

## Color sensors

### How they work

A color sensor shines a white LED at the target and measures reflected **red, green, blue** (and
often infrared) light. Most also report **proximity** from the IR reflection. Color readings depend
heavily on distance and lighting, so mount the sensor **close (a few cm) and shielded from outside
light**, and calibrate with real game pieces.

Use a color sensor when you need to know **which** game piece you have (alliance color, piece type),
not just whether one is there.

Matching: WPILib's `ColorMatch` class compares a reading to a list of calibrated target colors and
returns the closest one with a confidence value. Log the raw RGB values too, so you can recalibrate
from logs.

### Redux: Canandcolor (CAN, preferred for color)

```java
Canandcolor color = new Canandcolor(FeederConstants.kCanandcolorId);
double proximity = color.getProximity();   // 0..1, larger = closer
double r = color.getRed(), g = color.getGreen(), b = color.getBlue();  // normalized
boolean ok = color.isConnected();
```

It can also run its own digital outputs ("digout") from color or proximity rules, which can be
wired to a motor controller's limit input.

### REV: Color Sensor V3 (I2C)

```java
ColorSensorV3 sensor = new ColorSensorV3(I2C.Port.kMXP);
Color c = sensor.getColor();
int proximity = sensor.getProximity();     // 0 (far) .. 2047 (close)
boolean ok = sensor.isConnected();

ColorMatch matcher = new ColorMatch();
matcher.addColorMatch(kRedPiece);
matcher.addColorMatch(kBluePiece);
ColorMatchResult result = matcher.matchClosestColor(c);
```

:::{warning}
On the roboRIO, the **onboard I2C port** (`I2C.Port.kOnboard`) can lock up the robot code. Use the
MXP I2C port, or prefer a CAN color sensor. On **SystemCore**, I2C ports work differently and the
pinout is swapped; use a CAN sensor if you can.
:::

## Simulation

In the sim IO, compute what the sensor would see from the simulated world: for example,
`distanceMeters = pieceInFeeder ? 0.03 : 2.0`, or `isRed = simPiece.isRed()`. Keep it simple; the
goal is to exercise the logic that uses the sensor.
