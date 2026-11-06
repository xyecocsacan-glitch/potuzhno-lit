package ru.potuzhnolit;

/** Pure flight/animation math. Collision and authority remain on the server. */
public final class FlightSmoothing {
    // Updates arrive every tick. Keep a small interpolation buffer on the client.
    public static final int INTERPOLATION_TICKS = 2;
    private static final double TAU = Math.PI * 2;
    private static final FlightControls.Motion ZERO = new FlightControls.Motion(0, 0, 0);
    private FlightSmoothing() { }

    public static FlightControls.Motion accelerate(FlightControls.Motion current,
                                                    FlightControls.Motion target, boolean drone) {
        if (!finite(target)) return ZERO;
        if (!finite(current)) current = ZERO;
        double dx = target.x() - current.x(), dy = target.y() - current.y(), dz = target.z() - current.z();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean braking = target.x() == 0 && target.y() == 0 && target.z() == 0;
        double step = drone ? (braking ? .24 : .18) : (braking ? .16 : .12);
        if (distance <= step) return target;
        double factor = step / distance;
        return new FlightControls.Motion(current.x() + dx * factor,
            current.y() + dy * factor, current.z() + dz * factor);
    }

    private static boolean finite(FlightControls.Motion motion) {
        return Double.isFinite(motion.x()) && Double.isFinite(motion.y()) && Double.isFinite(motion.z());
    }

    public static float partial(float value) {
        return Float.isFinite(value) ? Math.clamp(value, 0f, 1f) : 0f;
    }

    /** Per-entity integrated phase: changing rotor speed must not change its angle instantly. */
    public static final class Rotor {
        private double previous, phase, speed = .18;
        public void tick(boolean powered) {
            double target = powered ? 2.4 : .18;
            speed += Math.clamp(target - speed, -.22, .22);
            previous = phase;
            phase += speed;
            if (phase >= TAU) {
                double turns = Math.floor(phase / TAU) * TAU;
                phase -= turns;
                previous -= turns;
            }
        }
        public float sample(float partialTick) {
            return (float) (previous + (phase - previous) * partial(partialTick));
        }
        public double speed() { return speed; }
    }
}
