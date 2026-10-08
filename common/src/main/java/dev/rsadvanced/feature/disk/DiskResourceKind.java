package dev.rsadvanced.feature.disk;

import com.mojang.serialization.Codec;
import com.refinedmods.refinedstorage.common.Platform;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/** RS uses different transfer units for items and fluids on each loader. */
public enum DiskResourceKind implements StringRepresentable {
    ITEM("disk", 64),
    FLUID("fluid_disk", 16);

    public static final Codec<DiskResourceKind> CODEC =
            StringRepresentable.fromEnum(DiskResourceKind::values);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    private final String modelName;
    private final int stackMultiplier;

    DiskResourceKind(String modelName, int stackMultiplier) {
        this.modelName = modelName;
        this.stackMultiplier = stackMultiplier;
    }

    public long transferQuota(boolean stackUpgrade) {
        long unit = this == FLUID ? Platform.INSTANCE.getBucketAmount() : 1;
        return unit * (stackUpgrade ? stackMultiplier : 1);
    }

    public ResourceLocation diskModel() {
        return ResourceLocation.fromNamespaceAndPath("refinedstorage", "block/disk/" + modelName);
    }
}
