package dev.rsadvanced.feature.anchor.fabric;

import java.util.Comparator;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

public final class AnchorTicketsImpl {
    private static final TicketType<UUID> ANCHOR = TicketType.create("rsadvanced:network_anchor",
            Comparator.comparing(UUID::toString));

    private AnchorTicketsImpl() {
    }

    public static void add(ServerLevel level, UUID owner, ChunkPos chunk) {
        // Region distance 2 gives ticket level 31: entity and block-entity ticking.
        level.getChunkSource().addRegionTicket(ANCHOR, chunk, 2, owner);
    }

    public static void remove(ServerLevel level, UUID owner, ChunkPos chunk) {
        level.getChunkSource().removeRegionTicket(ANCHOR, chunk, 2, owner);
    }
}
