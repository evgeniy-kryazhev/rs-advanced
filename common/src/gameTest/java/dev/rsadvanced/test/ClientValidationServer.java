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
    private static final Map<UUID, BlockPos> CONTROLLERS = new HashMap<>();
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
            player.serverLevel().setBlockAndUpdate(player.blockPosition().east(2),
                    dev.rsadvanced.feature.anchor.AnchorContent.BLOCK.get().defaultBlockState());
        }));
        TickEvent.SERVER_PRE.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                // Keep the test controller powered throughout the distance checks.
                BlockPos position = CONTROLLERS.get(player.getUUID());
                if (position == null) {
                    continue;
                }
                var controller = player.serverLevel().getBlockEntity(position);
                if (controller instanceof ControllerBlockEntity block) {
                    block.getEnergyStorage().receive(Long.MAX_VALUE,
                            Action.EXECUTE);
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
        CONTROLLERS.put(player.getUUID(), position);
        player.serverLevel().setBlockAndUpdate(position,
                Blocks.INSTANCE.getController()
                        .get(DyeColor.LIGHT_BLUE).defaultBlockState());
    }
}
