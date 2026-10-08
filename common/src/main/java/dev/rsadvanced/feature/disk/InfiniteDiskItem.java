package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageContainerItem;
import com.refinedmods.refinedstorage.common.api.storage.StorageInfo;
import com.refinedmods.refinedstorage.common.api.storage.StorageRepository;
import dev.rsadvanced.content.AdvancedComponents;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class InfiniteDiskItem extends Item implements StorageContainerItem {
    private final DiskResourceKind kind;

    public InfiniteDiskItem(DiskResourceKind kind) {
        super(new Item.Properties().stacksTo(1));
        this.kind = kind;
    }

    public DiskResourceKind kind() {
        return kind;
    }

    public Optional<CellDefinition> displayDefinition(ItemStack stack) {
        return CellDefinitions.displayDefinition(stack.get(AdvancedComponents.CELL_DEFINITION.get()))
                .filter(definition -> definition.kind() == kind);
    }

    @Override
    public Component getName(ItemStack stack) {
        return displayDefinition(stack)
                .map(definition -> Component.translatable("item.rsadvanced.infinite_cell",
                        CellResourceNames.name(definition.resourceKey())))
                .orElseGet(() -> Component.translatable("item.rsadvanced.unknown_infinite_cell"));
    }

    @Override
    public Optional<SerializableStorage> resolve(StorageRepository repository, ItemStack stack) {
        var definitionId = stack.get(AdvancedComponents.CELL_DEFINITION.get());
        return CellDefinitions.serverDefinition(definitionId)
                .filter(definition -> definition.kind() == kind)
                .map(definition -> InfiniteStorageType.forKind(kind).create(definitionId, definition));
    }

    @Override
    public Optional<StorageInfo> getInfo(StorageRepository repository, ItemStack stack) {
        return Optional.of(new StorageInfo(0, 0));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (displayDefinition(stack).isEmpty()) {
            var definitionId = stack.get(AdvancedComponents.CELL_DEFINITION.get());
            tooltip.add(Component.translatable("tooltip.rsadvanced.unknown_definition",
                    definitionId == null ? "—" : definitionId.toString()).withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("tooltip.rsadvanced.infinite_source").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.rsadvanced.disk_drive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rsadvanced.absorbs_returns").withStyle(ChatFormatting.GRAY));
    }
}
