package dev.rsadvanced.test.client;

import com.refinedmods.refinedstorage.common.storage.storageblock.StorageBlockContainerMenu;
import com.refinedmods.refinedstorage.common.storage.storageblock.StorageBlockScreen;
import com.refinedmods.refinedstorage.common.storage.AbstractProgressStorageScreen;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.CellResourceNames;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.network.InfiniteResourceMenu;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

import static dev.rsadvanced.test.TestAssertions.assertTrue;
import static dev.rsadvanced.test.TestAssertions.assertEquals;

/** Opens both storage menus through the real client's interaction packet and server response. */
public final class StorageBlockClientCheck {
    private static BlockPos firstPosition;
    private static int stage;
    private static int waitingTicks;

    private StorageBlockClientCheck() {
    }

    public static void start(Minecraft minecraft) {
        firstPosition = minecraft.player.blockPosition().west(2);
        stage = 0;
        waitingTicks = 0;
        interact(minecraft, firstPosition);
    }

    public static boolean tick(Minecraft minecraft) throws Exception {
        waitingTicks++;
        if (waitingTicks > 200) {
            throw new AssertionError("Server did not open infinite storage block menu at stage " + stage);
        }
        // Resource-pack completion precedes the end of Minecraft's loading-overlay fade.
        if (minecraft.getOverlay() != null) {
            return false;
        }
        if (!(minecraft.screen instanceof StorageBlockScreen)
                || !(minecraft.player.containerMenu instanceof StorageBlockContainerMenu menu)) {
            return false;
        }
        assertTrue(menu.containerId > 0);
        assertTrue(!menu.hasCapacity());
        var infiniteMenu = (InfiniteResourceMenu) menu;
        if (infiniteMenu.rsadvanced$getInfiniteResources().isEmpty()) {
            return false;
        }
        var resource = CellDefinitions.displayDefinition(RSAdvanced.id(stage == 0 ? "cobblestone" : "water"))
                .orElseThrow().resourceKey();
        assertEquals(Set.of(resource), infiniteMenu.rsadvanced$getInfiniteResources());
        assertEquals(1.0, menu.getProgress());
        var tooltipMethod = AbstractProgressStorageScreen.class.getDeclaredMethod("createProgressTooltip");
        tooltipMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        var tooltip = (List<Component>) tooltipMethod.invoke(minecraft.screen);
        assertEquals(1, tooltip.size());
        assertEquals(Component.translatable("tooltip.rsadvanced.drive_infinite_source",
                CellResourceNames.name(resource)).getString(), tooltip.getFirst().getString());
        // Hover the real progress widget so the screenshot also verifies its resource tooltip.
        var window = minecraft.getWindow();
        double mouseX = (window.getGuiScaledWidth() / 2.0) * window.getScreenWidth() / window.getGuiScaledWidth();
        double mouseY = (window.getGuiScaledHeight() / 2.0 - 30) * window.getScreenHeight()
                / window.getGuiScaledHeight();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(window.getWindow(), mouseX, mouseY);
        if (waitingTicks < 20) {
            return false;
        }
        try (var pixels = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            String screenshotName = stage == 0 ? "infinite-item-storage-menu.png" : "infinite-fluid-storage-menu.png";
            Path screenshot = Path.of(System.getProperty("rsadvanced.test.clientReport")).resolveSibling(screenshotName);
            pixels.writeToFile(screenshot);
        }
        minecraft.screen.onClose();
        if (stage == 0) {
            stage = 1;
            waitingTicks = 0;
            interact(minecraft, firstPosition.west());
            return false;
        }
        return true;
    }

    private static void interact(Minecraft minecraft, BlockPos position) {
        minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(position.getCenter(), Direction.UP, position, false));
    }
}
