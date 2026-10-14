package dev.rsadvanced.feature.mining.client;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.feature.mining.MiningBalance;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.feature.mining.MiningSelectionPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

/** Two rings keep mining speed and area independent. Clicking commits a server-validated selection. */
public final class MiningRadialScreen extends Screen {
    private final InteractionHand hand;
    private SlotReference reference;
    private MiningMode selectedMode;
    private int selectedRadius;

    public MiningRadialScreen(InteractionHand hand) {
        super(Component.translatable("gui.rsadvanced.mining_radial"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        reference = RefinedStorageApi.INSTANCE.createInventorySlotReference(minecraft.player, hand);
        var stack = minecraft.player.getItemInHand(hand);
        selectedMode = MiningItem.mode(stack);
        selectedRadius = MiningItem.radius(stack);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (minecraft.player == null || !(minecraft.player.getItemInHand(hand).getItem() instanceof MiningItem)) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen renders its blurred world background. Draw it before the rings and labels.
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x77000000);
        int centerX = width / 2;
        int centerY = height / 2;
        double outer = outerRadius();
        double distance = Math.hypot(mouseX - centerX, mouseY - centerY);
        int hoverMode = sector(mouseX, mouseY, 5);
        int hoverArea = sector(mouseX, mouseY, 3);
        for (int index = 0; index < 5; index++) {
            MiningMode mode = MiningMode.values()[index];
            boolean enabled = mode != MiningMode.VEIN || MiningBalance.settings().veinEnabled();
            int color = !enabled ? 0xFF444444 : mode == selectedMode ? 0xFF007F94 : 0xFF172B3E;
            if (enabled && distance >= outer * 0.56 && distance <= outer && hoverMode == index) {
                color = 0xFF16A8BD;
            }
            ring(graphics, centerX, centerY, outer * 0.56, outer, index, 5, color);
            double angle = 2 * Math.PI * (index + 0.5) / 5 - Math.PI / 2;
            graphics.drawCenteredString(font, Component.translatable("gui.rsadvanced.mining_mode." + mode.getSerializedName()),
                    centerX + (int) (outer * 0.78 * Math.cos(angle)), centerY + (int) (outer * 0.78 * Math.sin(angle)) - 4, 0xFFFFFF);
        }
        for (int index = 0; index < 3; index++) {
            int color = index == selectedRadius ? 0xFF007F94 : 0xFF172B3E;
            if (distance >= outer * 0.145 && distance < outer * 0.53 && hoverArea == index) {
                color = 0xFF16A8BD;
            }
            ring(graphics, centerX, centerY, outer * 0.145, outer * 0.53, index, 3, color);
            double angle = 2 * Math.PI * (index + 0.5) / 3 - Math.PI / 2;
            int size = index * 2 + 1;
            graphics.drawCenteredString(font, size + "×" + size,
                    centerX + (int) (outer * 0.34 * Math.cos(angle)), centerY + (int) (outer * 0.34 * Math.sin(angle)) - 4, 0xFFFFFF);
        }
        graphics.drawCenteredString(font, title, centerX, centerY - (int) outer - 18, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("gui.rsadvanced.mining_radial.help"),
                centerX, centerY + (int) outer + 12, 0xFFFFFF);
    }

    private double outerRadius() {
        // Minecraft can use a 240-pixel GUI height; keep labels and help inside that viewport.
        return Math.max(64, Math.min(110, height / 2.0 - 28));
    }

    private int sector(double x, double y, int count) {
        double angle = Math.atan2(y - height / 2.0, x - width / 2.0) + Math.PI / 2;
        if (angle < 0) {
            angle += Math.PI * 2;
        }
        return Math.min(count - 1, (int) (angle / (Math.PI * 2) * count));
    }

    private static void ring(GuiGraphics graphics, int x, int y, double inner, double outer,
                             int index, int count, int color) {
        var vertices = graphics.bufferSource().getBuffer(RenderType.gui());
        var matrix = graphics.pose().last().pose();
        double start = Math.PI * 2 * index / count - Math.PI / 2 + 0.015;
        double end = Math.PI * 2 * (index + 1) / count - Math.PI / 2 - 0.015;
        for (int segment = 0; segment < 32; segment++) {
            double first = start + (end - start) * segment / 32;
            double second = start + (end - start) * (segment + 1) / 32;
            vertices.addVertex(matrix, (float) (x + inner * Math.cos(first)), (float) (y + inner * Math.sin(first)), 0).setColor(color);
            vertices.addVertex(matrix, (float) (x + inner * Math.cos(second)), (float) (y + inner * Math.sin(second)), 0).setColor(color);
            vertices.addVertex(matrix, (float) (x + outer * Math.cos(second)), (float) (y + outer * Math.sin(second)), 0).setColor(color);
            vertices.addVertex(matrix, (float) (x + outer * Math.cos(first)), (float) (y + outer * Math.sin(first)), 0).setColor(color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        double outer = outerRadius();
        double distance = Math.hypot(mouseX - width / 2.0, mouseY - height / 2.0);
        if (distance >= outer * 0.56 && distance <= outer) {
            MiningMode mode = MiningMode.values()[sector(mouseX, mouseY, 5)];
            if (mode != MiningMode.VEIN || MiningBalance.settings().veinEnabled()) {
                selectedMode = mode;
            }
        } else if (distance >= outer * 0.145 && distance <= outer * 0.53) {
            selectedRadius = sector(mouseX, mouseY, 3);
        } else {
            return false;
        }
        NetworkManager.sendToServer(new MiningSelectionPayload(reference, selectedMode.ordinal(), selectedRadius));
        return true;
    }
}
