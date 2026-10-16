package dev.rsadvanced.feature.terminal;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import dev.rsadvanced.feature.connection.NetworkCardMenu;
import net.minecraft.world.entity.player.Inventory;

public final class TerminalCardMenu extends NetworkCardMenu {
    public TerminalCardMenu(int id, Inventory inventory, SlotReference reference) {
        super(TerminalContent.CARD_MENU.get(), id, inventory, reference);
    }
}
