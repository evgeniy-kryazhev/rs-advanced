package dev.rsadvanced.feature.anchor;

import dev.architectury.injectables.annotations.ExpectPlatform;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** A ticket owner is an anchor instance, never a globally shared force-load flag. */
public final class AnchorTickets {
    private AnchorTickets() {
    }

    @ExpectPlatform
    public static void add(ServerLevel level, UUID owner, ChunkPos chunk) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void remove(ServerLevel level, UUID owner, ChunkPos chunk) {
        throw new AssertionError();
    }
}
