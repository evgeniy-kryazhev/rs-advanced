package dev.rsadvanced.feature.mining;

import dev.rsadvanced.feature.mining.module.ToolMiningSession;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Coordinates module phases around vanilla destruction; feature rules belong to the installed modules. */
public final class MiningOperation {
    private static final ThreadLocal<ActiveBlock> ACTIVE = new ThreadLocal<>();

    private enum Result {
        REMOVED, SKIPPED, STOP
    }

    private MiningOperation() {
    }

    public static boolean destroy(ServerPlayer player, BlockPos center, Direction face, BooleanSupplier original) {
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof MiningItem) || ACTIVE.get() != null) {
            return original.getAsBoolean();
        }
        var modules = MiningItem.modules(tool);
        modules.prepare();
        if (!modules.canMine() || !player.serverLevel().isLoaded(center)) {
            return false;
        }
        var context = new MiningContext(player, modules, center, face);
        var sessions = modules.openMiningSessions(context);
        if (breakOne(context, sessions, MiningTarget.block(context, center), true, original) != Result.REMOVED) {
            return false;
        }
        for (var module : modules.installed()) {
            List<MiningTarget> targets = new ArrayList<>();
            // Reserve the whole phase before executing it. Later modules cannot schedule these blocks twice.
            for (MiningTarget target : module.miningTargets(context)) {
                if (context.plan(target.position())) {
                    targets.add(target);
                }
            }
            for (MiningTarget target : targets) {
                if (!canContinue(context, sessions)) {
                    return true;
                }
                if (breakOne(context, sessions, target, false,
                        () -> player.gameMode.destroyBlock(target.position())) == Result.STOP) {
                    return true;
                }
            }
        }
        return true;
    }

    private static boolean canContinue(MiningContext context, List<ToolMiningSession> sessions) {
        return context.ownsTool() && context.modules().canMine()
                && sessions.stream().allMatch(ToolMiningSession::canContinue);
    }

    private static Result breakOne(MiningContext context, List<ToolMiningSession> sessions,
                                   MiningTarget target, boolean selected, BooleanSupplier original) {
        BlockPos position = target.position();
        ServerLevel level = context.level();
        if (!context.ownsTool() || !target.ready().getAsBoolean() || !level.isLoaded(position)
                || !level.getWorldBorder().isWithinBounds(position) || !level.mayInteract(context.player(), position)) {
            return Result.SKIPPED;
        }
        var before = level.getBlockState(position);
        if (before.isAir() || !target.acceptsState().test(before)
                || !context.modules().canMineBlock(context, position, before, selected)) {
            return Result.SKIPPED;
        }
        try (var transaction = new MiningBlockTransaction()) {
            if (!transaction.reserve(sessions, target.cost().applyAsLong(before), selected)) {
                return Result.STOP;
            }
            if (!canContinue(context, sessions)) {
                return Result.STOP;
            }
            ACTIVE.set(new ActiveBlock(context, position, sessions));
            try {
                boolean result = original.getAsBoolean();
                boolean removed = level.getBlockState(position) != before;
                if (removed) {
                    context.modules().afterBlockMined(context.player(), position, before);
                }
                if (result && removed) {
                    context.recordRemoved(position, before);
                    target.afterRemoved().run();
                    return Result.REMOVED;
                }
                return Result.SKIPPED;
            } finally {
                ACTIVE.remove();
                // Include mutations made before an exception, so modules never refund an already removed block.
                transaction.removed(level.getBlockState(position) != before);
            }
        }
    }

    /** Routes only drops belonging to the currently coordinated block, after loader loot events. */
    public static boolean routeDrop(ServerLevel level, ItemEntity entity) {
        ActiveBlock active = ACTIVE.get();
        if (active == null || level != active.context().level() || !active.context().ownsTool()
                || !entity.blockPosition().equals(active.position())) {
            return false;
        }
        return active.sessions().stream().anyMatch(session -> session.routeDrop(entity));
    }

    private record ActiveBlock(MiningContext context, BlockPos position, List<ToolMiningSession> sessions) {
    }
}
