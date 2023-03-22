/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.FC;

import Plane.Actuators.EngineActuator;
import Plane.Actuators.LandingGearActuator;
import Plane.Actuators.OxygenMaskActuator;
import Plane.Actuators.TailActuator;
import Plane.Actuators.WingActuator;
import Plane.Components.LandingGear;
import Plane.Connections.ConnectionManager;
import Plane.Connections.Exchanges;
import Plane.Connections.RoutingKeys;
import Plane.Connections.SensorQueues;
import Plane.Main.Plane;
import Plane.Sensors.AltitudeSensor;
import Plane.Sensors.CabinPressureSensor;
import Plane.Sensors.SpeedDirectionSensor;
import Plane.Sensors.WeatherSensor;
import Plane.Sensors.WeatherCondition;
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
    private volatile boolean fclandingEvent = false;

    public FlightController() {
        try {
            factory = new ConnectionFactory();
            connection = factory.newConnection();

            //creates sensor channel
            sensorsChannel = connection.createChannel();
            ConnectionManager.declareExchange(Exchanges.SENSOR.getName(), sensorsChannel);
            ConnectionManager.declareSensorQueues(sensorsChannel);
            ConnectionManager.declareSensorBindings(sensorsChannel);

            //creates actuator channel
            actuatorsChannel = connection.createChannel();
            ConnectionManager.declareExchange(Exchanges.ACTUATOR.getName(), actuatorsChannel);
            ConnectionManager.declareActuatorQueues(actuatorsChannel);
            ConnectionManager.declareActuatorBindings(actuatorsChannel);
            
            //creates emergency channel
            emergencyChannel = connection.createChannel();

        } catch (IOException | TimeoutException ex) {
            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        //as long as thread is still running
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
                        fclandingEvent = true;
                    }
                }
            } else if (state.equals("landing") && fclandingEvent) {
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

    
    //CONSUMERS TO RECEIVE SENSORS READINGS
    public void receiveAltitudeReading() {
        try {
            sensorsChannel.basicConsume(SensorQueues.ALTITUDE.getName(),
                    true, ((consumerTag, message) -> {
                        String msg = new String(message.getBody(), "UTF-8");
                        //set current altitude of the plane
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
                //set current cabin pressure of the plane
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
                //set current speed and direction of the plane
                Plane.currentSpeed = speedReading;
                Plane.currentDirection = directionReading;

                System.out.println("[CONTROL-FC] Received Speed and Direction reading from [SENSOR-SDS] (" + msg + ")");
            }), consumerTag -> {
            });
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    //contains trigger to sudden cabin pressure loss event
    public void receiveWeatherReading() {
        try {
            sensorsChannel.basicConsume(SensorQueues.WEATHER.getName(),
                    true, (x, msg) -> {
                        String m = new String(
                                msg.getBody(), "UTF-8");
                        int weatherCode = Integer.parseInt(m);
                        switch (weatherCode) {
                            case 0:
                                System.out.println(
                                        "[CONTROL-FC] Received CLEAR SKY reading from [SENSOR-WS]");
                                //set current weather readings of the plane
                                Plane.currentWeather = WeatherCondition.CLEAR_SKY;
                                break;
                            case 1:
                                System.out.println(
                                        "[CONTROL-FC] Received STORM/TURBULENCE reading from [SENSOR-WS]");
                                Plane.currentWeather = WeatherCondition.TURBULENCE;
                                //if weather sensors detects turbulence, trigger cabin pressure loss event
                                pauseNormalCommunications();
                                break;
                            case 2:
                                System.out.println(
                                        "[CONTROL-FC] Received ICING reading from [SENSOR-WS]");
                                Plane.currentWeather = WeatherCondition.ICING;
                                break;
                            default:
                                break;
                        }
                    }, x -> {
                    });
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }

    
    
    //PROCESS READINGS FROM SENSOR PRODUCERS
    //data processing of altitude readings
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

        String wingFlapCorrection
                = String.join("",
                        String.valueOf(angleCorrection),
                        ":",
                        directionCorrection);
        sendWingActuatorData(wingFlapCorrection);
    }

    //data processing of direction readings
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

        String tailFlapCorrection = String.join("",
                String.valueOf(angleCorrection),
                ":", directionCorrection);
        sendTailActuatorData(tailFlapCorrection);
    }

    //data processing of speed readings 
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
    
    

    //PRODUCERS TO SEND CORRECTIONS TO RESPECTIVE ACTUATORS
    //send corrections to wing actuator
    public void sendWingActuatorData(String msg) {
        try {
            actuatorsChannel.basicPublish(
                    Exchanges.ACTUATOR.getName(),
                    RoutingKeys.WING_FLAPS.getKey(),
                    false, null, msg.getBytes());
            System.out.println(
                    "[CONTROL-FC] Sending wing actuator data readings to [ACTUATOR-WAWF] ("
                    + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    //send corrections to tail actuator
    public void sendTailActuatorData(String msg) {
        try {
            actuatorsChannel.basicPublish(
                    Exchanges.ACTUATOR.getName(),
                    RoutingKeys.TAIL_FLAPS.getKey(),
                    false, null, msg.getBytes());
            System.out.println(
                    "[CONTROL-FC] Sending tail actuator data readings to [ACTUATOR-TATF] ("
                    + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    //send corrections to engine actuator
    public void sendEngineActuatorData(String msg) {
        try {
            actuatorsChannel.basicPublish(
                    Exchanges.ACTUATOR.getName(),
                    RoutingKeys.ENGINES.getKey(),
                    false, null, msg.getBytes());
            System.out.println(
                    "[CONTROL-FC] Sending engine actuator data readings to [ACTUATOR-EAE] ("
                    + msg + ")");
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    
    
    //CABIN PRESSURE LOSS EVENT HANDLING
    //start the cabin pressure loss event
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
        System.out.println(
                "[x] [CONTROL-FC] EMERGENCY PROTOCOL ENABLED!");
    }

    //stop the cabin pressure loss event
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
        System.out.println(
                "[x] [CONTROL-FC] EMERGENCY PROTOCOL DISABLED!");
    }

    //pausing all normal communications, sleep thread for 3 secs to simulate event duration
    public void pauseNormalCommunications() {
        Plane.currentPressure = 1.0;
        System.out.println(
                "\n[x] [CONTROL-FC] Sudden cabin pressure loss detected! Current Cabin Pressure ("
                + Plane.currentPressure + ")");
        System.out.println();
        System.out.println("!!!=========!!!=========!!!=========!!!=========!!!=========!!!=========!!!=========!!!");

        startCabinPressureLossEvent();

        System.out.println("[x] [CONTROL-FC] Stabilizing aircraft parameters...");
        publishEmergencyReadings();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Logger.getLogger(FlightController.class.getName())
                    .log(Level.SEVERE, null, ex);
        } finally {
            System.out.println("===-----------------------===-----------------------===");
            System.out.println("\n[!] [CONTROL-FC] SUCCESSFULLY Regained control over aircraft!\n");

            stopCabinPressureLossEvent();

            System.out.println("!!!=========!!!=========!!!=========!!!=========!!!=========!!!=========!!!=========!!!\n");
        }
    }

    //sending emergency readings to actuators
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

            ConnectionManager.
                    declareExchange(Exchanges.EMERGENCY.getName(), emergencyChannel);
            ConnectionManager.
                    declareEmergencyQueues(emergencyChannel);
            ConnectionManager.
                    declareEmergencyBindings(emergencyChannel);
            emergencyChannel.basicPublish(
                    Exchanges.EMERGENCY.getName(),
                    RoutingKeys.WING_FLAPS_TEMP.getKey(),
                    null, targetWACorrection.getBytes());
            emergencyChannel.basicPublish(
                    Exchanges.EMERGENCY.getName(),
                    RoutingKeys.TAIL_FLAPS_TEMP.getKey(),
                    null, targetTACorrection.getBytes());
            emergencyChannel.basicPublish(
                    Exchanges.EMERGENCY.getName(),
                    RoutingKeys.ENGINES_TEMP.getKey(),
                    null, targetEACorrection.getBytes());
            emergencyChannel.basicPublish(
                    Exchanges.EMERGENCY.getName(),
                    RoutingKeys.OXYGEN_MASKS.getKey(),
                    null, deployOxygenMasks.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(FlightController.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
    }

    
    
    //LANDING MODE FOR PLANE
    //start the landing of the plane, sending new set of readings to actuators
    private void engageLandingRoutine() {
        sendAbsoluteLandingDataToWingActuator();
        sendAbsoluteLandingDataToTailActuator();
        sendAbsoluteLandingDataToEngineActuator();
        if (!LandingGear.isDeployed) {
            //deploys the landing gear
            LandingGearActuator.deployLandingGear();
            sendLandingSignalToLandingGear();
            LandingGearActuator.stopDeployLandingGear();
        }
    }

    // producers to send landing readings to the actuators
    private void sendLandingSignalToLandingGear() {
        String msg = "true";
        try {
            actuatorsChannel.basicPublish(Exchanges.ACTUATOR.getName(), RoutingKeys.LANDING_GEAR.getKey(), false, null, msg.getBytes());
            System.out.println("{LANDING} [CONTROL-FC] Sending landing gear command to [ACTUATOR-LGALG] (" + msg + ")");

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

}
