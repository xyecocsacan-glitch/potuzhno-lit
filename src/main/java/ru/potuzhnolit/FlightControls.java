package ru.potuzhnolit;

/** Pure, testable control math shared by both machines. */
public final class FlightControls {
    public static final int FORWARD = 1, BACK = 2, LEFT = 4, RIGHT = 8;
    public static final int UP = 16, DOWN = 32, FIRE = 64;
    public static final int DETONATE = 128, INTERACT = 256, EXIT = 512;
    public static final int MOVEMENT_MASK = 127;
    // Twice the old radius. Unloaded chunks remain off-limits; no unlimited chunk forcing.
    public static final double REMOTE_RANGE = 128.0;
    private FlightControls() { }
    public record Motion(double x, double y, double z) { }
    public static int verticalAxis(int flags) {
        return (flags & DOWN) != 0 ? -1 : (flags & UP) != 0 ? 1 : 0;
    }
    public static Motion motion(int flags, float yaw, boolean drone) {
        flags &= MOVEMENT_MASK;
        double forward = ((flags & FORWARD) != 0 ? 1 : 0) - ((flags & BACK) != 0 ? 1 : 0);
        double right = ((flags & RIGHT) != 0 ? 1 : 0) - ((flags & LEFT) != 0 ? 1 : 0);
        double length = Math.hypot(forward, right);
        if (length > 1) { forward /= length; right /= length; }
        double angle = Math.toRadians(Float.isFinite(yaw) ? yaw : 0);
        double speed = drone ? .7 : .42;
        return new Motion((-Math.sin(angle) * forward + Math.cos(angle) * right) * speed,
            verticalAxis(flags) * speed * .8,
            (Math.cos(angle) * forward + Math.sin(angle) * right) * speed);
    }
    public static boolean mayDetonate(int flags, boolean drone, boolean authorized) {
        return authorized && drone && (flags & DETONATE) != 0;
    }
}
