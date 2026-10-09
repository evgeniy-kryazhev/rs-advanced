package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken;
import com.refinedmods.refinedstorage.api.network.autocrafting.AutocraftingNetworkComponent;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.autocrafting.CraftingPatternState;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternGridBlockEntity;
import com.refinedmods.refinedstorage.common.autocrafting.patterngrid.PatternType;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import java.util.ArrayList;
import com.refinedmods.refinedstorage.common.autocrafting.autocrafter.AutocrafterBlockEntity;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;

/** Configures a real RS autocrafter through its public menu slots. */
public final class AnchorAutocraftingCheck {
    public static void configure(ServerLevel level, BlockPos position) {
        var crafter = (AutocrafterBlockEntity) level.getBlockEntity(position);
        var matrix = new ArrayList<ItemStack>();
        for (int slot = 0; slot < 9; slot++) {
            matrix.add(slot == 4 ? ItemStack.EMPTY : new ItemStack(Items.COBBLESTONE));
        }
        var pattern = PatternGridBlockEntity.createPatternStack(PatternType.CRAFTING);
        pattern.set(DataComponents.INSTANCE.getCraftingPatternState(),
                new CraftingPatternState(false, CraftingInput.ofPositioned(3, 3, matrix)));
        ServerPlayer player = new ServerPlayer(level.getServer(), level,
                new GameProfile(UUID.randomUUID(), "anchor-pattern-setup"), ClientInformation.createDefault());
        // The player is never added to the world: no-player ticking remains under test.
        var menu = crafter.createMenu(0, player.getInventory(), player);
        menu.getSlot(0).set(pattern);
    }

    public static long start(ServerLevel level, BlockPos position) {
        var crafter = (AutocrafterBlockEntity) level.getBlockEntity(position);
        var network = crafter.getContainerProvider().getContainers().iterator().next().getNode().getNetwork();
        var output = ItemResource.ofItemStack(new ItemStack(Items.FURNACE));
        long before = network.getComponent(StorageNetworkComponent.class).get(output);
        if (network.getComponent(AutocraftingNetworkComponent.class)
                .startTask(output, 1, Actor.EMPTY, false, CancellationToken.NONE).isEmpty()) {
            var node = (com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode)
                    crafter.getContainerProvider().getContainers().iterator().next().getNode();
            throw new AssertionError("Autocrafting task could not be scheduled: active=" + node.isActive()
                    + ", patterns=" + network.getComponent(AutocraftingNetworkComponent.class).getPatterns().size()
                    + ", cobble=" + network.getComponent(StorageNetworkComponent.class)
                    .get(ItemResource.ofItemStack(new ItemStack(Items.COBBLESTONE))));
        }
        return before;
    }

    public static void verify(ServerLevel level, BlockPos position, long before) {
        var crafter = (AutocrafterBlockEntity) level.getBlockEntity(position);
        var network = crafter.getContainerProvider().getContainers().iterator().next().getNode().getNetwork();
        long after = network.getComponent(StorageNetworkComponent.class)
                .get(ItemResource.ofItemStack(new ItemStack(Items.FURNACE)));
        if (after != before + 1) {
            throw new AssertionError("Autocrafting output: before=" + before + ", after=" + after
                    + ", tasks=" + network.getComponent(AutocraftingNetworkComponent.class).getStatuses());
        }
    }
}
