package dev.rsadvanced.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.rsadvanced.client.RSAdvancedClient;
import dev.rsadvanced.feature.anchor.AnchorNetworking;
import dev.rsadvanced.feature.anchor.AnchorStatePayload;
import dev.rsadvanced.feature.anchor.AnchorVisualizationRefreshPayload;
import net.minecraft.server.level.ServerPlayer;

public final class AdvancedNetworking {
    private AdvancedNetworking() {
    }

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, AnchorVisualizationRefreshPayload.TYPE,
                AnchorVisualizationRefreshPayload.CODEC, (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        AnchorNetworking.requestRefresh(player, payload.owner());
                    }
                }));
        // The client receiver also registers its codec; dedicated servers only need the payload type.
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(InfiniteResourcesPayload.TYPE, InfiniteResourcesPayload.STREAM_CODEC);
            NetworkManager.registerS2CPayloadType(AnchorStatePayload.TYPE, AnchorStatePayload.CODEC);
        }
        EnvExecutor.runInEnv(Env.CLIENT, () -> RSAdvancedClient::initialize);
    }
}
