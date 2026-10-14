package dev.rsadvanced.mixin;

import dev.rsadvanced.feature.mining.MiningCombat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class MiningCombatMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void rsadvanced$prepareAttack(Entity target, CallbackInfo callback) {
        MiningCombat.prepareAttack((Player) (Object) this);
    }
}
