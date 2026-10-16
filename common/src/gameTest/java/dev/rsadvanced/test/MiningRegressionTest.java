package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.rsadvanced.config.MiningSettings;
import dev.rsadvanced.config.RSAdvancedConfig;
import dev.rsadvanced.feature.mining.MiningBalance;
import dev.rsadvanced.feature.mining.MiningContent;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.feature.mining.MiningOperation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertFalse;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class MiningRegressionTest {
    @SharedGameTest("mining_nested_operations_preserve_checks_energy_and_route")
    public static void nestedOperationsKeepTheirChecksAndRestoreTheOuterRoute(GameTestHelper helper) {
        try (var fixture = new NetworkCardConnectionTest.Fixture(helper, MiningOperationTest.survivalPlayer(helper))) {
            ItemStack outerTool = MiningContent.ITEM.get().createAtEnergyCapacity();
            outerTool.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(fixture.card)));
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, outerTool);
            var nestedPlayer = MiningOperationTest.survivalPlayer(helper);
            ItemStack nestedTool = new ItemStack(MiningContent.ITEM.get());
            nestedPlayer.setItemInHand(InteractionHand.MAIN_HAND, nestedTool);
            var storage = fixture.network.getComponent(StorageNetworkComponent.class);
            storage.addSource(new LimitedStorageImpl(100));
            BlockPos outer = helper.absolutePos(new BlockPos(3, 6, 3));
            BlockPos nested = outer.east(2);
            BlockPos samePlayer = outer.north(2);
            BlockPos failed = outer.south(2);
            for (BlockPos position : List.of(outer, nested, samePlayer, failed)) {
                fixture.level.setBlockAndUpdate(position, Blocks.STONE.defaultBlockState());
            }
            boolean[] invoked = {false};
            BlockEvent.Break listener = (level, position, state, player, experience) -> {
                if (player != fixture.player || !position.equals(outer)) {
                    return EventResult.pass();
                }
                invoked[0] = true;
                MiningItem.setMode(nestedTool, MiningMode.OFF);
                assertFalse(nestedPlayer.gameMode.destroyBlock(nested));
                MiningItem.setMode(nestedTool, MiningMode.NORMAL);
                assertFalse(nestedPlayer.gameMode.destroyBlock(nested));
                assertTrue(fixture.level.getBlockState(nested).is(Blocks.STONE));

                ItemStack chargedTool = MiningContent.ITEM.get().createAtEnergyCapacity();
                nestedPlayer.setItemInHand(InteractionHand.MAIN_HAND, chargedTool);
                assertTrue(nestedPlayer.gameMode.destroyBlock(nested));
                assertEquals(MiningItem.CAPACITY - MiningItem.BLOCK_COST, MiningItem.energy(chargedTool));
                assertTrue(fixture.player.gameMode.destroyBlock(samePlayer));

                // Failure before removal must refund the nested payment and restore the enclosing drop route.
                var failure = new IllegalStateException("Expected nested regression-test failure");
                try {
                    MiningOperation.destroy(nestedPlayer, failed, Direction.UP, () -> {
                        throw failure;
                    });
                    throw new AssertionError("Nested destruction must propagate its failure");
                } catch (IllegalStateException exception) {
                    assertTrue(exception == failure);
                }
                assertEquals(MiningItem.CAPACITY - MiningItem.BLOCK_COST, MiningItem.energy(chargedTool));
                return EventResult.pass();
            };
            BlockEvent.BREAK.register(listener);
            try {
                assertTrue(fixture.player.gameMode.destroyBlock(outer));
                assertTrue(invoked[0]);
                assertEquals(2, storage.get(new ItemResource(Items.COBBLESTONE)));
                assertEquals(MiningItem.CAPACITY - 2 * MiningItem.BLOCK_COST, MiningItem.energy(outerTool));
                assertTrue(fixture.level.getBlockState(failed).is(Blocks.STONE));
                assertTrue(fixture.level.getBlockState(samePlayer).isAir());
                // A finished operation must leave no delegation which could bypass OFF afterwards.
                MiningItem.setMode(outerTool, MiningMode.OFF);
                assertFalse(fixture.player.gameMode.destroyBlock(failed));
            } finally {
                BlockEvent.BREAK.unregister(listener);
                for (BlockPos position : List.of(outer, nested, samePlayer, failed)) {
                    fixture.level.removeBlock(position, false);
                }
            }
        }
    }

    @SharedGameTest("mining_vein_uses_alternative_parent_after_denial")
    public static void veinUsesAnAlternativeSuccessfulParent(GameTestHelper helper) {
        checkProtectedVein(helper, false);
    }

    @SharedGameTest("mining_vein_uses_longer_detour_and_actual_cost")
    public static void veinUsesALongerSuccessfulDetour(GameTestHelper helper) {
        checkProtectedVein(helper, true);
    }

    private static void checkProtectedVein(GameTestHelper helper, boolean longerDetour) {
        var player = MiningOperationTest.survivalPlayer(helper);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        MiningItem.setVeinMining(tool, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos origin = helper.absolutePos(new BlockPos(5, 5, 4));
        BlockPos denied = origin.offset(-1, 1, 0);
        BlockPos unreachable = origin.offset(-2, 0, 0);
        List<BlockPos> accessible = longerDetour
                ? List.of(origin.offset(1, 1, 0), origin.offset(1, 2, 0), origin.offset(0, 3, 0),
                        origin.offset(-1, 3, 0), origin.offset(-2, 2, 0))
                : List.of(origin.offset(1, 1, 0), origin.offset(0, 2, 0));
        List<BlockPos> allPositions = new ArrayList<>(accessible);
        allPositions.addAll(List.of(origin, denied, unreachable));
        for (BlockPos position : allPositions) {
            helper.getLevel().setBlockAndUpdate(position, Blocks.COAL_ORE.defaultBlockState());
        }
        int[] denials = {0};
        BlockEvent.Break listener = (level, position, state, breakingPlayer, experience) -> {
            if (breakingPlayer == player && position.equals(denied)) {
                denials[0]++;
                return EventResult.interruptFalse();
            }
            return EventResult.pass();
        };
        BlockEvent.BREAK.register(listener);
        try {
            assertTrue(player.gameMode.destroyBlock(origin));
            assertEquals(1, denials[0]);
            assertTrue(helper.getLevel().getBlockState(origin).isAir());
            long expectedCost = MiningItem.BLOCK_COST;
            for (int index = 0; index < accessible.size(); index++) {
                assertTrue(helper.getLevel().getBlockState(accessible.get(index)).isAir());
                expectedCost += MiningBalance.veinCost(MiningMode.NORMAL, 3, index + 1);
            }
            assertEquals(MiningItem.CAPACITY - expectedCost, MiningItem.energy(tool));
            assertTrue(helper.getLevel().getBlockState(denied).is(Blocks.COAL_ORE));
            assertTrue(helper.getLevel().getBlockState(unreachable).is(Blocks.COAL_ORE));
            assertEquals(accessible.size() + 1, helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    new AABB(origin).inflate(5)).stream().filter(entity -> entity.getItem().is(Items.COAL))
                    .mapToInt(entity -> entity.getItem().getCount()).sum());
        } finally {
            BlockEvent.BREAK.unregister(listener);
            for (BlockPos position : allPositions) {
                helper.getLevel().removeBlock(position, false);
            }
        }
    }

    @SharedGameTest("mining_fast_vein_configuration_overflow")
    public static void rejectsOverflowingFastCostsAndCalculatesSupportedCosts() {
        boolean rejected = false;
        try {
            new MiningSettings(Long.MAX_VALUE, 2000, 1_000_000_000_000L, 4096, true);
        } catch (IllegalArgumentException exception) {
            rejected = true;
        }
        assertTrue(rejected);
        var original = RSAdvancedConfig.get();
        var supported = new MiningSettings(Long.MAX_VALUE, 2000, 100_000_000_000L, 4096, true);
        try {
            RSAdvancedConfig.load(() -> new RSAdvancedConfig(original.anchorBaseCost(),
                    original.anchorChunkCostMultiplier(), original.anchorMaxChunks(), original.anchorRandomTicks(),
                    original.anchorVisualizationDistance(), supported));
            assertEquals(1_677_721_600_000_000_000L, MiningBalance.veinCost(MiningMode.FAST, 3, 4096));
            for (MiningMode mode : List.of(MiningMode.SLOW, MiningMode.NORMAL, MiningMode.FAST)) {
                assertTrue(MiningBalance.veinCost(mode, 3, 4096) > 0);
                assertTrue(MiningBalance.veinCost(mode, 3, 4096) < Long.MAX_VALUE);
            }
        } finally {
            RSAdvancedConfig.load(() -> original);
        }
    }
}
