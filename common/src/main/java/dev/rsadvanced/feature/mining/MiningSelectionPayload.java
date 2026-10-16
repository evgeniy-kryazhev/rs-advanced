package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import dev.rsadvanced.RSAdvanced;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MiningSelectionPayload(SlotReference slot, ResourceLocation module, String option) implements CustomPacketPayload {
    public static final Type<MiningSelectionPayload> TYPE = new Type<>(RSAdvanced.id("mining_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningSelectionPayload> CODEC = StreamCodec.of(
            (buffer, payload) -> {
                SlotReferenceFactory.STREAM_CODEC.encode(buffer, payload.slot());
                buffer.writeResourceLocation(payload.module());
                buffer.writeUtf(payload.option(), 64);
            }, buffer -> new MiningSelectionPayload(SlotReferenceFactory.STREAM_CODEC.decode(buffer),
                    buffer.readResourceLocation(), buffer.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
