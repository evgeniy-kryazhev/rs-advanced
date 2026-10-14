package dev.rsadvanced.neoforge;

import dev.rsadvanced.client.AnchorScreen;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.feature.terminal.TerminalContent;
import dev.rsadvanced.feature.terminal.client.TerminalCardScreen;
import com.refinedmods.refinedstorage.common.support.network.item.NetworkItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

final class RSAdvancedNeoForgeClient {
    private RSAdvancedNeoForgeClient() {
    }

    static void initialize(IEventBus modBus, ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        // Register on our mod bus during construction, before RegisterMenuScreensEvent is dispatched.
        modBus.addListener(RSAdvancedNeoForgeClient::registerScreens);
        modBus.addListener(RSAdvancedNeoForgeClient::registerItemProperties);
    }

    private static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(TerminalContent.ITEM.get(),
                NetworkItemPropertyFunction.NAME, new NetworkItemPropertyFunction()));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AnchorContent.MENU.get(), AnchorScreen::new);
        event.register(TerminalContent.CARD_MENU.get(), TerminalCardScreen::new);
    }
}
