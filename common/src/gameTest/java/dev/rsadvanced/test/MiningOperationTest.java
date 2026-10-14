package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.rsadvanced.feature.mining.MiningContent;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningOperation;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertFalse;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class MiningOperationTest {
    @SharedGameTest("mining_without_card_uses_multitool_behavior")
    public static void minesWithoutCardThroughPlayerPackets(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MiningItem.setRadius(tool, 0);
        BlockPos center = helper.absolutePos(new BlockPos(3, 5, 3));
        player.setPos(center.getX() + 0.5, center.getY(), center.getZ() + 2);
        player.setOnGround(true);
        helper.getLevel().setBlockAndUpdate(center.east(), Blocks.STONE.defaultBlockState());
        int sequence = 0;
        for (var block : List.of(Blocks.DIRT, Blocks.OAK_LOG, Blocks.STONE)) {
            var state = block.defaultBlockState();
            assertEquals(20.0f, tool.getDestroySpeed(state));
            assertTrue(tool.isCorrectToolForDrops(state));
            helper.getLevel().setBlockAndUpdate(center, state);
            player.gameMode.handleBlockBreakAction(center,
                    net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    Direction.SOUTH, helper.getLevel().getMaxBuildHeight(), ++sequence);
            for (int tick = 0; tick < 100; tick++) {
                player.gameMode.tick();
            }
            player.gameMode.handleBlockBreakAction(center,
                    net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK,
                    Direction.SOUTH, helper.getLevel().getMaxBuildHeight(), ++sequence);
            assertTrue(helper.getLevel().getBlockState(center).isAir());
            assertTrue(helper.getLevel().getBlockState(center.east()).is(Blocks.STONE));
        }
        assertEquals(MiningItem.CAPACITY - 3 * MiningItem.BLOCK_COST,
                RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
        RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().extract(Long.MAX_VALUE, Action.EXECUTE);
        helper.getLevel().setBlockAndUpdate(center, Blocks.DIRT.defaultBlockState());
        assertFalse(player.gameMode.destroyBlock(center));
        assertTrue(helper.getLevel().getBlockState(center).is(Blocks.DIRT));
        helper.getLevel().removeBlock(center, false);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(center).inflate(1));
        for (var item : List.of(Items.DIRT, Items.OAK_LOG, Items.COBBLESTONE)) {
            assertEquals(1, drops.stream().filter(entity -> entity.getItem().is(item))
                    .mapToInt(entity -> entity.getItem().getCount()).sum());
        }
        helper.getLevel().removeBlock(center.east(), false);
    }

    @SharedGameTest("mining_creative_packet_breaks_non_pickaxe_blocks")
    public static void creativePacketBreaksSelectedBlocks(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        player.setGameMode(GameType.CREATIVE);
        // Loot suitability must not prevent creative breaking; energy is still paid without a card.
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos position = helper.absolutePos(new BlockPos(3, 5, 3));
        player.setPos(position.getX() + 0.5, position.getY(), position.getZ() + 2);
        int sequence = 0;
        for (var block : List.of(Blocks.DIRT, Blocks.OAK_LOG, Blocks.STONE, Blocks.BEDROCK)) {
            helper.getLevel().setBlockAndUpdate(position, block.defaultBlockState());
            player.gameMode.handleBlockBreakAction(position,
                    net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                    Direction.SOUTH, helper.getLevel().getMaxBuildHeight(), ++sequence);
            assertTrue(helper.getLevel().getBlockState(position).isAir());
        }
        assertEquals(MiningItem.CAPACITY - 4 * MiningItem.BLOCK_COST,
                RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
    }

    @SharedGameTest("mining_multitool_mixed_area")
    public static void survivalMinesMixedMaterialsInOneArea(GameTestHelper helper) {
        try (var fixture = new NetworkCardConnectionTest.Fixture(helper, survivalPlayer(helper))) {
            ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
            tool.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(fixture.card)));
            tool.enchant(fixture.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            MiningItem.setRadius(tool, 1);
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            var storage = fixture.network.getComponent(StorageNetworkComponent.class);
            storage.addSource(new LimitedStorageImpl(100));
            BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
            var positions = MiningOperation.area(center, Direction.UP, 1);
            var materials = List.of(Blocks.DIRT, Blocks.STONE, Blocks.OAK_LOG, Blocks.HAY_BLOCK,
                    Blocks.GLASS, Blocks.WHITE_WOOL, Blocks.COBWEB, Blocks.SAND, Blocks.OBSIDIAN);
            for (int index = 0; index < materials.size(); index++) {
                fixture.level.setBlockAndUpdate(positions.get(index), materials.get(index).defaultBlockState());
            }
            assertTrue(fixture.player.gameMode.destroyBlock(center));
            for (int index = 0; index < materials.size(); index++) {
                assertTrue(fixture.level.getBlockState(positions.get(index)).isAir());
                var material = materials.get(index);
                var expectedItem = material == Blocks.STONE ? Items.COBBLESTONE
                        : material == Blocks.COBWEB ? Items.STRING : material.asItem();
                assertEquals(material == Blocks.GLASS ? 0 : 1, storage.get(new ItemResource(expectedItem)));
            }
            assertEquals(MiningItem.CAPACITY - 9 * MiningItem.BLOCK_COST,
                    RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
        }
    }

    static net.minecraft.server.level.ServerPlayer survivalPlayer(GameTestHelper helper) {
        var mock = helper.makeMockServerPlayerInLevel();
        // Vanilla's GameTest mock hardcodes isCreative() to true, even after changing game mode.
        var player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                mock.getGameProfile(), net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = mock.connection;
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    @SharedGameTest("mining_budget_and_connection_loss")
    public static void stopsAreaWhenEnergyOrConnectionRunsOut(GameTestHelper helper) {
        try (var fixture = new NetworkCardConnectionTest.Fixture(helper, survivalPlayer(helper))) {
            ItemStack tool = new ItemStack(MiningContent.ITEM.get());
            tool.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(fixture.card)));
            MiningItem.setRadius(tool, 2);
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().receive(3 * MiningItem.BLOCK_COST, Action.EXECUTE);
            var storage = fixture.network.getComponent(StorageNetworkComponent.class);
            var ordinary = new LimitedStorageImpl(100);
            storage.addSource(ordinary);
            BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
            var area = MiningOperation.area(center, Direction.UP, 2);
            for (BlockPos pos : area) {
                fixture.level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            fixture.level.setBlockAndUpdate(area.get(1), Blocks.FURNACE.defaultBlockState());
            fixture.level.setBlockAndUpdate(area.get(2), Blocks.BEDROCK.defaultBlockState());
            fixture.level.setBlockAndUpdate(area.get(3), Blocks.DIRT.defaultBlockState());
            assertTrue(fixture.player.gameMode.destroyBlock(center));
            assertEquals(2, storage.get(new ItemResource(Items.COBBLESTONE)));
            assertEquals(1, storage.get(new ItemResource(Items.DIRT)));
            assertEquals(3, area.stream().filter(pos -> fixture.level.getBlockState(pos).isAir()).count());
            assertTrue(fixture.level.getBlockState(area.get(1)).is(Blocks.FURNACE));
            assertTrue(fixture.level.getBlockState(area.get(2)).is(Blocks.BEDROCK));
            assertTrue(fixture.level.getBlockState(area.get(3)).isAir());
            assertEquals(0, RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());

            for (BlockPos pos : area) {
                fixture.level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            storage.removeSource(ordinary);
            storage.addSource(new LimitedStorageImpl(100) {
                @Override
                public long insert(com.refinedmods.refinedstorage.api.resource.ResourceKey resource, long amount,
                                   Action action, com.refinedmods.refinedstorage.api.storage.Actor actor) {
                    long inserted = super.insert(resource, amount, action, actor);
                    if (action == Action.EXECUTE && inserted > 0) {
                        fixture.receiverNode.setActive(false);
                    }
                    return inserted;
                }
            });
            tool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), 2500L);
            assertTrue(fixture.player.gameMode.destroyBlock(center));
            assertEquals(1, area.stream().filter(pos -> fixture.level.getBlockState(pos).isAir()).count());
            assertEquals(1, storage.get(new ItemResource(Items.COBBLESTONE)));
            assertEquals(2500 - MiningItem.BLOCK_COST, RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
            for (BlockPos pos : area) {
                fixture.level.removeBlock(pos, false);
            }
        }
    }

    @SharedGameTest("mining_area_loot_and_energy")
    public static void minesActualBlocksAndPreservesOverflow(GameTestHelper helper) {
        try (var fixture = new NetworkCardConnectionTest.Fixture(helper, survivalPlayer(helper))) {
            fixture.player.setGameMode(GameType.SURVIVAL);
            ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
            tool.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(fixture.card)));
            MiningItem.setRadius(tool, 1);
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            var storage = fixture.network.getComponent(StorageNetworkComponent.class);
            storage.addSource(new LimitedStorageImpl(4));
            BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
            var area = MiningOperation.area(center, Direction.UP, 1);
            for (BlockPos pos : area) {
                fixture.level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
            assertTrue(fixture.player.gameMode.destroyBlock(center));
            for (BlockPos pos : area) {
                assertTrue(fixture.level.getBlockState(pos).isAir());
            }
            assertEquals(4, storage.get(new ItemResource(Items.COBBLESTONE)));
            assertEquals(5, fixture.player.getInventory().countItem(Items.COBBLESTONE));
            assertEquals(MiningItem.CAPACITY - 9 * MiningItem.BLOCK_COST,
                    RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
            assertEquals(0, fixture.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(center).inflate(3)).size());

            // Legacy Silk Touch is removed; the last paid block inserts its ordinary ore drop at zero charge.
            MiningItem.setRadius(tool, 0);
            tool.enchant(fixture.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
            var energy = RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow();
            energy.extract(energy.getStored() - MiningItem.BLOCK_COST, Action.EXECUTE);
            storage.addSource(new LimitedStorageImpl(10));
            fixture.level.setBlockAndUpdate(center, Blocks.DIAMOND_ORE.defaultBlockState());
            assertTrue(fixture.player.gameMode.destroyBlock(center));
            assertEquals(1, storage.get(new ItemResource(Items.DIAMOND)));
            assertFalse(tool.isEnchanted());
            assertEquals(0, RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
            fixture.level.setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
            assertFalse(fixture.player.gameMode.destroyBlock(center));
            assertTrue(fixture.level.getBlockState(center).is(Blocks.STONE));
            fixture.level.removeBlock(center, false);
        }
    }

    @SharedGameTest("mining_denial_fallback_and_planes")
    public static void refundsDeniedBlocksAndFallsBackWithoutNetwork(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        // An installed but unbound card uses the powered fallback; no-card mining is tested separately.
        tool.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(
                com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard()))));
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MiningItem.setRadius(tool, 0);
        BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
        helper.getLevel().setBlockAndUpdate(center, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(center.east(), Blocks.STONE.defaultBlockState());
        assertFalse(MiningOperation.destroy(player, center, Direction.UP, () -> false));
        assertEquals(MiningItem.CAPACITY, RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
        player.setGameMode(GameType.ADVENTURE);
        assertFalse(player.gameMode.destroyBlock(center));
        assertEquals(MiningItem.CAPACITY, RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
        player.setGameMode(GameType.SURVIVAL);
        assertTrue(player.gameMode.destroyBlock(center));
        assertTrue(helper.getLevel().getBlockState(center.east()).is(Blocks.STONE));
        assertEquals(MiningItem.CAPACITY - MiningItem.BLOCK_COST,
                RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
        for (Direction direction : Direction.values()) {
            List<BlockPos> area = MiningOperation.area(center, direction, 2);
            assertEquals(25, area.size());
            assertEquals(center, area.getFirst());
            assertEquals(25, area.stream().distinct().count());
            for (BlockPos position : area) {
                assertEquals(direction.getAxis().choose(center.getX(), center.getY(), center.getZ()),
                        direction.getAxis().choose(position.getX(), position.getY(), position.getZ()));
            }
        }
        helper.getLevel().removeBlock(center.east(), false);
    }
}
