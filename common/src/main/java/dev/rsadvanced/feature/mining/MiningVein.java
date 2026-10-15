package dev.rsadvanced.feature.mining;

import dev.rsadvanced.RSAdvanced;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MiningVein {
    public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, RSAdvanced.id("mining_ores"));
    public record Target(BlockPos position, int distance, BlockPos parent, Block block) {
    }

    private MiningVein() {
    }

    public static List<Target> find(ServerLevel level, BlockPos origin, BlockState original, int limit) {
        return find(level, Map.of(origin.immutable(), original), Set.of(origin.immutable()), limit);
    }

    public static List<Target> find(ServerLevel level, Map<BlockPos, BlockState> seeds,
                                    Set<BlockPos> area, int limit) {
        List<Target> result = new ArrayList<>();
        for (Target target : traverse(level, seeds, area, limit, ignored -> true)) {
            result.add(target);
        }
        return result;
    }

    /** Expand only successfully mined blocks; a denied branch must not claim another branch's descendants. */
    public static Iterable<Target> traverse(ServerLevel level, Map<BlockPos, BlockState> seeds,
                                            Set<BlockPos> area, int limit, Predicate<Target> wasMined) {
        return () -> new Traversal(level, seeds, area, limit, wasMined);
    }

    private static final class Traversal implements Iterator<Target> {
        private final ServerLevel level;
        private final int limit;
        private final Predicate<Target> wasMined;
        private final ArrayDeque<Target> frontier = new ArrayDeque<>();
        private final Set<BlockPos> visited;
        private Target previous;
        private int discovered;

        private Traversal(ServerLevel level, Map<BlockPos, BlockState> seeds, Set<BlockPos> area,
                          int limit, Predicate<Target> wasMined) {
            this.level = level;
            this.limit = limit;
            this.wasMined = wasMined;
            visited = new HashSet<>(area);
            for (var seed : seeds.entrySet()) {
                if (seed.getValue().is(ORES)) {
                    expand(new Target(seed.getKey(), 0, seed.getKey(), seed.getValue().getBlock()));
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (previous != null) {
                Target completed = previous;
                previous = null;
                if (wasMined.test(completed)) {
                    expand(completed);
                }
            }
            return !frontier.isEmpty();
        }

        @Override
        public Target next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            previous = frontier.removeFirst();
            return previous;
        }

        private void expand(Target current) {
            for (BlockPos neighbor : BlockPos.betweenClosed(current.position().offset(-1, -1, -1),
                    current.position().offset(1, 1, 1))) {
                BlockPos position = neighbor.immutable();
                if (visited.contains(position) || !level.isLoaded(position)) {
                    continue;
                }
                if (discovered < limit && level.getBlockState(position).is(current.block())) {
                    // Mismatches remain available to seeds of other ore types.
                    visited.add(position);
                    discovered++;
                    frontier.addLast(new Target(position, current.distance() + 1, current.position(), current.block()));
                }
            }
        }
    }
}
