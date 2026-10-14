package dev.rsadvanced.fabric;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.fabric.api.RefinedStorageFabricApi;
import com.refinedmods.refinedstorage.fabric.api.RefinedStoragePlugin;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.fabric.config.FabricConfig;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.InfiniteStorageBlockContent;
import dev.rsadvanced.feature.terminal.TerminalContent;
import dev.rsadvanced.feature.mining.MiningContent;
import com.refinedmods.refinedstorage.fabric.support.energy.EnergyStorageAdapter;
import team.reborn.energy.api.EnergyStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;

public final class RSAdvancedFabric implements ModInitializer, RefinedStoragePlugin {
    @Override
    public void onApiAvailable(RefinedStorageApi api) {
        // WirelessGridItem obtains its helpers in the constructor, after RS has initialized its public API.
        TerminalContent.register();
        MiningContent.register();
        EnergyStorage.ITEM.registerForItems(
                (stack, context) -> new EnergyStorageAdapter(TerminalContent.ITEM.get().createEnergyStorage(stack), context),
                TerminalContent.ITEM.get());
        EnergyStorage.ITEM.registerForItems(
                (stack, context) -> new EnergyStorageAdapter(MiningContent.ITEM.get().createEnergyStorage(stack), context),
                MiningContent.ITEM.get());
    }

    @Override
    public void onInitialize() {
        DynamicRegistries.registerSynced(
                CellDefinitions.REGISTRY_KEY,
                CellDefinition.CODEC);
        FabricConfig.register();
        RSAdvanced.initialize(FabricConfig::load);
        EnvExecutor.runInEnv(Env.CLIENT, () -> RSAdvancedFabricClient::initialize);
        LifecycleEvent.SETUP.register(() ->
                RefinedStorageFabricApi.INSTANCE
                        .getNetworkNodeContainerProviderLookup().registerForBlockEntity(
                                (anchor, side) -> anchor.getContainerProvider(),
                                AnchorContent.ENTITY.get()));
        LifecycleEvent.SETUP.register(() -> RefinedStorageFabricApi.INSTANCE
                .getNetworkNodeContainerProviderLookup().registerForBlockEntity(
                        (storageBlock, side) -> storageBlock.getContainerProvider(),
                        InfiniteStorageBlockContent.ENTITY.get()));
        ItemGroupEvents.MODIFY_ENTRIES_ALL.register((tab, entries) -> {
            // RS may initialize after this entrypoint; resolve its API only when tabs are built.
            var tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (!RefinedStorageApi.INSTANCE.getCreativeModeTabId().equals(tabId)) {
                return;
            }
            entries.accept(AdvancedContent.INFINITE_STORAGE_PART.get());
            entries.accept(AnchorContent.ITEM.get());
            entries.accept(TerminalContent.ITEM.get().createAtEnergyCapacity());
            entries.accept(MiningContent.ITEM.get().createAtEnergyCapacity());
            for (var cell : AdvancedContent.creativeCells(entries.getContext().holders())) {
                entries.accept(cell, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            for (var storageBlock : InfiniteStorageBlockContent.creativeBlocks(entries.getContext().holders())) {
                entries.accept(storageBlock, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
    }
}
