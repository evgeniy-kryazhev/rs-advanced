package dev.rsadvanced.mixin;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.storage.storageblock.StorageBlockContainerMenu;
import dev.rsadvanced.network.InfiniteResourceMenu;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = StorageBlockContainerMenu.class, remap = false)
public abstract class StorageBlockContainerMenuMixin implements InfiniteResourceMenu {
    @Unique
    private Set<ResourceKey> rsadvanced$infiniteResources = Set.of();

    @Override
    public Set<ResourceKey> rsadvanced$getInfiniteResources() {
        return rsadvanced$infiniteResources;
    }

    @Override
    public void rsadvanced$setInfiniteResources(Set<ResourceKey> resources) {
        rsadvanced$infiniteResources = Set.copyOf(resources);
    }

    @Override
    public void rsadvanced$synchronize(ServerPlayer player) {
        // Ordinary RS blocks have no infinite resource metadata to synchronize.
    }

    @Inject(method = "getProgress", at = @At("HEAD"), cancellable = true, require = 1)
    private void rsadvanced$fillInfiniteProgress(CallbackInfoReturnable<Double> callback) {
        if (!rsadvanced$infiniteResources.isEmpty()) {
            callback.setReturnValue(1.0);
        }
    }
}
