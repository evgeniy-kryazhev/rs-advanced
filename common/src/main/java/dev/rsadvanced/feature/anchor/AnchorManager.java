package dev.rsadvanced.feature.anchor;

import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.config.RSAdvancedConfig;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;

/** Server-thread owner reconciliation and payment. New tickets always precede old ticket removal. */
public final class AnchorManager {
    private static AnchorManager current;
    private static final Set<AnchorNetworkComponent> EARLY_COMPONENTS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<AnchorBlockEntity> EARLY_ANCHORS = Collections.newSetFromMap(new IdentityHashMap<>());
    private final MinecraftServer server;
    private final RSAdvancedConfig config;
    private final AnchorSavedData saved;
    private final Set<AnchorNetworkComponent> components = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<AnchorBlockEntity> loadedAnchors = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<UUID, AnchorSavedData.Snapshot> claims = new HashMap<>();
    private final Map<UUID, AnchorSavedData.Snapshot> recovering = new HashMap<>();
    private final Map<ServerLevel, Set<Long>> heldChunks = new IdentityHashMap<>();
    private final Map<ServerLevel, Map<Long, Long>> chunkTicks = new IdentityHashMap<>();
    private final Map<ServerLevel, Map<Long, Integer>> chunkTickCounts = new IdentityHashMap<>();
    private int recoveryTicks;
    private boolean stopping;
    private int lastProcessedTick = Integer.MIN_VALUE;

    private AnchorManager(MinecraftServer server) {
        this.server = server;
        config = RSAdvancedConfig.get();
        saved = AnchorSavedData.get(server);
        components.addAll(EARLY_COMPONENTS);
        loadedAnchors.addAll(EARLY_ANCHORS);
        EARLY_COMPONENTS.clear();
        EARLY_ANCHORS.clear();
        recovering.putAll(saved.snapshots());
        recovering.entrySet().removeIf(entry -> server.getLevel(entry.getValue().dimension()) == null
                || entry.getValue().chunks().size() > config.anchorMaxChunks());
        reconcile(recovering);
    }

    public static void start(MinecraftServer server) {
        current = new AnchorManager(server);
    }

    public static void stop(MinecraftServer server) {
        if (current != null && current.server == server) {
            current.stopping = true;
            // Ticket cleanup must not overwrite the already persisted paid snapshots.
            current.reconcile(Map.of());
            current = null;
            EARLY_COMPONENTS.clear();
            EARLY_ANCHORS.clear();
        }
    }

    public static void track(AnchorNetworkComponent component) {
        if (current != null) {
            current.checkThread();
            current.components.add(component);
        } else {
            EARLY_COMPONENTS.add(component);
        }
    }

    public static void loaded(AnchorBlockEntity anchor) {
        if (current != null) {
            current.checkThread();
            current.loadedAnchors.add(anchor);
        } else {
            EARLY_ANCHORS.add(anchor);
        }
    }

    public static void unloaded(AnchorBlockEntity anchor) {
        EARLY_ANCHORS.remove(anchor);
        if (current != null) {
            current.loadedAnchors.remove(anchor);
        }
    }

    public static void release(AnchorBlockEntity anchor) {
        if (current == null || current.stopping) {
            return;
        }
        current.checkThread();
        UUID owner = anchor.instanceId();
        current.recovering.remove(owner);
        current.saved.remove(owner);
        Map<UUID, AnchorSavedData.Snapshot> retained = new HashMap<>(current.claims);
        AnchorSavedData.Snapshot paid = retained.remove(owner);
        if (paid != null && anchor.node().getNetwork() != null) {
            for (AnchorNetworkComponent.Area area : anchor.node().getNetwork()
                    .getComponent(AnchorNetworkComponent.class).areas()) {
                if (!area.dimension().equals(paid.dimension())) {
                    continue;
                }
                area.anchors().stream().filter(candidate -> candidate != anchor && candidate.enabled()
                                && !candidate.isRemoved()).min(AnchorNetworkComponent.POSITION_ORDER)
                        .ifPresent(reserve -> retained.put(reserve.instanceId(),
                                new AnchorSavedData.Snapshot(paid.dimension(), reserve.getBlockPos(), paid.chunks())));
            }
        }
        current.reconcile(retained);
        current.saved.replace(retained);
    }

    public static void tick(MinecraftServer server) {
        if (current != null && current.server == server) {
            current.tick();
        }
    }

