package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.common.support.AbstractBaseBlock;
import com.refinedmods.refinedstorage.common.content.BlockConstants;
import dev.rsadvanced.content.AdvancedComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class InfiniteStorageBlock extends AbstractBaseBlock implements EntityBlock {
    private final DiskResourceKind kind;

    public InfiniteStorageBlock(DiskResourceKind kind) {
        super(BlockConstants.PROPERTIES);
        this.kind = kind;
    }

    public DiskResourceKind kind() {
        return kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new InfiniteStorageBlockEntity(position, state);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos position, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(position) instanceof InfiniteStorageBlockEntity storageBlock
                && storageBlock.definitionId() != null) {
            stack.set(AdvancedComponents.CELL_DEFINITION.get(), storageBlock.definitionId());
        }
        return stack;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos position, BlockState replacement, boolean moving) {
        // RS skips removal for blocks of the same class; our two resource kinds share this class.
        if (state.getBlock() != replacement.getBlock() && replacement.getBlock() instanceof InfiniteStorageBlock) {
            level.removeBlockEntity(position);
        }
        super.onRemove(state, level, position, replacement, moving);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide() || type != InfiniteStorageBlockContent.ENTITY.get()) {
            return null;
        }
        return (world, position, blockState, blockEntity) -> {
            if (blockEntity instanceof InfiniteStorageBlockEntity storageBlock) {
                storageBlock.updateActiveness(blockState, null);
                storageBlock.doWork();
            }
        };
    }
}
