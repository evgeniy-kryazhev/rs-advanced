package dev.rsadvanced.feature.mining.module;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.mining.MiningBalance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public final class CombatToolModule implements ToolModule {
    @Override
    public ResourceLocation id() {
        return RSAdvanced.id("combat");
    }

    public AttributeModifier damageModifier(ToolModuleContainer container) {
        long energy = Long.MAX_VALUE;
        if (RefinedStorageApi.INSTANCE.isEnergyRequired()) {
            energy = container.require(ToolModules.ENERGY).stored(container.stack());
        }
        return new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, MiningBalance.damage(energy),
                AttributeModifier.Operation.ADD_VALUE);
    }

    public AttributeModifier speedModifier() {
        return new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void beforeAttack(ToolModuleContainer container, Player player) {
        var damage = player.getAttribute(Attributes.ATTACK_DAMAGE);
        var speed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (damage != null && speed != null) {
            // Update before damage is calculated, preserving attribute modifiers from other sources.
            damage.removeModifier(Item.BASE_ATTACK_DAMAGE_ID);
            damage.addTransientModifier(damageModifier(container));
            speed.removeModifier(Item.BASE_ATTACK_SPEED_ID);
            speed.addTransientModifier(speedModifier());
        }
    }

    @Override
    public void hurtEnemy(ToolModuleContainer container, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && RefinedStorageApi.INSTANCE.isEnergyRequired()) {
            container.require(ToolModules.ENERGY).storage(container.stack())
                    .extract(MiningBalance.settings().weaponCost(), Action.EXECUTE);
        }
    }
}
