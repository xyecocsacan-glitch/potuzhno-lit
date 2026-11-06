package ru.potuzhnolit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A negative entity id closes the local camera. Never changes the server player's camera. */
public record DroneViewPayload(int entityId, double x, double y, double z) implements CustomPacketPayload {
    public static final Type<DroneViewPayload> TYPE = new Type<>(PotuzhnoLitMod.id("drone_view_v3"));
    public static final StreamCodec<FriendlyByteBuf, DroneViewPayload> CODEC = StreamCodec.of(
        (buffer, value) -> { buffer.writeVarInt(value.entityId()); buffer.writeDouble(value.x()); buffer.writeDouble(value.y()); buffer.writeDouble(value.z()); },
        buffer -> new DroneViewPayload(buffer.readVarInt(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
