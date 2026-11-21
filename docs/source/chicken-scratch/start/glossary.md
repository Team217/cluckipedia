# Glossary

Words you'll see in the app, the code and these docs. Skim it now; come
back when something is unfamiliar.

## FRC and scouting words

Alliance
:   Each match has a **red** and a **blue** alliance of three robots each.

Station
:   Which robot a scout watches: Red 1, Red 2, Red 3, Blue 1, Blue 2, Blue 3.
    Each tablet is set to one station.

Auton (autonomous)
:   The first part of a match (about 20 seconds), when robots drive
    themselves with no human control.

Teleop
:   The main part of the match, when drivers control the robots.

Endgame
:   The last part of teleop, when robots usually climb.

Event key
:   The Blue Alliance's short name for an event: the year plus a code, e.g.
    `2026mitr2` (2026, Michigan, Troy event 2). It appears everywhere in the
    data.

Qualification match ("qual", `qm`)
:   The matches before playoffs, numbered 1, 2, 3, … Most scouting happens
    here. Playoff levels are `sf` (semifinal) and `f` (final).

Alliance selection
:   After quals, top teams pick partners for playoffs. The dashboards exist
    largely to help with this.

The Blue Alliance (TBA)
:   thebluealliance.com, a public site with every event's schedule and
    results. Its *API* lets programs download that data with a key.

Scouting data / record
:   What one scout recorded about one robot in one match.

## Our system's words

Master tablet
:   The tablet the others sync to over Bluetooth, and the one the laptop
    talks to. In settings, **Master tablet** is switched on.

Scouting tablet, slave, worker
:   The other tablets. The code uses all three names.

Companion app
:   The desktop program in the `companion/` folder.

Sync
:   Making two devices hold the same records. Tablets sync over Bluetooth;
    the laptop syncs with the master over LoRa or TCP.

Restore
:   Sending records from the laptop back to a tablet.

Push (admin settings)
:   Sending the roster, events, schedules and settings from the laptop to
    the tablets.

LoRa
:   "Long Range" radio. Cheap USB radio dongles that send small messages a
    long way, slowly. We use a Waveshare USB-TO-LoRa dongle (SX1262 chip).

TCP
:   The normal way programs talk over a network. Our "TCP mode" sends the
    same messages as LoRa, over Wi-Fi or a USB cable instead of radio.

Season
:   One year's game. The data fields change every season.

## Programming words

App / application
:   A program people use, like the tablet app or the companion app.

Source code / code
:   The text files programmers write. The computer turns them into the app.

Repository (repo)
:   The project folder, tracked by **Git** so every change is recorded.
    Ours is on GitHub.

Git, commit, branch
:   Git saves snapshots of the project called **commits**. A **branch** is a
    line of commits you can work on without disturbing others. See
    [Git and teamwork](../maintain/git-workflow.md).

Flutter
:   Google's toolkit for building apps for Android, iOS and more from one
    code base. The tablet app is a Flutter app.

Dart
:   The programming language Flutter apps are written in. Files end in
    `.dart`.

Widget
:   In Flutter, every piece of screen (a button, a page, a row of text) is a
    widget. Screens are widgets made of widgets.

Package / dependency
:   Code someone else wrote that we use, like `sqflite` (database) or
    `qr_flutter` (QR codes). Listed in `pubspec.yaml` (Flutter) and
    `companion/requirements.txt` (Python).

Python
:   The programming language the companion app is written in. Files end in
    `.py`.

PyQt6 / Qt
:   The toolkit the companion app uses to draw windows, buttons and tables.

SQLAlchemy
:   A Python package that makes the database easier to use from Python code.

Database
:   A file that stores data in tables, which programs can search quickly. We
    use **SQLite**: a whole database in one file. See
    [Database basics](../database/basics.md).

SQL
:   The language for asking a database questions, e.g.
    `SELECT * FROM scout_records WHERE team_number = 217`.

JSON
:   A text format for data, e.g. `{"auton_fuel": 6, "climbed": true}`. Used
    for files, messages between devices, and the scouting data inside
    database records.

CSV
:   "Comma-separated values": a plain table format that Excel and Google
    Sheets open.

APK
:   An Android app file. Building the tablet app produces an APK you install
    on the tablets.

Test
:   Code that checks other code works, e.g. "save a record, read it back,
    check it's the same". Run them before every change you share. See
    [Testing](../maintain/testing.md).

Schema
:   The design of a database: which tables exist and what columns they
    have.

Migration
:   Code that upgrades an older database to a newer schema without losing
    data.

Protocol
:   The agreed format of messages between two programs, like the tablets and
    the laptop.

Virtual environment (venv)
:   A private folder of Python packages for one project, so projects don't
    interfere with each other. The companion app uses `companion/.venv`.
