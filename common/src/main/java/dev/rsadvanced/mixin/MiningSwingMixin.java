package dev.rsadvanced.mixin;

import dev.rsadvanced.feature.mining.MiningItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MiningSwingMixin {
    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"), cancellable = true)
    private void rsadvanced$suppressToolSwing(InteractionHand hand, boolean updateSelf, CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player && player.getItemInHand(hand).getItem() instanceof MiningItem) {
            // Swing state is shared by first- and third-person rendering. Damage and mining run separately.
            callback.cancel();
        }
    }
}
