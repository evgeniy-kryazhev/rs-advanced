package dev.rsadvanced.client;

import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import dev.architectury.networking.NetworkManager;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.DiskResourceKind;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.rsadvanced.feature.anchor.AnchorStatePayload;
import java.util.Optional;
import net.minecraft.client.Minecraft;

public final class RSAdvancedClient {
    private RSAdvancedClient() {
    }

    public static void initialize() {
        ClientTickEvent.CLIENT_POST.register(AnchorVisualization::tick);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                AnchorStatePayload.TYPE,
                AnchorStatePayload.CODEC,
                (payload, context) -> context.queue(() -> AnchorVisualization.receive(payload)));
        CellDefinitions.setDisplayLookup(definitionId -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!minecraft.isSameThread()) {
                // Integrated-server messages must use the server's session, not client render state.
                return CellDefinitions.serverDefinition(definitionId);
            }
            var connection = minecraft.getConnection();
            return connection == null ? Optional.empty()
                    : CellDefinitions.definition(connection.registryAccess(), definitionId);
        });
        NetworkManager.registerReceiver(
                NetworkManager.Side.S2C,
                InfiniteResourcesPayload.TYPE,
                InfiniteResourcesPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> InfiniteGridDisplay.receive(payload)));
    }

    public static void registerDiskModels() {
        for (DiskResourceKind kind : DiskResourceKind.values()) {
            RefinedStorageClientApi.INSTANCE.registerDiskModel(AdvancedContent.disk(kind).get(),
                    kind.diskModel());
        }
    }
}
