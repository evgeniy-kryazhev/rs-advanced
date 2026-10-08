package dev.rsadvanced.mixin;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.InfiniteSources;
import dev.rsadvanced.network.InfiniteGridMenu;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AbstractGridContainerMenu.class, remap = false)
public abstract class GridContainerMenuMixin implements InfiniteGridMenu {
    @Shadow
    private Grid grid;
    @Unique
    private Set<ResourceKey> rsadvanced$infiniteResources = Set.of();
    @Unique
    private Set<ResourceKey> rsadvanced$lastSentResources;

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
        Set<ResourceKey> resources = grid != null && grid.isGridActive()
                ? InfiniteSources.collect(grid.getItemStorage()) : Set.of();
        if (resources.equals(rsadvanced$lastSentResources)) {
            return;
        }
        rsadvanced$lastSentResources = resources;
        AbstractGridContainerMenu menu = (AbstractGridContainerMenu) (Object) this;
        RSAdvanced.sendInfiniteResources(player, new InfiniteResourcesPayload(menu.containerId, resources));
    }
}
