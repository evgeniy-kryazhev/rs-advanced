package dev.rsadvanced.test;

import dev.rsadvanced.feature.disk.CellDefinitions;
import java.io.IOException;
import java.nio.file.Files;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.storage.LevelResource;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Reload real world-pack files; definitions stay frozen while ordinary recipes are replaced. */
public final class DatapackReloadTest {
    public static void reloadKeepsDefinitionsAndUpdatesRecipes(GameTestHelper helper) throws IOException {
        var server = helper.getLevel().getServer();
        var pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("rsadvanced-example");
        var definitionPath = pack.resolve("data/my_pack/rsadvanced/infinite_cell/lava.json");
        var recipePath = pack.resolve("data/my_pack/recipe/infinite_lava_cell.json");
        String originalDefinition = Files.readString(definitionPath);
        String originalRecipe = Files.readString(recipePath);
        Files.writeString(definitionPath, originalDefinition.replace("minecraft:lava", "minecraft:water"));
        Files.writeString(recipePath, originalRecipe.replace("minecraft:lava_bucket", "minecraft:water_bucket"));
        server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((unused, failure) -> server.execute(() -> {
            try {
                // Restore files immediately, including on a failed assertion/reload.
                Files.writeString(definitionPath, originalDefinition);
                Files.writeString(recipePath, originalRecipe);
                if (failure != null) {
                    throw new IllegalStateException("Datapack reload failed", failure);
                }
                assertEquals(ResourceLocation.parse("minecraft:lava"),
                        CellDefinitions.serverDefinition(ResourceLocation.parse("my_pack:lava")).orElseThrow().resource());
                var recipe = (ShapelessRecipe) server.getRecipeManager()
                        .byKey(ResourceLocation.parse("my_pack:infinite_lava_cell")).orElseThrow().value();
                assertTrue(recipe.getIngredients().get(1).test(new net.minecraft.world.item.ItemStack(Items.WATER_BUCKET)));
                server.reloadResources(server.getPackRepository().getSelectedIds()).whenComplete((restored, restoreFailure) ->
                        server.execute(() -> {
                            if (restoreFailure != null) {
                                helper.fail("Cannot restore the example recipe: " + restoreFailure);
                            } else {
                                helper.succeed();
                            }
                        }));
            } catch (Exception | AssertionError exception) {
                helper.fail("Datapack reload check failed: " + exception);
            }
        }));
    }
}
