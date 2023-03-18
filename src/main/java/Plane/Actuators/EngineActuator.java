/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Actuators;

import Plane.Components.Engine;
import Plane.Connections.ActuatorQueues;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ryann
 */
public class EngineActuator implements Runnable {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel actuatorChannel;
    private Channel emergencyChannel;
    private static volatile boolean cabinPressureLossEvent = false;
    private static String state = "normal";

    public static void pauseActuator() {
        cabinPressureLossEvent = true;
        System.out.println("[x] [ACTUATOR-EAE] Pausing ENGINE Actuator...");
    }

    public static void unpauseActuator() {
        cabinPressureLossEvent = false;
        System.out.println("[x] [ACTUATOR-EAE] Resuming ENGINE Actuator...");
    }

    public EngineActuator() {
        try {
            factory = new ConnectionFactory();
            connection = factory.newConnection();
            actuatorChannel = connection.createChannel();
            emergencyChannel = connection.createChannel();
            System.out.println("[*] [ACTUATOR-EAE] ENGINE ACTUATOR: Started successfully.");
            //ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            //executor.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
        } catch (IOException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TimeoutException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (state.equals("normal")) {
                if (!cabinPressureLossEvent) {
                    receiveReading();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        state = "landing";
                    }
                }
                receiveEmergencyReading();
            } else if (state.equals("landing")) {
                try {
                    receiveLandingReading();
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    state = "stopping";
                }
            } else {
                Thread.currentThread().interrupt();
            }
        }

    }

    private void receiveLandingReading() {
        try {
            actuatorChannel.basicConsume(ActuatorQueues.ENGINES.getName(), true, (consumerTag, msg) -> {
                String m = new String(msg.getBody());

                int throttleCorrection = Integer.parseInt(m);
                handleLandingReading(throttleCorrection);

            }, consumerTag -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleLandingReading(int throttleCorrection) {
        System.out.println("[ACTUATOR-EAE] Received correction from [FC] (" + throttleCorrection + "%)");

        int newThrottle = Engine.getThrottle() + throttleCorrection;
        Engine.setThrottle(Math.max(0,newThrottle));


        System.out.println("[ACTUATOR-EAE] Updating Engine Throttle...");
        System.out.println("[ACTUATOR-EAE] Current Engine Readings: THROTTLE (" + Engine.getThrottle() + "%)");
    }


    public void handleReading(int throttleCorrection) {
        System.out.println("[ACTUATOR-EAE] Received correction from [FC] (" + throttleCorrection + "%)");

        int newThrottle = Engine.getThrottle() + throttleCorrection;
        int correctedThrottle = Math.min(100, Math.max(50, newThrottle));
        if (correctedThrottle == 100 || correctedThrottle == 50) {
            //reset the engine throttle
            Engine.setThrottle(89);
        } else {
            Engine.setThrottle(correctedThrottle);
        }

        System.out.println("[ACTUATOR-EAE] Updating Engine Throttle...");
        System.out.println("[ACTUATOR-EAE] Current Engine Readings: THROTTLE (" + Engine.getThrottle() + "%)");
    }

    public void receiveReading() {
        try {
            actuatorChannel.basicConsume(ActuatorQueues.ENGINES.getName(), true, (consumerTag, msg) -> {
                String m = new String(msg.getBody());

                int throttleCorrection = Integer.parseInt(m);
                handleReading(throttleCorrection);

            }, consumerTag -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveEmergencyReading() {
        try {
            emergencyChannel.basicConsume(ActuatorQueues.ENGINES_TEMP.getName(), true, (consumerTag, msg) -> {
                String m = new String(msg.getBody());

                int throttleCorrection = Integer.parseInt(m);
                handleReading(throttleCorrection);

            }, consumerTag -> {
                System.out.println(consumerTag);
            });
            //emergencyChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(EngineActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
