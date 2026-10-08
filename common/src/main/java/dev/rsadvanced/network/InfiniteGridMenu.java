package dev.rsadvanced.network;

import net.minecraft.server.level.ServerPlayer;

public interface InfiniteGridMenu {
    int rsadvanced$getInfiniteFlags();

    void rsadvanced$setInfiniteFlags(int flags);

    void rsadvanced$synchronize(ServerPlayer player);
}
