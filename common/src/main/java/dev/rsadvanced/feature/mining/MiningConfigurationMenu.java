package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A server-owned inspection menu. Its hidden slot synchronizes the held tool without allowing transfers. */
public final class MiningConfigurationMenu extends AbstractContainerMenu {
    private final SlotReference reference;
    private final ItemStack openingTool;

    public MiningConfigurationMenu(int id, Inventory inventory, SlotReference reference, boolean offhand) {
        super(MiningContent.CONFIGURATION_MENU.get(), id);
        this.reference = reference;
        openingTool = reference.resolve(inventory.player).orElse(ItemStack.EMPTY);
        addSlot(new Slot(inventory, offhand ? 40 : inventory.selected, -10000, -10000) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
    }

    public ItemStack tool() {
        return getSlot(0).getItem();
    }

    public SlotReference reference() {
        return reference;
    }

    @Override
    public boolean stillValid(Player player) {
        ItemStack current = reference.resolve(player).orElse(ItemStack.EMPTY);
        return current.getItem() instanceof MiningItem
                && (current == player.getMainHandItem() || current == player.getOffhandItem())
                && (player.level().isClientSide() || current == openingTool);
    }

    @Override
    public void clicked(int slot, int button, ClickType clickType, Player player) {
        // Inspection has no inventory interactions, including hotbar swaps and dropping the held stack.
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
