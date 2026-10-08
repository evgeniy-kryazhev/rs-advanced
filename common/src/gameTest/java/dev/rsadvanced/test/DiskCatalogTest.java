package dev.rsadvanced.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.refinedmods.refinedstorage.common.Platform;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.DiskResourceKind;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Missing generated files fail check/build with the affected catalog ID. */
public final class DiskCatalogTest {
    public static void catalogResourcesAndQuotasAreComplete() throws IOException {
        JsonObject english = json("assets/rsadvanced/lang/en_us.json");
        JsonObject russian = json("assets/rsadvanced/lang/ru_ru.json");
        JsonObject advancement = json("data/rsadvanced/advancement/recipes/infinite_disks.json");
        Set<String> unlockedRecipes = new HashSet<>();
        advancement.getAsJsonObject("rewards").getAsJsonArray("recipes")
                .forEach(recipe -> unlockedRecipes.add(recipe.getAsString()));
        Set<String> ids = new HashSet<>();
        int flags = 0;
        for (InfiniteDiskType type : InfiniteDiskType.values()) {
            String id = type.itemName();
            if (!ids.add(id) || (flags & type.flag()) != 0) {
                throw new AssertionError("Duplicate infinite disk ID or flag: " + id);
            }
            flags |= type.flag();
            assertEquals(type, AdvancedContent.disk(type).get().diskType());
            require(english.has(type.translationKey()), id, "English translation");
            require(russian.has(type.translationKey()), id, "Russian translation");
            assertEquals(type.englishName(), english.get(type.translationKey()).getAsString());
            assertEquals(type.russianName(), russian.get(type.translationKey()).getAsString());
            require(unlockedRecipes.contains("rsadvanced:" + id), id, "recipe unlock");
            JsonObject recipe = json("data/rsadvanced/recipe/" + id + ".json");
            assertEquals("rsadvanced:" + id, recipe.getAsJsonObject("result").get("id").getAsString());
            assertEquals(type.recipeIngredients(), recipe.getAsJsonArray("ingredients").asList().stream()
                    .map(ingredient -> ingredient.getAsJsonObject().get("item").getAsString()).toList());
            JsonObject model = json("assets/rsadvanced/models/item/" + id + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString());
            assertEquals("rsadvanced:item/" + id, model.getAsJsonObject("textures").get("layer0").getAsString());
            try (InputStream texture = resource("assets/rsadvanced/textures/item/" + id + ".png")) {
                byte[] signature = texture.readNBytes(8);
                assertTrue(java.util.Arrays.equals(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}, signature));
            }
            boolean fluid = type.description().kind() == DiskResourceKind.FLUID;
            long unit = fluid ? Platform.INSTANCE.getBucketAmount() : 1;
            assertEquals(unit, type.getDiskInterfaceTransferQuota(false));
            assertEquals(unit * (fluid ? 16 : 64), type.getDiskInterfaceTransferQuota(true));
            assertEquals("refinedstorage:block/disk/" + (fluid ? "fluid_disk" : "disk"),
                    type.description().kind().diskModel().toString());
        }
        assertEquals(1, InfiniteDiskType.COBBLESTONE.flag());
        assertEquals(2, InfiniteDiskType.WATER.flag());
    }

    private static void require(boolean present, String id, String detail) {
        if (!present) {
            throw new AssertionError("Missing " + detail + " for rsadvanced:" + id + "; run :fabric:runDatagen");
        }
    }

    private static InputStream resource(String path) {
        InputStream stream = DiskCatalogTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new AssertionError("Missing catalog resource: " + path + "; run :fabric:runDatagen or add the PNG texture");
        }
        return stream;
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(resource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
