package ru.potuzhnolit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** All session state and flight authority live on the server thread. */
public final class RemoteControl {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private RemoteControl() { }

    private static final class Session {
        final ServerPlayer player;
        final FlyingMachineEntity machine;
        final Vec3 anchor;
        final boolean previousNoGravity;
        int lastPacket;
        Session(ServerPlayer player, FlyingMachineEntity machine) {
            this.player = player;
            this.machine = machine;
            this.anchor = player.position();
            this.previousNoGravity = player.isNoGravity();
            this.lastPacket = player.tickCount;
        }
    }

    public static void handle(ServerPlayer player, ControlPayload payload) {
        if (!Float.isFinite(payload.yaw()) || !Float.isFinite(payload.pitch())) return;
        int flags = payload.flags();
        if ((flags & FlightControls.EXIT) != 0) { stop(player, true); return; }
        if ((flags & FlightControls.INTERACT) != 0) {
            if (SESSIONS.containsKey(player.getUUID())) { stop(player, true); return; }
            if (player.getVehicle() instanceof FlyingMachineEntity) { player.stopRiding(); return; }
            player.level().getEntitiesOfClass(FlyingMachineEntity.class,
                player.getBoundingBox().inflate(3), machine -> machine.available() && visible(player, machine))
                .stream().min(java.util.Comparator.comparingDouble(machine -> machine.distanceToSqr(player)))
                .ifPresent(machine -> use(player, machine));
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session != null) {
            if (!valid(session)) { stop(player, true); return; }
            session.lastPacket = player.tickCount;
            if (FlightControls.mayDetonate(flags, session.machine.getVariant() == 0, true)) {
                session.machine.detonate();
                return;
            }
            session.machine.applyControls(flags, payload.yaw(), payload.pitch());
        } else if (player.getVehicle() instanceof FlyingMachineEntity machine
                && machine.getVariant() == 1 && machine.getFirstPassenger() == player) {
            machine.applyControls(flags, payload.yaw(), payload.pitch());
        }
    }

    public static void use(ServerPlayer player, FlyingMachineEntity machine) {
        if (!player.isAlive() || player.isSpectator() || machine.isRemoved()
                || player.level() != machine.level() || player.distanceToSqr(machine) > 16) return;
        Session old = SESSIONS.get(player.getUUID());
        if (old != null) { stop(player, true); return; }
        if (!machine.available() || player.isPassenger() || !visible(player, machine)) return;
        if (machine.getVariant() == 1) { player.startRiding(machine); return; }
        if (!ServerPlayNetworking.canSend(player, DroneViewPayload.TYPE)) return;
        Session session = new Session(player, machine);
        SESSIONS.put(player.getUUID(), session);
        machine.remoteOwner = player.getUUID();
        machine.applyControls(0, player.getYRot(), player.getXRot());
        player.setDeltaMovement(Vec3.ZERO);
        player.setNoGravity(true);
        // ServerPlayer.setCamera would relocate the body. Only our client receives a view id.
        ServerPlayNetworking.send(player, new DroneViewPayload(machine.getId(),
            session.anchor.x, session.anchor.y, session.anchor.z));
        player.displayClientMessage(Component.translatable("message.potuzhnolit.connected"), true);
    }

    private static boolean visible(ServerPlayer player, FlyingMachineEntity machine) {
        return player.level().clip(new ClipContext(player.getEyePosition(),
            machine.position().add(0, .45, 0), ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private static boolean valid(Session session) {
        ServerPlayer player = session.player;
        return player.isAlive() && !player.isSpectator() && !player.isPassenger()
            && !session.machine.isRemoved() && session.machine.level() == player.level()
            && player.hurtTime == 0 && player.position().distanceToSqr(session.anchor) < 2.25;
    }

    public static void tick(MinecraftServer server) {
        for (Session session : new ArrayList<>(SESSIONS.values())) {
            if (!valid(session) || session.player.tickCount - session.lastPacket > 100) {
                stop(session.player, true);
                continue;
            }
            session.player.setDeltaMovement(Vec3.ZERO);
            session.player.setPos(session.anchor.x, session.anchor.y, session.anchor.z);
        }
    }

    public static ServerPlayer pilot(FlyingMachineEntity machine) {
        if (machine.remoteOwner == null) return null;
        Session session = SESSIONS.get(machine.remoteOwner);
        return session != null && session.machine == machine && valid(session) ? session.player : null;
    }

    public static boolean withinSignal(FlyingMachineEntity machine, Vec3 destination) {
        Session session = machine.remoteOwner == null ? null : SESSIONS.get(machine.remoteOwner);
        return session != null && session.machine == machine
            && session.anchor.distanceToSqr(destination) <= FlightControls.REMOTE_RANGE * FlightControls.REMOTE_RANGE
            && machine.level().hasChunkAt(BlockPos.containing(destination));
    }

    public static void stop(ServerPlayer player, boolean notify) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null) return;
        session.machine.remoteOwner = null;
        session.machine.applyControls(0, session.machine.getYRot(), session.machine.getXRot());
        session.machine.setDeltaMovement(Vec3.ZERO);
        player.setNoGravity(session.previousNoGravity);
        player.setDeltaMovement(Vec3.ZERO);
        if (notify && ServerPlayNetworking.canSend(player, DroneViewPayload.TYPE))
            ServerPlayNetworking.send(player, new DroneViewPayload(-1, 0, 0, 0));
    }

    public static void removed(FlyingMachineEntity machine) {
        for (Session session : new ArrayList<>(SESSIONS.values()))
            if (session.machine == machine) stop(session.player, true);
    }

    public static void clear() {
        for (Session session : new ArrayList<>(SESSIONS.values())) stop(session.player, false);
        SESSIONS.clear();
    }
}
