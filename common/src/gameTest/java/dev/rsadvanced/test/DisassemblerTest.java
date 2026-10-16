package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.feature.mining.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;

import static dev.rsadvanced.test.TestAssertions.*;

public final class DisassemblerTest {
    @SharedGameTest("disassembler_independent_veins_and_area")
    public static void areaSeedsDifferentVeinsAtEverySpeed(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = MiningOperationTest.survivalPlayer(helper);
        BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
        for (MiningMode speed : List.of(MiningMode.SLOW, MiningMode.NORMAL, MiningMode.FAST)) {
            for (int radius : List.of(0, 1, 2)) {
                ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
                MiningItem.setMode(tool, speed);
                MiningItem.setRadius(tool, radius);
                MiningItem.setVeinMining(tool, true);
                player.setItemInHand(InteractionHand.MAIN_HAND, tool);
                var positions = dev.rsadvanced.feature.mining.module.ToolModules.AREA.positions(center, net.minecraft.core.Direction.UP, radius);
                for (BlockPos position : positions) {
                    level.setBlockAndUpdate(position, Blocks.DIRT.defaultBlockState());
                }
                BlockPos ironSeed = center.east(radius);
                BlockPos coalExtra = center.above();
                BlockPos ironExtra = ironSeed.above();
                level.setBlockAndUpdate(center, Blocks.COAL_ORE.defaultBlockState());
                level.setBlockAndUpdate(coalExtra, Blocks.COAL_ORE.defaultBlockState());
                if (radius > 0) {
                    level.setBlockAndUpdate(ironSeed, Blocks.IRON_ORE.defaultBlockState());
                    level.setBlockAndUpdate(ironExtra, Blocks.IRON_ORE.defaultBlockState());
                }
                assertTrue(player.gameMode.destroyBlock(center));
                for (BlockPos position : positions) {
                    assertTrue(level.getBlockState(position).isAir());
                }
                assertTrue(level.getBlockState(coalExtra).isAir());
                if (radius > 0) {
                    assertTrue(level.getBlockState(ironExtra).isAir());
                }
                long expectedCost = positions.size() * MiningBalance.cost(speed, 1)
                        + (radius > 0 ? 2 : 1) * MiningBalance.veinCost(speed, 1, 1);
                assertEquals(MiningItem.CAPACITY - expectedCost, MiningItem.energy(tool));
            }
        }
    }