    private void tick() {
        checkThread();
        if (lastProcessedTick == server.getTickCount()) {
            return;
        }
        lastProcessedTick = server.getTickCount();
        recoveryTicks++;
        Map<UUID, AnchorSavedData.Snapshot> desired = new HashMap<>();
        for (AnchorBlockEntity anchor : loadedAnchors) {
            if (!anchor.enabled() || anchor.node().getNetwork() == null) {
                anchor.update(anchor.enabled() ? AnchorStatus.NO_NETWORK : AnchorStatus.DISABLED, 0, 0, 0, false);
            }
        }
        components.removeIf(AnchorNetworkComponent::removed);
        for (AnchorNetworkComponent component : components) {
            for (AnchorNetworkComponent.Area area : component.areas()) {
                process(area, desired);
            }
        }
        recovering.entrySet().removeIf(entry -> {
            AnchorSavedData.Snapshot snapshot = entry.getValue();
            ServerLevel level = server.getLevel(snapshot.dimension());
            if (level == null || recoveryTicks > 100) {
                return true;
            }
            if (level.getChunkSource().getChunkNow(snapshot.position().getX() >> 4,
                    snapshot.position().getZ() >> 4) == null) {
                return false;
            }
            if (!(level.getBlockEntity(snapshot.position()) instanceof AnchorBlockEntity anchor)
                    || !anchor.instanceId().equals(entry.getKey()) || !anchor.enabled()) {
                return true;
            }
            if (anchor.status() != AnchorStatus.ACTIVE && anchor.status() != AnchorStatus.RESERVE) {
                anchor.update(AnchorStatus.RECOVERING, snapshot.chunks().size(), snapshot.chunks().size(),
                        config.anchorCost(snapshot.chunks().size()), true);
            }
            return false;
        });
        recovering.forEach((owner, snapshot) -> desired.merge(owner, snapshot, (paid, restored) -> {
            Set<Long> combined = new HashSet<>(paid.chunks());
            combined.addAll(restored.chunks());
            return new AnchorSavedData.Snapshot(paid.dimension(), paid.position(), Set.copyOf(combined));
        }));
        reconcile(desired);
        saved.replace(desired);
    }

    private void process(AnchorNetworkComponent.Area area, Map<UUID, AnchorSavedData.Snapshot> desired) {
        ServerLevel level = server.getLevel(area.dimension());
        if (level == null) {
            return;
        }
        int size = area.chunks().size();
        long cost;
        try {
            cost = config.anchorCost(size);
        } catch (ArithmeticException exception) {
            // Over-limit graphs can exceed even the configured cost's validated range.
            // They never receive tickets; keep their GUI estimate representable.
            cost = Long.MAX_VALUE;
        }
        AnchorStatus status;
        if (size > config.anchorMaxChunks()) {
            status = AnchorStatus.LIMIT_EXCEEDED;
        } else {
            EnergyNetworkComponent energy = area.network().getComponent(EnergyNetworkComponent.class);
            if (RefinedStorageApi.INSTANCE.isEnergyRequired() && energy.getStored() < cost) {
                status = AnchorStatus.NO_ENERGY;
            } else {
                if (RefinedStorageApi.INSTANCE.isEnergyRequired()) {
                    energy.extract(cost);
                }
                status = AnchorStatus.ACTIVE;
                desired.put(area.leader().instanceId(), new AnchorSavedData.Snapshot(area.dimension(),
                        area.leader().getBlockPos(), area.chunks()));
            }
        }
        for (AnchorBlockEntity anchor : area.anchors()) {
            boolean leader = anchor == area.leader();
            AnchorStatus anchorStatus = status == AnchorStatus.ACTIVE && !leader ? AnchorStatus.RESERVE : status;
            anchor.update(anchorStatus, size, status == AnchorStatus.ACTIVE ? size : 0, cost, leader);
            boolean confirmedPowerLoss = status == AnchorStatus.NO_ENERGY
                    && area.network().getComponent(AnchorNetworkComponent.class).hasPowerProvider()
                    && !area.network().getComponent(AnchorNetworkComponent.class).hasInitializingPower();
            if (status == AnchorStatus.LIMIT_EXCEEDED || confirmedPowerLoss) {
                recovering.remove(anchor.instanceId());
            }
        }
    }

