// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;
import org.littletonrobotics.junction.Logger;
import org.photonvision.PhotonCamera;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.SwerveConstants;
import frc.robot.Constants.VisionConstants;
import frc.robot.Constants.VisionConstants.Mode;
import frc.robot.utilities.LoggedTunableNumber;
import frc.robot.utilities.pathplanner.PathPlanner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;
import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.estimator.PoseEstimator;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;


public class VisionSubsystem extends SubsystemBase {
  private PhotonCamera flAprilCamera;
  private PhotonCamera frAprilCamera;
  private PhotonCamera rearAprilCamera; 
  private List<Integer> possibleFrontTargets = new ArrayList<Integer>(); 
  private List<Integer> possibleRearTargets = new ArrayList<Integer>();
  List<PhotonTrackedTarget> targets; 
  Optional<PhotonTrackedTarget> target = Optional.empty();
  // private Alert latency; 
  // private Alert cameraAlert;
  private PIDController xController; 
  private PIDController yController;
  private PIDController rotController;
  private double velocityX = 0;
  private double velocityY = 0;
  private double velocityRot = 0;
  private Transform3d camToTarget;
  private double reefTagAngle;
  private char targetBranch = 'C'; 
  private double branchOffset = 0;
  private Mode visionMode = Mode.REEF;
  private boolean toggle = true;

  // Tuning SETUP
  private LoggedTunableNumber tunableXControllerP;
  private LoggedTunableNumber tunableXControllerI;
  private LoggedTunableNumber tunableXControllerD;
  
  private LoggedTunableNumber tunableYControllerP;
  private LoggedTunableNumber tunableYControllerI;
  private LoggedTunableNumber tunableYControllerD;
  
  private LoggedTunableNumber tunableRotControllerP;
  private LoggedTunableNumber tunableRotControllerI;
  private LoggedTunableNumber tunableRotControllerD;
  
  AprilTagFieldLayout fieldLayout = AprilTagFieldLayout.loadField(AprilTagFields.k2025ReefscapeAndyMark);
  
  private HashMap<Character, int[]> branchMap = new HashMap<Character, int[]>(); 
  
  public VisionSubsystem(String flAprilCam, String frAprilCam, String rearAprilCam, String frontDriveCam){
    //Tuning PID stuff
    tunableXControllerP = new LoggedTunableNumber(this.getName() + "/XCtrl/P", VisionConstants.kXControllerP);
    tunableXControllerI = new LoggedTunableNumber(this.getName() + "/XCtrl/I", VisionConstants.kXControllerI);
    tunableXControllerD = new LoggedTunableNumber(this.getName() + "/XCtrl/D", VisionConstants.kXControllerD);
    
    tunableYControllerP = new LoggedTunableNumber(this.getName() + "/YCtrl/P", VisionConstants.kYControllerP);
    tunableYControllerI = new LoggedTunableNumber(this.getName() + "/YCtrl/I", VisionConstants.kYControllerI);
    tunableYControllerD = new LoggedTunableNumber(this.getName() +  "/YCtrl/D", VisionConstants.kYControllerD);
    
    tunableRotControllerP = new LoggedTunableNumber(this.getName() + "/RotCtrl/P", VisionConstants.kRotControllerP);
    tunableRotControllerI = new LoggedTunableNumber(this.getName() + "/RotCtrl/I", VisionConstants.kRotControllerI);
    tunableRotControllerD = new LoggedTunableNumber(this.getName() + "/RotCtrl/D", VisionConstants.kRotControllerD);
    
    xController = new PIDController(tunableXControllerP.get(), tunableXControllerI.get(), tunableXControllerD.get());
    yController = new PIDController(tunableYControllerP.get(), tunableYControllerI.get(), tunableYControllerD.get());
    rotController = new PIDController(tunableRotControllerP.get(), tunableRotControllerI.get(), tunableRotControllerD.get());
    rotController.enableContinuousInput(Units.degreesToRadians(-180), Units.degreesToRadians(180));
    
    branchMap.put('A', new int[] {7, 18}); 
    branchMap.put('B', new int[] {7, 18}); 
    branchMap.put('C', new int[] {8, 17}); 
    branchMap.put('D', new int[] {8, 17}); 
    branchMap.put('E', new int[] {9, 22}); 
    branchMap.put('F', new int[] {9, 22}); 
    branchMap.put('G', new int[] {10, 21}); 
    branchMap.put('H', new int[] {10, 21}); 
    branchMap.put('I', new int[] {11, 20}); 
    branchMap.put('J', new int[] {11, 20}); 
    branchMap.put('K', new int[] {6, 19}); 
    branchMap.put('L', new int[] {6, 19}); 
    
    flAprilCamera = new PhotonCamera(flAprilCam);
    frAprilCamera = new PhotonCamera(frAprilCam);
    rearAprilCamera = new PhotonCamera(rearAprilCam); 
    
    int[] list = this.getAprilTags(); 
    
    for(int i = 0; i < 5; i++){
      possibleRearTargets.add(list[i]); 
    }
    
    for(int i = 5; i < 11; i++){
      possibleFrontTargets.add(list[i]); 
    }
  }
  
