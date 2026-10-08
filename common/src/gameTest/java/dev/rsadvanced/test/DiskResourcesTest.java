package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.storage.DiskInventory;
import com.refinedmods.refinedstorage.common.storage.StorageRepositoryImpl;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
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

        for (TestCell cell : TestCell.values()) {
            ItemStack original = cell.stack();
            assertTrue(inventory.canPlaceItem(0, original));
            assertEquals(1, original.getMaxStackSize());
            var saved = ItemStack.CODEC.encodeStart(operations, original).getOrThrow();
            ItemStack restored = ItemStack.CODEC.parse(operations, saved).getOrThrow();
            assertTrue(ItemStack.isSameItemSameComponents(original, restored));
            inventory.setItem(0, restored);

            var source = inventory.resolve(0).orElseThrow();
            assertEquals(64, source.extract(cell.resource(), 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(64, source.extract(cell.resource(), 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(64, source.insert(cell.resource(), 64, Action.EXECUTE, Actor.EMPTY));
        }
    }

    public static void survivalRecipesMatchAndReturnEmptyBuckets(ServerLevel level) {
        for (String definitionId : List.of("rsadvanced:cobblestone", "rsadvanced:water", "rsadvanced_test:lava", "my_pack:lava")) {
            TestCell cell = TestCell.named(definitionId);
            // Read the recipe from Minecraft's loaded datapacks, including the transformed common module.
            var holder = level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(cell.id().getNamespace(), "infinite_" + cell.id().getPath() + "_cell")).orElseThrow();
            ShapelessRecipe recipe = (ShapelessRecipe) holder.value();
            List<ItemStack> ingredients = new ArrayList<>();
            long expectedBuckets = 0;
            for (var recipeIngredient : recipe.getIngredients()) {
                ItemStack ingredient = recipeIngredient.getItems()[0].copy();
                assertTrue(!ingredient.isEmpty());
                ingredients.add(ingredient);
                if (ingredient.getItem().hasCraftingRemainingItem()
                        && ingredient.getItem().getCraftingRemainingItem() == Items.BUCKET) {
                    expectedBuckets++;
                }
            }
            while (ingredients.size() < 9) {
                ingredients.add(ItemStack.EMPTY);
            }
            CraftingInput input = CraftingInput.of(3, 3, ingredients);
            assertTrue(recipe.matches(input, level));
            ItemStack result = recipe.assemble(input, level.registryAccess());
            assertTrue(ItemStack.isSameItemSameComponents(cell.stack(), result));
            assertEquals(1, result.getCount());
            assertEquals(expectedBuckets, recipe.getRemainingItems(input).stream().filter(stack -> stack.is(Items.BUCKET)).count());
        }
    }

    public static void infinityPacketRoundTripsMenuIdentityAndResources() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        try {
            InfiniteResourcesPayload original = new InfiniteResourcesPayload(17, TestCell.values().stream()
                    .map(TestCell::resource).collect(java.util.stream.Collectors.toSet()));
            InfiniteResourcesPayload.STREAM_CODEC.encode(buffer, original);
            assertEquals(original, InfiniteResourcesPayload.STREAM_CODEC.decode(buffer));
        } finally {
            buffer.release();
        }
    }
}
