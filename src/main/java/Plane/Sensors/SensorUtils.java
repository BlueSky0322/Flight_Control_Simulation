package Plane.Sensors;

import Plane.Connections.ConnectionManager;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sensor util functions
 */
public class SensorUtils {

    /**
     * Creates a channel for each sensor to use
     */
    public static Channel createChannel(){

        Channel sensorsChannel = null;
        try {
            ConnectionFactory factory = new ConnectionFactory();
            Connection connection = factory.newConnection();
            sensorsChannel = connection.createChannel();
            ConnectionManager.purgeQueues(sensorsChannel, sensorsChannel);
        } catch (IOException | TimeoutException e) {
            Logger.getLogger(CabinPressureSensor.class.getName()).log(Level.SEVERE, null, e);
        }
        return sensorsChannel;
    }
}
