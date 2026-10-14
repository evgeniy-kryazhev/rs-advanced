package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.api.network.energy.EnergyStorage;
import com.refinedmods.refinedstorage.api.network.impl.energy.EnergyStorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.energy.AbstractEnergyItem;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceHandlerItem;
import dev.architectury.registry.menu.MenuRegistry;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** RS owns the energy component and charging adapters; vanilla owns mining and enchantments. */
public final class MiningItem extends AbstractEnergyItem implements SlotReferenceHandlerItem {
    public static final long CAPACITY = 100_000;
    public static final long BLOCK_COST = 100;
    private static final String MODE_KEY = "rsadvanced_mining_mode";

    public MiningItem() {
        super(new Properties().stacksTo(1).component(DataComponents.TOOL, createMultitoolProperties()),
                RefinedStorageApi.INSTANCE.getEnergyItemHelper());
    }

    private static Tool createMultitoolProperties() {
        float speed = Tiers.DIAMOND.getSpeed();
        return new Tool(List.of(
                Tool.Rule.deniesDrops(Tiers.DIAMOND.getIncorrectBlocksForDrops()),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, speed),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, speed),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, speed),
                Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, speed),
                Tool.Rule.minesAndDrops(List.of(net.minecraft.world.level.block.Blocks.COBWEB), speed)
        ), speed, 1);
    }

    public EnergyStorage createEnergyStorage(ItemStack stack) {
        return RefinedStorageApi.INSTANCE.asItemEnergyStorage(new EnergyStorageImpl(CAPACITY), stack);
    }

    public static int radius(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(MODE_KEY), 0, 2);
    }

    public static void setRadius(ItemStack stack, int radius) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(MODE_KEY, Math.clamp(radius, 0, 2)));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return Tiers.DIAMOND.getEnchantmentValue();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            use(serverPlayer, stack, RefinedStorageApi.INSTANCE.createInventorySlotReference(player, hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    @Override
    public void use(ServerPlayer player, ItemStack stack, SlotReference reference) {
        if (player.isShiftKeyDown()) {
            setRadius(stack, (radius(stack) + 1) % 3);
            player.displayClientMessage(modeLabel(stack), true);
            return;
        }
        MenuRegistry.openExtendedMenu(player, new SimpleMenuProvider(
                (id, inventory, owner) -> new MiningCardMenu(id, inventory, reference),
                Component.translatable("gui.rsadvanced.mining_card")),
                buffer -> SlotReferenceFactory.STREAM_CODEC.encode(
                        new RegistryFriendlyByteBuf(buffer, player.registryAccess()), reference));
    }

    private static Component modeLabel(ItemStack stack) {
        int width = radius(stack) * 2 + 1;
        return Component.translatable("item.rsadvanced.network_mining_tool.mode", width, width);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(modeLabel(stack));
        lines.add(Component.translatable("item.rsadvanced.network_mining_tool.help"));
        ItemStack card = NetworkCardSlot.card(stack);
        if (!card.isEmpty()) {
            card.getItem().appendHoverText(card, context, lines, flag);
        }
    }
}
