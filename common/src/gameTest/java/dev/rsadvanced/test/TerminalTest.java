package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.terminal.TerminalCardMenu;
import dev.rsadvanced.feature.terminal.TerminalContent;
import dev.rsadvanced.feature.terminal.TerminalItem;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertFalse;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class TerminalTest {
    @SharedGameTest("terminal_card_slot_and_persistence")
    public static void cardSlotTransfersLocksAndSaves(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack terminal = TerminalContent.ITEM.get().createAtEnergyCapacity();
            player.setItemInHand(hand, terminal);
            var reference = RefinedStorageApi.INSTANCE.createInventorySlotReference(player, hand);
            var menu = new TerminalCardMenu(1, player.getInventory(), reference);
            player.containerMenu = menu;
            ItemStack card = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
            card.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getNetworkLocation(),
                    GlobalPos.of(helper.getLevel().dimension(), new BlockPos(50000, 80, 50000)));
            assertFalse(menu.getSlot(TerminalCardMenu.CARD_SLOT).mayPlace(new ItemStack(Items.PAPER)));
            assertFalse(menu.getSlot(TerminalCardMenu.CARD_SLOT).mayPlace(terminal));
            menu.getSlot(0).set(card.copy());
            menu.quickMoveStack(player, 0);
            assertTrue(menu.getSlot(0).getItem().isEmpty());
            assertTrue(ItemStack.matches(card, TerminalItem.card(terminal)));
            // Number-key and offhand swaps cannot replace the item that owns the card slot.
            menu.clicked(TerminalCardMenu.CARD_SLOT, hand == InteractionHand.MAIN_HAND ? 0 : 40,
                    ClickType.SWAP, player);
            assertTrue(reference.resolve(player).orElseThrow() == terminal);
            assertTrue(ItemStack.matches(card, TerminalItem.card(terminal)));

            var saved = terminal.save(helper.getLevel().registryAccess());
            ItemStack restored = ItemStack.parse(helper.getLevel().registryAccess(), saved).orElseThrow();
            assertTrue(ItemStack.matches(card, TerminalItem.card(restored)));
            assertEquals(RefinedStorageApi.INSTANCE.getEnergyStorage(terminal).orElseThrow().getStored(),
                    RefinedStorageApi.INSTANCE.getEnergyStorage(restored).orElseThrow().getStored());
            // Card readers return copies: mutating a caller's stack cannot mutate persisted contents.
            TerminalItem.card(terminal).setCount(0);
            assertTrue(ItemStack.matches(card, TerminalItem.card(terminal)));

            menu.quickMoveStack(player, TerminalCardMenu.CARD_SLOT);
            assertTrue(TerminalItem.card(terminal).isEmpty());
            assertEquals(1, player.getInventory().countItem(card.getItem()));
            menu.getSlot(TerminalCardMenu.CARD_SLOT).set(card.copy());
            player.setItemInHand(hand, restored);
            assertFalse(menu.stillValid(player));
            menu.clicked(TerminalCardMenu.CARD_SLOT, 0, ClickType.PICKUP, player);
            assertTrue(menu.getCarried().isEmpty());
            player.getInventory().clearContent();
            player.containerMenu = player.inventoryMenu;
        }
    }

    @SharedGameTest("terminal_energy_and_recipe")
    public static void energyAndRecipeUseRsDefaults(GameTestHelper helper) {
        ItemStack terminal = new ItemStack(TerminalContent.ITEM.get());
        var energy = RefinedStorageApi.INSTANCE.getEnergyStorage(terminal).orElseThrow();
        assertEquals(0, energy.getStored());
        energy.receive(500, Action.EXECUTE);
        assertEquals(500, RefinedStorageApi.INSTANCE.getEnergyStorage(terminal).orElseThrow().getStored());
        var wirelessGrid = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getWirelessGrid());
        assertEquals(RefinedStorageApi.INSTANCE.getEnergyStorage(wirelessGrid).orElseThrow().getCapacity(), energy.getCapacity());
        var recipe = (ShapedRecipe) helper.getLevel().getRecipeManager()
                .byKey(RSAdvanced.id("network_terminal")).orElseThrow().value();
        var ingredients = new ArrayList<ItemStack>();
        for (var ingredient : recipe.getIngredients()) {
            ingredients.add(ingredient.getItems()[0].copy());
        }
        var input = CraftingInput.of(3, 3, ingredients);
        assertTrue(recipe.matches(input, helper.getLevel()));
        assertTrue(recipe.assemble(input, helper.getLevel().registryAccess()).is(TerminalContent.ITEM.get()));
    }
}
