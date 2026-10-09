package dev.rsadvanced.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Integer chunk-grid geometry, independent of the world and rendering APIs. */
public record AnchorBoundaryGeometry(List<Wall> walls, Set<Point> corners) {
    public AnchorBoundaryGeometry {
        walls = List.copyOf(walls);
        corners = Set.copyOf(corners);
    }

    public static AnchorBoundaryGeometry build(Set<Long> chunks) {
        Map<WallPlane, List<Integer>> exposedSegments = new HashMap<>();
        for (long packed : chunks) {
            int chunkX = (int) packed;
            int chunkZ = (int) (packed >> 32);
            addExposedSegment(chunks, exposedSegments, chunkX, chunkZ - 1,
                    new WallPlane(true, chunkZ, -1), chunkX);
            addExposedSegment(chunks, exposedSegments, chunkX, chunkZ + 1,
                    new WallPlane(true, chunkZ + 1, 1), chunkX);
            addExposedSegment(chunks, exposedSegments, chunkX - 1, chunkZ,
                    new WallPlane(false, chunkX, -1), chunkZ);
            addExposedSegment(chunks, exposedSegments, chunkX + 1, chunkZ,
                    new WallPlane(false, chunkX + 1, 1), chunkZ);
        }

        List<Wall> walls = new ArrayList<>();
        for (var entry : exposedSegments.entrySet()) {
            List<Integer> segments = entry.getValue();
            segments.sort(Integer::compareTo);
            int start = segments.getFirst();
            int end = start + 1;
            for (int index = 1; index < segments.size(); index++) {
                int nextStart = segments.get(index);
                if (nextStart == end) {
                    end++;
                } else {
                    walls.add(entry.getKey().wall(start, end));
                    start = nextStart;
                    end = start + 1;
                }
            }
            walls.add(entry.getKey().wall(start, end));
        }
        walls.sort(Comparator.comparingInt((Wall wall) -> wall.start().x())
                .thenComparingInt(wall -> wall.start().z())
                .thenComparingInt(wall -> wall.end().x())
                .thenComparingInt(wall -> wall.end().z()));

        Set<Point> corners = new HashSet<>();
        for (Wall wall : walls) {
            // A diagonal contact can be an endpoint of four walls but has only one vertical line.
            corners.add(wall.start());
            corners.add(wall.end());
        }
        return new AnchorBoundaryGeometry(walls, corners);
    }

    private static void addExposedSegment(Set<Long> chunks, Map<WallPlane, List<Integer>> segments,
            int neighborX, int neighborZ, WallPlane plane, int start) {
        long neighbor = (neighborX & 0xffffffffL) | ((neighborZ & 0xffffffffL) << 32);
        if (!chunks.contains(neighbor)) {
            segments.computeIfAbsent(plane, ignored -> new ArrayList<>()).add(start);
        }
    }

    public record Point(int x, int z) {
    }

    public record Wall(Point start, Point end) {
    }

    // Opposite-facing segments at a diagonal contact must not merge through the contact point.
    private record WallPlane(boolean alongX, int coordinate, int outwardDirection) {
        Wall wall(int start, int end) {
            if (alongX) {
                return new Wall(new Point(start, coordinate), new Point(end, coordinate));
            }
            return new Wall(new Point(coordinate, start), new Point(coordinate, end));
        }
    }
}
