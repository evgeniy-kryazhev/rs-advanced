package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.feature.mining.MiningCardMenu;
import dev.rsadvanced.feature.mining.MiningContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertFalse;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class MiningTest {
    @SharedGameTest("mining_card_slot_and_persistence")
    public static void cardSlotTransfersLocksAndSaves(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack terminal = MiningContent.ITEM.get().createAtEnergyCapacity();
            player.setItemInHand(hand, terminal);
            var reference = RefinedStorageApi.INSTANCE.createInventorySlotReference(player, hand);
            var menu = new MiningCardMenu(1, player.getInventory(), reference);
            player.containerMenu = menu;
            ItemStack card = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
            card.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getNetworkLocation(),
                    GlobalPos.of(helper.getLevel().dimension(), new BlockPos(50000, 80, 50000)));
            assertFalse(menu.getSlot(MiningCardMenu.CARD_SLOT).mayPlace(new ItemStack(Items.PAPER)));
            assertFalse(menu.getSlot(MiningCardMenu.CARD_SLOT).mayPlace(terminal));
            menu.getSlot(0).set(card.copy());
            menu.quickMoveStack(player, 0);
            assertTrue(menu.getSlot(0).getItem().isEmpty());
            assertTrue(ItemStack.matches(card, dev.rsadvanced.feature.connection.NetworkCardSlot.card(terminal)));
            // Number-key and offhand swaps cannot replace the item that owns the card slot.
            menu.clicked(MiningCardMenu.CARD_SLOT, hand == InteractionHand.MAIN_HAND ? 0 : 40,
                    ClickType.SWAP, player);
            assertTrue(reference.resolve(player).orElseThrow() == terminal);
            assertTrue(ItemStack.matches(card, dev.rsadvanced.feature.connection.NetworkCardSlot.card(terminal)));

            var saved = terminal.save(helper.getLevel().registryAccess());
            ItemStack restored = ItemStack.parse(helper.getLevel().registryAccess(), saved).orElseThrow();
            assertTrue(ItemStack.matches(card, dev.rsadvanced.feature.connection.NetworkCardSlot.card(restored)));
            assertEquals(RefinedStorageApi.INSTANCE.getEnergyStorage(terminal).orElseThrow().getStored(),
                    RefinedStorageApi.INSTANCE.getEnergyStorage(restored).orElseThrow().getStored());
            // Card readers return copies: mutating a caller's stack cannot mutate persisted contents.
            dev.rsadvanced.feature.connection.NetworkCardSlot.card(terminal).setCount(0);
            assertTrue(ItemStack.matches(card, dev.rsadvanced.feature.connection.NetworkCardSlot.card(terminal)));

            menu.quickMoveStack(player, MiningCardMenu.CARD_SLOT);
            assertTrue(dev.rsadvanced.feature.connection.NetworkCardSlot.card(terminal).isEmpty());
            assertEquals(1, player.getInventory().countItem(card.getItem()));
            menu.getSlot(MiningCardMenu.CARD_SLOT).set(card.copy());
            player.setItemInHand(hand, restored);
            assertFalse(menu.stillValid(player));
            menu.clicked(MiningCardMenu.CARD_SLOT, 0, ClickType.PICKUP, player);
            assertTrue(menu.getCarried().isEmpty());
            player.getInventory().clearContent();
            player.containerMenu = player.inventoryMenu;
        }
    }

}
