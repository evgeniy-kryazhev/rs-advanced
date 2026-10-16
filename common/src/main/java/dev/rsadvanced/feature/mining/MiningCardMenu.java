package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import dev.rsadvanced.feature.connection.NetworkCardMenu;
import net.minecraft.world.entity.player.Inventory;

public final class MiningCardMenu extends NetworkCardMenu {
    public MiningCardMenu(int id, Inventory inventory, SlotReference reference) {
        super(MiningContent.CARD_MENU.get(), id, inventory, reference);
    }
}
