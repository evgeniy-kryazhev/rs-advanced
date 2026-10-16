package dev.rsadvanced.feature.mining.client;

import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import dev.rsadvanced.feature.mining.MiningBalance;
import dev.rsadvanced.feature.mining.MiningConfigurationMenu;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningSelectionPayload;
import dev.rsadvanced.feature.mining.module.ToolModule;
import dev.rsadvanced.feature.mining.module.ToolModuleContainer;
import dev.rsadvanced.feature.mining.module.ToolModules;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Inspection is presentation only: all choices use the existing module commands and server validation. */
public final class MiningConfigurationScreen extends AbstractContainerScreen<MiningConfigurationMenu> {
    private static final int BACKGROUND = 0xF010171D;
    private static final int PANEL = 0xFF19242C;
    private static final int BORDER = 0xFF344650;
    private static final int ACCENT = 0xFF63D9E8;
    private static final int TEXT = 0xFFE3ECEF;
    private static final int MUTED = 0xFF92A7B1;
    private final List<Button> optionButtons = new ArrayList<>();
    private final List<ModulePoint> points = new ArrayList<>();
    private ToolModule selectedModule = ToolModules.AREA;
    private int settingsX;
    private int viewWidth;

    public MiningConfigurationScreen(MiningConfigurationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        imageWidth = Math.min(680, width - 12);
        imageHeight = Math.min(360, height - 12);
        super.init();
        settingsX = leftPos + imageWidth - 148;
        viewWidth = imageWidth - 158;
        points.clear();

        int nodeWidth = Math.min(108, (viewWidth - 16) / 2);
        int firstY = topPos + 44;
        int lastY = topPos + imageHeight - 70;
        int rightX = leftPos + viewWidth - nodeWidth;
        addModulePoint(ToolModules.AREA, leftPos + 10, firstY, nodeWidth, -0.33f, -0.27f);
        addModulePoint(ToolModules.SPEED, rightX, firstY, nodeWidth, 0.20f, 0.06f);
        addModulePoint(ToolModules.VEIN, leftPos + 10, lastY, nodeWidth, -0.07f, -0.02f);
        addModulePoint(ToolModules.ENERGY, rightX, lastY, nodeWidth, 0.33f, 0.16f);
        addModulePoint(ToolModules.NETWORK, leftPos + (viewWidth - nodeWidth) / 2,
                topPos + imageHeight - 46, nodeWidth, -0.08f, -0.08f);
        addRenderableWidget(Button.builder(Component.literal("×"), button -> onClose())
                .bounds(leftPos + imageWidth - 24, topPos + 7, 18, 18).build());
        rebuildOptions();
    }

    private void addModulePoint(ToolModule module, int x, int y, int nodeWidth, float anchorX, float anchorY) {
        if (modules().find(module.id()).isEmpty()) {
            return;
        }
        points.add(new ModulePoint(module, x, y, nodeWidth, anchorX, anchorY));
        addRenderableWidget(new InspectionButton(x, y, nodeWidth, 20, moduleName(module), button -> {
            selectedModule = module;
            rebuildOptions();
        }, () -> selectedModule == module));
    }

    private ToolModuleContainer modules() {
        // Read synchronized state rather than showing an optimistic selection the server might reject.
        return MiningItem.modules(menu.tool());
    }

    private void rebuildOptions() {
        for (Button button : optionButtons) {
            removeWidget(button);
        }
        optionButtons.clear();
        var container = modules();
        int optionY = topPos + 82;
        for (var option : selectedModule.options(container)) {
            Button button = new InspectionButton(settingsX + 8, optionY, 124, 22, option.label(), clicked -> {
                NetworkManager.sendToServer(new MiningSelectionPayload(menu.reference(), selectedModule.id(), option.id()));
            }, () -> selectedModule.selectedOption(modules()).equals(option.id()));
            button.active = option.enabled();
            optionButtons.add(addRenderableWidget(button));
            optionY += 27;
        }
    }

