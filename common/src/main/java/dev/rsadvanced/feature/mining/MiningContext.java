package dev.rsadvanced.feature.mining;

import dev.rsadvanced.feature.mining.module.ToolModuleContainer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Shared operation ledger. Module state and network/energy sessions do not belong here. */
public final class MiningContext {
    private final ServerPlayer player;
    private final ServerLevel level;
    private final ToolModuleContainer modules;
    private final BlockPos center;
    private final Direction face;
    private final BlockState selectedState;
    private final Set<BlockPos> planned = new LinkedHashSet<>();
    private final Map<BlockPos, BlockState> removed = new LinkedHashMap<>();

    public MiningContext(ServerPlayer player, ToolModuleContainer modules, BlockPos center, Direction face) {
        this.player = player;
        level = player.serverLevel();
        this.modules = modules;
        this.center = center.immutable();
        this.face = face;
        if (!level.isLoaded(center)) {
            throw new IllegalArgumentException("Mining context requires a loaded selected block");
        }
        selectedState = level.getBlockState(center);
        planned.add(this.center);
    }

    public ServerPlayer player() {
        return player;
    }

    public ServerLevel level() {
        return level;
    }

    public ToolModuleContainer modules() {
        return modules;
    }

    public ItemStack tool() {
        return modules.stack();
    }

    public BlockPos center() {
        return center;
    }

    public Direction face() {
        return face;
    }

    public BlockState selectedState() {
        return selectedState;
    }

    public boolean ownsTool() {
        return player.serverLevel() == level && player.getMainHandItem() == tool();
    }

    public Set<BlockPos> plannedPositions() {
        return Collections.unmodifiableSet(planned);
    }

    public Map<BlockPos, BlockState> removedBlocks() {
        return Collections.unmodifiableMap(removed);
    }

    public boolean plan(BlockPos position) {
        return planned.add(position.immutable());
    }

    public void recordRemoved(BlockPos position, BlockState state) {
        removed.put(position.immutable(), state);
    }
}
