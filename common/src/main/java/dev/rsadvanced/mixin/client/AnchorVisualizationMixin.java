package dev.rsadvanced.mixin.client;

import dev.rsadvanced.client.AnchorVisualization;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class AnchorVisualizationMixin {
    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void rsadvanced$renderAnchor(DeltaTracker delta, boolean outline, Camera camera,
            GameRenderer renderer, LightTexture light, Matrix4f view, Matrix4f projection, CallbackInfo callback) {
        AnchorVisualization.render(camera, view, projection);
    }
}
