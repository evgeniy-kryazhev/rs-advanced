package dev.rsadvanced.feature.mining;

import dev.rsadvanced.feature.mining.module.ToolMiningSession;
import java.util.ArrayList;
import java.util.List;

/** Coordinates module reservations. Each module decides what to reserve and how to finish or roll it back. */
final class MiningBlockTransaction implements AutoCloseable {
    private final List<ToolMiningSession> started = new ArrayList<>();
    private boolean removed;

    boolean reserve(List<ToolMiningSession> sessions, long cost, boolean selected) {
        for (ToolMiningSession session : sessions) {
            started.add(session);
            if (!session.beforeBlock(cost, selected)) {
                return false;
            }
        }
        return true;
    }

    void removed(boolean removed) {
        this.removed = removed;
    }

    @Override
    public void close() {
        Throwable failure = null;
        for (int index = started.size() - 1; index >= 0; index--) {
            try {
                started.get(index).afterBlock(removed);
            } catch (RuntimeException | Error exception) {
                // One failing extension must not prevent another module from refunding its reservation.
                if (failure == null) {
                    failure = exception;
                } else if (failure != exception) {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }
}
