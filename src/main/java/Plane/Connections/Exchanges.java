/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Connections;

/**
 *
 * @author ryann
 */
public enum Exchanges {
    SENSOR("sensor.exchange"),
    ACTUATOR("actuator.exchange"),
    EMERGENCY("emergency.exchange"),
    SENSOR_LANDING("temp"),
    ACTUATOR_LANDING("temp");

    private final String name;

    Exchanges(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
