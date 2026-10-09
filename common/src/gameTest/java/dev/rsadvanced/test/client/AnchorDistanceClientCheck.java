package dev.rsadvanced.test.client;

import dev.rsadvanced.client.AnchorVisualization;
import dev.rsadvanced.feature.anchor.AnchorMenu;
import dev.rsadvanced.feature.anchor.AnchorStatePayload;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Real server packets, teleports and menu actions; reflection stays in the development test mod. */
final class AnchorDistanceClientCheck {
    private static BlockPos anchor;
    private static Vec3 origin;
    private static int stage;
    private static int ticks;
    private static int initialChunks;
    private static int radius;
    private static AnchorStatePayload hiddenSnapshot;

    private AnchorDistanceClientCheck() {
    }

    static void start(Minecraft minecraft, BlockPos position) {
        anchor = position;
        origin = minecraft.player.position();
        stage = 0;
        ticks = 0;
    }

    static boolean tick(Minecraft minecraft) throws ReflectiveOperationException {
        ticks++;
        if (ticks > 600) {
            throw new AssertionError("Anchor distance check timed out at stage " + stage);
        }
        AnchorStatePayload snapshot = snapshot();
        switch (stage) {
            case 0 -> {
                if (snapshot != null && snapshot.visible() && !snapshot.chunks().isEmpty()) {
                    initialChunks = snapshot.chunks().size();
                    radius = snapshot.visualizationDistance();
                    assertTrue(radius == 96); // Test server differs from the client's default of 256.
                    teleport(minecraft, anchor.getCenter().add(radius + 16, 0, 0));
                    advance();
                }
            }
            case 1 -> {
                if (snapshot != null && !snapshot.visible()) {
                    assertTrue(snapshot.visualizing());
                    assertTrue(snapshot.chunks().isEmpty());
                    hiddenSnapshot = snapshot;
                    for (int offset = 1; offset <= 17; offset++) {
                        BlockPos position = anchor.east(offset);
                        minecraft.getConnection().sendCommand("setblock " + position.getX() + " "
                                + position.getY() + " " + position.getZ() + " refinedstorage:light_blue_cable");
                    }
                    advance();
                }
            }
            case 2 -> {
                assertTrue(snapshot != null && !snapshot.visible() && snapshot.visualizing());
                assertTrue(snapshot == hiddenSnapshot); // No duplicate hide or area packets while away.
                if (ticks >= 40) {
                    teleport(minecraft, origin);
                    advance();
                }
            }
            case 3 -> {
                if (snapshot != null && snapshot.visible() && snapshot.chunks().size() > initialChunks) {
                    teleport(minecraft, anchor.getCenter().add(0, radius + 16, 0));
                    advance();
                }
            }
            case 4 -> {
                if (snapshot != null && !snapshot.visible()) {
                    assertTrue(snapshot.visualizing());
                    teleport(minecraft, origin);
                    advance();
                }
            }
            case 5 -> {
                if (snapshot != null && snapshot.visible()) {
                    Field waiting = AnchorVisualization.class.getDeclaredField("awaitingFreshSnapshot");
                    waiting.setAccessible(true);
                    minecraft.player.setPos(anchor.getCenter().add(radius, 0, 0));
                    AnchorVisualization.tick(minecraft);
                    assertTrue(!waiting.getBoolean(null)); // The exact radius is included.
                    // Cross and return between server ticks: the refresh request must unpause the client.
                    minecraft.player.setPos(anchor.getCenter().add(radius + 1, 0, 0));
                    AnchorVisualization.tick(minecraft);
                    minecraft.player.setPos(origin);
                    AnchorVisualization.tick(minecraft);
                    advance();
                }
            }
            case 6 -> {
                if (ticks >= 10) {
                    Field waiting = AnchorVisualization.class.getDeclaredField("awaitingFreshSnapshot");
                    waiting.setAccessible(true);
                    assertTrue(!waiting.getBoolean(null));
                    minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(anchor.getCenter(), Direction.UP, anchor, false));
                    advance();
                }
            }
            case 7 -> {
                if (minecraft.player.containerMenu instanceof AnchorMenu menu && menu.visualizing) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    advance();
                }
            }
            case 8 -> {
                if (snapshot == null && minecraft.player.containerMenu instanceof AnchorMenu menu
                        && !menu.visualizing) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    advance();
                }
            }
            case 9 -> {
                if (snapshot != null && snapshot.visible()
                        && minecraft.player.containerMenu instanceof AnchorMenu menu) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    advance();
                }
            }
            case 10 -> {
                if (snapshot == null && minecraft.player.containerMenu instanceof AnchorMenu menu
                        && !menu.enabled && !menu.visualizing) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                    advance();
                }
            }
            case 11 -> {
                if (minecraft.player.containerMenu instanceof AnchorMenu menu && menu.enabled) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                    advance();
                }
            }
            case 12 -> {
                if (snapshot != null && snapshot.visible()) {
                    minecraft.player.closeContainer();
                    minecraft.getConnection().sendCommand("setblock " + anchor.getX() + " "
                            + anchor.getY() + " " + anchor.getZ() + " minecraft:air");
                    advance();
                }
            }
            case 13 -> {
                if (snapshot == null) {
                    return true;
                }
            }
            default -> throw new AssertionError("Unexpected distance test stage " + stage);
        }
        return false;
    }

    private static void advance() {
        stage++;
        ticks = 0;
    }

    private static void teleport(Minecraft minecraft, Vec3 position) {
        minecraft.getConnection().sendCommand("tp @s " + position.x + " " + position.y + " " + position.z);
    }

    private static AnchorStatePayload snapshot() throws ReflectiveOperationException {
        Field field = AnchorVisualization.class.getDeclaredField("selection");
        field.setAccessible(true);
        return (AnchorStatePayload) field.get(null);
    }
}
