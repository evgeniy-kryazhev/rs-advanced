package dev.rsadvanced.test.client;

import dev.rsadvanced.config.RSAdvancedConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import java.util.ArrayList;
import java.util.List;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Runs after the connected-server menu checks, using each loader's registered config screen. */
public final class ConfigurationClientCheck {
    private static Driver driver;
    private static int ticks;
    private static RSAdvancedConfig sessionConfig;

    private ConfigurationClientCheck() {
    }

    public static void register(Driver platformDriver) {
        driver = platformDriver;
    }

    public static void start(Minecraft minecraft) {
        assertTrue(driver != null);
        sessionConfig = RSAdvancedConfig.get();
        ticks = 0;
        minecraft.setScreen(driver.open());
    }

    public static boolean tick(Minecraft minecraft) throws Exception {
        ticks++;
        if (ticks > 200) {
            capture(minecraft);
            var widgets = descendants(minecraft.screen).stream()
                    .filter(child -> child instanceof AbstractWidget)
                    .map(child -> ((AbstractWidget) child).getMessage().getString())
                    .toList();
            throw new AssertionError("Configuration screen did not open: " + minecraft.screen + "; widgets=" + widgets);
        }
        if (ticks < 20 || !driver.showSettings(minecraft)) {
            return false;
        }
        capture(minecraft);
        driver.editAndSave(minecraft);
        assertEquals(sessionConfig, RSAdvancedConfig.get());
        String saved = Files.readString(driver.path());
        assertTrue(saved.contains("[anchor]"));
        assertTrue(saved.matches("(?s).*baseCost\\s*=\\s*123\\b.*"));
        driver.verifySaved();
        return true;
    }

    private static void capture(Minecraft minecraft) throws java.io.IOException {
        try (var pixels = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            pixels.writeToFile(Path.of(System.getProperty("rsadvanced.test.clientReport"))
                    .resolveSibling("configuration-screen.png"));
        }
    }

    public static List<GuiEventListener> descendants(ContainerEventHandler parent) {
        var result = new ArrayList<GuiEventListener>();
        for (var child : parent.children()) {
            result.add(child);
            if (child instanceof ContainerEventHandler container) {
                result.addAll(descendants(container));
            }
        }
        return result;
    }

    public interface Driver {
        Screen open();

        boolean showSettings(Minecraft minecraft);

        void editAndSave(Minecraft minecraft);

        Path path();

        void verifySaved();
    }
}