  // Commands to switch vision tasks.
  public Command CoralStation(){ //uses rearCamera only
    return runOnce(
    () -> {
      int[] aprilTags = this.getAprilTags(); 
      //removes all the possibleRearTargets that are not part of the coralStation
      for(int i = 0; i< possibleRearTargets.size(); i++){
        if((possibleRearTargets.get(i) != aprilTags[0]) && (possibleRearTargets.get(i) != aprilTags[1])){
          possibleRearTargets.remove(i); 
          i--;  
        }
      }
      visionMode = Mode.CORALSTATION;
    });
  }
  
  public Command Processor(){
    return runOnce(
    () -> {
      int[] aprilTags = this.getAprilTags(); 
      
      for(int i = 0; i < possibleFrontTargets.size(); i++){
        if(possibleFrontTargets.get(i) != aprilTags[2]){ //theres only 1 val for the processor
          possibleFrontTargets.remove(i); 
          i--; 
        }
      }
      
      for(int i = 0; i < possibleRearTargets.size(); i++){
        if(possibleRearTargets.get(i) != aprilTags[2]){ //theres only 1 val for the processor
          possibleRearTargets.remove(i); 
          i--; 
        }
      }
      visionMode = Mode.PROCESSOR;
    }); 
  }
  
  // -1 is rearCamera 
  //  1 is frontCamera
  public Command Processor(int x){
    return runOnce(
    () -> {
      
      int[] aprilTags = this.getAprilTags(); 
      
      if(x == -1){
        for(int i = 0; i < possibleRearTargets.size(); i++){
          if(possibleRearTargets.get(i) != aprilTags[2]){ //theres only 1 val for the processor
            possibleRearTargets.remove(i); 
            i--; 
          }
        }
      } else if(x == 1){
        for(int i = 0; i < possibleFrontTargets.size(); i++){
          if(possibleFrontTargets.get(i) != aprilTags[2]){ //theres only 1 val for the processor
            possibleFrontTargets.remove(i); 
            i--; 
          }
        }
      }
      visionMode = Mode.PROCESSOR;
    }); 
  }
  
  
  public Command Barge(){
    return runOnce(
    () -> {
      
      int[] aprilTags = this.getAprilTags(); 
      
      for(int i = 0; i < possibleFrontTargets.size(); i++){
        if((possibleFrontTargets.get(i) != aprilTags[3]) && (possibleFrontTargets.get(i) != aprilTags[4])){
          possibleFrontTargets.remove(i); 
          i--; 
        }
      }
      
      for(int i = 0; i < possibleRearTargets.size(); i++){
        if((possibleRearTargets.get(i) != aprilTags[3]) && (possibleRearTargets.get(i) != aprilTags[4])){
          possibleRearTargets.remove(i); 
          i--; 
        }
      }
      visionMode = Mode.PROCESSOR;
    }); 
  }
  
