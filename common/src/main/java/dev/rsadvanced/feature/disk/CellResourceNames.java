package dev.rsadvanced.feature.disk;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.chat.Component;

/** Use the same native name providers as RS, without loading client renderers on the server. */
public final class CellResourceNames {
    private CellResourceNames() {
    }

    public static Component name(ResourceKey resource) {
        if (resource instanceof ItemResource item) {
            return item.toItemStack().getHoverName();
        }
        if (resource instanceof FluidResource fluid) {
            return fluidName(fluid);
        }
        throw new IllegalArgumentException("Unsupported infinite cell resource: " + resource);
    }

    @ExpectPlatform
    public static Component fluidName(FluidResource resource) {
        throw new AssertionError("The loader must supply its native fluid name provider");
    }
}
