# Object Detection: Training & Deploying

With the data from [Training Data](data.md), this page trains a YOLO model on free cloud GPUs,
converts it for the coprocessor, and sets up the PhotonVision pipeline that the
[Robot Code](robot-code.md) reads.

## Phase 5: Training without an NVIDIA GPU

### Where to train

| Option | Cost | Notes |
|---|---|---|
| **Google Colab** (free GPU) | Free tier | **Recommended.** Sessions time out, so save runs to Google Drive and resume. |
| **Kaggle Notebooks** | Free weekly GPU quota | Good backup when Colab limits hit |
| **Roboflow Train** | Credits / paid | Easiest, but less control. Confirm you can export a compatible YOLO `.pt`. |
| **Local CPU** | Free | Too slow for pretraining on 10k images. OK for a **small fine-tune** (a few hundred real images) if needed. |
| **Apple Silicon Mac** | Free | Ultralytics supports `device=mps` |

### Dataset layout

```text
datasets/
  synth/      images/train/  labels/train/       ← merged gen.py runs
  copypaste/  images/        labels/
  real/
    images/train/ labels/train/                  ← sessions A, B, C
    images/val/   labels/val/                    ← session D
    images/test/  labels/test/                   ← session E
```

Ultralytics finds each label by replacing `images` with `labels` in the image path, so keep this structure.

**`pretrain.yaml`** trains on synthetic + copy-paste but **validates on real session D**, so the "best" checkpoint is the one that works best on *real* images:

```yaml
path: /content/datasets
train:
  - synth/images/train
  - copypaste/images
val: real/images/val
names:
  0: piece
```

**`real.yaml`**

```yaml
path: /content/datasets
train: real/images/train
val: real/images/val
test: real/images/test
names:
  0: piece
```

