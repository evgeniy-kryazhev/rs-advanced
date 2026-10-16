package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.network.InfiniteResourceMenu;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceContainer;
import com.refinedmods.refinedstorage.common.storage.StorageConfigurationContainer;
import com.refinedmods.refinedstorage.common.storage.storageblock.StorageBlockContainerMenu;
import com.refinedmods.refinedstorage.common.support.containermenu.ResourceSlot;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Reuses the RS client screen while checking current server permissions for every configuration change. */
public final class InfiniteStorageBlockMenu extends StorageBlockContainerMenu implements InfiniteResourceMenu {
    private final BooleanSupplier canConfigure;
    private Set<ResourceKey> infiniteResources = Set.of();
    private boolean resourcesSynchronized;

    @Override
    public Set<ResourceKey> rsadvanced$getInfiniteResources() {
        return infiniteResources;
    }

    @Override
    public void rsadvanced$setInfiniteResources(Set<ResourceKey> resources) {
        infiniteResources = Set.copyOf(resources);
        resourcesSynchronized = false;
    }

    @Override
    public void rsadvanced$synchronize(ServerPlayer player) {
        if (!resourcesSynchronized) {
            RSAdvanced.sendInfiniteResources(player, new InfiniteResourcesPayload(containerId, infiniteResources));
            resourcesSynchronized = true;
        }
    }

    @Override
    public double getProgress() {
        return infiniteResources.isEmpty() ? super.getProgress() : 1.0;
    }

    public InfiniteStorageBlockMenu(MenuType<?> type, int syncId, Player player, ResourceContainer filters,
            StorageConfigurationContainer configuration, Predicate<Player> stillValid, BooleanSupplier canConfigure) {
        super(type, syncId, player, filters, configuration, stillValid);
        this.canConfigure = canConfigure;
    }

    @Override
    public void receivePropertyChangeFromClient(ResourceLocation id, int newValue) {
        if (canConfigure.getAsBoolean()) {
            super.receivePropertyChangeFromClient(id, newValue);
        }
    }

    @Override
    public void handleResourceSlotUpdate(int slotIndex, ResourceAmount resourceAmount) {
        if (canConfigure.getAsBoolean()) {
            super.handleResourceSlotUpdate(slotIndex, resourceAmount);
        }
    }

    @Override
    public void handleResourceFilterSlotUpdate(int slotIndex, PlatformResourceKey resource) {
        if (canConfigure.getAsBoolean()) {
            super.handleResourceFilterSlotUpdate(slotIndex, resource);
        }
    }

    @Override
    public void handleResourceSlotChange(int slotIndex, boolean tryAlternatives) {
        if (canConfigure.getAsBoolean()) {
            super.handleResourceSlotChange(slotIndex, tryAlternatives);
        }
    }

    @Override
    public void handleResourceSlotAmountChange(int slotIndex, long amount) {
        if (canConfigure.getAsBoolean()) {
            super.handleResourceSlotAmountChange(slotIndex, amount);
        }
    }

    @Override
    public void addToResourceSlotIfNotExisting(ItemStack stack) {
        if (canConfigure.getAsBoolean()) {
            super.addToResourceSlotIfNotExisting(stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!canConfigure.getAsBoolean()) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        if (slotIndex >= 0 && slotIndex < slots.size()
                && slots.get(slotIndex) instanceof ResourceSlot && !canConfigure.getAsBoolean()) {
            return;
        }
        super.clicked(slotIndex, button, clickType, player);
    }
}
