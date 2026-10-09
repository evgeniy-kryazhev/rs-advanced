package dev.rsadvanced.feature.anchor;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.config.RSAdvancedConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AnchorStatePayload(int menuId, UUID owner, ResourceLocation dimension, BlockPos position,
        AnchorStatus status, int areaSize, int heldSize, long cost, boolean leader, boolean enabled,
        boolean visualizing, boolean visible, int visualizationDistance, Set<Long> chunks) implements CustomPacketPayload {
    public static final Type<AnchorStatePayload> TYPE = new Type<>(RSAdvanced.id("anchor_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AnchorStatePayload> CODEC = StreamCodec.of(
            AnchorStatePayload::write, AnchorStatePayload::read);

    public AnchorStatePayload {
        if (visualizationDistance < 1 || visualizationDistance > 4096) {
            throw new IllegalArgumentException("Invalid anchor visualization distance: " + visualizationDistance);
        }
        chunks = Set.copyOf(chunks);
    }

    public static AnchorStatePayload forMenu(int menuId, AnchorBlockEntity anchor, boolean visualizing) {
        return new AnchorStatePayload(menuId, anchor.instanceId(), anchor.getLevel().dimension().location(),
                anchor.getBlockPos(), anchor.status(), anchor.areaSize(), anchor.heldSize(), anchor.cost(),
                anchor.leader(), anchor.enabled(), visualizing, false,
                RSAdvancedConfig.get().anchorVisualizationDistance(), Set.of());
    }

    public static AnchorStatePayload forOverlay(AnchorBlockEntity anchor, boolean visualizing) {
        return forOverlay(anchor, visualizing, visualizing);
    }

    public static AnchorStatePayload forOverlay(AnchorBlockEntity anchor, boolean visualizing, boolean visible) {
        return new AnchorStatePayload(-1, anchor.instanceId(), anchor.getLevel().dimension().location(),
                anchor.getBlockPos(), anchor.status(), anchor.areaSize(), anchor.heldSize(), anchor.cost(),
                anchor.leader(), anchor.enabled(), visualizing, visible,
                RSAdvancedConfig.get().anchorVisualizationDistance(),
                visible ? AnchorManager.chunks(anchor) : Set.of());
    }

    private static void write(RegistryFriendlyByteBuf buffer, AnchorStatePayload payload) {
        buffer.writeVarInt(payload.menuId());
        buffer.writeUUID(payload.owner());
        buffer.writeResourceLocation(payload.dimension());
        buffer.writeBlockPos(payload.position());
        buffer.writeEnum(payload.status());
        buffer.writeVarInt(payload.areaSize());
        buffer.writeVarInt(payload.heldSize());
        buffer.writeVarLong(payload.cost());
        buffer.writeBoolean(payload.leader());
        buffer.writeBoolean(payload.enabled());
        buffer.writeBoolean(payload.visualizing());
        buffer.writeBoolean(payload.visible());
        buffer.writeVarInt(payload.visualizationDistance());
        buffer.writeVarInt(payload.chunks().size());
        for (long chunk : payload.chunks()) {
            buffer.writeLong(chunk);
        }
    }

    private static AnchorStatePayload read(RegistryFriendlyByteBuf buffer) {
        int menuId = buffer.readVarInt();
        UUID owner = buffer.readUUID();
        ResourceLocation dimension = buffer.readResourceLocation();
        BlockPos position = buffer.readBlockPos();
        AnchorStatus status = buffer.readEnum(AnchorStatus.class);
        int area = buffer.readVarInt();
        int held = buffer.readVarInt();
        long cost = buffer.readVarLong();
        boolean leader = buffer.readBoolean();
        boolean enabled = buffer.readBoolean();
        boolean visualizing = buffer.readBoolean();
        boolean visible = buffer.readBoolean();
        int visualizationDistance = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > 65536) {
            throw new IllegalArgumentException("Invalid anchor chunk count: " + count);
        }
        Set<Long> chunks = new HashSet<>();
        for (int index = 0; index < count; index++) {
            chunks.add(buffer.readLong());
        }
        return new AnchorStatePayload(menuId, owner, dimension, position, status, area, held, cost,
                leader, enabled, visualizing, visible, visualizationDistance, chunks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
