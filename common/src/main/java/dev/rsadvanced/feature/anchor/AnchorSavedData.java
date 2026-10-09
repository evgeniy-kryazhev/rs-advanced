package dev.rsadvanced.feature.anchor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Only the last paid areas are restorable. Normal shutdown does not deactivate them. */
public final class AnchorSavedData extends SavedData {
    private final Map<UUID, Snapshot> snapshots = new HashMap<>();

    public static AnchorSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(AnchorSavedData::new, AnchorSavedData::load, null), "rsadvanced_anchors");
    }

    public Map<UUID, Snapshot> snapshots() {
        return Map.copyOf(snapshots);
    }

    public void replace(Map<UUID, Snapshot> active) {
        if (!snapshots.equals(active)) {
            snapshots.clear();
            snapshots.putAll(active);
            setDirty();
        }
    }

    public void remove(UUID owner) {
        if (snapshots.remove(owner) != null) {
            setDirty();
        }
    }

    private static AnchorSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AnchorSavedData data = new AnchorSavedData();
        ListTag list = tag.getList("anchors", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("dimension"));
            if (!entry.hasUUID("owner") || dimension == null) {
                continue;
            }
            Set<Long> chunks = new HashSet<>();
            for (long chunk : entry.getLongArray("chunks")) {
                chunks.add(chunk);
            }
            data.snapshots.put(entry.getUUID("owner"), new Snapshot(
                    ResourceKey.create(Registries.DIMENSION, dimension),
                    BlockPos.of(entry.getLong("position")), Set.copyOf(chunks)));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        snapshots.forEach((owner, snapshot) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("owner", owner);
            entry.putString("dimension", snapshot.dimension().location().toString());
            entry.putLong("position", snapshot.position().asLong());
            entry.putLongArray("chunks", snapshot.chunks().stream().mapToLong(Long::longValue).sorted().toArray());
            list.add(entry);
        });
        tag.put("anchors", list);
        return tag;
    }

    public record Snapshot(ResourceKey<Level> dimension, BlockPos position, Set<Long> chunks) {
    }
}
