package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import dev.rsadvanced.feature.anchor.AnchorBlockEntity;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.feature.anchor.AnchorManager;
import dev.rsadvanced.feature.anchor.AnchorStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Actual world nodes and each loader's registered lookup/capability; no mock network. */
public final class AnchorIntegrationTest {
    public static void topologyAndPower(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos controllerPosition = new BlockPos(20014, 80, 20008);
        BlockPos firstPosition = controllerPosition.above();
        BlockPos reservePosition = firstPosition.above();
        BlockPos boundaryPosition = firstPosition.east();
        BlockPos nextChunkPosition = boundaryPosition.east();
        ChunkPos initialChunk = new ChunkPos(controllerPosition);
        level.setChunkForced(initialChunk.x, initialChunk.z, true);
        level.setBlockAndUpdate(controllerPosition, Blocks.INSTANCE.getController().get(DyeColor.LIGHT_BLUE).defaultBlockState());
        level.setBlockAndUpdate(firstPosition, AnchorContent.BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(reservePosition, AnchorContent.BLOCK.get().defaultBlockState());
        ControllerBlockEntity controller = (ControllerBlockEntity) level.getBlockEntity(controllerPosition);
        AnchorBlockEntity first = (AnchorBlockEntity) level.getBlockEntity(firstPosition);
        AnchorBlockEntity reserve = (AnchorBlockEntity) level.getBlockEntity(reservePosition);
        controller.getEnergyStorage().receive(controller.getEnergyStorage().getCapacity(), Action.EXECUTE);
        for (int refillTick = 1; refillTick < 90; refillTick += 10) {
            helper.runAtTickTime(refillTick, () -> controller.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE));
        }

        helper.runAtTickTime(60, () -> {
            if (first.status() != AnchorStatus.ACTIVE) {
                throw new IllegalStateException("Anchor boot status=" + first.status() + ", actual energy="
                        + controller.getEnergyStorage().getStored() + ", controller node="
                        + controller.getContainerProvider().getContainers().iterator().next().getNode()
                        + ", ticking=" + level.shouldTickBlocksAt(controllerPosition));
            }
            assertEquals(AnchorStatus.ACTIVE, first.status());
            level.setChunkForced(initialChunk.x, initialChunk.z, false);
            assertEquals(AnchorStatus.RESERVE, reserve.status());
            assertEquals(1, first.areaSize());
            assertEquals(81L, first.cost());
            assertEquals(0L, reserve.node().getEnergyUsage());
            level.setBlockAndUpdate(boundaryPosition, Blocks.INSTANCE.getCable().get(DyeColor.LIGHT_BLUE).defaultBlockState());
            level.setBlockAndUpdate(nextChunkPosition, Blocks.INSTANCE.getCable().get(DyeColor.LIGHT_BLUE).defaultBlockState());
        });
        long[] stored = new long[1];
        helper.runAtTickTime(65, () -> stored[0] = controller.getEnergyStorage().getStored());
        helper.runAtTickTime(66, () -> {
            long charged = stored[0] - controller.getEnergyStorage().getStored();
            if (charged != 83) {
                throw new IllegalStateException("Payment=" + charged + ", status=" + first.status()
                        + ", area=" + first.areaSize() + ", stored=" + controller.getEnergyStorage().getStored()
                        + ", same network=" + (first.node().getNetwork() == controller.getContainerProvider()
                        .getContainers().iterator().next().getNode().getNetwork()));
            }
        });
        helper.runAtTickTime(70, () -> {
            assertEquals(2, first.areaSize());
            assertEquals(83L, first.cost());
            if (!AnchorManager.chunks(first).contains(new ChunkPos(nextChunkPosition).toLong())) {
                throw new IllegalStateException("Missing held chunk; status=" + first.status() + ", enabled="
                        + first.enabled() + ", stored=" + controller.getEnergyStorage().getStored()
                        + ", chunks=" + AnchorManager.chunks(first));
            }
            first.setEnabled(false);
        });
        helper.runAtTickTime(80, () -> {
            assertEquals(AnchorStatus.DISABLED, first.status());
            assertEquals(AnchorStatus.ACTIVE, reserve.status());
            assertEquals(83L, reserve.node().getEnergyUsage());
            level.removeBlock(nextChunkPosition, false);
        });
        helper.runAtTickTime(90, () -> {
            assertEquals(1, reserve.areaSize());
            controller.getEnergyStorage().extract(Long.MAX_VALUE, Action.EXECUTE);
        });
        helper.runAtTickTime(91, () -> {
            assertEquals(AnchorStatus.NO_ENERGY, reserve.status());
            assertEquals(0, reserve.heldSize());
            assertTrue(AnchorManager.chunks(reserve).isEmpty());
            level.removeBlock(reservePosition, false);
            level.removeBlock(firstPosition, false);
            level.removeBlock(boundaryPosition, false);
            level.removeBlock(controllerPosition, false);
            helper.succeed();
        });
    }

