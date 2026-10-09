package dev.rsadvanced.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Clips view-space endpoints before the vanilla line shader divides by clip-space W. */
public final class AnchorLineClipper {
    // Match the vanilla line shader's view shrink, plus VIEW_OFFSET_Z_LAYERING.
    private static final float VIEW_SCALE = (1.0f - 1.0f / 256.0f) * 0.99975586f;
    private static final float NEAR_PLANE_MARGIN = 0.0001f;

    private AnchorLineClipper() {
    }

    public static boolean clip(Vector3f start, Vector3f end, Matrix4f projection) {
        float startDistance = nearPlaneDistance(start, projection);
        float endDistance = nearPlaneDistance(end, projection);
        if (startDistance < 0 && endDistance < 0) {
            return false;
        }
        if (startDistance < 0) {
            start.lerp(end, startDistance / (startDistance - endDistance));
        } else if (endDistance < 0) {
            end.lerp(start, endDistance / (endDistance - startDistance));
        }
        return start.distanceSquared(end) > 0.00000001f;
    }

    static Vector3f shaderDirection(Vector3f start, Vector3f end, Matrix4f projection) {
        Vector3f direction = new Vector3f(end).sub(start).normalize();
        // Match the signed-byte vertex normal, including rounding toward zero.
        direction.set((int) (direction.x * 127) / 127.0f,
                (int) (direction.y * 127) / 127.0f, (int) (direction.z * 127) / 127.0f);
        Vector4f projectedDirection = projection.transform(new Vector4f(direction, 0));
        if (projectedDirection.z + projectedDirection.w < 0) {
            // Position + Normal must remain visible even when view bobbing tilts the projection.
            direction.negate();
        }
        return direction;
    }

    private static float nearPlaneDistance(Vector3f point, Matrix4f projection) {
        Vector4f clipPosition = new Vector4f(point.x * VIEW_SCALE, point.y * VIEW_SCALE,
                point.z * VIEW_SCALE, 1.0f);
        projection.transform(clipPosition);
        return clipPosition.z + clipPosition.w - NEAR_PLANE_MARGIN;
    }
}
