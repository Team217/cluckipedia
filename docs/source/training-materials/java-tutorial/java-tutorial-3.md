# Java Tutorial | Tutorial 3 Java Practices & Robot Driving

Welcome to the third ThunderChickens Java Tutorial.
This tutorial will answer these questions:

1. What are subsystems?
2. What are the different drivebases?
3. What are some example subsystems?

There's a lot to do, so let's get started!


## What are subsystems?

Subsystems are a way for us to divide our code into multiple parts. For example, our robot could have:

**Swerve Subsystem** - How we drive the robot
**ScoreArm Subsystem** - A movable arm on the robot to score points

Think of subsystems as different functions of the robot. We use subsystems to keep the code clean. It would be very hard to find one function of the robot in one single file. It's like trying to find a needle in a haystack! (Except the needle is invisible!). Another use for subsystems is so that multiple people can be coding different things on the robot at the same time. Imagine two people trying to edit the same file at the same time. It would turn out to be quite the disaster.

## What are the different drivebases?

We have used many different driving systems over the years on the team. If you look in our robot graveyard, you may notice some robots with different wheels than the rest. We will focus on three: Tank, Arcade, and Swerve. If you were paying attention, we mentioned the swerve drivebase above!

## Tank Drivebase

ChatGPT says, "A tank drivebase is a drivetrain that uses two joysticks to control the left and right sides of a robot. It's a popular choice for robotics competitions because it offers precise control and maneuverability." There are some downsides, however: it has difficulty with strafing, poor traction when turning, and generally less agility compared to other drive systems.

## Arcade Drivebase

People on GitHub say, "An arcade drivebase is a method of controlling the motors of a tank drive drivetrain using two axes of a controller, where one of the axes controls the speed of the robot, and the other the steering of the robot." The arcade drivebase is mostly the same as the tank drivebase, but it changes the functions of the controller axies.

## Swerve Drivebase

Back to ChatGPT! "A swerve drive base is a type of robot drivebase that allows for highly precise and versatile movement in any direction. Unlike traditional drivebases that rely on a set of wheels that move forward, backward, or rotate (like tank drives), a swerve drive base uses individual wheels, each capable of rotating independently." This is the drivebase we use most often, as it is the fastest and most maneuverable.

## What are some example subsystems?

If you want to take a peek at some subsystems, you can download some examples of subsystems from our 2025 robot. Just press the "Download Subsystems" button below! Unzip the file using 7-Zip!

And there ya' go! You are all set to start learning about connecting hardware! Proceed to the next tutorial to learn about connecting hardware.

<br>
Made with ♡ by Jacob