package dev.rsadvanced.feature.mining.module;

import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import dev.rsadvanced.feature.mining.MiningContext;
import dev.rsadvanced.feature.mining.MiningTarget;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;

/** Stateless behavior. Every item's mutable configuration belongs to its own ItemStack. */
public interface ToolModule {
    ResourceLocation id();

    default void prepare(ToolModuleContainer container) {
    }

    default boolean canMine(ToolModuleContainer container) {
        return true;
    }

    default boolean correctForDrops(ToolModuleContainer container, BlockState state) {
        return true;
    }

    default boolean canMineBlock(MiningContext context, BlockPos position, BlockState state, boolean selected) {
        return true;
    }

    default long blockCost(MiningContext context, BlockPos position, BlockState state, long currentCost) {
        return currentCost;
    }

    /** The coordinator finishes each yielded block before requesting the next; do not collect lazy targets upfront. */
    default Iterable<MiningTarget> miningTargets(MiningContext context) {
        return List.of();
    }

    default ToolMiningSession openMiningSession(MiningContext context) {
        return ToolMiningSession.NONE;
    }

    default float miningSpeed(ToolModuleContainer container, BlockState state, float currentSpeed) {
        return currentSpeed;
    }

    default void afterBlockMined(ToolModuleContainer container, ServerPlayer player,
                                 BlockPos position, BlockState originalState) {
    }

    default void hurtEnemy(ToolModuleContainer container, LivingEntity target, LivingEntity attacker) {
    }

    default void beforeAttack(ToolModuleContainer container, Player player) {
    }

    default InteractionResult use(ToolModuleContainer container, ServerPlayer player, SlotReference reference) {
        return InteractionResult.PASS;
    }

    default InteractionResult useFallback(ToolModuleContainer container, ServerPlayer player, SlotReference reference) {
        return InteractionResult.PASS;
    }

    default List<ToolModuleOption> options(ToolModuleContainer container) {
        return List.of();
    }

    default String selectedOption(ToolModuleContainer container) {
        return "";
    }

    /** Called only after the container has validated the option against this module's available choices. */
    default void applyOption(ToolModuleContainer container, String option) {
        throw new IllegalArgumentException("Module has no configurable options: " + id());
    }

    default List<Component> status(ToolModuleContainer container) {
        return List.of();
    }

    default void appendTooltip(ToolModuleContainer container, Item.TooltipContext context,
                               List<Component> lines, TooltipFlag flag) {
    }
}
