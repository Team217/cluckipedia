# Object Detection: Testing & Competition

How to prove the system works, keep it working at events, and fix it when it doesn't. Run the
[unit tests and simulation](robot-code.md#test-it-without-a-robot) before any of the on-robot steps below.

## Testing plan

Test in this order. Don't skip ahead.

1. **Bench:** camera stream and detections appear in the PhotonVision UI and in NetworkTables.
2. **Projection check:** robot stationary, piece at measured spots (1 m, 2 m, 3 m; left, center, right). The `GamePieceField` piece positions should be within about **5–10 cm**. If not, re-check the camera transform, calibration, and `PIECE_CENTER_HEIGHT_M`.
3. **Latency compensation:** spin the robot slowly in place with a piece in view. The piece on the field view should stay **still**. If it swings around, the pose history or timestamps are wrong.
4. **Aim assist:** robot rotates to face pieces smoothly with no oscillation (tune the heading PID).
5. **DriveToPiece at low speed** (`MAX_SPEED = 1.0`), then increase.
6. **Edge cases:** piece at the edge of the image, two pieces close together, piece next to bumpers, a person walking through view, a piece rolling away.
7. **Auton patterns A → B → C**, each with pieces moved from their expected positions, and with pieces **missing**.
8. **Stress test:** 20+ consecutive pickups. Record success rate and average pickup time. Re-run after any model update.

## Competition operations

**Before the event**
- [ ] Model files versioned (e.g. `piece_v4_2027-02-10.rknn`) with notes on which data produced each one
- [ ] PhotonVision settings exported and backed up (USB drive + repo)
- [ ] Spare coprocessor pre-loaded with the same image, model, and settings
- [ ] Camera transform and calibration recorded

**Practice day**
- [ ] Check detection on the real field: lighting, carpet, field-element false positives
- [ ] Re-check exposure and white balance for the venue lights
- [ ] Take **50–100 photos** from the robot camera on the field
- [ ] If detection is weak: label quickly with model pre-labels, fine-tune overnight in Colab, convert, re-test before matches

**Between matches**
- [ ] Review logs and video for misses and false positives
- [ ] Adjust the confidence threshold first (fastest fix); retrain only if needed
- [ ] Re-check the camera mount after hard hits

## Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| Model won't upload / no detections | Wrong format, size, or YOLO version | Re-convert with the correct notebook; 640×640 quantized for Orange Pi |
| Wrong class names | Labels order mismatch | Labels must match training order exactly |
| Piece positions off by a consistent amount | Camera transform wrong | Re-measure camera x/y/z and pitch |
| Positions wrong mostly at image edges | Camera not calibrated | Run calibration at the pipeline's resolution |
| Pieces "swing" when robot turns | No or incorrect latency compensation | Check the pose history buffer and timestamps |
| Robot oscillates when aiming | Heading PID too aggressive | Lower P, add a little D |
| Robot stops just short of the piece | `LOST_TIMEOUT` or `MIN_SPEED` too low | Increase them slightly |
| Robot pushes the piece away | Too fast at contact, or intake not running | Lower `MIN_SPEED`, make sure the intake runs during the command |
| Chases pieces in bumpers / outside the field | False positives, no filtering | Raise the confidence threshold, add bumper negatives, use the `allowed` predicate |
| Target switches between two pieces | No target lock | Use `getPieceNear` (already in the examples); increase the turn penalty in `getBestPiece` |
| FPS drops after a few minutes | Thermal throttling | Heatsink/fan, better airflow |
| Coprocessor reboots during matches | Brownouts | Buck-boost regulator, check wiring |
| Good in shop, bad at event | Training data lacks variety | More real sessions in different lighting; fine-tune on event photos |

## References

The key information from each source is already on these pages, so you can work offline. The links
are for when you have internet and need the latest details.

| Source | What it covers | Where it's summarized here |
|---|---|---|
| [PhotonVision: About Object Detection](https://docs.photonvision.org/en/latest/docs/objectDetection/about-object-detection.html) | What PhotonVision's object detection pipeline does and reports | [Overview](overview.md#what-object-detection-is), [pipeline setup](training.md#configure-the-camera-pipeline) |
| [PhotonVision: Orange Pi 5](https://docs.photonvision.org/en/latest/docs/objectDetection/opi.html) | Supported models (640×640, quantized YOLOv5/v8/v11, `.rknn`) and the conversion notebook | [Convert the model](training.md#convert-the-model) |
| [PhotonVision: Rubik Pi 3](https://docs.photonvision.org/en/latest/docs/objectDetection/rubik.html) | `.tflite` models and `rubik_conversion.ipynb` | [Convert the model](training.md#convert-the-model) |
| [Ultralytics YOLO](https://docs.ultralytics.com) | Training and validation commands, dataset YAML format | [Training](training.md#phase-5-training-without-an-nvidia-gpu) |
| [BlenderProc](https://github.com/DLR-RM/BlenderProc) | Synthetic rendering library | [Synthetic data](data.md#phase-2-synthetic-data-3d-renders) |
| [Poly Haven HDRIs](https://polyhaven.com/hdris) | Free CC0 lighting environments for renders | [Folder setup](data.md#folder-setup) |
| [PathPlanner](https://pathplanner.dev) ([offline](../../reference-materials/offline/pathplanner-reference.md)) | AutoBuilder, named commands, pathfinding | [Autonomous](robot-code.md#autonomous) |
| [WPILib docs](https://docs.wpilib.org) ([offline](../../reference-materials/offline/controls-reference.md)) | Command-based, pose estimation | [Robot Code](robot-code.md) |

:::{admonition} Version note
:class: warning
PhotonVision, PathPlanner, WPILib, and BlenderProc change between seasons. The code here targets the
2026 libraries. Verify names against the current docs each season, and always check PhotonVision's
supported model list before training.
:::
