package dev.rsadvanced.feature.disk;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.rsadvanced.RSAdvanced;
import net.minecraft.resources.ResourceLocation;

public enum InfiniteStorageType implements StorageType {
    ITEM(DiskResourceKind.ITEM, "infinite_item"),
    FLUID(DiskResourceKind.FLUID, "infinite_fluid");

    private final DiskResourceKind kind;
    private final String id;

    InfiniteStorageType(DiskResourceKind kind, String id) {
        this.kind = kind;
        this.id = id;
    }

    public static InfiniteStorageType forKind(DiskResourceKind kind) {
        return kind == DiskResourceKind.ITEM ? ITEM : FLUID;
    }

    public static void register() {
        for (InfiniteStorageType type : values()) {
            RefinedStorageApi.INSTANCE.getStorageTypeRegistry().register(RSAdvanced.id(type.id), type);
        }
    }

    public InfiniteResourceStorage create(ResourceLocation definitionId, CellDefinition definition) {
        if (definition.kind() != kind) {
            throw new IllegalArgumentException("Cell " + definitionId + " is not " + kind);
        }
        return new InfiniteResourceStorage(definition.resourceKey(), this, definitionId);
    }

    @Override
    public SerializableStorage create(Long capacity, Runnable listener) {
        throw new IllegalArgumentException("An infinite cell requires a cell_definition ID");
    }

    @Override
    public MapCodec<SerializableStorage> getMapCodec(Runnable listener) {
        return ResourceLocation.CODEC.fieldOf("definition").flatXmap(definitionId -> {
            var definition = CellDefinitions.serverDefinition(definitionId);
            if (definition.isEmpty() || definition.get().kind() != kind) {
                return DataResult.error(() -> "Missing or incompatible infinite cell definition: " + definitionId);
            }
            return DataResult.success(create(definitionId, definition.get()));
        }, storage -> {
            if (!(storage instanceof InfiniteResourceStorage source) || source.getType() != this) {
                return DataResult.error(() -> "Cannot save an infinite cell without a matching definition");
            }
            return DataResult.success(source.definitionId());
        });
    }

    @Override
    public boolean isAllowed(ResourceKey resource) {
        return kind == DiskResourceKind.ITEM ? resource instanceof ItemResource : resource instanceof FluidResource;
    }

    @Override
    public long getDiskInterfaceTransferQuota(boolean stackUpgrade) {
        return kind.transferQuota(stackUpgrade);
    }
}
