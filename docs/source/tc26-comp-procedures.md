# 2026 ThunderChickens Programmer Competition Procedures

[TOC]

## Before the event

Before we arrive at the event we need to setup a new branch to hold our changes during the competition as well as collect the equipment and parts needed to support the event.

- Create a new branch for the competition following the defined branch naming scheme `comp/{compID}`
- Collect following equipment:
  - The Driver Station.
  - Primary and secondary programming laptops.
  - Two (2) back-up Xbox controllers.
  - 25' ethernet cable & USB-to-ethernet adapter.
  - 8.5"x11" AprilTag calibration target.
  - Official AprilTag on backing board.
- Collect the following spare parts:
  - Back-up MicroSD cards pre-flashed and configured with RoboRIO image, PhotonVision for each co-processor on the competition robot.
  - Spare cameras, at least one (1) of each represented on the robot. Along with associated cables.
  - Spare co-processor, at least one of each represented on the robot.
- Print any documentation to support conversation with judges at the event.
- Coordinate with Mechanical to ensure game elements are packed and ready to go.


## At Event Load-In

- Unpack Driver Station and programming laptops. Get charging cords plugged into the battery cart.
- Launch `VS Code`, `FRC Driver Station`, `Elastic`, `AdvantageScope`, and `PathPlanner` on the programming laptop.
- Get robot radio (VH-109) flashed at the radio programming kiosk.
- Remove lens covers from the cameras. Store them in the programming laptop bag so they don't get lost.


## When The Robot Enters the pit

Every time the robot enters the pit we need to pull the most recent match logs off of the robot. These logs are vital to preemptively catching issues with the robot before they impact the matches or troubleshooting issues on the field. Before we make any changes to the codebase we need to make a new branch. This allows us to track exactly what code is running on the robot for any given match.

- Ask the drive team if there were any issues with the robot during the last match. Controls not working as expected, mechanism failed, etc. Information from the drive team helps narrow down the issue and focus our diagnostic investigation.
- Take the Driver Station computer from the drive team and get it plugged in and charging.
- Connect to robot.
  - Power on robot
  - Turn on & log into programming laptop.
  - Connect USB-to-Ethernet adapter to the laptops USB port.
  - Plug Ethernet cable into the laptop Ethernet adapter.
  - Plug Ethernet cable into the `DS` port on the robot radio (VH-109).
- Download logs from robot.
  - Launch AdvantageScope.
  - Click `File` > `Download Logs...` to open the download window. Once connected to the robot, available logs are shown with the newest at the top.
  - Select the log files from the previous match. Should have naming like `_Q#`. There are three (3) distinct log types we need to download.
    - `.wpilog`: AdvantageKit logging
    - `.revlog`: Rev Hardware logging
    - `.hoot`: CTRE Hardware logging (in dedicated folder, click on folder to download it).
  - Click the ↓ symbol and select the `2026-robot-code/competition-logs/{event-code}` folder to save the logs to.
- Create match specific branch
  - Create new branch representing the upcoming match number following the defined branch naming scheme `comp/{compID}/match/{match-number}`
  - Commit any changes made to the codebase since the robot's last match.
- Review the match log data for any anomalies.
- Time permitting copy the Driver Station logs to the `2026-robot-code/competition-logs/{event-code}` folder. The Driver Station Logs are stored on the Desktop of the computer.


## Before the Robot Leaves the Pit

Match readiness is the single most important thing we do in the oit. When the robot leaves the pit it needs to be checked and ready to go for its next match. To ensure readiness we do functional system checks of the robot components.

- Adjust Auton based on performance in the previous match. This can be adjusting the commands that execute or modifying the paths. The drive coach may request a new Auton for a specific match or alliance. If we can execute in the time we have before the match then we will attempt to construct it. If we are unable to do it confidently we need to communicate that and suggest an alternative. The decision ultimately comes down to the drive coach.
- Make any changes to the codebase based on analysis of logs or match performance. *Any changes to the code should be tested to the best of our ability in the pits*. Avoid running untested code during a match.
- Commit code changes and deploy code to the robot.
- Preform a system check - season specific checklist outlined at the end of this document.
- On Driver Station computer:
  - Launch the `FRC Driver Station` software.
  - Launch `Elastic` dashboard.
  - Open `Chrome` browser and navigate to http://pv-one:5800 & http://pv-two:5800. This will allow the drive team to do a final camera check once the robot is on the field.


## At Test or Practice Areas

When at the Test or Practice areas at an event, the programming student/mentor is in charge. The rest of the pit crew with you is there to support the testing you need to complete. Time is limited in test areas and others are often waiting for their turn. We need to stay focused on what we came to test. When finished get out of the way quickly to let another team get their chance.

- Talk to the volunteer in charge of the area and let them know you want to test your robot. You need to let them know what element of the field you need to use.
- If there is a line (there often is), you can use this time to get the robot powered up and the programming laptop connected to the robot. You want to be ready to test as soon as we have access to the field.
- The Mechanical students in the pit will load the robot onto the test/practice area. We need to tell them were we want it.
- If testing Auton: **VERIFY YOU HAVE THE SPACE TO RUN THE PATH. IF YOU DO NOT HAVE THE SPACE YOU DO NOT RUN THE PATH.**
- When making changes to the code, do it quickly and precisely.
- Load the reduced AprilTag set for the test area. These tag sets support each individual field element (Hub/Outpost/Tower). As the test element locations may not be positioned properly relative to each other. We don't want our vision data compromised.


