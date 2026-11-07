package ru.potuzhno.cinema;

/** Standalone deterministic timeline: no Minecraft or rendering dependencies. */
public final class CinemaTimeline {
    public static final int TOTAL_TICKS = 240;
    public static final int FIRST_CUE_END = 70;
    public static final int SECOND_CUE_END = 160;
    private CinemaTimeline() { }
    public static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
    public static int cue(int tick) {
        if (tick < FIRST_CUE_END) return 0;
        if (tick < SECOND_CUE_END) return 1;
        return 2;
    }
    public static boolean finished(int tick) { return tick >= TOTAL_TICKS; }
}
