package dev.rsadvanced.feature.disk;

import dev.rsadvanced.RSAdvanced;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class CellDefinitions {
    public static final ResourceKey<Registry<CellDefinition>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(RSAdvanced.id("infinite_cell"));

    private static volatile RegistryAccess serverRegistries = RegistryAccess.EMPTY;
    private static volatile Function<ResourceLocation, Optional<CellDefinition>> displayLookup =
            CellDefinitions::serverDefinition;

    private CellDefinitions() {
    }

    public static void startSession(RegistryAccess registries) {
        // These world registries are frozen. Recipe reloads must not replace the cell catalog.
        serverRegistries = registries;
    }

    public static void endSession() {
        serverRegistries = RegistryAccess.EMPTY;
    }

    public static void setDisplayLookup(Function<ResourceLocation, Optional<CellDefinition>> lookup) {
        displayLookup = lookup;
    }

    public static Optional<CellDefinition> serverDefinition(ResourceLocation id) {
        return definition(serverRegistries, id);
    }

    public static Optional<CellDefinition> displayDefinition(ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return displayLookup.apply(id);
    }

    public static Optional<CellDefinition> definition(RegistryAccess registries, ResourceLocation id) {
        if (id == null) {
            return Optional.empty();
        }
        return registries.registry(REGISTRY_KEY).flatMap(registry -> registry.getOptional(id));
    }

    public static List<Map.Entry<ResourceLocation, CellDefinition>> entries(HolderLookup.Provider registries) {
        return registries.lookup(REGISTRY_KEY).map(lookup -> lookup.listElements()
                .map(holder -> Map.entry(holder.key().location(), holder.value()))
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList()).orElse(List.of());
    }

    public static List<Map.Entry<ResourceLocation, CellDefinition>> serverEntries() {
        return entries(serverRegistries);
    }
}
