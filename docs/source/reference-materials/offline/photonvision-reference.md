# PhotonVision Reference

Local summary of the PhotonVision documentation linked from [Odometry & Vision](../../training-materials/swerve/vision.md).
Source: [docs.photonvision.org](https://docs.photonvision.org/)

## What runs where

| Piece | Runs on | Job |
|---|---|---|
| **PhotonVision** | Coprocessor (for example Orange Pi 5) with USB cameras | Finds AprilTags, estimates pose, publishes results over NetworkTables |
| **PhotonLib** | roboRIO, in our Java code (a vendordep) | Reads results, runs pose estimation, simulates cameras |

## Installing on a coprocessor

1. While online, download the PhotonVision **image** for your coprocessor from PhotonVision's
   GitHub releases, and keep it on the USB stick. The image version must match the PhotonLib
   vendordep version in the robot code.
2. Flash the image to a microSD card with an imaging tool (for example balenaEtcher or Raspberry Pi
   Imager).
3. Insert it, power the coprocessor from the robot, and connect it to the radio/switch by Ethernet.
4. Open the web UI at `http://photonvision.local:5800`, or `http://<ip>:5800`.
5. In **Settings**: set team number **217**, and give the coprocessor a **static IP**
   (`10.2.17.11` or another address a mentor assigns; avoid .1, .2, and the radio's range).
   Static IPs are much more reliable at events than `.local` names.
6. Keep a pre-flashed spare SD card for every coprocessor (part of the competition checklist).

Updating later: upload the new `.jar` in Settings, or reflash the image.

## Cameras

- On the **Cameras** page, give each camera a **nickname**. This must exactly match the name in code:
  `new PhotonCamera("front")`.
- Choose a resolution and frame rate. Higher resolution sees tags from farther but costs frame rate
  and CPU. Typical: 1280×800 or 1280×720 for AprilTags on global-shutter cameras.

## Calibration (required for 3D pose)

Pose estimation needs each camera's lens model (focal length, distortion). Without calibration,
estimates are wrong.

1. Print the calibration target (a ChArUco or chessboard board) and mount it **perfectly flat**
   on something rigid. Measure the square size exactly.
2. On the camera's page, start calibration **at the resolution you'll use on the robot**.
   Calibration is per resolution.
3. Take **many** snapshots (aim for 25+) with the board at different distances, angles, and
   positions, covering the edges and corners of the image, not just the center.
4. Finish and check the **mean reprojection error**. Well under 1 pixel is good. If it's high,
   redo it with steadier, more varied images.
5. Export or back up the calibration. Calibration data can be lost when reflashing.

## AprilTag pipeline settings

| Setting | Recommendation |
|---|---|
| Pipeline type | AprilTag |
| Tag family | 36h11 (FRC's family since 2024) |
| 3D mode | On (needs calibration) |
| Exposure | Low, with auto exposure **off**, to reduce motion blur. Raise brightness/gain if tags aren't detected. |
| Decimate | Higher is faster but loses distant tags. Start at 2. |
| Multi-tag | Enable **MultiTag** estimation on the coprocessor. When several tags are visible, it computes one combined, much more accurate field pose. |
| Field layout | Uses the season's official AprilTag field layout. Upload a custom one if the field differs. |

## Using the results in code

```java
PhotonCamera camera = new PhotonCamera("front");
Transform3d robotToCamera =
    new Transform3d(
        new Translation3d(0.30, 0.0, 0.25),          // meters: forward, left, up from robot center
        new Rotation3d(0, Math.toRadians(-15), 0));  // roll, pitch (negative = tilted up), yaw
PhotonPoseEstimator estimator =
    new PhotonPoseEstimator(
        AprilTagFieldLayout.loadField(AprilTagFields.kDefaultField),
        PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR,
        robotToCamera);
estimator.setMultiTagFallbackStrategy(PoseStrategy.LOWEST_AMBIGUITY);
```

| Strategy | Meaning |
|---|---|
| `MULTI_TAG_PNP_ON_COPROCESSOR` | Use the coprocessor's multi-tag result. Best when 2+ tags are seen. |
| `LOWEST_AMBIGUITY` | With one tag, use the less ambiguous of the two possible poses |
| `CLOSEST_TO_REFERENCE_POSE` | Pick the candidate nearest a reference (for example the current estimate) |

Each loop, read `camera.getAllUnreadResults()`, update the estimator, filter, and pass accepted
poses to the drivetrain with the result's **timestamp**. See [Odometry & Vision](../../training-materials/swerve/vision.md).

The Rotation3d pitch sign is easy to get wrong. Verify `robotToCamera` by placing the robot at a
known spot on the field and checking that the vision pose matches.

## Simulation

PhotonLib can fake camera results from the simulated robot pose:

```java
VisionSystemSim visionSim = new VisionSystemSim("main");
visionSim.addAprilTags(fieldLayout);
SimCameraProperties props = new SimCameraProperties();
props.setCalibration(1280, 800, Rotation2d.fromDegrees(70)); // resolution and diagonal FOV
props.setFPS(30);
props.setAvgLatencyMs(35);
PhotonCameraSim cameraSim = new PhotonCameraSim(camera, props);
visionSim.addCamera(cameraSim, robotToCamera);

// every sim loop:
visionSim.update(simulatedRobotPose);
```

The 2026 `utilities/vision/VisionSim.java` does this using the physics-simulated pose.

## Troubleshooting

| Problem | Check |
|---|---|
| No camera in code ("camera not found") | Nickname matches; coprocessor on the network; versions match |
| Tags detected but pose wildly wrong | Calibration missing or for a different resolution; `robotToCamera` wrong |
| Pose jumps between two places | Single-tag ambiguity. Reject high ambiguity, prefer multi-tag. |
| Detections drop while moving | Exposure too high (motion blur) |
| Low frame rate | Lower resolution, raise decimate, check CPU temperature |
| Works at shop, not at event | Static IP conflicts with field network, or lighting differs. Re-check exposure. |
