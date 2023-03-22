/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Actuators;

import Plane.Components.OxygenMask;
import Plane.Connections.ActuatorQueues;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ryann
 */
public class OxygenMaskActuator implements Runnable {

    private Channel emergencyChannel;
    private static volatile boolean cabinPressureLossEvent = false;
    private static String state = "normal";

    public OxygenMaskActuator() {
        emergencyChannel = ActuatorUtils.createEmergencyChannel();
        System.out.println("[*] [ACTUATOR-OMA] OXYGEN MASK ACTUATOR: Started successfully.");
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (cabinPressureLossEvent && OxygenMask.isDeployed) {
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

    //CONSUMER FOR CORRECTIONS SENT FROM FC    
    //receive emergency readings only
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

    //start the oxygen mask actuator
    public static void deployOxygenMasks() {
        cabinPressureLossEvent = true;
        System.out.println("[x] [ACTUATOR-OMA] Initializing OXYGEN_MASK Actuator...");
    }

    //stop the oxygen mask actuator (deploy once)
    public static void stopDeployOxygenMasks() {
        cabinPressureLossEvent = false;
        System.out.println("[x] [ACTUATOR-OMA] Reverting to normal...");
    }

}
