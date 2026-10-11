package dev.rsadvanced.test.client;

import com.refinedmods.refinedstorage.common.api.support.HelpTooltipComponent;
import com.refinedmods.refinedstorage.common.support.tooltip.HelpClientTooltipComponent;
import com.refinedmods.refinedstorage.common.support.tooltip.SmallTextClientTooltipComponent;
import dev.rsadvanced.feature.anchor.AnchorContent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Renders the normal item-tooltip path in both loaders, with deterministic Shift state. */
public final class AnchorTooltipClientCheck extends Screen {
    private static final String[] SCREENSHOTS = {
            "network-anchor-tooltip.png", "rs-machine-tooltip.png",
            "network-anchor-tooltip-shift.png", "rs-machine-tooltip-shift.png"
    };
    private static Boolean shiftOverride;
    private final List<ItemStack> stacks;
    private final Runnable completed;
    private int scenario;
    private int renderedFrames;

    private AnchorTooltipClientCheck(List<ItemStack> stacks, Runnable completed) {
        super(Component.literal("Anchor tooltip comparison"));
        this.stacks = stacks;
        this.completed = completed;
    }

    public static Boolean shiftOverride() {
        return shiftOverride;
    }

    static void start(Minecraft minecraft, Runnable completed) {
        Item reference = BuiltInRegistries.ITEM.stream()
                .filter(item -> {
                    var identifier = BuiltInRegistries.ITEM.getKey(item);
                    return identifier.getNamespace().equals("refinedstorage")
                            && identifier.getPath().endsWith("network_transmitter");
                })
                .findFirst().orElseThrow();
        minecraft.setScreen(new AnchorTooltipClientCheck(
                List.of(new ItemStack(AnchorContent.ITEM.get()), new ItemStack(reference)), completed));
        shiftOverride = false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        try {
            shiftOverride = scenario >= 2;
            ItemStack stack = stacks.get(scenario % 2);
            var help = (HelpTooltipComponent) stack.getTooltipImage().orElseThrow();
            var renderedHelp = ClientTooltipComponent.create(help);
            if (shiftOverride) {
                assertTrue(Screen.hasShiftDown());
                assertTrue(renderedHelp instanceof HelpClientTooltipComponent);
                assertTrue(renderedHelp.getHeight() >= 20);
            } else {
                assertTrue(!Screen.hasShiftDown());
                assertTrue(renderedHelp instanceof SmallTextClientTooltipComponent);
            }
            if (stack.is(AnchorContent.ITEM.get())) {
                List<Component> additionalLines = new ArrayList<>();
                stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY,
                        additionalLines, TooltipFlag.NORMAL);
                assertTrue(additionalLines.isEmpty());
                assertEquals("block.rsadvanced.network_anchor.help",
                        ((net.minecraft.network.chat.contents.TranslatableContents) help.text().getContents()).getKey());
            }
            graphics.fill(0, 0, width, height, 0xFF25282E);
            graphics.drawCenteredString(font, "RS Advanced / Refined Storage: Shift = " + shiftOverride,
                    width / 2, 25, 0xFFFFFFFF);
            int itemX = width / 3;
            int itemY = height / 2;
            graphics.renderItem(stack, itemX, itemY);
            // Use the real loader tooltip factory and RS renderer, including name and help component.
            graphics.renderTooltip(font, stack, itemX + 20, itemY);
            graphics.flush();
            renderedFrames++;
            if (renderedFrames == 12) {
                Path screenshot = Path.of(System.getProperty("rsadvanced.test.clientReport"))
                        .resolveSibling(SCREENSHOTS[scenario]);
                try (var pixels = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                    pixels.writeToFile(screenshot);
                }
                scenario++;
                renderedFrames = 0;
                if (scenario == SCREENSHOTS.length) {
                    shiftOverride = null;
                    minecraft.execute(completed);
                }
            }
        } catch (Exception | AssertionError failure) {
            shiftOverride = null;
            throw new IllegalStateException("Anchor tooltip comparison failed", failure);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
