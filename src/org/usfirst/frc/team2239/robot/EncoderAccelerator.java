package org.usfirst.frc.team2239.robot;

import com.ctre.CANTalon;
import com.ctre.CANTalon.FeedbackDevice; //TODO delete if not needed

//TODO finish documentation and changing this from rotationAccelerator into the EncoderAccelerator
//TODO upgrade (this is just something that doesn't need to be done, but would make the program better.) Make a Accelerator class or framework that both of these are just versions of.

public class EncoderAccelerator implements Action {
	TechnoDrive driveTrain;
	CANTalon[] valueMotors;
	//the biggest values we're driving with i.e. tankDrive(-maxVelocity, maxVelocity).
	//Must be in between -1 and 1. Negative if going counter-clockwise
	double maxVelocity;
	double curVelocity = 0; //init to 0; we shouldn't be moving when we initiate //positive if moving forwards
	double rollPastDecrease = .15; //how much we decrease maxVelocity by if we overshoot.
	double accelerate = .05; //how quickly @param velocity will change
	double offset = .36; //the lowest power the motors should ever be at //always positive
	double tolerance = 20; //How close (in ticks) to the final destination should you get before stopping (should not be 0. Perfection is impossible.)
	double moveTicks; //how much to move, in inches (positive means forwards)
	//with 107 ticksPerInch, this thing tried to go 10 inches and instead went 22
	double ticksPerInch = 48.55; //used to be 53.5 on the practice bot
	double targetDistance; //the encoder value we aspire to be at when done.
	double maxVelocityTicks = 2000; //The ticks traveled at which we start to decrease velocity at //always positive
	boolean forward; //true if we should be moving forwards, false otherwise (still or moving backwards)	
	
	
	public EncoderAccelerator (TechnoDrive driveTrain, CANTalon[] motorsToLookAt, double distance, double maxVelocity) {
		this.forward = (distance>0);
		this.driveTrain = driveTrain;
		this.valueMotors = motorsToLookAt;
		this.moveTicks = distance*ticksPerInch;
		this.targetDistance = getEncoderValue()+this.moveTicks;
		this.maxVelocity = maxVelocity;
	}
	
	
	//returns true if the rotation is complete
	//returns false if the rotation is not complete
	public boolean run()
	{
		System.out.println("Im actually moving straight!");
		double curValue = getEncoderValue();
		double offDistance = (targetDistance-curValue);
		System.out.println("I'm this far off: "+offDistance);
		System.out.println("targetDistance: "+targetDistance);
		if (targetDistance-tolerance < curValue && curValue < targetDistance+tolerance) { //we did it!
			driveTrain.tankDrive(0, 0); //stop driving //TODO this causes a lot of slippage when moving forwards and bakwards
			return true;
		}
		
		boolean shouldBeForwards = offDistance>0;
		if (shouldBeForwards!=forward) {
			maxVelocity = Math.max(maxVelocity - rollPastDecrease, offset);
			curVelocity = 0; //stop it from swinging past
			System.out.println("Swung past the target!");
		}
		forward = shouldBeForwards;
		
		double targetVelocity;
		if (forward) {
			targetVelocity = Math.min(((maxVelocity-offset)/maxVelocityTicks)*offDistance+offset, maxVelocity);
		} else {
			targetVelocity = Math.max(((maxVelocity-offset)/maxVelocityTicks)*offDistance-offset, -maxVelocity);
		}
		
		System.out.println("Target velocity before setting is: "+targetVelocity);
		System.out.println("curVelocity before setting is: "+curVelocity);
		if (forward) {
			System.out.println("We're going forwards");
			if (targetVelocity > curVelocity+accelerate) { //if I'm going slower than I should, ramp up to it
				curVelocity = curVelocity+accelerate;
			} else {
				curVelocity = targetVelocity; //this handles deceleration with the proportionality stuff
			}
			curVelocity = Math.min(curVelocity, maxVelocity);
		} else {
			System.out.println("We're going counterclockwise");
			if (targetVelocity < curVelocity-accelerate) { //if I'm going slower than I should, ramp up to it
				curVelocity = curVelocity-accelerate;
			} else {
				curVelocity = targetVelocity; //this handles deceleration with the proportionality stuff
			}
			curVelocity = Math.max(curVelocity, -maxVelocity);
		}
		System.out.println("Target velocity is: "+targetVelocity);
		System.out.println("Actually driving at: " + curVelocity);
		driveTrain.tankDrive(curVelocity, curVelocity); //actually drive
		return false;
	}
	
	public double getEncoderValue() {
		//TODO don't just average - also check to see if any of the encoders are way off or may be broken and ignore those ones.
		double sum = 0;
		for (CANTalon motor : this.valueMotors) {
			sum+=motor.getEncPosition();
		}
		double avg = sum/this.valueMotors.length; //compute the average encoder value
		System.out.println("the average encoder values from sensors: " + avg);
		return -avg; //Had to put a negative sign because the encoder is giving opposite values 
	}
}
