package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import dev.rsadvanced.RSAdvanced;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record MiningSelectionPayload(SlotReference slot, int mode, int radius) implements CustomPacketPayload {
    public static final Type<MiningSelectionPayload> TYPE = new Type<>(RSAdvanced.id("mining_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningSelectionPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                SlotReferenceFactory.STREAM_CODEC.encode(buffer, payload.slot());
                buffer.writeVarInt(payload.mode());
                buffer.writeVarInt(payload.radius());
            }, buffer -> new MiningSelectionPayload(SlotReferenceFactory.STREAM_CODEC.decode(buffer),
                    buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
