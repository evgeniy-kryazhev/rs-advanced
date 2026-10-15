package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.rsadvanced.feature.connection.NetworkCardConnection;
import dev.rsadvanced.feature.connection.NetworkCardItemContext;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Synchronous scope around vanilla player destruction, including loader protection and loot events. */
public final class MiningOperation {
    private static final ThreadLocal<DropScope> DROPS = new ThreadLocal<>();

    private MiningOperation() {
    }

    public static boolean destroy(ServerPlayer player, BlockPos center, Direction face, BooleanSupplier original) {
        ItemStack tool = player.getMainHandItem();
        if (!(tool.getItem() instanceof MiningItem) || DROPS.get() != null) {
            return original.getAsBoolean();
        }
        MiningItem.prepare(tool);
        MiningMode mode = MiningItem.mode(tool);
        if (mode == MiningMode.OFF) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(center)) {
            return false;
        }
        BlockState state = level.getBlockState(center);
        if (!eligible(player, center, state, false)) {
            return false;
        }
        var reference = RefinedStorageApi.INSTANCE.createInventorySlotReference(player, InteractionHand.MAIN_HAND);
        var context = new NetworkCardItemContext(player, reference, NetworkCardSlot::card, BuiltinPermission.INSERT);
        Network network = context.connection().orElse(null);
        int radius = player.isCreative() || !player.hasCorrectToolForDrops(state) ? 0 : MiningItem.radius(tool);
        List<BlockPos> positions = area(center, face, radius);
        var seeds = new LinkedHashMap<BlockPos, BlockState>();
        long centerCost = MiningBalance.cost(mode, state.getDestroySpeed(level, center));
        if (!breakOne(player, center, tool, context, network, centerCost, true, original)) {
            return false;
        }
        seeds.put(center.immutable(), state);
        for (BlockPos candidate : positions) {
            if (candidate.equals(center)) {
                continue;
            }
            if (!canExpand(player, tool, network)) {
                return true;
            }
            if (!level.isLoaded(candidate)) {
                continue;
            }
            BlockState nextState = level.getBlockState(candidate);
            if (!eligible(player, candidate, nextState, true)) {
                continue;
            }
            long cost = MiningBalance.cost(mode, nextState.getDestroySpeed(level, candidate));
            if (RefinedStorageApi.INSTANCE.isEnergyRequired() && MiningItem.energy(tool) < cost) {
                return true;
            }
            if (breakOne(player, candidate, tool, context, network, cost, false,
                    () -> player.gameMode.destroyBlock(candidate))) {
                seeds.put(candidate.immutable(), nextState);
            }
        }
        if (player.isCreative() || !MiningItem.veinMining(tool) || !MiningBalance.settings().veinEnabled()) {
            return true;
        }
        // Only successfully removed area blocks seed veins; protected blocks cannot start expansion.
        var targets = MiningVein.find(level, seeds, new HashSet<>(positions), MiningBalance.settings().veinLimit());
        var removed = new HashSet<>(seeds.keySet());
        for (MiningVein.Target target : targets) {
            if (!canExpand(player, tool, network)) {
                break;
            }
            BlockPos candidate = target.position();
            if (!removed.contains(target.parent()) || !level.isLoaded(candidate)) {
                continue;
            }
            BlockState nextState = level.getBlockState(candidate);
            if (!nextState.is(target.block()) || !eligible(player, candidate, nextState, true)) {
                continue;
            }
            long cost = MiningBalance.veinCost(mode, nextState.getDestroySpeed(level, candidate), target.distance());
            if (RefinedStorageApi.INSTANCE.isEnergyRequired() && MiningItem.energy(tool) < cost) {
                break;
            }
            if (breakOne(player, candidate, tool, context, network, cost, false,
                    () -> player.gameMode.destroyBlock(candidate))) {
                removed.add(candidate);
                MiningNetworking.sendArc(level, target.parent(), candidate);
            }
        }
        return true;
    }

    private static boolean canExpand(ServerPlayer player, ItemStack tool, Network network) {
        if (player.getMainHandItem() != tool) {
            return false;
        }
        // Offline mining is independent of a card; online mining retains its original route.
        return network == null || NetworkCardConnection.resolve(player, NetworkCardSlot.card(tool),
                BuiltinPermission.INSERT).orElse(null) == network;
    }

    private static boolean eligible(ServerPlayer player, BlockPos pos, BlockState state, boolean expanded) {
        ServerLevel level = player.serverLevel();
        return level.isLoaded(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.mayInteract(player, pos) && !state.isAir()
                && (player.isCreative() || state.getDestroySpeed(level, pos) >= 0)
                // Loot suitability restricts the extra area, not the block the player actually selected.
                && (!expanded || (state.getFluidState().isEmpty() && !state.hasBlockEntity()
                && player.hasCorrectToolForDrops(state)));
    }

    private static boolean breakOne(ServerPlayer player, BlockPos pos, ItemStack tool,
                                    NetworkCardItemContext context, Network network, long requestedCost,
                                    boolean allowPartialPayment, BooleanSupplier original) {
        boolean charged = RefinedStorageApi.INSTANCE.isEnergyRequired();
        long payment = charged && allowPartialPayment ? Math.min(requestedCost, MiningItem.energy(tool)) : requestedCost;
        if (charged && (payment <= 0 || !context.tryConsumeEnergy(payment))) {
            player.displayClientMessage(Component.translatable("item.rsadvanced.network_mining_tool.no_energy"), true);
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockState before = level.getBlockState(pos);
        boolean removed = false;
        DROPS.set(new DropScope(player, pos.immutable(), tool, NetworkCardSlot.card(tool), network));
        try {
            boolean result = original.getAsBoolean();
            removed = level.getBlockState(pos) != before;
            if (removed) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5,
                        pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.02);
            }
            return result && removed;
        } finally {
            DROPS.remove();
            if (charged && !removed && level.getBlockState(pos) == before) {
                // Refund is not external charging and must not be truncated by the charge-rate limiter.
                tool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(),
                        Math.addExact(MiningItem.energy(tool), payment));
            }
        }
    }

    public static List<BlockPos> area(BlockPos center, Direction face, int radius) {
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

    /** Called at actual entity insertion, after loader loot events have modified or cancelled drops. */
    public static boolean routeDrop(ServerLevel level, ItemEntity entity) {
        DropScope scope = DROPS.get();
        if (scope == null || scope.network == null || level != scope.player.serverLevel()
                || !entity.blockPosition().equals(scope.pos) || scope.player.getMainHandItem() != scope.tool
                || !ItemStack.matches(scope.card, NetworkCardSlot.card(scope.tool))) {
            return false;
        }
        // Payment may have used the last FE. Revalidate the route and permission independently of tool charge.
        Network liveNetwork = NetworkCardConnection.resolve(scope.player, scope.card, BuiltinPermission.INSERT)
                .orElse(null);
        if (liveNetwork != scope.network) {
            return false;
        }
        ItemStack remaining = entity.getItem().copy();
        long inserted = liveNetwork.getComponent(StorageNetworkComponent.class).insert(
                ItemResource.ofItemStack(remaining), remaining.getCount(), Action.EXECUTE, new PlayerActor(scope.player));
        remaining.shrink(Math.toIntExact(inserted));
        if (!remaining.isEmpty()) {
            scope.player.getInventory().add(remaining);
        }
        if (remaining.isEmpty()) {
            return true;
        }
        entity.setItem(remaining);
        return false;
    }

    private record DropScope(ServerPlayer player, BlockPos pos, ItemStack tool, ItemStack card, Network network) {
    }
}
