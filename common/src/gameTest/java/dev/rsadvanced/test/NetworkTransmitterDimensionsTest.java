package dev.rsadvanced.test;

import com.mojang.authlib.GameProfile;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.Storage;
import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.content.DataComponents;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import com.refinedmods.refinedstorage.common.networking.NetworkReceiverBlockEntity;
import com.refinedmods.refinedstorage.common.networking.NetworkTransmitterBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.anchor.AnchorBlockEntity;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.feature.anchor.AnchorManager;
import dev.rsadvanced.feature.anchor.AnchorStatus;
import dev.rsadvanced.feature.disk.InfiniteStorageBlockContent;
import dev.rsadvanced.feature.disk.InfiniteStorageBlockEntity;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

/** Real RS cross-dimensional topology and anchor tickets, with no player loading either endpoint. */
public final class NetworkTransmitterDimensionsTest {
    @SharedGameTest(value = "transmitter_receiver_cross_dimension_anchors", timeoutTicks = 750,
            completion = SharedGameTest.Completion.MANUAL)
    public static void connectionSurvivesWithoutNearbyPlayers(GameTestHelper helper) {
        new Scenario(helper).start();
    }

    private static final class Scenario {
        // Equal chunk coordinates in different dimensions also exercise dimension-specific ticket ownership.
        private static final BlockPos ORIGIN = new BlockPos(40008, 80, 40008);
        private static final BlockPos ANCHOR = ORIGIN.above();
        private static final BlockPos TRANSMITTER = ORIGIN.east();
        private static final BlockPos STORAGE = ORIGIN.east();
        private static final BlockPos FURNACE = ORIGIN.above(2);
        private static final ChunkPos CHUNK = new ChunkPos(ORIGIN);

        private final GameTestHelper helper;
        private final ServerLevel sourceLevel;
        private final ServerLevel receiverLevel;
        private ControllerBlockEntity controller;
        private boolean sourceForced;
        private boolean receiverForced;
        private boolean finished;

        private Scenario(GameTestHelper helper) {
            this.helper = helper;
            sourceLevel = helper.getLevel();
            receiverLevel = sourceLevel.getServer().getLevel(Level.NETHER);
            require(sourceLevel.dimension() == Level.OVERWORLD, "Test requires the Overworld source");
            require(receiverLevel != null, "Nether is unavailable");
        }

        private void start() {
            try {
                require(!sourceLevel.getForcedChunks().contains(CHUNK.toLong()), "Source chunk already forced");
                require(!receiverLevel.getForcedChunks().contains(CHUNK.toLong()), "Receiver chunk already forced");
                sourceLevel.setChunkForced(CHUNK.x, CHUNK.z, true);
                sourceForced = true;
                receiverLevel.setChunkForced(CHUNK.x, CHUNK.z, true);
                receiverForced = true;
                clearBlocks();
                sourceLevel.setBlockAndUpdate(ORIGIN,
                        Blocks.INSTANCE.getController().get(DyeColor.LIGHT_BLUE).defaultBlockState());
                sourceLevel.setBlockAndUpdate(ANCHOR, AnchorContent.BLOCK.get().defaultBlockState());
                sourceLevel.setBlockAndUpdate(TRANSMITTER,
                        Blocks.INSTANCE.getNetworkTransmitter().get(DyeColor.LIGHT_BLUE).defaultBlockState());
                receiverLevel.setBlockAndUpdate(ORIGIN,
                        Blocks.INSTANCE.getNetworkReceiver().get(DyeColor.LIGHT_BLUE).defaultBlockState());
                receiverLevel.setBlockAndUpdate(ANCHOR, AnchorContent.BLOCK.get().defaultBlockState());
                receiverLevel.setBlockAndUpdate(STORAGE, InfiniteStorageBlockContent.ITEM_BLOCK.get().defaultBlockState());
                var remoteStorage = (InfiniteStorageBlockEntity) receiverLevel.getBlockEntity(STORAGE);
                remoteStorage.setDefinition(RSAdvanced.id("cobblestone"));
                controller = (ControllerBlockEntity) sourceLevel.getBlockEntity(ORIGIN);
                bindTransmitter();
                refillPower();
                schedule(1, () -> awaitConnection(1));
            } catch (RuntimeException | AssertionError failure) {
                cleanup();
                throw failure;
            }
        }

