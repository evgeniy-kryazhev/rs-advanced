package dev.rsadvanced;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.AdvancedFeatures;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.network.AdvancedNetworking;
import dev.rsadvanced.network.InfiniteGridMenu;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Shared Architectury bootstrap for every RS Advanced feature. */
public final class RSAdvanced {
    public static final String MOD_ID = "rsadvanced";

    private RSAdvanced() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void initialize() {
        AdvancedComponents.register();
        AdvancedContent.register();
        LifecycleEvent.SERVER_BEFORE_START.register(server ->
                CellDefinitions.startSession(server.registryAccess()));
        LifecycleEvent.SERVER_STOPPED.register(server ->
                CellDefinitions.endSession());
        AdvancedNetworking.register();
        LifecycleEvent.SETUP.register(AdvancedFeatures::initialize);
        TickEvent.SERVER_POST.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof InfiniteGridMenu menu) {
                    menu.rsadvanced$synchronize(player);
                }
            }
        });
    }

    public static void sendInfiniteResources(ServerPlayer player, InfiniteResourcesPayload payload) {
        NetworkManager.sendToPlayer(player, payload);
    }
}
