package dev.rsadvanced.mixin.client;

import dev.rsadvanced.client.CellResourceOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class CellResourceOverlayMixin {
    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V",
            at = @At("TAIL"))
    private void rsadvanced$renderResourceBadge(LivingEntity entity, Level level, ItemStack stack,
                                               int x, int y, int seed, int depth, CallbackInfo callback) {
        CellResourceOverlay.render((GuiGraphics) (Object) this, stack, x, y);
    }
}
