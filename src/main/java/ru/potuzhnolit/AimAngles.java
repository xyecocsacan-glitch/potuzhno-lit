package ru.potuzhnolit;

/** Mouse-controlled aim is independent of passenger/body rotation and server interpolation. */
public final class AimAngles {
    private float yaw;
    private float pitch;
    public static final float MAX_PITCH = 85;
    public void reset(float yaw, float pitch) {
        this.yaw = Float.isFinite(yaw) ? FlightTuning.wrap(yaw) : 0;
        this.pitch = Float.isFinite(pitch) ? Math.clamp(pitch, -MAX_PITCH, MAX_PITCH) : 0;
    }
    public void turn(double horizontal, double vertical) {
        if (!Double.isFinite(horizontal) || !Double.isFinite(vertical)) return;
        // MouseHandler has already applied sensitivity, cinematic smoothing and inversion.
        double nextYaw = (yaw + horizontal * .15) % 360;
        double nextPitch = Math.clamp(pitch + vertical * .15, -MAX_PITCH, MAX_PITCH);
        reset((float) nextYaw, (float) nextPitch);
    }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
}
