/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Connections;

/**
 *
 * @author ryann
 */
public enum ActuatorQueues {
    ENGINES("engine.actuator"),
    WING_FLAPS("wing.actuator"),
    TAIL_FLAPS("tail.actuator"),
    ENGINES_TEMP("engine.actuator.emergency"),
    WING_FLAPS_TEMP("wing.actuator.emergency"),
    TAIL_FLAPS_TEMP("tail.actuator.emergency"),
    LANDING_GEAR("landing.gear.actuator"),
    OXYGEN_MASKS("oxygen.masks.actuator");

    private final String name;

    ActuatorQueues(String name
    ) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
