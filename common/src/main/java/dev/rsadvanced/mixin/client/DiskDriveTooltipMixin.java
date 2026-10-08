package dev.rsadvanced.mixin.client;

import com.refinedmods.refinedstorage.common.storage.AbstractProgressStorageScreen;
import dev.rsadvanced.feature.disk.DiskDriveSources;
import dev.rsadvanced.feature.disk.InfiniteDiskDriveMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractProgressStorageScreen.class, remap = false)
public abstract class DiskDriveTooltipMixin {
    @Inject(method = "createProgressTooltip", at = @At("RETURN"), cancellable = true, require = 1)
    private void rsadvanced$showInfiniteSources(CallbackInfoReturnable<List<Component>> callback) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (!(screen.getMenu() instanceof InfiniteDiskDriveMenu drive)) {
            return;
        }
        DiskDriveSources sources = drive.rsadvanced$getSources();
        if (sources.resources().isEmpty()) {
            return;
        }
        List<Component> tooltip = sources.hasOrdinaryDisks()
                ? new ArrayList<>(callback.getReturnValue()) : new ArrayList<>();
        sources.appendTooltip(tooltip);
        callback.setReturnValue(tooltip);
    }
}
