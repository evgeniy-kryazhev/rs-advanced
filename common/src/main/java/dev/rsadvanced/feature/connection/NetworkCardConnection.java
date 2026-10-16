package dev.rsadvanced.feature.connection;

import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.SecurityHelper;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import com.refinedmods.refinedstorage.common.content.Items;
import com.refinedmods.refinedstorage.common.networking.NetworkReceiverBlockEntity;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Shared, uncached lookup for the 1.3.0 terminal and mining tool. */
public final class NetworkCardConnection {
    private NetworkCardConnection() {
    }

    /**
     * Resolve immediately before an operation, with all permissions required by that operation.
     * This method neither loads chunks nor applies wireless range or dimension restrictions.
     */
    public static Optional<Network> resolve(ServerPlayer player, ItemStack card,
                                             Permission... requiredPermissions) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(card, "card");
        Objects.requireNonNull(requiredPermissions, "requiredPermissions");
        for (Permission permission : requiredPermissions) {
            Objects.requireNonNull(permission, "permission");
        }
        requireServerThread(player);

        if (card.isEmpty()) {
            return Optional.empty();
        }
        if (!card.is(Items.INSTANCE.getNetworkCard())) {
            return Optional.empty();
        }
        GlobalPos location = card.get(DataComponents.INSTANCE.getNetworkLocation());
        if (location == null) {
            return Optional.empty();
        }

        ServerLevel receiverLevel = player.getServer().getLevel(location.dimension());
        if (receiverLevel == null) {
            return Optional.empty();
        }
        // getBlockEntity can request a chunk. Never call it until the chunk is already present.
        if (!receiverLevel.isLoaded(location.pos())) {
            return Optional.empty();
        }
        if (!(receiverLevel.getBlockEntity(location.pos()) instanceof NetworkReceiverBlockEntity receiver)
                || receiver.isRemoved()) {
            return Optional.empty();
        }
        Network network = receiver.getNetworkForItem();
        if (network == null) {
            return Optional.empty();
        }
        boolean receiverActive = receiver.getContainerProvider().getContainers().stream()
                .anyMatch(container -> container.getNode() instanceof AbstractNetworkNode node
                        && node.getNetwork() == network && node.isActive());
        boolean networkPowered = !RefinedStorageApi.INSTANCE.isEnergyRequired()
                || network.getComponent(EnergyNetworkComponent.class).getStored() > 0;
        if (!receiverActive || !networkPowered) {
            return Optional.empty();
        }
        for (Permission permission : requiredPermissions) {
            if (!SecurityHelper.isAllowed(player, permission, network)) {
                return Optional.empty();
            }
        }
        return Optional.of(network);
    }

    static void requireServerThread(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null || !server.isSameThread()) {
            throw new IllegalStateException("Network card access must run on the server thread");
        }
    }
}
