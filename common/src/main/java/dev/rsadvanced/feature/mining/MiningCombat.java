package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MiningCombat {
    private MiningCombat() {
    }

    public static AttributeModifier damageModifier(ItemStack stack) {
        long energy = RefinedStorageApi.INSTANCE.isEnergyRequired() ? MiningItem.energy(stack) : Long.MAX_VALUE;
        return new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, MiningBalance.damage(energy),
                AttributeModifier.Operation.ADD_VALUE);
    }

    public static AttributeModifier speedModifier() {
        return new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE);
    }

    public static void prepareAttack(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof MiningItem)) {
            return;
        }
        if (!player.level().isClientSide()) {
            MiningItem.prepare(stack);
        }
        var damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (damage != null && speed != null) {
            // Update before the actual damage calculation, without touching modifiers from other sources.
            damage.removeModifier(Item.BASE_ATTACK_DAMAGE_ID);
            damage.addTransientModifier(damageModifier(stack));
            speed.removeModifier(Item.BASE_ATTACK_SPEED_ID);
            speed.addTransientModifier(speedModifier());
        }
    }
}
