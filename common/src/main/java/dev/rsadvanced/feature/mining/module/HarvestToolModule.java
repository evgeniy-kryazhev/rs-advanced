package dev.rsadvanced.feature.mining.module;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.mining.MiningContext;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class HarvestToolModule implements ToolModule {
    public static final TagKey<Block> INCORRECT = TagKey.create(Registries.BLOCK,
            RSAdvanced.id("incorrect_for_mining_tool"));

    @Override
    public ResourceLocation id() {
        return RSAdvanced.id("harvest");
    }

    public Tool properties() {
        return new Tool(List.of(Tool.Rule.deniesDrops(INCORRECT)), 1, 0);
    }

    @Override
    public boolean correctForDrops(ToolModuleContainer container, BlockState state) {
        return !state.is(INCORRECT);
    }

    @Override
    public boolean canMineBlock(MiningContext context, BlockPos position, BlockState state, boolean selected) {
        return (context.player().isCreative() || state.getDestroySpeed(context.level(), position) >= 0)
                && (selected || (state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && context.player().hasCorrectToolForDrops(state)));
    }

    @Override
    public void afterBlockMined(ToolModuleContainer container, ServerPlayer player,
                                BlockPos position, BlockState originalState) {
        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, position.getX() + 0.5,
                position.getY() + 0.5, position.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
    }

    @Override
    public void prepare(ToolModuleContainer container) {
        // Legacy enchantments are removed on first server use, preserving the existing tool policy.
        container.stack().remove(DataComponents.ENCHANTMENTS);
        container.stack().remove(DataComponents.STORED_ENCHANTMENTS);
    }
}
