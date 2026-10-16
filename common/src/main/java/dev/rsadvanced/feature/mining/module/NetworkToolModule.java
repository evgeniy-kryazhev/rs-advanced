package dev.rsadvanced.feature.mining.module;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.architectury.registry.menu.MenuRegistry;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.connection.NetworkCardConnection;
import dev.rsadvanced.feature.connection.NetworkCardItemContext;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import dev.rsadvanced.feature.mining.MiningCardMenu;
import dev.rsadvanced.feature.mining.MiningContext;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class NetworkToolModule implements ToolModule {
    @Override
    public ResourceLocation id() {
        return RSAdvanced.id("network");
    }

    public NetworkCardItemContext context(ServerPlayer player, InteractionHand hand) {
        var reference = RefinedStorageApi.INSTANCE.createInventorySlotReference(player, hand);
        return new NetworkCardItemContext(player, reference, NetworkCardSlot::card, BuiltinPermission.INSERT);
    }

    public boolean retainsRoute(ServerPlayer player, ItemStack tool, Network originalNetwork) {
        return originalNetwork == null || NetworkCardConnection.resolve(player, NetworkCardSlot.card(tool),
                BuiltinPermission.INSERT).orElse(null) == originalNetwork;
    }

    @Override
    public ToolMiningSession openMiningSession(MiningContext context) {
        Network originalNetwork = context(context.player(), InteractionHand.MAIN_HAND).connection().orElse(null);
        return new ToolMiningSession() {
            private ItemStack blockCard = ItemStack.EMPTY;

            @Override
            public boolean canContinue() {
                return retainsRoute(context.player(), context.tool(), originalNetwork);
            }

            @Override
            public boolean beforeBlock(long cost, boolean selected) {
                blockCard = NetworkCardSlot.card(context.tool());
                return true;
            }

            @Override
            public boolean routeDrop(ItemEntity entity) {
                return NetworkToolModule.this.routeDrop(context.player(), context.tool(), blockCard, originalNetwork, entity);
            }

            @Override
            public void afterBlock(boolean removed) {
                blockCard = ItemStack.EMPTY;
            }
        };
    }

    public boolean routeDrop(ServerPlayer player, ItemStack tool, ItemStack originalCard,
                             Network originalNetwork, ItemEntity entity) {
        if (originalNetwork == null || !ItemStack.matches(originalCard, NetworkCardSlot.card(tool))) {
            return false;
        }
        // The last block may consume the final FE. Route validation is independent of tool charge.
        Network liveNetwork = NetworkCardConnection.resolve(player, originalCard, BuiltinPermission.INSERT).orElse(null);
        if (liveNetwork != originalNetwork) {
            return false;
        }
        ItemStack remaining = entity.getItem().copy();
        long inserted = liveNetwork.getComponent(StorageNetworkComponent.class).insert(
                ItemResource.ofItemStack(remaining), remaining.getCount(), Action.EXECUTE, new PlayerActor(player));
        remaining.shrink(Math.toIntExact(inserted));
        if (!remaining.isEmpty()) {
            player.getInventory().add(remaining);
        }
        if (remaining.isEmpty()) {
            return true;
        }
        entity.setItem(remaining);
        return false;
    }

    @Override
    public InteractionResult useFallback(ToolModuleContainer container, ServerPlayer player, SlotReference reference) {
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider(
                (id, inventory, owner) -> new MiningCardMenu(id, inventory, reference),
                Component.translatable("gui.rsadvanced.mining_card")),
                buffer -> SlotReferenceFactory.STREAM_CODEC.encode(
                        new RegistryFriendlyByteBuf(buffer, player.registryAccess()), reference));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ToolModuleContainer container, Item.TooltipContext context,
                              List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.rsadvanced.network_mining_tool.help"));
        ItemStack card = NetworkCardSlot.card(container.stack());
        if (!card.isEmpty()) {
            card.getItem().appendHoverText(card, context, lines, flag);
        }
    }
}
