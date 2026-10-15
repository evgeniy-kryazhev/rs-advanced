package dev.rsadvanced.feature.mining.module;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Presentation data only; no client screen or renderer is referenced by a tool module. */
public record ToolModuleOption(String id, Component label, ItemStack icon, boolean enabled) {
}
