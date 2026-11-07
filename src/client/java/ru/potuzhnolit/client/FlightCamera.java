package ru.potuzhnolit.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import ru.potuzhnolit.AimAngles;
import ru.potuzhnolit.FlightGeometry;
import ru.potuzhnolit.FlyingMachineEntity;

/** One mouse aim feeds the rendered camera, the flight packet and the server gun ray. */
public final class FlightCamera {
    private static CameraType previousPlaneType;
    private static FlyingMachineEntity aimMachine;
    private static final AimAngles AIM = new AimAngles();
    private static boolean wasPlaneAiming;
    private FlightCamera() { }
    public record Pose(Vec3 position, float yaw, float pitch) { }

    private static FlyingMachineEntity prepare(Minecraft client) {
        FlyingMachineEntity machine = PotuzhnoLitClient.activeMachine(client);
        if (machine == null || machine.isRemoved() || client.player == null || !client.player.isAlive()) return null;
        if (machine != aimMachine) {
            aimMachine = machine;
            AIM.reset(client.player.getYRot(), client.player.getXRot());
            wasPlaneAiming = false;
        }
        if (machine.getVariant() == 1 && !PotuzhnoLitClient.remote()) {
            boolean aiming = PotuzhnoLitClient.aimingPlane(client);
            if (wasPlaneAiming && !aiming) AIM.reset(machine.getYRot(), AIM.pitch());
            wasPlaneAiming = aiming;
        } else wasPlaneAiming = false;
        return machine;
    }

    /** Called by MouseHandler after vanilla sensitivity/invert-Y processing, every frame. */
    public static boolean turn(double horizontal, double vertical) {
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null || !client.isWindowActive() || prepare(client) == null) return false;
        AIM.turn(horizontal, vertical);
        return true;
    }

    public static float yaw(Minecraft client) {
        return prepare(client) == null ? (client.player == null ? 0 : client.player.getYRot()) : AIM.yaw();
    }
    public static float pitch(Minecraft client) {
        return prepare(client) == null ? (client.player == null ? 0 : client.player.getXRot()) : AIM.pitch();
    }

    public static void tick(Minecraft client) {
        FlyingMachineEntity machine = prepare(client);
        if (machine == null) { restore(client); return; }
        if (!PotuzhnoLitClient.remote() && machine.getVariant() == 1) {
            if (previousPlaneType == null) previousPlaneType = client.options.getCameraType();
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else restorePlaneType(client);
    }

    private static void restorePlaneType(Minecraft client) {
        if (previousPlaneType != null) client.options.setCameraType(previousPlaneType);
        previousPlaneType = null;
    }
    public static void restore(Minecraft client) {
        restorePlaneType(client);
        aimMachine = null;
        wasPlaneAiming = false;
    }

    public static Pose pose(Minecraft client, float partialTick) {
        FlyingMachineEntity machine = prepare(client);
        if (machine == null) return null;
        boolean drone = PotuzhnoLitClient.remote();
        if (!drone && (machine.getVariant() != 1 || client.getCameraEntity() != client.player)) return null;
        // Orbit the raised aircraft camera with the aim, not the delayed hull angle.
        Vec3 position = FlightGeometry.cameraPosition(machine, machine.getPosition(partialTick), AIM.yaw(), drone);
        return new Pose(position, AIM.yaw(), AIM.pitch());
    }
}
