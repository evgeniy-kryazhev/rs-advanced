package dev.rsadvanced.feature.mining;

import dev.rsadvanced.RSAdvanced;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MiningArcPayload(BlockPos from, BlockPos to) implements CustomPacketPayload {
    public static final Type<MiningArcPayload> TYPE = new Type<>(RSAdvanced.id("mining_arc"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningArcPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBlockPos(payload.from());
                buffer.writeBlockPos(payload.to());
            }, buffer -> new MiningArcPayload(buffer.readBlockPos(), buffer.readBlockPos()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
