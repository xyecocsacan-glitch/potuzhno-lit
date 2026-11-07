package ru.potuzhnolit.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import ru.potuzhnolit.FlyingMachineEntity;

/**
 * Client-only 12-second takeoff sequence. The plane remains server-authoritative:
 * no teleporting, invulnerability, forced chunks or server-side cinematic state.
 */
public final class TakeoffCutscene {
    public static final int DURATION_TICKS = 240;
    public static final int LIFT_TICK = 145;
    public static final int HANDOFF_TICK = 220;
    private static FlyingMachineEntity machine;
    private static int tick;
    private static boolean running;
    private static boolean wasEscape;
    private static boolean skipNotice;
    private static float startYaw;

    private TakeoffCutscene() { }

    public static boolean active() { return running; }
    public static int time() { return tick; }

    public static boolean start(Minecraft client, FlyingMachineEntity plane) {
        if (plane == null || plane.isRemoved() || plane.getVariant() != 1
                || client.player == null || client.player.getVehicle() != plane
                || client.screen != null || PotuzhnoLitClient.remote()) return false;
        machine = plane;
        startYaw = plane.getYRot();
        tick = 0;
        running = true;
        wasEscape = false;
        skipNotice = true;
        client.player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, .5f, .75f);
        return true;
    }

    public static void stop() {
        running = false;
        machine = null;
        tick = 0;
        wasEscape = false;
        skipNotice = false;
    }

    public static void step(Minecraft client, boolean escapeHeld) {
        if (!running) return;
        if (client.player == null || client.level == null || client.player.getVehicle() != machine
                || machine.isRemoved() || client.screen != null || !client.isWindowActive()) {
            stop();
            return;
        }
        if (escapeHeld && !wasEscape) {
            stop();
            return;
        }
        wasEscape = escapeHeld;
        tick++;
        if (tick == 65) client.player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, .7f, 1.25f);
        if (tick == 145) client.player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, .9f, 1.6f);
        if (tick >= DURATION_TICKS) stop();
    }

    /** During the final five seconds take off using normal server-side flight controls. */
    public static int flightFlags() {
        if (!running) return 0;
        if (tick >= LIFT_TICK) return ru.potuzhnolit.FlightControls.FORWARD
            | ru.potuzhnolit.FlightControls.UP;
        return 0;
    }
    public static float heading(FlyingMachineEntity plane) {
        return running && plane == machine ? startYaw : plane.getYRot();
    }

    public static Component caption() {
        if (!running) return Component.empty();
        if (tick < 65) return Component.translatable("cutscene.potuzhnolit.engine");
        if (tick < LIFT_TICK) return Component.translatable("cutscene.potuzhnolit.prepare");
        if (tick < HANDOFF_TICK) return Component.translatable("cutscene.potuzhnolit.takeoff");
        return Component.translatable("cutscene.potuzhnolit.controls");
    }

    public static boolean showSkipHint() { return running && skipNotice; }

    public static FlightCamera.Pose pose(FlyingMachineEntity plane, float partialTick) {
        if (!running || plane != machine) return null;
        float t = Math.min(DURATION_TICKS, tick + partialTick);
        double yawRadians = Math.toRadians(startYaw);
        Vec3 forward = new Vec3(-Math.sin(yawRadians), 0, Math.cos(yawRadians));
        Vec3 right = new Vec3(Math.cos(yawRadians), 0, Math.sin(yawRadians));
        double progress = Math.min(1, t / DURATION_TICKS);
        double orbit = Math.PI * (0.85 - .65 * smooth(Math.min(1, t / 145.0)));
        double radius = 4.8 + 1.7 * Math.sin(Math.PI * progress);
        double side = radius * Math.sin(orbit), back = radius * Math.cos(orbit);
        Vec3 center = plane.getPosition(partialTick).add(0, 1.2, 0);
        Vec3 camera = center.add(right.scale(side)).subtract(forward.scale(back))
            .add(0, 1.2 + 1.2 * smooth(progress), 0);
        // Final moments gently settle behind the raised normal aircraft camera.
        if (t > HANDOFF_TICK) {
            double handoff = smooth((t - HANDOFF_TICK) / (DURATION_TICKS - HANDOFF_TICK));
            Vec3 normal = center.add(0, 1.4, 0).subtract(forward.scale(3.2));
            camera = camera.lerp(normal, handoff);
        }
        Vec3 aim = center.add(forward.scale(t >= LIFT_TICK ? 2.0 : .2));
        Vec3 direction = aim.subtract(camera);
        double horizontal = Math.hypot(direction.x, direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, horizontal));
        return new FlightCamera.Pose(camera, Mth.wrapDegrees(yaw), Mth.clamp(pitch, -85, 85));
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
