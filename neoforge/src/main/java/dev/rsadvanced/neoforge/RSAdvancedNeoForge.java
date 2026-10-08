package dev.rsadvanced.neoforge;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@Mod(RSAdvanced.MOD_ID)
public final class RSAdvancedNeoForge {
    public RSAdvancedNeoForge(IEventBus modBus) {
        modBus.addListener(this::registerCellRegistry);
        modBus.addListener(this::appendCreativeCells);
        RSAdvanced.initialize();
    }

    private void appendCreativeCells(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(RefinedStorageApi.INSTANCE.getCreativeModeTabId())) {
            return;
        }
        for (var cell : AdvancedContent.creativeCells(event.getParameters().holders())) {
            event.accept(cell, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private void registerCellRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CellDefinitions.REGISTRY_KEY,
                CellDefinition.CODEC,
                CellDefinition.CODEC);
    }
}
