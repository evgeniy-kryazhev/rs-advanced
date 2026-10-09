package dev.rsadvanced.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RSAdvancedConfigTest {
    @Test
    void visualizationDistanceSupportsDefaultsAndExplicitBounds() {
        assertEquals(256, RSAdvancedConfig.DEFAULT.anchorVisualizationDistance());
        for (int distance : new int[] {1, 96, 4096}) {
            String json = """
                    {"baseCost":80,"chunkCostMultiplier":1,"maxChunks":256,"randomTicks":true,
                     "anchorVisualizationDistance":%d}
                    """.formatted(distance);
            assertEquals(distance, RSAdvancedConfig.fromJson(json).anchorVisualizationDistance());
        }
    }

    @Test
    void rejectsExplicitInvalidVisualizationDistance() {
        for (String distance : new String[] {"0", "-1", "4097", "1.5", "2147483648", "null", "true", "\"256\""}) {
            String json = """
                    {"baseCost":80,"chunkCostMultiplier":1,"maxChunks":256,"randomTicks":true,
                     "anchorVisualizationDistance":%s}
                    """.formatted(distance);
            assertThrows(RuntimeException.class, () -> RSAdvancedConfig.fromJson(json));
        }
    }
    @Test
    void readsExistingConfigurationKeys() {
        String json = """
                {
                  "baseCost": 10,
                  "chunkCostMultiplier": 2,
                  "maxChunks": 32,
                  "randomTicks": false
                }
                """;
        RSAdvancedConfig config = RSAdvancedConfig.fromJson(json);

        assertEquals(new RSAdvancedConfig(10, 2, 32, false), config);
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
        assertThrows(IllegalArgumentException.class, () -> new RSAdvancedConfig(80, 1, 0, true));
        assertThrows(ArithmeticException.class, () -> new RSAdvancedConfig(80, Long.MAX_VALUE, 256, true));
    }
}
