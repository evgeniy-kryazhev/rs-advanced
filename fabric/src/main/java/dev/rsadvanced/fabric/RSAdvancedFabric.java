package dev.rsadvanced.fabric;

import dev.rsadvanced.RSAdvanced;
import net.fabricmc.api.ModInitializer;

public final class RSAdvancedFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        RSAdvanced.initialize();
    }
}
