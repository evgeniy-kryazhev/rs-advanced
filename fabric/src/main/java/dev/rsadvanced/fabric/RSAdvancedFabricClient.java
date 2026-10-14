package dev.rsadvanced.fabric;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.registry.menu.MenuRegistry;
import dev.rsadvanced.client.AnchorScreen;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.fabric.config.FabricConfigScreen;
import dev.rsadvanced.feature.terminal.TerminalContent;
import dev.rsadvanced.feature.mining.MiningContent;
import dev.rsadvanced.feature.mining.client.MiningCardScreen;
import dev.rsadvanced.feature.terminal.client.TerminalCardScreen;
import com.refinedmods.refinedstorage.common.support.network.item.NetworkItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;

final class RSAdvancedFabricClient {
    private RSAdvancedFabricClient() {
    }

    static void initialize() {
        FabricConfigScreen.register();
        dev.rsadvanced.feature.mining.client.MiningClient.initialize();
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft ->
                MenuRegistry.registerScreenFactory(AnchorContent.MENU.get(), AnchorScreen::new));
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft ->
                MenuRegistry.registerScreenFactory(TerminalContent.CARD_MENU.get(), TerminalCardScreen::new));
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft ->
                MenuRegistry.registerScreenFactory(MiningContent.CARD_MENU.get(), MiningCardScreen::new));
        ClientLifecycleEvent.CLIENT_SETUP.register(minecraft -> ItemProperties.register(
                TerminalContent.ITEM.get(), NetworkItemPropertyFunction.NAME, new NetworkItemPropertyFunction()));
    }
}
