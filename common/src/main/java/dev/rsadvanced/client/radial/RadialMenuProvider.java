package dev.rsadvanced.client.radial;

import net.minecraft.client.gui.screens.Screen;

@FunctionalInterface
public interface RadialMenuProvider {
    Screen create(RadialMenuModel model);
}
