package dev.rsadvanced.feature.disk.fabric;

import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.network.chat.Component;

public final class CellResourceNamesImpl {
    private CellResourceNamesImpl() {
    }

    public static Component fluidName(FluidResource resource) {
        return FluidVariantAttributes.getName(FluidVariant.of(resource.fluid()));
    }
}
