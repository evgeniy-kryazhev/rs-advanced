package dev.rsadvanced;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.config.RSAdvancedConfig;
import dev.rsadvanced.feature.AdvancedFeatures;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.feature.anchor.AnchorManager;
import dev.rsadvanced.feature.anchor.AnchorNetworkComponent;
import dev.rsadvanced.feature.anchor.AnchorNetworking;
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
        AnchorContent.register();
        LifecycleEvent.SERVER_STARTED.register(AnchorManager::start);
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            AnchorManager.stop(server);
            AnchorNetworking.clear();
        });
        TickEvent.SERVER_PRE.register(AnchorManager::tick);
        TickEvent.SERVER_LEVEL_PRE.register(AnchorManager::beforeLevelTick);
        TickEvent.SERVER_LEVEL_POST.register(AnchorManager::afterLevelTick);
        TickEvent.SERVER_POST.register(AnchorNetworking::tick);
        LifecycleEvent.SERVER_BEFORE_START.register(server -> {
            RSAdvancedConfig.load();
            CellDefinitions.startSession(server.registryAccess());
        });
        LifecycleEvent.SERVER_STOPPED.register(server ->
                CellDefinitions.endSession());
        AdvancedNetworking.register();
        LifecycleEvent.SETUP.register(() -> {
            AdvancedFeatures.initialize();
            RefinedStorageApi.INSTANCE.getNetworkComponentMapFactory()
                    .addFactory(AnchorNetworkComponent.class, AnchorNetworkComponent::new);
        });
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
