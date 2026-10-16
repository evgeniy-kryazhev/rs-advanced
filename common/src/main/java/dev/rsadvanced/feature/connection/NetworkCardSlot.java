package dev.rsadvanced.feature.connection;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Vanilla container components provide saving, synchronization and copy isolation. */
public final class NetworkCardSlot {
    private NetworkCardSlot() {
    }

    public static ItemStack card(ItemStack device) {
        return device.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    public static boolean isNetworkCard(ItemStack stack) {
        return stack.is(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
    }
}
