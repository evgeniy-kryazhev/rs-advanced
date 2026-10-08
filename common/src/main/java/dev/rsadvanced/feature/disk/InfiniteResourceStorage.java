package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeAwareChild;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class InfiniteResourceStorage implements SerializableStorage, CompositeAwareChild, InfiniteSource {
    public static final long ADVERTISED_AMOUNT = 1_000_000_000L;

    private final ResourceKey resource;
    private final StorageType type;
    private final List<ResourceAmount> contents;

    public InfiniteResourceStorage(ResourceKey resource, StorageType type) {
        this.resource = Objects.requireNonNull(resource);
        this.type = Objects.requireNonNull(type);
        this.contents = List.of(new ResourceAmount(resource, ADVERTISED_AMOUNT));
    }

    @Override
    public long extract(ResourceKey requestedResource, long amount, Action action, Actor actor) {
        ResourceAmount.validate(requestedResource, amount);
        Objects.requireNonNull(action);
        Objects.requireNonNull(actor);
        return resource.equals(requestedResource) ? amount : 0;
    }

    @Override
    public long insert(ResourceKey requestedResource, long amount, Action action, Actor actor) {
        ResourceAmount.validate(requestedResource, amount);
        Objects.requireNonNull(action);
        Objects.requireNonNull(actor);
        return 0;
    }

    @Override
    public Amount compositeExtract(ResourceKey requestedResource, long amount, Action action, Actor actor) {
        long extracted = extract(requestedResource, amount, action, actor);
        // Extraction succeeds, but there is no finite balance to subtract from the composite cache.
        return new Amount(extracted, 0);
    }

    @Override
    public Amount compositeInsert(ResourceKey requestedResource, long amount, Action action, Actor actor) {
        insert(requestedResource, amount, action, actor);
        return Amount.ZERO;
    }

    @Override
    public void onAddedIntoComposite(ParentComposite parent) {
        // Stateless sources never change their advertised contents independently of the parent.
    }

    @Override
    public void onRemovedFromComposite(ParentComposite parent) {
    }

    @Override
    public boolean rsadvanced$isInfinite(ResourceKey requestedResource) {
        return resource.equals(requestedResource);
    }

    @Override
    public Collection<ResourceAmount> getAll() {
        return contents;
    }

    @Override
    public long getStored() {
        return ADVERTISED_AMOUNT;
    }

    @Override
    public StorageType getType() {
        return type;
    }
}
