/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Sensors;

import Plane.Connections.Exchanges;
import Plane.Connections.RoutingKeys;
import Plane.Main.Plane;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ryann
 */
public class SpeedDirectionSensor implements Runnable {

    private Channel sensorsChannel;
    private String state = "normal";
    private static volatile boolean pause = false;
    private static int speed;
    private static int direction;
    private static final int MAX_SPEED = 570;
    private static final int MIN_SPEED = 130;

    public SpeedDirectionSensor() {
        //initialise connection to sensorsChannel
        sensorsChannel = SensorUtils.createChannel();
        System.out.println("[*] [SENSOR-SDS] SPEED DIRECTION SENSOR: Started successfully.");

    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (state.equals("normal")) {
                generateReadings();
            } else if (state.equals("landing")) {
                generateLandingReadings();
            } else {
                Thread.currentThread().interrupt();
            }
        }
    }

    //pausing the sensor
    public static void pauseSensor() {
        pause = true;
        System.out.println("[x] [SENSOR-SDS] Pausing SPEED_DIRECTION Sensor...");
    }

    //unpausing the sensor
    public static void unpauseSensor() {
        pause = false;
        System.out.println("[x] [SENSOR-SDS] Resuming SPEED_DIRECTION Sensor...");
    }

    //use to handle cabin pressure loss event using "pause" boolean
    public void generateReadings() {
        if (!pause) {
            int speedReading = generateRandomSpeedChange();
            int directionReading = generateRandomDeviation();
            String reading = speedReading + ":" + directionReading;
            publishMessage(reading);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                state = "landing";
            }
        }//else, pause sensor readings, only maintain communication between FC and actuators
    }

    //generate random direction deviations
    public int generateRandomDeviation() {
        // Set the minimum and maximum degrees of deviation
        int minDeviation = -25;
        int maxDeviation = 25;
        int standardDirection = 90;
        // Generate a random integer value between the minimum and maximum values
        Random r = new Random();
        int deviation = r.nextInt(maxDeviation - minDeviation + 1) + minDeviation;

        // Add the deviation to the current direction
        direction = standardDirection + deviation;

        // Ensure that the new direction is within the valid range of -180 to 180 degrees
        direction = Math.max(Math.min(direction, 130), 50);

        return direction;
    }

    //generate speed changes
    public int generateRandomSpeedChange() {
        // Set the minimum and maximum normal speed changes for a commercial airliner
        int minSpeedChange = -25; // knots
        int maxSpeedChange = 25; // knots
        // Generate a random double value between the minimum and maximum values with one decimal point
        Random r = new Random();
        int randomSpeedChange = r.nextInt(maxSpeedChange - minSpeedChange + 1) + minSpeedChange;

        // Add the random speed change to the current speed
        speed += randomSpeedChange;

        // Ensure that the new speed is within the valid range of 130 to 570 knots
        speed = Math.max(MIN_SPEED, speed);
        speed = Math.min(MAX_SPEED, speed);

        return speed;
    }

    
    //handles the landing of the plane, generates decreasing sensor values until 0
    private void generateLandingReadings() {
        int speedReading = generateDecreasingSpeed();
        String reading = speedReading + ":" + direction;
        publishMessage(reading);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            state = "stopping";
        }
    }
    
    // Generates a decreasing speed of the plane
    private int generateDecreasingSpeed() {
        speed -= (new Random()).nextInt(MAX_SPEED) / (Plane.LANDING_DURATION - 3);
        speed = Math.max(0, speed);
        return speed;
    }

    
    // Producer to send messages to FC
    public void publishMessage(String msg) {
        try {
            sensorsChannel.basicPublish(Exchanges.SENSOR.getName(), RoutingKeys.SPEED_DIRECTION.getKey(), null, msg.getBytes());
            System.out.println("[SENSOR-SDS] Speed and Direction readings sent to [CONTROL-FC] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(SpeedDirectionSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
