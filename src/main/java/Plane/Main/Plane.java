/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Main;

import Plane.Actuators.*;
import Plane.Connections.ConnectionManager;
import Plane.FC.FlightController;
import Plane.Sensors.AltitudeSensor;
import Plane.Sensors.CabinPressureSensor;
import Plane.Sensors.SpeedDirectionSensor;
import Plane.Sensors.WeatherSensor;
import Plane.Utils.WeatherCondition;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ryann
 */
public class Plane {

    private static Thread altThread;
    private static Thread cpsThread;
    private static Thread sdsThread;
    private static Thread wsThread;
    private static Thread waThread;
    private static Thread taThread;
    private static Thread eaThread;
    private static Thread omaThread;
    private static Thread fcThread;
//    private ExecutorService executorService = Executors.newScheduledThreadPool(8);
//    private final AltitudeSensor as;
//    private final CabinPressureSensor cps;
//    private final SpeedDirectionSensor sds;
//    private final WeatherSensor ws;
//    private final WingActuator wa;
//    private final TailActuator ta;
//    private final EngineActuator ea;
//    private final OxygenMaskActuator oma;
//    private final FlightController fc;

    public static int currentAltitude = 33000; //normal cruising currentAltitude of planes
    public static double currentPressure = 8.9; //normal cabin pressures
    public static int currentSpeed = 300; // normal cruising speed in knots
    public static int currentDirection = 90; // normal direction in degrees (eastward)
    public static WeatherCondition currentWeather = WeatherCondition.CLEAR_SKY;// normal weather condition

    public static final int LANDING_DURATION = 10;

    public Plane() {

    }


    public static void main(String[] args) {
        Plane plane = new Plane();

        try {
            plane.start();
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            Logger.getLogger(Plane.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("================plane is landing===================");

        stop();

        try {

            Thread.sleep(LANDING_DURATION * 1000);
            System.out.println("==============plane is stopping============");
            stop();
        } catch (InterruptedException ex) {
            Logger.getLogger(Plane.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void start() {

        Plane.altThread = new Thread(new AltitudeSensor());
        Plane.cpsThread = new Thread(new CabinPressureSensor());
        Plane.sdsThread = new Thread(new SpeedDirectionSensor());
        Plane.wsThread = new Thread(new WeatherSensor());
        Plane.waThread = new Thread(new WingActuator());
        Plane.taThread = new Thread(new TailActuator());
        Plane.eaThread = new Thread(new EngineActuator());
        Plane.omaThread = new Thread(new OxygenMaskActuator());
        Plane.fcThread = new Thread(new FlightController());

        Plane.altThread.start();
        Plane.cpsThread.start();
        Plane.sdsThread.start();
        Plane.wsThread.start();
        Plane.waThread.start();
        Plane.taThread.start();
        Plane.eaThread.start();
        Plane.omaThread.start();
        Plane.fcThread.start();
    }

    public static void stop() {
        Plane.altThread.interrupt();
        Plane.cpsThread.interrupt();
        Plane.sdsThread.interrupt();
        Plane.wsThread.interrupt();
        Plane.waThread.interrupt();
        Plane.taThread.interrupt();
        Plane.eaThread.interrupt();
        Plane.omaThread.interrupt();
        Plane.fcThread.interrupt();
    }


    public static int getCurrentAltitude() {
        return currentAltitude;
    }

    public static void setCurrentAltitude(int alt) {
        currentAltitude = alt;
    }

    public static double getCurrentPressure() {
        return currentPressure;
    }

    public static void setCurrentPressure(double cp) {
        currentPressure = cp;
    }

    public static int getCurrentSpeed() {
        return currentSpeed;
    }

    public static void setCurrentSpeed(int spd) {
        currentSpeed = spd;
    }

    public static int getCurrentDirection() {
        return currentDirection;
    }

    public static void setCurrentDirection(int dir) {
        currentDirection = dir;
    }

    public static WeatherCondition getCurrentWeather() {
        return currentWeather;
    }

    public static void setCurrentWeather(WeatherCondition currentWeather) {
        currentWeather = currentWeather;
    }
}


