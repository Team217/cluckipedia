# Development Environment

Everything you need to install to write, simulate, and deploy robot code.

:::{admonition} Use the current season's versions
:class: warning
FRC tools are versioned by year. Always install the version that matches the season you are
writing code for. Ask a mentor if you're not sure. The links below always point to the latest
official documentation.
:::

## Install list

| Tool | What it's for | Where to get it |
|---|---|---|
| **WPILib installer** | VS Code with FRC extensions, the JDK, simulation tools, SysId, and AdvantageScope | [WPILib installation guide](https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/wpilib-setup.html) ([offline](../../reference-materials/offline/tools-install.md#wpilib)) |
| **FRC Game Tools** (Windows only) | Driver Station and roboRIO imaging tool | [NI Game Tools guide](https://docs.wpilib.org/en/stable/docs/zero-to-robot/step-2/frc-game-tools.html) ([offline](../../reference-materials/offline/tools-install.md#ni-frc-game-tools-windows-only)) |
| **Phoenix Tuner X** | Configure, update, and test CTRE devices | Microsoft Store (Windows) or [CTRE docs](https://v6.docs.ctr-electronics.com/en/stable/docs/tuner/index.html) ([offline](../../reference-materials/offline/ctre-reference.md#phoenix-tuner-x)) |
| **Git** | Version control | [git-scm.com](https://git-scm.com/downloads) ([offline](../../reference-materials/offline/tools-install.md#git-and-github-desktop)) |
| **GitHub Desktop** (optional) | A visual Git client for beginners | [desktop.github.com](https://desktop.github.com/) ([offline](../../reference-materials/offline/tools-install.md#git-and-github-desktop)) |
| **PathPlanner** | Draw autonomous paths | Microsoft Store or [PathPlanner docs](https://pathplanner.dev/home.html) ([offline](../../reference-materials/offline/pathplanner-reference.md)) |
| **Elastic** | Driver dashboard | [Elastic docs](https://frc-elastic.gitbook.io/docs) ([offline](../../reference-materials/offline/tools-install.md#elastic-dashboard)) |

### Installing WPILib step by step

1. Download the WPILib installer (an `.iso` file) from the link above.
2. Double-click the `.iso` to mount it (or extract it with 7-Zip), then run `WPILibInstaller.exe`.
3. Choose **Everything** as the install mode.
4. For VS Code, choose **Download for this computer only (fastest)**.
5. Restart your computer when the installer finishes.

### Vendor and coprocessor tools

The programming department laptops have all of these. Install them on your own laptop when you
work with that hardware.

| Tool | What it's for | Where to get it |
|---|---|---|
| **REV Hardware Client** | Configure and update REV devices (SPARK MAX/Flex) | [REV docs](https://docs.revrobotics.com/rev-hardware-client) ([offline](../rev/revlib.md#rev-hardware-client)) |
| **Redux Alchemist** | Configure Redux sensors (Canandmag, Canandgyro, Canandcolor) | [Redux docs](https://docs.reduxrobotics.com/alchemist/installation) |
| **Studica navX2-UI** | Configure and test a navX gyro | [navX docs](https://pdocs.kauailabs.com/navx-micro/software/navx-ui/) |
| **Raspberry Pi Imager** or **Balena Etcher** | Write the PhotonVision image to a coprocessor's SD card | [Raspberry Pi Imager](https://www.raspberrypi.com/software/), [Balena Etcher](https://etcher.balena.io/) |

More vendor tools are listed in [Department Resources](../../reference-materials/department-resources.md).

:::{admonition} AdvantageScope
:class: tip
AdvantageScope ships with the WPILib installer. Open it from the WPILib command palette in VS Code
({kbd}`Ctrl+Shift+P` → "WPILib: Start Tool") or from the desktop shortcut.
:::

## Accounts

1. Create a GitHub account. Use a username you'll be fine with in five years.
2. Send your username to a mentor so you can be added to the team's GitHub organization.
3. Set your name and email in Git:

```powershell
git config --global user.name "First Last"
git config --global user.email "you@example.com"
```

## Verify your install

1. Open **WPILib VS Code** (not a regular VS Code install; it has a different icon).
2. Press {kbd}`Ctrl+Shift+P`, then run **WPILib: Create a new project**. Choose *Template → Java →
   Command Robot*, pick a folder, and enter team number **217**.
3. In the new project, run **WPILib: Build Robot Code**. You should see `BUILD SUCCESSFUL`.
4. Run **WPILib: Simulate Robot Code**, choose **Sim GUI**, and confirm that the simulation window
   opens.

If any step fails, copy the **entire** error message into the team's programming channel. A
screenshot of only the last line is almost never enough to diagnose a problem.

## Cloning the robot code

```powershell
git clone https://github.com/<team-org>/<robot-repo>.git
cd <robot-repo>
./gradlew build
```

The first build downloads all the libraries and can take several minutes. After that, builds are fast.

## Useful VS Code shortcuts

| Shortcut | Action |
|---|---|
| {kbd}`Ctrl+Shift+P` | Command palette (all WPILib commands live here) |
| {kbd}`Ctrl+P` | Open a file by name |
| {kbd}`F12` | Go to definition |
| {kbd}`Shift+F12` | Find all references |
| {kbd}`Ctrl+.` | Quick fix (add an import, implement methods) |
| {kbd}`Shift+Alt+F` | Format document |
| {kbd}`F5` | Start debugging |

## 2027 setup differences

- Install the **2027 WPILib** release. It installs next to 2026, and its desktop shortcuts have
  "2027" in the name.
- **Opening a 2026 project in 2027 VS Code:** use the importer when prompted. It converts the
  project, including the package renames to `org.wpilib`. Do it on a branch and review the diff.
- **Driver Station:** SystemCore uses the new **2027 FIRST Driver Station** (Windows, macOS, Linux),
  a separate download from the NI Game Tools.
- **No roboRIO imaging tool:** SystemCore is configured from its web page at `http://robot.local`.
- Windows 10 isn't blocked yet, but future 2027 releases may stop supporting it.

More: [What's New for 2027](../season-2027/whats-new.md).
