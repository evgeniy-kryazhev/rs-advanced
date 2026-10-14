package dev.rsadvanced.feature.connection;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.energy.EnergyStorage;
import com.refinedmods.refinedstorage.api.network.security.Permission;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.item.NetworkItemContext;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** A single portable-device session backed by a live card slot and the device's RS energy storage. */
public final class NetworkCardItemContext implements NetworkItemContext {
    private final ServerPlayer player;
    private final SlotReference deviceSlot;
    private final ItemStack sessionDevice;
    private final NetworkItemContext deviceContext;
    private final Function<ItemStack, ItemStack> cardReader;
    private final Permission[] requiredPermissions;
    private Network sessionNetwork;
    private GlobalPos sessionReceiver;

    /**
     * The reader must return the currently installed card, or ItemStack.EMPTY, without mutating the device.
     * Pass OPEN for a terminal session and INSERT for a mining operation; check further operation rights separately.
     */
    public NetworkCardItemContext(ServerPlayer player, SlotReference deviceSlot,
                                  Function<ItemStack, ItemStack> cardReader, Permission... requiredPermissions) {
        this.player = Objects.requireNonNull(player, "player");
        NetworkCardConnection.requireServerThread(player);
        this.deviceSlot = Objects.requireNonNull(deviceSlot, "deviceSlot");
        this.sessionDevice = deviceSlot.resolve(player).orElse(ItemStack.EMPTY);
        // Reuse RS's device activity and energy behavior. Its network lookup remains range-limited,
        // so only the receiver route is supplied by this adapter.
        this.deviceContext = RefinedStorageApi.INSTANCE.getNetworkItemHelper()
                .createContext(sessionDevice, player, deviceSlot);
        this.cardReader = Objects.requireNonNull(cardReader, "cardReader");
        this.requiredPermissions = Objects.requireNonNull(requiredPermissions, "requiredPermissions").clone();
        for (Permission permission : this.requiredPermissions) {
            Objects.requireNonNull(permission, "permission");
        }
    }

    /** Rechecks the session permissions and any additional rights needed for the next operation. */
    public Optional<Network> connection(Permission... operationPermissions) {
        NetworkCardConnection.requireServerThread(player);
        Optional<ItemStack> device = currentDevice();
        if (device.isEmpty()) {
            return Optional.empty();
        }
        if (!deviceContext.isActive()) {
            return Optional.empty();
        }
        ItemStack card = Objects.requireNonNull(cardReader.apply(device.get()), "installed card");
        Objects.requireNonNull(operationPermissions, "operationPermissions");
        Permission[] permissions = new Permission[requiredPermissions.length + operationPermissions.length];
        System.arraycopy(requiredPermissions, 0, permissions, 0, requiredPermissions.length);
        System.arraycopy(operationPermissions, 0, permissions, requiredPermissions.length, operationPermissions.length);
        Optional<Network> result = NetworkCardConnection.resolve(player, card, permissions);
        if (result.isEmpty()) {
            return result;
        }
        Network network = result.orElseThrow();
        GlobalPos receiver = card.get(DataComponents.INSTANCE.getNetworkLocation());
        if (sessionNetwork == null) {
            sessionNetwork = network;
            sessionReceiver = receiver;
        } else if (sessionNetwork != network || !Objects.equals(sessionReceiver, receiver)) {
            // A menu watcher belongs to its original storage. Reopen rather than silently switch networks.
            return Optional.empty();
        }
        return result;
    }

    /**
     * force=true is only for removing watchers from the original network during cleanup, as in RS.
     * It must never be used to authorize storage operations and never loads the receiver's chunk.
     */
    @Override
    public Optional<Network> resolveNetwork(boolean force) {
        NetworkCardConnection.requireServerThread(player);
        if (force) {
            return Optional.ofNullable(sessionNetwork);
        }
        return connection();
    }

    @Override
    public boolean isActive() {
        return connection().isPresent();
    }

    /**
     * Returns false without extracting energy when the device cannot pay the full amount.
     * RS's void drainEnergy method can extract a partial amount; mining must pay before changing blocks.
     */
    public boolean tryConsumeEnergy(long amount) {
        NetworkCardConnection.requireServerThread(player);
        if (amount < 0) {
            throw new IllegalArgumentException("Energy cost must not be negative");
        }
        Optional<ItemStack> device = currentDevice();
        if (device.isEmpty()) {
            return false;
        }
        if (!RefinedStorageApi.INSTANCE.isEnergyRequired()) {
            return true;
        }
        Optional<EnergyStorage> energy = RefinedStorageApi.INSTANCE.getEnergyStorage(device.get());
        if (energy.isEmpty() || energy.get().extract(amount, Action.SIMULATE) != amount) {
            return false;
        }
        // Simulation and execution happen together on the server thread, against the same storage.
        long extracted = energy.get().extract(amount, Action.EXECUTE);
        if (extracted != amount) {
            throw new IllegalStateException("Device energy storage changed between simulation and extraction");
        }
        return true;
    }

    @Override
    public void drainEnergy(long amount) {
        NetworkCardConnection.requireServerThread(player);
        if (amount < 0) {
            throw new IllegalArgumentException("Energy cost must not be negative");
        }
        if (currentDevice().isPresent()) {
            deviceContext.drainEnergy(amount);
        }
    }

    private Optional<ItemStack> currentDevice() {
        // A replacement item in the same inventory slot must not inherit the old menu's session.
        return deviceSlot.resolve(player).filter(stack -> stack == sessionDevice && !stack.isEmpty());
    }
}
