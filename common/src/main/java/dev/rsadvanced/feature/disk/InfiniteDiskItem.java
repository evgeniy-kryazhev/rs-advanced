package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageContainerItem;
import com.refinedmods.refinedstorage.common.api.storage.StorageInfo;
import com.refinedmods.refinedstorage.common.api.storage.StorageRepository;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class InfiniteDiskItem extends Item implements StorageContainerItem {
    private final InfiniteDiskType diskType;

    public InfiniteDiskItem(InfiniteDiskType diskType) {
        super(new Item.Properties().stacksTo(1));
        this.diskType = diskType;
    }

    public InfiniteDiskType diskType() {
        return diskType;
    }

    @Override
    public Optional<SerializableStorage> resolve(StorageRepository repository, ItemStack stack) {
        // Each slot gets its own source, while all state is determined by the item type.
        return Optional.of(diskType.create(null, () -> { }));
    }

    @Override
    public Optional<StorageInfo> getInfo(StorageRepository repository, ItemStack stack) {
        // UI metadata describes physical stock, not the source's virtual availability.
        return Optional.of(new StorageInfo(0, 0));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.rsadvanced.infinite_source").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.rsadvanced.disk_drive").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.rsadvanced.absorbs_returns").withStyle(ChatFormatting.GRAY));
    }
}
