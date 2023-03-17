/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Plane.Components;

/**
 *
 * @author ryann
 */
public class WingFlap {

    public static String direction;
    public static int angle;

    public static int getAngle() {
        return angle;
    }

    public static String getDirection() {
        return direction;
    }

    public static void setAngle(int angle) {
        WingFlap.angle = angle;
    }

    public static void setDirection(String direction) {
        WingFlap.direction = direction;
    }
}
