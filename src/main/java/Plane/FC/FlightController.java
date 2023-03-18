/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.FC;

import Plane.Actuators.EngineActuator;
import Plane.Actuators.OxygenMaskActuator;
import Plane.Actuators.TailActuator;
import Plane.Actuators.WingActuator;
import Plane.Connections.ConnectionManager;
import Plane.Connections.Exchanges;
import Plane.Connections.RoutingKeys;
import Plane.Connections.SensorQueues;
import Plane.Main.Plane;
import Plane.Sensors.AltitudeSensor;
import Plane.Sensors.CabinPressureSensor;
import Plane.Sensors.SpeedDirectionSensor;
import Plane.Sensors.WeatherSensor;
import Plane.Utils.WeatherCondition;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ryann
 */
public class FlightController implements Runnable {

    private ConnectionFactory factory;
    private Connection connection;
    private Channel sensorsChannel;
    private Channel actuatorsChannel;
    private Channel emergencyChannel;

    private static String state = "normal";
    private volatile boolean emergencyEvent = false;

    public void startCabinPressureLossEvent() {
        emergencyEvent = true;
        AltitudeSensor.pauseSensor();
        CabinPressureSensor.pauseSensor();
        SpeedDirectionSensor.pauseSensor();
        WeatherSensor.pauseSensor();
        EngineActuator.pauseActuator();
        WingActuator.pauseActuator();
        TailActuator.pauseActuator();
        OxygenMaskActuator.deployOxygenMasks();
        System.out.println("[x] [CONTROL-FC] EMERGENCY PROTOCOL ENABLED!");
    }

    public void stopCabinPressureLossEvent() {
        emergencyEvent = false;
        AltitudeSensor.unpauseSensor();
        CabinPressureSensor.unpauseSensor();
        SpeedDirectionSensor.unpauseSensor();
        WeatherSensor.unpauseSensor();
        EngineActuator.unpauseActuator();
        WingActuator.unpauseActuator();
        TailActuator.unpauseActuator();
        OxygenMaskActuator.stopDeployOxygenMasks();
        System.out.println("[x] [CONTROL-FC] EMERGENCY PROTOCOL DISABLED!");
    }


