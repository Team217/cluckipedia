# Object Detection Overview

Object detection lets the robot **find game pieces on its own**: a neural network running on a
vision coprocessor looks at camera frames and reports every game piece it sees. This section
covers the whole pipeline, from the real game piece on kickoff day to a robot that drives onto
pieces in teleop and autonomous.

:::{admonition} Before you start
:class: note
This is an advanced topic. Be comfortable with [Odometry & Vision](../swerve/vision.md),
[Building an IO Subsystem](../advantagekit/io-subsystem.md), and [PathPlanner](../autonomous/pathplanner.md)
first. Training the model needs some Python, but every script is provided.
:::

| Page | What it covers |
|---|---|
| **Overview** (this page) | What object detection is, why to use it, the pipeline, hardware, season timeline |
| [Training Data](data.md) | Kickoff-day capture, synthetic 3D renders, copy-paste images, real images and labeling |
| [Training & Deploying](training.md) | Training in Google Colab without an NVIDIA GPU, converting the model, setting up PhotonVision |
| [Robot Code](robot-code.md) | An AdvantageKit IO layer for detections, a field-position tracker, pickup commands, autos, simulation, and unit tests |
| [Testing & Competition](testing-ops.md) | Test order, event checklists, troubleshooting, references |

Hardware assumed: an **Orange Pi 5** or **Rubik Pi 3** coprocessor running **PhotonVision**, and
**no NVIDIA GPU** on team computers.

## What object detection is

Object detection is a neural network that takes a camera image and returns a list of objects. Each object has:

- a **class** (for example `piece` or `robot`)
- a **bounding box** (where it is in the image)
- a **confidence** (0–1, how sure the model is)

**How it differs from AprilTags:** AprilTags are printed markers at known field positions, used to find out where *your robot* is. Object detection finds things that **don't have tags and move around**: game pieces on the floor, other robots, a piece inside your intake.

We use **YOLO** models (Ultralytics YOLOv5 / v8 / v11). They're fast enough to run in real time on the NPU (AI accelerator chip) of an Orange Pi 5 or Rubik Pi 3.

PhotonVision gives robot code the following for each detection:

| Value | Meaning |
|---|---|
| `yaw` | Horizontal angle to the box center (degrees, **positive = right**) |
| `pitch` | Vertical angle to the box center (degrees, **positive = up**) |
| `area` | Percent of the image the box covers |
| class ID | Which class (index into the labels list) |
| confidence | Model's confidence |
| timestamp | When the frame was captured (for latency compensation) |

## Why use it, and what it's used for

### Why
- **Faster cycles:** the robot lines up on pieces faster and more consistently than a driver.
- **Robust autonomous:** auto still works when pieces get bumped, moved, or taken by a partner.
- **Less driver load:** drivers focus on strategy and defense instead of fine alignment.
- **Field awareness:** the robot knows where pieces are, even ones the driver can't see well.

### On-robot uses

Code for each of these is on [Robot Code](robot-code.md).

| Use | Mode | Description |
|---|---|---|
| **Aim assist** | Teleop | Driver translates, robot automatically points its intake at the piece |
| **Auto pickup (full)** | Teleop | Hold a button and the robot drives onto the piece and intakes it |
| **Semi-auto pickup** | Teleop | Robot steers, driver controls speed with a trigger |
| **Vision-corrected pickup** | Auton | Path gets close, vision takes over for the final approach |
| **Interrupt-on-sight** | Auton | Follow a path, but switch to vision as soon as a piece is confirmed |
| **Dynamic multi-piece auton** | Auton | Robot chooses the best piece, picks it up, scores, repeats, and searches if nothing is visible |
| **Driver feedback** | Teleop | Controller rumble / LEDs when a piece is in range |
| **Auto intake deploy** | Teleop | Deploy intake automatically when a piece is close |

### Other uses
- **Robot detection** (add a `robot` class): avoid defenders, ignore pieces next to opponents.
- **Held-piece check:** a camera inside the intake confirms the piece is seated correctly.
- **Scouting:** run the model on match video to count cycles and map robot movement.

## How the whole pipeline fits together

```text
 Real game piece + official CAD
            │
   ┌────────┼─────────────────────┐
   ▼        ▼                     ▼
 Synthetic  Copy-paste          Real images
 renders    (real cutouts on    (robot camera,
 (Blender-  random backgrounds)  5 venue sessions,
 Proc)                           hand-labeled)
   │        │                     │
   └────┬───┘                     │
        ▼                         ▼
  PRETRAIN (Colab/Kaggle) ──► FINE-TUNE on real ──► TEST on held-out real session
                                                       │
                                                       ▼
                         Convert (.rknn for Orange Pi / .tflite for Rubik Pi)
                                                       │
                                                       ▼
                                 Upload to PhotonVision, tune pipeline
                                                       │
                                                       ▼
                      Robot code: piece tracker → teleop assists + auton routines
```

