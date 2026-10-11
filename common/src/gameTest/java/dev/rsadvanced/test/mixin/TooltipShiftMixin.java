package dev.rsadvanced.test.mixin;

import dev.rsadvanced.test.client.AnchorTooltipClientCheck;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Only the development test mod supplies a modifier override while capturing tooltip scenarios. */
@Mixin(Screen.class)
public abstract class TooltipShiftMixin {
    @Inject(method = "hasShiftDown", at = @At("HEAD"), cancellable = true)
    private static void overrideShiftForTooltipCheck(CallbackInfoReturnable<Boolean> callback) {
        Boolean shiftOverride = AnchorTooltipClientCheck.shiftOverride();
        if (shiftOverride != null) {
            callback.setReturnValue(shiftOverride);
        }
    }
}
