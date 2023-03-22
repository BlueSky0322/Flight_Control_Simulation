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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ryann
 */
public class WeatherSensor implements Runnable {

    private Channel sensorsChannel;
    private static volatile boolean pause = false;
    private static String state = "normal";

    public WeatherSensor() {
        //initialise connection to sensorsChannel
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

    //pausing sensor
    public static void pauseSensor() {
        pause = true;
        System.out.println("[x] [SENSOR-WS] Pausing WEATHER Sensor...");
    }

    //unpausing sensor
    public static void unpauseSensor() {
        pause = false;
        System.out.println("[x] [SENSOR-WS] Resuming WEATHER Sensor...");
    }

    //use to handle cabin pressure loss event using "pause" boolean
    public void generateReadings() {
        if (!pause) {
            //not paused, sensors communicate normally
            int reading = generateEnvironmentReading();
            WeatherCondition newCondition = determineWeatherCondition(reading);
            WeatherCondition currentWeatherCondition = Plane.getCurrentWeather();
            if (newCondition != currentWeatherCondition) {
                currentWeatherCondition = newCondition;
                publishMessage(Integer.toString(currentWeatherCondition.getCode()));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                state = "landing";
            }
        }//else, pause sensor readings, only maintain communication between FC and actuators
    }

    //determine the weather condition based on the environment readings
    public WeatherCondition determineWeatherCondition(int reading) {
        // Set the thresholds for clear skies, turbulence, and icing
        int icingThreshold = 20;
        int turbulenceThreshold = 50;
        int clearSkiesThreshold = 80;

        if (reading < icingThreshold) {
            System.out.println("[SENSOR-WS] Icing is happening!");
            return WeatherCondition.ICING;
        } else if (reading < turbulenceThreshold) {
            // Add a random delay between 10 and 20 seconds before returning turbulence
            System.out.println("[SENSOR-WS] Turbulence incoming!");
            return WeatherCondition.TURBULENCE;
        } else if (reading < clearSkiesThreshold) {
            // Add a random delay between 30 and 60 seconds before returning icing
            System.out.println("[SENSOR-WS] Clear skies right now!");
            return WeatherCondition.CLEAR_SKY;
        } else {
            // If the reading is above all thresholds, default to icing
            System.out.println("[SENSOR-WS] Clear skies right now!");
            return WeatherCondition.CLEAR_SKY;
        }
    }

    //generate environment readings
    private int generateEnvironmentReading() {
        // Define the probabilities for each range of values
        double probLow = 0.1;
        double probMid = 0.1;

        // Generate a random value between 0 and 1
        double rand = Math.random();

        // Choose the range of values based on the random value and the probabilities
        int reading;
        if (rand < probLow) {
            // Values between 0 and 20
            reading = (int) (Math.random() * 20);
        } else if (rand < probLow + probMid) {
            // Values between 21 and 50
            reading = 20 + (int) (Math.random() * 30);
        } else {
            // Values between 51 and 100
            reading = 50 + (int) (Math.random() * 51);
        }
        return reading;
    }

    //Generates a clear-sky reading to FC during landing
    private void generateLandingReadings() {
        publishMessage(Integer.toString(WeatherCondition.CLEAR_SKY.getCode()));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            state = "stopping";
        }
    }

    // Producer to send messages to FC
    public void publishMessage(String msg) {
        try {
            sensorsChannel.basicPublish(Exchanges.SENSOR.getName(), RoutingKeys.WEATHER.getKey(), null, msg.getBytes("UTF-8"));
            System.out.println("[SENSOR-WS] Weather condition reading sent to [CONTROL-FC] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(WeatherSensor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
