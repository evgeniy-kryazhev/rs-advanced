package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.impl.node.storage.StorageNetworkNode;
import com.refinedmods.refinedstorage.api.network.impl.storage.AbstractConfiguredProxyStorage;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.ParentComposite;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import dev.rsadvanced.feature.disk.InfiniteResourceStorage;
import java.util.Collection;

import static dev.rsadvanced.test.TestAssertions.assertEquals;

/** Rejects full catalog snapshots during operations, independently of timing or machine speed. */
public final class CacheLookupIntegrationTest {
    private CacheLookupIntegrationTest() {
    }

    public static void infiniteTransfersDoNotCopyTheDriveCatalog() {
        TestCell cell = TestCell.named("rsadvanced:cobblestone");
        ResourceKey resource = cell.resource();
        StorageNetworkNode node = new StorageNetworkNode(1, 1, 8);
        node.setActive(true);

        StorageImpl ordinary = new StorageImpl();
        ordinary.insert(resource, 25, Action.EXECUTE, Actor.EMPTY);
        SnapshotGuardComposite drive = new SnapshotGuardComposite();
        drive.addSource(new StateTrackedStorage(ordinary, null));
        StateTrackedStorage infinite = new StateTrackedStorage(cell.create(), null);
        drive.addSource(infinite);

        var configuredDrive = new AbstractConfiguredProxyStorage<CompositeStorageImpl>(
                node.getStorageConfiguration(), drive) {
            @Override
            public void onAddedIntoComposite(ParentComposite parent) {
                drive.onAddedIntoComposite(parent);
            }

            @Override
            public void onRemovedFromComposite(ParentComposite parent) {
                drive.onRemovedFromComposite(parent);
            }
        };
        RootStorageImpl network = new RootStorageImpl();
        network.addSource(configuredDrive);

        // Adding/removing a drive may snapshot its contents; individual transfers must not.
        drive.rejectSnapshots = true;
        assertEquals(64, network.extract(resource, 64, Action.SIMULATE, Actor.EMPTY));
        assertEquals(25, ordinary.getStored());
        assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT + 25, network.get(resource));
        assertEquals(64, network.extract(resource, 64, Action.EXECUTE, Actor.EMPTY));
        assertEquals(0, ordinary.getStored());
        assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, network.get(resource));

        assertEquals(40, network.insert(resource, 40, Action.SIMULATE, Actor.EMPTY));
        assertEquals(0, ordinary.getStored());
        assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, network.get(resource));
        assertEquals(40, network.insert(resource, 40, Action.EXECUTE, Actor.EMPTY));
        assertEquals(40, ordinary.getStored());
        assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT + 40, network.get(resource));

        drive.removeSource(infinite);
        assertEquals(40, network.get(resource));
        drive.rejectSnapshots = false;
        network.removeSource(configuredDrive);
        assertEquals(0, network.get(resource));
    }

    private static final class SnapshotGuardComposite extends CompositeStorageImpl {
        private boolean rejectSnapshots;

        private SnapshotGuardComposite() {
            super(MutableResourceListImpl.create());
        }

        @Override
        public Collection<ResourceAmount> getAll() {
            if (rejectSnapshots) {
                throw new AssertionError("A resource transfer must not snapshot the drive catalog");
            }
            return super.getAll();
        }
    }
}
