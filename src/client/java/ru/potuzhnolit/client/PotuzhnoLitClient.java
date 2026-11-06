package ru.potuzhnolit.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.lwjgl.glfw.GLFW;
import ru.potuzhnolit.ControlPayload;
import ru.potuzhnolit.FlyingMachineEntity;
import ru.potuzhnolit.PotuzhnoLitMod;

public class PotuzhnoLitClient implements ClientModInitializer {
    private static KeyMapping DOWN;
    private static KeyMapping GUN;
    private static KeyMapping INTERACT;
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(PotuzhnoLitMod.MACHINE, EmptyRenderer::new);
        INTERACT = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.potuzhnolit.interact", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_E,
            KeyMapping.Category.MISC));
        DOWN = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.potuzhnolit.down", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X,
            KeyMapping.Category.MISC));
        GUN = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.potuzhnolit.gun", InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_LEFT,
            KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getConnection() == null) return;
            while (INTERACT.consumeClick()) {
                ClientPlayNetworking.send(new ControlPayload(256));
            }
            if (!(client.player.getVehicle() instanceof FlyingMachineEntity)) return;
            int flags = 0;
            if (client.options.keyUp.isDown()) flags |= 1;
            if (client.options.keyDown.isDown()) flags |= 2;
            if (client.options.keyLeft.isDown()) flags |= 4;
            if (client.options.keyRight.isDown()) flags |= 8;
            if (client.options.keyJump.isDown()) flags |= 16;
            if (DOWN.isDown()) flags |= 32;
            if (GUN.isDown()) flags |= 64;
            ClientPlayNetworking.send(new ControlPayload(flags));
        });
    }
    /** Appearance is supplied by the server-side vanilla block-display parts. */
    public static final class EmptyRenderer extends EntityRenderer<FlyingMachineEntity, EntityRenderState> {
        public EmptyRenderer(EntityRendererProvider.Context ctx) { super(ctx); }
        @Override public EntityRenderState createRenderState() { return new EntityRenderState(); }
    }
}
