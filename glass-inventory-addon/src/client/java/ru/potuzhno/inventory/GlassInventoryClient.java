package ru.potuzhno.inventory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Client-only inventory camera. No server/network dependencies. */
public final class GlassInventoryClient implements ClientModInitializer {
    private CameraType originalCamera;
    private boolean cameraSwitched;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::updateCamera);
    }

    private void updateCamera(Minecraft client) {
        boolean inInventory = client.screen instanceof InventoryScreen
            || client.screen instanceof CreativeModeInventoryScreen;
        if (client.level == null || client.player == null || !inInventory) {
            restoreCamera(client);
            return;
        }
        if (!cameraSwitched) {
            originalCamera = client.options.getCameraType();
            cameraSwitched = true;
        }
        if (client.options.getCameraType() != CameraType.THIRD_PERSON_FRONT) {
            client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        }
    }

    private void restoreCamera(Minecraft client) {
        if (!cameraSwitched) return;
        cameraSwitched = false;
        if (originalCamera != null) client.options.setCameraType(originalCamera);
        originalCamera = null;
    }
}
