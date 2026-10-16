package dev.rsadvanced.feature.mining.module;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.mining.MiningContext;
import dev.rsadvanced.feature.mining.MiningTarget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

public final class AreaToolModule implements ToolModule {
    private static final String LEGACY_RADIUS_KEY = "rsadvanced_mining_mode";

    @Override
    public ResourceLocation id() {
        return RSAdvanced.id("area");
    }

    public int radius(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getInt(LEGACY_RADIUS_KEY), 0, 2);
    }

    public void set(ItemStack stack, int radius) {
        // Retain the existing saved key and unrelated custom data. A module container needs no migration copy.
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putInt(LEGACY_RADIUS_KEY, Math.clamp(radius, 0, 2)));
    }

    public List<BlockPos> positions(BlockPos center, Direction face, int radius) {
        int boundedRadius = Math.clamp(radius, 0, 2);
        List<BlockPos> positions = new ArrayList<>();
        for (int first = -boundedRadius; first <= boundedRadius; first++) {
            for (int second = -boundedRadius; second <= boundedRadius; second++) {
                positions.add(switch (face.getAxis()) {
                    case X -> center.offset(0, first, second);
                    case Y -> center.offset(first, 0, second);
                    case Z -> center.offset(first, second, 0);
                });
            }
        }
        positions.sort(Comparator.comparingDouble(center::distSqr));
        return positions;
    }

    @Override
    public List<MiningTarget> miningTargets(MiningContext context) {
        if (context.player().isCreative() || !context.player().hasCorrectToolForDrops(context.selectedState())) {
            return List.of();
        }
        return positions(context.center(), context.face(), radius(context.tool())).stream()
                .filter(position -> !position.equals(context.center()))
                .map(position -> MiningTarget.block(context, position)).toList();
    }

    @Override
    public List<ToolModuleOption> options(ToolModuleContainer container) {
        List<ToolModuleOption> options = new ArrayList<>();
        for (int size : List.of(1, 3, 5)) {
            options.add(new ToolModuleOption(Integer.toString(size), Component.literal(size + "×" + size),
                    new ItemStack(Items.STONE), true));
        }
        return List.copyOf(options);
    }

    @Override
    public String selectedOption(ToolModuleContainer container) {
        return Integer.toString(radius(container.stack()) * 2 + 1);
    }

    @Override
    public void applyOption(ToolModuleContainer container, String option) {
        set(container.stack(), (Integer.parseInt(option) - 1) / 2);
    }

    @Override
    public List<Component> status(ToolModuleContainer container) {
        int size = radius(container.stack()) * 2 + 1;
        return List.of(Component.literal(size + "×" + size));
    }

    @Override
    public void appendTooltip(ToolModuleContainer container, Item.TooltipContext context,
                              List<Component> lines, TooltipFlag flag) {
        int size = radius(container.stack()) * 2 + 1;
        lines.add(Component.translatable("item.rsadvanced.network_mining_tool.mode", size, size));
    }
}
