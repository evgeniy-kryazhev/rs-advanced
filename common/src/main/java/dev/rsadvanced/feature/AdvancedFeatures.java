package dev.rsadvanced.feature;

import dev.rsadvanced.feature.disk.InfiniteDiskType;

/** Explicit startup catalog for independent RS Advanced features. */
public final class AdvancedFeatures {
    private AdvancedFeatures() {
    }

    public static void initialize() {
        InfiniteDiskType.registerStorageTypes();
    }
}
