package dev.rsadvanced.fabric;

import dev.rsadvanced.client.InfiniteGridDisplay;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class RSAdvancedFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(InfiniteResourcesPayload.TYPE,
                (payload, context) -> context.client().execute(() -> InfiniteGridDisplay.receive(payload)));
    }
}
