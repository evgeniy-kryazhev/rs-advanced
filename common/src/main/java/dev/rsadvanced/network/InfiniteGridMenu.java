package dev.rsadvanced.network;

import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

public interface InfiniteGridMenu {
    Set<ResourceKey> rsadvanced$getInfiniteResources();
    void rsadvanced$setInfiniteResources(Set<ResourceKey> resources);
    void rsadvanced$synchronize(ServerPlayer player);
}
