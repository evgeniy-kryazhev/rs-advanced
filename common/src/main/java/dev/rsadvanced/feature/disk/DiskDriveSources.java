package dev.rsadvanced.feature.disk;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/** Presentation metadata derived from installed slots, independent of network availability. */
public record DiskDriveSources(Set<InfiniteDiskType> infiniteTypes, boolean hasOrdinaryDisks) {
    public DiskDriveSources {
        infiniteTypes = Set.copyOf(infiniteTypes);
    }

    public static DiskDriveSources fromSlots(List<Slot> diskSlots) {
        Set<InfiniteDiskType> types = EnumSet.noneOf(InfiniteDiskType.class);
        boolean ordinaryDisks = false;
        for (Slot slot : diskSlots) {
            if (slot.getItem().isEmpty()) {
                continue;
            }
            if (slot.getItem().getItem() instanceof InfiniteDiskItem infiniteDisk) {
                types.add(infiniteDisk.diskType());
            } else {
                ordinaryDisks = true;
            }
        }
        return new DiskDriveSources(types, ordinaryDisks);
    }

    public void appendTooltip(List<Component> tooltip) {
        // Enum order keeps the tooltip stable when disks move between slots.
        for (InfiniteDiskType type : InfiniteDiskType.values()) {
            if (infiniteTypes.contains(type)) {
                String resourceKey = type == InfiniteDiskType.WATER
                        ? "block.minecraft.water" : "block.minecraft.cobblestone";
                tooltip.add(Component.translatable("tooltip.rsadvanced.drive_infinite_source",
                        Component.translatable(resourceKey)).withStyle(ChatFormatting.AQUA));
            }
        }
    }
}
