package dev.rsadvanced.test;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.storage.DiskInventory;
import com.refinedmods.refinedstorage.common.storage.StorageRepositoryImpl;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;


import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public class DiskResourcesTest {
    public static void standardDriveAcceptsDisksAndReloadsTheirStatelessItemStacks() {
        var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var operations = registries.createSerializationContext(NbtOps.INSTANCE);
        DiskInventory inventory = new DiskInventory((container, slot) -> { }, 8);
        inventory.setStorageRepository(new StorageRepositoryImpl());

        for (InfiniteDiskType diskType : InfiniteDiskType.values()) {
            ItemStack original = new ItemStack(BuiltInRegistries.ITEM.get(RSAdvanced.id(diskType.itemName())));
            assertTrue(inventory.canPlaceItem(0, original));
            assertEquals(1, original.getMaxStackSize());
            var saved = ItemStack.CODEC.encodeStart(operations, original).getOrThrow();
            ItemStack restored = ItemStack.CODEC.parse(operations, saved).getOrThrow();
            inventory.setItem(0, restored);

            var source = inventory.resolve(0).orElseThrow();
            assertEquals(64, source.extract(diskType.resource(), 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(64, source.extract(diskType.resource(), 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(64, source.insert(diskType.resource(), 64, Action.EXECUTE, Actor.EMPTY));
        }
    }

    public static void survivalRecipesMatchAndReturnEmptyBuckets(ServerLevel level) {
        ItemStack housing = new ItemStack(BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("refinedstorage", "storage_housing")));
        for (InfiniteDiskType diskType : InfiniteDiskType.values()) {
            // Read the recipe from Minecraft's loaded datapacks, including the transformed common module.
            var holder = level.getRecipeManager().byKey(RSAdvanced.id(diskType.itemName())).orElseThrow();
            ShapelessRecipe recipe = (ShapelessRecipe) holder.value();
            List<ItemStack> ingredients = diskType == InfiniteDiskType.WATER
                    ? List.of(housing, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.WATER_BUCKET), ItemStack.EMPTY)
                    : List.of(housing, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.LAVA_BUCKET),
                            new ItemStack(Items.COBBLESTONE));
            CraftingInput input = CraftingInput.of(2, 2, ingredients);
            assertTrue(recipe.matches(input, level));
            ItemStack result = recipe.assemble(input, level.registryAccess());
            assertEquals(RSAdvanced.id(diskType.itemName()), BuiltInRegistries.ITEM.getKey(result.getItem()));
            assertEquals(1, result.getCount());
            assertEquals(2, recipe.getRemainingItems(input).stream().filter(stack -> stack.is(Items.BUCKET)).count());
        }
    }

    public static void infinityPacketRoundTripsMenuIdentityAndFlags() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            InfiniteResourcesPayload original = new InfiniteResourcesPayload(17, 3);
            InfiniteResourcesPayload.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, InfiniteResourcesPayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}

