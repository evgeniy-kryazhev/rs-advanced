package dev.rsadvanced.feature.mining.client;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.client.radial.RadialMenuEntry;
import dev.rsadvanced.client.radial.RadialMenuModel;
import dev.rsadvanced.client.radial.RadialMenus;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningSelectionPayload;
import dev.rsadvanced.feature.mining.module.ToolModuleContainer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

/** Adapts installed module commands to any radial backend; it knows no individual module settings. */
public final class MiningRadialMenu {
    private final Minecraft minecraft;
    private final InteractionHand hand;
    private final int openingSlot;
    private final SlotReference reference;
    private final ToolModuleContainer preview;

    private MiningRadialMenu(Minecraft minecraft, InteractionHand hand) {
        this.minecraft = minecraft;
        this.hand = hand;
        openingSlot = minecraft.player.getInventory().selected;
        reference = RefinedStorageApi.INSTANCE.createInventorySlotReference(minecraft.player, hand);
        // Pending choices never mutate the live client stack before the server accepts the command.
        preview = MiningItem.modules(minecraft.player.getItemInHand(hand).copy());
    }

    public static void open(InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getItemInHand(hand).getItem() instanceof MiningItem)) {
            return;
        }
        RadialMenus.open(new MiningRadialMenu(minecraft, hand).model());
    }

    private RadialMenuModel model() {
        List<RadialMenuEntry> entries = new ArrayList<>();
        for (var module : preview.installed()) {
            for (var option : module.options(preview)) {
                entries.add(new RadialMenuEntry(module.id() + "/" + option.id(), option.label(), option.icon(),
                        () -> module.selectedOption(preview).equals(option.id()),
                        () -> module.options(preview).stream().anyMatch(choice ->
                                choice.id().equals(option.id()) && choice.enabled()),
                        () -> sendSelection(module.id(), option.id())));
            }
        }
        return new RadialMenuModel(Component.translatable("gui.rsadvanced.mining_radial"),
                entries, preview::status, this::canRemainOpen);
    }

    private boolean canRemainOpen() {
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }
        if (hand == InteractionHand.MAIN_HAND && minecraft.player.getInventory().selected != openingSlot) {
            return false;
        }
        return minecraft.player.getItemInHand(hand).getItem() instanceof MiningItem;
    }

    private void sendSelection(ResourceLocation module, String option) {
        if (canRemainOpen() && preview.select(module, option)) {
            NetworkManager.sendToServer(new MiningSelectionPayload(reference, module, option));
        }
    }
}
