package dev.rsadvanced.client.radial.slice;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SliceLayoutTest {
    @Test
    void detectsCommandsAroundTheWholeCircleIncludingTheAngleSeam() {
        var slots = SliceLayout.positions(8, 100, 100, 80);
        assertEquals(0, SliceLayout.hovered(0, -80, slots, 48, 104));
        assertEquals(2, SliceLayout.hovered(80, 0, slots, 48, 104));
        assertEquals(4, SliceLayout.hovered(0, 80, slots, 48, 104));
        assertEquals(6, SliceLayout.hovered(-80, 0, slots, 48, 104));
        assertEquals(7, SliceLayout.hovered(-56, -56, slots, 48, 104));
        var single = SliceLayout.positions(1, 0, 0, 80);
        assertEquals(0, SliceLayout.hovered(80, 0, single, 48, 104));
    }

    @Test
    void deadzonesPreventAccidentalChangesAtTheCenterAndOutsideTheMenu() {
        var slots = SliceLayout.positions(8, 100, 100, 80);
        assertEquals(-1, SliceLayout.hovered(0, 0, slots, 48, 104));
        assertEquals(-1, SliceLayout.hovered(47, 0, slots, 48, 104));
        assertEquals(-1, SliceLayout.hovered(105, 0, slots, 48, 104));
        assertEquals(2, SliceLayout.hovered(48, 0, slots, 48, 104));
        assertEquals(2, SliceLayout.hovered(104, 0, slots, 48, 104));
        assertThrows(IllegalArgumentException.class, () -> SliceLayout.positions(0, 0, 0, 80));
    }
}
