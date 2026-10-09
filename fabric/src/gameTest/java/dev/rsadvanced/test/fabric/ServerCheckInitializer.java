package dev.rsadvanced.test.fabric;

import dev.rsadvanced.test.ClientValidationServer;
import net.fabricmc.api.ModInitializer;

public final class ServerCheckInitializer implements ModInitializer {
    @Override
    public void onInitialize() {
        ClientValidationServer.register();
        dev.rsadvanced.test.AnchorRestartScenario.register();
    }
}
