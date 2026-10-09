package dev.rsadvanced.feature.anchor.neoforge;

import dev.rsadvanced.RSAdvanced;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.TicketController;

public final class AnchorTicketsImpl {
    public static final TicketController CONTROLLER = new TicketController(RSAdvanced.id("network_anchor"),
            (level, helper) -> {
                // Our SavedData is authoritative. Remove native persisted tickets before bounded recovery
                // reissues validated owners; otherwise an expired bootstrap could leave immortal tickets.
                for (UUID owner : helper.getEntityTickets().keySet()) {
                    helper.removeAllTickets(owner);
                }
                for (var position : helper.getBlockTickets().keySet()) {
                    helper.removeAllTickets(position);
                }
            });

    private AnchorTicketsImpl() {
    }

    public static void add(ServerLevel level, UUID owner, ChunkPos chunk) {
        // Non-forced random ticking: the common recorder supplements vanilla ticks when needed.
        CONTROLLER.forceChunk(level, owner, chunk.x, chunk.z, true, false);
    }

    public static void remove(ServerLevel level, UUID owner, ChunkPos chunk) {
        CONTROLLER.forceChunk(level, owner, chunk.x, chunk.z, false, false);
    }
}
