package ca.mcgill.ecse211.localization;

import ca.mcgill.ecse211.navigation.Navigation;
import ca.mcgill.ecse211.odometer.Odometer;
import ca.mcgill.ecse211.resources.Resources;
import ca.mcgill.ecse211.sensor.ColorController;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.robotics.SampleProvider;

/**
 * Class that enables us to light localize to x = 0, y = 0 using color sensor
 * 
 * @author Adam Gobran, Ali Shobeiri, Abe Yesgat, Reda El Khili
 *
 */
public class LightLocalizer {

	private static Odometer odometer = Resources.getOdometer();
	private static double SENSOR_DISTANCE = 15.5;
	private static double[] lightData = new double[5];

	/**
	 * Localizes about a point (x, y) by calling subsequent helper functions
	 * 
	 * @param x
	 *            x coordinate relative to x = 0 to localize about
	 * @param y
	 *            y coordinate relative to y = 0 to localize about
	 */
	public static void doLocalization(double x, double y) {
		double currentHeading = Navigation.getHeading();
		if (currentHeading == 0) {
			Navigation.pointTo(45);
			Navigation.driveDistance(7, false);
		} else if (currentHeading == 90) {
			Navigation.pointTo(135);
			Navigation.driveDistance(9, false);
		} else if (currentHeading == 180) {
			Navigation.pointTo(225);
			Navigation.driveDistance(7, false);
		} else if (currentHeading == 270) {
			Navigation.pointTo(315);
			Navigation.driveDistance(7, false);
		}
		rotateLightSensor();
		correctPosition(x, y, currentHeading);
		Navigation.travelTo(x, y);
	}

	/** This method overrides the previously defined doLocalization and is used specifically
	 * for localizing in a corner
	 * @param corner Corner parameter passed via wifi
	 */
	public static void doLocalization(int corner) {

		if (corner == 0 || corner == 2) {
			Navigation.pointTo(odometer.getThetaDegrees() + 45);
		} else {
			Navigation.pointTo(odometer.getThetaDegrees() - 45);
		}

		// Will rotate the robot and collect lines
		rotateLightSensor();
		Sound.beep();
		// correct position of our robot using light sensor data
		correctPosition(0, 0, corner);

		// travel to 0,0 then turn to the 0 angle
		Navigation.travelTo(0, 0);

		// Navigation.setSpeed(0,0);

	}

	/*
	 * Rotates sensor around the origin and saves the theta which the point was
	 * encoutered at
	 */
	public static void rotateLightSensor() {
		Navigation.turnTo(360, true);
		int lineIndex = 1;
		while (Navigation.isNavigating()) {
			if (ColorController.middleLineDetected() && lineIndex < 5) {
				lightData[lineIndex] = odometer.getThetaDegrees();
				lineIndex++;
				Sound.beep();
			}
		}
	}

	/**
	 * Uses mathematical calculations to compute the correct robot position
	 * 
	 * @param x
	 *            x value relative to x = 0 for which robot should correct its
	 *            position towards
	 * @param y
	 *            y value relative to y = 0 for which robot should correct its
	 *            position towards
	 */
	private static void correctPosition(double x, double y, int corner) {
		// compute difference in angles
		double deltaThetaY = 0, deltaThetaX = 0, Xnew = 0, Ynew = 0;
		if (corner == 0 || corner == 2) {
			deltaThetaY = Math.abs(lightData[4] - lightData[2]);
			deltaThetaX = Math.abs(lightData[3] - lightData[1]);
		} else if (corner == 1 || corner == 3) {
			deltaThetaY = Math.abs(lightData[3] - lightData[1]);
			deltaThetaX = Math.abs(lightData[4] - lightData[2]);
		}

		if (corner == 0 || corner == 2) {
			Xnew = (x * 30.48) - SENSOR_DISTANCE * Math.cos(Math.toRadians(deltaThetaY) / 2);
		} else if (corner == 1 || corner == 3) {
			Xnew = (x * 30.48) + SENSOR_DISTANCE * Math.cos(Math.toRadians(deltaThetaY) / 2);
		} else if (corner == 4) {
			double currentHeading = Navigation.getHeading();
			// case that corresponds to initial heading = 0
			if (currentHeading == 45) {
				Xnew = (x * 30.48) - SENSOR_DISTANCE * Math.cos(Math.toRadians(deltaThetaY) / 2);
			} else if (currentHeading == 225) { // case that corresponds to initial heading = 180
				Xnew = (x * 30.48) - SENSOR_DISTANCE * Math.cos(Math.toRadians(deltaThetaY) / 2);
			}
		}
		Ynew = (y * 30.48) - SENSOR_DISTANCE * Math.cos(Math.toRadians(deltaThetaX) / 2);

		odometer.setPosition(new double[] { Xnew, Ynew, 0 }, new boolean[] { true, true, false });

	}

	/** This method corrects our odometer theta based on the previously obtained light sensor data
	 * @param x desired x position
	 * @param y desired y position
	 * @param heading current heading
	 */
	private static void correctPosition(double x, double y, double heading) {
		double deltaThetaY = 0, deltaThetaX = 0, Xnew = 0, Ynew = 0;
		if (heading == 90 || heading == 270) {
			deltaThetaY = Math.abs(lightData[3] - lightData[1]);
			deltaThetaX = Math.abs(lightData[4] - lightData[2]);
		} else {
			deltaThetaY = Math.abs(lightData[4] - lightData[2]);
			deltaThetaX = Math.abs(lightData[3] - lightData[1]);
		}
		
		if (heading == 0 || heading == 90) {
			Xnew = (x * 30.48)-SENSOR_DISTANCE * Math.cos(Math.toRadians(deltaThetaY) / 2);
		} else if (heading == 180 || heading == 270) {
			Xnew = (x * 30.48)+SENSOR_DISTANCE * Math.cos(Math.toRadians(deltaThetaY) / 2);
		}
		
		Ynew = (y * 30.48) - SENSOR_DISTANCE * Math.cos(Math.toRadians(deltaThetaX) / 2);

		odometer.setPosition(new double[] { Xnew, Ynew, 0 }, new boolean[] { true, true, false });
	}
}
