package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.api.network.impl.node.storage.StorageNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.grid.GridInsertMode;
import com.refinedmods.refinedstorage.api.storage.AccessMode;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.StateTrackedStorage;
import com.refinedmods.refinedstorage.api.storage.composite.CompositeStorage;
import com.refinedmods.refinedstorage.api.storage.root.RootStorageImpl;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.grid.Grid;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceType;
import dev.rsadvanced.feature.disk.InfiniteDiskType;
import dev.rsadvanced.feature.disk.InfiniteResourceStorage;
import java.lang.reflect.Proxy;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Exercises the loader's real item/fluid strategies and container transactions, without a client. */
public final class GridInsertionTest {
    public static void gridReturnsCobblestoneAndEmptiesWaterBucket(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AbstractContainerMenu menu = player.containerMenu;
        for (InfiniteDiskType diskType : InfiniteDiskType.values()) {
            StorageNetworkNode node = new StorageNetworkNode(1, 1, 8);
            node.setActive(true);
            ((CompositeStorage) node.getStorage()).addSource(
                    new StateTrackedStorage(diskType.create(null, () -> { }), null));
            RootStorageImpl network = new RootStorageImpl();
            network.addSource(node.getStorage());
            Grid grid = insertionOnlyGrid(network);
            var strategy = RefinedStorageApi.INSTANCE.createGridInsertionStrategy(menu, player, grid);
            ItemStack returnedStack = diskType == InfiniteDiskType.WATER
                    ? new ItemStack(Items.WATER_BUCKET) : new ItemStack(Items.COBBLESTONE, 64);

            node.getStorageConfiguration().setAccessMode(AccessMode.EXTRACT);
            menu.setCarried(returnedStack.copy());
            strategy.onInsert(GridInsertMode.ENTIRE_RESOURCE, true);
            assertTrue(ItemStack.matches(returnedStack, menu.getCarried()));

            node.getStorageConfiguration().setAccessMode(AccessMode.INSERT_EXTRACT);
            assertTrue(strategy.onInsert(GridInsertMode.ENTIRE_RESOURCE, true));
            if (diskType == InfiniteDiskType.WATER) {
                assertTrue(menu.getCarried().is(Items.BUCKET));
                assertEquals(1, menu.getCarried().getCount());
            } else {
                assertTrue(menu.getCarried().isEmpty());
            }
            assertEquals(InfiniteResourceStorage.ADVERTISED_AMOUNT, network.get(diskType.resource()));
            menu.setCarried(ItemStack.EMPTY);
        }
    }

    private static Grid insertionOnlyGrid(RootStorageImpl network) {
        // Only the Grid facade is substituted. RS's operations, loader strategies, and storage are real.
        return (Grid) Proxy.newProxyInstance(Grid.class.getClassLoader(), new Class<?>[]{Grid.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("createOperations")) {
                        ResourceType resourceType = (ResourceType) arguments[0];
                        return resourceType.createGridOperations(network, Actor.EMPTY);
                    }
                    throw new UnsupportedOperationException("Unexpected Grid operation: " + method.getName());
                });
    }
}