(If real images don't exist yet on kickoff day, temporarily point `val` at a 10% slice of synthetic. Just know that score will look better than real-world performance.)

### Colab notebook cells

Zip the `datasets` folder (with both YAML files inside it) and upload it to Google Drive as `MyDrive/frc/datasets.zip`. Then in Colab (**Runtime → Change runtime type → GPU**):

```text
# Cell 1: setup
from google.colab import drive
drive.mount('/content/drive')
!pip install -q ultralytics albumentations
!unzip -q /content/drive/MyDrive/frc/datasets.zip -d /content/
```

```text
# Cell 2: pretrain on synthetic + copy-paste (imgsz MUST be 640 for Orange Pi 5)
!yolo detect train data=/content/datasets/pretrain.yaml model=yolo11n.pt imgsz=640 \
    epochs=100 batch=32 project=/content/drive/MyDrive/frc/runs name=pretrain
```

```text
# Cell 3 (only if the session disconnected): resume
!yolo detect train resume model=/content/drive/MyDrive/frc/runs/pretrain/weights/last.pt
```

```text
# Cell 4: fine-tune on real images
!yolo detect train data=/content/datasets/real.yaml \
    model=/content/drive/MyDrive/frc/runs/pretrain/weights/best.pt imgsz=640 \
    epochs=40 optimizer=SGD lr0=0.001 project=/content/drive/MyDrive/frc/runs name=finetune
```

```text
# Cell 5: final test on the untouched session E
!yolo detect val model=/content/drive/MyDrive/frc/runs/finetune/weights/best.pt \
    data=/content/datasets/real.yaml split=test
```

Notes:
- **`yolo11n`** (nano) is the right size for these NPUs. Use the YOLO version your PhotonVision version supports (v5, v8, or v11 for Orange Pi).
- `optimizer=SGD` must be set explicitly. With the default `auto`, Ultralytics ignores `lr0`.
- Installing `albumentations` adds blur, noise, and contrast augmentations automatically.
- Check `train_batch0.jpg` in the run folder to confirm the labels look right, and `results.png` for the training curves.

### The comparison experiment (do it once)
Train these and compare on the **same real validation session**:

| Model | Training data |
|---|---|
| 1 | Real only |
| 2 | Synthetic + copy-paste only |
| 3 | Synthetic + copy-paste → real fine-tune |

This confirms the pipeline is worth it, and it's good material for judging interviews.

### Optional: leave-one-session-out check
Train five fine-tuned models, each holding out a different session, and average the scores. This estimates how well the model handles **a venue it has never seen**, and shows which environment is hardest, so you know where to collect more data.

### Metrics that matter for the robot
- **Recall** at your chosen confidence threshold: fraction of real pieces found
- **False positives on negative images**, especially bumpers
- Performance on **small/far** pieces versus **close** pieces
- **FPS on the coprocessor** after conversion ([Convert and deploy](training.md#phase-6-convert-and-deploy-to-photonvision))

### Improving the data based on failures

| Symptom | Fix |
|---|---|
| Misses pieces in bright or dim venues | Wider light-strength ranges, more HDRIs, more real sessions in that lighting |
| False positives on bumpers | More bumper distractors and bumper negatives |
| Misses far or small pieces | Camera positions farther out in `gen.py`, lower `MIN_PIXELS`, more small copy-paste pieces |
| Misses pieces at the image edge / near the robot | More off-center camera targets, more partially cut-off pieces |
| Misses pieces while driving | More blur augmentation, more real frames taken in motion |
| Synthetic-only model much worse than fine-tuned | Improve piece color and material in renders, add real floor/carpet backgrounds |

## Phase 6: Convert and deploy to PhotonVision

### Convert the model

PhotonVision does **not** train models. It runs converted ones. Each coprocessor has its own conversion notebook, and the tools are **Linux-only**, so run them in **Google Colab**.

| Coprocessor | Format | Conversion |
|---|---|---|
| **Orange Pi 5** (RK3588 NPU) | `.rknn`, **quantized**, **640×640**, YOLOv5/v8/v11 | PhotonVision's Orange Pi conversion notebook (see "Converting Custom Models" in the docs) |
| **Rubik Pi 3** (Qualcomm NPU via TFLite) | `.tflite` | PhotonVision's `rubik_conversion.ipynb`. In Colab: **File → Open notebook → GitHub tab**, paste the PhotonVision GitHub URL, select the notebook. |

**Quantization calibration images:** the Orange Pi conversion quantizes the model to INT8 and needs a set of sample images. Use **100–300 real images** from your sessions that cover your lighting variety. After converting, check that accuracy didn't drop much. A few percent is normal.

### Upload to PhotonVision
1. Open the PhotonVision UI (`http://photonvision.local:5800` or the coprocessor's IP).
2. **Settings → Object Detection → Upload Model**. Select the file and fill in the properties: labels (in training order, e.g. `piece`), input size, and model version.
3. **Back up** models and settings using PhotonVision's export feature.

### Configure the camera pipeline
1. Create a pipeline on the intake camera and set its type to **Object Detection**. Select your model.
2. **Confidence threshold:** start at **0.5**. Raise it if you see false positives, lower it if you miss pieces.
3. **NMS threshold:** the default is usually fine. Lower it if one piece produces two boxes.
4. **Target sorting:** "Largest" or "Closest to center" (robot code does its own selection anyway).
5. **Camera settings:** **turn off auto exposure and auto white balance**. Use short exposure to cut motion blur, with gain or brightness adjusted so the image still looks natural.
6. **Camera calibration:** run PhotonVision's calibration with a printed checkerboard or ChArUco board at the resolution you'll use. This is essential for accurate angles and distances.
7. **Name the camera** (e.g. `intakeCam`). This string must match robot code.

### Check on the device
- **FPS ≥ 30** and reasonable latency (shown in the UI)
- No thermal throttling after 5+ minutes of running
- Move a piece around: boxes should be stable, with no detections on bumpers or people
