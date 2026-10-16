package dev.rsadvanced.client.radial;

import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A command and its presentation, independent of the radial menu implementation. */
public record RadialMenuEntry(String id, Component label, ItemStack icon,
                              BooleanSupplier selected, BooleanSupplier enabled, Runnable action) {
}
