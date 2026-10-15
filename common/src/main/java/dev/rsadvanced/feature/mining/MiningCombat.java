package dev.rsadvanced.feature.mining;

import dev.rsadvanced.feature.mining.module.ToolModules;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class MiningCombat {
    private MiningCombat() {
    }

    public static AttributeModifier damageModifier(ItemStack stack) {
        var modules = MiningItem.modules(stack);
        return modules.require(ToolModules.COMBAT).damageModifier(modules);
    }

    public static AttributeModifier speedModifier() {
        return ToolModules.COMBAT.speedModifier();
    }

    public static void prepareAttack(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MiningItem)) {
            return;
        }
        var modules = MiningItem.modules(stack);
        if (!player.level().isClientSide()) {
            modules.prepare();
        }
        modules.beforeAttack(player);
    }
}