        private void awaitConnection(int tick) {
            refillPower();
            var transmitter = (NetworkTransmitterBlockEntity) sourceLevel.getBlockEntity(TRANSMITTER);
            var storage = (InfiniteStorageBlockEntity) receiverLevel.getBlockEntity(STORAGE);
            var sourceAnchor = (AnchorBlockEntity) sourceLevel.getBlockEntity(ANCHOR);
            var receiverAnchor = (AnchorBlockEntity) receiverLevel.getBlockEntity(ANCHOR);
            boolean ready = transmitter.getMenuData().transmitting() && storage.node().isActive()
                    && sourceAnchor.status() == AnchorStatus.ACTIVE && receiverAnchor.status() == AnchorStatus.ACTIVE;
            if (!ready) {
                require(tick < 250, "Connection bootstrap timed out: status=" + transmitter.getMenuData()
                        + ", storage active=" + storage.node().isActive() + ", anchors="
                        + sourceAnchor.status() + "/" + receiverAnchor.status());
                // RS retries unreachable receivers on wall-clock time. Pace only bootstrap at normal TPS,
                // without replacing its rate limiter or manually updating network nodes.
                LockSupport.parkNanos(50_000_000L);
                schedule(tick + 1, () -> awaitConnection(tick + 1));
                return;
            }
            releaseBootstrapTickets();
            // The default RS controller holds only 1000 FE. Supply every tick so the two anchors
            // and transmitter cannot exhaust it between checkpoints and invalidate the loading test.
            for (int checkpoint = tick + 1; checkpoint <= tick + 410; checkpoint++) {
                schedule(checkpoint, () -> {
                    checkConnectedAndHeld();
                    refillPower();
                });
            }
            schedule(tick + 411, () -> {
                checkConnectedAndHeld();
                verifyFurnace(sourceLevel);
                verifyFurnace(receiverLevel);
                cleanup();
                helper.succeed();
            });
        }

        private void bindTransmitter() {
            var transmitter = (NetworkTransmitterBlockEntity) sourceLevel.getBlockEntity(TRANSMITTER);
            var card = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
            card.set(DataComponents.INSTANCE.getNetworkLocation(), GlobalPos.of(receiverLevel.dimension(), ORIGIN));
            // A menu owner is sufficient for public inventory access; never add this player to either world.
            var owner = new ServerPlayer(sourceLevel.getServer(), sourceLevel,
                    new GameProfile(UUID.randomUUID(), "transmitter-card-setup"), ClientInformation.createDefault());
            var menu = transmitter.createMenu(0, owner.getInventory(), owner);
            require(menu != null, "Transmitter menu unavailable");
            menu.slots.getLast().set(card);
        }

        private void releaseBootstrapTickets() {
            checkNetwork();
            verifyAnchor(sourceLevel);
            verifyAnchor(receiverLevel);
            refillPower();
            sourceLevel.setChunkForced(CHUNK.x, CHUNK.z, false);
            sourceForced = false;
            receiverLevel.setChunkForced(CHUNK.x, CHUNK.z, false);
            receiverForced = false;
            prepareFurnace(sourceLevel);
            prepareFurnace(receiverLevel);
        }

        private void checkConnectedAndHeld() {
            for (ServerLevel level : List.of(sourceLevel, receiverLevel)) {
                require(!level.getForcedChunks().contains(CHUNK.toLong()), "Bootstrap force remains in " + level.dimension());
                require(level.getChunkSource().getChunkNow(CHUNK.x, CHUNK.z) != null,
                        "Anchor chunk unloaded in " + level.dimension());
                require(level.shouldTickBlocksAt(ORIGIN), "Anchor chunk is not ticking in " + level.dimension());
                for (ServerPlayer player : level.players()) {
                    require(player.distanceToSqr(ORIGIN.getCenter()) > 128 * 128,
                            "Player is loading endpoint in " + level.dimension());
                }
                verifyAnchor(level);
            }
            checkNetwork();
        }

