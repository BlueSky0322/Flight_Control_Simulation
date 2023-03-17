/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Actuators;

import Plane.Components.WingFlap;
import Plane.Connections.ActuatorQueues;
import Plane.Connections.ConnectionManager;
import Plane.Connections.Exchanges;
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
 *
 * @author ryann
 */
public class WingActuator implements Runnable {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel actuatorChannel;
    private Channel emergencyChannel;
    private static volatile boolean cabinPressureLossEvent = false;

    public static void pauseActuator() {
        cabinPressureLossEvent = true;
        System.out.println("[x] [ACTUATOR-WAWF] Pausing WING Actuator...");
    }

    public static void unpauseActuator() {
        cabinPressureLossEvent = false;
        System.out.println("[x] [ACTUATOR-WAWF] Resuming WING Actuator...");
    }


    public WingActuator() {
        try {
            factory = new ConnectionFactory();
            connection = factory.newConnection();
            actuatorChannel = connection.createChannel();
            emergencyChannel = connection.createChannel();
            //ConnectionManager.declareExchange(Exchanges.ACTUATOR.getName(), actuatorChannel);
            System.out.println("[*] [ACTUATOR-WAWF] WING ACTUATOR: Started successfully.");

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            executor.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(WingActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        if (!cabinPressureLossEvent) {
            receiveReading();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(WingActuator.class.getName()).log(Level.SEVERE, null, ex);
            }
            receiveEmergencyReading();
        }
    }

    public void receiveReading() {
        try {
            actuatorChannel.basicConsume(ActuatorQueues.WING_FLAPS.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");
                String[] readings = m.split(":");

                WingFlap.setAngle(WingFlap.getAngle() + Integer.parseInt(readings[0]));
                WingFlap.setDirection(readings[1]);

                System.out.println("[ACTUATOR-WAWF] Received correction from [FC]");
                System.out.println("[ACTUATOR-WAWF] Updating Wing Flap...");
                System.out.println("[ACTUATOR-WAWF] Current Wing Flap Readings: ANGLE (" + WingFlap.getAngle() + ") ; DIRECTION (" + WingFlap.getDirection() + ")");
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(WingActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveEmergencyReading() {
        try {
            emergencyChannel.basicConsume(ActuatorQueues.WING_FLAPS_TEMP.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");
                String[] readings = m.split(":");

                WingFlap.setAngle(WingFlap.getAngle() + Integer.parseInt(readings[0]));
                WingFlap.setDirection(readings[1]);

                System.out.println("[ACTUATOR-WAWF] Received emergency corrections from [FC]");
                System.out.println("[ACTUATOR-WAWF] Updating Wing Flap...");
                System.out.println("[ACTUATOR-WAWF] Current Wing Flap Readings: ANGLE (" + WingFlap.getAngle() + ") ; DIRECTION (" + WingFlap.getDirection() + ")");
            }, x -> {
            });
            //emergencyChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(WingActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
