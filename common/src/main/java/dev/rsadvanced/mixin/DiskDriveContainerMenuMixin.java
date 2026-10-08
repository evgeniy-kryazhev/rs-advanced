package dev.rsadvanced.mixin;

import com.refinedmods.refinedstorage.common.storage.diskdrive.DiskDriveContainerMenu;
import dev.rsadvanced.feature.disk.DiskDriveSources;
import dev.rsadvanced.feature.disk.InfiniteDiskDriveMenu;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DiskDriveContainerMenu.class, remap = false)
public abstract class DiskDriveContainerMenuMixin implements InfiniteDiskDriveMenu {
    @Shadow
    @Final
    private List<Slot> diskSlots;

    @Override
    public DiskDriveSources rsadvanced$getSources() {
        return DiskDriveSources.fromSlots(diskSlots);
    }

    @Inject(method = "getDiskStacks", at = @At("RETURN"), cancellable = true, require = 1)
    private void rsadvanced$excludeInfiniteSourcesFromStatistics(CallbackInfoReturnable<Stream<ItemStack>> callback) {
        // RS still resolves and sums ordinary disks using its own repository and capacity rules.
        callback.setReturnValue(callback.getReturnValue()
                .filter(stack -> !(stack.getItem() instanceof InfiniteDiskItem)));
    }

    @Inject(method = "getProgress", at = @At("HEAD"), cancellable = true, require = 1)
    private void rsadvanced$keepSourceOnlyProgressEmpty(CallbackInfoReturnable<Double> callback) {
        DiskDriveSources sources = rsadvanced$getSources();
        if (!sources.infiniteTypes().isEmpty() && !sources.hasOrdinaryDisks()) {
            // No finite capacity exists; avoid RS's empty-stream 0/0 calculation.
            callback.setReturnValue(0.0);
        }
    }
}