    @Override
    protected void containerTick() {
        if (minecraft.player == null || !menu.stillValid(minecraft.player)) {
            onClose();
            return;
        }
        var options = selectedModule.options(modules());
        for (int index = 0; index < optionButtons.size(); index++) {
            optionButtons.get(index).active = index < options.size() && options.get(index).enabled();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, BACKGROUND);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, BORDER);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + 2, ACCENT);
        graphics.drawString(font, title, leftPos + 12, topPos + 12, TEXT, false);
        graphics.fill(leftPos + 10, topPos + 32, leftPos + imageWidth - 10, topPos + 33, BORDER);
        graphics.fill(settingsX, topPos + 39, leftPos + imageWidth - 8, topPos + imageHeight - 34, PANEL);

        int centerX = leftPos + viewWidth / 2;
        int centerY = topPos + (imageHeight - 30) / 2;
        int modelSize = modelSize();
        // The existing baked item model includes its GUI transform and textures on both loaders.
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        float modelScale = modelSize / 16.0f;
        graphics.pose().scale(modelScale, modelScale, 1);
        graphics.renderItem(menu.tool(), -8, -8);
        graphics.pose().popPose();

        // Flush the item before drawing the diagram so the module markers remain visible over its depth.
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (ModulePoint point : points) {
            int anchorX = centerX + Math.round(point.anchorX() * modelSize);
            int anchorY = centerY + Math.round(point.anchorY() * modelSize);
            int lineX = point.x() + point.width() / 2;
            int lineY = point.y() < centerY ? point.y() + 20 : point.y();
            int elbowY = point.y() < centerY ? lineY + 5 : lineY - 5;
            int color = selectedModule == point.module() ? ACCENT : BORDER;
            graphics.fill(lineX, Math.min(lineY, elbowY), lineX + 1, Math.max(lineY, elbowY) + 1, color);
            graphics.fill(Math.min(lineX, anchorX), elbowY, Math.max(lineX, anchorX) + 1, elbowY + 1, color);
            graphics.fill(anchorX, Math.min(elbowY, anchorY), anchorX + 1, Math.max(elbowY, anchorY) + 1, color);
            graphics.fill(anchorX - 3, anchorY - 3, anchorX + 4, anchorY + 4, BACKGROUND);
            graphics.renderOutline(anchorX - 3, anchorY - 3, 7, 7, color);
            graphics.fill(anchorX - 1, anchorY - 1, anchorX + 2, anchorY + 2, color);
        }
        graphics.pose().popPose();

        graphics.drawString(font, moduleName(selectedModule), settingsX + 8, topPos + 49, ACCENT, false);
        graphics.drawString(font, Component.translatable("gui.rsadvanced.inspection.settings"),
                settingsX + 8, topPos + 65, MUTED, false);
        renderModuleDetails(graphics);
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable("gui.rsadvanced.inspection.help").getString(), imageWidth - 24),
                leftPos + 12, topPos + imageHeight - 19, MUTED, false);
    }

    private void renderModuleDetails(GuiGraphics graphics) {
        List<Component> lines = new ArrayList<>();
        var container = modules();
        if (selectedModule == ToolModules.ENERGY) {
            lines.add(Component.translatable("gui.rsadvanced.inspection.energy", MiningItem.energy(menu.tool())));
            lines.add(Component.translatable("gui.rsadvanced.inspection.capacity", MiningBalance.settings().capacity()));
        } else if (selectedModule == ToolModules.NETWORK) {
            lines.add(Component.translatable(NetworkCardSlot.card(menu.tool()).isEmpty()
                    ? "gui.rsadvanced.mining_offline" : "gui.rsadvanced.inspection.card_installed"));
            lines.add(Component.translatable("gui.rsadvanced.inspection.network_help"));
        } else {
            lines.addAll(selectedModule.status(container));
            if (selectedModule == ToolModules.VEIN && !MiningBalance.settings().veinEnabled()) {
                lines.add(Component.translatable("gui.rsadvanced.inspection.disabled"));
            }
        }
        int lineY = topPos + 86 + optionButtons.size() * 27;
        for (Component line : lines) {
            for (var wrapped : font.split(line, 124)) {
                graphics.drawString(font, wrapped, settingsX + 8, lineY, TEXT, false);
                lineY += 12;
            }
            lineY += 6;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // This inspection view has no visible inventory or inventory labels.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int centerX = leftPos + viewWidth / 2;
            int centerY = topPos + (imageHeight - 30) / 2;
            int modelSize = modelSize();
            for (ModulePoint point : points) {
                int anchorX = centerX + Math.round(point.anchorX() * modelSize);
                int anchorY = centerY + Math.round(point.anchorY() * modelSize);
                if (Math.abs(mouseX - anchorX) <= 6 && Math.abs(mouseY - anchorY) <= 6) {
                    selectedModule = point.module();
                    rebuildOptions();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static Component moduleName(ToolModule module) {
        return Component.translatable("gui.rsadvanced.inspection.module." + module.id().getPath());
    }

    private int modelSize() {
        return Math.min(viewWidth - 30, (int) (imageHeight * 0.45));
    }

    private record ModulePoint(ToolModule module, int x, int y, int width, float anchorX, float anchorY) {
    }

    private static final class InspectionButton extends Button {
        private final BooleanSupplier selected;

        private InspectionButton(int x, int y, int width, int height, Component label,
                                 OnPress action, BooleanSupplier selected) {
            super(x, y, width, height, label, action, DEFAULT_NARRATION);
            this.selected = selected;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean highlighted = selected.getAsBoolean();
            int border = highlighted || isHoveredOrFocused() ? ACCENT : BORDER;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), PANEL);
            graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), active ? border : BORDER);
            int textColor = active ? TEXT : MUTED;
            if (active && highlighted) {
                textColor = ACCENT;
            }
            renderScrollingString(graphics, net.minecraft.client.Minecraft.getInstance().font, 5, textColor);
        }
    }
}
