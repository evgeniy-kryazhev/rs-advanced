package dev.rsadvanced.feature.terminal;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rsadvanced.RSAdvanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;

/** Registration and lifecycle for the portable terminal feature. */
public final class TerminalContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.MENU);
    public static final RegistrySupplier<TerminalItem> ITEM = ITEMS.register("network_terminal", TerminalItem::new);
    public static final RegistrySupplier<MenuType<TerminalCardMenu>> CARD_MENU = MENUS.register("terminal_card",
            () -> MenuRegistry.ofExtended((id, inventory, buffer) ->
                    new TerminalCardMenu(id, inventory, SlotReferenceFactory.STREAM_CODEC.decode(
                            new RegistryFriendlyByteBuf(buffer, inventory.player.registryAccess())))));

    private TerminalContent() {
    }

    public static void register() {
        ITEMS.register();
        MENUS.register();
        TickEvent.SERVER_PRE.register(TerminalSessions::tick);
        LifecycleEvent.SERVER_STOPPED.register(server -> TerminalSessions.clear());
    }
}
