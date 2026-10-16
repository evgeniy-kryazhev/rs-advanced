package dev.rsadvanced.feature.mining.neoforge;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningMode;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public final class MiningItemFactoryImpl {
    private MiningItemFactoryImpl() {
    }

    public static MiningItem create() {
        return new NeoForgeMiningItem();
    }

    private static final class NeoForgeMiningItem extends MiningItem {
        @Override
        public boolean canPerformAction(ItemStack stack, ItemAbility action) {
            if (MiningItem.mode(stack) == MiningMode.OFF) {
                return false;
            }
            return (!RefinedStorageApi.INSTANCE.isEnergyRequired()
                    || MiningItem.energy(stack) > 0)
                    && (action == ItemAbilities.AXE_DIG || action == ItemAbilities.HOE_DIG
                    || action == ItemAbilities.SHOVEL_DIG || action == ItemAbilities.PICKAXE_DIG
                    || action == ItemAbilities.SWORD_DIG);
        }

        @Override
        public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
            return false;
        }

        @Override
        public boolean supportsEnchantment(ItemStack stack,
                Holder<Enchantment> enchantment) {
            return false;
        }

        @Override
        public boolean isPrimaryItemFor(ItemStack stack,
                Holder<Enchantment> enchantment) {
            return false;
        }
    }
}
