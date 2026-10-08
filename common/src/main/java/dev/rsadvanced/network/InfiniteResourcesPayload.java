package dev.rsadvanced.network;

import dev.rsadvanced.RSAdvanced;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record InfiniteResourcesPayload(int menuId, int flags) implements CustomPacketPayload {
    public static final Type<InfiniteResourcesPayload> TYPE = new Type<>(RSAdvanced.id("infinite_resources"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfiniteResourcesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, InfiniteResourcesPayload::menuId,
                    ByteBufCodecs.VAR_INT, InfiniteResourcesPayload::flags,
                    InfiniteResourcesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