    public static void incrementalContainerCounts(GameTestHelper helper) {
        var network = new com.refinedmods.refinedstorage.api.network.impl.NetworkImpl(
                com.refinedmods.refinedstorage.common.api.RefinedStorageApi.INSTANCE.getNetworkComponentMapFactory());
        var component = network.getComponent(dev.rsadvanced.feature.anchor.AnchorNetworkComponent.class);
        var anchor = new AnchorBlockEntity(new BlockPos(16, 80, 0), AnchorContent.BLOCK.get().defaultBlockState());
        anchor.setLevel(helper.getLevel());
        anchor.node().setNetwork(network);
        var anchorContainer = anchor.getContainerProvider().getContainers().iterator().next();
        network.addContainer(anchorContainer);
        var firstCable = createContainer(helper, new BlockPos(32, 80, 0), network);
        var secondCable = createContainer(helper, new BlockPos(33, 80, 0), network);
        assertEquals(2, component.areas().getFirst().chunks().size());
        network.removeContainer(firstCable);
        assertEquals(2, component.areas().getFirst().chunks().size());
        network.removeContainer(secondCable);
        assertEquals(1, component.areas().getFirst().chunks().size());
        network.removeContainer(anchorContainer);
        assertTrue(component.areas().isEmpty());
        network.remove();
    }

