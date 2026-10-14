package dev.rsadvanced.feature.mining;

import dev.rsadvanced.RSAdvanced;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MiningVein {
    public static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, RSAdvanced.id("mining_ores"));
    public record Target(BlockPos position, int distance, BlockPos parent) {
    }

    private MiningVein() {
    }

    public static List<Target> find(ServerLevel level, BlockPos origin, BlockState original, int limit) {
        if (!original.is(ORES)) {
            return List.of();
        }
        List<Target> result = new ArrayList<>();
        ArrayDeque<Target> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        frontier.add(new Target(origin.immutable(), 0, origin.immutable()));
        visited.add(origin.immutable());
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
                visited.add(position);
                if (level.getBlockState(position).is(original.getBlock())) {
                    frontier.addLast(new Target(position, current.distance() + 1, current.position()));
                    // Every queued matching block will count towards the bounded result.
                    if (result.size() + frontier.size() >= limit) {
                        break;
                    }
                }
            }
        }
        return result;
    }
}
