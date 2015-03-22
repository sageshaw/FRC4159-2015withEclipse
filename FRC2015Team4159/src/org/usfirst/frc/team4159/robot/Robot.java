package org.usfirst.frc.team4159.robot;

//import edu.wpi.first.wpilibj.DigitalInput;
//import edu.wpi.first.wpilibj.DigitalOutput;
//import edu.wpi.first.wpilibj.Gyro;
//import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.IterativeRobot;
//import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.Timer;
//import edu.wpi.first.wpilibj.Victor;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


public class Robot extends IterativeRobot {


	boolean isToteIn;
	int autoChoice;

//	Timer testTime = new Timer();
    public void robotInit() {
    	AutoChooser.setup();
    }
    
    public void autonomousInit() {
    	autoChoice = AutoChooser.getChoice();
    	IO.mainDrive.octoShift(1);            //Shifts to mecanum
//    	testTime.reset();
//    	testTime.start();
    	
    }
    
    
    
    public void autonomousPeriodic() {
    	AutoMethods.runRoutine(autoChoice);
//   	if(testTime.get() > 2.0) {
//    		OctoDrive.autoDrive.drive(0.0, 0.0);
//    		testTime.stop();
//    	} else {
//    		OctoDrive.autoDrive.drive(0.5, 0.0);
//    	}
//    	SmartDashboard.putNumber("Auto Time", testTime.get());
    }
    public void teleopInit() {
    	
    }
    
    public void teleopPeriodic() {
    	isToteIn = (IO.toteSensor.get()? false : true);
    	SmartDashboard.putBoolean("Tote Sensed?",  isToteIn);
    	
    	if (IO.leftStick.getRawButton(3)) { //Changes to tank
    		IO.mainDrive.octoShift(2);
    		SmartDashboard.putString("Drive State:", "Traction/Tank");
    	}
    	else if (IO.leftStick.getRawButton(2)) { //Changes to mecanum
    		IO.mainDrive.octoShift(1);
    		SmartDashboard.putString("Drive State:", "Mecanum");
    	} else if(IO.leftStick.getRawButton(5)) { //Raises back set piston set and changes control to tank
    		IO.mainDrive.octoShift(3);
    		SmartDashboard.putString("Drive State:", "Back Traction, Front Mecanum/Tank");
    	}
    	
    	IO.mainDrive.manualDrive(-IO.leftStick.getX(), IO.leftStick.getY(), 
    			IO.rightStick.getX(), IO.rightStick.getY()); 				  //Drives according to tank/mecanum boolean in OctoDrive
        
    	if (IO.secondaryStick.getRawButton(3)){								  //Moves elevator up
    		IO.elevator.autoLift(1.0);          
    	} 
    	else if (IO.secondaryStick.getRawButton(2)){						  //Moves elevator down
    		IO.elevator.autoLift(-1.0);
    	} else {
    		IO.elevator.autoLift(0.0);										  //Stops elevator if there is no joystick input
    	}
    	
    }
    
    public void testInit() {

    	
    }
    
    public void testPeriodic() {
//    	if(lowSensor.get()) { //Some limit switch testing code
//		testLED.set(true);
//	}  else {
//		testLED.set(false);
//	}
    }	
    
    public void disabledInit() {
    	
    	
    }
}

//=============================================================================================================================//
//AUTONOMOUS//
class AutoMethods {
	
	private static Timer autoTime;
	
	
	private static double travelTime = 3.0;        //Change based on how the mecanum wheels perform on carpet
	private static double rejoinRouteTime = 1.0; 
	private static double liftTime = 1.0;
	private static double exitTime = 3.0;
	private static double toteDropTime = 1.0;
	
	private static double Kp = 0.0028;				//tune for gyro
	private static double drivetrainOffset = 0.1;
	private static double toteAimTime = 1;
	
	private AutoMethods() {
		
	}
	
 	public static void toteTimedLift(double liftTime) {
 		IO.elevator.moveLow();
 		autoTime.start();
 		while (!autoTime.hasPeriodPassed(liftTime)) {
 			IO.elevator.autoLift(1.0);
 		}
 		autoTime.stop();
 		autoTime.reset();
 		IO.elevator.autoLift(0.0);
 		
 	}
	
	//================================//
	//METHODS THAT DO NOT REQUIRE GYRO//
	//================================//
	public static void straightDrive(double speed, double duration, double offset) {
		autoTime.reset();
		autoTime.start();
		while (autoTime.get() < duration) {
			OctoDrive.autoDrive.drive(speed, offset);
		}
		OctoDrive.autoDrive.drive(0.0, 0.0);
		autoTime.stop();
		autoTime.reset();
	}
	
	public static void toteGet(double speed, double offset) {
		while(IO.toteSensor.get()) {
			OctoDrive.autoDrive.drive(speed, offset);
		}
		OctoDrive.autoDrive.drive(0.0, 0.0);
	}
	
	public static void autoStrafe(double speed, double duration) {
		autoTime.reset();
		autoTime.start();
		while (autoTime.get() < duration) {
			OctoDrive.autoDrive.mecanumDrive_Cartesian(speed, 0.0, 0.0, 0.0);
		}
		autoTime.stop();
		autoTime.reset();
		OctoDrive.autoDrive.mecanumDrive_Cartesian(speed, 0.0, 0.0, 0.0);
	}
	
	public static void continuedRoutine() {
		toteGet(0.5, drivetrainOffset);
		IO.elevator.moveLow();
		toteTimedLift(liftTime);
		autoStrafe(0.5, rejoinRouteTime);
		straightDrive(0.5, travelTime, drivetrainOffset);
		autoStrafe(-0.5, rejoinRouteTime);
	}
	
	public static void endRoutine() {
		toteGet(0.5, drivetrainOffset);
		IO.elevator.moveLow();
		toteTimedLift(liftTime);
		autoStrafe(0.5, exitTime);
		IO.elevator.moveLow();
		straightDrive(0.5, toteDropTime, drivetrainOffset);
	}
	
	public static void runRoutine(int autoChoice) {
		switch (autoChoice) {
			case AutoChooser.MOVE_ONLY:
				AutoMethods.straightDrive(0.5, 2, -0.05);                           //Fix with gyro method
				break;
			case AutoChooser.PICK_THREE_TOTE:
				AutoMethods.continuedRoutine();
				AutoMethods.continuedRoutine();
				AutoMethods.endRoutine();
				break;
			default:
				break;
		}
	}
}
