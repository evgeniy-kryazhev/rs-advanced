package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.energy.EnergyStorage;
import com.refinedmods.refinedstorage.api.network.impl.NetworkImpl;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.security.SecurityPolicy;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.security.PlatformSecurityNetworkComponent;
import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import com.refinedmods.refinedstorage.common.networking.NetworkReceiverBlockEntity;
import com.refinedmods.refinedstorage.common.security.BuiltinPermission;
import com.refinedmods.refinedstorage.common.security.PlatformSecurityNetworkComponentImpl;
import dev.rsadvanced.feature.connection.NetworkCardConnection;
import dev.rsadvanced.feature.connection.NetworkCardItemContext;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertFalse;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class NetworkCardConnectionTest {
    @SharedGameTest("network_card_unavailable_targets")
    public static void rejectsInvalidAndUnloadedTargets(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        assertConnection(false, player, ItemStack.EMPTY);
        ItemStack forgedCard = new ItemStack(Items.PAPER);
        forgedCard.set(DataComponents.INSTANCE.getNetworkLocation(),
                GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(BlockPos.ZERO)));
        assertConnection(false, player, forgedCard);
        ItemStack card = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
        assertConnection(false, player, card);

        ResourceKey<Level> missingDimension = ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath("rsadvanced_test", "missing_dimension"));
        card.set(DataComponents.INSTANCE.getNetworkLocation(), GlobalPos.of(missingDimension, BlockPos.ZERO));
        assertConnection(false, player, card);

        BlockPos unloadedPosition = new BlockPos(25000000, 80, 25000000);
        assertFalse(helper.getLevel().isLoaded(unloadedPosition));
        card.set(DataComponents.INSTANCE.getNetworkLocation(),
                GlobalPos.of(helper.getLevel().dimension(), unloadedPosition));
        assertConnection(false, player, card);
        assertFalse(helper.getLevel().isLoaded(unloadedPosition));

        BlockPos loadedPosition = helper.absolutePos(new BlockPos(0, 4, 0));
        card.set(DataComponents.INSTANCE.getNetworkLocation(),
                GlobalPos.of(helper.getLevel().dimension(), loadedPosition));
        helper.getLevel().setBlockAndUpdate(loadedPosition, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        try {
            assertConnection(false, player, card);
        } finally {
            helper.getLevel().removeBlock(loadedPosition, false);
        }
    }

    @SharedGameTest("network_card_permissions_and_sessions")
    public static void rechecksPermissionsPowerAndSessionIdentity(GameTestHelper helper) {
        try (Fixture fixture = new Fixture(helper)) {
            assertConnection(true, fixture.player, fixture.card);
            fixture.allow(BuiltinPermission.OPEN);
            assertTrue(NetworkCardConnection.resolve(fixture.player, fixture.card, BuiltinPermission.INSERT).isEmpty());
            fixture.allow(BuiltinPermission.OPEN, BuiltinPermission.INSERT);

            ItemStack device = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getWirelessGrid());
            device.set(DataComponents.INSTANCE.getNetworkLocation(), cardLocation(fixture.card));
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, device);
            EnergyStorage energy = RefinedStorageApi.INSTANCE.getEnergyStorage(device).orElseThrow();
            energy.receive(100, Action.EXECUTE);
            AtomicReference<ItemStack> installedCard = new AtomicReference<>(fixture.card);
            var slot = RefinedStorageApi.INSTANCE.createInventorySlotReference(fixture.player, InteractionHand.MAIN_HAND);
            var standardContext = RefinedStorageApi.INSTANCE.getNetworkItemHelper().createContext(device, fixture.player, slot);
            assertTrue(standardContext.isActive());
            // RS's normal route needs a wireless transmitter. Only the card route should bypass its range check.
            assertTrue(standardContext.resolveNetwork().isEmpty());
            NetworkCardItemContext context = new NetworkCardItemContext(fixture.player, slot,
                    stack -> installedCard.get(), BuiltinPermission.OPEN);
            assertTrue(context.isActive());
            assertTrue(context.resolveNetwork().orElseThrow() == fixture.network);
            assertTrue(context.connection(BuiltinPermission.EXTRACT).isEmpty());

            fixture.allow(BuiltinPermission.INSERT);
            assertTrue(context.connection().isEmpty());
            assertTrue(context.resolveNetwork().isEmpty());
            assertTrue(context.resolveNetwork(true).orElseThrow() == fixture.network);
            fixture.allow(BuiltinPermission.OPEN, BuiltinPermission.INSERT);

            assertFalse(context.tryConsumeEnergy(101));
            assertEquals(100, RefinedStorageApi.INSTANCE.getEnergyStorage(device).orElseThrow().getStored());
            assertTrue(context.tryConsumeEnergy(40));
            // RS item-energy adapters snapshot the component when created; reacquire after another adapter writes.
            assertEquals(60, RefinedStorageApi.INSTANCE.getEnergyStorage(device).orElseThrow().getStored());
            context.drainEnergy(60);
            assertTrue(context.connection().isEmpty());
            RefinedStorageApi.INSTANCE.getEnergyStorage(device).orElseThrow().receive(100, Action.EXECUTE);

            installedCard.set(ItemStack.EMPTY);
            assertTrue(context.connection().isEmpty());
            installedCard.set(fixture.card);
            assertTrue(context.isActive());
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, device.copy());
            assertTrue(context.connection().isEmpty());
            fixture.player.setItemInHand(InteractionHand.MAIN_HAND, device);

            fixture.receiverNode.setActive(false);
            assertTrue(context.connection().isEmpty());
            fixture.receiverNode.setActive(true);
            fixture.controller.getEnergyStorage().extract(Long.MAX_VALUE, Action.EXECUTE);
            assertTrue(context.connection().isEmpty());
            fixture.controller.getEnergyStorage().receive(1000, Action.EXECUTE);

            fixture.receiverNode.setNetwork(null);
            assertTrue(context.connection().isEmpty());
            fixture.receiverNode.setNetwork(fixture.network);
            assertTrue(context.isActive());
            // A replacement network cannot inherit a menu watcher registered with the old network.
            Network replacementNetwork = fixture.guardedNetwork();
            fixture.receiverNode.setNetwork(replacementNetwork);
            assertTrue(context.connection().isEmpty());
            assertTrue(context.resolveNetwork(true).orElseThrow() == fixture.network);
            fixture.receiverNode.setNetwork(fixture.network);

            // The permission fixture has a substituted network, not a registered RS graph edge.
            // Detach it before letting RS process the real block removal.
            fixture.receiverNode.setNetwork(null);
            fixture.level.removeBlock(fixture.receiverPosition, false);
            assertTrue(context.connection().isEmpty());
            assertTrue(context.resolveNetwork(true).orElseThrow() == fixture.network);
        }
    }

    @SharedGameTest(value = "network_card_real_cross_dimension", timeoutTicks = 150,
            completion = SharedGameTest.Completion.MANUAL)
    public static void resolvesRealRemoteNetwork(GameTestHelper helper) {
        ServerLevel receiverLevel = helper.getLevel().getServer().getLevel(Level.NETHER);
        if (receiverLevel == null) {
            helper.fail("Nether is unavailable");
            return;
        }
        BlockPos controllerPosition = new BlockPos(52008, 80, 52008);
        BlockPos receiverPosition = controllerPosition.east();
        int chunkX = controllerPosition.getX() >> 4;
        int chunkZ = controllerPosition.getZ() >> 4;
        boolean alreadyForced = receiverLevel.getForcedChunks().contains(new net.minecraft.world.level.ChunkPos(controllerPosition).toLong());
        if (alreadyForced) {
            helper.fail("Cross-dimension test chunk is already forced");
            return;
        }
        receiverLevel.setChunkForced(chunkX, chunkZ, true);
        receiverLevel.setBlockAndUpdate(controllerPosition, Blocks.INSTANCE.getController().get(DyeColor.LIGHT_BLUE).defaultBlockState());
        receiverLevel.setBlockAndUpdate(receiverPosition, Blocks.INSTANCE.getNetworkReceiver().get(DyeColor.LIGHT_BLUE).defaultBlockState());
        ControllerBlockEntity controller = (ControllerBlockEntity) receiverLevel.getBlockEntity(controllerPosition);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setPos(-52008, 80, -52008);
        ItemStack card = boundCard(receiverLevel, receiverPosition);
        Runnable cleanup = () -> {
            receiverLevel.removeBlock(receiverPosition, false);
            receiverLevel.removeBlock(controllerPosition, false);
            receiverLevel.setChunkForced(chunkX, chunkZ, false);
        };
        AtomicBoolean finished = new AtomicBoolean();
        // Register callbacks before ticking: adding entries from inside a callback can invalidate
        // Minecraft 1.21.1's scheduled-callback map iterator.
        for (int tick = 1; tick <= 100; tick++) {
            awaitRemoteNetwork(helper, receiverLevel, controller, player, card, cleanup, finished, tick);
        }
    }

    private static void awaitRemoteNetwork(GameTestHelper helper, ServerLevel receiverLevel,
                                          ControllerBlockEntity controller, ServerPlayer player,
                                          ItemStack card, Runnable cleanup, AtomicBoolean finished, int tick) {
        helper.runAtTickTime(tick, () -> {
            if (finished.get()) {
                return;
            }
            try {
                controller.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE);
                var result = NetworkCardConnection.resolve(player, card, BuiltinPermission.OPEN, BuiltinPermission.INSERT);
                if (result.isPresent()) {
                    assertTrue(player.serverLevel() != receiverLevel);
                    assertTrue(result.orElseThrow() == controller.getNetworkForItem());
                    receiverLevel.removeBlock(card.get(DataComponents.INSTANCE.getNetworkLocation()).pos(), false);
                    assertConnection(false, player, card);
                    finished.set(true);
                    cleanup.run();
                    helper.succeed();
                } else if (tick == 100) {
                    finished.set(true);
                    cleanup.run();
                    helper.fail("Remote network did not become available");
                }
            } catch (RuntimeException | AssertionError failure) {
                finished.set(true);
                cleanup.run();
                helper.fail(failure.toString());
            }
        });
    }

    private static ItemStack boundCard(ServerLevel level, BlockPos position) {
        ItemStack card = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
        card.set(DataComponents.INSTANCE.getNetworkLocation(), GlobalPos.of(level.dimension(), position));
        return card;
    }

    private static GlobalPos cardLocation(ItemStack card) {
        return java.util.Objects.requireNonNull(card.get(DataComponents.INSTANCE.getNetworkLocation()));
    }

    private static void assertConnection(boolean expected, ServerPlayer player, ItemStack card) {
        var network = NetworkCardConnection.resolve(player, card, BuiltinPermission.OPEN);
        assertEquals(expected, network.isPresent());
    }

    /** Real blocks and energy; only security policy is substituted for deterministic permission changes. */
    static final class Fixture implements AutoCloseable {
        final ServerLevel level;
        private final BlockPos controllerPosition;
        private final BlockPos receiverPosition;
        private final ControllerBlockEntity controller;
        final AbstractNetworkNode receiverNode;
        final ServerPlayer player;
        final ItemStack card;
        private final NetworkImpl backingNetwork;
        final Network network;
        private final AtomicReference<PlatformSecurityNetworkComponent> security = new AtomicReference<>();

        Fixture(GameTestHelper helper) {
            this(helper, helper.makeMockServerPlayerInLevel());
        }

        Fixture(GameTestHelper helper, ServerPlayer testPlayer) {
            level = helper.getLevel();
            controllerPosition = helper.absolutePos(new BlockPos(0, 4, 0));
            receiverPosition = controllerPosition.east();
            level.setBlockAndUpdate(controllerPosition, Blocks.INSTANCE.getController().get(DyeColor.LIGHT_BLUE).defaultBlockState());
            level.setBlockAndUpdate(receiverPosition, Blocks.INSTANCE.getNetworkReceiver().get(DyeColor.LIGHT_BLUE).defaultBlockState());
            controller = (ControllerBlockEntity) level.getBlockEntity(controllerPosition);
            var receiver = (NetworkReceiverBlockEntity) level.getBlockEntity(receiverPosition);
            receiverNode = (AbstractNetworkNode) receiver.getContainerProvider().getContainers().iterator().next().getNode();
            player = testPlayer;
            player.setPos(-100000, 80, -100000);
            card = boundCard(level, receiverPosition);
            allow(BuiltinPermission.OPEN, BuiltinPermission.INSERT);
            backingNetwork = new NetworkImpl(RefinedStorageApi.INSTANCE.getNetworkComponentMapFactory());
            var controllerContainer = controller.getContainerProvider().getContainers().iterator().next();
            ((AbstractNetworkNode) controllerContainer.getNode()).setActive(true);
            backingNetwork.addContainer(controllerContainer);
            network = guardedNetwork();
            receiverNode.setNetwork(network);
            receiverNode.setActive(true);
            controller.getEnergyStorage().receive(1000, Action.EXECUTE);
        }

        void allow(BuiltinPermission... permissions) {
            security.set(new PlatformSecurityNetworkComponentImpl(SecurityPolicy.of(permissions)));
        }

        private Network guardedNetwork() {
            return (Network) Proxy.newProxyInstance(Network.class.getClassLoader(), new Class<?>[]{Network.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("getComponent")
                                && arguments[0] == PlatformSecurityNetworkComponent.class) {
                            return security.get();
                        }
                        return method.invoke(backingNetwork, arguments);
                    });
        }

        @Override
        public void close() {
            receiverNode.setNetwork(null);
            backingNetwork.remove();
            level.removeBlock(receiverPosition, false);
            level.removeBlock(controllerPosition, false);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }
}
