package dev.rsadvanced.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/** Generates vanilla JSON shared by Fabric and NeoForge; no loader-specific providers. */
public final class DiskDataProvider implements DataProvider {
    private final Path output;

    public DiskDataProvider(PackOutput output) {
        this.output = output.getOutputFolder();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        List<CompletableFuture<?>> writes = new ArrayList<>();
        Map<String, String> english = new TreeMap<>(InterfaceTranslations.ENGLISH);
        Map<String, String> russian = new TreeMap<>(InterfaceTranslations.RUSSIAN);
        JsonArray unlockedRecipes = new JsonArray();
        for (InfiniteDiskType type : InfiniteDiskType.values()) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", "rsadvanced:item/" + type.itemName());
            model.add("textures", textures);
            writes.add(save(cache, "assets/rsadvanced/models/item/" + type.itemName() + ".json", model));

            JsonObject recipe = new JsonObject();
            recipe.addProperty("type", "minecraft:crafting_shapeless");
            recipe.addProperty("category", "misc");
            JsonArray ingredients = new JsonArray();
            for (String ingredientId : type.recipeIngredients()) {
                JsonObject ingredient = new JsonObject();
                ingredient.addProperty("item", ingredientId);
                ingredients.add(ingredient);
            }
            recipe.add("ingredients", ingredients);
            JsonObject result = new JsonObject();
            result.addProperty("id", "rsadvanced:" + type.itemName());
            result.addProperty("count", 1);
            recipe.add("result", result);
            writes.add(save(cache, "data/rsadvanced/recipe/" + type.itemName() + ".json", recipe));

            english.put(type.translationKey(), type.englishName());
            russian.put(type.translationKey(), type.russianName());
            unlockedRecipes.add("rsadvanced:" + type.itemName());
        }

        JsonObject advancement = new JsonObject();
        advancement.addProperty("parent", "minecraft:recipes/root");
        JsonObject criterion = new JsonObject();
        criterion.addProperty("trigger", "minecraft:inventory_changed");
        JsonObject itemPredicate = new JsonObject();
        itemPredicate.addProperty("items", "refinedstorage:storage_housing");
        JsonArray items = new JsonArray();
        items.add(itemPredicate);
        JsonObject conditions = new JsonObject();
        conditions.add("items", items);
        criterion.add("conditions", conditions);
        JsonObject criteria = new JsonObject();
        criteria.add("has_storage_housing", criterion);
        advancement.add("criteria", criteria);
        JsonArray requirement = new JsonArray();
        requirement.add("has_storage_housing");
        JsonArray requirements = new JsonArray();
        requirements.add(requirement);
        advancement.add("requirements", requirements);
        JsonObject rewards = new JsonObject();
        rewards.add("recipes", unlockedRecipes);
        advancement.add("rewards", rewards);
        writes.add(save(cache, "data/rsadvanced/advancement/recipes/infinite_disks.json", advancement));
        writes.add(save(cache, "assets/rsadvanced/lang/en_us.json", translations(english)));
        writes.add(save(cache, "assets/rsadvanced/lang/ru_ru.json", translations(russian)));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<?> save(CachedOutput cache, String path, JsonObject json) {
        return DataProvider.saveStable(cache, json, output.resolve(path));
    }

    private static JsonObject translations(Map<String, String> entries) {
        JsonObject translations = new JsonObject();
        entries.forEach(translations::addProperty);
        return translations;
    }

    @Override
    public String getName() {
        return "RS Advanced disk catalog";
    }
}
