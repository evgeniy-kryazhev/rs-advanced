package dev.rsadvanced.feature.disk;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedComponents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class InfiniteStorageBlockContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.BLOCK);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(RSAdvanced.MOD_ID, Registries.ITEM);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
            DeferredRegister.create(RSAdvanced.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<InfiniteStorageBlock> ITEM_BLOCK = BLOCKS.register("infinite_item_storage_block",
            () -> new InfiniteStorageBlock(DiskResourceKind.ITEM));
    public static final RegistrySupplier<InfiniteStorageBlock> FLUID_BLOCK = BLOCKS.register("infinite_fluid_storage_block",
            () -> new InfiniteStorageBlock(DiskResourceKind.FLUID));
    public static final RegistrySupplier<InfiniteStorageBlockItem> ITEM = ITEMS.register("infinite_item_storage_block",
            () -> new InfiniteStorageBlockItem(ITEM_BLOCK.get()));
    public static final RegistrySupplier<InfiniteStorageBlockItem> FLUID = ITEMS.register("infinite_fluid_storage_block",
            () -> new InfiniteStorageBlockItem(FLUID_BLOCK.get()));
    public static final RegistrySupplier<BlockEntityType<InfiniteStorageBlockEntity>> ENTITY =
            ENTITIES.register("infinite_storage_block", () -> BlockEntityType.Builder.of(
                    InfiniteStorageBlockEntity::new, ITEM_BLOCK.get(), FLUID_BLOCK.get()).build(null));

    private InfiniteStorageBlockContent() {
    }

    public static ItemStack stack(ResourceLocation definitionId, DiskResourceKind kind) {
        ItemStack stack = new ItemStack(kind == DiskResourceKind.ITEM ? ITEM.get() : FLUID.get());
        stack.set(AdvancedComponents.CELL_DEFINITION.get(), definitionId);
        return stack;
    }

    public static List<ItemStack> creativeBlocks(HolderLookup.Provider registries) {
        List<ItemStack> blocks = new ArrayList<>();
        for (var entry : CellDefinitions.entries(registries)) {
            blocks.add(stack(entry.getKey(), entry.getValue().kind()));
        }
        return blocks;
    }

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
        ENTITIES.register();
    }
}
