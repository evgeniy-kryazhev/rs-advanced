package dev.rsadvanced.content;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.DiskResourceKind;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AdvancedContent {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.ITEM);
    public static final RegistrySupplier<InfiniteDiskItem> ITEM_DISK =
            ITEMS.register("infinite_item_disk", () -> new InfiniteDiskItem(DiskResourceKind.ITEM));
    public static final RegistrySupplier<InfiniteDiskItem> FLUID_DISK =
            ITEMS.register("infinite_fluid_disk", () -> new InfiniteDiskItem(DiskResourceKind.FLUID));

    private AdvancedContent() {
    }

    public static RegistrySupplier<InfiniteDiskItem> disk(DiskResourceKind kind) {
        return kind == DiskResourceKind.ITEM ? ITEM_DISK : FLUID_DISK;
    }

    public static ItemStack cell(ResourceLocation definitionId, DiskResourceKind kind) {
        ItemStack stack = new ItemStack(disk(kind).get());
        stack.set(AdvancedComponents.CELL_DEFINITION.get(), definitionId);
        return stack;
    }

    public static List<ItemStack> creativeCells(HolderLookup.Provider registries) {
        List<ItemStack> cells = new ArrayList<>();
        // Use the current world's catalog on every tab rebuild; never cache variants across worlds.
        for (var entry : CellDefinitions.entries(registries)) {
            cells.add(cell(entry.getKey(), entry.getValue().kind()));
        }
        return cells;
    }

    public static void register() {
        ITEMS.register();
    }
}
