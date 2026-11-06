package ru.potuzhnolit.client;

import java.util.Locale;
import java.util.Random;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import ru.potuzhnolit.FlightControls;
import ru.potuzhnolit.FlyingMachineEntity;

/** Cosmetic HUD only: jokes and CRT grain never alter flight physics or world blocks. */
public final class FlightHud {
    private static final Random RANDOM = new Random();
    private static int untilJoke = 300, jokeTicks, jokeId;
    private FlightHud() { }
    public static void resetFlight() { untilJoke = 300; jokeTicks = 0; }
    public static void clearJoke() { jokeTicks = 0; }

    public static void tick(Minecraft client) {
        if (jokeTicks > 0) jokeTicks--;
        if (!PotuzhnoLitClient.jokesEnabled || --untilJoke > 0) return;
        untilJoke = 600 + RANDOM.nextInt(601);
        jokeId = RANDOM.nextInt(5);
        jokeTicks = 100;
        Entity camera = client.getCameraEntity();
        if (camera == null || client.level == null) return;
        SoundEvent sound = switch (jokeId) {
            case 0 -> SoundEvents.CHICKEN_AMBIENT;
            case 1 -> SoundEvents.VILLAGER_YES;
            case 2 -> SoundEvents.SHEEP_AMBIENT;
            case 3 -> SoundEvents.COW_AMBIENT;
            default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
        };
        client.level.playLocalSound(camera.getX(), camera.getY(), camera.getZ(), sound, SoundSource.PLAYERS, .22f, 1.25f, false);
        Vec3 point = camera.getEyePosition().add(camera.getLookAngle().scale(3));
        for (int i = 0; i < 5; i++) client.level.addParticle(ParticleTypes.NOTE,
            point.x + (RANDOM.nextDouble() - .5), point.y - .4, point.z + (RANDOM.nextDouble() - .5),
            RANDOM.nextDouble(), 0, 0);
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        FlyingMachineEntity machine = PotuzhnoLitClient.activeMachine(client);
        if (machine == null || client.screen != null || client.options.hideGui) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        boolean remote = PotuzhnoLitClient.remote();
        if (remote) {
            if (PotuzhnoLitClient.noiseEnabled) drawNoise(graphics, width, height);
            drawFrame(graphics, width, height);
            long seconds = PotuzhnoLitClient.remoteTicks() / 20;
            String time = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
            graphics.fill(22, 22, 27, 27, 0xFFE06060);
            graphics.drawString(client.font, "REC " + time, 32, 21, 0xFFF0F0F0, true);
            double distance = PotuzhnoLitClient.anchor() == null ? 0
                : machine.position().distanceTo(PotuzhnoLitClient.anchor());
            int signal = (int) Math.max(0, 100 * (1 - distance / FlightControls.REMOTE_RANGE));
            Component link = Component.translatable("hud.potuzhnolit.signal", signal + "%");
            graphics.drawString(client.font, link, Math.max(28, width - client.font.width(link) - 24), 21,
                signal < 15 ? 0xFFFFC080 : 0xFFE8F1E8, true);
            graphics.drawString(client.font, Component.translatable("hud.potuzhnolit.distance",
                (int) distance, (int) FlightControls.REMOTE_RANGE), 23, 34, 0xFFDDDDDD, true);
            graphics.drawString(client.font, "Y: " + (int) machine.getY(), 23, 47, 0xFFDDDDDD, true);
            if (signal < 10) centered(graphics, client,
                Component.translatable("hud.potuzhnolit.range_limit"), width, 62, 0xFFFFC080);
        }
        drawCrosshair(graphics, width / 2, height / 2);
        graphics.fill(0, height - 34, width, height, 0x88000000);
        centered(graphics, client, Component.translatable("hud.potuzhnolit.flight_keys",
            client.options.keyJump.getTranslatedKeyMessage(), PotuzhnoLitClient.DOWN.getTranslatedKeyMessage(),
            PotuzhnoLitClient.INTERACT.getTranslatedKeyMessage()), width, height - 28, 0xFFF0F0F0);
        Component second = remote
            ? Component.translatable("hud.potuzhnolit.drone_keys", PotuzhnoLitClient.DETONATE.getTranslatedKeyMessage(),
                PotuzhnoLitClient.EFFECTS.getTranslatedKeyMessage(), PotuzhnoLitClient.JOKES.getTranslatedKeyMessage())
            : Component.translatable("hud.potuzhnolit.plane_keys", PotuzhnoLitClient.GUN.getTranslatedKeyMessage(),
                PotuzhnoLitClient.JOKES.getTranslatedKeyMessage());
        centered(graphics, client, second, width, height - 15, 0xFFDADADA);
        if (jokeTicks > 0 && PotuzhnoLitClient.jokesEnabled)
            centered(graphics, client, Component.translatable("joke.potuzhnolit." + jokeId),
                width, height - 52, 0xFFFFE0A0);
    }

    private static void centered(GuiGraphics graphics, Minecraft client, Component text, int width, int y, int color) {
        graphics.drawString(client.font, text, Math.max(4, (width - client.font.width(text)) / 2), y, color, true);
    }
    private static void drawCrosshair(GuiGraphics graphics, int x, int y) {
        int color = 0xDDFFFFFF;
        graphics.fill(x - 9, y, x - 3, y + 1, color);
        graphics.fill(x + 4, y, x + 10, y + 1, color);
        graphics.fill(x, y - 9, x + 1, y - 3, color);
        graphics.fill(x, y + 4, x + 1, y + 10, color);
    }
    private static void drawFrame(GuiGraphics graphics, int width, int height) {
        int margin = 12, length = 24, bottom = height - 42, color = 0xDDEAF0E8;
        graphics.fill(0, 0, width, 5, 0x55000000);
        graphics.fill(0, 5, 5, height - 34, 0x55000000);
        graphics.fill(width - 5, 5, width, height - 34, 0x55000000);
        // Four viewfinder brackets: the center of the image stays unobstructed.
        graphics.fill(margin, margin, margin + length, margin + 2, color);
        graphics.fill(margin, margin, margin + 2, margin + length, color);
        graphics.fill(width - margin - length, margin, width - margin, margin + 2, color);
        graphics.fill(width - margin - 2, margin, width - margin, margin + length, color);
        graphics.fill(margin, bottom - 2, margin + length, bottom, color);
        graphics.fill(margin, bottom - length, margin + 2, bottom, color);
        graphics.fill(width - margin - length, bottom - 2, width - margin, bottom, color);
        graphics.fill(width - margin - 2, bottom - length, width - margin, bottom, color);
    }
    private static void drawNoise(GuiGraphics graphics, int width, int height) {
        // Low opacity, sparse grains, no full-screen flashing or camera shake.
        for (int y = 3; y < height - 34; y += 4) graphics.fill(5, y, width - 5, y + 1, 0x0A000000);
        long seed = System.nanoTime() / 160_000_000L;
        for (int i = 0; i < 24; i++) {
            seed = seed * 1664525L + 1013904223L;
            int x = (int) Math.floorMod(seed, Math.max(1, width - 16)) + 8;
            seed = seed * 1664525L + 1013904223L;
            int y = (int) Math.floorMod(seed, Math.max(1, height - 48)) + 8;
            graphics.fill(x, y, x + 2, y + 1, 0x20FFFFFF);
        }
    }
}
