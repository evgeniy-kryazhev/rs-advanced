package dev.rsadvanced.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.feature.anchor.AnchorMenu;
import dev.rsadvanced.feature.anchor.AnchorStatePayload;
import dev.rsadvanced.feature.anchor.AnchorVisualizationRefreshPayload;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Selection lives outside the screen; never alters server chunk ownership. */
public final class AnchorVisualization {
    private static AnchorStatePayload selection;
    private static boolean awaitingFreshSnapshot;
    private static boolean outsideRadius;
    private static Set<Long> geometryChunks = Set.of();
    private static AnchorBoundaryGeometry geometry = AnchorBoundaryGeometry.build(Set.of());

    private AnchorVisualization() {
    }

    public static void receive(AnchorStatePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (payload.menuId() >= 0) {
            if (minecraft.player != null && minecraft.player.containerMenu instanceof AnchorMenu menu
                    && menu.containerId == payload.menuId() && menu.position.equals(payload.position())) {
                menu.status = payload.status();
                menu.areaSize = payload.areaSize();
                menu.heldSize = payload.heldSize();
                menu.cost = payload.cost();
                menu.leader = payload.leader();
                menu.enabled = payload.enabled();
                menu.visualizing = payload.visualizing();
            }
            return;
        }
        if (!payload.visualizing()) {
            clearSelection();
            return;
        }
        if (!payload.visible()) {
            selection = payload;
            awaitingFreshSnapshot = true;
            return;
        }
        if (minecraft.player != null && minecraft.player.distanceToSqr(payload.position().getCenter())
                > (double) payload.visualizationDistance() * payload.visualizationDistance()) {
            // A visible packet may have been sent before the player crossed the boundary locally.
            selection = payload;
            awaitingFreshSnapshot = true;
            outsideRadius = true;
            return;
        }
        if (!geometryChunks.equals(payload.chunks())) {
            geometry = AnchorBoundaryGeometry.build(payload.chunks());
            geometryChunks = payload.chunks();
        }
        selection = payload;
        awaitingFreshSnapshot = false;
    }

    public static void tick(Minecraft minecraft) {
        if (selection != null && (minecraft.level == null || minecraft.player == null
                || !minecraft.level.dimension().location().equals(selection.dimension()))) {
            clearSelection();
        }
        if (selection != null) {
            boolean outside = minecraft.player.distanceToSqr(selection.position().getCenter())
                    > (double) selection.visualizationDistance() * selection.visualizationDistance();
            if (outside) {
                // Never redisplay cached geometry on return before the server supplies the current area.
                awaitingFreshSnapshot = true;
            } else if (outsideRadius) {
                NetworkManager.sendToServer(new AnchorVisualizationRefreshPayload(selection.owner()));
            }
            outsideRadius = outside;
        }
    }

    private static void clearSelection() {
        selection = null;
        awaitingFreshSnapshot = false;
        outsideRadius = false;
        geometryChunks = Set.of();
        geometry = AnchorBoundaryGeometry.build(Set.of());
    }

    public static void render(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        Minecraft minecraft = Minecraft.getInstance();
        tick(minecraft);
        if (selection == null || !selection.visible() || awaitingFreshSnapshot || selection.chunks().isEmpty()) {
            return;
        }
        PoseStack pose = new PoseStack();
        pose.mulPose(viewMatrix);
        var cameraPosition = camera.getPosition();
        var buffers = minecraft.renderBuffers().bufferSource();
        var fill = buffers.getBuffer(AnchorRenderTypes.FACES);
        int bottom = minecraft.level.getMinBuildHeight();
        int top = minecraft.level.getMaxBuildHeight();
        for (var wall : geometry.walls()) {
            drawWall(pose, fill,
                    (float) (wall.start().x() * 16.0 - cameraPosition.x),
                    (float) (wall.start().z() * 16.0 - cameraPosition.z),
                    (float) (wall.end().x() * 16.0 - cameraPosition.x),
                    (float) (wall.end().z() * 16.0 - cameraPosition.z),
                    (float) (bottom - cameraPosition.y), (float) (top - cameraPosition.y));
        }
        buffers.endBatch(AnchorRenderTypes.FACES);
        var lines = buffers.getBuffer(AnchorRenderTypes.LINES);
        for (var wall : geometry.walls()) {
            drawLine(lines, viewMatrix, projectionMatrix,
                    relativePosition(wall.start(), bottom, cameraPosition),
                    relativePosition(wall.end(), bottom, cameraPosition));
            drawLine(lines, viewMatrix, projectionMatrix,
                    relativePosition(wall.start(), top, cameraPosition),
                    relativePosition(wall.end(), top, cameraPosition));
        }
        for (var corner : geometry.corners()) {
            drawLine(lines, viewMatrix, projectionMatrix,
                    relativePosition(corner, bottom, cameraPosition),
                    relativePosition(corner, top, cameraPosition));
        }
        buffers.endBatch(AnchorRenderTypes.LINES);
    }

    private static Vector3f relativePosition(AnchorBoundaryGeometry.Point point, int height,
            Vec3 cameraPosition) {
        // Subtract the camera in double precision before converting to GPU floats.
        return new Vector3f((float) (point.x() * 16.0 - cameraPosition.x),
                (float) (height - cameraPosition.y), (float) (point.z() * 16.0 - cameraPosition.z));
    }

    private static void drawLine(VertexConsumer vertices, Matrix4f view, Matrix4f projection,
            Vector3f start, Vector3f end) {
        view.transformPosition(start);
        view.transformPosition(end);
        if (!AnchorLineClipper.clip(start, end, projection)) {
            return;
        }
        Vector4f projectedStart = projection.transform(new Vector4f(start, 1));
        Vector4f projectedEnd = projection.transform(new Vector4f(end, 1));
        float screenX = projectedEnd.x / projectedEnd.w - projectedStart.x / projectedStart.w;
        float screenY = projectedEnd.y / projectedEnd.w - projectedStart.y / projectedStart.w;
        if (screenX * screenX + screenY * screenY < 0.0000000001f) {
            // A line pointing directly into the camera has no stable screen-space perpendicular.
            return;
        }
        Vector3f direction = AnchorLineClipper.shaderDirection(start, end, projection);
        vertices.addVertex(start.x, start.y, start.z).setColor(255, 255, 255, 255)
                .setNormal(direction.x, direction.y, direction.z);
        vertices.addVertex(end.x, end.y, end.z).setColor(255, 255, 255, 255)
                .setNormal(direction.x, direction.y, direction.z);
    }

    private static void drawWall(PoseStack pose, VertexConsumer vertices,
            float startX, float startZ, float endX, float endZ, float bottom, float top) {
        Matrix4f matrix = pose.last().pose();
        vertices.addVertex(matrix, startX, bottom, startZ).setColor(0.25f, 0.60f, 1.0f, 0.20f);
        vertices.addVertex(matrix, endX, bottom, endZ).setColor(0.25f, 0.60f, 1.0f, 0.20f);
        vertices.addVertex(matrix, endX, top, endZ).setColor(0.25f, 0.60f, 1.0f, 0.20f);
        vertices.addVertex(matrix, startX, top, startZ).setColor(0.25f, 0.60f, 1.0f, 0.20f);
    }
}
