package dev.rsadvanced.test.client;

import dev.rsadvanced.client.AnchorVisualization;
import dev.rsadvanced.feature.anchor.AnchorStatePayload;
import dev.rsadvanced.feature.anchor.AnchorStatus;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

/** Deterministic multi-chunk views in actual loader clients; never packaged in release JARs. */
final class AnchorVisualizationClientCheck {
    private static final String[] SCREENSHOTS = {
            "network-anchor-boundaries.png", "network-anchor-inside.png", "network-anchor-coplanar.png",
            "network-anchor-concave.png", "network-anchor-diagonal.png", "network-anchor-near-corner.png",
            "network-anchor-on-corner.png", "network-anchor-hole.png"
    };
    private static BlockPos anchor;
    private static ChunkPos origin;
    private static int scene;

    private AnchorVisualizationClientCheck() {
    }

    static void start(Minecraft minecraft) {
        anchor = minecraft.player.blockPosition();
        origin = new ChunkPos(anchor);
        scene = 0;
        showScene(minecraft);
    }

    static boolean captureAndAdvance(Minecraft minecraft) throws IOException {
        Path screenshot = Path.of(System.getProperty("rsadvanced.test.clientReport"))
                .resolveSibling(SCREENSHOTS[scene]);
        try (var pixels = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            pixels.writeToFile(screenshot);
        }
        scene++;
        if (scene == SCREENSHOTS.length) {
            minecraft.setCameraEntity(minecraft.player);
            return true;
        }
        showScene(minecraft);
        return false;
    }

    private static void showScene(Minecraft minecraft) {
        int minX = origin.getMinBlockX();
        int minZ = origin.getMinBlockZ();
        int height = anchor.getY();
        Set<Long> chunks = switch (scene) {
            case 3 -> chunks(0, 0, 1, 0, 0, 1);
            case 4, 5, 6 -> chunks(0, 0, 1, 1);
            case 7 -> chunks(0, 0, 1, 0, 2, 0, 0, 1, 2, 1, 0, 2, 1, 2, 2, 2);
            default -> chunks(0, 0, 1, 0, 0, 1, 1, 1);
        };
        AnchorVisualization.receive(new AnchorStatePayload(-1, UUID.randomUUID(),
                minecraft.level.dimension().location(), anchor, AnchorStatus.ACTIVE,
                chunks.size(), chunks.size(), 90, true, true, true, chunks));

        if (scene == 2) {
            // Block north faces lie exactly on the overlay plane; the camera sees both faces and edges.
            for (int offsetX = 4; offsetX < 12; offsetX++) {
                for (int offsetY = 0; offsetY < 5; offsetY++) {
                    minecraft.level.setBlock(new BlockPos(minX + offsetX, height + offsetY, minZ),
                            (offsetX < 8 ? Blocks.STONE : Blocks.OAK_LEAVES).defaultBlockState(), 3);
                }
            }
            setCamera(minecraft, minX + 8, height + 2, minZ - 6, 0, 0);
        } else if (scene == 1) {
            setCamera(minecraft, minX + 8, height + 12, minZ + 8, -45, 10);
        } else if (scene == 5 || scene == 6) {
            // Stand nearly on the diagonal contact and look across the near plane.
            double distance = scene == 5 ? 0.1 : 0.01;
            setCamera(minecraft, minX + 16 + distance, height + 10, minZ + 16 - distance, 45, 0);
        } else {
            setCamera(minecraft, minX - 20, height + 18, minZ - 20, -45, 10);
        }
    }

    private static Set<Long> chunks(int... coordinates) {
        Set<Long> result = new HashSet<>();
        for (int index = 0; index < coordinates.length; index += 2) {
            result.add(ChunkPos.asLong(origin.x + coordinates[index], origin.z + coordinates[index + 1]));
        }
        return result;
    }

    private static void setCamera(Minecraft minecraft, double x, double y, double z, float yaw, float pitch) {
        ArmorStand camera = new ArmorStand(minecraft.level, x, y, z);
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        // LivingEntity cameras interpolate head yaw rather than body yaw.
        camera.setYHeadRot(yaw);
        camera.yHeadRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
        camera.setOldPosAndRot();
        minecraft.setCameraEntity(camera);
    }
}
