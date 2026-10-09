package dev.rsadvanced.client;

import dev.rsadvanced.feature.anchor.AnchorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public final class AnchorScreen extends AbstractContainerScreen<AnchorMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace(
            "textures/gui/demo_background.png");
    private Button enabledButton;
    private Button visualizationButton;

    public AnchorScreen(AnchorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        enabledButton = addRenderableWidget(Button.builder(toggleText(menu.enabled), button ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
                .bounds(leftPos + 12, topPos + 130, 108, 22).build());
        visualizationButton = addRenderableWidget(Button.builder(visualizationText(), button ->
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1))
                .bounds(leftPos + 128, topPos + 130, 108, 22).build());
    }

    private Component toggleText(boolean enabled) {
        return Component.translatable(enabled ? "gui.rsadvanced.network_anchor.disable" : "gui.rsadvanced.network_anchor.enable");
    }

    private Component visualizationText() {
        return Component.translatable(menu.visualizing
                ? "gui.rsadvanced.network_anchor.hide" : "gui.rsadvanced.network_anchor.visualize");
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 12, 9, 0x404040, false);
        graphics.drawString(font, Component.translatable(menu.status.translationKey()), 12, 36, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.rsadvanced.network_anchor.area", menu.areaSize), 12, 56, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.rsadvanced.network_anchor.held", menu.heldSize), 12, 74, 0x404040, false);
        graphics.drawString(font, Component.translatable("gui.rsadvanced.network_anchor.cost", menu.cost), 12, 92, 0x404040, false);
        graphics.drawString(font, Component.translatable(menu.leader
                ? "gui.rsadvanced.network_anchor.leader" : "gui.rsadvanced.network_anchor.reserve"), 12, 110, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        enabledButton.setMessage(toggleText(menu.enabled));
        visualizationButton.setMessage(visualizationText());
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
