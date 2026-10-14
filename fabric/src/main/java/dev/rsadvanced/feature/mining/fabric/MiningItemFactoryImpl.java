package dev.rsadvanced.feature.mining.fabric;

import dev.rsadvanced.feature.mining.MiningItem;
import net.fabricmc.fabric.api.item.v1.EnchantingContext;
import net.fabricmc.fabric.api.item.v1.FabricItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public final class MiningItemFactoryImpl {
    private MiningItemFactoryImpl() {
    }

    public static MiningItem create() {
        return new FabricMiningItem();
    }

    private static final class FabricMiningItem extends MiningItem implements FabricItem {
        @Override
        public boolean canBeEnchantedWith(ItemStack stack,
                Holder<Enchantment> enchantment,
                EnchantingContext context) {
            return false;
        }
    }
}
