package dev.rsadvanced.test.fabric;

import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.rsadvanced.fabric.config.FabricConfig;
import dev.rsadvanced.fabric.config.RSAdvancedModMenu;
import dev.rsadvanced.test.client.ConfigurationClientCheck;
import java.nio.file.Path;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.entries.LongListEntry;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.language.ClientLanguage;
import java.util.List;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class FabricConfigurationClientCheck implements ConfigurationClientCheck.Driver {
    @Override
    public Screen open() {
        for (String language : List.of("en_us", "ru_ru")) {
            var translations = ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(),
                    List.of("en_us", language), false);
            for (String key : List.of("anchor", "anchor.baseCost", "anchor.chunkCostMultiplier",
                    "anchor.maxChunks", "anchor.randomTicks", "anchor.visualizationDistance")) {
                assertTrue(translations.has("text.autoconfig.rsadvanced.option." + key + ".@Tooltip"));
            }
        }
        var entrypoint = FabricLoader.getInstance().getEntrypoints("modmenu", ModMenuApi.class).stream()
                .filter(api -> api instanceof RSAdvancedModMenu)
                .findFirst().orElseThrow();
        return entrypoint.getModConfigScreenFactory().create(null);
    }

    @Override
    public boolean showSettings(Minecraft minecraft) {
        return minecraft.screen instanceof ClothConfigScreen;
    }

    @Override
    public void editAndSave(Minecraft minecraft) {
        var screen = (ClothConfigScreen) minecraft.screen;
        var anchor = screen.getCategorizedEntries().values().stream().flatMap(java.util.Collection::stream)
                .filter(entry -> entry instanceof SubCategoryListEntry)
                .map(entry -> (SubCategoryListEntry) entry)
                .findFirst().orElseThrow();
        assertEquals(5, anchor.getValue().size());
        String name = Component.translatable("text.autoconfig.rsadvanced.option.anchor.baseCost").getString();
        var cost = anchor.getValue().stream()
                .filter(entry -> entry.getFieldName().getString().equals(name))
                .map(entry -> (LongListEntry) entry)
                .findFirst().orElseThrow();
        assertTrue(cost.isRequiresRestart());
        cost.setValue("-1");
        assertTrue(cost.getError().isPresent());
        cost.setValue("123");
        assertTrue(cost.getError().isEmpty());
        screen.saveAll(false);
    }

    @Override
    public Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("rsadvanced.toml");
    }

    @Override
    public void verifySaved() {
        assertEquals(123, FabricConfig.load().anchorBaseCost());
    }
}
