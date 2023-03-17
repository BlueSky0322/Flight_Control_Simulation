/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Sensors;

import Plane.Connections.ConnectionManager;
import Plane.Connections.Exchanges;
import Plane.Connections.RoutingKeys;
import Plane.Connections.SensorQueues;
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
 * @author ryann
 */
public class CabinPressureSensor implements Runnable {

    private Channel sensorsChannel;
    private static volatile boolean pause = false;
    private String state = "normal";
    private static final double OPTIMAL_PRESSURE = 8.9;

    private static final double MIN_PRESSURE = 4.3;

    private static final double MAX_PRESSURE = 12.3;
    private static double pressure = 8.9;


    public static void pauseSensor() {
        pause = true;
        System.out.println("[x] [SENSOR-CPS] Pausing CABIN_PRESSURE Sensor...");
    }

    public static void unpauseSensor() {
        pause = false;
        System.out.println("[x] [SENSOR-CPS] Resuming CABIN_PRESSURE Sensor...");
    }

    public CabinPressureSensor() {

        sensorsChannel = SensorUtils.createChannel();
        System.out.println("[*] [SENSOR-CPS] CABIN PRESSURE SENSOR: Started successfully.");
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

    public void generateReadings() {
        if (!pause) {
            double reading = generateRandomPressureChange();
            publishMessage(Double.toString(reading));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                state = "landing";
            }
        }
    }

    public void generateLandingReadings() {
        double reading = generateDecreasingPressureChange();
        publishMessage(Double.toString(reading));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            state = "stopping";
        }

    }

    public double generateRandomPressureChange() {
        // Set the minimum and maximum normal cabin pressure changes
        double minPressureChange = -1.5;
        double maxPressureChange = 1.5;
        // Generate a random double value between the minimum and maximum values with one decimal point
        Random r = new Random();
        double randomPressureChange = Math.round((minPressureChange + (maxPressureChange - minPressureChange) * r.nextDouble()) * 10.0) / 10.0;

        // Add the random pressure change to the current pressure
        pressure += randomPressureChange;

        // Ensure that the new pressure is within the valid range of 4.3 to 12.3 psi
        pressure = Math.max(4.3, pressure);
        pressure = Math.min(12.3, pressure);

        return pressure;
    }

    public double generateDecreasingPressureChange() {
        double pressureDecrease = (new Random()).nextDouble(MAX_PRESSURE - MIN_PRESSURE) / (Plane.LANDING_DURATION - 3);
        pressure -= pressureDecrease;
        return Double.max(MIN_PRESSURE, pressure);
    }

    public void publishMessage(String msg) {
        try {
            sensorsChannel.basicPublish(Exchanges.SENSOR.getName(), RoutingKeys.CABIN.getKey(), null, msg.getBytes());
            String formattedPressure = String.format("%.1f", Double.parseDouble(msg));
            System.out.println("[SENSOR-CPS] Cabin pressure reading sent to [CONTROL-FC] (" + formattedPressure + ")");

        } catch (IOException ex) {
            Logger.getLogger(CabinPressureSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
