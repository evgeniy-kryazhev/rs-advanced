package dev.rsadvanced.neoforge;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.feature.anchor.neoforge.AnchorTicketsImpl;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
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
        modBus.addListener(this::registerAnchorCapability);
        modBus.addListener(this::registerAnchorTickets);
        RSAdvanced.initialize();
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> RSAdvancedNeoForgeClient.initialize(modBus));
    }

    private void appendCreativeCells(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(RefinedStorageApi.INSTANCE.getCreativeModeTabId())) {
            return;
        }
        event.accept(AdvancedContent.INFINITE_STORAGE_PART.get());
        event.accept(AnchorContent.ITEM.get());
        for (var cell : AdvancedContent.creativeCells(event.getParameters().holders())) {
            event.accept(cell, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private void registerCellRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CellDefinitions.REGISTRY_KEY,
                CellDefinition.CODEC,
                CellDefinition.CODEC);
    }

    private void registerAnchorCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE
                        .getNetworkNodeContainerProviderCapability(),
                AnchorContent.ENTITY.get(),
                (anchor, side) -> anchor.getContainerProvider());
    }

    private void registerAnchorTickets(RegisterTicketControllersEvent event) {
        event.register(AnchorTicketsImpl.CONTROLLER);
    }
}
