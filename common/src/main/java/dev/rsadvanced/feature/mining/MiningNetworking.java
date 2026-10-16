package dev.rsadvanced.feature.mining;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReferenceFactory;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.rsadvanced.config.RSAdvancedConfig;
import dev.rsadvanced.feature.connection.NetworkCardConnection;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

public final class MiningNetworking {
    private static final Map<UUID, Boolean[]> CONNECTIONS = new HashMap<>();
    private static final Set<UUID> SYNCED_SETTINGS = new HashSet<>();
    private MiningNetworking() {
    }

    public static void register() {
        LifecycleEvent.SERVER_BEFORE_START.register(MiningBalance::bindServer);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MiningConfigurationPayload.TYPE,
                MiningConfigurationPayload.CODEC, (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        openConfiguration(player, payload.offhand());
                    }
                }));
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, MiningSelectionPayload.TYPE,
                MiningSelectionPayload.CODEC, (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayer player) {
                        select(player, payload);
                    }
                }));
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(MiningArcPayload.TYPE, MiningArcPayload.CODEC);
            NetworkManager.registerS2CPayloadType(MiningSettingsPayload.TYPE, MiningSettingsPayload.CODEC);
            NetworkManager.registerS2CPayloadType(MiningConnectionPayload.TYPE, MiningConnectionPayload.CODEC);
        }
        PlayerEvent.PLAYER_JOIN.register(player -> player.getServer().execute(() -> syncSettings(player)));
        PlayerEvent.PLAYER_QUIT.register(player -> {
            CONNECTIONS.remove(player.getUUID());
            SYNCED_SETTINGS.remove(player.getUUID());
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            CONNECTIONS.clear();
            SYNCED_SETTINGS.clear();
            MiningBalance.bindServer(null);
        });
        TickEvent.SERVER_POST.register(server -> {
            if (server.getTickCount() % 10 != 0) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                syncSettings(player);
                if (!NetworkManager.canPlayerReceive(player, MiningConnectionPayload.TYPE)) {
                    continue;
                }
                Boolean[] previous = CONNECTIONS.computeIfAbsent(player.getUUID(), id -> new Boolean[2]);
                for (InteractionHand hand : InteractionHand.values()) {
                    var stack = player.getItemInHand(hand);
                    if (!(stack.getItem() instanceof MiningItem)) {
                        previous[hand.ordinal()] = null;
                        continue;
                    }
                    boolean connected = NetworkCardConnection.resolve(player,
                            NetworkCardSlot.card(stack),
                            BuiltinPermission.INSERT).isPresent();
                    if (!Objects.equals(previous[hand.ordinal()], connected)) {
                        NetworkManager.sendToPlayer(player, new MiningConnectionPayload(
                                hand == InteractionHand.OFF_HAND, connected));
                        previous[hand.ordinal()] = connected;
                    }
                }
            }
        });
    }

    private static void syncSettings(ServerPlayer player) {
        // GameTest mock players and clients without the channel cannot receive custom packets.
        // Retrying after login also covers platforms which finish channel negotiation after PLAYER_JOIN.
        if (!SYNCED_SETTINGS.contains(player.getUUID()) && NetworkManager.canPlayerReceive(player, MiningSettingsPayload.TYPE)) {
            NetworkManager.sendToPlayer(player, new MiningSettingsPayload(
                    RSAdvancedConfig.get().mining()));
            SYNCED_SETTINGS.add(player.getUUID());
        }
    }

    private static void openConfiguration(ServerPlayer player, boolean offhand) {
        InteractionHand hand = offhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        if (!(player.getItemInHand(hand).getItem() instanceof MiningItem)) {
            return;
        }
        var reference = RefinedStorageApi.INSTANCE.createInventorySlotReference(player, hand);
        MenuRegistry.openExtendedMenu(player,
                new SimpleMenuProvider(
                        (id, inventory, owner) -> new MiningConfigurationMenu(id, inventory, reference, offhand),
                        Component.translatable("gui.rsadvanced.mining_configuration")),
                buffer -> {
                    var registryBuffer = new RegistryFriendlyByteBuf(buffer, player.registryAccess());
                    SlotReferenceFactory.STREAM_CODEC.encode(registryBuffer, reference);
                    registryBuffer.writeBoolean(offhand);
                });
    }

    public static boolean select(ServerPlayer player, MiningSelectionPayload payload) {
        var stack = payload.slot().resolve(player).orElse(ItemStack.EMPTY);
        if (!(stack.getItem() instanceof MiningItem) || (stack != player.getMainHandItem() && stack != player.getOffhandItem())) {
            return false;
        }
        if (!MiningItem.modules(stack).select(payload.module(), payload.option())) {
            return false;
        }
        player.getInventory().setChanged();
        return true;
    }

    public static void sendArc(ServerLevel level, BlockPos from, BlockPos to) {
        MiningArcPayload payload = new MiningArcPayload(from, to);
        for (ServerPlayer observer : level.players()) {
            if (NetworkManager.canPlayerReceive(observer, MiningArcPayload.TYPE)
                    && observer.distanceToSqr(to.getX() + 0.5, to.getY() + 0.5, to.getZ() + 0.5) <= 4096) {
                NetworkManager.sendToPlayer(observer, payload);
            }
        }
    }
}
