package dev.rsadvanced.test;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.locks.LockSupport;

/** The GameTest server exposes a loopback listener only for opt-in client checks. */
public final class ClientValidationServer {
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
            // Give the actual client a nearby block for the normal interaction/open-menu packet path.
            player.serverLevel().setBlockAndUpdate(player.blockPosition().east(2),
                    dev.rsadvanced.feature.anchor.AnchorContent.BLOCK.get().defaultBlockState());
        }));
    }
}
