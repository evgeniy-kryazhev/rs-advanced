package dev.rsadvanced.mixin;

import dev.rsadvanced.feature.mining.MiningOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class MiningDropsMixin {
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void rsadvanced$storeDrop(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (entity instanceof ItemEntity item && MiningOperation.routeDrop((ServerLevel) (Object) this, item)) {
            callback.setReturnValue(true);
        }
    }
}
