package dev.rsadvanced.test.client;

import com.mojang.serialization.Lifecycle;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi;
import com.refinedmods.refinedstorage.common.api.support.HelpTooltipComponent;
import com.refinedmods.refinedstorage.common.grid.GridContainerMenu;
import com.refinedmods.refinedstorage.common.grid.GridData;
import com.refinedmods.refinedstorage.common.grid.view.FluidGridResource;
import com.refinedmods.refinedstorage.common.grid.view.ItemGridResource;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.tooltip.HelpClientTooltipComponent;
import com.refinedmods.refinedstorage.common.support.tooltip.SmallTextClientTooltipComponent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.client.InfiniteGridDisplay;
import dev.rsadvanced.client.AnchorScreen;
import dev.rsadvanced.feature.anchor.AnchorMenu;
import dev.rsadvanced.feature.anchor.AnchorContent;
import dev.rsadvanced.feature.anchor.AnchorStatus;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.DiskDriveSources;
import dev.rsadvanced.feature.disk.DiskResourceKind;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
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
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.RegistryAccess;
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
    private static int anchorPreviewTicks;
    private static int anchorInteractionTicks;
    private static boolean anchorInteractionSent;
    private static int anchorInteractionStage;

    public static void register() {
        if (Boolean.getBoolean("rsadvanced.test.clientValidation")) {
            ClientTickEvent.CLIENT_POST.register(CellPresentationClientCheck::tick);
        }
    }

    private static void tick(Minecraft minecraft) {
        if (anchorInteractionTicks > 0) {
            checkAnchorInteraction(minecraft);
            return;
        }
        if (anchorPreviewTicks > 0) {
            anchorPreviewTicks++;
            if (anchorPreviewTicks == 80) {
                try (var pixels = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                    Path screenshot = Path.of(System.getProperty("rsadvanced.test.clientReport"))
                            .resolveSibling("network-anchor-menu.png");
                    pixels.writeToFile(screenshot);
                    minecraft.setScreen(null);
                    AnchorVisualizationClientCheck.start(minecraft);
                } catch (Exception exception) {
                    finish(minecraft, exception);
                }
            }
            if (anchorPreviewTicks >= 110 && (anchorPreviewTicks - 110) % 30 == 0) {
                try {
                    if (AnchorVisualizationClientCheck.captureAndAdvance(minecraft)) {
                        finish(minecraft, null);
                    }
                } catch (Exception exception) {
                    finish(minecraft, exception);
                }
            }
            return;
        }
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
                    showOverlayPreview(minecraft);
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
        assertTrue(!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(RSAdvanced.id("main")));
        var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(RefinedStorageApi.INSTANCE.getCreativeModeTabId());
        var parameters = new CreativeModeTab.ItemDisplayParameters(minecraft.level.enabledFeatures(), false,
                minecraft.getConnection().registryAccess());
        var expectedIds = entries.stream().map(java.util.Map.Entry::getKey).toList();
        tab.buildContents(parameters);
        checkCreativeVariants(tab, expectedIds);
        tab.buildContents(parameters);
        checkCreativeVariants(tab, expectedIds);

        // Rebuild the actual loader event with a different catalog to detect cached variants.
        var alternateRegistry = new MappedRegistry<dev.rsadvanced.feature.disk.CellDefinition>(
                CellDefinitions.REGISTRY_KEY, Lifecycle.stable());
        var lavaId = ResourceLocation.parse("my_pack:lava");
        alternateRegistry.register(net.minecraft.resources.ResourceKey.create(CellDefinitions.REGISTRY_KEY, lavaId),
                CellDefinitions.displayDefinition(lavaId).orElseThrow(), RegistrationInfo.BUILT_IN);
        var alternateAccess = new RegistryAccess.ImmutableRegistryAccess(List.of(alternateRegistry)).freeze();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(minecraft.level.enabledFeatures(), false, alternateAccess));
        checkCreativeVariants(tab, List.of(lavaId));
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(minecraft.level.enabledFeatures(), false, RegistryAccess.EMPTY));
        checkCreativeVariants(tab, List.of());
        tab.buildContents(parameters);
        checkCreativeVariants(tab, expectedIds);
        var partModel = minecraft.getModelManager().getModel(ModelResourceLocation.inventory(
                BuiltInRegistries.ITEM.getKey(AdvancedContent.INFINITE_STORAGE_PART.get())));
        assertTrue(partModel != minecraft.getModelManager().getMissingModel());
        for (DiskResourceKind kind : DiskResourceKind.values()) {
            var item = AdvancedContent.disk(kind).get();
            assertEquals(kind.diskModel(), RefinedStorageClientApi.INSTANCE.getDiskModelsByItem().get(item));
            var model = minecraft.getModelManager().getModel(ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)));
            assertTrue(model != minecraft.getModelManager().getMissingModel());
        }
    }

    private static void checkCreativeVariants(CreativeModeTab tab, List<ResourceLocation> expectedIds) {
        for (var stacks : List.of(tab.getDisplayItems(), tab.getSearchTabDisplayItems())) {
            assertEquals(1, stacks.stream().filter(stack -> stack.is(AdvancedContent.INFINITE_STORAGE_PART.get())).count());
            var cells = stacks.stream().filter(stack -> stack.getItem() instanceof InfiniteDiskItem).toList();
            var actualIds = cells.stream().map(stack -> stack.get(AdvancedComponents.CELL_DEFINITION.get())).toList();
            assertEquals(expectedIds, actualIds);
            assertEquals(expectedIds.size(), actualIds.stream().distinct().count());
        }
        var displayItems = new ArrayList<>(tab.getDisplayItems());
        var tail = displayItems.subList(displayItems.size() - expectedIds.size(), displayItems.size());
        assertEquals(expectedIds, tail.stream().map(stack -> stack.get(AdvancedComponents.CELL_DEFINITION.get())).toList());
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
        String partName = language.equals("ru_ru") ? "Часть для бесконечного хранения" : "Infinite Storage Part";
        assertEquals(partName, new ItemStack(AdvancedContent.INFINITE_STORAGE_PART.get()).getHoverName().getString());
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
        checkHelpTooltip(minecraft, stoneCell, language);
        checkHelpTooltip(minecraft, waterCell, language);
        LOGGER.info("Checked {}: {}", language, tooltip.getFirst().getString());
    }

    private static void checkHelpTooltip(Minecraft minecraft, ItemStack cell, String language) {
        var help = (HelpTooltipComponent) cell.getItem().getTooltipImage(cell).orElseThrow();
        String expectedHelp = language.equals("ru_ru")
                ? "Устанавливается в дисковый привод Refined Storage. Предоставляет бесконечный запас указанного ресурса и принимает его обратно без накопления."
                : "Insert into a Refined Storage Disk Drive to provide an infinite supply of the specified resource. Accepts the same resource back without accumulating it.";
        assertEquals(expectedHelp, help.text().getString());
        List<Component> ordinaryTooltip = new ArrayList<>();
        cell.getItem().appendHoverText(cell, net.minecraft.world.item.Item.TooltipContext.EMPTY,
                ordinaryTooltip, net.minecraft.world.item.TooltipFlag.NORMAL);
        assertTrue(ordinaryTooltip.isEmpty());
        var collapsed = net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(help);
        assertTrue(collapsed instanceof SmallTextClientTooltipComponent);

        boolean originalUnicode = minecraft.options.forceUnicodeFont().get();
        try {
            for (boolean unicode : List.of(false, true)) {
                minecraft.options.forceUnicodeFont().set(unicode);
                // Exercise RS's expanded layout directly; physical Shift input remains a manual scenario.
                var expanded = HelpClientTooltipComponent.createAlwaysDisplayed(help.text());
                assertTrue(expanded.getHeight() > 20);
                assertTrue(expanded.getWidth(minecraft.font) > 0);
            }
        } finally {
            minecraft.options.forceUnicodeFont().set(originalUnicode);
        }
    }

    private static void showOverlayPreview(Minecraft minecraft) {
        minecraft.setScreen(new net.minecraft.client.gui.screens.Screen(Component.literal("Cell resource icons")) {
            private int renderedFrames;

            @Override
            public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.fill(0, 0, width, height, 0xFF25282E);
                graphics.drawCenteredString(font, "RS Advanced: supplied sprites and dynamic resource icons",
                        width / 2, 20, 0xFFFFFFFF);
                List<ItemStack> cells = List.of(
                        new ItemStack(AdvancedContent.ITEM_DISK.get()),
                        previewCell("rsadvanced:cobblestone", DiskResourceKind.ITEM),
                        new ItemStack(AdvancedContent.FLUID_DISK.get()),
                        previewCell("rsadvanced:water", DiskResourceKind.FLUID),
                        previewCell("my_pack:lava", DiskResourceKind.FLUID),
                        previewCell("rsadvanced_test:red_wool", DiskResourceKind.ITEM));
                List<String> labels = List.of("Base item", "Cobblestone", "Base fluid", "Water", "Lava", "Red wool");
                int spacing = Math.min(80, (width - 16) / cells.size());
                int startX = (width - cells.size() * spacing) / 2;
                for (int index = 0; index < cells.size(); index++) {
                    int x = startX + index * spacing;
                    graphics.pose().pushPose();
                    graphics.pose().translate(x, 60, 0);
                    graphics.pose().scale(4, 4, 1);
                    graphics.renderItem(cells.get(index), 0, 0);
                    graphics.pose().popPose();
                    graphics.drawCenteredString(font, labels.get(index), x + 32, 135, 0xFFFFFFFF);
                    graphics.renderItem(cells.get(index), x + 24, 160);
                }
                graphics.flush();
                renderedFrames++;
                if (renderedFrames == 8) {
                    Path screenshot = Path.of(System.getProperty("rsadvanced.test.clientReport"))
                            .resolveSibling("cell-resource-icons.png");
                    try (var pixels = net.minecraft.client.Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
                        pixels.writeToFile(screenshot);
                        minecraft.execute(() -> {
                            minecraft.setScreen(null);
                            anchorInteractionTicks = 1;
                        });
                    } catch (Exception exception) {
                        finish(minecraft, exception);
                    }
                }
            }

            private ItemStack previewCell(String definition, DiskResourceKind kind) {
                var id = ResourceLocation.parse(definition);
                assertTrue(CellDefinitions.displayDefinition(id).isPresent());
                return AdvancedContent.cell(id, kind);
            }
        });
    }

    private static void showAnchorPreview(Minecraft minecraft) {
        var menu = new AnchorMenu(89, minecraft.player.getInventory(),
                minecraft.player.blockPosition());
        menu.enabled = true;
        menu.leader = true;
        menu.status = AnchorStatus.ACTIVE;
        menu.areaSize = 2;
        menu.heldSize = 2;
        menu.cost = 83;
        minecraft.setScreen(new AnchorScreen(menu, minecraft.player.getInventory(),
                Component.translatable("block.rsadvanced.network_anchor")));
        anchorPreviewTicks = 1;
    }

    private static void checkAnchorInteraction(Minecraft minecraft) {
        try {
            anchorInteractionTicks++;
            if (anchorInteractionTicks > 200) {
                throw new AssertionError("Anchor interaction did not complete: stage=" + anchorInteractionStage
                        + ", screen=" + minecraft.screen + ", menu=" + minecraft.player.containerMenu);
            }
            var position = minecraft.player.blockPosition().east(2);
            if (!anchorInteractionSent) {
                if (!minecraft.level.getBlockState(position).is(AnchorContent.BLOCK.get())) {
                    return;
                }
                minecraft.gameMode.useItemOn(minecraft.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                        new net.minecraft.world.phys.BlockHitResult(position.getCenter(),
                                net.minecraft.core.Direction.UP, position, false));
                anchorInteractionSent = true;
                return;
            }
            if (!(minecraft.screen instanceof AnchorScreen)
                    || !(minecraft.player.containerMenu instanceof AnchorMenu menu)) {
                return;
            }
            assertEquals(position, menu.position);
            if (anchorInteractionStage == 0 && menu.enabled) {
                // This must travel through the vanilla button packet and return in the server snapshot.
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                anchorInteractionStage = 1;
            } else if (anchorInteractionStage == 1 && !menu.enabled) {
                assertEquals(AnchorStatus.DISABLED, menu.status);
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                anchorInteractionStage = 2;
            } else if (anchorInteractionStage == 2 && menu.enabled) {
                LOGGER.info("Anchor right-click opened registered screen; server confirmed disable and enable");
                minecraft.player.closeContainer();
                anchorInteractionTicks = 0;
                showAnchorPreview(minecraft);
            }
        } catch (Exception | AssertionError failure) {
            finish(minecraft, failure);
        }
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
