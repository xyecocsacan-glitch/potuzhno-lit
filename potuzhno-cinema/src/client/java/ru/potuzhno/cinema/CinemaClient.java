package ru.potuzhno.cinema;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Client-only cinematic demo, usable alongside other mods without server installation. */
public final class CinemaClient implements ClientModInitializer {
    private static KeyMapping PLAY;
    private static boolean running;
    private static int tick;
    private static boolean escapeHeldLast;
    private static Vec3 anchor;
    private static float heading;
    private static net.minecraft.world.level.Level world;

    public record Pose(Vec3 position, float yaw, float pitch) { }

    public static boolean active() { return running; }

    @Override public void onInitializeClient() {
        PLAY = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.potuzhnocinema.play", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8, KeyMapping.Category.MISC));
        ClientTickEvents.START_CLIENT_TICK.register(CinemaClient::step);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            Identifier.fromNamespaceAndPath("potuzhnocinema", "subtitles"), CinemaClient::draw);
    }

    private static void start(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null
                || client.player.isPassenger() || client.getCameraEntity() != client.player) return;
        anchor = client.player.position();
        heading = client.player.getYRot();
        world = client.level;
        tick = 0;
        escapeHeldLast = false;
        running = true;
        client.player.playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, .4f, 1.35f);
    }

    private static void stop() {
        running = false;
        anchor = null;
        world = null;
        tick = 0;
        escapeHeldLast = false;
    }

    private static void step(Minecraft client) {
        if (client.player == null || client.level == null || client.screen != null || !client.isWindowActive()) {
            stop();
            return;
        }
        while (PLAY.consumeClick()) {
            if (running) stop();
            else start(client);
        }
        if (!running) return;
        if (client.level != world || !client.player.isAlive() || client.player.isPassenger() || client.getCameraEntity() != client.player) { stop(); return; }
        long window = GLFW.glfwGetCurrentContext();
        boolean esc = window != 0 && GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
        if (esc && !escapeHeldLast) { stop(); return; }
        escapeHeldLast = esc;
        if (++tick >= CinemaTimeline.TOTAL_TICKS) stop();
    }

    public static Pose camera(float partialTick) {
        Minecraft client = Minecraft.getInstance();
        if (!running || client.player == null || client.level != world) return null;
        double t = Math.min(1, (tick + partialTick) / CinemaTimeline.TOTAL_TICKS);
        double orbit = Math.toRadians(heading) + .3 + Math.PI * 1.7 * CinemaTimeline.smooth(t);
        double radius = 4.0 + 1.5 * Math.sin(Math.PI * t);
        Vec3 eye = anchor.add(0, 1.5, 0);
        Vec3 camera = eye.add(Math.sin(orbit) * radius, 1.0 + .8 * Math.sin(Math.PI * t), Math.cos(orbit) * radius);
        Vec3 dir = eye.subtract(camera);
        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
        float pitch = (float) -Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));
        return new Pose(camera, yaw, pitch);
    }

    private static void draw(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!running) return;
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null || client.options.hideGui) return;
        int w = graphics.guiWidth(), h = graphics.guiHeight();
        graphics.fill(0, 0, w, 26, 0xDD05070C);
        graphics.fill(0, h - 56, w, h, 0xDD05070C);
        String key = switch (CinemaTimeline.cue(tick)) {
            case 0 -> "cinema.potuzhnocinema.one";
            case 1 -> "cinema.potuzhnocinema.two";
            default -> "cinema.potuzhnocinema.three";
        };
        Component subtitle = Component.translatable(key);
        Component skip = Component.translatable("cinema.potuzhnocinema.skip");
        graphics.drawString(client.font, subtitle, Math.max(4, (w - client.font.width(subtitle)) / 2), h - 38, 0xFFFFD8AD, true);
        graphics.drawString(client.font, skip, Math.max(4, (w - client.font.width(skip)) / 2), h - 22, 0xFFE8E8E8, true);
    }
}
