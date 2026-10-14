package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.feature.terminal.TerminalContent;
import dev.rsadvanced.feature.terminal.TerminalItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

/** Included in the existing separate-JVM restart harness, in a chest outside automation. */
final class TerminalRestartCheck {
    private static final GlobalPos RECEIVER = GlobalPos.of(Level.NETHER, new BlockPos(50000, 80, 50000));

    private TerminalRestartCheck() {
    }

    static void seed(ServerLevel level, BlockPos position) {
        level.setBlockAndUpdate(position, Blocks.CHEST.defaultBlockState());
        ItemStack terminal = new ItemStack(TerminalContent.ITEM.get());
        ItemStack card = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
        card.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getNetworkLocation(), RECEIVER);
        terminal.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(card)));
        RefinedStorageApi.INSTANCE.getEnergyStorage(terminal).orElseThrow().receive(321, Action.EXECUTE);
        ((ChestBlockEntity) level.getBlockEntity(position)).setItem(0, terminal);
        ItemStack miningTool = new ItemStack(dev.rsadvanced.feature.mining.MiningContent.ITEM.get());
        miningTool.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(card)));
        dev.rsadvanced.feature.mining.MiningItem.setRadius(miningTool, 2);
        dev.rsadvanced.feature.mining.MiningItem.setMode(miningTool, dev.rsadvanced.feature.mining.MiningMode.FAST);
        RefinedStorageApi.INSTANCE.getEnergyStorage(miningTool).orElseThrow().receive(789, Action.EXECUTE);
        ((ChestBlockEntity) level.getBlockEntity(position)).setItem(1, miningTool);
    }

    static void verify(ServerLevel level, BlockPos position) {
        ItemStack miningTool = ((ChestBlockEntity) level.getBlockEntity(position)).getItem(1);
        TestAssertions.assertTrue(miningTool.is(dev.rsadvanced.feature.mining.MiningContent.ITEM.get()));
        TestAssertions.assertEquals(2, dev.rsadvanced.feature.mining.MiningItem.radius(miningTool));
        TestAssertions.assertEquals(dev.rsadvanced.feature.mining.MiningMode.FAST,
                dev.rsadvanced.feature.mining.MiningItem.mode(miningTool));
        TestAssertions.assertEquals(789, RefinedStorageApi.INSTANCE.getEnergyStorage(miningTool).orElseThrow().getStored());
        TestAssertions.assertEquals(RECEIVER, dev.rsadvanced.feature.connection.NetworkCardSlot.card(miningTool)
                .get(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getNetworkLocation()));
        ItemStack terminal = ((ChestBlockEntity) level.getBlockEntity(position)).getItem(0);
        TestAssertions.assertTrue(terminal.is(TerminalContent.ITEM.get()));
        ItemStack card = TerminalItem.card(terminal);
        TestAssertions.assertTrue(TerminalItem.isNetworkCard(card));
        TestAssertions.assertEquals(RECEIVER,
                card.get(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getNetworkLocation()));
        TestAssertions.assertEquals(321, RefinedStorageApi.INSTANCE.getEnergyStorage(terminal).orElseThrow().getStored());
    }
}
