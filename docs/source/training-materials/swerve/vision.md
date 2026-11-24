# Odometry & Vision

## Why odometry alone isn't enough

Odometry adds up small wheel movements. Every bump, wheel slip, and collision adds a little error,
and the error **never goes away on its own**. After a hard match, the pose can be off by a meter.
**Vision** fixes this by measuring the robot's position directly from AprilTags on the field.

## The pose estimator

WPILib's `SwerveDrivePoseEstimator` (built into CTRE's swerve and the AdvantageKit template) combines
both:

- **Odometry** updates every loop (or faster, from the odometry thread).
- **Vision measurements** are added whenever a camera sees tags:
  `addVisionMeasurement(pose, timestamp, stdDevs)`.
- **Standard deviations** tell it how much to trust each measurement. Bigger means less trust.

The **timestamp** must be when the camera image was **captured**, not when we received it. The
estimator rewinds to that moment and applies the correction there.

## PhotonVision

PhotonVision runs on a coprocessor with one or more cameras. It finds AprilTags and computes where
the robot is from them. In code we use **PhotonLib**:

```java
PhotonCamera camera = new PhotonCamera("front");
PhotonPoseEstimator estimator =
    new PhotonPoseEstimator(fieldLayout, PoseStrategy.MULTI_TAG_PNP_ON_COPROCESSOR, robotToCamera);

for (PhotonPipelineResult result : camera.getAllUnreadResults()) {
  estimator.update(result).ifPresent(est ->
      drive.addVisionMeasurement(
          est.estimatedPose.toPose2d(), est.timestampSeconds, stdDevsFor(est)));
}
```

`robotToCamera` is the camera's position and angle on the robot (a `Transform3d`). Get it from CAD
and **double-check it**. A camera mounted 5° off from what the code thinks makes every estimate wrong.

## Rejecting bad measurements

Bad vision data is worse than none. The 2026 code (`utilities/vision/Camera.java`) implements the
first two filters below; the others are worth adding:

| Filter | Why |
|---|---|
| **Ambiguity** for single-tag estimates | One tag can fit two different poses. Reject if ambiguity is high. |
| **Max distance** | Far tags are inaccurate. |
| **Field bounds** | Reject poses outside the field or below/above the floor. |
| **Scale std devs by distance and tag count** | Trust close, multi-tag estimates more. |

Log every accepted and rejected pose separately (`Vision/AcceptedPoses`,
`Vision/RejectedPoses`). In AdvantageScope you can then see exactly why the pose jumped.

## Vision in simulation

PhotonLib's `VisionSystemSim` generates fake tag detections from the **simulated** robot pose, so the
whole pipeline (estimation, filtering, std devs) can be tested on a laptop. The 2026
`utilities/vision/VisionSim.java` shows how. In the AdvantageKit pattern, this lives in a
`VisionIOPhotonVisionSim` implementation.

Docs: [PhotonVision](https://docs.photonvision.org/) ([offline](../../reference-materials/offline/photonvision-reference.md)), [WPILib pose estimators](https://docs.wpilib.org/en/stable/docs/software/advanced-controls/state-space/state-space-pose-estimators.html) ([offline](../../reference-materials/offline/controls-reference.md#pose-estimators))

## 2027 status

PhotonLib only had development builds for WPILib 2027 as of November 2026. The PhotonVision setup,
pose estimation, and filtering on this page don't change. Expect class and import names to follow
the `org.wpilib` rename.

## Beyond AprilTags: finding game pieces

AprilTags tell the robot where **it** is. To find **game pieces** that move around the field, a
neural network on the coprocessor detects them. See the
[Object Detection](../object-detection/overview.md) section.
