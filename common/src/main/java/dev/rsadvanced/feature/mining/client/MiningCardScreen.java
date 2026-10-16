package dev.rsadvanced.feature.mining.client;

import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen;
import dev.rsadvanced.feature.mining.MiningCardMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Shares RS's one-card layout, inventory rendering and tooltip behavior. */
public final class MiningCardScreen extends AbstractBaseScreen<MiningCardMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "refinedstorage", "textures/gui/network_transmitter.png");

    public MiningCardScreen(MiningCardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        inventoryLabelY = 42;
        imageWidth = 176;
        imageHeight = 137;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        graphics.drawString(font, Component.translatable("gui.rsadvanced.mining_card.slot"), 30, 25, 4210752, false);
    }

    @Override
    protected ResourceLocation getTexture() {
        return TEXTURE;
    }
}
