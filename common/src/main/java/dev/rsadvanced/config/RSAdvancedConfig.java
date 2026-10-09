package dev.rsadvanced.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
        @SerializedName("randomTicks") boolean anchorRandomTicks) {
    public static final RSAdvancedConfig DEFAULT = new RSAdvancedConfig(80, 1, 256, true);
    private static volatile RSAdvancedConfig current = DEFAULT;

    public RSAdvancedConfig {
        if (anchorBaseCost < 0 || anchorChunkCostMultiplier < 0 || anchorMaxChunks < 1 || anchorMaxChunks > 65536) {
            throw new IllegalArgumentException("Anchor costs must be nonnegative and maxChunks must be 1..65536");
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
            RSAdvancedConfig config = gson.fromJson(Files.readString(path), RSAdvancedConfig.class);
            if (config == null) {
                throw new IllegalArgumentException("Empty RS Advanced configuration");
            }
            current = config;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Cannot load " + path, exception);
        }
    }
}
