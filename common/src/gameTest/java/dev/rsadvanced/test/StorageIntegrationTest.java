package dev.rsadvanced.test;

import com.mojang.serialization.JsonOps;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.impl.node.storage.StorageNetworkNode;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.filter.FilterMode;
import com.refinedmods.refinedstorage.api.storage.AccessMode;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import com.refinedmods.refinedstorage.api.storage.limited.LimitedStorageImpl;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import dev.rsadvanced.feature.disk.InfiniteResourceStorage;
import dev.rsadvanced.feature.disk.InfiniteSources;
import java.util.Set;


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

    public static void returnedResourcesAreAbsorbedWithoutCacheGrowth() {
        for (InfiniteDiskType diskType : InfiniteDiskType.values()) {
            ResourceKey resource = diskType.resource();
            StorageNetworkNode node = new StorageNetworkNode(1, 1, 8);
            node.setActive(true);
            CompositeStorage drive = (CompositeStorage) node.getStorage();
            StateTrackedStorage disk = new StateTrackedStorage(diskType.create(null, () -> { }), null);
            drive.addSource(disk);
            RootStorageImpl network = new RootStorageImpl();
            network.addSource(node.getStorage());

            for (Action action : Action.values()) {
                assertEquals(Long.MAX_VALUE, network.insert(resource, Long.MAX_VALUE, action, Actor.EMPTY));
                assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, network.get(resource));
            }
            for (int iteration = 0; iteration < 1_000; iteration++) {
                assertEquals(64, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
                assertEquals(64, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
            }
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, network.get(resource));

            var configuration = node.getStorageConfiguration();
            configuration.setAccessMode(AccessMode.EXTRACT);
            assertEquals(0, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
            configuration.setAccessMode(AccessMode.INSERT);
            assertEquals(64, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(0, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
            configuration.setAccessMode(AccessMode.INSERT_EXTRACT);
            configuration.setFilterMode(FilterMode.BLOCK);
            configuration.setFilters(Set.of(resource));
            assertEquals(0, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
            configuration.setFilterMode(FilterMode.ALLOW);
            assertEquals(64, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
            configuration.setFilters(Set.of());
            configuration.setFilterMode(FilterMode.BLOCK);
            node.setActive(false);
            assertEquals(0, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
            node.setActive(true);
            drive.removeSource(disk);
            assertEquals(0, network.get(resource));
        }
    }

    public static void insertionPreservesFiniteStockPrioritiesAndVoidExcess() {
        for (InfiniteDiskType diskType : InfiniteDiskType.values()) {
            ResourceKey resource = diskType.resource();
            ResourceKey foreignResource = java.util.Arrays.stream(InfiniteDiskType.values())
                    .filter(candidate -> !candidate.resource().equals(resource))
                    .findFirst().orElseThrow().resource();
            StorageNetworkNode node = new StorageNetworkNode(1, 1, 8);
            node.setActive(true);
            CompositeStorage drive = (CompositeStorage) node.getStorage();
            LimitedStorageImpl ordinary = new LimitedStorageImpl(10);
            drive.addSource(new StateTrackedStorage(ordinary, null));
            StateTrackedStorage infinite = new StateTrackedStorage(diskType.create(null, () -> { }), null);
            drive.addSource(infinite);
            RootStorageImpl network = new RootStorageImpl();
            network.addSource(node.getStorage());

            assertEquals(64, network.insert(resource, 64, Action.SIMULATE, Actor.EMPTY));
            assertEquals(0, ordinary.getStored());
            assertEquals(64, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(10, ordinary.getStored());
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT + 10, network.get(resource));
            assertEquals(0, network.insert(foreignResource, 64, Action.EXECUTE, Actor.EMPTY));
            node.getStorageConfiguration().setVoidExcess(true);
            node.getStorageConfiguration().setFilterMode(FilterMode.ALLOW);
            node.getStorageConfiguration().setFilters(Set.of(resource, foreignResource));
            assertEquals(64, network.insert(foreignResource, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(0, network.get(foreignResource));
            node.getStorageConfiguration().setVoidExcess(false);
            node.getStorageConfiguration().setFilters(Set.of());
            node.getStorageConfiguration().setFilterMode(FilterMode.BLOCK);

            drive.removeSource(infinite);
            assertEquals(10, network.get(resource));

            StorageNetworkNode infiniteNode = new StorageNetworkNode(1, 1, 8);
            infiniteNode.setActive(true);
            ((CompositeStorage) infiniteNode.getStorage()).addSource(infinite);
            infiniteNode.getStorageConfiguration().setInsertPriority(100);
            network.addSource(infiniteNode.getStorage());
            assertEquals(10, network.extract(resource, 10, Action.EXECUTE, Actor.EMPTY));
            assertEquals(64, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(0, ordinary.getStored());
            node.getStorageConfiguration().setInsertPriority(200);
            network.sortSources();
            assertEquals(64, network.insert(resource, 64, Action.EXECUTE, Actor.EMPTY));
            assertEquals(10, ordinary.getStored());
            network.removeSource(infiniteNode.getStorage());
            assertEquals(10, network.get(resource));
        }
    }
}

