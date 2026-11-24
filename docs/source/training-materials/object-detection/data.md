# Object Detection: Training Data

A model is only as good as its training data. We combine three sources: **synthetic 3D renders**,
**copy-paste images** made from photos of the real piece, and **real images from the robot's camera**,
labeled by hand. See the [pipeline diagram](overview.md#how-the-whole-pipeline-fits-together) for how they fit.

:::{admonition} Python on this page
:class: note
The scripts here are Python, not Java. They run on team laptops, not on the robot. Install Python
3.10–3.12 and the packages listed in the [software checklist](overview.md#software-to-install-locally).
The scripts are starting templates: library APIs change between versions, so always do a small test
run first.
:::

## Phase 1: Kickoff day with the real game piece

### Get the CAD
- FIRST publishes field and game piece CAD at kickoff: **Game & Season Materials → Playing Field / Field Drawings & CAD**, usually as STEP files and public Onshape documents.
- Community cleaned-up meshes appear on **Chief Delphi** within hours. AdvantageScope's field assets often include ready-made GLB game piece models.

### Measure and compare the real piece
- Measure key dimensions and compare them to the CAD. Foam and fabric pieces differ from the designed shape.
- Note how it **rests on the floor**: which orientations, whether it rolls, whether it sags.
- Note whether the shape is **convex** (ball, cylinder, cube) or **concave** (ring, hoop). This decides the physics collision shape in the renderer.

### Capture appearance
- **Color reference:** photograph the piece next to a sheet of white paper under even light. Sample the piece's RGB color and write it down as 0–1 values (e.g. `[1.0, 0.45, 0.05]`).
- **Material:** shiny or matte? Fuzzy? This sets the renderer's roughness range.
- **Texture photos:** close-ups of logos, seams, and printing (optional, for higher-quality renders).
- **Optional 3D scan:** apps like Polycam, RealityScan, or Scaniverse produce a textured mesh. Use the CAD for *shape* and the scan or photos for *appearance*.

### Prepare the mesh
1. Export the CAD as **OBJ** (Onshape: right-click part → Export; or open the STEP in FreeCAD → Export).
2. Open it in Blender and check:
   - **Scale:** a 30 cm piece should measure 0.3 m. CAD is often in mm (scale factor 0.001) or inches (0.0254).
   - **Origin:** *Object → Set Origin → Origin to Geometry*.
   - **Polygon count:** apply a *Decimate* modifier if it has hundreds of thousands of faces.
   - **Remove hidden internal parts.**
3. Save as `synth/assets/piece.obj`.

### Same day
- Photograph the piece for **copy-paste cutouts** ([Copy-paste data](data.md#phase-3-copy-paste-data-real-cutouts)).
- Start **Real Session A** in the shop with the robot camera ([Real images](data.md#phase-4-real-images-and-labeling)). With only one piece, focus on single-piece scenes.
- Start the **overnight synthetic render** ([Synthetic data](data.md#phase-2-synthetic-data-3d-renders)).

## Phase 2: Synthetic data (3D renders)

### Concept: domain randomization
A model trained on one perfect-looking render won't work in the real world. The renderer **randomizes everything except the object itself** (lighting, background, floor, camera, distractors, piece color within a small range), so the only consistent thing the model can learn is the piece.

### Folder setup

```text
synth/
  assets/
    piece.obj
    hdris/       ← 20–50 .hdr/.exr environment maps (free CC0 ones from Poly Haven)
  gen.py
  check_labels.py
```

Choose HDRIs that look like FRC venues: gyms, arenas, warehouses, halls, offices, plus a few outdoor ones for variety. 1K–2K resolution is enough.

### Generator script (`gen.py`)

This drops 1–5 pieces onto a floor with physics so they settle naturally, adds red/blue bumper-like distractors, randomizes lighting and appearance, takes several camera views per scene from robot-camera heights, and writes **YOLO labels** directly. Boxes cover only the **visible** part of each piece, matching the rule for hand-labeled images.

> This is a **starting template**. BlenderProc's API changes slightly between versions, so expect minor fixes. Always do a 5-scene test run first.

```python
import blenderproc as bproc   # MUST be the first import
import bpy, numpy as np, random, glob, os, sys
from PIL import Image

# ======================= SETTINGS =======================
SEED          = int(sys.argv[1]) if len(sys.argv) > 1 else 0
OUT           = f"output/run{SEED}"
N_SCENES      = 300            # scenes per run (use 5 for a test)
VIEWS         = 8              # camera views per scene
MAX_PIECES    = 5
N_DISTRACTORS = 4
IMG_W, IMG_H  = 640, 480
FOV_DEG       = 75             # match your real camera's horizontal FOV
PIECE_RGB     = [1.0, 0.45, 0.05]   # measured color of the real piece (0-1)
PIECE_SCALE   = 0.001          # 0.001 if OBJ is in mm, 0.0254 if inches, 1.0 if meters
COLLISION     = "CONVEX_HULL"  # "MESH" for rings/hoops/concave pieces (slower)
MIN_PIXELS    = 60             # skip pieces that are almost fully hidden
SAMPLES       = 24             # render quality; low + denoiser is fine for training
USE_CPU_ONLY  = True           # True for no-GPU machines. See notes for AMD/Intel GPUs.
# ========================================================

np.random.seed(SEED); random.seed(SEED)
bproc.init()
os.makedirs(f"{OUT}/images", exist_ok=True)
os.makedirs(f"{OUT}/labels", exist_ok=True)

# Render settings tuned for CPU
bproc.renderer.set_render_devices(use_only_cpu=USE_CPU_ONLY)
bproc.renderer.set_max_amount_of_samples(SAMPLES)
bproc.renderer.set_denoiser("INTEL")        # Intel Open Image Denoise runs on CPU
bproc.camera.set_intrinsics_from_blender_params(
    lens=np.radians(FOV_DEG), lens_unit="FOV", image_width=IMG_W, image_height=IMG_H)
bproc.renderer.enable_segmentation_output(
    map_by=["category_id", "instance", "name"], default_values={"category_id": 0})

hdris = glob.glob("assets/hdris/*.hdr") + glob.glob("assets/hdris/*.exr")

# ---- Floor ----
floor = bproc.object.create_primitive("PLANE", scale=[6, 6, 1])
floor.enable_rigidbody(active=False, collision_shape="BOX")
floor_mat = bproc.material.create("floor")
floor.replace_materials(floor_mat)

# ---- Extra light (on top of the HDRI) ----
light = bproc.types.Light()
light.set_type("POINT")

# ---- Game pieces ----
base = bproc.loader.load_obj("assets/piece.obj")[0]
base.set_scale([PIECE_SCALE] * 3)
piece_mat = bproc.material.create("piece")
base.replace_materials(piece_mat)
pieces = [base] + [base.duplicate() for _ in range(MAX_PIECES - 1)]
for p in pieces:
    p.set_cp("category_id", 1)
    p.enable_rigidbody(active=True, collision_shape=COLLISION, mass=0.3)

# ---- Distractors: bumper-like bars + random blocks ----
distractors = []
for i in range(N_DISTRACTORS):
    d = bproc.object.create_primitive("CUBE")
    d.set_cp("category_id", 0)
    m = bproc.material.create(f"distractor{i}")
    d.replace_materials(m)
    d.enable_rigidbody(active=False, collision_shape="BOX")
    distractors.append((d, m))

for scene in range(N_SCENES):
    bproc.utility.reset_keyframes()

    # Lighting
    if hdris:
        bproc.world.set_world_background_hdr_img(
            random.choice(hdris), strength=np.random.uniform(0.3, 2.0))
    light.set_location(np.random.uniform([-3, -3, 2], [3, 3, 6]))
    light.set_energy(np.random.uniform(50, 1500))
    light.set_color(np.random.uniform([0.8, 0.8, 0.8], [1, 1, 1]))

    # Appearance
    floor_mat.set_principled_shader_value("Base Color", [*np.random.uniform(0.03, 0.45, 3), 1])
    floor_mat.set_principled_shader_value("Roughness", np.random.uniform(0.6, 1.0))
    c = np.clip(np.array(PIECE_RGB) + np.random.normal(0, 0.05, 3), 0, 1)
    piece_mat.set_principled_shader_value("Base Color", [*c, 1])
    piece_mat.set_principled_shader_value("Roughness", np.random.uniform(0.3, 0.95))

    # Distractors
    for d, m in distractors:
        if np.random.rand() < 0.6:   # bumper-like bar (cube primitive is 2 m, so scale = half size)
            d.set_scale([np.random.uniform(0.3, 0.5), 0.07, 0.06])
            col = [0.8, 0.05, 0.05] if np.random.rand() < 0.5 else [0.05, 0.1, 0.8]
        else:
            d.set_scale(np.random.uniform(0.05, 0.3, 3))
            col = np.random.uniform(0, 1, 3)
        m.set_principled_shader_value("Base Color", [*col, 1])
        d.set_location([*np.random.uniform(-2, 2, 2), d.get_scale()[2]])
        d.set_rotation_euler([0, 0, np.random.uniform(0, 2 * np.pi)])

    # Drop a random number of pieces with physics
    n = np.random.randint(1, MAX_PIECES + 1)
    for i, p in enumerate(pieces):
        if i < n:
            p.hide(False)
            p.set_location(np.random.uniform([-1.2, -1.2, 0.2], [1.2, 1.2, 0.8]))
            p.set_rotation_euler(np.random.uniform(0, 2 * np.pi, 3))
        else:
            p.hide(True)
            p.set_location([50 + i * 3, 50, 0])
    bproc.object.simulate_physics_and_fix_final_poses(
        min_simulation_time=1, max_simulation_time=3, check_object_interval=1)

    # Camera views at robot-camera heights
    active = pieces[:n]
    poi = bproc.object.compute_poi(active)
    added, tries = 0, 0
    while added < VIEWS and tries < 200:
        tries += 1
        loc = poi + np.random.uniform([-3.5, -3.5, 0], [3.5, 3.5, 0])
        loc[2] = np.random.uniform(0.15, 1.0)
        target = poi + np.random.uniform([-0.6, -0.6, -0.1], [0.6, 0.6, 0.1])
        rot = bproc.camera.rotation_from_forward_vec(
            target - loc, inplane_rot=np.random.uniform(-0.08, 0.08))
        cam = bproc.math.build_transformation_mat(loc, rot)
        visible = bproc.camera.visible_objects(cam)
        if any(p in visible for p in active) or np.random.rand() < 0.08:  # ~8% negatives
            bproc.camera.add_camera_pose(cam)
            added += 1

    # Render and write YOLO labels
    data = bproc.renderer.render()
    for v, (img, seg, attrs) in enumerate(zip(
            data["colors"], data["instance_segmaps"], data["instance_attribute_maps"])):
        name = f"r{SEED}_s{scene:05d}_v{v:02d}"
        Image.fromarray(np.uint8(img)).convert("RGB").save(
            f"{OUT}/images/{name}.jpg", quality=int(np.random.randint(70, 95)))
        lines = []
        for a in attrs:
            if a.get("category_id") != 1:
                continue
            ys, xs = np.where(seg == a["idx"])
            if len(xs) < MIN_PIXELS:
                continue
            x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
            lines.append(f"0 {(x0 + x1) / 2 / IMG_W:.6f} {(y0 + y1) / 2 / IMG_H:.6f} "
                         f"{(x1 - x0) / IMG_W:.6f} {(y1 - y0) / IMG_H:.6f}")
        with open(f"{OUT}/labels/{name}.txt", "w") as f:
            f.write("\n".join(lines))   # empty file = negative image
```

Run it (the first run downloads Blender automatically):

```powershell
blenderproc run gen.py 0
```

### Rendering without an NVIDIA GPU

| Option | How | Notes |
|---|---|---|
| **CPU (any computer)** | `USE_CPU_ONLY = True`, low `SAMPLES` + Intel denoiser | Slowest, but works everywhere. Expect a few seconds per image on a modern laptop CPU (more on older ones). |
| **AMD GPU (Radeon RX 6000+)** | `bproc.renderer.set_render_devices(desired_gpu_device_type="HIP")` | Blender Cycles supports AMD via HIP |
| **Intel Arc GPU** | `desired_gpu_device_type="ONEAPI"` | Blender Cycles supports Intel Arc via oneAPI |
| **Apple Silicon Mac** | `desired_gpu_device_type="METAL"` | Works well |
| **Google Colab** | Run BlenderProc in a Colab notebook on a cloud GPU | Possible, but session time limits apply. Save output to Google Drive. |

**Spread the work across the team:** every laptop runs `gen.py` with a **different seed** (`gen.py 0`, `gen.py 1`, ...) overnight. Each writes to its own `output/runN` folder with unique filenames, so you just merge the folders afterward.

**Target:** about **5,000–10,000 synthetic images** total. For example, 4 laptops × 300 scenes × 8 views ≈ 9,600 images.

### Check the labels before the big run

`check_labels.py`: press any key to move to the next image.

```python
import cv2, glob, random, sys
folder = sys.argv[1] if len(sys.argv) > 1 else "output/run0"
for p in random.sample(glob.glob(f"{folder}/images/*.jpg"), 20):
    im = cv2.imread(p); h, w = im.shape[:2]
    for line in open(p.replace("images", "labels").replace(".jpg", ".txt")):
        _, cx, cy, bw, bh = map(float, line.split())
        cv2.rectangle(im, (int((cx - bw/2) * w), int((cy - bh/2) * h)),
                          (int((cx + bw/2) * w), int((cy + bh/2) * h)), (0, 255, 0), 2)
    cv2.imshow("check", im); cv2.waitKey(0)
```

Look for:
- Boxes tight around pieces, with none missing
- Correct piece size (a scale bug makes pieces giant or tiny)
- Pieces resting on the floor, not floating or sunk in
- Varied but sensible lighting (no all-black or all-white frames)
- Distractors present, and some negative images

## Phase 3: Copy-paste data (real cutouts)

This is cheap on CPU and gives **real piece appearance**. It works with just one real piece.

### Capture
1. Photograph the real piece **50–100 times**: every side and resting orientation, near and far, under a few lighting setups. A plain background makes cutting out easier.
2. Collect **200+ background photos** with no game pieces: shop floor, gyms, hallways, past event photos and video frames, carpet close-ups, robots and bumpers.

### Cut out the piece automatically

```powershell
rembg p piece_photos cutouts
```

This writes transparent PNGs. Delete any with bad cutouts.

### Generator (`copypaste.py`)

```python
import cv2, numpy as np, random, glob, os

CUTOUTS     = glob.glob("cutouts/*.png")        # RGBA with transparent background
BACKGROUNDS = glob.glob("backgrounds/*.jpg")
OUT, N, W, H = "copypaste", 3000, 640, 480
os.makedirs(f"{OUT}/images", exist_ok=True); os.makedirs(f"{OUT}/labels", exist_ok=True)

for i in range(N):
    bg = cv2.resize(cv2.imread(random.choice(BACKGROUNDS)), (W, H))
    bg = np.clip(bg.astype(float) * random.uniform(0.6, 1.3), 0, 255).astype(np.uint8)
    labels = []
    for _ in range(random.choice([0, 1, 1, 2, 2, 3, 4])):       # 0 = negative image
        cut = cv2.imread(random.choice(CUTOUTS), cv2.IMREAD_UNCHANGED)
        s = random.uniform(0.04, 0.45) * H / cut.shape[0]         # far to near
        cut = cv2.resize(cut, None, fx=s, fy=s)
        if random.random() < 0.5:
            cut = cv2.flip(cut, 1)
        ch, cw = cut.shape[:2]
        x = random.randint(-cw // 3, W - 2 * cw // 3)              # allow partly off-frame
        y = random.randint(int(H * 0.25), H - ch // 2)             # pieces sit on the floor (lower image)
        x0, y0, x1, y1 = max(x, 0), max(y, 0), min(x + cw, W), min(y + ch, H)
        if x1 - x0 < 8 or y1 - y0 < 8:
            continue
        crop = cut[y0 - y:y1 - y, x0 - x:x1 - x]
        a = crop[..., 3:4].astype(float) / 255.0
        rgb = np.clip(crop[..., :3].astype(float) * random.uniform(0.7, 1.2), 0, 255)
        bg[y0:y1, x0:x1] = (a * rgb + (1 - a) * bg[y0:y1, x0:x1]).astype(np.uint8)
        ys, xs = np.where(crop[..., 3] > 127)
        if len(xs) < 30:
            continue
        bx0, bx1, by0, by1 = x0 + xs.min(), x0 + xs.max() + 1, y0 + ys.min(), y0 + ys.max() + 1
        labels.append(f"0 {(bx0+bx1)/2/W:.6f} {(by0+by1)/2/H:.6f} {(bx1-bx0)/W:.6f} {(by1-by0)/H:.6f}")
    if random.random() < 0.3:                                      # motion blur
        k = random.choice([3, 5, 7]); kernel = np.zeros((k, k)); kernel[k // 2, :] = 1 / k
        bg = cv2.filter2D(bg, -1, kernel)
    cv2.imwrite(f"{OUT}/images/cp_{i:05d}.jpg", bg, [cv2.IMWRITE_JPEG_QUALITY, random.randint(70, 95)])
    open(f"{OUT}/labels/cp_{i:05d}.txt", "w").write("\n".join(labels))
```

**Known limitation:** a piece pasted later can cover an earlier one, leaving that earlier box slightly too big. At this scale it's acceptable, and fine-tuning on real data corrects it.

**Target:** about **2,000–3,000 copy-paste images**. Check them with `check_labels.py copypaste`.

## Phase 4: Real images and labeling

### Session plan (5 × 100 images)

| Session | Location / lighting | Role |
|---|---|---|
| A | Shop (fluorescent) | Train |
| B | Gym / cafeteria (high ceiling, mixed light) | Train |
| C | Strong window light / harsh shadows | Train |
| D | Dim or warm light, or another team's practice field | **Validation** |
| E | **Most competition-like** (week-0 / off-season event / real field) | **Test, never trained on** |

**Within every session:**
- Use the **robot's actual camera at its real mounting position**.
- Vary distance (at the intake out to max range), position in frame (center, edges, partly cut off), and number of pieces (once more pieces arrive).
- Include clutter: robots, bumpers, people, field elements.
- Include some frames taken **while driving** (motion blur).
- Include **10–15 negative images** with bumpers and similar-colored objects but no pieces.
- Aim for **1,000+ labeled piece instances** in total across sessions, not just 500 images.

**Extract frames from robot video** (about 2 frames per second, so frames aren't near-duplicates):

```powershell
ffmpeg -i sessionA.mp4 -vf fps=2 sessionA/frame_%05d.jpg
```

### Labeling tool
- **Roboflow**: easiest, browser-based, team collaboration, AI-assisted labeling, exports YOLO format. Free-tier datasets are **public**.
- **CVAT**: free, can be private and self-hosted, has video interpolation and AI assist.

### Team labeling rules (write these down and share them)
1. Class list: `piece` (class 0). Add `robot` only if code will use it.
2. **Every** qualifying piece in **every** image gets a box. Missing labels are the most harmful error.
3. Boxes are **tight around the visible part**.
4. Label pieces that are partly hidden if roughly **≥25–30% is visible**.
5. Label pieces cut off at the image edge (the box ends at the border).
6. Skip pieces under about **10 px wide** (too far away to matter).
7. Decide and document: do pieces held by robots or already scored count? Usually **no**, if the robot shouldn't chase them.
8. Unsure? **Flag it for review**, don't guess.
9. Images with no pieces are marked as **null/negative**, not deleted.

### Speed up labeling with the model
1. Train an early model on synthetic + copy-paste data ([Training](training.md#phase-5-training-without-an-nvidia-gpu)).
2. Use it to **pre-label** new real images (Roboflow and CVAT both support model-assisted labeling).
3. Humans only correct: fix boxes, add misses, delete false positives. This is typically several times faster.

### Quality control
- A second person reviews each batch.
- Spot-check specifically for **missing** boxes.
- After training, look at images where the model and the labels strongly disagree. Often the label is wrong.

### Export
Export as **YOLO format**: one `.txt` per image, each line `class cx cy w h`, normalized 0–1.
