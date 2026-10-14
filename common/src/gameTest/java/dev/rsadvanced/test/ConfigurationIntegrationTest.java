package dev.rsadvanced.test;

import dev.architectury.platform.Platform;
import dev.rsadvanced.config.RSAdvancedConfig;
import java.io.IOException;
import java.nio.file.Files;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class ConfigurationIntegrationTest {
    private ConfigurationIntegrationTest() {
    }

    @SharedGameTest(value = "toml_configuration", order = 4)
    public static void defaultsAreGeneratedWithoutJsonMigration() throws IOException {
        String filename = Platform.isFabric() ? "rsadvanced.toml" : "rsadvanced-common.toml";
        var directory = Platform.getConfigFolder();
        String toml = Files.readString(directory.resolve(filename));
        assertTrue(toml.contains("[anchor]"));
        assertTrue(toml.matches("(?s).*baseCost\\s*=\\s*80\\b.*"));
        assertTrue(toml.matches("(?s).*visualizationDistance\\s*=\\s*256\\b.*"));
        assertEquals(RSAdvancedConfig.DEFAULT, RSAdvancedConfig.get());
        assertEquals("legacy config must be ignored\n", Files.readString(directory.resolve("rsadvanced.json")));
    }
}
