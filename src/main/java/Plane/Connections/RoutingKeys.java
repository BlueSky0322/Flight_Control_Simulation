/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Connections;

/**
 *
 * @author ryann
 */
public enum RoutingKeys {
    ALTITUDE("altitude_key"),
    CABIN("cabin_key"),
    SPEED_DIRECTION("speed_dir_key"),
    WEATHER("weather_key"),
    ENGINES("engines_key"),
    WING_FLAPS("wing_flaps_key"),
    TAIL_FLAPS("tail_flaps_key"),
    ENGINES_TEMP("engines_emer_key"),
    WING_FLAPS_TEMP("wing_flaps_emer_key"),
    TAIL_FLAPS_TEMP("tail_flaps_emer_key"),
    FC_TO_ACTUATORS("flightcontroller.actuators"),
    LANDING_GEAR("landing_gear_key"),
    OXYGEN_MASKS("oxygen.mask.key"),
    EMERGENCY("emergency");
    private final String value;

    RoutingKeys(String value) {
        this.value = value;
    }

    public String getKey() {
        return value;
    }
}
