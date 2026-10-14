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
import dev.rsadvanced.neoforge.config.NeoForgeConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.InfiniteStorageBlockContent;
import dev.rsadvanced.feature.terminal.TerminalContent;
import com.refinedmods.refinedstorage.neoforge.support.energy.EnergyStorageAdapter;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@Mod(RSAdvanced.MOD_ID)
public final class RSAdvancedNeoForge {
    public RSAdvancedNeoForge(IEventBus modBus, ModContainer modContainer) {
        var config = new NeoForgeConfig();
        modContainer.registerConfig(ModConfig.Type.COMMON, config.spec());
        modBus.addListener(this::registerCellRegistry);
        modBus.addListener(this::appendCreativeCells);
        modBus.addListener(this::registerAnchorCapability);
        modBus.addListener(this::registerAnchorTickets);
        RSAdvanced.initialize(config::snapshot);
        TerminalContent.register();
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> RSAdvancedNeoForgeClient.initialize(modBus, modContainer));
    }

    private void appendCreativeCells(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().location().equals(RefinedStorageApi.INSTANCE.getCreativeModeTabId())) {
            return;
        }
        event.accept(AdvancedContent.INFINITE_STORAGE_PART.get());
        event.accept(AnchorContent.ITEM.get());
        event.accept(TerminalContent.ITEM.get().createAtEnergyCapacity());
        for (var cell : AdvancedContent.creativeCells(event.getParameters().holders())) {
            event.accept(cell, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
        for (var storageBlock : InfiniteStorageBlockContent.creativeBlocks(event.getParameters().holders())) {
            event.accept(storageBlock, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    private void registerCellRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CellDefinitions.REGISTRY_KEY,
                CellDefinition.CODEC,
                CellDefinition.CODEC);
    }

    private void registerAnchorCapability(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new EnergyStorageAdapter(TerminalContent.ITEM.get().createEnergyStorage(stack)),
                TerminalContent.ITEM.get());
        event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE
                        .getNetworkNodeContainerProviderCapability(),
                AnchorContent.ENTITY.get(),
                (anchor, side) -> anchor.getContainerProvider());
        event.registerBlockEntity(RefinedStorageNeoForgeApi.INSTANCE
                        .getNetworkNodeContainerProviderCapability(),
                InfiniteStorageBlockContent.ENTITY.get(),
                (storageBlock, side) -> storageBlock.getContainerProvider());
    }

    private void registerAnchorTickets(RegisterTicketControllersEvent event) {
        event.register(AnchorTicketsImpl.CONTROLLER);
    }
}
