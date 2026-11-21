# Install the tools

You need these on the computer you'll work on. It takes about an hour, much
of it waiting for downloads. The instructions assume **Windows**; Linux notes
are at the end.

| Tool | What it's for | Needed for |
|---|---|---|
| Git | Downloading the project and saving your changes | Everything |
| VS Code | Editing code (with helpful extensions) | Everything |
| Flutter SDK | Building and testing the tablet app | Tablet app |
| Android Studio | Supplies the Android SDK that Flutter needs to build APKs | Tablet app |
| Python 3.11 or newer | Running the companion app | Companion app |
| DB Browser for SQLite *(optional)* | Looking inside database files | Data work |

```{admonition} Ask before installing on a school computer
:class: note
You may need an administrator. A personal laptop is easiest.
```

## 1. Git

1. Download from **git-scm.com** and run the installer. The default options
   are fine.
2. Open a new terminal (**Start → "Terminal"** or **PowerShell**) and check:

    ```powershell
    git --version
    ```

3. Tell Git who you are (it puts this on your commits):

    ```powershell
    git config --global user.name "Your Name"
    git config --global user.email "you@example.com"
    ```

## 2. VS Code

1. Install from **code.visualstudio.com**.
2. In VS Code open **Extensions** (the four-squares icon on the left) and
   install:
    - **Flutter** (by Dart Code; it installs the Dart extension too)
    - **Python** (by Microsoft)
    - **Markdown Preview Mermaid Support** (optional: shows the diagrams in
      these docs while you edit them)

## 3. Flutter and Android

Flutter's own guide is the best source and stays current:
**docs.flutter.dev → Get started → Install → Windows → Android**. Follow it,
choosing *Android* as the target. In short:

1. Download the Flutter SDK zip and extract it to a folder **with no
   spaces**, e.g. `C:\dev\flutter`.
2. Add `C:\dev\flutter\bin` to your **PATH**. The guide shows how. Then open
   a *new* terminal.
3. Install **Android Studio**. On first launch let it install the Android
   SDK. Then in **Settings → Languages & Frameworks → Android SDK → SDK
   Tools**, tick **Android SDK Command-line Tools** and click Apply.
4. Accept the Android licences:

    ```powershell
    flutter doctor --android-licenses
    ```

5. Check everything:

    ```powershell
    flutter doctor
    ```

    You want ✓ next to **Flutter** and **Android toolchain**. Warnings about
    Chrome, Visual Studio or iOS don't matter for this project.

```{admonition} Which Flutter version?
:class: note
The app is known to build with **Flutter 3.32** (stable). Newer stable
versions should work. If a new version breaks the build, see
[Known issues](../maintain/known-issues.md). The Android build tools are
pinned to older versions (Android Gradle Plugin 8.3.1, Kotlin 1.8.22),
which is why some packages are pinned too.
```

## 4. Python

1. Install **Python 3.11 or newer** from **python.org**. On the first
   installer screen, tick **"Add python.exe to PATH"**.
2. Check in a new terminal:

    ```powershell
    python --version
    ```

The companion app's packages (PyQt6, SQLAlchemy, pyserial) are installed
into a private folder for this project in [Your first day](first-day.md).

## 5. DB Browser for SQLite (optional)

**sqlitebrowser.org**: a free program for opening `.db` files and looking at
the tables. Handy for [Querying the data](../database/querying.md).

## 6. Get the project

```powershell
cd $HOME\Documents
git clone https://github.com/Team217/chicken_scratch.git
cd chicken_scratch
code .
```

`code .` opens the folder in VS Code. If you don't have access to the
repository, ask a mentor to add your GitHub account to the Team217
organisation.

## Linux notes

- Install Git, Python 3.11+ and `python3-venv` with your package manager
  (e.g. `sudo apt install git python3 python3-venv`).
- Follow Flutter's **Linux → Android** install guide.
- To use a LoRa dongle, add yourself to the `dialout` group and log out and
  back in: `sudo usermod -aG dialout $USER`.
- In commands in these docs, `.venv\Scripts\python` becomes
  `.venv/bin/python`.
