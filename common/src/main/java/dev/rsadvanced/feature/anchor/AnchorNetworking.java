package dev.rsadvanced.feature.anchor;

import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.config.RSAdvancedConfig;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AnchorNetworking {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    private AnchorNetworking() {
    }

    public static void requestRefresh(ServerPlayer player, UUID owner) {
        Selection selection = SELECTIONS.get(player.getUUID());
        if (selection != null && selection.anchor.instanceId().equals(owner)) {
            // The next tick rechecks permissions, dimension and distance before sending anything.
            selection.lastSent = null;
        }
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
        updateSelections(server, NetworkManager::sendToPlayer);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof AnchorMenu menu) {
                menu.synchronize(player);
            }
        }
    }

    private static void updateSelections(MinecraftServer server,
            BiConsumer<ServerPlayer, AnchorStatePayload> send) {
        Iterator<Map.Entry<UUID, Selection>> iterator = SELECTIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Selection selection = entry.getValue();
            AnchorBlockEntity anchor = selection.anchor;
            boolean valid = player != null && !anchor.isRemoved() && anchor.enabled() && anchor.canOpen(player)
                    && player.level() == anchor.getLevel();
            if (!valid) {
                if (player != null) {
                    send.accept(player, AnchorStatePayload.forOverlay(anchor, false));
                }
                iterator.remove();
                continue;
            }
            int distance = RSAdvancedConfig.get().anchorVisualizationDistance();
            boolean visible = player.distanceToSqr(anchor.getBlockPos().getCenter()) <= (double) distance * distance;
            if (!visible) {
                // Keep the selection, but send only one hide packet and avoid area snapshots while away.
                if (selection.lastSent == null || selection.lastSent.visible()) {
                    AnchorStatePayload hidden = AnchorStatePayload.forOverlay(anchor, true, false);
                    send.accept(player, hidden);
                    selection.lastSent = hidden;
                }
                continue;
            }
            AnchorStatePayload payload = AnchorStatePayload.forOverlay(anchor, true, true);
            if (!payload.equals(selection.lastSent)) {
                send.accept(player, payload);
                selection.lastSent = payload;
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
