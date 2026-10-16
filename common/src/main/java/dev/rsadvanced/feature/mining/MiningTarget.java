package dev.rsadvanced.feature.mining;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Modules own target-specific requirements, pricing and successful-removal effects. */
public record MiningTarget(BlockPos position, BooleanSupplier ready, Predicate<BlockState> acceptsState,
                           ToLongFunction<BlockState> cost, Runnable afterRemoved) {
    public MiningTarget {
        position = position.immutable();
    }

    public static MiningTarget block(MiningContext context, BlockPos position) {
        return new MiningTarget(position, () -> true, state -> true,
                state -> context.modules().blockCost(context, position, state), () -> { });
    }
}
