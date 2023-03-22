package Plane.Actuators;

import Plane.Components.LandingGear;
import Plane.Connections.ActuatorQueues;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LandingGearActuator implements Runnable {

    private Channel actuatorChannel;
    private static String state = "normal";
    private static volatile boolean landingEvent = false;

    public LandingGearActuator() {
        actuatorChannel = ActuatorUtils.createNormalChannel();
        System.out.println("[*] [ACTUATOR-LGALG] LANDING GEAR ACTUATOR: Started successfully.");
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (landingEvent && LandingGear.isDeployed) {
                continue;
            }
            if (state.equals("normal")) {
                LandingGear.setIsDeployed(false);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    state = "landing";
                }

            } else if (state.equals("landing")) {
                receiveLandingReading();
                try {
                    Thread.sleep(1000);
                    continue;
                } catch (InterruptedException ex) {
                    state = "stopping";
                }
            } else {
                Thread.currentThread().interrupt();
            }
        }

    }

    //start landing gear actuator
    public static void deployLandingGear() {
        landingEvent = true;
        System.out.println("[x] [ACTUATOR-LGALG] Initializing LANDING_GEAR Actuator...");
    }

    //stop landing gear actuator (deploy once)
    public static void stopDeployLandingGear() {
        landingEvent = false;
    }
    
    //CONSUMER FOR CORRECTIONS SENT FROM FC  
    //receive landing readings only
    private void receiveLandingReading() {
        try {
            actuatorChannel.basicConsume(ActuatorQueues.LANDING_GEAR.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");

                LandingGear.setIsDeployed(Boolean.parseBoolean(m));
                System.out.println("{LANDING} [ACTUATOR-LGALG] Received landing instructions from [FC]");
                System.out.println("{LANDING} [ACTUATOR-LGALG] Deploying Landing Gear...");
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
