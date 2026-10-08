package dev.rsadvanced.client;

import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.network.InfiniteResourcesPayload;

public final class RSAdvancedClient {
    private RSAdvancedClient() {
    }

    public static void initialize() {
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                InfiniteResourcesPayload.TYPE,
                InfiniteResourcesPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> InfiniteGridDisplay.receive(payload)));
    }
}
