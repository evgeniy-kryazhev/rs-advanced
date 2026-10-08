package dev.rsadvanced.client;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
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

    public static void registerDiskModels() {
        for (InfiniteDiskType type : InfiniteDiskType.values()) {
            RefinedStorageClientApi.INSTANCE.registerDiskModel(AdvancedContent.disk(type).get(),
                    type.description().kind().diskModel());
        }
    }
}
