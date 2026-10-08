package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import java.util.Set;

/** Explicit source metadata, independent of the numeric quantity shown by RS. */
public interface InfiniteSource {
    boolean rsadvanced$isInfinite(ResourceKey resource);

    Set<ResourceKey> rsadvanced$getInfiniteResources();
}
