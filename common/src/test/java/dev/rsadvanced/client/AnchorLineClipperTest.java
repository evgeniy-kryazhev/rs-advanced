package dev.rsadvanced.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorLineClipperTest {
    private final Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(70), 1.6f, 0.05f, 1000);

    @Test
    void visibleSegmentIsUnchanged() {
        Vector3f start = new Vector3f(1, -10, -5);
        Vector3f end = new Vector3f(1, 10, -5);
        assertTrue(AnchorLineClipper.clip(start, end, projection));
        assertEquals(new Vector3f(1, -10, -5), start);
        assertEquals(new Vector3f(1, 10, -5), end);
    }

    @Test
    void segmentBehindCameraIsDiscarded() {
        assertFalse(AnchorLineClipper.clip(new Vector3f(0, -10, 5), new Vector3f(0, 10, 5), projection));
    }

    @Test
    void crossingSegmentIsClippedInBothEndpointOrders() {
        Vector3f start = new Vector3f(1, 0, 5);
        Vector3f end = new Vector3f(1, 0, -5);
        assertTrue(AnchorLineClipper.clip(start, end, projection));
        assertTrue(start.z < -0.05f);
        assertEquals(-5, end.z);

        Vector3f reversedStart = new Vector3f(1, 0, -5);
        Vector3f reversedEnd = new Vector3f(1, 0, 5);
        assertTrue(AnchorLineClipper.clip(reversedStart, reversedEnd, projection));
        assertEquals(start.z, reversedEnd.z, 0.00001f);
    }

    @Test
    void segmentInsideNearPlaneAndDegenerateSegmentAreDiscarded() {
        assertFalse(AnchorLineClipper.clip(new Vector3f(0, -10, -0.01f),
                new Vector3f(0, 10, -0.01f), projection));
        assertFalse(AnchorLineClipper.clip(new Vector3f(0, 0, -5), new Vector3f(0, 0, -5), projection));
    }

    @Test
    void shaderHelperPointStaysVisibleWithTiltedProjection() {
        for (float angle : new float[] {-0.3f, 0, 0.3f}) {
            Matrix4f tiltedProjection = new Matrix4f(projection).rotateX(angle);
            Vector3f start = new Vector3f(0, -100, -1);
            Vector3f end = new Vector3f(0, 100, -1);
            assertTrue(AnchorLineClipper.clip(start, end, tiltedProjection));
            Vector3f direction = AnchorLineClipper.shaderDirection(start, end, tiltedProjection);
            for (Vector3f endpoint : new Vector3f[] {start, end}) {
                Vector3f helper = new Vector3f(endpoint).add(direction);
                assertTrue(AnchorLineClipper.clip(new Vector3f(endpoint), helper, tiltedProjection));
                assertEquals(helper, new Vector3f(endpoint).add(direction));
            }
        }
    }
}
