package dev.rsadvanced.test.client;

import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.grid.GridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.GridData;
import com.refinedmods.refinedstorage.common.grid.view.FluidGridResource;
import com.refinedmods.refinedstorage.common.grid.view.ItemGridResource;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.client.InfiniteGridDisplay;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.DiskDriveSources;
import dev.rsadvanced.feature.disk.DiskResourceKind;
import dev.rsadvanced.network.InfiniteGridMenu;
import dev.rsadvanced.network.InfiniteResourcesPayload;
import io.netty.buffer.Unpooled;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Opt-in real client checks: synchronized catalog, native RS names, models and a resource-pack override. */
public final class CellPresentationClientCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("rsadvanced-client-check");
    private static final long STARTED_AT = System.nanoTime();
    private static boolean started;

    public static void register() {
        if (Boolean.getBoolean("rsadvanced.test.clientValidation")) {
            ClientTickEvent.CLIENT_POST.register(CellPresentationClientCheck::tick);
        }
    }

    private static void tick(Minecraft minecraft) {
        if (started) {
            return;
        }
        if (System.nanoTime() - STARTED_AT > 150_000_000_000L) {
            started = true;
            finish(minecraft, new AssertionError("Client did not join the validation server"));
            return;
        }
        if (minecraft.level == null || minecraft.getConnection() == null) {
            return;
        }
        started = true;
        try {
            checkCatalogAndModels(minecraft);
            checkGridInfinity(minecraft);
            checkNames(minecraft, "en_us", "Cobblestone", "Water", "Storage Housing");
            checkNames(minecraft, "ru_ru", "Булыжник", "Вода", "Корпус диска");
            var packs = minecraft.getResourcePackRepository();
            packs.reload();
            var selected = new ArrayList<>(packs.getSelectedIds());
            selected.add("file/rsadvanced-translations");
            packs.setSelected(selected);
            assertTrue(packs.getSelectedIds().contains("file/rsadvanced-translations"));
            minecraft.reloadResourcePacks().whenComplete((result, error) -> minecraft.execute(() -> {
                if (error != null) {
                    finish(minecraft, error);
                    return;
                }
                try {
                    checkNames(minecraft, "en_us", "Pack Stone", "Pack Water", "Pack Housing");
                    checkNames(minecraft, "ru_ru", "Камень сборки", "Вода сборки", "Корпус сборки");
                    finish(minecraft, null);
                } catch (Exception | AssertionError exception) {
                    finish(minecraft, exception);
                }
            }));
        } catch (Exception | AssertionError exception) {
            finish(minecraft, exception);
        }
    }

    private static void checkCatalogAndModels(Minecraft minecraft) {
        var entries = CellDefinitions.entries(minecraft.getConnection().registryAccess());
        assertTrue(entries.size() > 31);
        assertTrue(CellDefinitions.displayDefinition(ResourceLocation.parse("my_pack:lava")).isPresent());
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(RSAdvanced.id("main"));
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(minecraft.level.enabledFeatures(), false,
                minecraft.getConnection().registryAccess()));
        assertEquals(entries.stream().map(java.util.Map.Entry::getKey).toList(), tab.getDisplayItems().stream()
                .map(stack -> stack.get(AdvancedComponents.CELL_DEFINITION.get())).toList());
        for (DiskResourceKind kind : DiskResourceKind.values()) {
            var item = AdvancedContent.disk(kind).get();
            assertEquals(kind.diskModel(), RefinedStorageClientApi.INSTANCE.getDiskModelsByItem().get(item));
            var model = minecraft.getModelManager().getModel(ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)));
            assertTrue(model != minecraft.getModelManager().getMissingModel());
        }
    }

    private static void checkGridInfinity(Minecraft minecraft) {
        var stone = new ItemResource(Items.COBBLESTONE);
        var water = new FluidResource(Fluids.WATER);
        var data = new GridData(true, List.of(
                new GridData.GridResource(
                        new ResourceAmount(stone, 64), Optional.empty()),
                new GridData.GridResource(
                        new ResourceAmount(water, 64), Optional.empty())), Set.of());
        var menu = new GridContainerMenu(88, minecraft.player.getInventory(), data);
        var previousMenu = minecraft.player.containerMenu;
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                minecraft.getConnection().registryAccess());
        try {
            minecraft.player.containerMenu = menu;
            var available = CellDefinitions.entries(minecraft.getConnection().registryAccess()).stream()
                    .map(entry -> entry.getValue().resourceKey()).collect(Collectors.toSet());
            var payload = new InfiniteResourcesPayload(88, available);
            InfiniteResourcesPayload.STREAM_CODEC.encode(buffer, payload);
            InfiniteGridDisplay.receive(InfiniteResourcesPayload.STREAM_CODEC.decode(buffer));
            assertTrue(((InfiniteGridMenu) menu).rsadvanced$getInfiniteResources().size() > 31);
            var stoneView = new ItemGridResource(
                    stone, stone.toItemStack(), "Cobblestone", "Cobblestone", attribute -> Set.of());
            var waterView = new FluidGridResource(water, "Water", attribute -> Set.of());
            assertEquals("∞", stoneView.getDisplayedAmount(menu.getRepository()));
            assertEquals("∞", stoneView.getAmountInTooltip(menu.getRepository()));
            assertEquals("∞", waterView.getDisplayedAmount(menu.getRepository()));
            assertEquals("∞", waterView.getAmountInTooltip(menu.getRepository()));
            InfiniteGridDisplay.receive(new InfiniteResourcesPayload(87, Set.of()));
            assertEquals("∞", stoneView.getDisplayedAmount(menu.getRepository()));
            InfiniteGridDisplay.receive(new InfiniteResourcesPayload(88, Set.of()));
            assertEquals("64", stoneView.getDisplayedAmount(menu.getRepository()));
        } finally {
            minecraft.player.containerMenu = previousMenu;
            buffer.release();
        }
    }

    private static void checkNames(Minecraft minecraft, String language, String stone, String water, String housing) {
        Language.inject(ClientLanguage.loadFrom(minecraft.getResourceManager(), List.of("en_us", language), false));
        String prefix = language.equals("ru_ru") ? "Бесконечная ячейка" : "Infinite Cell";
        ItemStack stoneCell = AdvancedContent.cell(RSAdvanced.id("cobblestone"), DiskResourceKind.ITEM);
        ItemStack waterCell = AdvancedContent.cell(RSAdvanced.id("water"), DiskResourceKind.FLUID);
        ItemStack housingCell = AdvancedContent.cell(ResourceLocation.parse("rsadvanced_test:modded_item"), DiskResourceKind.ITEM);
        assertEquals(prefix + " (" + stone + ")", stoneCell.getHoverName().getString());
        assertEquals(prefix + " (" + water + ")", waterCell.getHoverName().getString());
        assertEquals(prefix + " (" + housing + ")", housingCell.getHoverName().getString());
        List<Component> tooltip = new ArrayList<>();
        new DiskDriveSources(Set.of(CellDefinitions.displayDefinition(RSAdvanced.id("water")).orElseThrow(),
                CellDefinitions.displayDefinition(RSAdvanced.id("cobblestone")).orElseThrow()), false).appendTooltip(tooltip);
        String drivePrefix = language.equals("ru_ru") ? "Бесконечные ресурсы" : "Infinite Resources";
        assertEquals(1, tooltip.size());
        assertEquals(drivePrefix + " (" + stone + ", " + water + ")", tooltip.getFirst().getString());
        LOGGER.info("Checked {}: {}", language, tooltip.getFirst().getString());
    }

    private static void finish(Minecraft minecraft, Throwable failure) {
        try {
            String result = failure == null ? "PASSED" : "FAILED: " + failure;
            Files.writeString(Path.of(System.getProperty("rsadvanced.test.clientReport")), result + "\n");
            if (failure == null) {
                LOGGER.info("RS Advanced client validation PASSED");
            } else {
                LOGGER.error("RS Advanced client validation FAILED", failure);
            }
        } catch (java.io.IOException exception) {
            LOGGER.error("Cannot write client report", exception);
        }
        minecraft.stop();
    }
}
