// SPDX-License-Identifier: CC-BY-NC-4.0
// Adapted from ansi-sudo/Slice, commit 0e81c17eafc67cee8008a199634c8c920594a7e1.
// See licenses/Slice-NOTICE.txt for attribution and changes.
package dev.rsadvanced.client.radial.slice;

import java.util.ArrayList;
import java.util.List;

/** Slice's circular slot layout and nearest-angle selection, without hotbar or global config state. */
public final class SliceLayout {
    public record Slot(int index, double angle, int x, int y) {
    }

    private SliceLayout() {
    }

    public static List<Slot> positions(int count, int centerX, int centerY, double radius) {
        if (count < 1 || radius <= 0) {
            throw new IllegalArgumentException("Slot count and radius must be positive");
        }
        List<Slot> positions = new ArrayList<>(count);
        double angleStep = Math.PI * 2 / count;
        for (int index = 0; index < count; index++) {
            double angle = -Math.PI / 2 + index * angleStep;
            int x = centerX + (int) (Math.cos(angle) * radius);
            int y = centerY + (int) (Math.sin(angle) * radius);
            positions.add(new Slot(index, angle, x, y));
        }
        return List.copyOf(positions);
    }

    public static int hovered(double relativeX, double relativeY, List<Slot> slots,
                              double innerBoundary, double outerBoundary) {
        double distance = Math.hypot(relativeX, relativeY);
        if (slots.isEmpty() || distance < innerBoundary || distance > outerBoundary) {
            return -1;
        }
        double fullTurn = Math.PI * 2;
        double mouseAngle = (Math.atan2(relativeY, relativeX) + fullTurn) % fullTurn;
        int bestSlot = -1;
        double bestAngularDistance = Double.MAX_VALUE;
        for (Slot slot : slots) {
            double slotAngle = (slot.angle() + fullTurn) % fullTurn;
            double angularDistance = Math.abs(mouseAngle - slotAngle);
            if (angularDistance > Math.PI) {
                angularDistance = fullTurn - angularDistance;
            }
            if (angularDistance < bestAngularDistance) {
                bestAngularDistance = angularDistance;
                bestSlot = slot.index();
            }
        }
        return bestSlot;
    }
}
