package ru.potuzhnolit.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import ru.potuzhnolit.ControlPayload;
import ru.potuzhnolit.DroneViewPayload;
import ru.potuzhnolit.FlightControls;
import ru.potuzhnolit.FlyingMachineEntity;
import ru.potuzhnolit.PotuzhnoLitMod;

public class PotuzhnoLitClient implements ClientModInitializer {
    public static KeyMapping DOWN, GUN, INTERACT, DETONATE, EFFECTS, JOKES;
    public static boolean noiseEnabled = true, jokesEnabled = true;
    private static int droneId = -1, missingTicks, remoteTicks;
    private static DroneCamera camera;
    private static Entity previousCamera;
    private static CameraType previousCameraType;
    private static Vec3 bodyAnchor;
    private static boolean previousNoGravity;
    private static boolean wasInteract, wasDetonate, wasEffects, wasJokes;

    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(PotuzhnoLitMod.MACHINE, MachineRenderer::new);
        INTERACT = binding("interact", GLFW.GLFW_KEY_E);
        DOWN = binding("down", GLFW.GLFW_KEY_X);
        GUN = binding("fire", GLFW.GLFW_KEY_R);
        DETONATE = binding("detonate", GLFW.GLFW_KEY_G);
        EFFECTS = binding("effects", GLFW.GLFW_KEY_V);
        JOKES = binding("jokes", GLFW.GLFW_KEY_B);
        ClientPlayNetworking.registerGlobalReceiver(DroneViewPayload.TYPE, (payload, context) ->
            context.client().execute(() -> beginView(context.client(), payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> endView(client));
        ClientTickEvents.START_CLIENT_TICK.register(PotuzhnoLitClient::tickStart);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (remote() && client.player != null) {
                freezeBody(client);
                FlyingMachineEntity drone = activeMachine(client);
                // Sample AFTER entity interpolation, not one tick before it.
                if (drone != null && !drone.isRemoved()) updateCamera(client, drone);
            }
        });
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT,
            PotuzhnoLitMod.id("flight_hud"), FlightHud::render);
    }

    private static KeyMapping binding(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.potuzhnolit." + name,
            InputConstants.Type.KEYSYM, key, KeyMapping.Category.MISC));
    }
    public static boolean remote() { return droneId >= 0; }
    public static int remoteTicks() { return remoteTicks; }
    public static Vec3 anchor() { return bodyAnchor; }
    public static FlyingMachineEntity activeMachine(Minecraft client) {
        if (client.level == null || client.player == null) return null;
        if (remote()) return client.level.getEntity(droneId) instanceof FlyingMachineEntity machine ? machine : null;
        return client.player.getVehicle() instanceof FlyingMachineEntity machine ? machine : null;
    }

    private static void tickStart(Minecraft client) {
        if (client.player == null || client.level == null || client.getConnection() == null
                || !ClientPlayNetworking.canSend(ControlPayload.TYPE)) {
            endView(client);
            wasInteract = wasDetonate = wasEffects = wasJokes = false;
            return;
        }
        boolean interact = held(INTERACT), detonate = held(DETONATE);
        boolean effects = held(EFFECTS), jokes = held(JOKES);
        boolean interactClick = interact && !wasInteract;
        boolean detonateClick = detonate && !wasDetonate;
        boolean effectsClick = effects && !wasEffects;
        boolean jokesClick = jokes && !wasJokes;
        wasInteract = interact; wasDetonate = detonate; wasEffects = effects; wasJokes = jokes;
        FlyingMachineEntity machine = activeMachine(client);
        if (client.screen != null || !client.isWindowActive()) {
            if (remote()) { send(client, FlightControls.EXIT); endView(client); }
            else if (machine != null) send(client, 0);
            return;
        }
        boolean nearby = !client.level.getEntitiesOfClass(FlyingMachineEntity.class,
            client.player.getBoundingBox().inflate(3), e -> !e.isRemoved()).isEmpty();
        if (interactClick && (remote() || machine != null || nearby)) {
            clear(client.options.keyInventory);
            clear(INTERACT);
            send(client, FlightControls.INTERACT);
            if (remote()) { endView(client); return; }
            if (machine != null) return;
        }
        if (remote() && (machine == null || machine.isRemoved() || !client.player.isAlive())) {
            if (++missingTicks > 40 || !client.player.isAlive()) {
                send(client, FlightControls.EXIT);
                endView(client);
            } else {
                freezeBody(client);
                suppressBodyKeys(client);
                send(client, 0);
            }
            return;
        }
        missingTicks = 0;
        if (machine == null) { FlightHud.resetFlight(); return; }
        if (effectsClick) noiseEnabled = !noiseEnabled;
        if (jokesClick) { jokesEnabled = !jokesEnabled; FlightHud.clearJoke(); }
        int flags = 0;
        if (held(client.options.keyUp)) flags |= FlightControls.FORWARD;
        if (held(client.options.keyDown)) flags |= FlightControls.BACK;
        if (held(client.options.keyLeft)) flags |= FlightControls.LEFT;
        if (held(client.options.keyRight)) flags |= FlightControls.RIGHT;
        if (held(client.options.keyJump) || key(GLFW.GLFW_KEY_UP)) flags |= FlightControls.UP;
        if (held(DOWN) || key(GLFW.GLFW_KEY_DOWN) || (remote() && held(client.options.keyShift))) flags |= FlightControls.DOWN;
        if (!remote() && (held(GUN) || key(GLFW.GLFW_KEY_R) || mouse(GLFW.GLFW_MOUSE_BUTTON_LEFT))) flags |= FlightControls.FIRE;
        if (remote() && detonateClick) flags |= FlightControls.DETONATE;
        clear(client.options.keyAttack);
        clear(client.options.keyUse);
        clear(client.options.keyInventory);
        if (remote()) {
            remoteTicks++;
            freezeBody(client);
            suppressBodyKeys(client);
        }
        send(client, flags);
        FlightHud.tick(client);
    }

    private static void send(Minecraft client, int flags) {
        if (client.player != null && ClientPlayNetworking.canSend(ControlPayload.TYPE))
            ClientPlayNetworking.send(new ControlPayload(flags, client.player.getYRot(), client.player.getXRot()));
    }
    private static void beginView(Minecraft client, DroneViewPayload payload) {
        endView(client);
        if (payload.entityId() < 0 || client.player == null || client.level == null) return;
        droneId = payload.entityId();
        bodyAnchor = new Vec3(payload.x(), payload.y(), payload.z());
        previousNoGravity = client.player.isNoGravity();
        previousCamera = client.getCameraEntity();
        previousCameraType = client.options.getCameraType();
        remoteTicks = missingTicks = 0;
        freezeBody(client);
        suppressBodyKeys(client);
    }
    private static void freezeBody(Minecraft client) {
        if (bodyAnchor == null || client.player == null) return;
        client.player.setNoGravity(true);
        client.player.setDeltaMovement(Vec3.ZERO);
        client.player.setPos(bodyAnchor.x, bodyAnchor.y, bodyAnchor.z);
    }
    private static Vec3 cameraPosition(Vec3 position, float yaw) {
        double angle = Math.toRadians(yaw);
        return position.add(-Math.sin(angle) * 1.45, .65, Math.cos(angle) * 1.45);
    }
    private static void updateCamera(Minecraft client, FlyingMachineEntity drone) {
        boolean firstFrame = camera == null;
        if (firstFrame) camera = new DroneCamera(client);
        // Use the SAME old/current positions as the rendered machine. xo/yo/zo alone
        // are not the render interpolation history in 1.21.11: setOldPosAndRot is required.
        Vec3 current = cameraPosition(drone.position(), drone.getYRot());
        Vec3 previous = cameraPosition(drone.oldPosition(), drone.getYRot(0));
        if (firstFrame || current.distanceToSqr(previous) > 64) previous = current;
        float yaw = client.player.getYRot(), pitch = Math.clamp(client.player.getXRot(), -85f, 85f);
        camera.setPos(current.x, current.y, current.z);
        camera.setYRot(yaw); camera.setXRot(pitch);
        camera.setOldPosAndRot(previous, client.player.yRotO, client.player.xRotO);
        client.options.setCameraType(CameraType.FIRST_PERSON);
        if (client.getCameraEntity() != camera) client.setCameraEntity(camera);
    }

    /** Not spawned or ticked in the world. Mouse look remains responsive at render frame rate. */
    private static final class DroneCamera extends FlyingMachineEntity {
        private final Minecraft client;
        DroneCamera(Minecraft client) {
            super(PotuzhnoLitMod.MACHINE, client.level);
            this.client = client;
            noPhysics = true;
        }
        @Override public float getViewYRot(float partialTick) {
            return client.player == null ? super.getViewYRot(partialTick) : client.player.getViewYRot(partialTick);
        }
        @Override public float getViewXRot(float partialTick) {
            return client.player == null ? super.getViewXRot(partialTick)
                : Math.clamp(client.player.getViewXRot(partialTick), -85f, 85f);
        }
    }

    private static void endView(Minecraft client) {
        boolean wasRemote = remote();
        if (wasRemote && client.player != null) {
            client.player.setNoGravity(previousNoGravity);
            client.player.setDeltaMovement(Vec3.ZERO);
            Entity target = previousCamera != null && !previousCamera.isRemoved()
                && previousCamera.level() == client.level ? previousCamera : client.player;
            client.setCameraEntity(target);
            if (previousCameraType != null) client.options.setCameraType(previousCameraType);
            suppressBodyKeys(client);
        }
        droneId = -1;
        camera = null; previousCamera = null; previousCameraType = null; bodyAnchor = null;
        remoteTicks = missingTicks = 0;
        FlightHud.resetFlight();
    }
    private static void suppressBodyKeys(Minecraft client) {
        clear(client.options.keyUp); clear(client.options.keyDown);
        clear(client.options.keyLeft); clear(client.options.keyRight);
        clear(client.options.keyJump); clear(client.options.keyShift); clear(client.options.keySprint);
        clear(client.options.keyAttack); clear(client.options.keyUse);
    }
    private static void clear(KeyMapping mapping) {
        mapping.setDown(false);
        while (mapping.consumeClick()) { }
    }
    private static long window() { return GLFW.glfwGetCurrentContext(); }
    private static boolean key(int code) {
        long window = window();
        return window != 0 && code >= GLFW.GLFW_KEY_SPACE && code <= GLFW.GLFW_KEY_LAST
            && GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }
    private static boolean mouse(int button) {
        long window = window();
        return window != 0 && button >= 0 && button <= GLFW.GLFW_MOUSE_BUTTON_LAST
            && GLFW.glfwGetMouseButton(window, button) == GLFW.GLFW_PRESS;
    }
    private static boolean held(KeyMapping mapping) {
        InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(mapping);
        if (bound.getValue() < 0) return false;
        if (bound.getType() == InputConstants.Type.MOUSE) return mouse(bound.getValue());
        if (bound.getType() == InputConstants.Type.KEYSYM) return key(bound.getValue());
        for (int code = GLFW.GLFW_KEY_SPACE; code <= GLFW.GLFW_KEY_LAST; code++)
            if (GLFW.glfwGetKeyScancode(code) == bound.getValue()) return key(code);
        return false;
    }
}
