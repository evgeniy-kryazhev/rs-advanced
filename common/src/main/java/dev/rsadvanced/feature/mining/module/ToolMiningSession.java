package dev.rsadvanced.feature.mining.module;

import net.minecraft.world.entity.item.ItemEntity;

/** Per-operation state owned by a module, including transactional payment or an original network route. */
public interface ToolMiningSession {
    ToolMiningSession NONE = new ToolMiningSession() { };

    default boolean canContinue() {
        return true;
    }

    /** False stops expansion and rolls back every reservation already started for this block. */
    default boolean beforeBlock(long cost, boolean selected) {
        return true;
    }

    /** Called even after a rejected reservation or an exception; removed means the world actually changed. */
    default void afterBlock(boolean removed) {
    }

    default boolean routeDrop(ItemEntity entity) {
        return false;
    }
}
