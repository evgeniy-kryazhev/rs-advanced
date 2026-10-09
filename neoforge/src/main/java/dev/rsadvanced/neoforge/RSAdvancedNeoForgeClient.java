package dev.rsadvanced.neoforge;

import dev.rsadvanced.client.AnchorScreen;
import dev.rsadvanced.feature.anchor.AnchorContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class RSAdvancedNeoForgeClient {
    private RSAdvancedNeoForgeClient() {
    }

    static void initialize(IEventBus modBus) {
        // Register on our mod bus during construction, before RegisterMenuScreensEvent is dispatched.
        modBus.addListener(RSAdvancedNeoForgeClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AnchorContent.MENU.get(), AnchorScreen::new);
    }
}
