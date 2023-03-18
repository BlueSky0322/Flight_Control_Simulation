package Plane.Actuators;

import Plane.Components.LandingGear;
import Plane.Components.OxygenMask;
import Plane.Connections.ActuatorQueues;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LandingGearActuator implements Runnable {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel actuatorChannel;
    private static String state = "normal";

    public static void deployLandingGear() {
        System.out.println("[x] [ACTUATOR-LGALG] Initializing LANDING_GEAR Actuator...");
    }

    public LandingGearActuator() {
        try {
            factory = new ConnectionFactory();
            connection = factory.newConnection();
            actuatorChannel = connection.createChannel();
            //ConnectionManager.declareExchange(Exchanges.ACTUATOR.getName(), actuatorChannel);
            System.out.println("[*] [ACTUATOR-LGALG] LANDING GEAR ACTUATOR: Started successfully.");
        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(OxygenMaskActuator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (LandingGear.isDeployed) {
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
                LandingGear.setIsDeployed(false);
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
        LandingGear.setIsDeployed(true);
    }

}
