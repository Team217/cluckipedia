# Vision Subsystem Checklist

Use this checklist as a quick reference to ensure your subsystem meets the requirements set out in the Subsystem Functional Specification. Always refer to the full functional specification for more detailed implementation details for any subsystem.

## Core Functions

- [ ] Camera name(s) & robot-to-camera transforms in the constants object (verified on the robot).
- [ ] PhotonLib pose estimation per camera, multi-tag on coprocessor.
- [ ] Process every unread result each loop.
- [ ] Filtering: single-tag ambiguity, max tag distance, field bounds.
- [ ] Send accepted measurements to the drivebase pose estimator with timestamp & standard deviations.
- [ ] Camera disconnect alert.
- [ ] Logging via AdvantageKit: accepted & rejected poses, tags used.


## Advanced Functions

- [ ] Standard deviations scaled by distance and tag count.
- [ ] Simulation with `VisionSystemSim`.
- [ ] Targeting data for aiming.


## Object Detection Functions

- [ ] Color camera mounted low, angled toward the intake; transform in constants.
- [ ] Detections projected to field positions using the pose at the frame timestamp.
- [ ] Tracker: smoothing, confirmation after several sightings, expiry.
- [ ] Queries: piece confirmed, piece within distance, best piece, piece near a point.
- [ ] Log raw detections (replay) and confirmed piece positions.
- [ ] Unit tests for projection & tracker.


## Revision History

| Date       | Author | Description                         |
| ---------- | ------ | ----------------------------------- |
| 09/02/2026 | AK     | Initial Release                     |
| 11/23/2026 | GV     | Completed checklist                 |
|            |        |                                     |