  // -1 is rearCamera 
  //  1 is frontCamera
  public Command Barge(int x){
    return runOnce(
    () -> {
      
      int[] aprilTags = this.getAprilTags(); 
      
      if(x == -1){
        for(int i = 0; i < possibleRearTargets.size(); i++){
          if((possibleRearTargets.get(i) != aprilTags[3]) && (possibleRearTargets.get(i) != aprilTags[4])){
            possibleRearTargets.remove(i); 
            i--; 
          }
        }
      } else if (x == 1){
        for(int i = 0; i < possibleFrontTargets.size(); i++){
          if((possibleFrontTargets.get(i) != aprilTags[3]) && (possibleFrontTargets.get(i) != aprilTags[4])){
            possibleFrontTargets.remove(i); 
            i--; 
          }
        }
      }
      visionMode = Mode.BARGE;
    }); 
  }
  
  public Command ScoreOnReef(){
    return runOnce(
    () -> {
      int[] aprilTags = this.getAprilTags(); 
      
      possibleFrontTargets.clear(); 
      for(int i = 5; i < aprilTags.length; i++){
        possibleFrontTargets.add(aprilTags[i]); 
      }
      visionMode = Mode.REEF;
    });
  }
  
  public Command DriveTo(SwerveSubsystem swerveSubsystem){
    return run(
    () -> {
      if(target.isEmpty()){
        Logger.recordOutput(this.getName() + "/DriveTo", false);
        double angle = swerveSubsystem.getHeading().getRadians();
        swerveSubsystem.drive(swerveSubsystem.getTargetSpeeds(0,0,Rotation2d.fromRadians(angle)));
      }
      else{
        Logger.recordOutput(this.getName() + "/DriveTo", true);
        swerveSubsystem.drive(new ChassisSpeeds(velocityX,velocityY, velocityRot));
      }
    }).until(()->this.atTarget()); 
  }
  
  public Command PPOverride (PathPlanner pathPlanner){
    return run(
    () -> {
      if(target.isEmpty()){ 
        Logger.recordOutput(this.getName() + "/PPOveride", false);
        pathPlanner.ClearX();
        pathPlanner.ClearY();
        pathPlanner.ClearRotation();
      }
      else{
        Logger.recordOutput(this.getName() + "/PPOveride", true);
        pathPlanner.OverrideX(()->velocityX);
        pathPlanner.OverrideY(()->velocityY);
        pathPlanner.OverrideRotation(()->velocityRot);
      }
    }).until(()->this.atTarget()); 
  }
  
  public BooleanSupplier hasTarget() {
    return ()->checkID(branchLookup(targetBranch));
  }
  
  public boolean checkID(int x){
    if(target.isEmpty()){
      return false; 
    }
    return (x == target.get().getFiducialId());
  }
  
  //gets the april tags according to alliance. Default to Blue
  public int[] getAprilTags(){
    if (!(DriverStation.getAlliance().isEmpty()) && (DriverStation.getAlliance().get() == Alliance.Red)){  
      int[] list = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
      return list; 
    }
    int[] list = {12, 13, 16, 14, 15, 17, 18, 19, 20, 21, 22}; //had to order this weird
    return list; 
  }
  
  public List<PhotonTrackedTarget> filterListByPossibleList(List<PhotonTrackedTarget> x, List<Integer> possible){
    for(int i = 0; i < x.size();){
      boolean found = false; 
      for(int j = 0; j < possible.size(); j++){
        if(x.get(i).getFiducialId() == possible.get(j)){
          found = true; 
        }
      }
      
      if(found == false){
        x.remove(i);
      } else {
        i++; 
      }
    }
    return x; 
  }
  
