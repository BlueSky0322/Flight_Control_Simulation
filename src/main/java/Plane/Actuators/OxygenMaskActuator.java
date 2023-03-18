/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Actuators;

import Plane.Components.OxygenMask;
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
 * @author ryann
 */
public class OxygenMaskActuator implements Runnable {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel actuatorChannel;
    private Channel emergencyChannel;
    private static volatile boolean cabinPressureLossEvent = false;
    private static String state = "normal";

    public static void deployOxygenMasks() {
        cabinPressureLossEvent = true;
        System.out.println("[x] [ACTUATOR-OMA] Initializing OXYGEN_MASK Actuator...");
    }

    public static void stopDeployOxygenMasks() {
        cabinPressureLossEvent = false;
        System.out.println("[x] [ACTUATOR-OMA] Reverting to normal...");
    }

    public OxygenMaskActuator() {
        try {
            factory = new ConnectionFactory();
            connection = factory.newConnection();
            emergencyChannel = connection.createChannel();
            //ConnectionManager.declareExchange(Exchanges.ACTUATOR.getName(), actuatorChannel);
            System.out.println("[*] [ACTUATOR-OMA] OXYGEN MASK ACTUATOR: Started successfully.");
        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (cabinPressureLossEvent && OxygenMask.isDeployed){
                continue;
            }
            if (state.equals("normal") && cabinPressureLossEvent) {
                receiveReading();

                OxygenMask.setIsDeployed(false);
            }
            try {
                Thread.sleep(1000);
                continue;
            } catch (InterruptedException ex) {
                state = "landing";
            }
            if (state.equals("landing")) {
                OxygenMask.setIsDeployed(false);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    state = "stopping";
                }
            } else {
                Thread.currentThread().interrupt();
            }
        }
    }


    public void receiveReading() {
        try {
            emergencyChannel.basicConsume(ActuatorQueues.OXYGEN_MASKS.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");

                OxygenMask.setIsDeployed(Boolean.parseBoolean(m));

                System.out.println("[ACTUATOR-OMA] Received emergency instructions from [FC]");
                System.out.println("[ACTUATOR-OMA] Deploying OXYGEN MASKS");
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
