package dev.rsadvanced.fabric.config;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.config.RSAdvancedConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;

/** Fabric's editable TOML model; gameplay uses an immutable server-session snapshot. */
@Config(name = RSAdvanced.MOD_ID)
public final class FabricConfig implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    @ConfigEntry.Gui.Tooltip
    public Anchor anchor = new Anchor();

    public static void register() {
        AutoConfig.register(FabricConfig.class, Toml4jConfigSerializer::new);
    }

    public static RSAdvancedConfig load() {
        var holder = AutoConfig.getConfigHolder(FabricConfig.class);
        if (!holder.load()) {
            throw new IllegalStateException("Cannot load config/rsadvanced.toml; see the configuration error above");
        }
        return holder.getConfig().snapshot();
    }

    public RSAdvancedConfig snapshot() {
        if (anchor == null) {
            throw new IllegalArgumentException("The anchor configuration must be a TOML table");
        }
        return new RSAdvancedConfig(anchor.baseCost, anchor.chunkCostMultiplier, anchor.maxChunks,
                anchor.randomTicks, anchor.visualizationDistance);
    }

    @Override
    public void validatePostLoad() throws ValidationException {
        try {
            snapshot();
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw new ValidationException("Invalid Network Anchor configuration: " + exception.getMessage(), exception);
        }
    }

    public static final class Anchor {
        @ConfigEntry.BoundedDiscrete(min = 0, max = Long.MAX_VALUE)
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public long baseCost = RSAdvancedConfig.DEFAULT.anchorBaseCost();

        @ConfigEntry.BoundedDiscrete(min = 0, max = Long.MAX_VALUE)
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public long chunkCostMultiplier = RSAdvancedConfig.DEFAULT.anchorChunkCostMultiplier();

        @ConfigEntry.BoundedDiscrete(min = 1, max = 65536)
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public int maxChunks = RSAdvancedConfig.DEFAULT.anchorMaxChunks();

        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public boolean randomTicks = RSAdvancedConfig.DEFAULT.anchorRandomTicks();

        @ConfigEntry.BoundedDiscrete(min = 1, max = 4096)
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public int visualizationDistance = RSAdvancedConfig.DEFAULT.anchorVisualizationDistance();
    }
}