  public void setTargetBranch(char x){
    targetBranch = x; 
  }
  
  // Branch filtering methods.
  public int branchLookup(char x){
    if(branchMap.get(x) == null){
      return -1; 
    }
    if((DriverStation.getAlliance().isPresent()) && (DriverStation.getAlliance().get() == Alliance.Blue)){
      return branchMap.get(x)[1];
    }
    return branchMap.get(x)[0]; 
  }
  
  public boolean seizeBranch(char x){      
    return checkID(branchLookup(x));    
  }
  
  public boolean inDistance(){
    if(!target.isEmpty() & camToTarget.getX() < VisionConstants.kXDistance){
      return true;
    }
    else{
      return false;
    }
  }
  
  public boolean atTarget() {
    if(hasTarget().getAsBoolean() && (Math.abs(xController.getError()) <= SwerveConstants.kXTolerance) && (Math.abs(yController.getError()) <= SwerveConstants.kYTolerance)) {
      return true;
    }
    return false;
  }
  
  private Optional<PhotonTrackedTarget> frontRightCamera(){
    List<PhotonTrackedTarget> frTargets; 
    Optional<PhotonTrackedTarget> frTarget = Optional.empty();
    List<PhotonPipelineResult> unRead = new LinkedList<PhotonPipelineResult>();
    
    // If we have access to the camera, query for latest results.
    if (frAprilCamera.isConnected()){
      unRead = frAprilCamera.getAllUnreadResults();
    }
    
    // If no data, exit.
    if (unRead.isEmpty()) {
      System.out.println("FR Yikes");
      return Optional.empty();
    }
    
    // Get only the latest result.
    PhotonPipelineResult result = unRead.get(unRead.size() - 1);
    frTargets = result.getTargets();
    
    // Logging all front targets.
    for(int i = 0; i < frTargets.size(); i++){
      Logger.recordOutput(this.getName() + "/FRTargets/" + frTargets.get(i).getFiducialId(), frTargets.get(i)); 
    }
    
    // If scoring on reef with a target in mind, just filter to that target.
    if ((visionMode == Mode.REEF) && (targetBranch >= 65 && targetBranch <= 76)) {
      int tagId = branchLookup(targetBranch);
      for (PhotonTrackedTarget possibleTarget : frTargets) {
        if(possibleTarget.getFiducialId() == tagId) {          
          frTarget = Optional.of(possibleTarget);
          continue;
        }
      }
    } else {
      // Filter down to aplicable targets (Based on alliance & objective).
      filterListByPossibleList(frTargets, possibleFrontTargets);
      
      if(frTargets.isEmpty()) {
        return Optional.empty();
      }
      
      // Finds biggest target and set it to main target
      int largest = 0; 
      for(int i = 1; i < frTargets.size(); i++){
        if(frTargets.get(i).getArea() > frTargets.get(largest).getArea()) {
          largest = i; 
          continue;
        }
      }
      frTarget = Optional.of(frTargets.get(largest)); 
    }
    
    if (frTarget.isEmpty()) {
      return Optional.empty();
    }
    // Log current target.
    Logger.recordOutput(this.getName() + "/FRTargets/Selected", frTarget.get());
    return frTarget;
  }
  
