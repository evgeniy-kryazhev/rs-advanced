package dev.rsadvanced.test.neoforge;

import dev.rsadvanced.test.client.ConfigurationClientCheck;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;

import static dev.rsadvanced.test.TestAssertions.assertEquals;

public final class NeoForgeConfigurationClientCheck implements ConfigurationClientCheck.Driver {
    @Override
    public Screen open() {
        var container = ModList.get().getModContainerById("rsadvanced").orElseThrow();
        return IConfigScreenFactory.getForMod(container.getModInfo()).orElseThrow().createScreen(container, null);
    }

    @Override
    public boolean showSettings(Minecraft minecraft) {
        if (findCostField(minecraft) != null) {
            return true;
        }
        String anchorButton = Component.translatable("rsadvanced.configuration.anchor.button").getString();
        for (var child : ConfigurationClientCheck.descendants(minecraft.screen)) {
            if (child instanceof Button button && (button.getMessage().getString().contains(anchorButton)
                    || button.getMessage().getString().contains("rsadvanced-common.toml"))) {
                button.onPress();
                break;
            }
        }
        return false;
    }

    private EditBox findCostField(Minecraft minecraft) {
        String name = Component.translatable("rsadvanced.configuration.anchor.baseCost").getString();
        return ConfigurationClientCheck.descendants(minecraft.screen).stream()
                .filter(child -> child instanceof EditBox field && field.getMessage().getString().equals(name))
                .map(child -> (EditBox) child)
                .findFirst().orElse(null);
    }

    @Override
    public void editAndSave(Minecraft minecraft) {
        var field = findCostField(minecraft);
        field.setValue("123");
        // Close the anchor section and then the file section, where the native UI saves.
        for (int depth = 0; depth < 3
                && minecraft.screen instanceof ConfigurationScreen.ConfigurationSectionScreen; depth++) {
            minecraft.screen.onClose();
        }
    }

    @Override
    public Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("rsadvanced-common.toml");
    }

    @Override
    public void verifySaved() {
        var config = ModConfigs.getConfigSet(ModConfig.Type.COMMON).stream()
                .filter(entry -> entry.getModId().equals("rsadvanced"))
                .findFirst().orElseThrow();
        var spec = (ModConfigSpec) config.getSpec();
        Number value = spec.getValues().<ModConfigSpec.LongValue>get("anchor.baseCost").getRaw();
        assertEquals(123, value.longValue());
    }
}
