package Plane.Actuators;

import Plane.Sensors.*;
import Plane.Connections.ConnectionManager;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ActuatorUtils {
    //creates channel for each actuator to use (normal/emergency)
    public static Channel createNormalChannel() {

        Channel actuatorsChannel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            Connection connection = factory.newConnection();
            actuatorsChannel = connection.createChannel();
            ConnectionManager.purgeActuatorQueues(actuatorsChannel);
        } catch (IOException | TimeoutException e) {
            Logger.getLogger(CabinPressureSensor.class.getName()).log(Level.SEVERE, null, e);
        }
        return actuatorsChannel;
    }

    public static Channel createEmergencyChannel() {

        Channel emergencyChannel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            Connection connection = factory.newConnection();
            emergencyChannel = connection.createChannel();
            ConnectionManager.purgeActuatorQueues(emergencyChannel);
        } catch (IOException | TimeoutException e) {
            Logger.getLogger(CabinPressureSensor.class.getName()).log(Level.SEVERE, null, e);
        }
        return emergencyChannel;
    }
}
