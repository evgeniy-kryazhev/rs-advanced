package dev.rsadvanced.client;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import net.minecraft.resources.ResourceLocation;

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
        RefinedStorageClientApi.INSTANCE.registerDiskModel(AdvancedContent.COBBLESTONE_DISK.get(),
                ResourceLocation.fromNamespaceAndPath("refinedstorage", "block/disk/disk"));
        RefinedStorageClientApi.INSTANCE.registerDiskModel(AdvancedContent.WATER_DISK.get(),
                ResourceLocation.fromNamespaceAndPath("refinedstorage", "block/disk/fluid_disk"));
    }
}
