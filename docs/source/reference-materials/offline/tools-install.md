# Installing the Tools Offline

Local summary of the installation guides linked from
[Development Environment](../../training-materials/setup/dev-environment.md). Get the installers from a mentor's USB
stick if you don't have internet.

## WPILib

**Source:** [WPILib installation guide](https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/wpilib-setup.html)

WPILib ships as a single **disk image (`.iso`)** for Windows (there are also macOS and Linux
versions). It contains everything for Java robot development:

- A team-specific copy of **VS Code** with the WPILib extension
- The **JDK** (Java), so you don't need to install Java separately
- WPILib's libraries in a local Maven folder, so WPILib itself builds without internet
- Tools: **AdvantageScope**, **SysId**, Glass, the Sim GUI, OutlineViewer, Data Log Tool, and others

### Installing (Windows)

1. Right-click the `.iso` file → **Mount**. It appears as a new drive.
2. Run **`WPILibInstaller.exe`** from that drive.
3. Choose **Everything** (not "Tools Only").
4. Choose **Install for this User** unless a mentor tells you otherwise.
5. On the VS Code step, pick one of:
   - **Download for this computer only.** Needs internet.
   - **Select existing VS Code archive for offline install.** Use this with no internet. Point it
     at the VS Code zip from the mentor USB stick.
   - Mentors: the option to **download VS Code for offline installs** creates that zip so other
     laptops can install without internet. Put it on the USB stick next to the `.iso`.
6. Finish. You'll get desktop shortcuts named with the year (for example "2027 WPILib VS Code").

Everything installs under `C:\Users\Public\wpilib\<year>\`. Each season installs side by side with
earlier seasons. Always open the VS Code with the **current year** in its name.

### Uninstalling an old season

Delete `C:\Users\Public\wpilib\<old year>\` and its desktop shortcuts. There's no uninstaller.

## NI FRC Game Tools (Windows only)

**Source:** [FRC Game Tools guide](https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/frc-game-tools.html)

The Game Tools contain the **FRC Driver Station** and the **roboRIO Imaging Tool**. Only laptops
that will drive the robot or image a roboRIO need them. Programming laptops that only write code
and simulate can skip this.

1. Run the installer (the download is large, so keep a copy on the USB stick).
2. Accept the license agreements and the default options.
3. Reboot when asked.

### Driver Station basics

| Area | What it does |
|---|---|
| **Operation tab** | Pick the mode (TeleOperated / Autonomous / Practice / Test), and **Enable** or **Disable** |
| **Diagnostics tab** | Shows robot connection, radio, and roboRIO status |
| **Setup tab** | **Team number** (217) |
| **USB Devices tab** | Controller order. Slot 0 is the driver controller, slot 1 the operator (drag to reorder). Press F1 to rescan. |
| **Status indicators** | Communications, Robot Code, Joysticks. All three must be green to enable. |
| **Messages** | Errors and warnings printed by robot code |

Keyboard safety keys: **Enter** disables the robot. **Space bar** is an **emergency stop**, which
requires rebooting the roboRIO before the robot can be enabled again. Know where these are before
every test.

### roboRIO Imaging Tool

Used once per season (or after a corrupted roboRIO) to install the roboRIO's operating system:
connect over USB, open the tool, set team number 217, select the newest image, check
**Format Target**, and click **Reformat**. Ask a mentor before reimaging the competition robot.

## 2027: Driver Station and SystemCore

For SystemCore robots (2027 and later):

- **2027 FIRST Driver Station:** a separate download from the NI Game Tools, available for Windows,
  macOS, and Linux. Keep the installer for each OS on the USB stick. "Practice" is now **Match
  Mode**, there's an **A-Stop** button, and you can choose an **OpMode**. Enter still disables and
  the space bar still E-Stops; confirm in the 2027 DS before relying on them.
- **No imaging tool.** Configure SystemCore from its web page at `http://robot.local`: set team
  number 217, update the OS image (keep image files on the USB stick), and install packages such as
  AdvantageScope Lite and Elastic.
- **Connecting on the bench:** SystemCore has its own Wi-Fi access point (the SSID and password are
  printed in the setup docs and may be shown on its screen), and a USB connection. Its OLED screen
  shows its IP addresses.

## Git and GitHub Desktop

**Sources:** [git-scm.com](https://git-scm.com/downloads), [desktop.github.com](https://desktop.github.com/)

- **Git for Windows:** run the installer and accept the defaults. One useful change: on the
  "default editor" page, choose **VS Code** instead of Vim.
- **GitHub Desktop** (optional): run the installer and sign in with your GitHub account when you're
  online. Cloning, committing, and branching all work offline; pushing and pulling need internet.

## Phoenix Tuner X

**Source:** [Tuner X documentation](https://v6.docs.ctr-electronics.com/en/stable/docs/tuner/index.html)

Tuner X is distributed through the **Microsoft Store** on Windows (and app stores on mobile). With
no internet, ask a mentor. It must be installed on a laptop while online. Usage is covered in
[Phoenix Tuner X](../../training-materials/ctre/tuner-x.md) and the [CTRE Reference](ctre-reference.md#phoenix-tuner-x).

## PathPlanner

**Source:** [PathPlanner docs](https://pathplanner.dev/home.html)

The PathPlanner GUI is on the **Microsoft Store**, and as a downloadable release on its GitHub page
for offline installs. The library (PathPlannerLib) is already a vendordep in the robot project. Usage
is covered in the [PathPlanner Reference](pathplanner-reference.md).

## Elastic (dashboard)

**Source:** [Elastic docs](https://frc-elastic.gitbook.io/docs)

Elastic is a driver dashboard that reads NetworkTables. It's installed from its GitHub releases
(a Windows installer). Keep a copy on the USB stick.

Using it:

- In **Settings**, set the team number to 217 (or connect to `localhost` for simulation).
- Add widgets by dragging topics from the **Add Widget** list onto a tab. Common widgets: boolean
  boxes, numbers, graphs, the field view, the auto chooser (a `SendableChooser`), and alerts.
- Layouts save as JSON files. **File → Save/Open layout.**
- **Loading the layout from the robot:** the 2026 code serves the files in
  `src/main/deploy/elastic-dashboard/` from the robot on port 5800 (see `WebServer.start(...)` in
  `Robot.java`). Elastic can download its layout from the robot, so every driver station gets the
  same dashboard. Look for the "download layout from robot" option in Elastic's file menu.

## AdvantageScope

AdvantageScope is **included in the WPILib installer**. There's nothing separate to install. Usage is
covered in the [AdvantageKit & AdvantageScope Reference](advantagekit-reference.md#advantagescope).
