package dev.rsadvanced.neoforge;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@Mod(RSAdvanced.MOD_ID)
public final class RSAdvancedNeoForge {
    public RSAdvancedNeoForge(IEventBus modBus) {
        modBus.addListener(this::registerCellRegistry);
        RSAdvanced.initialize();
    }
    private void registerCellRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CellDefinitions.REGISTRY_KEY,
                CellDefinition.CODEC,
                CellDefinition.CODEC);
    }
}