    @SharedGameTest("disassembler_veins_disabled_and_unpaid")
    public static void independentToggleAndInsufficientChargeStopVeins(GameTestHelper helper) {
        var player = MiningOperationTest.survivalPlayer(helper);
        BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
        for (boolean enabled : List.of(false, true)) {
            ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
            MiningItem.setMode(tool, MiningMode.FAST);
            MiningItem.setVeinMining(tool, enabled);
            long initialEnergy = enabled ? 512 : MiningItem.CAPACITY;
            tool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), initialEnergy);
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            helper.getLevel().setBlockAndUpdate(center, Blocks.COAL_ORE.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(center.above(), Blocks.COAL_ORE.defaultBlockState());
            assertTrue(player.gameMode.destroyBlock(center));
            assertEquals(initialEnergy - 512, MiningItem.energy(tool));
            assertTrue(helper.getLevel().getBlockState(center.above()).is(Blocks.COAL_ORE));
            helper.getLevel().removeBlock(center.above(), false);
        }
    }

    @SharedGameTest("disassembler_legacy_vein_migration")
    public static void legacyVeinModeRetainsAreaEnergyAndCard(GameTestHelper helper) {
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        tool.set(dev.rsadvanced.content.AdvancedComponents.MINING_MODE.get(), MiningMode.VEIN);
        MiningItem.setRadius(tool, 2);
        var card = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
        tool.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(List.of(card)));
        tool = ItemStack.parse(helper.getLevel().registryAccess(), tool.save(helper.getLevel().registryAccess()))
                .orElseThrow();
        assertEquals(MiningMode.NORMAL, MiningItem.mode(tool));
        assertTrue(MiningItem.veinMining(tool));
        MiningItem.prepare(tool);
        assertEquals(MiningMode.NORMAL, tool.get(dev.rsadvanced.content.AdvancedComponents.MINING_MODE.get()));
        assertTrue(MiningItem.veinMining(tool));
        MiningItem.setMode(tool, MiningMode.SLOW);
        assertTrue(MiningItem.veinMining(tool));
        MiningItem.setVeinMining(tool, false);
        assertEquals(MiningMode.SLOW, MiningItem.mode(tool));
        assertEquals(2, MiningItem.radius(tool));
        assertEquals(MiningItem.CAPACITY, MiningItem.energy(tool));
        assertTrue(ItemStack.matches(card, dev.rsadvanced.feature.connection.NetworkCardSlot.card(tool)));
    }

    @SharedGameTest("disassembler_server_balance_authority")
    public static void clientSnapshotCannotOverrideServerBalance(GameTestHelper helper) {
        try {
            MiningBalance.acceptServerSettings(new dev.rsadvanced.config.MiningSettings(100, 1, 1000, 1, false));
            assertEquals(dev.rsadvanced.config.RSAdvancedConfig.get().mining(), MiningBalance.settings());
            assertEquals(80, MiningBalance.cost(MiningMode.NORMAL, 1));
        } finally {
            MiningBalance.clearServerSettings();
        }
    }

    @SharedGameTest("disassembler_enchanting_table_and_anvil")
    public static void rejectsEnchantingTableAndBookApplication(GameTestHelper helper) {
        var player = MiningOperationTest.survivalPlayer(helper);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        var access = net.minecraft.world.inventory.ContainerLevelAccess.create(helper.getLevel(),
                helper.absolutePos(new BlockPos(3, 8, 3)));
        var enchanting = new net.minecraft.world.inventory.EnchantmentMenu(1, player.getInventory(), access);
        enchanting.getSlot(0).set(tool.copy());
        enchanting.getSlot(1).set(new ItemStack(net.minecraft.world.item.Items.LAPIS_LAZULI, 3));
        for (int cost : enchanting.costs) {
            assertEquals(0, cost);
        }
        var enchantment = helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.EFFICIENCY);
        ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
        net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(book,
                builder -> builder.set(enchantment, 1));
        var anvil = new net.minecraft.world.inventory.AnvilMenu(2, player.getInventory(), access);
        anvil.getSlot(0).set(tool.copy());
        anvil.getSlot(1).set(book);
        anvil.createResult();
        assertTrue(anvil.getSlot(2).getItem().isEmpty());
        assertEquals(0, tool.getItem().getEnchantmentValue());
    }

    @SharedGameTest("disassembler_modes_partial_charge_and_off")
    public static void modesRespectChargeAndRemoveLegacyEnchantments(GameTestHelper helper) {
        var player = MiningOperationTest.survivalPlayer(helper);
        ItemStack tool = new ItemStack(MiningContent.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var energy = RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow();
        assertEquals(2000, energy.receive(Long.MAX_VALUE, Action.EXECUTE));
        assertEquals(400000, energy.getCapacity());
        assertEquals(2000, RefinedStorageApi.INSTANCE.getEnergyStorage(tool).orElseThrow().getStored());
        BlockPos pos = helper.absolutePos(new BlockPos(3, 8, 3));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        for (MiningMode mode : List.of(MiningMode.NORMAL, MiningMode.SLOW, MiningMode.FAST)) {
            MiningItem.setMode(tool, mode);
            assertEquals((float) mode.speed(), tool.getDestroySpeed(Blocks.STONE.defaultBlockState()));
        }
        MiningItem.setMode(tool, MiningMode.OFF);
        assertEquals(0.0f, tool.getDestroySpeed(Blocks.STONE.defaultBlockState()));
        assertFalse(player.gameMode.destroyBlock(pos));
        assertEquals(2000, MiningItem.energy(tool));
        MiningItem.setMode(tool, MiningMode.FAST);
        tool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), 256L);
        assertEquals(10.0f, tool.getDestroySpeed(Blocks.STONE.defaultBlockState()));
        tool.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        assertTrue(player.gameMode.destroyBlock(pos));
        assertEquals(0, MiningItem.energy(tool));
        assertFalse(tool.isEnchanted());
        assertFalse(tool.getItem().isEnchantable(tool));
        helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        assertFalse(player.gameMode.destroyBlock(pos));
        helper.getLevel().removeBlock(pos, false);
    }

    @SharedGameTest("disassembler_vein_offline_and_limits")
    public static void minesDiagonalVeinsWithoutCardAndStopsAtDifferentOre(GameTestHelper helper) {
        var player = MiningOperationTest.survivalPlayer(helper);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        MiningItem.setMode(tool, MiningMode.NORMAL);
        MiningItem.setVeinMining(tool, true);
        MiningItem.setRadius(tool, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
        BlockPos diagonal = center.offset(1, 1, 1);
        BlockPos distant = center.offset(2, 2, 2);
        for (BlockPos position : List.of(center, diagonal, distant)) {
            helper.getLevel().setBlockAndUpdate(position, Blocks.COAL_ORE.defaultBlockState());
        }
        helper.getLevel().setBlockAndUpdate(center.east(), Blocks.IRON_ORE.defaultBlockState());
        var found = MiningVein.find(helper.getLevel(), center, Blocks.COAL_ORE.defaultBlockState(), 2);
        assertEquals(2, found.size());
        assertEquals(1, found.get(0).distance());
        assertEquals(2, found.get(1).distance());
        assertEquals(1, MiningVein.find(helper.getLevel(), center, Blocks.COAL_ORE.defaultBlockState(), 1).size());
        assertTrue(player.gameMode.destroyBlock(center));
        assertTrue(helper.getLevel().getBlockState(diagonal).isAir());
        assertTrue(helper.getLevel().getBlockState(distant).isAir());
        assertTrue(helper.getLevel().getBlockState(center.east()).is(Blocks.IRON_ORE));
        assertEquals(MiningItem.CAPACITY - 80 - 40 - 114, MiningItem.energy(tool));
        assertEquals(3, helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(center).inflate(4)).stream()
                .filter(entity -> entity.getItem().is(net.minecraft.world.item.Items.COAL))
                .mapToInt(entity -> entity.getItem().getCount()).sum());
        helper.getLevel().removeBlock(center.east(), false);
    }

    @SharedGameTest("disassembler_offline_area_and_selection_validation")
    public static void offlineAreaAndServerSelectionWork(GameTestHelper helper) {
        var player = MiningOperationTest.survivalPlayer(helper);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var slot = RefinedStorageApi.INSTANCE.createInventorySlotReference(player, InteractionHand.MAIN_HAND);
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("unknown"), "normal")));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("area"), "99")));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("speed"), "vein")));
        assertTrue(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("speed"), "fast")));
        assertTrue(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("area"), "5")));
        assertTrue(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("vein"), "toggle")));
        assertEquals(MiningMode.FAST, MiningItem.mode(tool));
        assertEquals(2, MiningItem.radius(tool));
        assertTrue(MiningItem.veinMining(tool));
        assertTrue(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("speed"), "slow")));
        assertTrue(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("area"), "3")));
        assertTrue(MiningItem.veinMining(tool));
        assertTrue(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("vein"), "toggle")));
        assertEquals(MiningMode.SLOW, MiningItem.mode(tool));
        assertFalse(MiningItem.veinMining(tool));
        BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
        for (BlockPos pos : dev.rsadvanced.feature.mining.module.ToolModules.AREA.positions(center, net.minecraft.core.Direction.UP, 1)) {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
        assertTrue(player.gameMode.destroyBlock(center));
        assertEquals(MiningItem.CAPACITY - 9 * 32, MiningItem.energy(tool));
        for (BlockPos pos : dev.rsadvanced.feature.mining.module.ToolModules.AREA.positions(center, net.minecraft.core.Direction.UP, 1)) {
            assertTrue(helper.getLevel().getBlockState(pos).isAir());
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STICK));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(slot, dev.rsadvanced.RSAdvanced.id("speed"), "normal")));
    }

    @SharedGameTest("mining_tool_uses_pickaxe_attack_without_energy_cost")
    public static void ordinaryPickaxeAttackDoesNotDependOnCharge(GameTestHelper helper) {
        var player = MiningOperationTest.survivalPlayer(helper);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        ItemStack pickaxe = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        assertEquals(pickaxe.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS),
                tool.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS));
        for (long charge : new long[] {MiningItem.CAPACITY, 400L, 0L}) {
            tool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), charge);
            for (int tick = 0; tick < 25; tick++) {
                // Equipment attributes are applied in the player's full entity tick, not ServerPlayer.tick().
                player.doTick();
            }
            assertEquals(5.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            assertTrue(Math.abs(player.getAttributeValue(Attributes.ATTACK_SPEED) - 1.2) < 0.001);
            var target = EntityType.COW.create(helper.getLevel());
            target.setPos(player.position().add(1, 0, 0));
            helper.getLevel().addFreshEntity(target);
            float initialHealth = target.getHealth();
            player.setOnGround(true);
            player.fallDistance = 0;
            player.attack(target);
            assertEquals(initialHealth - 5.0f, target.getHealth());
            assertEquals(charge, MiningItem.energy(tool));
            target.discard();
        }
    }
}
