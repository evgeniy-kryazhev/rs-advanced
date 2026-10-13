package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.support.BaseBlockItem;
import dev.rsadvanced.content.AdvancedComponents;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

public final class InfiniteStorageBlockItem extends BaseBlockItem {
    private final DiskResourceKind kind;

    public InfiniteStorageBlockItem(InfiniteStorageBlock block) {
        super(block, new Properties().stacksTo(1).fireResistant(),
                Component.translatable("block.rsadvanced.infinite_storage_block.help"));
        this.kind = block.kind();
    }

    public Optional<CellDefinition> displayDefinition(ItemStack stack) {
        return CellDefinitions.displayDefinition(stack.get(AdvancedComponents.CELL_DEFINITION.get()))
                .filter(definition -> definition.kind() == kind);
    }

    @Override
    public Component getName(ItemStack stack) {
        return displayDefinition(stack)
                .map(definition -> Component.translatable("block.rsadvanced.infinite_storage_block",
                        CellResourceNames.name(definition.resourceKey())))
                .orElseGet(() -> Component.translatable("block.rsadvanced.unknown_infinite_storage_block"));
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        if (context.getPlayer() instanceof ServerPlayer player
                && !RefinedStorageApi.INSTANCE.canPlaceNetworkNode(player, context.getLevel(),
                        context.getClickedPos(), state)) {
            return false;
        }
        return super.placeBlock(context, state);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (displayDefinition(stack).isEmpty()) {
            var definitionId = stack.get(AdvancedComponents.CELL_DEFINITION.get());
            tooltip.add(Component.translatable("tooltip.rsadvanced.unknown_definition",
                    definitionId == null ? "—" : definitionId.toString()).withStyle(ChatFormatting.RED));
        }
    }
}
