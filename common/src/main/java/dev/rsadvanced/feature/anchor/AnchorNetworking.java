package dev.rsadvanced.feature.anchor;

import dev.architectury.networking.NetworkManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AnchorNetworking {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    private AnchorNetworking() {
    }

    public static boolean isVisualizing(ServerPlayer player, UUID anchor) {
        Selection selection = SELECTIONS.get(player.getUUID());
        return selection != null && selection.anchor.instanceId().equals(anchor);
    }

    public static void toggleVisualization(ServerPlayer player, AnchorBlockEntity anchor) {
        Selection previous = SELECTIONS.remove(player.getUUID());
        if (previous != null) {
            NetworkManager.sendToPlayer(player, AnchorStatePayload.forOverlay(previous.anchor, false));
        }
        if (previous == null || previous.anchor != anchor) {
            SELECTIONS.put(player.getUUID(), new Selection(anchor));
        }
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Selection>> iterator = SELECTIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Selection selection = entry.getValue();
            AnchorBlockEntity anchor = selection.anchor;
            boolean valid = player != null && !anchor.isRemoved() && anchor.enabled() && anchor.canOpen(player)
                    && player.level() == anchor.getLevel()
                    && player.distanceToSqr(anchor.getBlockPos().getCenter()) <= 64 * 64;
            if (!valid) {
                if (player != null) {
                    NetworkManager.sendToPlayer(player, AnchorStatePayload.forOverlay(anchor, false));
                }
                iterator.remove();
                continue;
            }
            AnchorStatePayload payload = AnchorStatePayload.forOverlay(anchor, true);
            if (!payload.equals(selection.lastSent)) {
                NetworkManager.sendToPlayer(player, payload);
                selection.lastSent = payload;
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof AnchorMenu menu) {
                menu.synchronize(player);
            }
        }
    }

    public static void clear() {
        SELECTIONS.clear();
    }

    private static final class Selection {
        private final AnchorBlockEntity anchor;
        private AnchorStatePayload lastSent;

        private Selection(AnchorBlockEntity anchor) {
            this.anchor = anchor;
        }
    }
}