**Why three data sources?**

| Source | Strength | Weakness |
|---|---|---|
| Synthetic renders | Unlimited angles, lighting, many pieces, occlusion, perfect labels | Can look "fake" |
| Copy-paste | Real appearance, fast, cheap on CPU | Unrealistic edges and shadows |
| Real labeled images | Exactly what the robot sees | Slow to collect and label |

Pretraining on synthetic + copy-paste, then fine-tuning on real, gives the best results with the least labeling.

## Hardware and software checklist

### Robot hardware

| Item | Notes |
|---|---|
| **Orange Pi 5** (RK3588) or **Rubik Pi 3** (Qualcomm QCS6490) | Both are supported for PhotonVision object detection. They use **different model formats** (see [Convert and deploy](training.md#phase-6-convert-and-deploy-to-photonvision)). |
| **Color, global-shutter USB camera** | For example an OV9782 color global-shutter module (see [Vision Resources](../../reference-materials/vision-resources.md) for models and lenses). **Do not use a monochrome AprilTag camera** (e.g. OV9281) for color game pieces. |
| Wide FOV lens (~70–100°) | Keeps pieces in view during the approach |
| Rigid camera mount | Low, angled down toward the intake area. Measure its position precisely. |
| **Buck-boost regulator** | Keeps the coprocessor alive during brownouts. Follow FRC wiring and fusing rules. |
| Cooling (heatsink/fan) | NPUs throttle when hot |
| Intake piece sensor | Beam break, CANrange/ToF, or current detection. Used to end pickup commands. |

### Team computers (no NVIDIA GPU needed)

| Task | Where it runs |
|---|---|
| Synthetic rendering | Team laptops/desktops on **CPU**, or an **AMD/Intel GPU**, or Google Colab ([Synthetic data](data.md#phase-2-synthetic-data-3d-renders)) |
| Copy-paste generation | Any laptop (CPU, fast) |
| Labeling | Browser: **Roboflow** or **CVAT** |
| Training | **Google Colab** or **Kaggle** free cloud GPUs ([Training](training.md#phase-5-training-without-an-nvidia-gpu)) |
| Model conversion | **Google Colab** using PhotonVision's conversion notebooks ([Convert and deploy](training.md#phase-6-convert-and-deploy-to-photonvision)) |

### Software to install locally
- Python 3.10–3.12
- `pip install blenderproc ultralytics opencv-python pillow rembg`
- FreeCAD or Blender (to inspect and convert CAD)
- WPILib, PhotonLib, PathPlanner (robot code)
- AdvantageScope (viewing logs and the piece map)

> **Before anything else:** check the current PhotonVision docs for supported model types. As of the 2026 docs, the Orange Pi 5 pipeline accepts **640×640, quantized, Ultralytics YOLOv5/v8/v11** models converted to `.rknn`. The Rubik Pi 3 uses TensorFlow Lite models converted with a separate notebook. Training the wrong architecture or input size means retraining.
>
> Also check whether PhotonVision ships a **pre-trained model for the current game piece**. It did for the 2026 game. If it does, use it as a baseline to compare against.

## Season timeline

| When | Work |
|---|---|
| **Off-season** | Build and test the **entire** pipeline with a past game piece: rendering, copy-paste, labeling, Colab training, conversion, PhotonVision, tracker, all commands. Fix power and mounting issues. |
| **Kickoff day 1** | Download CAD; measure and photograph the real piece; prepare the mesh; start overnight renders on all team laptops; make copy-paste data; capture Real Session A. |
| **Day 2** | Pretrain in Colab on synthetic + copy-paste; convert; deploy to PhotonVision; first test on the real piece. |
| **Week 1** | Label Session A (with model pre-labels); first fine-tune; aim assist + DriveToPiece working on the test robot. |
| **Weeks 2–4** | Sessions B–E as more pieces arrive; full fine-tune; comparison experiment; auton patterns A/B/C on the competition robot. |
| **Each event** | Field photos on practice day; overnight fine-tune if needed; log review between matches. |

## 2027 notes

- **Coprocessors don't change** with SystemCore: PhotonVision still runs on the Orange Pi or Rubik
  Pi and talks to the robot over the network.
- **PhotonLib** had only development builds for WPILib 2027 as of November 2026. Expect class and
  import names to follow the `org.wpilib` rename. The robot code on this wiki already logs with
  AdvantageKit instead of `SmartDashboard`/`Field2d`, which are removed in 2027.
- **Check PhotonVision's supported model list** for the 2027 release before training. Model
  formats and YOLO versions have changed between seasons.
