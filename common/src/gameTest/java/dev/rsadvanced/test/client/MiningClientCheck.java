package dev.rsadvanced.test.client;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import dev.rsadvanced.feature.mining.MiningCardMenu;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.feature.mining.client.MiningCardScreen;
import dev.rsadvanced.feature.mining.client.MiningConfigurationScreen;
import dev.rsadvanced.feature.mining.MiningConfigurationMenu;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
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
import static dev.rsadvanced.test.TestAssertions.assertFalse;

/** Server-created menus, synchronized mode changes and in-world item presentation captures. */
final class MiningClientCheck {
    private static int stage;
    private static int ticks;
    private static int configurationStep;
    private static HumanoidArm originalMainHand;
    private static CameraType originalCamera;

    private MiningClientCheck() {
    }

    static void start(Minecraft minecraft) {
        stage = 0;
        ticks = 0;
        configurationStep = 0;
        // Captures must show the model even when the validation window loses desktop focus.
        minecraft.options.pauseOnLostFocus = false;
        minecraft.getConnection().sendCommand("give @s refinedstorage:network_card");
        minecraft.player.getInventory().selected = 6;
        minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
    }

    static boolean tick(Minecraft minecraft) throws Exception {
        // Login notifications must not cover the module panels in the presentation captures.
        minecraft.getToasts().clear();
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
                if (minecraft.screen instanceof MiningConfigurationScreen configuration) {
                    assertTrue(minecraft.player.containerMenu instanceof MiningConfigurationMenu);
                    assertTrue(minecraft.player.containerMenu.containerId > 0);
                    switch (configurationStep) {
                        case 0 -> {
                            capture(minecraft, "mining-configuration.png");
                            click(configuration, Component.literal("3×3"));
                        }
                        case 1 -> {
                            assertTrue(MiningItem.radius(minecraft.player.getMainHandItem()) == 1);
                            click(configuration, Component.translatable("gui.rsadvanced.inspection.module.speed"));
                            click(configuration, Component.translatable("gui.rsadvanced.mining_mode.fast"));
                        }
                        case 2 -> {
                            assertTrue(MiningItem.mode(minecraft.player.getMainHandItem()) == MiningMode.FAST);
                            capture(minecraft, "mining-configuration-speed.png");
                            click(configuration, Component.translatable("gui.rsadvanced.inspection.module.vein"));
                            click(configuration, Component.translatable("gui.rsadvanced.mining_vein"));
                        }
                        case 3 -> {
                            assertTrue(MiningItem.veinMining(minecraft.player.getMainHandItem()));
                            capture(minecraft, "mining-configuration-vein.png");
                            click(configuration, Component.translatable("gui.rsadvanced.inspection.module.energy"));
                        }
                        case 4 -> {
                            capture(minecraft, "mining-configuration-energy.png");
                            click(configuration, Component.translatable("gui.rsadvanced.inspection.module.network"));
                        }
                        case 5 -> {
                            capture(minecraft, "mining-configuration-network.png");
                            configuration.onClose();
                        }
                        default -> throw new AssertionError("Unexpected configuration step " + configurationStep);
                    }
                    configurationStep++;
                    ticks = 0;
                    return false;
                }
                if (MiningItem.radius(minecraft.player.getMainHandItem()) == 1
                        && MiningItem.mode(minecraft.player.getMainHandItem()) == MiningMode.FAST
                        && MiningItem.veinMining(minecraft.player.getMainHandItem())) {
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
                minecraft.player.swing(InteractionHand.MAIN_HAND);
                assertFalse(minecraft.player.swinging);
                capture(minecraft, "mining-first-person-right.png");
                minecraft.options.mainHand().set(HumanoidArm.LEFT);
                advance();
            }
            case 6 -> {
                minecraft.player.swing(InteractionHand.MAIN_HAND, true);
                assertFalse(minecraft.player.swinging);
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
                minecraft.getConnection().sendCommand("item replace entity @s weapon.offhand with rsadvanced:network_mining_tool");
                advance();
            }
            case 10 -> {
                if (minecraft.player.getOffhandItem().getItem() instanceof MiningItem) {
                    net.minecraft.client.KeyMapping.click(
                            dev.rsadvanced.feature.mining.client.MiningClient.SELECT_MODE.getDefaultKey());
                    advance();
                }
            }
            case 11 -> {
                if (minecraft.screen instanceof MiningConfigurationScreen configuration) {
                    assertTrue(minecraft.player.containerMenu instanceof MiningConfigurationMenu);
                    click(configuration, Component.translatable("gui.rsadvanced.inspection.module.speed"));
                    click(configuration, Component.translatable("gui.rsadvanced.mining_mode.slow"));
                    advance();
                }
            }
            case 12 -> {
                if (MiningItem.mode(minecraft.player.getOffhandItem()) == MiningMode.SLOW) {
                    minecraft.player.swing(InteractionHand.OFF_HAND);
                    assertFalse(minecraft.player.swinging);
                    capture(minecraft, "mining-configuration-offhand.png");
                    minecraft.screen.onClose();
                    MiningAnimationClientCheck.start(minecraft);
                    advance();
                }
            }
            case 13 -> {
                if (MiningAnimationClientCheck.tick(minecraft)) {
                    return true;
                }
            }
            default -> throw new AssertionError("Unexpected mining check stage " + stage);
        }
        return false;
    }

    private static void advance() {
        stage++;
        ticks = 0;
    }

    private static void click(MiningConfigurationScreen screen, Component label) {
        Button button = screen.children().stream()
                .filter(child -> child instanceof Button)
                .map(child -> (Button) child)
                .filter(candidate -> candidate.getMessage().getString().equals(label.getString()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing configuration button: " + label.getString()));
        assertTrue(button.active);
        assertTrue(screen.mouseClicked(button.getX() + button.getWidth() / 2.0,
                button.getY() + button.getHeight() / 2.0, 0));
    }

    private static void capture(Minecraft minecraft, String filename) throws Exception {
        try (var pixels = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            pixels.writeToFile(Path.of(System.getProperty("rsadvanced.test.clientReport"))
                    .resolveSibling(filename));
        }
    }
}
