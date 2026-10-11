package dev.rsadvanced.config;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RSAdvancedConfigTest {
    @Test
    void visualizationDistanceSupportsDefaultsAndExplicitBounds() {
        assertEquals(256, RSAdvancedConfig.DEFAULT.anchorVisualizationDistance());
        for (int distance : new int[] {1, 96, 4096}) {
            var config = new RSAdvancedConfig(80, 1, 256, true, distance);
            assertEquals(distance, config.anchorVisualizationDistance());
        }
    }

    @Test
    void rejectsExplicitInvalidVisualizationDistance() {
        for (int distance : new int[] {0, -1, 4097, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> new RSAdvancedConfig(80, 1, 256, true, distance));
        }
    }

    @Test
    void appliesEditsOnlyWhenAnotherServerSessionLoads() {
        var original = RSAdvancedConfig.get();
        var edited = new RSAdvancedConfig(10, 2, 32, false, 96);
        var platformConfig = new AtomicReference<>(RSAdvancedConfig.DEFAULT);
        try {
            RSAdvancedConfig.load(platformConfig::get);
            platformConfig.set(edited);
            assertEquals(RSAdvancedConfig.DEFAULT, RSAdvancedConfig.get());

            RSAdvancedConfig.load(platformConfig::get);
            assertEquals(edited, RSAdvancedConfig.get());

            assertThrows(ArithmeticException.class, () -> RSAdvancedConfig.load(() ->
                    new RSAdvancedConfig(80, Long.MAX_VALUE, 256, true)));
            assertEquals(edited, RSAdvancedConfig.get());
            assertThrows(NullPointerException.class, () -> RSAdvancedConfig.load(() -> null));
            assertEquals(edited, RSAdvancedConfig.get());
        } finally {
            RSAdvancedConfig.load(() -> original);
        }
    }

    @Test
    void quadraticCostAndConfiguredMultiplier() {
        assertEquals(80, RSAdvancedConfig.DEFAULT.anchorCost(0));
        assertEquals(81, RSAdvancedConfig.DEFAULT.anchorCost(1));
        assertEquals(83, RSAdvancedConfig.DEFAULT.anchorCost(2));
        assertEquals(32976, RSAdvancedConfig.DEFAULT.anchorCost(256));
        assertEquals(16, new RSAdvancedConfig(10, 2, 10, false).anchorCost(2));
    }

    @Test
    void rejectsInvalidAndOverflowingSettings() {
        assertThrows(IllegalArgumentException.class, () -> new RSAdvancedConfig(-1, 1, 256, true));
        assertThrows(IllegalArgumentException.class, () -> new RSAdvancedConfig(80, -1, 256, true));
        assertThrows(IllegalArgumentException.class, () -> new RSAdvancedConfig(80, 1, 0, true));
        assertThrows(IllegalArgumentException.class, () -> new RSAdvancedConfig(80, 1, 65537, true));
        assertThrows(ArithmeticException.class, () -> new RSAdvancedConfig(80, Long.MAX_VALUE, 256, true));
        assertThrows(ArithmeticException.class, () -> new RSAdvancedConfig(Long.MAX_VALUE, 1, 1, true));
    }
}
