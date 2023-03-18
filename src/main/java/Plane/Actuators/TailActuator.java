/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Actuators;

import Plane.Components.TailFlap;
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
public class TailActuator implements Runnable {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel actuatorChannel;
    private Channel emergencyChannel;
    private TailFlap tf;
    private static volatile boolean cabinPressureLossEvent = false;
    private static String state = "normal";

    public static void pauseActuator() {
        cabinPressureLossEvent = true;
        System.out.println("[x] [ACTUATOR-TATF] Pausing TAIL Actuator...");
    }

    public static void unpauseActuator() {
        cabinPressureLossEvent = false;
        System.out.println("[x] [ACTUATOR-TATF] Resuming TAIL Actuator...");
    }

    public TailActuator() {

        try {
            factory = new ConnectionFactory();
            connection = factory.newConnection();
            actuatorChannel = connection.createChannel();

            emergencyChannel = connection.createChannel();
            System.out.println("[*] [ACTUATOR-TATF] TAIL ACTUATOR: Started successfully.");
        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(TailActuator.class.getName()).log(Level.SEVERE, null, ex);
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
            
            actuatorChannel.basicConsume(ActuatorQueues.TAIL_FLAPS.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");
                String[] readings = m.split(":");

                int angleCorrection = Integer.parseInt(readings[0]);
                String directionCorrection = readings[1];

                handleLandingReading(angleCorrection, directionCorrection);
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(TailActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void handleLandingReading(int angle, String direction) {
        System.out.println("{LANDING} [ACTUATOR-TATF] Received absolute value from [FC]: ANGLE (" + angle + ") ; DIRECTION (" + direction + ")");

        TailFlap.setAngle(angle);
        TailFlap.setDirection(direction);
        System.out.println("{LANDING} [ACTUATOR-TATF] Updating Tail Flap...");
        System.out.println("{LANDING} [ACTUATOR-TATF] Current Tail Flap Readings: ANGLE (" + tf.getAngle() + ") ; DIRECTION (" + tf.getDirection() + ")");
    }

    public void handleReading(int angleCorrection, String directionCorrection) {
        System.out.println("[ACTUATOR-TATF] Received correction from [FC]: ANGLE (" + angleCorrection + ") ; DIRECTION (" + directionCorrection + ")");

        int newAngle = TailFlap.getAngle() + angleCorrection;
        int correctedAngle = Math.min(50, Math.max(-50, newAngle));
        TailFlap.setAngle(correctedAngle);
        TailFlap.setDirection(directionCorrection);
        System.out.println("[ACTUATOR-TATF] Updating Tail Flap...");
        System.out.println("[ACTUATOR-TATF] Current Tail Flap Readings: ANGLE (" + tf.getAngle() + ") ; DIRECTION (" + tf.getDirection() + ")");
    }

    public void receiveReading() {
        try {
            actuatorChannel.basicConsume(ActuatorQueues.TAIL_FLAPS.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");
                String[] readings = m.split(":");

                int angleCorrection = Integer.parseInt(readings[0]);
                String directionCorrection = readings[1];

                handleReading(angleCorrection, directionCorrection);
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(TailActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveEmergencyReading() {
        try {
            emergencyChannel.basicConsume(ActuatorQueues.TAIL_FLAPS_TEMP.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");
                String[] readings = m.split(":");

                int angleCorrection = Integer.parseInt(readings[0]);
                String directionCorrection = readings[1];

                handleReading(angleCorrection, directionCorrection);
            }, x -> {
            });
            //emergencyChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(TailActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
