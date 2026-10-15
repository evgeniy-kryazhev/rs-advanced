package dev.rsadvanced.feature.mining.module;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.feature.mining.MiningBalance;
import dev.rsadvanced.feature.mining.MiningContext;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.mixin.MiningBlockStateAccessor;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

public final class SpeedToolModule implements ToolModule {
    @Override
    public ResourceLocation id() {
        return RSAdvanced.id("speed");
    }

    public MiningMode mode(ItemStack stack) {
        MiningMode stored = stack.getOrDefault(AdvancedComponents.MINING_MODE.get(), MiningMode.NORMAL);
        return stored == MiningMode.VEIN ? MiningMode.NORMAL : stored;
    }

    public void set(ItemStack stack, MiningMode mode) {
        migrate(stack);
        if (mode == MiningMode.VEIN) {
            stack.set(AdvancedComponents.MINING_VEIN.get(), true);
            mode = MiningMode.NORMAL;
        }
        stack.set(AdvancedComponents.MINING_MODE.get(), mode);
    }

    private void migrate(ItemStack stack) {
        if (stack.getOrDefault(AdvancedComponents.MINING_MODE.get(), MiningMode.NORMAL) == MiningMode.VEIN) {
            // Preserve an explicit independent flag while decoding the old exclusive mode.
            stack.set(AdvancedComponents.MINING_VEIN.get(),
                    stack.getOrDefault(AdvancedComponents.MINING_VEIN.get(), true));
            stack.set(AdvancedComponents.MINING_MODE.get(), MiningMode.NORMAL);
        }
    }

    @Override
    public void prepare(ToolModuleContainer container) {
        migrate(container.stack());
    }

    @Override
    public boolean canMine(ToolModuleContainer container) {
        return mode(container.stack()) != MiningMode.OFF;
    }

    public long cost(ItemStack stack, float hardness) {
        return MiningBalance.cost(mode(stack), hardness);
    }

    @Override
    public long blockCost(MiningContext context, BlockPos position, BlockState state, long currentCost) {
        return cost(context.tool(), state.getDestroySpeed(context.level(), position));
    }

    @Override
    public float miningSpeed(ToolModuleContainer container, BlockState state, float currentSpeed) {
        MiningMode mode = mode(container.stack());
        if (!RefinedStorageApi.INSTANCE.isEnergyRequired()) {
            return mode.speed();
        }
        long required = cost(container.stack(), ((MiningBlockStateAccessor) state).rsadvanced$rawDestroySpeed());
        long available = container.require(ToolModules.ENERGY).stored(container.stack());
        return MiningBalance.speed(mode, available, required);
    }

    @Override
    public List<ToolModuleOption> options(ToolModuleContainer container) {
        return MiningMode.SPEED_MODES.stream().map(mode -> {
            var icon = switch (mode) {
                case NORMAL -> Items.CLOCK;
                case SLOW -> Items.FEATHER;
                case FAST -> Items.SUGAR;
                case OFF -> Items.BARRIER;
                case VEIN -> throw new IllegalArgumentException("Legacy Vein is not a speed choice");
            };
            return new ToolModuleOption(mode.getSerializedName(), label(mode), new ItemStack(icon), true);
        }).toList();
    }

    @Override
    public String selectedOption(ToolModuleContainer container) {
        return mode(container.stack()).getSerializedName();
    }

    @Override
    public void applyOption(ToolModuleContainer container, String option) {
        MiningMode selected = MiningMode.SPEED_MODES.stream()
                .filter(mode -> mode.getSerializedName().equals(option)).findFirst().orElseThrow();
        set(container.stack(), selected);
    }

    @Override
    public List<Component> status(ToolModuleContainer container) {
        return List.of(label(mode(container.stack())));
    }

    @Override
    public void appendTooltip(ToolModuleContainer container, Item.TooltipContext context,
                              List<Component> lines, TooltipFlag flag) {
        MiningMode mode = mode(container.stack());
        lines.add(Component.translatable("item.rsadvanced.network_mining_tool.speed_mode", label(mode), mode.speed()));
    }

    private static Component label(MiningMode mode) {
        return Component.translatable("gui.rsadvanced.mining_mode." + mode.getSerializedName());
    }
}
