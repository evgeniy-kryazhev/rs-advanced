package dev.rsadvanced.client;

import com.refinedmods.refinedstorage.api.resource.repository.ResourceRepository;
import com.refinedmods.refinedstorage.common.api.grid.view.GridResource;
import com.refinedmods.refinedstorage.common.grid.AbstractGridContainerMenu;
import dev.rsadvanced.network.InfiniteGridMenu;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class InfiniteGridDisplay {
    private InfiniteGridDisplay() {
    }

    public static void receive(InfiniteResourcesPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        AbstractContainerMenu menu = minecraft.player.containerMenu;
        if (menu.containerId == payload.menuId() && menu instanceof InfiniteGridMenu infiniteMenu) {
            infiniteMenu.rsadvanced$setInfiniteResources(payload.resources());
        }
    }

    public static boolean isInfinite(GridResource resource, ResourceRepository<GridResource> repository) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        if (!(minecraft.player.containerMenu instanceof AbstractGridContainerMenu menu)
                || menu.getRepository() != repository
                || !(menu instanceof InfiniteGridMenu infiniteMenu)) {
            return false;
        }

        return infiniteMenu.rsadvanced$getInfiniteResources().contains(resource.getResourceForRecipeMods());
    }
}
