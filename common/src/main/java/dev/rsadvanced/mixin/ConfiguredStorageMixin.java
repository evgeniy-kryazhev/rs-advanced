package dev.rsadvanced.mixin;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.impl.storage.AbstractConfiguredProxyStorage;
import com.refinedmods.refinedstorage.api.network.impl.storage.StorageConfiguration;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.filter.FilterMode;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild.Amount;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import dev.rsadvanced.feature.disk.InfiniteSource;
import dev.rsadvanced.feature.disk.InfiniteSources;
import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractConfiguredProxyStorage.class, remap = false)
public abstract class ConfiguredStorageMixin implements InfiniteSource {
    @Shadow
    private Storage delegate;

    @Shadow
    @Final
    private StorageConfiguration config;

    @Override
    public Set<ResourceKey> rsadvanced$getInfiniteResources() {
        if (!config.isActive() || config.getAccessMode().isInsertOnly()) {
            return Set.of();
        }
        return InfiniteSources.collect(delegate);
    }

    @Override
    public boolean rsadvanced$isInfinite(ResourceKey resource) {
        return config.isActive()
                && !config.getAccessMode().isInsertOnly()
                && InfiniteSources.contains(delegate, resource);
    }

    @Inject(method = "compositeExtract", at = @At("HEAD"), cancellable = true, require = 1)
    private void rsadvanced$preserveInfiniteBalance(
            ResourceKey resource, long amount, Action action, Actor actor, CallbackInfoReturnable<Amount> callback) {
        if (!(delegate instanceof CompositeStorageImpl) || !InfiniteSources.contains(delegate, resource)) {
            return;
        }
        if (!config.isActive() || config.getAccessMode().isInsertOnly()) {
            callback.setReturnValue(Amount.ZERO);
            return;
        }

        long before = rsadvanced$getAmount(resource);
        long extracted = delegate.extract(resource, amount, action, actor);
        long cacheChange = action == Action.EXECUTE ? before - rsadvanced$getAmount(resource) : 0;
        // A drive can mix ordinary and infinite disks; only the finite portion changes its parent cache.
        callback.setReturnValue(new Amount(extracted, cacheChange));
    }

    @Unique
    private long rsadvanced$getAmount(ResourceKey resource) {
        for (ResourceAmount entry : delegate.getAll()) {
            if (entry.resource().equals(resource)) {
                return entry.amount();
            }
        }
        return 0;
    }

    @Inject(method = "compositeInsert", at = @At("HEAD"), cancellable = true, require = 1)
    private void rsadvanced$preserveInfiniteInsertionBalance(
            ResourceKey resource, long amount, Action action, Actor actor, CallbackInfoReturnable<Amount> callback) {
        if (!(delegate instanceof CompositeStorageImpl) || !InfiniteSources.contains(delegate, resource)) {
            return;
        }
        if (!config.isActive() || config.getAccessMode().isExtractOnly() || !config.isAllowed(resource)) {
            callback.setReturnValue(Amount.ZERO);
            return;
        }

        long before = rsadvanced$getAmount(resource);
        long inserted = delegate.insert(resource, amount, action, actor);
        long cacheChange = action == Action.EXECUTE ? rsadvanced$getAmount(resource) - before : 0;
        // Only resources kept by ordinary disks increase the parent cache.
        if (config.isVoidExcess() && config.getFilterMode() == FilterMode.ALLOW && inserted < amount) {
            inserted = amount;
        }
        callback.setReturnValue(new Amount(inserted, cacheChange));
    }
}
