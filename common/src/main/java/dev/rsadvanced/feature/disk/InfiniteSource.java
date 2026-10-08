package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;

/** Explicit source metadata, independent of the numeric quantity shown by RS. */
public interface InfiniteSource {
    boolean rsadvanced$isInfinite(ResourceKey resource);
}
