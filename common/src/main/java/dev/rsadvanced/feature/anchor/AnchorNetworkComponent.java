package dev.rsadvanced.feature.anchor;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.NetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.container.NetworkNodeContainer;
import com.refinedmods.refinedstorage.api.network.impl.node.controller.ControllerNetworkNode;
import com.refinedmods.refinedstorage.api.network.energy.EnergyProvider;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Incremental container counts; topology events never scan blocks in the world. */
public final class AnchorNetworkComponent implements NetworkComponent {
    public static final Comparator<AnchorBlockEntity> POSITION_ORDER = Comparator
            .comparingInt((AnchorBlockEntity anchor) -> anchor.getBlockPos().getX())
            .thenComparingInt(anchor -> anchor.getBlockPos().getY())
            .thenComparingInt(anchor -> anchor.getBlockPos().getZ());
    private final Network network;
    private final Map<NetworkNodeContainer, GlobalPos> positions = new IdentityHashMap<>();
    private final Map<ResourceKey<Level>, Map<Long, Integer>> counts = new HashMap<>();
    private final Set<AnchorBlockEntity> anchors = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<ControllerNetworkNode> controllers = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<EnergyProvider> powerProviders = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private List<Area> areas = List.of();
    private boolean dirty = true;
    private boolean removed;

    public AnchorNetworkComponent(Network network) {
        this.network = network;
    }

    @Override
    public void onContainerAdded(NetworkNodeContainer container) {
        if (!(container instanceof InWorldNetworkNodeContainer inWorld) || positions.containsKey(container)) {
            return;
        }
        GlobalPos position = inWorld.getPosition();
        positions.put(container, position);
        if (container.getNode() instanceof ControllerNetworkNode controller) {
            controllers.add(controller);
        }
        if (container.getNode() instanceof EnergyProvider provider) {
            powerProviders.add(provider);
        }
        counts.computeIfAbsent(position.dimension(), ignored -> new HashMap<>())
                .merge(new ChunkPos(position.pos()).toLong(), 1, Integer::sum);
        if (container.getNode() instanceof AnchorNode node && node.anchor() != null) {
            anchors.add(node.anchor());
            AnchorManager.track(this);
        }
        dirty = true;
    }

    @Override
    public void onContainerRemoved(NetworkNodeContainer container) {
        GlobalPos position = positions.remove(container);
        if (container.getNode() instanceof ControllerNetworkNode controller) {
            controllers.remove(controller);
        }
        if (container.getNode() instanceof EnergyProvider provider) {
            powerProviders.remove(provider);
        }
        if (position == null) {
            return;
        }
        Map<Long, Integer> dimensionCounts = counts.get(position.dimension());
        long chunk = new ChunkPos(position.pos()).toLong();
        dimensionCounts.computeIfPresent(chunk, (ignored, count) -> count == 1 ? null : count - 1);
        if (container.getNode() instanceof AnchorNode node) {
            anchors.remove(node.anchor());
        }
        dirty = true;
    }

    @Override
    public void onNetworkRemoved() {
        removed = true;
        dirty = true;
    }

    @Override
    public void onNetworkSplit(Set<Network> networks) {
        dirty = true;
    }

    @Override
    public void onNetworkMergedWith(Network network) {
        dirty = true;
    }

    public void invalidate() {
        dirty = true;
    }

    public boolean removed() {
        return removed;
    }

    public boolean hasInitializingPower() {
        return controllers.stream().anyMatch(controller -> !controller.isActive() && controller.getActualStored() > 0);
    }

    public boolean hasPowerProvider() {
        return !powerProviders.isEmpty();
    }

    public List<Area> areas() {
        if (!dirty) {
            return areas;
        }
        Map<ResourceKey<Level>, List<AnchorBlockEntity>> byDimension = new HashMap<>();
        for (AnchorBlockEntity anchor : anchors) {
            if (!anchor.isRemoved() && anchor.enabled() && anchor.node().getNetwork() == network) {
                byDimension.computeIfAbsent(anchor.getLevel().dimension(), ignored -> new ArrayList<>()).add(anchor);
            }
        }
        List<Area> rebuilt = new ArrayList<>();
        if (!removed) {
            byDimension.forEach((dimension, dimensionAnchors) -> {
                dimensionAnchors.sort(POSITION_ORDER);
                rebuilt.add(new Area(network, dimension, List.copyOf(dimensionAnchors),
                        Set.copyOf(counts.getOrDefault(dimension, Map.of()).keySet())));
            });
        }
        areas = List.copyOf(rebuilt);
        dirty = false;
        return areas;
    }

    public record Area(Network network, ResourceKey<Level> dimension, List<AnchorBlockEntity> anchors,
                       Set<Long> chunks) {
        public AnchorBlockEntity leader() {
            return anchors.getFirst();
        }
    }
}
