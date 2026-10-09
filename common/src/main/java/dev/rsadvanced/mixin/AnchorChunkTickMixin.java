package dev.rsadvanced.mixin;

import dev.rsadvanced.feature.anchor.AnchorManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class AnchorChunkTickMixin {
    @Inject(method = "tickChunk", at = @At("HEAD"))
    private void rsadvanced$recordChunkTick(LevelChunk chunk, int randomTickSpeed, CallbackInfo callback) {
        AnchorManager.recordChunkTick((ServerLevel) (Object) this, chunk.getPos().toLong());
    }
}