    public FlightController() {
        try {
            factory = new ConnectionFactory();
            connection = factory.newConnection();

            //ConnectionManager.resetSensorQueues(sensorsChannel);
            sensorsChannel = connection.createChannel();
            ConnectionManager.declareExchange(Exchanges.SENSOR.getName(), sensorsChannel);
            ConnectionManager.declareSensorQueues(sensorsChannel);
            ConnectionManager.declareSensorBindings(sensorsChannel);

            //ConnectionManager.resetActuatorQueues(actuatorsChannel);
            actuatorsChannel = connection.createChannel();
            ConnectionManager.declareExchange(Exchanges.ACTUATOR.getName(), actuatorsChannel);
            ConnectionManager.declareActuatorQueues(actuatorsChannel);
            ConnectionManager.declareActuatorBindings(actuatorsChannel);

            emergencyChannel = connection.createChannel();

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            if (state.equals("normal")) {
                if (!emergencyEvent) {
                    receiveAltitudeReading();
                    receiveCabinPressureReading();
                    receiveSpeedDirectionReading();
                    receiveWeatherReading();
                    processAltitudeOutOfRange(Plane.currentAltitude);
                    processDirectionDeviation(Plane.currentDirection);
                    processSpeedDeviation(Plane.currentSpeed, Plane.currentAltitude);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        state = "landing";
                    }
                }
            } else if (state.equals("landing")) {
                engageLandingRoutine();
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

    private void engageLandingRoutine() {
        sendAbsoluteLandingDataToWingActuator();
        sendAbsoluteLandingDataToTailActuator();
        sendAbsoluteLandingDataToEngineActuator();
        sendLandingSignalToLandingGear();
    }

    private void sendLandingSignalToLandingGear() {
        String msg = "true";
        try {
            actuatorsChannel.basicPublish(Exchanges.ACTUATOR.getName(), RoutingKeys.LANDING_GEAR.getKey(), false, null, msg.getBytes());
            System.out.println("{LANDING} [CONTROL-FC] Sending landing gear to [ACTUATOR-LGALG] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendAbsoluteLandingDataToWingActuator() {
        String msg = "-30:down";
        try {
            actuatorsChannel.basicPublish(Exchanges.ACTUATOR.getName(), RoutingKeys.WING_FLAPS.getKey(), false, null, msg.getBytes());
            System.out.println("{LANDING} [CONTROL-FC] Sending wing actuator data readings to [ACTUATOR-WAWF] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void sendAbsoluteLandingDataToTailActuator() {
        String msg = "0:neutral";
        try {
            actuatorsChannel.basicPublish(Exchanges.ACTUATOR.getName(), RoutingKeys.TAIL_FLAPS.getKey(), false, null, msg.getBytes());
            System.out.println("{LANDING} [CONTROL-FC] Sending tail actuator data readings to [ACTUATOR-TATF] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void sendAbsoluteLandingDataToEngineActuator() {
        String msg = "-10";
        try {
            actuatorsChannel.basicPublish(Exchanges.ACTUATOR.getName(), RoutingKeys.ENGINES.getKey(), false, null, msg.getBytes());
            System.out.println("{LANDING} [CONTROL-FC] Sending engine actuator data readings to [ACTUATOR-EAE] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void pauseReadings() {
        Plane.currentPressure = 1.0;
        System.out.println("\n[x] [CONTROL-FC] Sudden cabin pressure loss detected! Current Cabin Pressure (" + Plane.currentPressure + ")");
        System.out.println();
        System.out.println("!!!=========!!!=========!!!=========!!!=========!!!=========!!!=========!!!=========!!!");
        startCabinPressureLossEvent();

        System.out.println("[x] [CONTROL-FC] Stabilizing aircraft parameters...");
        publishEmergencyReadings();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            System.out.println("===-----------------------===-----------------------===");
            System.out.println("\n[!] [CONTROL-FC] SUCCESSFULLY Regained control over aircraft!\n");

            stopCabinPressureLossEvent();

            System.out.println("!!!=========!!!=========!!!=========!!!=========!!!=========!!!=========!!!=========!!!\n");
        }
    }


    public void publishEmergencyReadings() {
        try {
            String targetWACorrection = "90:up";
            String targetTACorrection = "0:neutral";
            String targetEACorrection = "-10";
            String deployOxygenMasks = "true";

            System.out.println("[x] [CONTROL-FC] Sending emergency actuator data readings to ALL Actuators...");
            System.out.println("===-----------------------===-----------------------===");
            System.out.println("[x] [ACTUATOR-WAWF] Target correction: " + targetWACorrection);
            System.out.println("[x] [ACTUATOR-TATF] Target correction: " + targetTACorrection);
            System.out.println("[x] [ACTUATOR-EAE] Target correction: " + targetEACorrection);
            System.out.println("[x] [ACTUATOR-OMA] Deploy Masks: Yes");

            //System.out.println("[" + targetWACorrection + "] & [" + targetTACorrection + "] & [" + targetEACorrection + "] & [" + deployOxygenMasks + "]");
            //ConnectionManager.resetEmergencyQueues(emergencyChannel);
            ConnectionManager.declareExchange(Exchanges.EMERGENCY.getName(), emergencyChannel);
            ConnectionManager.declareEmergencyQueues(emergencyChannel);
            ConnectionManager.declareEmergencyBindings(emergencyChannel);
            emergencyChannel.basicPublish(Exchanges.EMERGENCY.getName(), RoutingKeys.WING_FLAPS_TEMP.getKey(), null, targetWACorrection.getBytes());
            emergencyChannel.basicPublish(Exchanges.EMERGENCY.getName(), RoutingKeys.TAIL_FLAPS_TEMP.getKey(), null, targetTACorrection.getBytes());
            emergencyChannel.basicPublish(Exchanges.EMERGENCY.getName(), RoutingKeys.ENGINES_TEMP.getKey(), null, targetEACorrection.getBytes());
            emergencyChannel.basicPublish(Exchanges.EMERGENCY.getName(), RoutingKeys.OXYGEN_MASKS.getKey(), null, deployOxygenMasks.getBytes());

            //emergencyChannel.close();
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void receiveAltitudeReading() {
        try {
            sensorsChannel.basicConsume(SensorQueues.ALTITUDE.getName(), 
                    true, ((consumerTag, message) -> {
                String msg = new String(message.getBody(), "UTF-8");
                Plane.currentAltitude = Integer.parseInt(msg);
                System.out.println(
                        "[CONTROL-FC] Received altitude reading from [SENSOR-AS] (" 
                                + msg + ")");

            }), consumerTag -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }

    public void receiveCabinPressureReading() {
        try {
            sensorsChannel.basicConsume(SensorQueues.CABIN.getName(), true, ((consumerTag, message) -> {
                String m = new String(message.getBody(), "UTF-8");
                String formattedPressure = String.format("%.1f", Double.parseDouble(m));
                Plane.currentPressure = Double.parseDouble(formattedPressure);
                System.out.println("[CONTROL-FC] Received cabin pressure reading from [SENSOR-CPS] (" + formattedPressure + ")");
            }), consumerTag -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void receiveSpeedDirectionReading() {
        try {
            sensorsChannel.basicConsume(SensorQueues.SPEED_DIRECTION.getName(), true, ((consumerTag, message) -> {
                String msg = new String(message.getBody(), "UTF-8");
                String[] readings = msg.split(":");
                int speedReading = Integer.parseInt(readings[0]);
                int directionReading = Integer.parseInt(readings[1]);
                Plane.currentSpeed = speedReading;
                Plane.currentDirection = directionReading;

                System.out.println("[CONTROL-FC] Received Speed and Direction reading from [SENSOR-SDS] (" + msg + ")");
            }), consumerTag -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void receiveWeatherReading() {
        try {
            sensorsChannel.basicConsume(SensorQueues.WEATHER.getName(), true, (x, msg) -> {
                String m = new String(msg.getBody(), "UTF-8");
                int weatherCode = Integer.parseInt(m);
                if (weatherCode == 0) {
                    System.out.println("[CONTROL-FC] Received CLEAR SKY reading from [SENSOR-WS]");
                    Plane.currentWeather = WeatherCondition.CLEAR_SKY;
                } else if (weatherCode == 1) {
                    System.out.println("[CONTROL-FC] Received STORM/TURBULENCE reading from [SENSOR-WS]");
                    Plane.currentWeather = WeatherCondition.TURBULENCE;
                    pauseReadings();
                } else if (weatherCode == 2) {
                    System.out.println("[CONTROL-FC] Received ICING reading from [SENSOR-WS]");
                    Plane.currentWeather = WeatherCondition.ICING;
                }
            }, x -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /*
        PROCESS READINGS
     */

    public void processAltitudeOutOfRange(int currentAltitude) {
        int safeAltitude = 33000;
        int altitudeMargin = 400;
        int angleCorrection;
        String directionCorrection;
        if (currentAltitude > safeAltitude + altitudeMargin) {
            angleCorrection = (new Random().nextInt(6) - 5);
            directionCorrection = "up";
        } else if (currentAltitude < safeAltitude - altitudeMargin) {
            angleCorrection = (new Random().nextInt(6));
            directionCorrection = "down";
        } else {
            angleCorrection = 0;
            directionCorrection = "neutral";
        }

        String wingFlapCorrection = String.join("", String.valueOf(angleCorrection), ":", directionCorrection);
        sendWingActuatorData(wingFlapCorrection);
    }

    public void processDirectionDeviation(int currDir) {
        int courseDirection = 90; // assume 90 degrees is the desired course direction
        int deviation = Math.abs(currDir - courseDirection);
        int angleThreshold = 10; // assume a threshold of 10 degrees

        int angleCorrection;
        String directionCorrection;

        if (deviation > angleThreshold) {
            if (currDir > courseDirection) {
                angleCorrection = -1 * (deviation - angleThreshold) / 5;
                directionCorrection = "left";
            } else {
                angleCorrection = (deviation - angleThreshold) / 5;
                directionCorrection = "right";
            }
        } else {
            angleCorrection = 0;
            directionCorrection = "neutral";
        }

        String tailFlapCorrection = String.join("", String.valueOf(angleCorrection), ":", directionCorrection);
        sendTailActuatorData(tailFlapCorrection);
    }

    public void processSpeedDeviation(int currSpd, int currAlt) {
        int standardThrottle = 60;
        int maxSpeed = 570;
        int correspondingAltitude = 33000;
        int currentThrottle = currSpd * 100 / maxSpeed;
        int throttleDiff = Math.abs(standardThrottle - currentThrottle);
        int throttleCorrection;

        if (currAlt > correspondingAltitude) {
            if (throttleDiff <= 1) {
                // throttle within 2% margin, no correction needed
                throttleCorrection = 0;
            } else {
                Random r = new Random();
                throttleCorrection = r.nextInt(4) + 1;
                if (currentThrottle > standardThrottle) {
                    throttleCorrection = -throttleCorrection;
                }
            }

        } else {
            // throttle outside 5% margin, or altitude is lower than 36000
            throttleCorrection = 0;
        }

        String engineCorrection = Integer.toString(throttleCorrection);
        sendEngineActuatorData(engineCorrection);
    }

    /*
        Send corrections
    */
    public void sendWingActuatorData(String msg) {
        try {
            actuatorsChannel.basicPublish(Exchanges.ACTUATOR.getName(), RoutingKeys.WING_FLAPS.getKey(), false, null, msg.getBytes());
            System.out.println("[CONTROL-FC] Sending wing actuator data readings to [ACTUATOR-WAWF] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendTailActuatorData(String msg) {
        try {
            actuatorsChannel.basicPublish(Exchanges.ACTUATOR.getName(), RoutingKeys.TAIL_FLAPS.getKey(), false, null, msg.getBytes());
            System.out.println("[CONTROL-FC] Sending tail actuator data readings to [ACTUATOR-TATF] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void sendEngineActuatorData(String msg) {
        try {
            actuatorsChannel.basicPublish(Exchanges.ACTUATOR.getName(), RoutingKeys.ENGINES.getKey(), false, null, msg.getBytes());
            System.out.println("[CONTROL-FC] Sending engine actuator data readings to [ACTUATOR-EAE] (" + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

}
