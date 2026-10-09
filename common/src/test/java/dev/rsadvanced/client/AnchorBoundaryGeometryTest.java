package dev.rsadvanced.client;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorBoundaryGeometryTest {
    @Test
    void singleChunkHasFourWallsAndCorners() {
        assertGeometry(chunks(0, 0), 4, 4, 4);
    }

    @Test
    void adjacentChunksHaveOneRectangleWithoutInternalWallOrSubdivision() {
        AnchorBoundaryGeometry geometry = assertGeometry(chunks(0, 0, 1, 0), 4, 4, 6);
        assertEquals(Set.of(wall(0, 0, 2, 0), wall(0, 1, 2, 1),
                wall(0, 0, 0, 1), wall(2, 0, 2, 1)), Set.copyOf(geometry.walls()));
        assertFalse(geometry.corners().contains(new AnchorBoundaryGeometry.Point(1, 0)));
    }

    @Test
    void squareHasOnlyOuterContour() {
        AnchorBoundaryGeometry geometry = assertGeometry(chunks(0, 0, 1, 0, 0, 1, 1, 1), 4, 4, 8);
        assertFalse(geometry.corners().contains(new AnchorBoundaryGeometry.Point(1, 1)));
    }

    @Test
    void concaveRegionKeepsItsReentrantCorner() {
        AnchorBoundaryGeometry geometry = assertGeometry(chunks(0, 0, 1, 0, 0, 1), 6, 6, 8);
        assertTrue(geometry.corners().contains(new AnchorBoundaryGeometry.Point(1, 1)));
    }

    @Test
    void diagonalContactHasOneSharedVerticalLine() {
        AnchorBoundaryGeometry geometry = assertGeometry(chunks(0, 0, 1, 1), 8, 7, 8);
        assertEquals(1, geometry.corners().stream()
                .filter(point -> point.equals(new AnchorBoundaryGeometry.Point(1, 1))).count());
    }

    @Test
    void disconnectedRegionsKeepBothContours() {
        assertGeometry(chunks(0, 0, 3, 0), 8, 8, 8);
    }

    @Test
    void holeKeepsInnerAndOuterContours() {
        AnchorBoundaryGeometry geometry = assertGeometry(
                chunks(0, 0, 1, 0, 2, 0, 0, 1, 2, 1, 0, 2, 1, 2, 2, 2), 8, 8, 16);
        assertEquals(Set.of(wall(1, 1, 2, 1), wall(1, 2, 2, 2),
                wall(1, 1, 1, 2), wall(2, 1, 2, 2)),
                geometry.walls().stream().filter(wall -> wall.start().x() > 0 && wall.start().z() > 0
                        && wall.end().x() < 3 && wall.end().z() < 3)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void negativeCoordinatesAndEmptySelectionAreSupported() {
        AnchorBoundaryGeometry geometry = assertGeometry(chunks(-2, -1, -1, -1), 4, 4, 6);
        assertEquals(Set.of(wall(-2, -1, 0, -1), wall(-2, 0, 0, 0),
                wall(-2, -1, -2, 0), wall(0, -1, 0, 0)), Set.copyOf(geometry.walls()));
        assertGeometry(Set.of(), 0, 0, 0);
    }

    private static AnchorBoundaryGeometry assertGeometry(Set<Long> chunks, int wallCount,
            int cornerCount, int perimeter) {
        AnchorBoundaryGeometry geometry = AnchorBoundaryGeometry.build(chunks);
        assertEquals(wallCount, geometry.walls().size());
        assertEquals(wallCount, new HashSet<>(geometry.walls()).size());
        assertEquals(cornerCount, geometry.corners().size());
        assertEquals(perimeter, geometry.walls().stream().mapToInt(wall ->
                Math.abs(wall.end().x() - wall.start().x())
                        + Math.abs(wall.end().z() - wall.start().z())).sum());
        return geometry;
    }

    private static Set<Long> chunks(int... coordinates) {
        Set<Long> chunks = new HashSet<>();
        for (int index = 0; index < coordinates.length; index += 2) {
            chunks.add((coordinates[index] & 0xffffffffL) | ((coordinates[index + 1] & 0xffffffffL) << 32));
        }
        return chunks;
    }

    private static AnchorBoundaryGeometry.Wall wall(int startX, int startZ, int endX, int endZ) {
        return new AnchorBoundaryGeometry.Wall(new AnchorBoundaryGeometry.Point(startX, startZ),
                new AnchorBoundaryGeometry.Point(endX, endZ));
    }
}
