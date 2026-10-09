package dev.rsadvanced.feature.anchor;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Vanilla menu button packets carry actions, with distance and RS permissions checked again server-side. */
public final class AnchorMenu extends AbstractContainerMenu {
    public final BlockPos position;
    public AnchorStatus status = AnchorStatus.NO_NETWORK;
    public int areaSize;
    public int heldSize;
    public long cost;
    public boolean leader;
    public boolean enabled;
    public boolean visualizing;
    private AnchorStatePayload lastSent;

    public AnchorMenu(int id, Inventory inventory, BlockPos position) {
        super(AnchorContent.MENU.get(), id);
        this.position = position;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5) <= 64
                && player.level().getBlockEntity(position) instanceof AnchorBlockEntity anchor
                && (!(player instanceof ServerPlayer serverPlayer) || anchor.canOpen(serverPlayer));
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player)
                || !(player.level().getBlockEntity(position) instanceof AnchorBlockEntity anchor)) {
            return false;
        }
        if (button == 0 && anchor.canBuild(serverPlayer)) {
            anchor.setEnabled(!anchor.enabled());
            return true;
        }
        if (button == 1) {
            AnchorNetworking.toggleVisualization(serverPlayer, anchor);
            return true;
        }
        return false;
    }

    public void synchronize(ServerPlayer player) {
        if (!(player.level().getBlockEntity(position) instanceof AnchorBlockEntity anchor)) {
            return;
        }
        AnchorStatePayload payload = AnchorStatePayload.forMenu(containerId, anchor,
                AnchorNetworking.isVisualizing(player, anchor.instanceId()));
        if (!payload.equals(lastSent)) {
            dev.architectury.networking.NetworkManager.sendToPlayer(player, payload);
            lastSent = payload;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