## On the Field for Sensor Calibration

With our increasing reliance on vision for the robot to perform correctly in a match the measurement and calibration time on the field is important to get right.

- Connect to robot.
  - Power on robot
  - Turn on & log into programming laptop.
  - Connect USB-to-Ethernet adapter to the laptops USB port.
  - Plug Ethernet cable into the laptop Ethernet adapter.
  - Plug Ethernet cable into the `DS` port on the robot radio (VH-109).
- Vision System Checks
  - [ ] Verify `PV-ONE` is power on and connected to the robot. Navigate to http://pv-one:5800.
  - [ ] Verify camera `left-rear-camera` is connected and streaming on the dashboard. Place AprilTag in frame and verify 3D processing detects the tag.
  - [ ] Verify `PV-TWO` is power on and connected to the robot. Navigate to http://pv-two:5800.
  - [ ] Verify camera `right-rear-camera` is connected and streaming on the dashboard. Place AprilTag in frame and verify 3D processing detects the tag.
  - [ ] Verify camera `hopper-right-camera` is connected and streaming on the dashboard. Place AprilTag in frame and verify 3D processing detects the tag.
  - [ ] Verify accurate pose estimation in `AdvantageScope`. The field view in `AdvantageScope` should accurately reflect where we are on the field and remain stable.
    - [ ] Move the robot close, far, angled, and around the field to ensure no extra targets are found and pose is accurate.
- Tune Cameras
  - Exposure time should be set as low as possible while still allowing for the target to be reliably tracked.
  - Increasing gain/brightness as needed.
  - [ ] Tune camera `left-rear-camera`.
  - [ ] Tune camera `right-rear-camera`.
  - [ ] Tune camera `hopper-right-camera`.
- [ ] Run WPICal to build our own AprilTagLayout. Generate the tag layout and upload it to our robot. Test on field time permitting.
- [ ] Test turret and hood auto alignment routine is working as expected.
- Physical Field Measurements:
  - [ ] Red Hub X/Y/Z measurements. Verify against our own field measurements.
  - [ ] Blue Hub X/Y/Z measurements. Verify against our own field measurements.
  - [ ] Red right Trench Z measurement. Taken at both ends of the trench. (make sure its not sloped).
  - [ ] Red left Trench Z measurement. Taken at both ends of the trench. (make sure its not sloped).
  - [ ] Blue right Trench Z measurement. Taken at both ends of the trench. (make sure its not sloped).
  - [ ] Blue left Trench Z measurement. Taken at both ends of the trench. (make sure its not sloped).


## After the Event

At the end of the event we need to tear down the pit and get everything back to the build space. Any code changes made during the event become part of our `Main` branch and need to get merged.

- Replace lens covers for the cameras.
- Support pit crew and mentors in packing up the pit and robot. Power down devices, bundle and secure cables and controllers, etc.
- Merge the competition brach back to the `Main` branch.
- Return the equipment and spare parts back to the Programming Department.

## 2026 Competition Robot Pit Checklist
- System Checks
  - [ ] Verify `PV-ONE` is power on and connected to the robot. Navigate to http://pv-one:5800.
  - [ ] Verify camera `left-rear-camera` is connected and streaming on the dashboard. Place AprilTag in frame and verify 3D processing detects the tag.
  - [ ] Verify `PV-TWO` is power on and connected to the robot. Navigate to http://pv-two:5800.
  - [ ] Verify camera `right-rear-camera` is connected and streaming on the dashboard. Place AprilTag in frame and verify 3D processing detects the tag.
  - [ ] Verify camera `hopper-right-camera` is connected and streaming on the dashboard. Place AprilTag in frame and verify 3D processing detects the tag.
- Driver Checks
  - Switch/pickup to the driver controller.
  - [ ] Verify swerve controls are functional & modules respond properly to input
    - [ ] Press up/down on the left Y axis. The wheels should all point front/back and spin in the same direction.
    - [ ] Press left/right on the left X axis. The wheels should all point left/right and spin in the same direction.
    - [ ] Press left/right on the right X axis. The wheels should all point at a 45° angle (making a crude circle) and spin in the same direction.
  - [ ] Align all swerve modules facing forward and bevel gear is facing the left side of the robot.
- Operator Checks
  - Switch/pickup to the operator controller.
  - Before enabling the robot you must verify:
    - Pivot is in upright stowed position and the pivot encoder reads ~0.
    - Turret is centered facing toward the intake and the turret encoder reads ~0.
    - Hood is lowered completely and the hood potentiometer reads ~0.
  - [ ] Verify Intake Roller spins.
  - [ ] Verify Pivot stows and deploys the intake.
  - [ ] Verify Hopper belts spin.
  - [ ] Verify Feed wheels spin.
  - [ ] Verify Turret can move between it's limits.
  - [ ] Verify Hood angle adjusts between its limits.
  - [ ] Verify Flywheel can maintain velocity setpoint.
