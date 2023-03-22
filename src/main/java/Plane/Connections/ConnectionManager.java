/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Connections;

import Plane.Main.Plane;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ryann
 */
public class ConnectionManager {

    private static Connection connection = null;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                ConnectionFactory connectionFactory = new ConnectionFactory();
                connection = connectionFactory.newConnection();
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void declareExchange(String exc, Channel channel) {
        try {
            channel.exchangeDeclare(exc, BuiltinExchangeType.DIRECT, true);
        } catch (IOException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void declareSensorQueues(Channel channel) {
        try {
            channel.queueDeclare(SensorQueues.ALTITUDE.getName(), true, false, false, null);
            channel.queueDeclare(SensorQueues.CABIN.getName(), true, false, false, null);
            channel.queueDeclare(SensorQueues.SPEED_DIRECTION.getName(), true, false, false, null);
            channel.queueDeclare(SensorQueues.WEATHER.getName(), true, false, false, null);
        } catch (IOException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void declareActuatorQueues(Channel channel) {
        try {
            channel.queueDeclare(ActuatorQueues.WING_FLAPS.getName(), true, false, false, null);
            channel.queueDeclare(ActuatorQueues.TAIL_FLAPS.getName(), true, false, false, null);
            channel.queueDeclare(ActuatorQueues.ENGINES.getName(), true, false, false, null);
            channel.queueDeclare(ActuatorQueues.LANDING_GEAR.getName(), true, false, false, null);
        } catch (IOException ex) {
            Logger.getLogger(Plane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void declareEmergencyQueues(Channel channel) {
        try {
            channel.queueDeclare(ActuatorQueues.ENGINES_TEMP.getName(), true, false, false, null);
            channel.queueDeclare(ActuatorQueues.WING_FLAPS_TEMP.getName(), true, false, false, null);
            channel.queueDeclare(ActuatorQueues.TAIL_FLAPS_TEMP.getName(), true, false, false, null);
            channel.queueDeclare(ActuatorQueues.OXYGEN_MASKS.getName(), true, false, false, null);
        } catch (IOException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void declareSensorBindings(Channel channel) {
        try {
            channel.queueBind(SensorQueues.ALTITUDE.getName(), Exchanges.SENSOR.getName(), RoutingKeys.ALTITUDE.getKey());
            channel.queueBind(SensorQueues.CABIN.getName(), Exchanges.SENSOR.getName(), RoutingKeys.CABIN.getKey());
            channel.queueBind(SensorQueues.SPEED_DIRECTION.getName(), Exchanges.SENSOR.getName(), RoutingKeys.SPEED_DIRECTION.getKey());
            channel.queueBind(SensorQueues.WEATHER.getName(), Exchanges.SENSOR.getName(), RoutingKeys.WEATHER.getKey());
        } catch (IOException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void declareActuatorBindings(Channel channel) {
        try {
            channel.queueBind(ActuatorQueues.WING_FLAPS.getName(), Exchanges.ACTUATOR.getName(), RoutingKeys.WING_FLAPS.getKey());
            channel.queueBind(ActuatorQueues.TAIL_FLAPS.getName(), Exchanges.ACTUATOR.getName(), RoutingKeys.TAIL_FLAPS.getKey());
            channel.queueBind(ActuatorQueues.ENGINES.getName(), Exchanges.ACTUATOR.getName(), RoutingKeys.ENGINES.getKey());
            channel.queueBind(ActuatorQueues.LANDING_GEAR.getName(), Exchanges.ACTUATOR.getName(), RoutingKeys.LANDING_GEAR.getKey());

        } catch (IOException ex) {
            Logger.getLogger(ConnectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void declareEmergencyBindings(Channel channel) {
        try {
            channel.queueBind(ActuatorQueues.ENGINES_TEMP.getName(), Exchanges.EMERGENCY.getName(), RoutingKeys.ENGINES_TEMP.getKey());
            channel.queueBind(ActuatorQueues.WING_FLAPS_TEMP.getName(), Exchanges.EMERGENCY.getName(), RoutingKeys.WING_FLAPS_TEMP.getKey());
            channel.queueBind(ActuatorQueues.TAIL_FLAPS_TEMP.getName(), Exchanges.EMERGENCY.getName(), RoutingKeys.TAIL_FLAPS_TEMP.getKey());
            channel.queueBind(ActuatorQueues.OXYGEN_MASKS.getName(), Exchanges.EMERGENCY.getName(), RoutingKeys.OXYGEN_MASKS.getKey());
        } catch (IOException ex) {
            Logger.getLogger(Plane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void purgeSensorQueues(Channel sensorsChannel) {
        try {
            //purge sensor queues
            sensorsChannel.queuePurge(SensorQueues.ALTITUDE.getName());
            sensorsChannel.queuePurge(SensorQueues.CABIN.getName());
            sensorsChannel.queuePurge(SensorQueues.SPEED_DIRECTION.getName());
            sensorsChannel.queuePurge(SensorQueues.WEATHER.getName());
        } catch (IOException ex) {
            Logger.getLogger(Plane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void purgeActuatorQueues(Channel actuatorsChannel) {
        try {
            //purge actuator queues
            actuatorsChannel.queuePurge(ActuatorQueues.WING_FLAPS.getName());
            actuatorsChannel.queuePurge(ActuatorQueues.TAIL_FLAPS.getName());
            actuatorsChannel.queuePurge(ActuatorQueues.ENGINES.getName());
            actuatorsChannel.queuePurge(ActuatorQueues.LANDING_GEAR.getName());
        } catch (IOException ex) {
            Logger.getLogger(Plane.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
