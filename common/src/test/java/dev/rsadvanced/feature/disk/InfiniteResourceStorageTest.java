package dev.rsadvanced.feature.disk;

import com.mojang.serialization.MapCodec;
import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceListImpl;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorageImpl;
import com.refinedmods.refinedstorage.common.api.storage.SerializableStorage;
import com.refinedmods.refinedstorage.common.api.storage.StorageType;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InfiniteResourceStorageTest {
    private static final ResourceKey RESOURCE = new TestResource("cobblestone");
    private static final ResourceLocation DEFINITION_ID = ResourceLocation.parse("test:cell");
    private static final StorageType TYPE = new TestStorageType();

    @Test
    void repeatedExtractionAndSimulationLeaveSourceAndCompositeUnchanged() {
        InfiniteResourceStorage source = new InfiniteResourceStorage(RESOURCE, TYPE, DEFINITION_ID);
        CompositeStorageImpl composite = new CompositeStorageImpl(MutableResourceListImpl.create());
        composite.addSource(source);

        for (int iteration = 0; iteration < 10_000; iteration++) {
            assertEquals(64, composite.extract(RESOURCE, 64, Action.EXECUTE, Actor.EMPTY));
        }
        assertEquals(Long.MAX_VALUE, composite.extract(RESOURCE, Long.MAX_VALUE, Action.SIMULATE, Actor.EMPTY));
        assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, composite.getStored());
        assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, composite.getAll().iterator().next().amount());
    }

    @Test
    void multipleSourcesCanBeRemovedWithoutLosingOrdinaryStock() {
        CompositeStorageImpl composite = new CompositeStorageImpl(MutableResourceListImpl.create());
        InfiniteResourceStorage firstSource = new InfiniteResourceStorage(RESOURCE, TYPE, DEFINITION_ID);
        InfiniteResourceStorage secondSource = new InfiniteResourceStorage(RESOURCE, TYPE, DEFINITION_ID);
        StorageImpl ordinary = new StorageImpl(MutableResourceListImpl.create());
        ordinary.insert(RESOURCE, 25, Action.EXECUTE, Actor.EMPTY);
        composite.addSource(firstSource);
        composite.addSource(secondSource);
        composite.addSource(ordinary);

        assertEquals(2 * InfiniteResourceStorage.ADVERTISED_AMOUNT + 25, composite.getStored());
        composite.removeSource(firstSource);
        assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT + 25, composite.getStored());
        composite.removeSource(secondSource);
        assertEquals(25, composite.getAll().iterator().next().amount());
        assertEquals(25, composite.extract(RESOURCE, 64, Action.EXECUTE, Actor.EMPTY));
        assertEquals(0, composite.getStored());
    }

    @Test
    void insertionIsAbsorbedWithoutGrowingSourceOrComposite() {
        CompositeStorageImpl composite = new CompositeStorageImpl(MutableResourceListImpl.create());
        InfiniteResourceStorage source = new InfiniteResourceStorage(RESOURCE, TYPE, DEFINITION_ID);
        StorageImpl ordinary = new StorageImpl(MutableResourceListImpl.create());
        composite.addSource(source);
        composite.addSource(ordinary);

        assertEquals(64, composite.insert(RESOURCE, 64, Action.EXECUTE, Actor.EMPTY));
        assertEquals(0, ordinary.getStored());
        for (Action action : Action.values()) {
            assertEquals(Long.MAX_VALUE, composite.insert(RESOURCE, Long.MAX_VALUE, action, Actor.EMPTY));
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, composite.getStored());
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, composite.getAll().iterator().next().amount());
        }
    }

    @Test
    void acceptsOwnResourceAndRejectsForeignResourcesAndInvalidAmounts() {
        InfiniteResourceStorage source = new InfiniteResourceStorage(RESOURCE, TYPE, DEFINITION_ID);
        for (Action action : Action.values()) {
            assertEquals(0, source.extract(new TestResource("stone"), 64, action, Actor.EMPTY));
            assertEquals(64, source.insert(RESOURCE, 64, action, Actor.EMPTY));
            assertEquals(0, source.insert(new TestResource("stone"), 64, action, Actor.EMPTY));
            assertThrows(IllegalArgumentException.class, () -> source.extract(RESOURCE, 0, action, Actor.EMPTY));
            assertThrows(IllegalArgumentException.class, () -> source.insert(RESOURCE, -1, action, Actor.EMPTY));
        }
    }

    private record TestResource(String name) implements ResourceKey {
    }

    private static final class TestStorageType implements StorageType {
        @Override
        public SerializableStorage create(Long capacity, Runnable listener) {
            return new InfiniteResourceStorage(RESOURCE, this, DEFINITION_ID);
        }

        @Override
        public MapCodec<SerializableStorage> getMapCodec(Runnable listener) {
            return MapCodec.unit(() -> create(null, listener));
        }

        @Override
        public boolean isAllowed(ResourceKey resource) {
            return RESOURCE.equals(resource);
        }

        @Override
        public long getDiskInterfaceTransferQuota(boolean stackUpgrade) {
            return stackUpgrade ? 64 : 1;
        }
    }
}
