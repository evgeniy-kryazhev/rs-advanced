package dev.rsadvanced.feature.disk;

import com.mojang.serialization.MapCodec;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import dev.rsadvanced.RSAdvanced;
import java.util.List;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

public enum InfiniteDiskType implements StorageType {
    COBBLESTONE("infinite_cobblestone_disk",
            DiskResource.item(() -> Items.COBBLESTONE, "block.minecraft.cobblestone"),
            "Infinite Cobblestone Disk", "Бесконечный диск булыжника",
            List.of("refinedstorage:storage_housing", "minecraft:water_bucket",
                    "minecraft:lava_bucket", "minecraft:cobblestone")),
    WATER("infinite_water_disk",
            DiskResource.fluid(() -> Fluids.WATER, "block.minecraft.water"),
            "Infinite Water Disk", "Бесконечный диск воды",
            List.of("refinedstorage:storage_housing", "minecraft:water_bucket", "minecraft:water_bucket"));

    static {
        if (values().length > 31) {
            throw new IllegalStateException("Infinite disk catalog supports at most 31 types for Grid flags");
        }
    }

    private final String itemName;
    private final DiskResource description;
    private final String englishName;
    private final String russianName;
    private final List<String> recipeIngredients;

    InfiniteDiskType(String itemName, DiskResource description, String englishName,
                     String russianName, List<String> recipeIngredients) {
        this.itemName = itemName;
        this.description = description;
        this.englishName = englishName;
        this.russianName = russianName;
        this.recipeIngredients = List.copyOf(recipeIngredients);
    }

    public String itemName() {
        return itemName;
    }

    public int flag() {
        // Append new entries: existing positions retain their wire flags.
        return 1 << ordinal();
    }

    public ResourceKey resource() {
        return description.resource();
    }

    public DiskResource description() {
        return description;
    }

    public String translationKey() {
        return "item.rsadvanced." + itemName;
    }

    public String englishName() {
        return englishName;
    }

    public String russianName() {
        return russianName;
    }

    public List<String> recipeIngredients() {
        return recipeIngredients;
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
        return description.kind().transferQuota(stackUpgrade);
    }
}
