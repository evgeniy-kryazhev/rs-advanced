package dev.rsadvanced.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rsadvanced.feature.mining.MiningCombat;
import dev.rsadvanced.feature.mining.MiningItem;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
public abstract class MiningAttributesMixin {
    @WrapMethod(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V")
    private void rsadvanced$slotModifiers(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
                                         Operation<Void> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof MiningItem) || slot != EquipmentSlot.MAINHAND) {
            original.call(slot, consumer);
            return;
        }
        original.call(slot, filtered(consumer));
        addModifiers(stack, consumer);
    }

    @WrapMethod(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V")
    private void rsadvanced$groupModifiers(EquipmentSlotGroup group, BiConsumer<Holder<Attribute>, AttributeModifier> consumer,
                                          Operation<Void> original) {
        ItemStack stack = (ItemStack) (Object) this;
        if (!(stack.getItem() instanceof MiningItem) || !group.test(EquipmentSlot.MAINHAND)) {
            original.call(group, consumer);
            return;
        }
        original.call(group, filtered(consumer));
        addModifiers(stack, consumer);
    }

    private static BiConsumer<Holder<Attribute>, AttributeModifier> filtered(BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        return (attribute, modifier) -> {
            if (!modifier.id().equals(Item.BASE_ATTACK_DAMAGE_ID) && !modifier.id().equals(Item.BASE_ATTACK_SPEED_ID)) {
                consumer.accept(attribute, modifier);
            }
        };
    }

    private static void addModifiers(ItemStack stack, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        consumer.accept(Attributes.ATTACK_DAMAGE, MiningCombat.damageModifier(stack));
        consumer.accept(Attributes.ATTACK_SPEED, MiningCombat.speedModifier());
    }
}
