package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.storage.AccessMode;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.feature.disk.DiskResourceKind;
import dev.rsadvanced.feature.disk.InfiniteResourceStorage;
import dev.rsadvanced.feature.disk.InfiniteSources;
import dev.rsadvanced.feature.disk.InfiniteStorageBlockContent;
import dev.rsadvanced.feature.disk.InfiniteStorageBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.Direction;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class InfiniteStorageBlockTest {
    @SharedGameTest(value = "infinite_storage_blocks", order = 1)
    public static void placementDropsReloadAndRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos position = helper.absolutePos(new BlockPos(0, 4, 0));
        level.removeBlock(position, false);
        player.setPos(position.getCenter().add(3, 0, 0));
        for (TestCell cell : TestCell.values()) {
            ItemStack stack = InfiniteStorageBlockContent.stack(cell.id(), cell.definition().kind());
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack.copy(),
                    new BlockHitResult(position.getCenter(), Direction.UP, position, false));
            check(((net.minecraft.world.item.BlockItem) stack.getItem()).place(context).consumesAction(),
                    "Cannot place storage block for " + cell.id() + " at " + context.getClickedPos()
                            + ", existing state=" + level.getBlockState(context.getClickedPos()));
            var block = (InfiniteStorageBlockEntity) level.getBlockEntity(position);
            assertEquals(cell.id(), block.definitionId());
            block.node().setActive(true);
            // A detached RS node does not expose its sources on activation until they are refreshed.
            block.node().onStorageChanged(0);
            var configuredStorage = new com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl();
            configuredStorage.addSource(block.node().getStorage());
            check(InfiniteSources.contains(configuredStorage, cell.resource()), "Missing infinity metadata for " + cell.id());
            for (int iteration = 0; iteration < 100; iteration++) {
                assertEquals(64, configuredStorage.extract(cell.resource(), 64, Action.EXECUTE, Actor.EMPTY));
                assertEquals(64, configuredStorage.insert(cell.resource(), 64, Action.EXECUTE, Actor.EMPTY));
            }
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, configuredStorage.getStored());
            block.node().getStorageConfiguration().setAccessMode(AccessMode.INSERT);
            assertEquals(0, configuredStorage.extract(cell.resource(), 64, Action.EXECUTE, Actor.EMPTY));
            assertTrue(!InfiniteSources.contains(configuredStorage, cell.resource()));
            block.node().getStorageConfiguration().setAccessMode(AccessMode.INSERT_EXTRACT);
            block.node().getStorageConfiguration().setInsertPriority(37);
            CompoundTag saved = block.saveWithoutMetadata(level.registryAccess());
            assertTrue(!saved.contains("sid"));
            var restored = new InfiniteStorageBlockEntity(position, block.getBlockState());
            restored.loadWithComponents(saved, level.registryAccess());
            assertEquals(cell.id(), restored.definitionId());
            assertEquals(37, restored.node().getStorageConfiguration().getInsertPriority());
            assertEquals(64, restored.resolve(0).orElseThrow().extract(cell.resource(), 64, Action.EXECUTE, Actor.EMPTY));
            var drops = Block.getDrops(block.getBlockState(), level, position, block);
            assertEquals(1, drops.size());
            check(ItemStack.isSameItemSameComponents(stack, drops.getFirst()),
                    "Drop differs for " + cell.id() + ": " + drops.getFirst().getComponents());
            var clone = block.getBlockState().getBlock().getCloneItemStack(level, position, block.getBlockState());
            assertEquals(cell.id(), clone.get(AdvancedComponents.CELL_DEFINITION.get()));
            level.removeBlock(position, false);
        }
        level.setBlockAndUpdate(position, InfiniteStorageBlockContent.ITEM_BLOCK.get().defaultBlockState());
        var unknown = (InfiniteStorageBlockEntity) level.getBlockEntity(position);
        unknown.setDefinition(ResourceLocation.parse("missing:resource"));
        assertTrue(unknown.resolve(0).isEmpty());
        assertEquals(ResourceLocation.parse("missing:resource"), Block.getDrops(unknown.getBlockState(), level,
                position, unknown).getFirst().get(AdvancedComponents.CELL_DEFINITION.get()));
        unknown.setDefinition(RSAdvanced.id("water"));
        assertTrue(unknown.resolve(0).isEmpty());
        level.removeBlock(position, false);

        for (String resource : List.of("cobblestone", "water")) {
            var recipe = (ShapedRecipe) level.getRecipeManager()
                    .byKey(RSAdvanced.id("infinite_" + resource + "_storage_block")).orElseThrow().value();
            List<ItemStack> ingredients = new ArrayList<>();
            for (var ingredient : recipe.getIngredients()) {
                ingredients.add(ingredient.getItems()[0].copy());
            }
            var input = CraftingInput.of(3, 3, ingredients);
            check(recipe.matches(input, level), "Block recipe does not match for " + resource);
            var kind = resource.equals("water") ? DiskResourceKind.FLUID : DiskResourceKind.ITEM;
            var expected = InfiniteStorageBlockContent.stack(RSAdvanced.id(resource), kind);
            assertTrue(ItemStack.isSameItemSameComponents(expected, recipe.assemble(input, level.registryAccess())));
            if (kind == DiskResourceKind.FLUID) {
                assertEquals(1, recipe.getRemainingItems(input).stream().filter(stack -> stack.is(Items.BUCKET)).count());
            }
        }
    }

    @SharedGameTest(value = "infinite_storage_block_permissions", order = 2)
    public static void menuRequiresBuildPermission(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos position = helper.absolutePos(new BlockPos(0, 4, 0));
        level.setBlockAndUpdate(position, InfiniteStorageBlockContent.ITEM_BLOCK.get().defaultBlockState());
        var storageBlock = (InfiniteStorageBlockEntity) level.getBlockEntity(position);
        storageBlock.setDefinition(RSAdvanced.id("cobblestone"));
        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(position.getCenter().add(2, 0, 0));
        var menu = (dev.rsadvanced.feature.disk.InfiniteStorageBlockMenu)
                storageBlock.createMenu(91, player.getInventory(), player);
        var security = new com.refinedmods.refinedstorage.common.security.PlatformSecurityNetworkComponentImpl(
                com.refinedmods.refinedstorage.api.network.security.SecurityPolicy.of(
                        com.refinedmods.refinedstorage.common.security.BuiltinPermission.OPEN));
        var currentSecurity = new java.util.concurrent.atomic.AtomicReference<>(security);
        var backingNetwork = new com.refinedmods.refinedstorage.api.network.impl.NetworkImpl(
                com.refinedmods.refinedstorage.common.api.RefinedStorageApi.INSTANCE.getNetworkComponentMapFactory());
        var guardedNetwork = (com.refinedmods.refinedstorage.api.network.Network) java.lang.reflect.Proxy.newProxyInstance(
                com.refinedmods.refinedstorage.api.network.Network.class.getClassLoader(),
                new Class<?>[] {com.refinedmods.refinedstorage.api.network.Network.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getComponent") && arguments[0]
                            == com.refinedmods.refinedstorage.common.api.security.PlatformSecurityNetworkComponent.class) {
                        return currentSecurity.get();
                    }
                    return method.invoke(backingNetwork, arguments);
                });
        storageBlock.node().setNetwork(guardedNetwork);
        try {
            assertTrue(storageBlock.canOpen(player));
            menu.receivePropertyChangeFromClient(
                    com.refinedmods.refinedstorage.common.storage.StoragePropertyTypes.INSERT_PRIORITY.id(), 72);
            assertEquals(0, storageBlock.node().getStorageConfiguration().getInsertPriority());
            menu.setCarried(new ItemStack(Items.COBBLESTONE));
            menu.handleResourceSlotChange(0, false);
            check(storageBlock.getMenuData().resources().getFirst().isEmpty(), "Denied filter change was applied");
            currentSecurity.set(new com.refinedmods.refinedstorage.common.security.PlatformSecurityNetworkComponentImpl(
                    com.refinedmods.refinedstorage.api.network.security.SecurityPolicy.of(
                            com.refinedmods.refinedstorage.common.security.BuiltinPermission.OPEN,
                            com.refinedmods.refinedstorage.common.security.BuiltinPermission.BUILD)));
            menu.receivePropertyChangeFromClient(
                    com.refinedmods.refinedstorage.common.storage.StoragePropertyTypes.INSERT_PRIORITY.id(), 72);
            assertEquals(72, storageBlock.node().getStorageConfiguration().getInsertPriority());
            menu.handleResourceSlotChange(0, false);
            check(storageBlock.getMenuData().resources().getFirst().isPresent(), "Allowed filter change was ignored");
        } finally {
            storageBlock.node().setNetwork(null);
            backingNetwork.remove();
            level.removeBlock(position, false);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @SharedGameTest(value = "infinite_storage_block_network", order = 3, timeoutTicks = 100, completion = SharedGameTest.Completion.MANUAL)
    public static void networkConnectionAndRemoval(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos controllerPosition = new BlockPos(25008, 80, 25008);
        BlockPos storagePosition = controllerPosition.above();
        var chunk = new net.minecraft.world.level.ChunkPos(controllerPosition);
        level.setChunkForced(chunk.x, chunk.z, true);
        level.setBlockAndUpdate(controllerPosition, Blocks.INSTANCE.getController().get(DyeColor.LIGHT_BLUE).defaultBlockState());
        level.setBlockAndUpdate(storagePosition, InfiniteStorageBlockContent.ITEM_BLOCK.get().defaultBlockState());
        var controller = (ControllerBlockEntity) level.getBlockEntity(controllerPosition);
        var block = (InfiniteStorageBlockEntity) level.getBlockEntity(storagePosition);
        TestCell cell = TestCell.named("rsadvanced:cobblestone");
        block.setDefinition(cell.id());
        controller.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE);
        helper.runAtTickTime(40, () -> {
            assertTrue(block.node().getNetwork() != null);
            assertTrue(block.node().isActive());
            var storageComponent = block.node().getNetwork()
                    .getComponent(com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent.class);
            var root = (com.refinedmods.refinedstorage.api.storage.Storage) storageComponent;
            assertTrue(InfiniteSources.contains(root, cell.resource()));
            assertEquals(64, root.extract(cell.resource(), 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, root.getStored());
            level.removeBlock(storagePosition, false);
            helper.runAtTickTime(50, () -> {
                assertTrue(!InfiniteSources.contains(root, cell.resource()));
                level.removeBlock(controllerPosition, false);
                level.setChunkForced(chunk.x, chunk.z, false);
                helper.succeed();
            });
        });
    }
}
