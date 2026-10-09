package dev.rsadvanced.feature.anchor;

import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.api.support.network.AbstractNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class AnchorBlockEntity extends AbstractNetworkNodeContainerBlockEntity<AnchorNode> implements MenuProvider {
    private UUID instanceId = UUID.randomUUID();
    private boolean enabled = true;
    private AnchorStatus status = AnchorStatus.NO_NETWORK;
    private int areaSize;
    private int heldSize;
    private long cost;
    private boolean leader;

    public AnchorBlockEntity(BlockPos position, BlockState state) {
        super(AnchorContent.ENTITY.get(), position, state, new AnchorNode());
        mainNetworkNode.bind(this);
    }

    public AnchorNode node() {
        return mainNetworkNode;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public boolean enabled() {
        return enabled;
    }

    public AnchorStatus status() {
        return status;
    }

    public int areaSize() {
        return areaSize;
    }

    public int heldSize() {
        return heldSize;
    }

    public long cost() {
        return cost;
    }

    public boolean leader() {
        return leader;
    }

    public boolean canOpen(ServerPlayer player) {
        // An unconnected block can be configured; connected blocks use the network's security manager.
        return mainNetworkNode.getNetwork() == null || SecurityHelper.isAllowed(player, BuiltinPermission.OPEN, mainNetworkNode);
    }

    public boolean canBuild(ServerPlayer player) {
        return mainNetworkNode.getNetwork() == null || SecurityHelper.isAllowed(player, BuiltinPermission.BUILD, mainNetworkNode);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        setChanged();
        if (mainNetworkNode.getNetwork() != null) {
            mainNetworkNode.getNetwork().getComponent(AnchorNetworkComponent.class).invalidate();
        }
        if (!enabled) {
            AnchorManager.release(this);
            update(AnchorStatus.DISABLED, areaSize, 0, cost, false);
        }
    }

    public void update(AnchorStatus status, int areaSize, int heldSize, long cost, boolean leader) {
        this.status = status;
        this.areaSize = areaSize;
        this.heldSize = heldSize;
        this.cost = cost;
        this.leader = leader;
        boolean active = status == AnchorStatus.ACTIVE || status == AnchorStatus.RESERVE;
        mainNetworkNode.setActive(active);
        mainNetworkNode.reportCost(leader && active ? cost : 0);
        if (level instanceof ServerLevel && getBlockState().getValue(AnchorBlock.ACTIVE) != active) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(AnchorBlock.ACTIVE, active));
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level instanceof ServerLevel) {
            AnchorManager.loaded(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel) {
            AnchorManager.unloaded(this);
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("AnchorInstance", instanceId);
        tag.putBoolean("Enabled", enabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("AnchorInstance")) {
            instanceId = tag.getUUID("AnchorInstance");
        }
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rsadvanced.network_anchor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AnchorMenu(id, inventory, worldPosition);
    }
}
