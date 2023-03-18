package Plane.Components;

import lombok.Data;

public class LandingGear {
    private static boolean isDeployed;

    public static boolean isIsDeployed() {
        return isDeployed;
    }

    public static void setIsDeployed(boolean isDeployed) {
        OxygenMask.isDeployed = isDeployed;

    }
}
