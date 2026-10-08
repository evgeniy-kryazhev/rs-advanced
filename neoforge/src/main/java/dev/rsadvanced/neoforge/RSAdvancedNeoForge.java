package dev.rsadvanced.neoforge;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.client.InfiniteGridDisplay;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import dev.rsadvanced.network.InfiniteGridMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(RSAdvanced.MOD_ID)
public final class RSAdvancedNeoForge {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RSAdvanced.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RSAdvanced.MOD_ID);

    private static final DeferredItem<InfiniteDiskItem> COBBLESTONE_DISK =
            ITEMS.register(InfiniteDiskType.COBBLESTONE.itemName(), () -> new InfiniteDiskItem(InfiniteDiskType.COBBLESTONE));
    private static final DeferredItem<InfiniteDiskItem> WATER_DISK =
            ITEMS.register(InfiniteDiskType.WATER.itemName(), () -> new InfiniteDiskItem(InfiniteDiskType.WATER));

    static {
        TABS.register("main", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.rsadvanced"))
                .icon(() -> COBBLESTONE_DISK.get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(COBBLESTONE_DISK.get());
                    output.accept(WATER_DISK.get());
                })
                .build());
    }

    public RSAdvancedNeoForge(IEventBus modBus) {
        ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(RSAdvancedNeoForge::setup);
        modBus.addListener(RSAdvancedNeoForge::registerPayloads);
        NeoForge.EVENT_BUS.addListener(RSAdvancedNeoForge::synchronizeGrid);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> RSAdvanced.initialize(PacketDistributor::sendToPlayer));
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(
                InfiniteResourcesPayload.TYPE,
                InfiniteResourcesPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> InfiniteGridDisplay.receive(payload)));
    }

    private static void synchronizeGrid(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.containerMenu instanceof InfiniteGridMenu menu) {
            menu.rsadvanced$synchronize(player);
        }
    }
}
