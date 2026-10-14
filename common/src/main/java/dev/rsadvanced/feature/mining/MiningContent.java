package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rsadvanced.RSAdvanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;

/** Registration and lifecycle for the mining tool feature. */
public final class MiningContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.MENU);
    public static final RegistrySupplier<MiningItem> ITEM = ITEMS.register("network_mining_tool", MiningItem::new);
    public static final RegistrySupplier<MenuType<MiningCardMenu>> CARD_MENU = MENUS.register("mining_card",
            () -> MenuRegistry.ofExtended((id, inventory, buffer) ->
                    new MiningCardMenu(id, inventory, SlotReferenceFactory.STREAM_CODEC.decode(
                            new RegistryFriendlyByteBuf(buffer, inventory.player.registryAccess())))));

    private MiningContent() {
    }

    public static void register() {
        ITEMS.register();
        MENUS.register();
    }
}
