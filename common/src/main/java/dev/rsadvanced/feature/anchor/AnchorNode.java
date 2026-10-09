package dev.rsadvanced.feature.anchor;

import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;

public final class AnchorNode extends AbstractNetworkNode {
    private AnchorBlockEntity anchor;
    private long reportedCost;

    public void bind(AnchorBlockEntity anchor) {
        this.anchor = anchor;
    }

    public AnchorBlockEntity anchor() {
        return anchor;
    }

    public void reportCost(long cost) {
        reportedCost = cost;
    }

    @Override
    public long getEnergyUsage() {
        return reportedCost;
    }

    @Override
    public void doWork() {
        // The manager charges the whole area atomically once per server tick.
        // RS still reads getEnergyUsage() when presenting network statistics.
    }
}
