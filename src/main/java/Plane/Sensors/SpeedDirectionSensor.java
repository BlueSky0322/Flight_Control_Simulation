/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Sensors;

import Plane.Connections.ConnectionManager;
import Plane.Connections.Exchanges;
import Plane.Connections.RoutingKeys;
import Plane.FC.FlightController;
import Plane.Main.Plane;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ryann
 */
public class SpeedDirectionSensor implements Runnable {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel sensorsChannel;
    private static volatile boolean pause = false;

    public static void pauseSensor() {
        pause = true;
        System.out.println("[x] [SENSOR-SDS] Pausing SPEED_DIRECTION Sensor...");
    }

    public static void unpauseSensor() {
        pause = false;
        System.out.println("[x] [SENSOR-SDS] Resuming SPEED_DIRECTION Sensor...");
    }

    public SpeedDirectionSensor() {
        try {
            factory = new ConnectionFactory();
            connection = factory.newConnection();
            sensorsChannel = connection.createChannel();
            //sensorsChannel.exchangeDeclare(Exchanges.SENSOR.getName(), "direct", true);
            System.out.println("[*] [SENSOR-SDS] SPEED DIRECTION SENSOR: Started successfully.");
        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(SpeedDirectionSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        generateReadings();
    }

    public void generateReadings() {
        if (!pause) {
            int speedReading = generateRandomSpeedChange();
            int directionReading = generateRandomDeviation();
            String reading = speedReading + ":" + directionReading;
            publishMessage(reading);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(SpeedDirectionSensor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public int generateRandomDeviation() {
        // Set the minimum and maximum degrees of deviation
        int minDeviation = -25;
        int maxDeviation = 25;
        int standardDirection = 90;
        // Generate a random integer value between the minimum and maximum values
        Random r = new Random();
        int deviation = r.nextInt(maxDeviation - minDeviation + 1) + minDeviation;

        // Add the deviation to the current direction
        int newDirection = standardDirection + deviation;

        // Ensure that the new direction is within the valid range of -180 to 180 degrees
        newDirection = Math.max(Math.min(newDirection, 130), 50);

        return newDirection;
    }

    public int generateRandomSpeedChange() {
        // Set the minimum and maximum normal speed changes for a commercial airliner
        int minSpeedChange = -25; // knots
        int maxSpeedChange = 25; // knots
        int standardSpeed = 350;
        // Generate a random double value between the minimum and maximum values with one decimal point
        Random r = new Random();
        int randomSpeedChange = r.nextInt(maxSpeedChange - minSpeedChange + 1) + minSpeedChange;

        // Add the random speed change to the current speed
        int newSpeed = standardSpeed + (int) randomSpeedChange;

        // Ensure that the new speed is within the valid range of 130 to 570 knots
        newSpeed = Math.max(130, newSpeed);
        newSpeed = Math.min(570, newSpeed);

        return newSpeed;
    }

    public void publishMessage(String msg) {
        try {
            sensorsChannel.basicPublish(Exchanges.SENSOR.getName(), RoutingKeys.SPEED_DIRECTION.getKey(), null, msg.getBytes());
            System.out.println("[SENSOR-SDS] Speed and Direction readings sent to [CONTROL-FC] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(SpeedDirectionSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
