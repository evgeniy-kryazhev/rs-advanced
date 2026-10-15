package dev.rsadvanced.feature.mining;

import dev.rsadvanced.RSAdvanced;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        ArrayDeque<Target> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>(area);
        for (var seed : seeds.entrySet()) {
            if (seed.getValue().is(ORES)) {
                frontier.add(new Target(seed.getKey(), 0, seed.getKey(), seed.getValue().getBlock()));
            }
        }
        int discovered = 0;
        while (!frontier.isEmpty() && result.size() < limit) {
            Target current = frontier.removeFirst();
            if (current.distance() > 0) {
                result.add(current);
            }
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
        return result;
    }
}
