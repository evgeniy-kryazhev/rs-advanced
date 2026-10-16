package dev.rsadvanced.feature.mining.module;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import dev.rsadvanced.feature.mining.MiningContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

/** A view of the installed loadout bound to one stack. It owns no separate inventory or saved state. */
public final class ToolModuleContainer {
    private final ItemStack stack;
    private final Map<ResourceLocation, ToolModule> modules;

    public ToolModuleContainer(ItemStack stack, List<ToolModule> installed) {
        this.stack = stack;
        Map<ResourceLocation, ToolModule> indexed = new LinkedHashMap<>();
        for (ToolModule module : installed) {
            if (indexed.putIfAbsent(module.id(), module) != null) {
                throw new IllegalArgumentException("Duplicate tool module: " + module.id());
            }
        }
        modules = java.util.Collections.unmodifiableMap(indexed);
    }

    public ItemStack stack() {
        return stack;
    }

    public List<ToolModule> installed() {
        return List.copyOf(modules.values());
    }

    public Optional<ToolModule> find(ResourceLocation id) {
        return Optional.ofNullable(modules.get(id));
    }

    public <M extends ToolModule> M require(M module) {
        if (modules.get(module.id()) != module) {
            throw new IllegalStateException("Required module is not installed: " + module.id());
        }
        return module;
    }

    public void prepare() {
        for (ToolModule module : modules.values()) {
            module.prepare(this);
        }
    }

    public boolean canMine() {
        return modules.values().stream().allMatch(module -> module.canMine(this));
    }

    public boolean correctForDrops(BlockState state) {
        return modules.values().stream().allMatch(module -> module.correctForDrops(this, state));
    }

    public boolean canMineBlock(MiningContext context, BlockPos position, BlockState state, boolean selected) {
        return modules.values().stream().allMatch(module -> module.canMineBlock(context, position, state, selected));
    }

    public long blockCost(MiningContext context, BlockPos position, BlockState state) {
        long cost = 0;
        for (ToolModule module : modules.values()) {
            cost = module.blockCost(context, position, state, cost);
        }
        return cost;
    }

    public List<ToolMiningSession> openMiningSessions(MiningContext context) {
        return modules.values().stream().map(module -> module.openMiningSession(context)).toList();
    }

    public float miningSpeed(BlockState state) {
        float speed = 1;
        for (ToolModule module : modules.values()) {
            speed = module.miningSpeed(this, state, speed);
        }
        return speed;
    }

    public void afterBlockMined(ServerPlayer player, BlockPos position, BlockState originalState) {
        for (ToolModule module : modules.values()) {
            module.afterBlockMined(this, player, position, originalState);
        }
    }

    public InteractionResult use(ServerPlayer player, SlotReference reference) {
        for (ToolModule module : modules.values()) {
            InteractionResult result = module.use(this, player, reference);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        // Opening the card slot is a fallback, so newly added interaction modules can handle the click first.
        for (ToolModule module : modules.values()) {
            InteractionResult result = module.useFallback(this, player, reference);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }

    public boolean select(ResourceLocation moduleId, String option) {
        ToolModule module = modules.get(moduleId);
        if (module == null || module.options(this).stream()
                .noneMatch(choice -> choice.id().equals(option) && choice.enabled())) {
            return false;
        }
        prepare();
        module.applyOption(this, option);
        return true;
    }

    public List<Component> status() {
        List<Component> status = new ArrayList<>();
        for (ToolModule module : modules.values()) {
            status.addAll(module.status(this));
        }
        return List.copyOf(status);
    }

    public void appendTooltip(Item.TooltipContext context, List<Component> lines, TooltipFlag flag) {
        for (ToolModule module : modules.values()) {
            module.appendTooltip(this, context, lines, flag);
        }
    }
}
