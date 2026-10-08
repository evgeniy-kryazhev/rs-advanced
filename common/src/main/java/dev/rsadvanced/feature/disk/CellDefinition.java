package dev.rsadvanced.feature.disk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

/** A world registry entry; resource registries are consulted only during decoding or use. */
public record CellDefinition(DiskResourceKind kind, ResourceLocation resource) {
    public static final Codec<CellDefinition> CODEC = RecordCodecBuilder.<CellDefinition>create(instance -> instance.group(
            DiskResourceKind.CODEC.fieldOf("kind").forGetter(CellDefinition::kind),
            ResourceLocation.CODEC.fieldOf("resource").forGetter(CellDefinition::resource)
    ).apply(instance, CellDefinition::new)).validate(CellDefinition::validate);

    private DataResult<CellDefinition> validate() {
        boolean exists;
        if (kind == DiskResourceKind.ITEM) {
            exists = BuiltInRegistries.ITEM.containsKey(resource)
                    && BuiltInRegistries.ITEM.get(resource) != Items.AIR;
        } else {
            exists = BuiltInRegistries.FLUID.containsKey(resource)
                    && BuiltInRegistries.FLUID.get(resource) != Fluids.EMPTY;
        }
        if (!exists) {
            return DataResult.error(() -> "Unknown or empty " + kind.getSerializedName() + " resource: " + resource);
        }
        return DataResult.success(this);
    }

    public ResourceKey resourceKey() {
        if (kind == DiskResourceKind.ITEM) {
            return new ItemResource(BuiltInRegistries.ITEM.get(resource));
        }
        return new FluidResource(BuiltInRegistries.FLUID.get(resource));
    }
}
