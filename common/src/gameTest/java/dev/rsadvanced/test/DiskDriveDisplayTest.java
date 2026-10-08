package dev.rsadvanced.test;

import com.refinedmods.refinedstorage.common.api.storage.StorageContainerItem;
import com.refinedmods.refinedstorage.common.api.storage.StorageInfo;
import com.refinedmods.refinedstorage.common.storage.StorageRepositoryImpl;
import com.refinedmods.refinedstorage.common.storage.diskdrive.DiskDriveContainerMenu;
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.disk.InfiniteDiskDriveMenu;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertFalse;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class DiskDriveDisplayTest {
    public static void driveStatisticsExcludeInfiniteDisks(GameTestHelper helper) throws ReflectiveOperationException {
        var player = helper.makeMockServerPlayerInLevel();
        DiskDriveContainerMenu menu = new DiskDriveContainerMenu(7, player.getInventory(),
                new ResourceContainerData(List.of()));
        InfiniteDiskDriveMenu sourcesMenu = (InfiniteDiskDriveMenu) menu;
        ItemStack cobblestoneDisk = disk(TestCell.named("rsadvanced:cobblestone"));
        ItemStack waterDisk = disk(TestCell.named("rsadvanced:water"));
        var repository = new StorageRepositoryImpl();
        assertEquals(new StorageInfo(0, 0),
                ((StorageContainerItem) cobblestoneDisk.getItem()).getInfo(repository, cobblestoneDisk).orElseThrow());

        // Supply known client metadata without sending client packets from a GameTest server.
        Field infoAccessor = DiskDriveContainerMenu.class.getDeclaredField("storageInfoAccessor");
        infoAccessor.setAccessible(true);
        Object accessor = Proxy.newProxyInstance(infoAccessor.getType().getClassLoader(),
                new Class<?>[]{infoAccessor.getType()}, (proxy, method, arguments) -> {
                    if (!method.getName().equals("getInfo")) {
                        throw new UnsupportedOperationException("Unexpected info operation: " + method.getName());
                    }
                    ItemStack stack = (ItemStack) arguments[0];
                    if (stack.getItem() instanceof InfiniteDiskItem) {
                        return ((StorageContainerItem) stack.getItem()).getInfo(repository, stack);
                    }
                    return Optional.of(new StorageInfo(25, 100));
                });
        infoAccessor.set(menu, accessor);

        // Player inventory slots must never be counted as installed infinite sources.
        player.getInventory().setItem(0, waterDisk.copy());
        assertTrue(sourcesMenu.rsadvanced$getSources().resources().isEmpty());
        for (TestCell type : TestCell.values()) {
            menu.getSlot(0).set(disk(type));
            assertEquals(Set.of(type.definition()), sourcesMenu.rsadvanced$getSources().resources());
            assertEquals(0, menu.getStored());
            assertEquals(0, menu.getCapacity());
            assertEquals(Double.valueOf(0.0), Double.valueOf(menu.getProgress()));
            List<Component> singleTooltip = new ArrayList<>();
            sourcesMenu.rsadvanced$getSources().appendTooltip(singleTooltip);
            assertEquals(List.of(Component.translatable("tooltip.rsadvanced.drive_infinite_source",
                    Component.empty().append(dev.rsadvanced.feature.disk.CellResourceNames.name(type.resource()))).withStyle(net.minecraft.ChatFormatting.AQUA)),
                    singleTooltip);
        }
        menu.getSlot(0).set(cobblestoneDisk.copy());
        menu.getSlot(1).set(cobblestoneDisk.copy());
        menu.getSlot(2).set(waterDisk.copy());
        var sources = sourcesMenu.rsadvanced$getSources();
        assertEquals(Set.of(TestCell.named("rsadvanced:cobblestone").definition(), TestCell.named("rsadvanced:water").definition()), sources.resources());
        assertFalse(sources.hasOrdinaryDisks());
        List<Component> tooltip = new ArrayList<>();
        sources.appendTooltip(tooltip);
        assertEquals(1, tooltip.size());

        var ordinaryDisk = BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof StorageContainerItem)
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("refinedstorage"))
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getPath().contains("disk"))
                .findFirst().orElseThrow();
        menu.getSlot(3).set(new ItemStack(ordinaryDisk));
        assertTrue(sourcesMenu.rsadvanced$getSources().hasOrdinaryDisks());
        assertTrue(menu.hasCapacity());
        assertEquals(25, menu.getStored());
        assertEquals(100, menu.getCapacity());
        assertEquals(Double.valueOf(0.25), Double.valueOf(menu.getProgress()));
        for (int slot = 0; slot < 3; slot++) {
            menu.getSlot(slot).set(ItemStack.EMPTY);
        }
        assertTrue(sourcesMenu.rsadvanced$getSources().resources().isEmpty());
        assertEquals(25, menu.getStored());
        assertEquals(Double.valueOf(0.25), Double.valueOf(menu.getProgress()));
        menu.getSlot(3).set(ItemStack.EMPTY);
        assertFalse(sourcesMenu.rsadvanced$getSources().hasOrdinaryDisks());
        assertEquals(0, menu.getStored());
    }

    private static ItemStack disk(TestCell type) {
        return type.stack();
    }
}
