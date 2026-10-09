package dev.rsadvanced.fabric;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.fabric.api.RefinedStorageFabricApi;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;

public final class RSAdvancedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DynamicRegistries.registerSynced(
                CellDefinitions.REGISTRY_KEY,
                CellDefinition.CODEC);
        RSAdvanced.initialize();
        EnvExecutor.runInEnv(Env.CLIENT, () -> RSAdvancedFabricClient::initialize);
        LifecycleEvent.SETUP.register(() ->
                RefinedStorageFabricApi.INSTANCE
                        .getNetworkNodeContainerProviderLookup().registerForBlockEntity(
                                (anchor, side) -> anchor.getContainerProvider(),
                                AnchorContent.ENTITY.get()));
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((tab, entries) -> {
            // RS may initialize after this entrypoint; resolve its API only when tabs are built.
            var tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (!RefinedStorageApi.INSTANCE.getCreativeModeTabId().equals(tabId)) {
                return;
            }
            entries.accept(AdvancedContent.INFINITE_STORAGE_PART.get());
            entries.accept(AnchorContent.ITEM.get());
            for (var cell : AdvancedContent.creativeCells(entries.getContext().holders())) {
                entries.accept(cell, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
    }
}