  private Optional<PhotonTrackedTarget> frontLeftCamera(){
    List<PhotonTrackedTarget> flTargets; 
    Optional<PhotonTrackedTarget> flTarget = Optional.empty();
    List<PhotonPipelineResult> unRead = new LinkedList<PhotonPipelineResult>();
    
    // If we have access to the camera, query for latest results.
    if (flAprilCamera.isConnected()){
      System.out.println("FL Yikes");
      unRead = flAprilCamera.getAllUnreadResults();
    }
    
    // If no data, exit.
    if (unRead.isEmpty()) {
      return Optional.empty();
    }
    
    // Get only the latest result.
    PhotonPipelineResult result = unRead.get(unRead.size() - 1);
    flTargets = result.getTargets();
    
    // Logging all front targets.
    for(int i = 0; i < flTargets.size(); i++){
      Logger.recordOutput(this.getName() + "/FLTargets/" + flTargets.get(i).getFiducialId(), flTargets.get(i)); 
    }
    
    // If scoring on reef with a target in mind, just filter to that target.
    if ((visionMode == Mode.REEF) && (targetBranch >= 65 && targetBranch <= 76)) {
      int tagId = branchLookup(targetBranch);
      for (PhotonTrackedTarget possibleTarget : flTargets) {
        if(possibleTarget.getFiducialId() == tagId) {
          flTarget = Optional.of(possibleTarget);
          continue;
        }
      }
    } else {
      // Filter down to aplicable targets (Based on alliance & objective).
      filterListByPossibleList(flTargets, possibleFrontTargets);
      
      if(flTargets.isEmpty()) {
        return Optional.empty();
      }
      
      // Finds biggest target and set it to main target
      int largest = 0; 
      for(int i = 1; i < flTargets.size(); i++){
        if(flTargets.get(i).getArea() > flTargets.get(largest).getArea()) {
          largest = i; 
          continue;
        }
      }
      flTarget = Optional.of(flTargets.get(largest)); 
    }
    
    if (flTarget.isEmpty()) {
      return Optional.empty();
    }
    // Log current target.
    Logger.recordOutput(this.getName() + "/FLTargets/Selected", flTarget.get());
    
    //gets the x, y, & z components of the target position.
    return flTarget;
  }
  
