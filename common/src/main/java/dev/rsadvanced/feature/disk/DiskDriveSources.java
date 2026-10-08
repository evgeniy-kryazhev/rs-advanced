package dev.rsadvanced.feature.disk;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/** Installed resource identities, independent of power, extraction access and duplicate definitions. */
public record DiskDriveSources(Set<CellDefinition> resources, boolean hasOrdinaryDisks) {
    public DiskDriveSources {
        resources = Set.copyOf(resources);
    }

    public static DiskDriveSources fromSlots(List<Slot> diskSlots) {
        Set<CellDefinition> resources = new HashSet<>();
        boolean ordinaryDisks = false;
        for (Slot slot : diskSlots) {
            if (slot.getItem().isEmpty()) {
                continue;
            }
            if (slot.getItem().getItem() instanceof InfiniteDiskItem disk) {
                disk.displayDefinition(slot.getItem()).ifPresent(resources::add);
            } else {
                ordinaryDisks = true;
            }
        }
        return new DiskDriveSources(resources, ordinaryDisks);
    }

    public void appendTooltip(List<Component> tooltip) {
        if (resources.isEmpty()) {
            return;
        }
        var names = Component.empty();
        var ordered = resources.stream().sorted(Comparator.comparing(CellDefinition::kind)
                .thenComparing(definition -> definition.resource().toString())).toList();
        for (int index = 0; index < ordered.size(); index++) {
            if (index > 0) {
                names.append(", ");
            }
            names.append(CellResourceNames.name(ordered.get(index).resourceKey()));
        }
        tooltip.add(Component.translatable("tooltip.rsadvanced.drive_infinite_source", names)
                .withStyle(ChatFormatting.AQUA));
    }
}
