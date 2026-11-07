package ru.potuzhnolit.client;

import java.util.Locale;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import ru.potuzhnolit.FlightControls;
import ru.potuzhnolit.FlyingMachineEntity;

/** Viewfinder and optional subtle CRT grain only. No random gameplay/cosmetic events. */
public final class FlightHud {
    private FlightHud() { }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        FlyingMachineEntity machine = PotuzhnoLitClient.activeMachine(client);
        if (machine == null || client.screen != null || client.options.hideGui) return;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        boolean remote = PotuzhnoLitClient.remote();
        if (TakeoffCutscene.active()) {
            graphics.fill(0, 0, width, 29, 0xDD000000);
            graphics.fill(0, height - 50, width, height, 0xDD000000);
            centered(graphics, client, TakeoffCutscene.caption(), width, height - 31, 0xFFFFD5A0);
            centered(graphics, client, Component.translatable("cutscene.potuzhnolit.skip"),
                width, height - 16, 0xFFE0E0E0);
            return;
        }
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
        if (!remote && PotuzhnoLitClient.aimingPlane(client))
            centered(graphics, client, Component.translatable("hud.potuzhnolit.aim_active"),
                width, height / 2 + 22, 0xFFFFC080);
        graphics.fill(0, height - 34, width, height, 0x88000000);
        centered(graphics, client, Component.translatable("hud.potuzhnolit.flight_keys",
            client.options.keyJump.getTranslatedKeyMessage(), PotuzhnoLitClient.DOWN.getTranslatedKeyMessage(),
            PotuzhnoLitClient.INTERACT.getTranslatedKeyMessage()), width, height - 28, 0xFFF0F0F0);
        Component second = remote
            ? Component.translatable("hud.potuzhnolit.drone_keys", PotuzhnoLitClient.DETONATE.getTranslatedKeyMessage(),
                PotuzhnoLitClient.EFFECTS.getTranslatedKeyMessage())
            : Component.translatable("hud.potuzhnolit.plane_keys", PotuzhnoLitClient.GUN.getTranslatedKeyMessage());
        centered(graphics, client, second, width, height - 15, 0xFFDADADA);
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
