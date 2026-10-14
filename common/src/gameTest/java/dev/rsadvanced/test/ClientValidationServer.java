package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.GameType;

/** The GameTest server exposes a loopback listener only for opt-in client checks. */
public final class ClientValidationServer {
    private static final Map<UUID, java.util.List<BlockPos>> CONTROLLERS = new HashMap<>();
    public static Runnable completed = () -> { };

    public static void register() {
        if (!Boolean.getBoolean("rsadvanced.test.clientServer")) {
            return;
        }
        // GameTest normally runs ticks without waiting. Real login timeouts need normal wall-clock pacing.
        TickEvent.SERVER_PRE.register(server -> LockSupport.parkNanos(50_000_000L));
        LifecycleEvent.SERVER_STARTED.register(server -> {
            server.setUsesAuthentication(false);
            try {
                server.getConnection().startTcpServerListener(InetAddress.getLoopbackAddress(),
                        Integer.getInteger("rsadvanced.test.port", 25575));
            } catch (IOException exception) {
                throw new IllegalStateException("Cannot bind local client validation server", exception);
            }
        });
        PlayerEvent.PLAYER_QUIT.register(player -> player.getServer().execute(() -> completed.run()));
        PlayerEvent.PLAYER_JOIN.register(player -> player.getServer().execute(() -> {
            serverPlayerSetup(player);
            // Give the actual client a nearby block for the normal interaction/open-menu packet path.
            var anchorPosition = player.blockPosition().east(2);
            // A previous run leaves the distance-check cable line in this persistent test world.
            // Remove it before measuring the initial area so adding the line grows the network again.
            for (int offset = 0; offset <= 17; offset++) {
                player.serverLevel().removeBlock(anchorPosition.east(offset), false);
            }
            player.serverLevel().setBlockAndUpdate(anchorPosition,
                    dev.rsadvanced.feature.anchor.AnchorContent.BLOCK.get().defaultBlockState());
            var storagePosition = player.blockPosition().west(2);
            player.serverLevel().setBlockAndUpdate(storagePosition.below(),
                    Blocks.INSTANCE.getController().get(DyeColor.LIGHT_BLUE).defaultBlockState());
            var storageController = (ControllerBlockEntity) player.serverLevel().getBlockEntity(storagePosition.below());
            storageController.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE);
            CONTROLLERS.get(player.getUUID()).add(storagePosition.below());
            player.serverLevel().setBlockAndUpdate(storagePosition,
                    dev.rsadvanced.feature.disk.InfiniteStorageBlockContent.ITEM_BLOCK.get().defaultBlockState());
            var itemBlock = (dev.rsadvanced.feature.disk.InfiniteStorageBlockEntity)
                    player.serverLevel().getBlockEntity(storagePosition);
            itemBlock.setDefinition(dev.rsadvanced.RSAdvanced.id("cobblestone"));
            player.serverLevel().setBlockAndUpdate(storagePosition.west(),
                    dev.rsadvanced.feature.disk.InfiniteStorageBlockContent.FLUID_BLOCK.get().defaultBlockState());
            var fluidBlock = (dev.rsadvanced.feature.disk.InfiniteStorageBlockEntity)
                    player.serverLevel().getBlockEntity(storagePosition.west());
            fluidBlock.setDefinition(dev.rsadvanced.RSAdvanced.id("water"));
            var receiverPosition = storagePosition.above();
            player.serverLevel().setBlockAndUpdate(receiverPosition,
                    Blocks.INSTANCE.getNetworkReceiver().get(DyeColor.LIGHT_BLUE).defaultBlockState());
            player.getInventory().setItem(7, dev.rsadvanced.feature.terminal.TerminalContent.ITEM.get().createAtEnergyCapacity());
            var card = new net.minecraft.world.item.ItemStack(
                    com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
            card.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getNetworkLocation(),
                    net.minecraft.core.GlobalPos.of(player.level().dimension(), receiverPosition));
            player.getInventory().setItem(8, card);
            player.getInventory().setItem(6, dev.rsadvanced.feature.mining.MiningContent.ITEM.get().createAtEnergyCapacity());
        }));
        TickEvent.SERVER_PRE.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                // Both fixtures need continuous power; the terminal tests use the storage network.
                var positions = CONTROLLERS.get(player.getUUID());
                if (positions == null) {
                    continue;
                }
                for (BlockPos position : positions) {
                    var controller = player.serverLevel().getBlockEntity(position);
                    if (controller instanceof ControllerBlockEntity block) {
                        block.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE);
                    }
                }
            }
        });
    }

    private static void serverPlayerSetup(ServerPlayer player) {
        // GameTestServer's default operator level is insufficient for teleport/setblock commands.
        player.getServer().getPlayerList().getOps().add(new ServerOpListEntry(player.getGameProfile(), 4, true));
        player.getServer().getCommands().sendCommands(player);
        player.setGameMode(GameType.CREATIVE);
        var position = player.blockPosition().east(2).below();
        CONTROLLERS.put(player.getUUID(), new java.util.ArrayList<>(java.util.List.of(position)));
        player.serverLevel().setBlockAndUpdate(position,
                Blocks.INSTANCE.getController()
                        .get(DyeColor.LIGHT_BLUE).defaultBlockState());
    }
}
