package dev.rsadvanced.test.client;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import dev.rsadvanced.feature.mining.MiningCardMenu;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.feature.mining.client.MiningCardScreen;
import dev.rsadvanced.feature.mining.client.MiningRadialScreen;
import java.nio.file.Path;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.inventory.ClickType;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Server-created menus, synchronized mode changes and in-world item presentation captures. */
final class MiningClientCheck {
    private static int stage;
    private static int ticks;
    private static HumanoidArm originalMainHand;
    private static CameraType originalCamera;

    private MiningClientCheck() {
    }

    static void start(Minecraft minecraft) {
        stage = 0;
        ticks = 0;
        // Captures must show the model even when the validation window loses desktop focus.
        minecraft.options.pauseOnLostFocus = false;
        minecraft.getConnection().sendCommand("give @s refinedstorage:network_card");
        minecraft.player.getInventory().selected = 6;
        minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
    }

    static boolean tick(Minecraft minecraft) throws Exception {
        if (++ticks > 200) {
            throw new AssertionError("Mining menu timed out at stage " + stage);
        }
        if (ticks < 10) {
            return false;
        }
        switch (stage) {
            case 0 -> {
                if (minecraft.screen instanceof MiningCardScreen
                        && minecraft.player.containerMenu instanceof MiningCardMenu menu) {
                    assertTrue(menu.containerId > 0);
                    for (int slot = 0; slot < 36; slot++) {
                        if (NetworkCardSlot.isNetworkCard(menu.getSlot(slot).getItem())) {
                            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot, 0,
                                    ClickType.QUICK_MOVE, minecraft.player);
                            advance();
                            break;
                        }
                    }
                }
            }
            case 1 -> {
                if (!NetworkCardSlot.card(minecraft.player.getMainHandItem()).isEmpty()) {
                    capture(minecraft, "mining-card-slot.png");
                    minecraft.screen.onClose();
                    minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
                    advance();
                }
            }
            case 2 -> {
                if (minecraft.player.containerMenu instanceof MiningCardMenu menu
                        && menu.getSlot(MiningCardMenu.CARD_SLOT).hasItem()) {
                    minecraft.gameMode.handleInventoryMouseClick(menu.containerId, MiningCardMenu.CARD_SLOT,
                            0, ClickType.QUICK_MOVE, minecraft.player);
                    advance();
                }
            }
            case 3 -> {
                if (NetworkCardSlot.card(minecraft.player.getMainHandItem()).isEmpty()) {
                    minecraft.screen.onClose();
                    net.minecraft.client.KeyMapping.click(
                            dev.rsadvanced.feature.mining.client.MiningClient.SELECT_MODE.getDefaultKey());
                    advance();
                }
            }
            case 4 -> {
                if (minecraft.screen instanceof MiningRadialScreen radial) {
                    capture(minecraft, "mining-radial.png");
                    // Area sector 1 points down; speed sector 2 is Fast, also pointing down.
                    radial.mouseClicked(radial.width / 2.0, radial.height / 2.0 + 38, 0);
                    radial.mouseClicked(radial.width / 2.0, radial.height / 2.0 + 86, 0);
                    radial.onClose();
                }
                if (MiningItem.radius(minecraft.player.getMainHandItem()) == 1
                        && MiningItem.mode(minecraft.player.getMainHandItem()) == MiningMode.FAST) {
                    var model = minecraft.getModelManager().getModel(ModelResourceLocation.inventory(
                            RSAdvanced.id("network_mining_tool")));
                    assertTrue(model != minecraft.getModelManager().getMissingModel());
                    assertTrue(model.isGui3d());
                    originalMainHand = minecraft.options.mainHand().get();
                    originalCamera = minecraft.options.getCameraType();
                    minecraft.options.mainHand().set(HumanoidArm.RIGHT);
                    minecraft.options.setCameraType(CameraType.FIRST_PERSON);
                    minecraft.player.setXRot(0);
                    advance();
                }
            }
            case 5 -> {
                capture(minecraft, "mining-first-person-right.png");
                minecraft.options.mainHand().set(HumanoidArm.LEFT);
                advance();
            }
            case 6 -> {
                capture(minecraft, "mining-first-person-left.png");
                minecraft.options.mainHand().set(HumanoidArm.RIGHT);
                minecraft.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                advance();
            }
            case 7 -> {
                capture(minecraft, "mining-third-person.png");
                minecraft.options.setCameraType(CameraType.FIRST_PERSON);
                minecraft.getConnection().sendCommand("gamemode survival @s");
                advance();
            }
            case 8 -> {
                if (!minecraft.gameMode.hasInfiniteItems()) {
                    minecraft.setScreen(new InventoryScreen(minecraft.player));
                    advance();
                }
            }
            case 9 -> {
                capture(minecraft, "mining-inventory.png");
                minecraft.screen.onClose();
                minecraft.getConnection().sendCommand("gamemode creative @s");
                minecraft.options.mainHand().set(originalMainHand);
                minecraft.options.setCameraType(originalCamera);
                minecraft.player.getInventory().selected = 0;
                return true;
            }
            default -> throw new AssertionError("Unexpected mining check stage " + stage);
        }
        return false;
    }

    private static void advance() {
        stage++;
        ticks = 0;
    }

    private static void capture(Minecraft minecraft, String filename) throws Exception {
        try (var pixels = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            pixels.writeToFile(Path.of(System.getProperty("rsadvanced.test.clientReport"))
                    .resolveSibling(filename));
        }
    }
}
