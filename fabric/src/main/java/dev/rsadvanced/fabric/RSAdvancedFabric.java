package dev.rsadvanced.fabric;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;

public final class RSAdvancedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DynamicRegistries.registerSynced(
                CellDefinitions.REGISTRY_KEY,
                CellDefinition.CODEC);
        RSAdvanced.initialize();
    }
}
