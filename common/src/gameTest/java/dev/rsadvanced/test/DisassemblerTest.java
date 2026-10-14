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
    @SharedGameTest("disassembler_server_balance_authority")
    public static void clientSnapshotCannotOverrideServerBalance(GameTestHelper helper) {
        try {
            MiningBalance.acceptServerSettings(new dev.rsadvanced.config.MiningSettings(100, 1, 1000, 1, 1, false));
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
        MiningItem.setMode(tool, MiningMode.VEIN);
        MiningItem.setRadius(tool, 2);
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
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(slot, 99, 1)));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(slot, 0, 99)));
        assertTrue(MiningNetworking.select(player, new MiningSelectionPayload(slot, MiningMode.SLOW.ordinal(), 1)));
        BlockPos center = helper.absolutePos(new BlockPos(3, 8, 3));
        for (BlockPos pos : MiningOperation.area(center, net.minecraft.core.Direction.UP, 1)) {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
        assertTrue(player.gameMode.destroyBlock(center));
        assertEquals(MiningItem.CAPACITY - 9 * 32, MiningItem.energy(tool));
        for (BlockPos pos : MiningOperation.area(center, net.minecraft.core.Direction.UP, 1)) {
            assertTrue(helper.getLevel().getBlockState(pos).isAir());
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STICK));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(slot, 0, 1)));
    }

    @SharedGameTest("disassembler_weapon_live_charge")
    public static void weaponUsesCurrentChargeAndSpendsEnergyOnHit(GameTestHelper helper) {
        var player = MiningOperationTest.survivalPlayer(helper);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        MiningCombat.prepareAttack(player);
        assertEquals(21.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        var target = EntityType.COW.create(helper.getLevel());
        target.setHealth(10);
        helper.getLevel().addFreshEntity(target);
        for (int tick = 0; tick < 25; tick++) {
            player.tick();
        }
        player.attack(target);
        assertEquals(MiningItem.CAPACITY - 800, MiningItem.energy(tool));
        tool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), 400L);
        MiningCombat.prepareAttack(player);
        assertEquals(13.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        tool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), 0L);
        MiningCombat.prepareAttack(player);
        assertEquals(5.0, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
        target.discard();
    }
}