  @Override
  public void periodic() {
    if (toggle) {
      toggle = false;
      return;
    }
    toggle = true;

    camToTarget = Transform3d.kZero;
    target = Optional.empty(); 
    velocityX = 0;
    velocityY = 0;
    velocityRot = 0;
    
    Optional<PhotonTrackedTarget> flTarget = frontLeftCamera();
    Optional<PhotonTrackedTarget> frTarget = frontRightCamera();
    
    String position = "";
    
    if(flTarget.isPresent() && frTarget.isPresent()){
      // if (flTarget.get().getBestCameraToTarget().getY() <= frTarget.get().getBestCameraToTarget().getY()) {
      Logger.recordOutput(this.getName() + "/bothTargetsPresent", true);
      if (targetBranch % 2 == 0){
        target = Optional.of(flTarget.get());
        position = "flTarget";
      } else {
        target = Optional.of(frTarget.get());
        position = "frTarget";
      }
    } else if (frTarget.isPresent()) {
      target = Optional.of(frTarget.get());
      position = "frTarget";
    } else if (flTarget.isPresent()) {\
      target = Optional.of(flTarget.get());
      position = "flTarget";
    }
    
    
    if (target.isEmpty()) {
      return;
    }
    
    
    Logger.recordOutput(this.getName() + "/target", target.get());
    Logger.recordOutput(this.getName() + "/position", position);
    if(visionMode == Mode.REEF && seizeBranch(targetBranch)){
      camToTarget = target.get().getBestCameraToTarget();
      Optional<Pose3d> tagPose = fieldLayout.getTagPose(target.get().getFiducialId());
      reefTagAngle = Rotation2d.fromRadians(tagPose.get().getRotation().getAngle()).rotateBy(Rotation2d.k180deg).getRadians();
      if (targetBranch % 2 == 0){
        branchOffset = -1 * VisionConstants.kBranchOffset; 
      } else {
        branchOffset = VisionConstants.kBranchOffset; 
      }
    } else {
      target = Optional.empty();
      branchOffset = 0;
    }
    
    double xOffset = 0;
    double yOffset = 0;
    
    switch(position){
      case "flTarget":
      xOffset = VisionConstants.kFLCameraPosition.getX();
      yOffset = VisionConstants.kFLCameraPosition.getY();
      break;
      
      case "frTarget":
      xOffset = VisionConstants.kFRCameraPosition.getX();
      yOffset = VisionConstants.kFRCameraPosition.getY();
      break;
    }
    
    velocityX = -1 * xController.calculate(camToTarget.getX(), xOffset); 
    velocityY = -1 * yController.calculate(camToTarget.getY(), yOffset+branchOffset);  
    velocityRot = -1 * rotController.calculate(camToTarget.getRotation().getZ(), Units.degreesToRadians(180));
    
    if(Math.abs(xController.getError()) > SwerveConstants.kXTolerance) {
      velocityX = MathUtil.clamp(velocityX, -SwerveConstants.kMaxSpeed/4, SwerveConstants.kMaxSpeed/4); 
    } else{
      velocityX = 0;
    }
    
    if(Math.abs(yController.getError()) > SwerveConstants.kYTolerance) {
      velocityY = MathUtil.clamp(velocityY, -SwerveConstants.kMaxSpeed, SwerveConstants.kMaxSpeed); 
    } else{
      velocityY = 0;
    }
    
    if(Math.abs(rotController.getError()) > SwerveConstants.kRotTolerance) {
      velocityRot = MathUtil.clamp(velocityRot, -SwerveConstants.kMaxSpeed, SwerveConstants.kMaxSpeed); 
    } else {
      velocityRot = 0;
    }
    
    if (tunableXControllerP.hasChanged(hashCode())
    || tunableXControllerI.hasChanged(hashCode())
    || tunableXControllerD.hasChanged(hashCode())) {
      xController.setPID(tunableXControllerP.get(), tunableXControllerI.get(), tunableXControllerD.get());
    }
    
    if (tunableYControllerP.hasChanged(hashCode())
    || tunableYControllerI.hasChanged(hashCode())
    || tunableYControllerD.hasChanged(hashCode())) {
      yController.setPID(tunableYControllerP.get(), tunableYControllerI.get(), tunableYControllerD.get());
    }
    
    if (tunableRotControllerP.hasChanged(hashCode())
    || tunableRotControllerI.hasChanged(hashCode())
    || tunableRotControllerD.hasChanged(hashCode())) {
      rotController.setPID(tunableRotControllerP.get(), tunableRotControllerI.get(), tunableRotControllerD.get());
    }
    
    Logger.recordOutput(this.getName() + "/AngleTarget", Units.radiansToDegrees(camToTarget.getRotation().getZ()));
    Logger.recordOutput(this.getName() + "/ReefTagAngle", Units.radiansToDegrees(reefTagAngle)); 
    Logger.recordOutput(this.getName() + "/Transform3d", camToTarget);
    Logger.recordOutput(this.getName() + "/velocityX", velocityX);
    Logger.recordOutput(this.getName() + "/velocityY", velocityY);
    Logger.recordOutput(this.getName() + "/velocityRot", velocityRot);
    Logger.recordOutput(this.getName() + "/xError", xController.getError());
    Logger.recordOutput(this.getName() + "/yError", yController.getError());
    Logger.recordOutput(this.getName() + "/rotError", rotController.getError());
    Logger.recordOutput(this.getName() + "/TargetBranch", targetBranch);
    Logger.recordOutput(this.getName() + "/BranchOffset", branchOffset);
    Logger.recordOutput(this.getName() + "/hasTarget", hasTarget().getAsBoolean());
    Logger.recordOutput(this.getName()+ "/atTarget", atTarget());
    Logger.recordOutput(this.getName() + "/xOffset", xOffset);
    Logger.recordOutput(this.getName() + "/yOffset", yOffset);
    
    // Do latency check 
    
    // Disable automation
    // if (result.metadata.getLatencyMillis() > 100){
    //   latency = new Alert("Front Camera: Latency Error", AlertType.kWarning); 
    //   return; 
    // } 
  }
  
  @Override
  public void simulationPeriodic() {
    // This method will be called once per scheduler run during simulation
  }
  
}

