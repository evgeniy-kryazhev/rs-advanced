package dev.rsadvanced.feature.mining.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import dev.rsadvanced.feature.mining.MiningArcPayload;
import dev.rsadvanced.feature.mining.MiningBalance;
import dev.rsadvanced.feature.mining.MiningConnectionPayload;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.feature.mining.MiningSettingsPayload;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class MiningClient {
    public static final KeyMapping SELECT_MODE = new KeyMapping("key.rsadvanced.mining_mode", 86, "key.categories.rsadvanced");
    private static final List<Arc> ARCS = new ArrayList<>();
    private static final boolean[] CONNECTED = new boolean[2];
    private record Arc(Vec3 from, Vec3 to, int remaining) {
    }

    private MiningClient() {
    }

    public static void initialize() {
        KeyMappingRegistry.register(SELECT_MODE);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MiningConnectionPayload.TYPE,
                MiningConnectionPayload.CODEC, (payload, context) -> context.queue(() ->
                        CONNECTED[payload.offhand() ? 1 : 0] = payload.connected()));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MiningSettingsPayload.TYPE,
                MiningSettingsPayload.CODEC, (payload, context) -> context.queue(() ->
                        MiningBalance.acceptServerSettings(payload.settings())));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, MiningArcPayload.TYPE,
                MiningArcPayload.CODEC, (payload, context) -> context.queue(() -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.level != null && ARCS.size() < 128) {
                        ARCS.add(new Arc(payload.from().getCenter(), payload.to().getCenter(), 6));
                    }
                }));
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.player == null || minecraft.level == null) {
                ARCS.clear();
                Arrays.fill(CONNECTED, false);
                MiningBalance.clearServerSettings();
                return;
            }
            while (SELECT_MODE.consumeClick()) {
                if (minecraft.screen == null) {
                    InteractionHand hand = minecraft.player.getMainHandItem().getItem() instanceof MiningItem
                            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                    if (minecraft.player.getItemInHand(hand).getItem() instanceof MiningItem) {
                        minecraft.setScreen(new MiningRadialScreen(hand));
                    }
                }
            }
            Iterator<Arc> iterator = ARCS.iterator();
            List<Arc> remaining = new ArrayList<>();
            while (iterator.hasNext()) {
                Arc arc = iterator.next();
                iterator.remove();
                if (minecraft.options.particles().get() != net.minecraft.client.ParticleStatus.MINIMAL) {
                    int count = minecraft.options.particles().get() == net.minecraft.client.ParticleStatus.ALL ? 12 : 4;
                    for (int point = 0; point <= count; point++) {
                        Vec3 location = arc.from().lerp(arc.to(), (double) point / count);
                        minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                                location.x + (minecraft.level.random.nextDouble() - 0.5) * 0.12,
                                location.y + (minecraft.level.random.nextDouble() - 0.5) * 0.12,
                                location.z + (minecraft.level.random.nextDouble() - 0.5) * 0.12, 0, 0, 0);
                    }
                }
                if (arc.remaining() > 1) {
                    remaining.add(new Arc(arc.from(), arc.to(), arc.remaining() - 1));
                }
            }
            ARCS.addAll(remaining);
        });
        ClientGuiEvent.RENDER_HUD.register((graphics, delta) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.options.hideGui) {
                return;
            }
            InteractionHand hand = minecraft.player.getMainHandItem().getItem() instanceof MiningItem
                    ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = minecraft.player.getItemInHand(hand);
            if (!(stack.getItem() instanceof MiningItem)) {
                return;
            }
            MiningMode mode = MiningItem.mode(stack);
            int size = MiningItem.radius(stack) * 2 + 1;
            Component label = Component.translatable("gui.rsadvanced.mining_hud",
                    Component.translatable("gui.rsadvanced.mining_mode." + mode.getSerializedName()),
                    mode == MiningMode.VEIN ? "—" : size + "×" + size, MiningItem.energy(stack),
                    NetworkCardSlot.card(stack).isEmpty()
                            ? Component.translatable("gui.rsadvanced.mining_offline")
                            : Component.translatable(CONNECTED[hand.ordinal()]
                                    ? "gui.rsadvanced.mining_connected" : "gui.rsadvanced.mining_unavailable"));
            graphics.drawString(minecraft.font, label, 8, graphics.guiHeight() - 62, 0xFF55FFFF, true);
        });
    }
}
