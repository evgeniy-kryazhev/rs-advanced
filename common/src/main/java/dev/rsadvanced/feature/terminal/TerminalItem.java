package dev.rsadvanced.feature.terminal;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.HelpTooltipComponent;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import com.refinedmods.refinedstorage.common.grid.WirelessGridItem;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import dev.architectury.registry.menu.MenuRegistry;
import dev.rsadvanced.feature.connection.NetworkCardItemContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;

/** RS supplies the grid, resource operations, autocrafting, energy capacity and operation costs. */
public final class TerminalItem extends WirelessGridItem {
    public TerminalItem() {
        super(false);
    }

    public static ItemStack card(ItemStack terminal) {
        return terminal.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    public static boolean isNetworkCard(ItemStack stack) {
        return stack.is(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
    }

    @Override
    public void use(ServerPlayer player, ItemStack stack, SlotReference slotReference) {
        if (player.isShiftKeyDown() || card(stack).isEmpty()) {
            MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider(
                    (id, inventory, owner) -> new TerminalCardMenu(id, inventory, slotReference),
                    Component.translatable("gui.rsadvanced.terminal_card")),
                    buffer -> SlotReferenceFactory.STREAM_CODEC.encode(
                            new RegistryFriendlyByteBuf(buffer, player.registryAccess()), slotReference));
            return;
        }
        NetworkCardItemContext context = new NetworkCardItemContext(player, slotReference,
                TerminalItem::card, BuiltinPermission.OPEN);
        if (!context.isActive()) {
            player.displayClientMessage(Component.translatable("item.rsadvanced.network_terminal.unavailable"), true);
            return;
        }
        // The protected RS entry point accepts our route while keeping its complete grid implementation.
        super.use(stack.getHoverName(), player, slotReference, context);
        TerminalSessions.track(player, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Binding belongs to the removable card; never write a second address onto the terminal itself.
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public boolean isBound(ItemStack stack) {
        return isNetworkCard(card(stack)) && networkItemHelper.isBound(card(stack));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new HelpTooltipComponent(Component.translatable("item.rsadvanced.network_terminal.help")));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        RefinedStorageApi.INSTANCE.getEnergyItemHelper().addTooltip(stack, lines);
        ItemStack installedCard = card(stack);
        if (installedCard.isEmpty()) {
            lines.add(Component.translatable("item.rsadvanced.network_terminal.no_card").withStyle(ChatFormatting.GRAY));
        } else {
            installedCard.getItem().appendHoverText(installedCard, context, lines, flag);
        }
    }
}
