package dev.rsadvanced.mixin.client;

import com.refinedmods.refinedstorage.common.AbstractClientModInitializer;
import dev.rsadvanced.client.RSAdvancedClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractClientModInitializer.class, remap = false)
public abstract class DiskModelRegistrationMixin {
    @Inject(method = "registerDiskModels", at = @At("TAIL"), require = 1)
    private static void rsadvanced$registerStandardDiskModels(CallbackInfo callback) {
        // RS's own hook guarantees its client API is ready and registration precedes model baking on both loaders.
        RSAdvancedClient.registerDiskModels();
    }
}
