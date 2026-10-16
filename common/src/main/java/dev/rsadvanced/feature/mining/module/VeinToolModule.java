package dev.rsadvanced.feature.mining.module;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.feature.mining.MiningBalance;
import dev.rsadvanced.feature.mining.MiningContext;
import dev.rsadvanced.feature.mining.MiningTarget;
import dev.rsadvanced.feature.mining.MiningNetworking;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.feature.mining.MiningVein;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

public final class VeinToolModule implements ToolModule {
    @Override
    public ResourceLocation id() {
        return RSAdvanced.id("vein");
    }

    public boolean selected(ItemStack stack) {
        boolean legacy = stack.getOrDefault(AdvancedComponents.MINING_MODE.get(), MiningMode.NORMAL) == MiningMode.VEIN;
        return stack.getOrDefault(AdvancedComponents.MINING_VEIN.get(), legacy);
    }

    public boolean active(ItemStack stack) {
        return selected(stack) && MiningBalance.settings().veinEnabled();
    }

    public void set(ItemStack stack, boolean selected) {
        stack.set(AdvancedComponents.MINING_VEIN.get(), selected);
        ToolModules.SPEED.set(stack, ToolModules.SPEED.mode(stack));
    }

    public List<MiningVein.Target> targets(ToolModuleContainer container, ServerLevel level,
                                         Map<BlockPos, BlockState> seeds, Set<BlockPos> area) {
        if (!active(container.stack())) {
            return List.of();
        }
        return MiningVein.find(level, seeds, area, MiningBalance.settings().veinLimit());
    }

    public long cost(ToolModuleContainer container, float hardness, int distance) {
        return MiningBalance.veinCost(container.require(ToolModules.SPEED).mode(container.stack()), hardness, distance);
    }

    @Override
    public Iterable<MiningTarget> miningTargets(MiningContext context) {
        if (context.player().isCreative() || !active(context.tool())) {
            return List.of();
        }
        var traversal = MiningVein.traverse(context.level(), context.removedBlocks(), context.plannedPositions(),
                MiningBalance.settings().veinLimit(), target -> {
                    BlockState removed = context.removedBlocks().get(target.position());
                    return removed != null && removed.is(target.block());
                });
        return () -> new Iterator<>() {
            private final Iterator<MiningVein.Target> targets = traversal.iterator();

            @Override
            public boolean hasNext() {
                return targets.hasNext();
            }

            @Override
            public MiningTarget next() {
                MiningVein.Target target = targets.next();
                return new MiningTarget(target.position(),
                        () -> context.removedBlocks().containsKey(target.parent()),
                        state -> state.is(target.block()),
                        state -> cost(context.modules(), state.getDestroySpeed(context.level(), target.position()), target.distance()),
                        () -> MiningNetworking.sendArc(context.level(), target.parent(), target.position()));
            }
        };
    }

    @Override
    public List<ToolModuleOption> options(ToolModuleContainer container) {
        return List.of(new ToolModuleOption("toggle", Component.translatable("gui.rsadvanced.mining_vein"),
                new ItemStack(Items.IRON_ORE), MiningBalance.settings().veinEnabled()));
    }

    @Override
    public String selectedOption(ToolModuleContainer container) {
        return active(container.stack()) ? "toggle" : "";
    }

    @Override
    public void applyOption(ToolModuleContainer container, String option) {
        set(container.stack(), !selected(container.stack()));
    }

    @Override
    public List<Component> status(ToolModuleContainer container) {
        return List.of(Component.translatable(selected(container.stack())
                ? "gui.rsadvanced.mining_vein_on" : "gui.rsadvanced.mining_vein_off"));
    }

    @Override
    public void appendTooltip(ToolModuleContainer container, Item.TooltipContext context,
                              List<Component> lines, TooltipFlag flag) {
        lines.addAll(status(container));
    }
}
