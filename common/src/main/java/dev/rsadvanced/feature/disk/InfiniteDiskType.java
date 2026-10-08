package dev.rsadvanced.feature.disk;

import com.mojang.serialization.MapCodec;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.rsadvanced.RSAdvanced;
import java.util.function.Supplier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

public enum InfiniteDiskType implements StorageType {
    COBBLESTONE("infinite_cobblestone_disk", 1, () -> new ItemResource(Items.COBBLESTONE)),
    WATER("infinite_water_disk", 2, () -> new FluidResource(Fluids.WATER));

    private final String itemName;
    private final int flag;
    private final Supplier<ResourceKey> resourceFactory;

    InfiniteDiskType(String itemName, int flag, Supplier<ResourceKey> resourceFactory) {
        this.itemName = itemName;
        this.flag = flag;
        this.resourceFactory = resourceFactory;
    }

    public String itemName() {
        return itemName;
    }

    public int flag() {
        return flag;
    }

    public ResourceKey resource() {
        return resourceFactory.get();
    }

    public static void registerStorageTypes() {
        for (InfiniteDiskType type : values()) {
            RefinedStorageApi.INSTANCE.getStorageTypeRegistry().register(RSAdvanced.id(type.itemName), type);
        }
    }

    @Override
    public InfiniteResourceStorage create(Long capacity, Runnable listener) {
        return new InfiniteResourceStorage(resource(), this);
    }

    @Override
    public MapCodec<SerializableStorage> getMapCodec(Runnable listener) {
        // The registered type is the entire persistent state; no generated stock is serialized.
        return MapCodec.unit(() -> create(null, listener));
    }

    @Override
    public boolean isAllowed(ResourceKey requestedResource) {
        return resource().equals(requestedResource);
    }

    @Override
    public long getDiskInterfaceTransferQuota(boolean stackUpgrade) {
        long unit = this == WATER ? Platform.INSTANCE.getBucketAmount() : 1;
        long multiplier = stackUpgrade ? (this == WATER ? 16 : 64) : 1;
        return unit * multiplier;
    }
}
