package dev.rsadvanced.feature.mining;

import dev.rsadvanced.RSAdvanced;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Only requests inspection of a held tool; the server resolves the actual stack. */
public record MiningConfigurationPayload(boolean offhand) implements CustomPacketPayload {
    public static final Type<MiningConfigurationPayload> TYPE = new Type<>(RSAdvanced.id("mining_configuration"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningConfigurationPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBoolean(payload.offhand()),
            buffer -> new MiningConfigurationPayload(buffer.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
