package dev.rsadvanced.test.mixin;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Development-only access to the actual renderer state, rather than a duplicate animation model. */
@Mixin(ItemInHandRenderer.class)
public interface ItemInHandRendererAccessor {
    @Accessor("mainHandHeight")
    float rsadvanced$mainHandHeight();

    @Accessor("offHandHeight")
    float rsadvanced$offHandHeight();

    @Accessor("mainHandItem")
    ItemStack rsadvanced$mainHandItem();

    @Accessor("offHandItem")
    ItemStack rsadvanced$offHandItem();
}
