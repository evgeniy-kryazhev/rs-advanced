package dev.rsadvanced.feature.anchor;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rsadvanced.RSAdvanced;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class AnchorContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(RSAdvanced.MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.MENU);
    public static final RegistrySupplier<AnchorBlock> BLOCK = BLOCKS.register("network_anchor", AnchorBlock::new);
    public static final RegistrySupplier<Item> ITEM = ITEMS.register("network_anchor",
            () -> new BlockItem(BLOCK.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context,
                        java.util.List<net.minecraft.network.chat.Component> tooltip,
                        net.minecraft.world.item.TooltipFlag flag) {
                    tooltip.add(net.minecraft.network.chat.Component.translatable("block.rsadvanced.network_anchor.help"));
                }
            });
    public static final RegistrySupplier<BlockEntityType<AnchorBlockEntity>> ENTITY = ENTITIES.register("network_anchor",
            () -> BlockEntityType.Builder.of(AnchorBlockEntity::new, BLOCK.get()).build(null));
    public static final RegistrySupplier<MenuType<AnchorMenu>> MENU = MENUS.register("network_anchor",
            () -> MenuRegistry.ofExtended((id, inventory, buffer) ->
                    new AnchorMenu(id, inventory, buffer.readBlockPos())));

    private AnchorContent() {
    }

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        ENTITIES.register();
        MENUS.register();
    }
}
