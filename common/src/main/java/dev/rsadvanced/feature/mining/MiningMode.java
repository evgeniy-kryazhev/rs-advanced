package dev.rsadvanced.feature.mining;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum MiningMode implements StringRepresentable {
    NORMAL(20),
    SLOW(8),
    FAST(128),
    // Kept solely to decode existing items. MiningItem exposes it as Normal + independent vein mining.
    VEIN(20),
    OFF(0);

    public static final Codec<MiningMode> CODEC = StringRepresentable.fromEnum(MiningMode::values);
    public static final java.util.List<MiningMode> SPEED_MODES = java.util.List.of(NORMAL, SLOW, FAST, OFF);
    private final int speed;

    MiningMode(int speed) {
        this.speed = speed;
    }

    public int speed() {
        return speed;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
