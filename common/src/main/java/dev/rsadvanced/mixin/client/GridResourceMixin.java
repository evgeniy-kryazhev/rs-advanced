package dev.rsadvanced.mixin.client;

import com.refinedmods.refinedstorage.api.resource.repository.ResourceRepository;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResource;
import com.refinedmods.refinedstorage.common.grid.view.FluidGridResource;
import com.refinedmods.refinedstorage.common.grid.view.ItemGridResource;
import dev.rsadvanced.client.InfiniteGridDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = {ItemGridResource.class, FluidGridResource.class}, remap = false)
public abstract class GridResourceMixin {
    @Inject(method = {"getDisplayedAmount", "getAmountInTooltip"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void rsadvanced$displayInfinity(ResourceRepository<GridResource> repository,
                                           CallbackInfoReturnable<String> callback) {
        GridResource resource = (GridResource) this;
        if (InfiniteGridDisplay.isInfinite(resource, repository)) {
            callback.setReturnValue("∞");
        }
    }
}
