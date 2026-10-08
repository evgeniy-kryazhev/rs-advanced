package dev.rsadvanced.feature.disk.neoforge;

import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import net.minecraft.network.chat.Component;


public final class CellResourceNamesImpl {
    private CellResourceNamesImpl() {
    }

    public static Component fluidName(FluidResource resource) {
        return resource.fluid().getFluidType().getDescription();
    }
}
