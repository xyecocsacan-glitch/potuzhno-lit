package ru.potuzhnolit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Versioned channel: older clients cannot send an incompatible packet layout. */
public record ControlPayload(int flags, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<ControlPayload> TYPE = new Type<>(PotuzhnoLitMod.id("control_v3"));
    public static final StreamCodec<FriendlyByteBuf, ControlPayload> CODEC = StreamCodec.of(
        (buffer, value) -> { buffer.writeVarInt(value.flags()); buffer.writeFloat(value.yaw()); buffer.writeFloat(value.pitch()); },
        buffer -> new ControlPayload(buffer.readVarInt(), buffer.readFloat(), buffer.readFloat())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
