/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Actuators;

import Plane.Components.WingFlap;
import Plane.Connections.ActuatorQueues;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ryann
 */
public class WingActuator implements Runnable {

    private Channel actuatorChannel;
    private Channel emergencyChannel;
    private static volatile boolean cabinPressureLossEvent = false;
    private static String state = "normal";

    public WingActuator() {
        actuatorChannel = ActuatorUtils.createNormalChannel();
        emergencyChannel = ActuatorUtils.createEmergencyChannel();
        System.out.println("[*] [ACTUATOR-WAWF] WING ACTUATOR: Started successfully.");
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

    //CONSUMER FOR CORRECTIONS SENT FROM FC    
    //receive landing readings only
    private void receiveLandingReading() {
        try {
            actuatorChannel.basicConsume(ActuatorQueues.WING_FLAPS.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");
                String[] readings = m.split(":");

                WingFlap.setAngle(Integer.parseInt(readings[0]));
                WingFlap.setDirection(readings[1]);

                System.out.println("{LANDING} [ACTUATOR-WAWF] Received corrections from [FC]");
                System.out.println("{LANDING} [ACTUATOR-WAWF] Updating Wing Flap...");
                System.out.println("{LANDING} [ACTUATOR-WAWF] Current Wing Flap Readings: ANGLE (" + WingFlap.getAngle() + ") ; DIRECTION (" + WingFlap.getDirection() + ")");
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(WingActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //receive normal readings
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

    //receive emergency readings for cabin pressure loss event
    public void receiveEmergencyReading() {
        try {
            emergencyChannel.basicConsume(
                    ActuatorQueues.WING_FLAPS_TEMP.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");
                String[] readings = m.split(":");

                WingFlap.setAngle(WingFlap.getAngle() + Integer.parseInt(readings[0]));
                WingFlap.setDirection(readings[1]);

                System.out.println("[ACTUATOR-WAWF] Received emergency corrections from [FC]");
                System.out.println("[ACTUATOR-WAWF] Updating Wing Flap...");
                System.out.println("[ACTUATOR-WAWF] Current Wing Flap Readings: ANGLE ("
                        + WingFlap.getAngle() + ") ; DIRECTION (" + WingFlap.getDirection() + ")");
            }, x -> {
            });
            //emergencyChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(WingActuator.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }

    //pausing the actuators
    public static void pauseActuator() {
        cabinPressureLossEvent = true;
        System.out.println("[x] [ACTUATOR-WAWF] Pausing WING Actuator...");
    }

    //resuming the actuators
    public static void unpauseActuator() {
        cabinPressureLossEvent = false;
        System.out.println("[x] [ACTUATOR-WAWF] Resuming WING Actuator...");
    }
}
