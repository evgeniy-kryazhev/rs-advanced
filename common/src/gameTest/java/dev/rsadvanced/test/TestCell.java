package dev.rsadvanced.test;

import com.mojang.serialization.MapCodec;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.DiskResourceKind;
import dev.rsadvanced.feature.disk.InfiniteResourceStorage;
import dev.rsadvanced.feature.disk.InfiniteStorageType;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Tests exercise the loaded world catalog, including definitions supplied by the separate test mod. */
public record TestCell(ResourceLocation id, CellDefinition definition) {
    public static List<TestCell> values() {
        return CellDefinitions.serverEntries().stream()
                .map(entry -> new TestCell(entry.getKey(), entry.getValue())).toList();
    }

    public static TestCell named(String id) {
        ResourceLocation location = ResourceLocation.parse(id);
        return new TestCell(location, CellDefinitions.serverDefinition(location).orElseThrow());
    }

    public ResourceKey resource() {
        return definition.resourceKey();
    }

    public InfiniteResourceStorage create() {
        return InfiniteStorageType.forKind(definition.kind()).create(id, definition);
    }

    public InfiniteStorageType storageType() {
        return InfiniteStorageType.forKind(definition.kind());
    }

    public MapCodec<SerializableStorage> getMapCodec(Runnable listener) {
        return storageType().getMapCodec(listener);
    }

    public ItemStack stack() {
        return AdvancedContent.cell(id, definition.kind());
    }

    public Item returnItem() {
        if (definition.kind() == DiskResourceKind.ITEM) {
            return BuiltInRegistries.ITEM.get(definition.resource());
        }
        return BuiltInRegistries.FLUID.get(definition.resource()).getBucket();
    }
}
