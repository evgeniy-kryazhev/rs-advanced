package dev.rsadvanced.mixin;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import dev.rsadvanced.feature.disk.InfiniteResourceStorage;
import dev.rsadvanced.feature.disk.InfiniteSource;
import dev.rsadvanced.feature.disk.InfiniteSources;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = StateTrackedStorage.class, remap = false)
public abstract class StateTrackedStorageMixin implements CompositeAwareChild, InfiniteSource {
    @Shadow
    @Final
    private Storage delegate;

    @Override
    public Amount compositeExtract(ResourceKey resource, long amount, Action action, Actor actor) {
        long extracted = ((StateTrackedStorage) (Object) this).extract(resource, amount, action, actor);
        long cacheChange = delegate instanceof InfiniteResourceStorage ? 0 : extracted;
        return new Amount(extracted, cacheChange);
    }

    @Override
    public Amount compositeInsert(ResourceKey resource, long amount, Action action, Actor actor) {
        long inserted = ((StateTrackedStorage) (Object) this).insert(resource, amount, action, actor);
        return new Amount(inserted, inserted);
    }

    @Override
    public void onAddedIntoComposite(ParentComposite parent) {
        // StateTrackedStorage owns no independently changing resource list.
    }

    @Override
    public void onRemovedFromComposite(ParentComposite parent) {
    }

    @Override
    public boolean rsadvanced$isInfinite(ResourceKey resource) {
        return InfiniteSources.contains(delegate, resource);
    }
}
