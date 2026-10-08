package dev.rsadvanced.client;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class CellResourceOverlay {
    private static boolean renderingResource;

    private CellResourceOverlay() {
    }

    public static void render(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (renderingResource || !(stack.getItem() instanceof InfiniteDiskItem disk)) {
            return;
        }
        var definition = disk.displayDefinition(stack);
        if (definition.isEmpty()) {
            return;
        }
        var resource = definition.get().resourceKey();
        var renderer = RefinedStorageClientApi.INSTANCE.getResourceRendering(resource.getClass());
        graphics.flush();
        renderingResource = true;
        graphics.pose().pushPose();
        try {
            // GUI items add their own 150-depth translation; fluids render at the supplied depth.
            // Both badges sit just above the disk, below vanilla count and tooltip layers.
            float depth = resource instanceof ItemResource ? 30 : 180;
            graphics.pose().translate(x + 8, y + 8, depth);
            graphics.pose().scale(0.5F, 0.5F, 1);
            renderer.render(resource, graphics, 0, 0);
            graphics.flush();
        } finally {
            graphics.pose().popPose();
            renderingResource = false;
        }
    }
}
