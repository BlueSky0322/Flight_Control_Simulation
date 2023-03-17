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
                //generateLandingReadings();
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
                Logger.getLogger(CabinPressureSensor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void generateLandingReadings() {
        int reading = generateDecreasingPressureChange();
        publishMessage(Integer.toString(reading));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(AltitudeSensor.class.getName()).log(Level.SEVERE, null, ex);
            state = "stopping";
        }

    }

    public double generateRandomPressureChange() {
        // Set the minimum and maximum normal cabin pressure changes
        double minPressureChange = -1.5;
        double maxPressureChange = 1.5;
        double standardPressure = 8.9;
        // Generate a random double value between the minimum and maximum values with one decimal point
        Random r = new Random();
        double randomPressureChange = Math.round((minPressureChange + (maxPressureChange - minPressureChange) * r.nextDouble()) * 10.0) / 10.0;

        // Add the random pressure change to the current pressure
        double newPressure = standardPressure + randomPressureChange;

        // Ensure that the new pressure is within the valid range of 4.3 to 12.3 psi
        newPressure = Math.max(4.3, newPressure);
        newPressure = Math.min(12.3, newPressure);

        return newPressure;
    }

    public int generateDecreasingPressureChange() {
        int altitudeDecrease = (new Random()).nextInt(4001);
        //altitude-=altitudeDecrease;
        return 0;
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
