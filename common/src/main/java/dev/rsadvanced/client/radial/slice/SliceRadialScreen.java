// SPDX-License-Identifier: CC-BY-NC-4.0
// Adapted from ansi-sudo/Slice, commit 0e81c17eafc67cee8008a199634c8c920594a7e1.
// See licenses/Slice-NOTICE.txt for attribution and changes.
package dev.rsadvanced.client.radial.slice;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.client.radial.RadialMenuModel;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Slice's textured slots are adapted to generic commands instead of inventory slot selection. */
public final class SliceRadialScreen extends Screen {
    private static final int SLOT_SIZE = 26;
    private static final ResourceLocation SLOT = texture("radial_slot");
    private static final ResourceLocation ACTIVE_SLOT = texture("radial_slot_active");
    private static final ResourceLocation HOVERED_SLOT = texture("radial_slot_hovered");
    private final RadialMenuModel model;
    private List<SliceLayout.Slot> slots = List.of();
    private double radius;

    public SliceRadialScreen(RadialMenuModel model) {
        super(model.title());
        this.model = model;
    }

    @Override
    protected void init() {
        radius = Math.max(52, Math.min(90, Math.min(width, height) / 2.0 - 38));
        slots = SliceLayout.positions(model.entries().size(), width / 2, height / 2, radius);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!model.canRemainOpen().getAsBoolean()) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x77000000);
        int hovered = hovered(mouseX, mouseY);
        for (SliceLayout.Slot slot : slots) {
            var entry = model.entries().get(slot.index());
            boolean enabled = entry.enabled().getAsBoolean();
            boolean selected = entry.selected().getAsBoolean();
            ResourceLocation texture = enabled && hovered == slot.index() ? HOVERED_SLOT
                    : selected ? ACTIVE_SLOT : SLOT;
            graphics.blit(texture, slot.x() - SLOT_SIZE / 2, slot.y() - SLOT_SIZE / 2,
                    0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
            graphics.renderItem(entry.icon(), slot.x() - 8, slot.y() - 8);
            graphics.drawCenteredString(font, entry.label(), slot.x(), slot.y() + SLOT_SIZE / 2 + 2,
                    enabled ? selected ? 0x55FFFF : 0xFFFFFF : 0x777777);
        }
        graphics.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
        List<Component> status = model.status().get();
        int firstLine = height / 2 - status.size() * 6;
        for (int index = 0; index < status.size(); index++) {
            graphics.drawCenteredString(font, status.get(index), width / 2, firstLine + index * 12, 0x55FFFF);
        }
        graphics.drawCenteredString(font, Component.translatable("gui.rsadvanced.radial.help"),
                width / 2, height - 14, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int index = hovered(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        var entry = model.entries().get(index);
        if (!model.canRemainOpen().getAsBoolean() || !entry.enabled().getAsBoolean()) {
            return false;
        }
        entry.action().run();
        return true;
    }

    private int hovered(double mouseX, double mouseY) {
        return SliceLayout.hovered(mouseX - width / 2.0, mouseY - height / 2.0, slots,
                radius - 32, radius + 24);
    }

    private static ResourceLocation texture(String name) {
        return RSAdvanced.id("textures/gui/slice/" + name + ".png");
    }
}
