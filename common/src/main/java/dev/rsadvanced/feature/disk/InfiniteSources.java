package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import java.util.HashSet;
import java.util.Set;

public final class InfiniteSources {
    private InfiniteSources() {
    }

    public static Set<ResourceKey> collect(Storage storage) {
        // Traverse source metadata, rather than scanning every finite resource in the network each tick.
        if (storage instanceof InfiniteSource source) {
            return source.rsadvanced$getInfiniteResources();
        }
        Set<ResourceKey> resources = new HashSet<>();
        if (storage instanceof CompositeStorage composite) {
            for (Storage child : composite.getSources()) {
                resources.addAll(collect(child));
            }
        }
        return Set.copyOf(resources);
    }

    public static boolean contains(Storage storage, ResourceKey resource) {
        // Configured proxies implement InfiniteSource so their access checks take precedence.
        if (storage instanceof InfiniteSource source) {
            return source.rsadvanced$isInfinite(resource);
        }
        if (storage instanceof CompositeStorage composite) {
            for (Storage source : composite.getSources()) {
                if (contains(source, resource)) {
                    return true;
                }
            }
        }
        return false;
    }
}
