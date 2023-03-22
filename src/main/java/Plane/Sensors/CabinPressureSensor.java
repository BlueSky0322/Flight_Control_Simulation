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
public class CabinPressureSensor implements Runnable {

    private Channel sensorsChannel;
    private static volatile boolean pause = false;
    private String state = "normal";
    private static final double MIN_PRESSURE = 4.3;
    private static final double MAX_PRESSURE = 12.3;
    private static double pressure = 8.9;

    public CabinPressureSensor() {
        //initialise connection to sensorsChannel
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
    
    
    //pausing the sensor
    public static void pauseSensor() {
        pause = true;
        System.out.println("[x] [SENSOR-CPS] Pausing CABIN_PRESSURE Sensor...");
    }

    //unpausing the sensor
    public static void unpauseSensor() {
        pause = false;
        System.out.println("[x] [SENSOR-CPS] Resuming CABIN_PRESSURE Sensor...");
    }

    //use to handle cabin pressure loss event using "pause" boolean
    public void generateReadings() {
        if (!pause) {
            //not paused, sensors communicate normally
            double reading = generateRandomPressureChange();
            publishMessage(Double.toString(reading));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                state = "landing";
            }
        }//else, pause sensor readings, only maintain communication between FC and actuators
    }

    //generate random cabin pressure change readings
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

    //handles the landing of the plane, generates decreasing sensor values until 4.3 (standard sea level cabin pressure)
    public void generateLandingReadings() {
        double reading = generateDecreasingPressureChange();
        publishMessage(Double.toString(reading));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            state = "stopping";
        }

    }

    // Generates a decreasing cabin pressure based on the current cabin pressure of the plane
    public double generateDecreasingPressureChange() {
        double pressureDecrease = (new Random()).nextDouble(MAX_PRESSURE - MIN_PRESSURE) / (Plane.LANDING_DURATION - 3);
        pressure -= pressureDecrease;
        return Double.max(MIN_PRESSURE, pressure);
    }

    
    
    
    // Producer to send messages to FC
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
