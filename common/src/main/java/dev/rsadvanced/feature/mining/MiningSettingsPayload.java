package dev.rsadvanced.feature.mining;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.config.MiningSettings;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MiningSettingsPayload(MiningSettings settings) implements CustomPacketPayload {
    public static final Type<MiningSettingsPayload> TYPE = new Type<>(RSAdvanced.id("mining_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningSettingsPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                MiningSettings settings = payload.settings();
                buffer.writeVarLong(settings.capacity());
                buffer.writeVarLong(settings.chargeRate());
                buffer.writeVarLong(settings.baseCost());
                buffer.writeVarLong(settings.weaponCost());
                buffer.writeVarInt(settings.veinLimit());
                buffer.writeBoolean(settings.veinEnabled());
            }, buffer -> new MiningSettingsPayload(new MiningSettings(buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readVarLong(), buffer.readVarLong(), buffer.readVarInt(), buffer.readBoolean())));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
