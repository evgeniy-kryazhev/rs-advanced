package dev.rsadvanced.client.radial;

import dev.rsadvanced.client.radial.slice.SliceRadialScreen;
import java.util.Objects;
import net.minecraft.client.Minecraft;

/** The only place choosing a menu implementation. Features supply commands, never backend classes. */
public final class RadialMenus {
    private static RadialMenuProvider provider = SliceRadialScreen::new;

    private RadialMenus() {
    }

    public static void setProvider(RadialMenuProvider replacement) {
        provider = Objects.requireNonNull(replacement, "replacement");
    }

    public static void open(RadialMenuModel model) {
        Minecraft.getInstance().setScreen(provider.create(model));
    }
}