    private void reconcile(Map<UUID, AnchorSavedData.Snapshot> desired) {
        if (claims.equals(desired)) {
            return;
        }
        desired.forEach((owner, snapshot) -> {
            ServerLevel level = server.getLevel(snapshot.dimension());
            if (level == null) {
                return;
            }
            AnchorSavedData.Snapshot previous = claims.get(owner);
            for (long chunk : snapshot.chunks()) {
                if (previous == null || !previous.dimension().equals(snapshot.dimension())
                        || !previous.chunks().contains(chunk)) {
                    AnchorTickets.add(level, owner, new ChunkPos(chunk));
                }
            }
        });
        claims.forEach((owner, previous) -> {
            ServerLevel level = server.getLevel(previous.dimension());
            if (level == null) {
                return;
            }
            AnchorSavedData.Snapshot next = desired.get(owner);
            for (long chunk : previous.chunks()) {
                if (next == null || !next.dimension().equals(previous.dimension()) || !next.chunks().contains(chunk)) {
                    AnchorTickets.remove(level, owner, new ChunkPos(chunk));
                }
            }
        });
        claims.clear();
        claims.putAll(desired);
        heldChunks.clear();
        claims.values().forEach(snapshot -> {
            ServerLevel level = server.getLevel(snapshot.dimension());
            if (level != null) {
                heldChunks.computeIfAbsent(level, ignored -> new HashSet<>()).addAll(snapshot.chunks());
            }
        });
        chunkTicks.forEach((level, times) -> times.keySet().retainAll(heldChunks.getOrDefault(level, Set.of())));
        chunkTickCounts.forEach((level, counts) -> counts.keySet().retainAll(heldChunks.getOrDefault(level, Set.of())));
    }

    public static Set<Long> chunks(AnchorBlockEntity anchor) {
        if (current == null || anchor.node().getNetwork() == null) {
            return Set.of();
        }
        for (AnchorNetworkComponent.Area area : anchor.node().getNetwork()
                .getComponent(AnchorNetworkComponent.class).areas()) {
            if (area.dimension().equals(anchor.getLevel().dimension()) && area.anchors().contains(anchor)
                    && (anchor.status() == AnchorStatus.ACTIVE || anchor.status() == AnchorStatus.RESERVE)) {
                AnchorSavedData.Snapshot claim = current.claims.get(area.leader().instanceId());
                return claim == null ? Set.of() : claim.chunks();
            }
        }
        return Set.of();
    }

    public static void beforeLevelTick(ServerLevel level) {
        if (current != null && !current.heldChunks.getOrDefault(level, Set.of()).isEmpty()) {
            level.resetEmptyTime();
        }
    }

    public static void recordChunkTick(ServerLevel level, long chunk) {
        if (current != null && current.heldChunks.getOrDefault(level, Set.of()).contains(chunk)) {
            Map<Long, Long> times = current.chunkTicks.computeIfAbsent(level, ignored -> new HashMap<>());
            Map<Long, Integer> counts = current.chunkTickCounts.computeIfAbsent(level, ignored -> new HashMap<>());
            if (times.getOrDefault(chunk, Long.MIN_VALUE) != level.getGameTime()) {
                counts.put(chunk, 0);
            }
            times.put(chunk, level.getGameTime());
            counts.merge(chunk, 1, Integer::sum);
        }
    }

    public static int chunkTickCount(ServerLevel level, long chunk) {
        if (current == null || current.chunkTicks.getOrDefault(level, Map.of())
                .getOrDefault(chunk, Long.MIN_VALUE) != level.getGameTime()) {
            return 0;
        }
        return current.chunkTickCounts.getOrDefault(level, Map.of()).getOrDefault(chunk, 0);
    }

    public static void afterLevelTick(ServerLevel level) {
        if (current == null || !current.config.anchorRandomTicks() || !level.tickRateManager().runsNormally()) {
            return;
        }
        Set<Long> held = current.heldChunks.getOrDefault(level, Set.of());
        Map<Long, Long> ticked = current.chunkTicks.computeIfAbsent(level, ignored -> new HashMap<>());
        int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        for (long position : held) {
            if (ticked.getOrDefault(position, Long.MIN_VALUE) == level.getGameTime()) {
                continue;
            }
            ChunkPos chunkPosition = new ChunkPos(position);
            var chunk = level.getChunkSource().getChunkNow(chunkPosition.x, chunkPosition.z);
            if (chunk != null) {
                level.tickChunk(chunk, speed);
            }
        }
        ticked.keySet().retainAll(held);
        current.chunkTickCounts.computeIfAbsent(level, ignored -> new HashMap<>()).keySet().retainAll(held);
    }

    private void checkThread() {
        if (!server.isSameThread()) {
            throw new IllegalStateException("Anchor topology and tickets must run on the server thread");
        }
    }
}
