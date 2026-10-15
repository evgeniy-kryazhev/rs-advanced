package dev.rsadvanced.feature.mining.client;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.client.radial.RadialMenuEntry;
import dev.rsadvanced.client.radial.RadialMenuModel;
import dev.rsadvanced.client.radial.RadialMenus;
import dev.rsadvanced.feature.mining.MiningBalance;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.feature.mining.MiningSelectionPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Mining commands and pending choices. No knowledge of the screen, layout or menu backend. */
public final class MiningRadialMenu {
    private final Minecraft minecraft;
    private final InteractionHand hand;
    private final int openingSlot;
    private final SlotReference reference;
    private MiningMode selectedMode;
    private int selectedRadius;
    private boolean selectedVein;

    private MiningRadialMenu(Minecraft minecraft, InteractionHand hand) {
        this.minecraft = minecraft;
        this.hand = hand;
        openingSlot = minecraft.player.getInventory().selected;
        reference = RefinedStorageApi.INSTANCE.createInventorySlotReference(minecraft.player, hand);
        ItemStack stack = minecraft.player.getItemInHand(hand);
        selectedMode = MiningItem.mode(stack);
        selectedRadius = MiningItem.radius(stack);
        selectedVein = MiningItem.veinMining(stack) && MiningBalance.settings().veinEnabled();
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
        for (MiningMode mode : MiningMode.SPEED_MODES) {
            var icon = switch (mode) {
                case NORMAL -> Items.CLOCK;
                case SLOW -> Items.FEATHER;
                case FAST -> Items.SUGAR;
                case OFF -> Items.BARRIER;
                case VEIN -> throw new IllegalArgumentException("Legacy Vein is not a speed choice");
            };
            entries.add(new RadialMenuEntry("speed_" + mode.getSerializedName(), speedLabel(mode),
                    new ItemStack(icon), () -> selectedMode == mode, () -> true, () -> {
                        selectedMode = mode;
                        sendSelection();
                    }));
        }
        for (int radius = 0; radius <= 2; radius++) {
            int selectedArea = radius;
            int size = radius * 2 + 1;
            entries.add(new RadialMenuEntry("area_" + size, Component.literal(size + "×" + size),
                    new ItemStack(Items.STONE), () -> selectedRadius == selectedArea, () -> true, () -> {
                        selectedRadius = selectedArea;
                        sendSelection();
                    }));
        }
        entries.add(new RadialMenuEntry("vein", Component.translatable("gui.rsadvanced.mining_vein"),
                new ItemStack(Items.IRON_ORE), () -> selectedVein, () -> MiningBalance.settings().veinEnabled(), () -> {
                    selectedVein = !selectedVein;
                    sendSelection();
                }));
        return new RadialMenuModel(Component.translatable("gui.rsadvanced.mining_radial"),
                entries, this::status, this::canRemainOpen);
    }

    private List<Component> status() {
        int size = selectedRadius * 2 + 1;
        return List.of(speedLabel(selectedMode),
                Component.translatable(selectedVein ? "gui.rsadvanced.mining_vein_on" : "gui.rsadvanced.mining_vein_off"),
                Component.literal(size + "×" + size));
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

    private void sendSelection() {
        if (canRemainOpen()) {
            NetworkManager.sendToServer(new MiningSelectionPayload(reference,
                    selectedMode.ordinal(), selectedRadius, selectedVein));
        }
    }

    private static Component speedLabel(MiningMode mode) {
        return Component.translatable("gui.rsadvanced.mining_mode." + mode.getSerializedName());
    }
}
