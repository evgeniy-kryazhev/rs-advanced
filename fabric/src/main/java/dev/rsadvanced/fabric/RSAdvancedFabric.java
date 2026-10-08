package dev.rsadvanced.fabric;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public final class RSAdvancedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DynamicRegistries.registerSynced(
                CellDefinitions.REGISTRY_KEY,
                CellDefinition.CODEC);
        RSAdvanced.initialize();
        var storageTab = ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                RefinedStorageApi.INSTANCE.getCreativeModeTabId());
        ItemGroupEvents.modifyEntriesEvent(storageTab).register(entries -> {
            for (var cell : AdvancedContent.creativeCells(entries.getContext().holders())) {
                entries.accept(cell, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
    }
}
