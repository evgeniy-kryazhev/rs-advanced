package dev.rsadvanced.test.fabric;

import dev.rsadvanced.test.client.CellPresentationClientCheck;
import net.fabricmc.api.ClientModInitializer;

public final class ClientCheckInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        dev.rsadvanced.test.client.ConfigurationClientCheck.register(new FabricConfigurationClientCheck());
        CellPresentationClientCheck.register();
    }
}
