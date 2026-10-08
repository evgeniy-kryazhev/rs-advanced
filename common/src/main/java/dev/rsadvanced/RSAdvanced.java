package dev.rsadvanced;

import dev.rsadvanced.feature.AdvancedFeatures;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import java.util.Objects;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Shared bootstrap for RS Advanced features; loader-specific registration stays outside the core. */
public final class RSAdvanced {
    public static final String MOD_ID = "rsadvanced";
    private static BiConsumer<ServerPlayer, InfiniteResourcesPayload> payloadSender;

    private RSAdvanced() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void initialize(BiConsumer<ServerPlayer, InfiniteResourcesPayload> sender) {
        payloadSender = Objects.requireNonNull(sender);
        AdvancedFeatures.initialize();
    }

    public static void sendInfiniteResources(ServerPlayer player, InfiniteResourcesPayload payload) {
        Objects.requireNonNull(payloadSender, "RS Advanced has not been initialized").accept(player, payload);
    }
}