    public static void overlapsMergesAndLimit(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos firstControllerPosition = new BlockPos(30002, 80, 30008);
        BlockPos secondControllerPosition = firstControllerPosition.east(4);
        BlockPos firstPosition = firstControllerPosition.above();
        BlockPos secondPosition = secondControllerPosition.above();
        ChunkPos sharedChunk = new ChunkPos(firstPosition);
        level.setChunkForced(sharedChunk.x, sharedChunk.z, true);
        // A failed previous run may have saved disabled anchors at these remote positions.
        for (int offset = 0; offset <= 4; offset++) {
            level.removeBlock(firstPosition.east(offset), false);
        }
        for (BlockPos position : java.util.List.of(firstControllerPosition, secondControllerPosition)) {
            level.removeBlock(position, false);
            level.setBlockAndUpdate(position, Blocks.INSTANCE.getController().get(DyeColor.LIGHT_BLUE).defaultBlockState());
            level.setBlockAndUpdate(position.above(), AnchorContent.BLOCK.get().defaultBlockState());
        }
        var firstController = (ControllerBlockEntity) level.getBlockEntity(firstControllerPosition);
        var secondController = (ControllerBlockEntity) level.getBlockEntity(secondControllerPosition);
        var first = (AnchorBlockEntity) level.getBlockEntity(firstPosition);
        var second = (AnchorBlockEntity) level.getBlockEntity(secondPosition);
        for (int refillTick = 1; refillTick < 140; refillTick += 10) {
            helper.runAtTickTime(refillTick, () -> {
                firstController.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE);
                secondController.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE);
            });
        }
        long[] stored = new long[1];
        helper.runAtTickTime(60, () -> {
            assertEquals(AnchorStatus.ACTIVE, first.status());
            assertEquals(AnchorStatus.ACTIVE, second.status());
            level.setChunkForced(sharedChunk.x, sharedChunk.z, false);
        });
        helper.runAtTickTime(65, () -> stored[0] = firstController.getEnergyStorage().getStored()
                + secondController.getEnergyStorage().getStored());
        helper.runAtTickTime(66, () -> assertEquals(162L, stored[0]
                - firstController.getEnergyStorage().getStored() - secondController.getEnergyStorage().getStored()));
        helper.runAtTickTime(70, () -> {
            for (int offset = 1; offset < 4; offset++) {
                level.setBlockAndUpdate(firstPosition.east(offset),
                        Blocks.INSTANCE.getCable().get(DyeColor.LIGHT_BLUE).defaultBlockState());
            }
        });
        helper.runAtTickTime(75, () -> {
            assertEquals(AnchorStatus.ACTIVE, first.status());
            assertEquals(AnchorStatus.RESERVE, second.status());
            stored[0] = firstController.getEnergyStorage().getStored() + secondController.getEnergyStorage().getStored();
        });
        helper.runAtTickTime(76, () -> assertEquals(81L, stored[0]
                - firstController.getEnergyStorage().getStored() - secondController.getEnergyStorage().getStored()));
        helper.runAtTickTime(80, () -> level.removeBlock(firstPosition.east(2), false));
        helper.runAtTickTime(90, () -> {
            assertEquals(AnchorStatus.ACTIVE, first.status());
            assertEquals(AnchorStatus.ACTIVE, second.status());
            first.setEnabled(false);
            assertTrue(!AnchorManager.chunks(second).isEmpty());
            assertTrue(level.shouldTickBlocksAt(secondPosition));
        });
        var extraContainers = new java.util.ArrayList<com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer>();
        // Over-limit areas intentionally unload. Keep this test's graph externally loaded
        // while removing the extra nodes, so automatic recovery has a live graph to inspect.
        helper.runAtTickTime(95, () -> level.setChunkForced(sharedChunk.x, sharedChunk.z, true));
        helper.runAtTickTime(100, () -> {
            for (int index = 0; index < 256; index++) {
                extraContainers.add(createContainer(helper, new BlockPos(40000 + index * 16, 80, 40000), second.node().getNetwork()));
            }
        });
        helper.runAtTickTime(110, () -> {
            assertEquals(AnchorStatus.LIMIT_EXCEEDED, second.status());
            assertEquals(257, second.areaSize());
            assertEquals(0, second.heldSize());
            for (var container : extraContainers) {
                second.node().getNetwork().removeContainer(container);
            }
        });
        helper.runAtTickTime(120, () -> {
            assertEquals(AnchorStatus.ACTIVE, second.status());
            assertEquals(1, second.areaSize());
            level.setChunkForced(sharedChunk.x, sharedChunk.z, false);
        });
        helper.runAtTickTime(140, () -> {
            for (BlockPos position : java.util.List.of(firstPosition, secondPosition, firstControllerPosition,
                    secondControllerPosition, firstPosition.east(), firstPosition.east(3))) {
                level.removeBlock(position, false);
            }
            assertTrue(AnchorManager.chunks(second).isEmpty());
            helper.succeed();
        });
    }

    public static void persistenceRecipeAndPermissions(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos position = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(position, AnchorContent.BLOCK.get().defaultBlockState());
        var anchor = (AnchorBlockEntity) level.getBlockEntity(position);
        var restored = (AnchorBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(position,
                anchor.getBlockState(), anchor.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
        assertEquals(anchor.instanceId(), restored.instanceId());
        assertTrue(restored.enabled());

        var recipes = level.getRecipeManager();
        var recipe = recipes.byKey(dev.rsadvanced.RSAdvanced.id("network_anchor")).orElseThrow();
        var processor = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("refinedstorage:advanced_processor"));
        var casing = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.parse("refinedstorage:machine_casing"));
        var ingredients = java.util.List.of(new net.minecraft.world.item.ItemStack(processor),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL),
                new net.minecraft.world.item.ItemStack(processor),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL),
                new net.minecraft.world.item.ItemStack(casing),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL),
                new net.minecraft.world.item.ItemStack(processor),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL),
                new net.minecraft.world.item.ItemStack(processor));
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, ingredients);
        @SuppressWarnings("unchecked")
        var craftingRecipe = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>) recipe.value();
        assertTrue(craftingRecipe.matches(input, level));
        assertEquals(AnchorContent.ITEM.get(), craftingRecipe.assemble(input, level.registryAccess()).getItem());
        var drops = net.minecraft.world.level.block.Block.getDrops(anchor.getBlockState(), level, position, anchor);
        assertEquals(1, drops.size());
        assertEquals(AnchorContent.ITEM.get(), drops.getFirst().getItem());

        var player = helper.makeMockServerPlayerInLevel();
        player.setPos(position.getCenter());
        var originalNetwork = anchor.node().getNetwork();
        var permission = new com.refinedmods.refinedstorage.common.security.PlatformSecurityNetworkComponentImpl(
                com.refinedmods.refinedstorage.api.network.security.SecurityPolicy.of(
                        com.refinedmods.refinedstorage.common.security.BuiltinPermission.OPEN));
        var guardedNetwork = (com.refinedmods.refinedstorage.api.network.Network) java.lang.reflect.Proxy.newProxyInstance(
                com.refinedmods.refinedstorage.api.network.Network.class.getClassLoader(),
                new Class<?>[]{com.refinedmods.refinedstorage.api.network.Network.class}, (proxy, method, arguments) -> {
                    if (method.getName().equals("getComponent") && arguments[0]
                            == com.refinedmods.refinedstorage.common.api.security.PlatformSecurityNetworkComponent.class) {
                        return permission;
                    }
                    return method.invoke(originalNetwork, arguments);
                });
        anchor.node().setNetwork(guardedNetwork);
        var menu = new dev.rsadvanced.feature.anchor.AnchorMenu(77, player.getInventory(), position);
        assertTrue(anchor.canOpen(player));
        assertTrue(!anchor.canBuild(player));
        assertTrue(!menu.clickMenuButton(player, 0));
        assertTrue(anchor.enabled());
        anchor.node().setNetwork(originalNetwork);
        assertTrue(menu.clickMenuButton(player, 0));
        assertTrue(!anchor.enabled());
        level.removeBlock(position, false);
    }

    private static com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer createContainer(
            GameTestHelper helper, BlockPos position, com.refinedmods.refinedstorage.api.network.Network network) {
        var entity = new net.minecraft.world.level.block.entity.BlockEntity(AnchorContent.ENTITY.get(), position,
                AnchorContent.BLOCK.get().defaultBlockState()) { };
        entity.setLevel(helper.getLevel());
        var node = new dev.rsadvanced.feature.anchor.AnchorNode();
        node.setNetwork(network);
        var container = com.refinedmods.refinedstorage.common.api.RefinedStorageApi.INSTANCE
                .createNetworkNodeContainer(entity, node).build();
        network.addContainer(container);
        return container;
    }
}
