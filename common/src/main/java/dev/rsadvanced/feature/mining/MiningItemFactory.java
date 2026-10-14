package dev.rsadvanced.feature.mining;

import dev.architectury.injectables.annotations.ExpectPlatform;

public final class MiningItemFactory {
    private MiningItemFactory() {
    }

    @ExpectPlatform
    public static MiningItem create() {
        throw new AssertionError("Platform implementation is missing");
    }
}
