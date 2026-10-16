package dev.rsadvanced.feature.mining;

import dev.rsadvanced.RSAdvanced;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MiningConnectionPayload(boolean offhand, boolean connected) implements CustomPacketPayload {
    public static final Type<MiningConnectionPayload> TYPE = new Type<>(RSAdvanced.id("mining_connection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningConnectionPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.offhand());
                buffer.writeBoolean(payload.connected());
            }, buffer -> new MiningConnectionPayload(buffer.readBoolean(), buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