        private void verifyAnchor(ServerLevel level) {
            var anchor = (AnchorBlockEntity) level.getBlockEntity(ANCHOR);
            require(anchor != null && anchor.status() == AnchorStatus.ACTIVE,
                    "Anchor inactive in " + level.dimension() + ": " + (anchor == null ? "missing" : anchor.status()));
            require(anchor.heldSize() == 1 && AnchorManager.chunks(anchor).contains(CHUNK.toLong()),
                    "Anchor does not own its dimension's endpoint chunk: " + level.dimension());
        }

        private void checkNetwork() {
            var transmitter = (NetworkTransmitterBlockEntity) sourceLevel.getBlockEntity(TRANSMITTER);
            var receiver = (NetworkReceiverBlockEntity) receiverLevel.getBlockEntity(ORIGIN);
            Network network = network(controller);
            require(network != null && network(transmitter) == network && network(receiver) == network,
                    "Transmitter and receiver are not in the powered controller network: controller=" + network
                            + ", transmitter=" + network(transmitter) + ", receiver=" + network(receiver)
                            + ", status=" + (transmitter == null ? "missing" : transmitter.getMenuData())
                            + ", source ticking=" + sourceLevel.shouldTickBlocksAt(ORIGIN)
                            + ", receiver ticking=" + receiverLevel.shouldTickBlocksAt(ORIGIN));
            require(transmitter.getMenuData().transmitting(), "Transmitter reports no cross-dimensional connection");
            Storage storage = (Storage) network.getComponent(StorageNetworkComponent.class);
            require(storage.extract(TestCell.named("rsadvanced:cobblestone").resource(), 64, Action.EXECUTE, Actor.EMPTY) == 64,
                    "Cannot extract from the remote Nether storage through the source network");
        }

        private static Network network(AbstractBaseNetworkNodeContainerBlockEntity<?> block) {
            if (block == null) {
                return null;
            }
            return block.getContainerProvider().getContainers().iterator().next().getNode().getNetwork();
        }

        private static void prepareFurnace(ServerLevel level) {
            level.setBlockAndUpdate(FURNACE, net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());
            var furnace = (FurnaceBlockEntity) level.getBlockEntity(FURNACE);
            furnace.setItem(0, new ItemStack(Items.IRON_ORE, 2));
            furnace.setItem(1, new ItemStack(Items.COAL));
        }

        private static void verifyFurnace(ServerLevel level) {
            var furnace = (FurnaceBlockEntity) level.getBlockEntity(FURNACE);
            require(furnace != null && furnace.getItem(2).is(Items.IRON_INGOT) && furnace.getItem(2).getCount() == 2,
                    "Furnace failed to smelt twice without players in " + level.dimension());
        }

        private void refillPower() {
            // Supply energy without supplying chunk tickets or ticking the RS nodes manually.
            controller.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE);
        }

        private void schedule(long tick, Runnable action) {
            helper.runAtTickTime(tick, () -> {
                if (finished) {
                    return;
                }
                try {
                    action.run();
                } catch (RuntimeException | AssertionError failure) {
                    cleanup();
                    // Scheduled callbacks must use GameTest's assertion type so failures become reports,
                    // rather than an uncaught Error terminating the dedicated server.
                    helper.fail(failure.toString());
                }
            });
        }

        private void cleanup() {
            finished = true;
            clearBlocks();
            if (sourceForced) {
                sourceLevel.setChunkForced(CHUNK.x, CHUNK.z, false);
                sourceForced = false;
            }
            if (receiverForced) {
                receiverLevel.setChunkForced(CHUNK.x, CHUNK.z, false);
                receiverForced = false;
            }
        }

        private void clearBlocks() {
            for (ServerLevel level : List.of(sourceLevel, receiverLevel)) {
                for (BlockPos position : List.of(FURNACE, ANCHOR, ORIGIN.east(), ORIGIN)) {
                    level.removeBlock(position, false);
                }
            }
        }

        private static void require(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }
    }
}
