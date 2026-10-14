package dev.rsadvanced.feature.mining;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum MiningMode implements StringRepresentable {
    NORMAL(20),
    SLOW(8),
    FAST(128),
    VEIN(20),
    OFF(0);

    public static final Codec<MiningMode> CODEC = StringRepresentable.fromEnum(MiningMode::values);
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
