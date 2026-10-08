package dev.rsadvanced.fabric;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import dev.rsadvanced.network.InfiniteGridMenu;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

public final class RSAdvancedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        InfiniteDiskItem cobblestoneDisk = Registry.register(
                BuiltInRegistries.ITEM, RSAdvanced.id(InfiniteDiskType.COBBLESTONE.itemName()),
                new InfiniteDiskItem(InfiniteDiskType.COBBLESTONE));
        InfiniteDiskItem waterDisk = Registry.register(
                BuiltInRegistries.ITEM, RSAdvanced.id(InfiniteDiskType.WATER.itemName()),
                new InfiniteDiskItem(InfiniteDiskType.WATER));

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, RSAdvanced.id("main"), FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.rsadvanced"))
                .icon(cobblestoneDisk::getDefaultInstance)
                .displayItems((parameters, output) -> {
                    output.accept(cobblestoneDisk);
                    output.accept(waterDisk);
                })
                .build());

        PayloadTypeRegistry.playS2C().register(InfiniteResourcesPayload.TYPE, InfiniteResourcesPayload.STREAM_CODEC);
        RSAdvanced.initialize(ServerPlayNetworking::send);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                if (player.containerMenu instanceof InfiniteGridMenu menu) {
                    menu.rsadvanced$synchronize(player);
                }
            }
        });
    }
}
