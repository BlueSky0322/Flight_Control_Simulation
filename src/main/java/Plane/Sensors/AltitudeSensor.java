/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Sensors;

import Plane.Connections.ConnectionManager;
import Plane.Connections.Exchanges;
import Plane.Connections.RoutingKeys;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ryann
 */
public class AltitudeSensor implements Runnable {

    private Channel sensorsChannel;
    private String state = "normal";



    private static volatile boolean pause = false;

    public static void pauseSensor() {
        pause = true;
        System.out.println("[x] [SENSOR-AS] Pausing ALTITUDE Sensor...");
    }

    public static void unpauseSensor() {
        pause = false;
        System.out.println("[x] [SENSOR-AS] Resuming ALTITUDE Sensor...");
    }

    public AltitudeSensor() {
        sensorsChannel = SensorUtils.createChannel();
        System.out.println("[*] [SENSOR-AS] ALTITUDE SENSOR: Started successfully.");

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

    public void generateLandingReadings() {
        System.out.println("landing readings");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Logger.getLogger(AltitudeSensor.class.getName()).log(Level.SEVERE, null, ex);
            state = "stopping";
        }

    }

    public void generateReadings() {
        if (!pause) {
            int reading = generateRandomAltitude();
            publishMessage(Integer.toString(reading));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(AltitudeSensor.class.getName()).log(Level.SEVERE, null, ex);
                state = "landing";
            }
        }
    }



    public int generateRandomAltitude() {
        // Generate a random altitude difference between -2000 and 2000 feet
        int standardCruisingAltitude = 33000;
        int altitudeDifference = (new Random()).nextInt(4001) - 2000;

        // Add the altitude difference to the current altitude
        int altitude = standardCruisingAltitude + altitudeDifference;

        // Ensure that altitude is within the valid range of 28000 to 45000 feet
        if (altitude < 28000) {
            altitude = (new Random()).nextInt(4001) + 24000;
        } else if (altitude > 45000) {
            altitude = (new Random()).nextInt(4001) + 41000;
        }

        return altitude;
    }

    public void publishMessage(String msg) {
        try {
            sensorsChannel.basicPublish(Exchanges.SENSOR.getName(), RoutingKeys.ALTITUDE.getKey(), null, msg.getBytes());
            System.out.println("[SENSOR-AS] Altitude reading sent to [CONTROL-FC] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(AltitudeSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
