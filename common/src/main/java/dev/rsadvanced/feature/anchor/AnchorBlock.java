package dev.rsadvanced.feature.anchor;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class AnchorBlock extends Block implements EntityBlock {
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AnchorBlock() {
        super(Properties.of().strength(2.5f, 6).sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new AnchorBlockEntity(position, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos position,
            Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(position) instanceof AnchorBlockEntity anchor && anchor.canOpen(serverPlayer)) {
            MenuRegistry.openExtendedMenu(serverPlayer, anchor, buffer -> buffer.writeBlockPos(position));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos position, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(position) instanceof AnchorBlockEntity anchor) {
            AnchorManager.release(anchor);
        }
        super.onRemove(state, level, position, replacement, moving);
    }
}
