/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Connections;

/**
 *
 * @author ryann
 */
public enum SensorQueues {
    ALTITUDE( "altitude.sensor"),
    CABIN( "cabin.sensor"),
    SPEED_DIRECTION( "speed.direction.sensor"),
    WEATHER( "weather.sensor");
    
    private final String name;

    SensorQueues(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
