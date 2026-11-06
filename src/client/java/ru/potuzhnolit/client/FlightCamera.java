package ru.potuzhnolit.client;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import ru.potuzhnolit.FlightGeometry;
import ru.potuzhnolit.FlyingMachineEntity;

/** Camera pose is sampled every rendered frame, not just on the 20 Hz client tick. */
public final class FlightCamera {
    private static CameraType previousPlaneType;
    private FlightCamera() { }
    public record Pose(Vec3 position, float yaw, float pitch) { }

    public static void tick(Minecraft client) {
        boolean plane = client.player != null && client.player.isAlive() && !PotuzhnoLitClient.remote()
            && client.player.getVehicle() instanceof FlyingMachineEntity machine && machine.getVariant() == 1;
        if (plane) {
            if (previousPlaneType == null) previousPlaneType = client.options.getCameraType();
            // Render the pilot and aircraft below the raised camera.
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        } else restore(client);
    }

    public static void restore(Minecraft client) {
        if (previousPlaneType != null) client.options.setCameraType(previousPlaneType);
        previousPlaneType = null;
    }

    public static Pose pose(Minecraft client, float partialTick) {
        FlyingMachineEntity machine = PotuzhnoLitClient.activeMachine(client);
        if (machine == null || machine.isRemoved() || client.player == null || !client.player.isAlive()) return null;
        boolean drone = PotuzhnoLitClient.remote();
        if (!drone && (machine.getVariant() != 1 || client.getCameraEntity() != client.player)) return null;
        float yaw = client.player.getYRot();
        float pitch = Mth.clamp(client.player.getXRot(), -85, 85);
        float hullYaw = Mth.rotLerp(partialTick, machine.yRotO, machine.getYRot());
        Vec3 position = FlightGeometry.cameraPosition(machine, machine.getPosition(partialTick),
            drone ? yaw : hullYaw, drone);
        return new Pose(position, yaw, pitch);
    }
}
