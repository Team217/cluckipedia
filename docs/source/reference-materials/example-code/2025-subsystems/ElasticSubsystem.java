// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import edu.wpi.first.net.WebServer;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class ElasticSubsystem extends SubsystemBase {
  public void DashboardInit() {
    WebServer.start(5800, Filesystem.getDeployDirectory().getPath());
    //System.out.println("Working");
  }
}
