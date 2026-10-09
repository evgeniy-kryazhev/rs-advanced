package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.common.content.Blocks;
import com.refinedmods.refinedstorage.common.controller.ControllerBlockEntity;
import dev.architectury.event.events.common.TickEvent;
import dev.rsadvanced.feature.anchor.AnchorBlockEntity;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.feature.anchor.AnchorSavedData;
import dev.rsadvanced.feature.anchor.AnchorStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;

/** Opt-in dedicated-server harness. Seed/check/empty run in separate JVMs against the same saved world. */
public final class AnchorRestartScenario {
    private static final BlockPos CONTROLLER = new BlockPos(10014, 80, 10008);
    private static final BlockPos ANCHOR = CONTROLLER.above();
    private static final BlockPos OUTPUT = ANCHOR.east(3).above();
    private static final BlockPos INPUT = ANCHOR.east(3).below();
    private static int ticks;
    private static boolean completed;
    private static long furnacesBefore;

    private AnchorRestartScenario() {
    }

    public static void register() {
        String phase = System.getProperty("rsadvanced.test.anchorRestart");
        if (phase == null) {
            return;
        }
        TickEvent.SERVER_POST.register(server -> {
            if (completed) {
                return;
            }
            ticks++;
            try {
                var level = server.overworld();
                if (phase.equals("seed") || phase.equals("norandom")) {
                    if (ticks == 5) {
                        level.setChunkForced(CONTROLLER.getX() >> 4, CONTROLLER.getZ() >> 4, true);
                        for (BlockPos position : java.util.List.of(CONTROLLER, ANCHOR, ANCHOR.above(), ANCHOR.east(),
                                ANCHOR.east(2), ANCHOR.east(2).above(), ANCHOR.east(2).below(), ANCHOR.above(2), INPUT, OUTPUT)) {
                            level.removeBlock(position, false);
                        }
                        level.setBlockAndUpdate(CONTROLLER,
                                Blocks.INSTANCE.getController().get(DyeColor.LIGHT_BLUE).defaultBlockState());
                        level.setBlockAndUpdate(ANCHOR, AnchorContent.BLOCK.get().defaultBlockState());
                        level.setBlockAndUpdate(ANCHOR.east(),
                                Blocks.INSTANCE.getCable().get(DyeColor.LIGHT_BLUE).defaultBlockState());
                        level.setBlockAndUpdate(ANCHOR.east(2),
                                Blocks.INSTANCE.getCable().get(DyeColor.LIGHT_BLUE).defaultBlockState());
                        level.setBlockAndUpdate(ANCHOR.above(), Blocks.INSTANCE.getDiskDrive().defaultBlockState());
                        level.setBlockAndUpdate(ANCHOR.above(2), Blocks.INSTANCE.getAutocrafter()
                                .get(DyeColor.LIGHT_BLUE).defaultBlockState());
                        AnchorAutocraftingCheck
                                .configure(level, ANCHOR.above(2));
                        level.setBlockAndUpdate(ANCHOR.east(2).above(), Blocks.INSTANCE.getExporter()
                                .get(DyeColor.LIGHT_BLUE).rotated(net.minecraft.core.Direction.EAST));
                        var exporter = (com.refinedmods.refinedstorage.common.exporter.AbstractExporterBlockEntity)
                                level.getBlockEntity(ANCHOR.east(2).above());
                        var filter = com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl.createForFilter();
                        filter.change(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE), false);
                        var configuration = new net.minecraft.nbt.CompoundTag();
                        configuration.put("rf", filter.toTag(server.registryAccess()));
                        exporter.readConfiguration(configuration, server.registryAccess());
                        level.setBlockAndUpdate(ANCHOR.east(2).below(), Blocks.INSTANCE.getImporter()
                                .get(DyeColor.LIGHT_BLUE).rotated(net.minecraft.core.Direction.EAST));
                        level.setBlockAndUpdate(OUTPUT, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
                        level.setBlockAndUpdate(INPUT, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
                        var input = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(INPUT);
                        input.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 8));
                    }
                    if (ticks == 30) {
                        level.setChunkForced(CONTROLLER.getX() >> 4, CONTROLLER.getZ() >> 4, false);
                        // Insert after graph initialization so RS publishes the storage change to the network.
                        var drive = (com.refinedmods.refinedstorage.common.storage.diskdrive.AbstractDiskDriveBlockEntity)
                                level.getBlockEntity(ANCHOR.above());
                        drive.getDiskInventory().setItem(0, dev.rsadvanced.content.AdvancedContent.cell(
                                dev.rsadvanced.RSAdvanced.id("cobblestone"), dev.rsadvanced.feature.disk.DiskResourceKind.ITEM));
                        var finiteDisk = new net.minecraft.world.item.ItemStack(
                                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                                        net.minecraft.resources.ResourceLocation.parse("refinedstorage:1k_storage_disk")));
                        var setupPlayer = new net.minecraft.server.level.ServerPlayer(server, level,
                                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "anchor-disk-setup"),
                                net.minecraft.server.level.ClientInformation.createDefault());
                        // Ordinary RS disks acquire their repository ID in a player's inventory.
                        // This setup player is never added to the world.
                        finiteDisk.getItem().inventoryTick(finiteDisk, level, setupPlayer, 0, false);
                        drive.getDiskInventory().setItem(1, finiteDisk);
                    }
                    if (ticks >= 5) {
                        ControllerBlockEntity controller = (ControllerBlockEntity) level.getBlockEntity(CONTROLLER);
                        controller.getEnergyStorage().receive(Long.MAX_VALUE, Action.EXECUTE);
                    }
                    if (ticks == 310) {
                        resetAutomation(level);
                        furnacesBefore = AnchorAutocraftingCheck
                                .start(level, ANCHOR.above(2));
                    }
                    if (ticks == 420) {
                        AnchorBlockEntity anchor = (AnchorBlockEntity) level.getBlockEntity(ANCHOR);
                        TestAssertions.assertEquals(AnchorStatus.ACTIVE, anchor.status());
                        TestAssertions.assertEquals(2, anchor.areaSize());
                        TestAssertions.assertEquals(1, AnchorSavedData.get(server).snapshots().size());
                        assertAutomation(level);
                        AnchorAutocraftingCheck
                                .verify(level, ANCHOR.above(2), furnacesBefore);
                        TestAssertions.assertEquals(phase.equals("norandom") ? 0 : 1,
                                dev.rsadvanced.feature.anchor.AnchorManager.chunkTickCount(level,
                                        new net.minecraft.world.level.ChunkPos(ANCHOR).toLong()));
                        if (phase.equals("seed")) {
                            var snapshots = new java.util.HashMap<>(AnchorSavedData.get(server).snapshots());
                            snapshots.put(java.util.UUID.randomUUID(), new AnchorSavedData.Snapshot(level.dimension(),
                                    ANCHOR, java.util.Set.of(new net.minecraft.world.level.ChunkPos(ANCHOR).toLong())));
                            snapshots.put(java.util.UUID.randomUUID(), new AnchorSavedData.Snapshot(
                                    net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                                            dev.rsadvanced.RSAdvanced.id("missing_dimension")), ANCHOR, java.util.Set.of()));
                            AnchorSavedData.get(server).replace(snapshots);
                        }
                        finish(server, phase, "PASSED: paid two-chunk area survived 420 ticks without players");
                    }
                } else if (phase.equals("check")) {
                    // getChunkNow never loads the base; only the recovered tickets may bring it back.
                    if (ticks == 110 || ticks == 330) {
                        TestAssertions.assertTrue(level.getChunkSource().getChunkNow(ANCHOR.getX() >> 4,
                                ANCHOR.getZ() >> 4) != null);
                        AnchorBlockEntity anchor = (AnchorBlockEntity) level.getBlockEntity(ANCHOR);
                        TestAssertions.assertEquals(AnchorStatus.ACTIVE, anchor.status());
                        TestAssertions.assertEquals(2, anchor.heldSize());
                        TestAssertions.assertEquals(1, AnchorSavedData.get(server).snapshots().size());
                    }
                    if (ticks == 200) {
                        resetAutomation(level);
                    }
                    if (ticks == 310) {
                        furnacesBefore = AnchorAutocraftingCheck
                                .start(level, ANCHOR.above(2));
                    }
                    if (ticks == 330) {
                        assertAutomation(level);
                        AnchorAutocraftingCheck
                                .verify(level, ANCHOR.above(2), furnacesBefore);
                        ControllerBlockEntity controller = (ControllerBlockEntity) level.getBlockEntity(CONTROLLER);
                        controller.getEnergyStorage().extract(Long.MAX_VALUE, Action.EXECUTE);
                    }
                    if (ticks == 331) {
                        AnchorBlockEntity anchor = (AnchorBlockEntity) level.getBlockEntity(ANCHOR);
                        TestAssertions.assertEquals(AnchorStatus.NO_ENERGY, anchor.status());
                        TestAssertions.assertTrue(AnchorSavedData.get(server).snapshots().isEmpty());
                        finish(server, phase, "PASSED: recovered without visiting base; power loss cleared saved area");
                    }
                } else if (phase.equals("empty")) {
                    if (ticks == 120) {
                        TestAssertions.assertTrue(AnchorSavedData.get(server).snapshots().isEmpty());
                        TestAssertions.assertTrue(level.getChunkSource().getChunkNow(ANCHOR.getX() >> 4,
                                ANCHOR.getZ() >> 4) == null);
                        finish(server, phase, "PASSED: stopped area was not recovered after another restart");
                    }
                } else {
                    throw new IllegalArgumentException("Unknown restart phase: " + phase);
                }
            } catch (Exception | AssertionError exception) {
                org.slf4j.LoggerFactory.getLogger("rsadvanced-anchor-check").error("Restart phase failed: " + phase, exception);
                finish(server, phase, "FAILED: " + exception);
            }
        });
    }

    private static void resetAutomation(net.minecraft.server.level.ServerLevel level) {
        var input = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(INPUT);
        var output = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(OUTPUT);
        output.clearContent();
        input.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 8));
    }

    private static void assertAutomation(net.minecraft.server.level.ServerLevel level) {
        var input = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(INPUT);
        var output = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(OUTPUT);
        if (!input.isEmpty() || output.isEmpty()) {
            var exporter = (com.refinedmods.refinedstorage.common.api.support.network.AbstractNetworkNodeContainerBlockEntity<?>)
                    level.getBlockEntity(ANCHOR.east(2).above());
            var node = (com.refinedmods.refinedstorage.api.network.impl.node.exporter.ExporterNetworkNode)
                    exporter.getContainerProvider().getContainers().iterator().next().getNode();
            throw new AssertionError("Automation: input=" + input.getItem(0) + ", output=" + output.getItem(0)
                    + ", exporter active=" + node.isActive() + ", last result=" + node.getLastResult(0)
                    + ", available cobble=" + node.getNetwork().getComponent(
                            com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent.class)
                    .get(com.refinedmods.refinedstorage.common.support.resource.ItemResource.ofItemStack(
                            new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE))));
        }
    }

    private static void finish(net.minecraft.server.MinecraftServer server, String phase, String result) {
        completed = true;
        try {
            Path report = Path.of(System.getProperty("rsadvanced.test.anchorReport"));
            Files.createDirectories(report.getParent());
            Files.writeString(report, result + System.lineSeparator());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot write restart report: " + phase, exception);
        }
        Thread shutdownMonitor = new Thread(() -> {
            try {
                server.getRunningThread().join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
            // Architectury's development watcher can keep the JVM alive after normal shutdown.
            // Exit only after the server thread has finished saving every dimension.
            System.exit(0);
        }, "anchor-validation-shutdown");
        shutdownMonitor.setDaemon(true);
        shutdownMonitor.start();
        server.halt(false);
    }
}
