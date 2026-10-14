package dev.rsadvanced.feature.terminal;

import dev.rsadvanced.feature.connection.NetworkCardItemContext;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** RS's wireless grid stays open when disconnected; terminal sessions close on a lost card route. */
final class TerminalSessions {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private TerminalSessions() {
    }

    static void track(ServerPlayer player, NetworkCardItemContext context) {
        SESSIONS.put(player.getUUID(), new Session(player.containerMenu, context));
    }

    static void clear() {
        SESSIONS.clear();
    }

    static void tick(MinecraftServer server) {
        SESSIONS.entrySet().removeIf(entry -> {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Session session = entry.getValue();
            if (player == null || player.containerMenu != session.menu()) {
                return true;
            }
            if (!session.context().isActive()) {
                player.closeContainer();
                return true;
            }
            return false;
        });
    }

    private record Session(AbstractContainerMenu menu, NetworkCardItemContext context) {
    }
}
