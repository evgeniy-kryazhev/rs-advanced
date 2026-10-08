package dev.rsadvanced.network;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import dev.rsadvanced.RSAdvanced;
import io.netty.handler.codec.DecoderException;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record InfiniteResourcesPayload(int menuId, Set<ResourceKey> resources) implements CustomPacketPayload {
    public static final Type<InfiniteResourcesPayload> TYPE = new Type<>(RSAdvanced.id("infinite_resources"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InfiniteResourcesPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.menuId());
                buffer.writeVarInt(payload.resources().size());
                for (ResourceKey resource : payload.resources()) {
                    ResourceCodecs.STREAM_CODEC.encode(buffer, (PlatformResourceKey) resource);
                }
            }, buffer -> {
                int menuId = buffer.readVarInt();
                int size = buffer.readVarInt();
                if (size < 0 || size > 65536) {
                    throw new DecoderException("Invalid infinite resource count: " + size);
                }
                Set<ResourceKey> resources = new HashSet<>();
                for (int index = 0; index < size; index++) {
                    resources.add(ResourceCodecs.STREAM_CODEC.decode(buffer));
                }
                return new InfiniteResourcesPayload(menuId, resources);
            });

    public InfiniteResourcesPayload {
        resources = Set.copyOf(resources);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
