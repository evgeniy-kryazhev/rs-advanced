package dev.rsadvanced.test.client;

import com.refinedmods.refinedstorage.api.network.node.grid.GridExtractMode;
import com.refinedmods.refinedstorage.api.network.node.grid.GridInsertMode;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.grid.WirelessGridContainerMenu;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.rsadvanced.feature.terminal.TerminalCardMenu;
import dev.rsadvanced.feature.terminal.TerminalItem;
import dev.rsadvanced.feature.terminal.client.TerminalCardScreen;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Uses actual item-use, slot-click and RS resource packets against the validation server. */
final class TerminalClientCheck {
    private static int stage;
    private static int ticks;
    private static long initialEnergy;
    private static BlockPos receiver;

    private TerminalClientCheck() {
    }

    static void start(Minecraft minecraft) {
        stage = 0;
        ticks = 0;
        receiver = minecraft.player.blockPosition().west(2).above();
        minecraft.player.getInventory().selected = 7;
        initialEnergy = RefinedStorageApi.INSTANCE.getEnergyStorage(minecraft.player.getMainHandItem())
                .orElseThrow().getStored();
        use(minecraft, false);
    }

    static boolean tick(Minecraft minecraft) throws Exception {
        ticks++;
        if (ticks > 200) {
            capture(minecraft, "terminal-failure.png");
            throw new AssertionError("Terminal client check timed out at stage " + stage
                    + ", menu=" + minecraft.player.containerMenu.getClass().getSimpleName());
        }
        if (minecraft.getOverlay() != null || ticks < 10) {
            return false;
        }
        switch (stage) {
            case 0 -> {
                if (minecraft.screen instanceof TerminalCardScreen
                        && minecraft.player.containerMenu instanceof TerminalCardMenu menu) {
                    assertTrue(menu.containerId > 0);
                    minecraft.gameMode.handleInventoryMouseClick(menu.containerId, 35, 0, ClickType.QUICK_MOVE, minecraft.player);
                    advance();
                }
            }
            case 1 -> {
                if (minecraft.player.containerMenu.getSlot(TerminalCardMenu.CARD_SLOT).hasItem()) {
                    assertTrue(TerminalItem.isNetworkCard(TerminalItem.card(minecraft.player.getMainHandItem())));
                    capture(minecraft, "terminal-card-slot.png");
                    minecraft.screen.onClose();
                    use(minecraft, false);
                    advance();
                }
            }
            case 2 -> {
                if (minecraft.player.containerMenu instanceof WirelessGridContainerMenu menu) {
                    assertTrue(menu.containerId > 0);
                    assertTrue(!menu.getRepository().copyBackingList().getAll().isEmpty());
                    capture(minecraft, "terminal-rs-grid.png");
                    menu.onExtract(new ItemResource(Items.COBBLESTONE), GridExtractMode.ENTIRE_RESOURCE, true);
                    advance();
                }
            }
            case 3 -> {
                if (minecraft.player.containerMenu.getCarried().is(Items.COBBLESTONE)) {
                    assertEquals(64, minecraft.player.containerMenu.getCarried().getCount());
                    ((WirelessGridContainerMenu) minecraft.player.containerMenu).onInsert(GridInsertMode.ENTIRE_RESOURCE, false);
                    advance();
                }
            }
            case 4 -> {
                if (minecraft.player.containerMenu.getCarried().isEmpty()) {
                    long remaining = RefinedStorageApi.INSTANCE.getEnergyStorage(minecraft.player.getMainHandItem())
                            .orElseThrow().getStored();
                    assertTrue(remaining < initialEnergy);
                    minecraft.screen.onClose();
                    use(minecraft, true);
                    advance();
                }
            }
            case 5 -> {
                if (minecraft.player.containerMenu instanceof TerminalCardMenu menu) {
                    assertTrue(menu.getSlot(TerminalCardMenu.CARD_SLOT).hasItem());
                    minecraft.gameMode.handleInventoryMouseClick(menu.containerId, TerminalCardMenu.CARD_SLOT,
                            0, ClickType.QUICK_MOVE, minecraft.player);
                    advance();
                }
            }
            case 6 -> {
                if (TerminalItem.card(minecraft.player.getMainHandItem()).isEmpty()) {
                    var menu = (TerminalCardMenu) minecraft.player.containerMenu;
                    assertTrue(!menu.getSlot(TerminalCardMenu.CARD_SLOT).hasItem());
                    int cards = 0;
                    int sourceSlot = -1;
                    for (int slot = 0; slot < 36; slot++) {
                        if (TerminalItem.isNetworkCard(menu.getSlot(slot).getItem())) {
                            cards += menu.getSlot(slot).getItem().getCount();
                            sourceSlot = slot;
                        }
                    }
                    assertEquals(1, cards);
                    minecraft.gameMode.handleInventoryMouseClick(menu.containerId, sourceSlot, 0, ClickType.QUICK_MOVE,
                            minecraft.player);
                    advance();
                }
            }
            case 7 -> {
                if (!TerminalItem.card(minecraft.player.getMainHandItem()).isEmpty()) {
                    minecraft.screen.onClose();
                    use(minecraft, false);
                    advance();
                }
            }
            case 8 -> {
                if (minecraft.player.containerMenu instanceof WirelessGridContainerMenu) {
                    minecraft.getConnection().sendCommand("setblock " + receiver.getX() + " " + receiver.getY()
                            + " " + receiver.getZ() + " minecraft:air");
                    advance();
                }
            }
            case 9 -> {
                if (minecraft.player.containerMenu == minecraft.player.inventoryMenu) {
                    minecraft.player.getInventory().selected = 0;
                    return true;
                }
            }
            default -> throw new AssertionError("Unexpected terminal check stage " + stage);
        }
        return false;
    }

    private static void use(Minecraft minecraft, boolean configure) {
        if (configure) {
            minecraft.getConnection().send(new ServerboundPlayerCommandPacket(minecraft.player,
                    ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY));
        }
        minecraft.gameMode.useItem(minecraft.player, InteractionHand.MAIN_HAND);
        if (configure) {
            minecraft.getConnection().send(new ServerboundPlayerCommandPacket(minecraft.player,
                    ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
        }
    }

    private static void advance() {
        stage++;
        ticks = 0;
    }

    private static void capture(Minecraft minecraft, String filename) throws Exception {
        try (var pixels = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            pixels.writeToFile(Path.of(System.getProperty("rsadvanced.test.clientReport")).resolveSibling(filename));
        }
    }
}
