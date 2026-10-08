package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;

public final class InfiniteSources {
    private InfiniteSources() {
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
