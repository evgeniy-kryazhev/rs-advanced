package dev.rsadvanced.test;

import com.mojang.serialization.JsonOps;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.impl.node.storage.StorageNetworkNode;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.AccessMode;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import dev.rsadvanced.feature.disk.InfiniteResourceStorage;
import dev.rsadvanced.feature.disk.InfiniteSources;


import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertFalse;
import static dev.rsadvanced.test.TestAssertions.assertInstanceOf;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Runs on both loaders, using the actual Disk Drive wrapper chain. */
public class StorageIntegrationTest {
    public static void diskWrappersPreserveInfiniteStockAndOrdinaryStock() {
        for (InfiniteDiskType diskType : InfiniteDiskType.values()) {
            ResourceKey resource = diskType.resource();
            StorageNetworkNode driveNode = new StorageNetworkNode(1, 1, 8);
            driveNode.setActive(true);
            CompositeStorage drive = (CompositeStorage) driveNode.getStorage();

            StorageImpl ordinary = new StorageImpl();
            ordinary.insert(resource, 25, Action.EXECUTE, Actor.EMPTY);
            StateTrackedStorage ordinaryDisk = new StateTrackedStorage(ordinary, null);
            StateTrackedStorage infiniteDisk = new StateTrackedStorage(diskType.create(null, () -> { }), null);
            drive.addSource(ordinaryDisk);
            drive.addSource(infiniteDisk);

            RootStorageImpl network = new RootStorageImpl();
            network.addSource(driveNode.getStorage());
            assertTrue(InfiniteSources.contains(network, resource));

            assertEquals(64, network.extract(resource, 64, Action.SIMULATE, Actor.EMPTY));
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT + 25, network.get(resource));
            assertEquals(64, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(0, ordinary.getStored());
            for (int iteration = 0; iteration < 1_000; iteration++) {
                assertEquals(64, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
            }
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, network.get(resource));

            assertEquals(40, network.insert(resource, 40, Action.EXECUTE, Actor.EMPTY));
            assertEquals(40, ordinary.getStored());
            drive.removeSource(infiniteDisk);
            assertFalse(InfiniteSources.contains(network, resource));
            assertEquals(40, network.get(resource));
            assertEquals(40, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(0, network.get(resource));
        }
    }

    public static void multipleDisksAndAccessChangesUpdateInfinityMetadata() {
        InfiniteDiskType diskType = InfiniteDiskType.COBBLESTONE;
        ResourceKey resource = diskType.resource();
        StorageNetworkNode node = new StorageNetworkNode(1, 1, 8);
        node.setActive(true);
        CompositeStorage drive = (CompositeStorage) node.getStorage();
        StateTrackedStorage first = new StateTrackedStorage(diskType.create(null, () -> { }), null);
        StateTrackedStorage second = new StateTrackedStorage(diskType.create(null, () -> { }), null);
        drive.addSource(first);
        drive.addSource(second);
        RootStorageImpl network = new RootStorageImpl();
        network.addSource(node.getStorage());

        assertEquals(2 * InfiniteResourceStorage.ADVERTISED_AMOUNT, network.get(resource));
        drive.removeSource(first);
        assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, network.get(resource));
        assertTrue(InfiniteSources.contains(network, resource));

        node.getStorageConfiguration().setAccessMode(AccessMode.INSERT);
        assertEquals(0, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
        assertFalse(InfiniteSources.contains(network, resource));
        node.getStorageConfiguration().setAccessMode(AccessMode.INSERT_EXTRACT);
        assertTrue(InfiniteSources.contains(network, resource));
        node.setActive(false);
        assertEquals(0, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
        assertFalse(InfiniteSources.contains(network, resource));
    }

    public static void sourceCodecStoresOnlyItsTypeSpecificEmptyState() {
        for (InfiniteDiskType diskType : InfiniteDiskType.values()) {
            var codec = diskType.getMapCodec(() -> { }).codec();
            var encoded = codec.encodeStart(JsonOps.INSTANCE, diskType.create(null, () -> { })).getOrThrow();
            assertEquals("{}", encoded.toString());
            var decoded = codec.parse(JsonOps.INSTANCE, encoded).getOrThrow();
            assertInstanceOf(InfiniteResourceStorage.class, decoded);
            assertEquals(diskType, decoded.getType());
            assertEquals(64, decoded.extract(diskType.resource(), 64, Action.EXECUTE, Actor.EMPTY));
        }
    }
}

