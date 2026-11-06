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
    public static KeyMapping DOWN, GUN, INTERACT, DETONATE, EFFECTS;
    public static boolean noiseEnabled = true;
    private static int droneId = -1, missingTicks, remoteTicks;
    private static FlyingMachineEntity camera;
    private static Entity previousCamera;
    private static CameraType previousCameraType;
    private static Vec3 bodyAnchor;
    private static boolean previousNoGravity;
    private static boolean wasInteract, wasDetonate, wasEffects;

    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(PotuzhnoLitMod.MACHINE, MachineRenderer::new);
        INTERACT = binding("interact", GLFW.GLFW_KEY_E);
        DOWN = binding("down", GLFW.GLFW_KEY_X);
        GUN = binding("fire", GLFW.GLFW_KEY_R);
        DETONATE = binding("detonate", GLFW.GLFW_KEY_G);
        EFFECTS = binding("effects", GLFW.GLFW_KEY_V);
        ClientPlayNetworking.registerGlobalReceiver(DroneViewPayload.TYPE, (payload, context) ->
            context.client().execute(() -> beginView(context.client(), payload)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> endView(client));
        ClientTickEvents.START_CLIENT_TICK.register(PotuzhnoLitClient::tickStart);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (remote() && client.player != null) {
                freezeBody(client);
                FlyingMachineEntity drone = activeMachine(client);
                if (drone != null && !drone.isRemoved()) updateCameraProxy(client, drone);
            }
            FlightCamera.tick(client);
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
            wasInteract = wasDetonate = wasEffects = false;
            return;
        }
        boolean interact = held(INTERACT), detonate = held(DETONATE), effects = held(EFFECTS);
        boolean interactClick = interact && !wasInteract;
        boolean detonateClick = detonate && !wasDetonate;
        boolean effectsClick = effects && !wasEffects;
        wasInteract = interact; wasDetonate = detonate; wasEffects = effects;
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
        if (machine == null) return;
        if (effectsClick) noiseEnabled = !noiseEnabled;
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
    }

    private static void send(Minecraft client, int flags) {
        if (client.player != null && ClientPlayNetworking.canSend(ControlPayload.TYPE))
            ClientPlayNetworking.send(new ControlPayload(flags, FlightCamera.yaw(client), FlightCamera.pitch(client)));
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
    private static void updateCameraProxy(Minecraft client, FlyingMachineEntity drone) {
        boolean firstFrame = camera == null;
        if (firstFrame) {
            camera = new FlyingMachineEntity(PotuzhnoLitMod.MACHINE, client.level);
            camera.noPhysics = true;
        }
        camera.xOld = camera.xo = camera.getX();
        camera.yOld = camera.yo = camera.getY();
        camera.zOld = camera.zo = camera.getZ();
        camera.yRotO = camera.getYRot(); camera.xRotO = camera.getXRot();
        float yaw = FlightCamera.yaw(client), pitch = FlightCamera.pitch(client);
        camera.setPos(drone.getX(), drone.getY() + .65, drone.getZ());
        camera.setYRot(yaw); camera.setXRot(pitch);
        if (firstFrame) {
            camera.xOld = camera.xo = camera.getX();
            camera.yOld = camera.yo = camera.getY();
            camera.zOld = camera.zo = camera.getZ();
            camera.yRotO = yaw; camera.xRotO = pitch;
        }
        client.options.setCameraType(CameraType.FIRST_PERSON);
        if (client.getCameraEntity() != camera) client.setCameraEntity(camera);
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
        FlightCamera.restore(client);
        droneId = -1;
        camera = null; previousCamera = null; previousCameraType = null; bodyAnchor = null;
        remoteTicks = missingTicks = 0;
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
