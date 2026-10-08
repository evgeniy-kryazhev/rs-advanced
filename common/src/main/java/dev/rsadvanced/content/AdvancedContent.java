package dev.rsadvanced.content;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

/** Shared content catalog; additional features register their content here. */
public final class AdvancedContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(RSAdvanced.MOD_ID, Registries.CREATIVE_MODE_TAB);

    private static final Map<InfiniteDiskType, RegistrySupplier<InfiniteDiskItem>> DISKS = registerDisks();
    public static final RegistrySupplier<InfiniteDiskItem> COBBLESTONE_DISK = disk(InfiniteDiskType.COBBLESTONE);
    public static final RegistrySupplier<InfiniteDiskItem> WATER_DISK = disk(InfiniteDiskType.WATER);

    private static Map<InfiniteDiskType, RegistrySupplier<InfiniteDiskItem>> registerDisks() {
        Map<InfiniteDiskType, RegistrySupplier<InfiniteDiskItem>> disks = new EnumMap<>(InfiniteDiskType.class);
        for (InfiniteDiskType type : InfiniteDiskType.values()) {
            disks.put(type, ITEMS.register(type.itemName(), () -> new InfiniteDiskItem(type)));
        }
        return Map.copyOf(disks);
    }

    public static RegistrySupplier<InfiniteDiskItem> disk(InfiniteDiskType type) {
        return DISKS.get(type);
    }

    private AdvancedContent() {
    }

    public static void register() {
        TABS.register("main", () -> CreativeTabRegistry.create(builder -> {
            builder.title(Component.translatable("itemGroup.rsadvanced"));
            builder.icon(() -> COBBLESTONE_DISK.get().getDefaultInstance());
            builder.displayItems((parameters, output) -> {
                for (InfiniteDiskType type : InfiniteDiskType.values()) {
                    output.accept(disk(type).get());
                }
            });
        }));
        ITEMS.register();
        TABS.register();
    }
}
