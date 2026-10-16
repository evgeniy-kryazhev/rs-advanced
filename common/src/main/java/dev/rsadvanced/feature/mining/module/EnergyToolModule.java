package dev.rsadvanced.feature.mining.module;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.energy.EnergyStorage;
import com.refinedmods.refinedstorage.api.network.impl.energy.AbstractProxyEnergyStorage;
import com.refinedmods.refinedstorage.api.network.impl.energy.EnergyStorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.mining.MiningBalance;
import dev.rsadvanced.feature.mining.MiningContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class EnergyToolModule implements ToolModule {
    @Override
    public ResourceLocation id() {
        return RSAdvanced.id("energy");
    }

    public long stored(ItemStack stack) {
        return stack.getOrDefault(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), 0L);
    }

    public void fill(ItemStack stack) {
        stack.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(),
                MiningBalance.settings().capacity());
    }

    public EnergyStorage storage(ItemStack stack) {
        EnergyStorage delegate = RefinedStorageApi.INSTANCE.asItemEnergyStorage(
                new EnergyStorageImpl(MiningBalance.settings().capacity()), stack);
        return new AbstractProxyEnergyStorage(delegate) {
            @Override
            public long receive(long amount, Action action) {
                // Limit external charging, not restoration of the RS-backed saved energy component.
                return delegate.receive(Math.min(amount, MiningBalance.settings().chargeRate()), action);
            }
        };
    }

    public void refund(ItemStack stack, long payment) {
        // A refund is internal accounting and must bypass the external charge-rate limit.
        stack.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(),
                Math.addExact(stored(stack), payment));
    }

    @Override
    public ToolMiningSession openMiningSession(MiningContext context) {
        return new ToolMiningSession() {
            private long payment;

            @Override
            public boolean beforeBlock(long cost, boolean selected) {
                if (cost < 0) {
                    throw new IllegalArgumentException("Mining cost must not be negative");
                }
                payment = 0;
                if (!RefinedStorageApi.INSTANCE.isEnergyRequired()) {
                    return true;
                }
                long requested = selected ? Math.min(cost, stored(context.tool())) : cost;
                var energy = RefinedStorageApi.INSTANCE.getEnergyStorage(context.tool());
                if (requested <= 0 || energy.isEmpty()
                        || energy.get().extract(requested, Action.SIMULATE) != requested) {
                    if (selected) {
                        context.player().displayClientMessage(
                                Component.translatable("item.rsadvanced.network_mining_tool.no_energy"), true);
                    }
                    return false;
                }
                // Simulation and extraction use the same RS storage on the server thread.
                payment = energy.get().extract(requested, Action.EXECUTE);
                if (payment != requested) {
                    throw new IllegalStateException("Tool energy changed between simulation and extraction");
                }
                return true;
            }

            @Override
            public void afterBlock(boolean removed) {
                if (!removed && payment > 0) {
                    refund(context.tool(), payment);
                }
                payment = 0;
            }
        };
    }
}
