package dev.rsadvanced.neoforge.config;

import dev.rsadvanced.config.RSAdvancedConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Native NeoForge config and screen schema, kept outside the shared gameplay code. */
public final class NeoForgeConfig {
    private final ModConfigSpec spec;
    private final ModConfigSpec.LongValue miningCapacity;
    private final ModConfigSpec.LongValue miningChargeRate;
    private final ModConfigSpec.LongValue miningBaseCost;
    private final ModConfigSpec.IntValue miningVeinLimit;
    private final ModConfigSpec.BooleanValue miningVeinEnabled;
    private final ModConfigSpec.LongValue baseCost;
    private final ModConfigSpec.LongValue chunkCostMultiplier;
    private final ModConfigSpec.IntValue maxChunks;
    private final ModConfigSpec.BooleanValue randomTicks;
    private final ModConfigSpec.IntValue visualizationDistance;

    public NeoForgeConfig() {
        var builder = new ModConfigSpec.Builder();
        var defaults = RSAdvancedConfig.DEFAULT;
        builder.comment("Network Anchor settings. Restart the server or reopen the world to apply changes.",
                        "These settings apply to this installation, not to a remote server.")
                .translation("rsadvanced.configuration.anchor")
                .push("anchor");
        baseCost = builder.comment("Base energy cost in FE per tick. Must be nonnegative.")
                .translation("rsadvanced.configuration.anchor.baseCost")
                .worldRestart()
                .defineInRange("baseCost", defaults.anchorBaseCost(), 0L, Long.MAX_VALUE);
        chunkCostMultiplier = builder.comment("Multiplier for N * (N + 1) / 2 FE per tick.",
                        "The total cost at maxChunks must fit in a signed 64-bit integer.")
                .translation("rsadvanced.configuration.anchor.chunkCostMultiplier")
                .worldRestart()
                .defineInRange("chunkCostMultiplier", defaults.anchorChunkCostMultiplier(), 0L, Long.MAX_VALUE);
        maxChunks = builder.comment("Maximum chunks held by an anchor area. Exceeding this stops the entire area.")
                .translation("rsadvanced.configuration.anchor.maxChunks")
                .worldRestart()
                .defineInRange("maxChunks", defaults.anchorMaxChunks(), 1, 65536);
        randomTicks = builder.comment("Allow random block ticks in held chunks, following randomTickSpeed.")
                .translation("rsadvanced.configuration.anchor.randomTicks")
                .worldRestart()
                .define("randomTicks", defaults.anchorRandomTicks());
        visualizationDistance = builder.comment("Overlay and area-update radius in blocks, including height.")
                .translation("rsadvanced.configuration.anchor.visualizationDistance")
                .worldRestart()
                .defineInRange("visualizationDistance", defaults.anchorVisualizationDistance(), 1, 4096);
        builder.pop();
        builder.comment("Mining multitool settings, in FE. Restart required.").push("mining");
        miningCapacity = builder.worldRestart().defineInRange("capacity", 400_000L, 1L, Long.MAX_VALUE);
        miningChargeRate = builder.worldRestart().defineInRange("chargeRate", 2_000L, 1L, Long.MAX_VALUE);
        miningBaseCost = builder.worldRestart().defineInRange("baseCost", 4L, 1L, Long.MAX_VALUE / 128);
        miningVeinLimit = builder.worldRestart().defineInRange("veinLimit", 128, 1, 4096);
        miningVeinEnabled = builder.worldRestart().define("veinEnabled", true);
        builder.pop();
        spec = builder.build();
    }

    public ModConfigSpec spec() {
        return spec;
    }

    public RSAdvancedConfig snapshot() {
        // Validate the combined cost as well as the individual ranges enforced by the spec.
        return new RSAdvancedConfig(baseCost.get(), chunkCostMultiplier.get(), maxChunks.get(),
                randomTicks.get(), visualizationDistance.get(),
                new dev.rsadvanced.config.MiningSettings(miningCapacity.get(), miningChargeRate.get(), miningBaseCost.get(),
                        miningVeinLimit.get(), miningVeinEnabled.get()));
    }
}
