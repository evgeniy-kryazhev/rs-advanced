package dev.rsadvanced.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import dev.architectury.platform.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Shared configuration for all features, loaded centrally once per server session. */
public record RSAdvancedConfig(
        @SerializedName("baseCost") long anchorBaseCost,
        @SerializedName("chunkCostMultiplier") long anchorChunkCostMultiplier,
        @SerializedName("maxChunks") int anchorMaxChunks,
        @SerializedName("randomTicks") boolean anchorRandomTicks,
        int anchorVisualizationDistance) {
    public static final RSAdvancedConfig DEFAULT = new RSAdvancedConfig(80, 1, 256, true, 256);
    private static volatile RSAdvancedConfig current = DEFAULT;

    public RSAdvancedConfig(long baseCost, long chunkCostMultiplier, int maxChunks, boolean randomTicks) {
        this(baseCost, chunkCostMultiplier, maxChunks, randomTicks, 256);
    }

    public RSAdvancedConfig {
        if (anchorBaseCost < 0 || anchorChunkCostMultiplier < 0 || anchorMaxChunks < 1 || anchorMaxChunks > 65536) {
            throw new IllegalArgumentException("Anchor costs must be nonnegative and maxChunks must be 1..65536");
        }
        if (anchorVisualizationDistance < 1 || anchorVisualizationDistance > 4096) {
            throw new IllegalArgumentException("anchorVisualizationDistance must be an integer in 1..4096");
        }
        long maximumChunkCost = (long) anchorMaxChunks * (anchorMaxChunks + 1) / 2;
        Math.addExact(anchorBaseCost, Math.multiplyExact(anchorChunkCostMultiplier, maximumChunkCost));
    }

    public long anchorCost(int chunks) {
        long chunkCost = (long) chunks * (chunks + 1) / 2;
        return Math.addExact(anchorBaseCost, Math.multiplyExact(anchorChunkCostMultiplier, chunkCost));
    }

    public static RSAdvancedConfig get() {
        return current;
    }

    public static RSAdvancedConfig fromJson(String json) {
        JsonObject settings = JsonParser.parseString(json).getAsJsonObject();
        if (!settings.has("anchorVisualizationDistance")) {
            // Older installations retain their existing settings and receive the new default.
            settings.addProperty("anchorVisualizationDistance", DEFAULT.anchorVisualizationDistance());
        } else {
            var distance = settings.get("anchorVisualizationDistance");
            try {
                if (!distance.isJsonPrimitive() || !distance.getAsJsonPrimitive().isNumber()) {
                    throw new IllegalArgumentException("Expected a number");
                }
                distance.getAsBigDecimal().intValueExact();
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("anchorVisualizationDistance must be an integer in 1..4096", exception);
            }
        }
        return new Gson().fromJson(settings, RSAdvancedConfig.class);
    }

    public static void load() {
        Path path = Platform.getConfigFolder().resolve("rsadvanced.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(DEFAULT) + System.lineSeparator());
                current = DEFAULT;
                return;
            }
            // Keep the existing JSON keys compatible while naming feature settings explicitly in Java.
            RSAdvancedConfig config = fromJson(Files.readString(path));
            if (config == null) {
                throw new IllegalArgumentException("Empty RS Advanced configuration");
            }
            current = config;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load " + path, exception);
        }
    }
}
