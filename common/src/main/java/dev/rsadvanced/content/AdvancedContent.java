package dev.rsadvanced.content;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/** Shared content catalog; additional features register their content here. */
public final class AdvancedContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(RSAdvanced.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<InfiniteDiskItem> COBBLESTONE_DISK = ITEMS.register(
            InfiniteDiskType.COBBLESTONE.itemName(), () -> new InfiniteDiskItem(InfiniteDiskType.COBBLESTONE));
    public static final RegistrySupplier<InfiniteDiskItem> WATER_DISK = ITEMS.register(
            InfiniteDiskType.WATER.itemName(), () -> new InfiniteDiskItem(InfiniteDiskType.WATER));

    private AdvancedContent() {
    }

    public static void register() {
        TABS.register("main", () -> CreativeTabRegistry.create(builder -> {
            builder.title(Component.translatable("itemGroup.rsadvanced"));
            builder.icon(() -> COBBLESTONE_DISK.get().getDefaultInstance());
            builder.displayItems((parameters, output) -> {
                output.accept(COBBLESTONE_DISK.get());
                output.accept(WATER_DISK.get());
            });
        }));
        ITEMS.register();
        TABS.register();
    }
}
