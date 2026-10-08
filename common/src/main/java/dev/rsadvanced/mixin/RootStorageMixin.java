package dev.rsadvanced.mixin;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import dev.rsadvanced.feature.disk.InfiniteSource;
import dev.rsadvanced.feature.disk.InfiniteSources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = RootStorageImpl.class, remap = false)
public abstract class RootStorageMixin implements InfiniteSource {
    @Shadow
    @Final
    protected CompositeStorageImpl storage;

    @Override
    public boolean rsadvanced$isInfinite(ResourceKey resource) {
        return InfiniteSources.contains(storage, resource);
    }
}
