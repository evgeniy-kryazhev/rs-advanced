package dev.rsadvanced.config;

import java.util.Objects;
import java.util.function.Supplier;

/** Shared configuration for all features, loaded centrally once per server session. */
public record RSAdvancedConfig(
        long anchorBaseCost,
        long anchorChunkCostMultiplier,
        int anchorMaxChunks,
        boolean anchorRandomTicks,
        int anchorVisualizationDistance, MiningSettings mining) {
    public static final RSAdvancedConfig DEFAULT = new RSAdvancedConfig(80, 1, 256, true, 256);
    private static volatile RSAdvancedConfig current = DEFAULT;

    public RSAdvancedConfig(long baseCost, long chunkCostMultiplier, int maxChunks, boolean randomTicks,
                            int visualizationDistance) {
        this(baseCost, chunkCostMultiplier, maxChunks, randomTicks, visualizationDistance, MiningSettings.DEFAULT);
    }

    public RSAdvancedConfig(long baseCost, long chunkCostMultiplier, int maxChunks, boolean randomTicks) {
        this(baseCost, chunkCostMultiplier, maxChunks, randomTicks, 256);
    }

    public RSAdvancedConfig {
        Objects.requireNonNull(mining, "mining");
        if (anchorBaseCost < 0 || anchorChunkCostMultiplier < 0 || anchorMaxChunks < 1 || anchorMaxChunks > 65536) {
            throw new IllegalArgumentException("Anchor costs must be nonnegative and maxChunks must be 1..65536");
        }
        if (anchorVisualizationDistance < 1 || anchorVisualizationDistance > 4096) {
            throw new IllegalArgumentException("anchorVisualizationDistance must be an integer in 1..4096");
        }
        long maximumChunkCost = (long) anchorMaxChunks * (anchorMaxChunks + 1) / 2;
        Math.addExact(anchorBaseCost, Math.multiplyExact(anchorChunkCostMultiplier, maximumChunkCost));
    }

    public long anchorCost(int chunks) {
        long chunkCost = (long) chunks * (chunks + 1) / 2;
        return Math.addExact(anchorBaseCost, Math.multiplyExact(anchorChunkCostMultiplier, chunkCost));
    }

    public static RSAdvancedConfig get() {
        return current;
    }

    public static void load(Supplier<RSAdvancedConfig> loader) {
        // Freeze all settings together for this server session. GUI edits must not change
        // networking limits while an existing AnchorManager still uses the previous costs.
        current = Objects.requireNonNull(loader.get(), "The platform configuration must not be null");
    }
}
