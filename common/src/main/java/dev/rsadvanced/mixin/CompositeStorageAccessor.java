package dev.rsadvanced.mixin;

import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CompositeStorageImpl.class, remap = false)
public interface CompositeStorageAccessor {
    @Accessor("list")
    MutableResourceList rsadvanced$getResourceList();
}
