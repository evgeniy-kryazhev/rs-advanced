package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/** Suppliers are resolved after registration, never while the catalog initializes. */
public record DiskResource(DiskResourceKind kind, Supplier<ResourceKey> resourceFactory,
                           String translationKey, Supplier<Item> returnItem) {
    public DiskResource {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(resourceFactory);
        Objects.requireNonNull(translationKey);
        Objects.requireNonNull(returnItem);
    }

    public static DiskResource item(Supplier<Item> item, String translationKey) {
        return new DiskResource(DiskResourceKind.ITEM, () -> new ItemResource(item.get()), translationKey, item);
    }

    public static DiskResource fluid(Supplier<Fluid> fluid, String translationKey) {
        return new DiskResource(DiskResourceKind.FLUID, () -> new FluidResource(fluid.get()),
                translationKey, () -> fluid.get().getBucket());
    }

    public ResourceKey resource() {
        return resourceFactory.get();
    }
}
