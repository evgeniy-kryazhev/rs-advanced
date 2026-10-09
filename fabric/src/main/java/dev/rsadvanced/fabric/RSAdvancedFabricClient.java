package dev.rsadvanced.fabric;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.menu.MenuRegistry;
import dev.rsadvanced.client.AnchorScreen;
import dev.rsadvanced.feature.anchor.AnchorContent;

final class RSAdvancedFabricClient {
    private RSAdvancedFabricClient() {
    }

    static void initialize() {
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft ->
                MenuRegistry.registerScreenFactory(AnchorContent.MENU.get(), AnchorScreen::new));
    }
}
