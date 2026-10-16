package dev.rsadvanced.feature.connection;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.support.AbstractBaseContainerMenu;
import com.refinedmods.refinedstorage.common.support.FilteredContainer;
import com.refinedmods.refinedstorage.common.support.containermenu.ValidatedSlot;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** A real, persistent card slot. RS handles inventory locking and shift-click transfers. */
public class NetworkCardMenu extends AbstractBaseContainerMenu {
    public static final int CARD_SLOT = 36;
    private final ItemStack device;
    private final FilteredContainer cardInventory = new FilteredContainer(1, NetworkCardSlot::isNetworkCard);

    public NetworkCardMenu(net.minecraft.world.inventory.MenuType<?> type, int id, Inventory inventory, SlotReference slotReference) {
        super(type, id);
        disabledSlot = slotReference;
        device = slotReference.resolve(inventory.player).orElse(ItemStack.EMPTY);
        cardInventory.setItem(0, NetworkCardSlot.card(device));
        addPlayerInventory(inventory, 8, 55);
        addSlot(new ValidatedSlot(cardInventory, 0, 8, 20, NetworkCardSlot::isNetworkCard));
        transferManager.addBiTransfer(inventory, cardInventory);
        if (!inventory.player.level().isClientSide()) {
            cardInventory.addListener(container -> {
                if (ownsDevice(inventory.player)) {
                    device.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(container.getItem(0))));
                    inventory.setChanged();
                }
            });
        }
    }

    private boolean ownsDevice(Player player) {
        return !device.isEmpty()
                && disabledSlot.resolve(player).orElse(ItemStack.EMPTY) == device;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().isClientSide() || ownsDevice(player);
    }

    @Override
    public void clicked(int slot, int button, ClickType clickType, Player player) {
        if (stillValid(player)) {
            super.clicked(slot, button, clickType, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        if (!stillValid(player)) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, slot);
    }
}
