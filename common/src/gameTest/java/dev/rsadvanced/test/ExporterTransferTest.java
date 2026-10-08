package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.impl.node.storage.StorageNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.exporter.ExporterTransferStrategy;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.StorageImpl;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.exporter.ExporterTransferStrategyFactory;
import com.refinedmods.refinedstorage.common.api.upgrade.UpgradeItem;
import com.refinedmods.refinedstorage.common.api.upgrade.UpgradeState;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import dev.rsadvanced.feature.disk.InfiniteResourceStorage;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Uses RS's registered loader strategy and a real chest capability/transaction. */
public final class ExporterTransferTest {
    public static void exporterRespectsQuotaAndDestinationCapacity(GameTestHelper helper) {
        BlockPos chestPosition = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(chestPosition, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(chestPosition);
        ExporterTransferStrategyFactory factory = RefinedStorageApi.INSTANCE
                .getExporterTransferStrategyRegistry().getAll().stream()
                .filter(candidate -> candidate.getResourceType() == ItemResource.class)
                .findFirst().orElseThrow();
        ResourceKey resource = InfiniteDiskType.COBBLESTONE.resource();

        for (boolean infinite : new boolean[]{false, true}) {
            StorageNetworkNode drive = new StorageNetworkNode(1, 1, 8);
            drive.setActive(true);
            StorageImpl ordinaryStorage = new StorageImpl();
            ordinaryStorage.insert(resource, 4_096, Action.EXECUTE, Actor.EMPTY);
            var source = infinite ? InfiniteDiskType.COBBLESTONE.create(null, () -> { }) : ordinaryStorage;
            ((CompositeStorage) drive.getStorage()).addSource(new StateTrackedStorage(source, null));
            RootStorageImpl rootStorage = new RootStorageImpl();
            rootStorage.addSource(drive.getStorage());
            Network network = storageOnlyNetwork(rootStorage);

            chest.clearContent();
            var singleTransfer = factory.create(helper.getLevel(), chestPosition, Direction.UP,
                    new TestUpgrades(false, 0), false);
            assertEquals(ExporterTransferStrategy.Result.EXPORTED, singleTransfer.transfer(resource, Actor.EMPTY, network));
            assertEquals(1, chest.getItem(0).getCount());

            chest.clearContent();
            var stackTransfer = factory.create(helper.getLevel(), chestPosition, Direction.UP,
                    new TestUpgrades(true, 0), false);
            assertEquals(ExporterTransferStrategy.Result.EXPORTED, stackTransfer.transfer(resource, Actor.EMPTY, network));
            assertEquals(64, chest.getItem(0).getCount());
            assertTrue(chest.getItem(1).isEmpty());

            // Leave exactly one item of space; repeated transfers must stop at the slot limit.
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                chest.setItem(slot, new ItemStack(Items.COBBLESTONE, slot == 0 ? 63 : 64));
            }
            assertEquals(ExporterTransferStrategy.Result.EXPORTED, stackTransfer.transfer(resource, Actor.EMPTY, network));
            for (int attempt = 0; attempt < 100; attempt++) {
                assertEquals(ExporterTransferStrategy.Result.DESTINATION_DOES_NOT_ACCEPT,
                        stackTransfer.transfer(resource, Actor.EMPTY, network));
            }
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                assertEquals(64, chest.getItem(slot).getCount());
            }

            chest.clearContent();
            var regulatedTransfer = factory.create(helper.getLevel(), chestPosition, Direction.UP,
                    new TestUpgrades(true, 10), false);
            assertEquals(ExporterTransferStrategy.Result.EXPORTED, regulatedTransfer.transfer(resource, Actor.EMPTY, network));
            assertEquals(10, chest.getItem(0).getCount());
            assertEquals(ExporterTransferStrategy.Result.SKIPPED, regulatedTransfer.transfer(resource, Actor.EMPTY, network));

            chest.clearContent();
            drive.setActive(false);
            assertEquals(ExporterTransferStrategy.Result.RESOURCE_MISSING, stackTransfer.transfer(resource, Actor.EMPTY, network));
            assertTrue(chest.getItem(0).isEmpty());
            if (infinite) {
                assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, source.getStored());
            } else {
                assertEquals(4_020, source.getStored());
            }
        }
    }

    private static Network storageOnlyNetwork(RootStorageImpl rootStorage) {
        // Substitute only network facades; extraction, quotas and destination insertion remain real.
        StorageNetworkComponent component = (StorageNetworkComponent) Proxy.newProxyInstance(
                StorageNetworkComponent.class.getClassLoader(), new Class<?>[]{StorageNetworkComponent.class},
                (proxy, method, arguments) -> rootStorage.getClass()
                        .getMethod(method.getName(), method.getParameterTypes()).invoke(rootStorage, arguments));
        return (Network) Proxy.newProxyInstance(Network.class.getClassLoader(), new Class<?>[]{Network.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getComponent") && arguments[0] == StorageNetworkComponent.class) {
                        return component;
                    }
                    throw new UnsupportedOperationException("Unexpected network operation: " + method.getName());
                });
    }

    private record TestUpgrades(boolean stackUpgrade, long regulatedAmount) implements UpgradeState {
        @Override
        public long getRegulatedAmount(ResourceKey resource) {
            return regulatedAmount;
        }

        @Override
        public int getAmount(UpgradeItem upgrade) {
            var items = com.refinedmods.refinedstorage.common.content.Items.INSTANCE;
            if (upgrade == items.getStackUpgrade() && stackUpgrade) {
                return 1;
            }
            if (upgrade == items.getRegulatorUpgrade() && regulatedAmount > 0) {
                return 1;
            }
            return 0;
        }
    }
}
