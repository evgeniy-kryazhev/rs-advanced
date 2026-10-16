package dev.rsadvanced.client.radial;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public record RadialMenuModel(Component title, List<RadialMenuEntry> entries,
                             Supplier<List<Component>> status, BooleanSupplier canRemainOpen) {
    public RadialMenuModel {
        entries = List.copyOf(entries);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("A radial menu must contain at least one entry");
        }
    }
}
