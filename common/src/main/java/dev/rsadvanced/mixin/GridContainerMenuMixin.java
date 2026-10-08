package dev.rsadvanced.mixin;

import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import dev.rsadvanced.feature.disk.InfiniteSources;
import dev.rsadvanced.network.InfiniteGridMenu;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AbstractGridContainerMenu.class, remap = false)
public abstract class GridContainerMenuMixin implements InfiniteGridMenu {
    @Shadow
    private Grid grid;

    @Unique
    private int rsadvanced$infiniteFlags;

    @Unique
    private int rsadvanced$lastSentFlags = -1;

    @Override
    public int rsadvanced$getInfiniteFlags() {
        return rsadvanced$infiniteFlags;
    }

    @Override
    public void rsadvanced$setInfiniteFlags(int flags) {
        rsadvanced$infiniteFlags = flags;
    }

    @Override
    public void rsadvanced$synchronize(ServerPlayer player) {
        int flags = 0;
        if (grid != null && grid.isGridActive()) {
            for (InfiniteDiskType diskType : InfiniteDiskType.values()) {
                if (InfiniteSources.contains(grid.getItemStorage(), diskType.resource())) {
                    flags |= diskType.flag();
                }
            }
        }
        if (flags == rsadvanced$lastSentFlags) {
            return;
        }

        rsadvanced$lastSentFlags = flags;
        AbstractGridContainerMenu menu = (AbstractGridContainerMenu) (Object) this;
        RSAdvanced.sendInfiniteResources(player, new InfiniteResourcesPayload(menu.containerId, flags));
    }
}
