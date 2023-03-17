/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Utils;

/**
 *
 * @author ryann
 */
public enum WeatherCondition {
    CLEAR_SKY(0),
    TURBULENCE(1),
    ICING(2);
    private final int code;

    private WeatherCondition(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
