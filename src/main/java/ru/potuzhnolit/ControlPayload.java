package ru.potuzhnolit;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ControlPayload(int flags) implements CustomPacketPayload {
    public static final Type<ControlPayload> TYPE = new Type<>(PotuzhnoLitMod.id("control"));
    public static final StreamCodec<FriendlyByteBuf, ControlPayload> CODEC = StreamCodec.of(
        (buffer, payload) -> buffer.writeVarInt(payload.flags()),
        buffer -> new ControlPayload(buffer.readVarInt())
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
