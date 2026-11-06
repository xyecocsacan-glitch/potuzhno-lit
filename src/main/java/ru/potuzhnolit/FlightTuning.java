package ru.potuzhnolit;

/** Pure flight/camera mathematics, shared by rendering, server aiming and tests. */
public final class FlightTuning {
    public static final int INTERPOLATION_TICKS = 3;
    public static final double PLANE_CAMERA_HEIGHT = 2.6;
    public static final double PLANE_CAMERA_BACK = 3.2;
    public static final double GUN_RANGE = 36.0;
    /** Damage per machine-gun hit; doubled from 4 for version 0.7.1. */
    public static final float MACHINE_GUN_DAMAGE = 8.0f;
    public static final float DRONE_EXPLOSION_POWER = 5.0f;
    private FlightTuning() { }

    public static double velocity(double current, double target) {
        if (!Double.isFinite(current) || !Double.isFinite(target)) return 0;
        double result = current + (target - current) * (target == 0 ? .45 : .35);
        return Math.abs(result) < .0005 ? 0 : result;
    }

    public static float turn(float current, float target) {
        if (!Float.isFinite(current) || !Float.isFinite(target)) return 0;
        float difference = wrap(target - current);
        return wrap(current + difference * .35f);
    }

    public static float wrap(float angle) {
        float result = angle % 360;
        if (result >= 180) result -= 360;
        if (result < -180) result += 360;
        return result;
    }

    public record Offset(double x, double y, double z) { }
    public static Offset cameraOffset(boolean drone, float yaw) {
        double angle = Math.toRadians(yaw);
        double forward = drone ? 1.45 : -PLANE_CAMERA_BACK;
        return new Offset(-Math.sin(angle) * forward, drone ? .65 : PLANE_CAMERA_HEIGHT,
            Math.cos(angle) * forward);
    }
}
