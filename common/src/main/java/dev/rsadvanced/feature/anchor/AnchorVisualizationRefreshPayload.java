package dev.rsadvanced.feature.anchor;

import dev.rsadvanced.RSAdvanced;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Refreshes an existing selection; cannot select an anchor or acquire chunk tickets. */
public record AnchorVisualizationRefreshPayload(UUID owner) implements CustomPacketPayload {
    public static final Type<AnchorVisualizationRefreshPayload> TYPE =
            new Type<>(RSAdvanced.id("anchor_visualization_refresh"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorVisualizationRefreshPayload> CODEC =
            StreamCodec.of((buffer, payload) -> buffer.writeUUID(payload.owner()),
                    buffer -> new AnchorVisualizationRefreshPayload(buffer.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
