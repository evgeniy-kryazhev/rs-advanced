package dev.rsadvanced.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.refinedmods.refinedstorage.common.Platform;
import com.refinedmods.refinedstorage.common.storage.StorageRepositoryImpl;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.content.AdvancedContent;
import dev.rsadvanced.feature.disk.CellDefinition;
import dev.rsadvanced.feature.disk.CellDefinitions;
import dev.rsadvanced.feature.disk.DiskResourceKind;
import dev.rsadvanced.feature.disk.InfiniteDiskItem;
import dev.rsadvanced.feature.disk.InfiniteStorageType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

public final class DiskCatalogTest {
    public static void catalogResourcesAndQuotasAreComplete(GameTestHelper helper) throws IOException {
        // Read the competing fixture from selected packs in priority order, independently of the world registry.
        String expectedResource = null;
        int competingDefinitions = 0;
        var path = ResourceLocation.parse("rsadvanced_test:rsadvanced/infinite_cell/priority.json");
        for (var selectedPack : helper.getLevel().getServer().getPackRepository().getSelectedPacks()) {
            try (var pack = selectedPack.open()) {
                var input = pack.getResource(net.minecraft.server.packs.PackType.SERVER_DATA, path);
                if (input != null) {
                    try (var reader = new InputStreamReader(input.get(), StandardCharsets.UTF_8)) {
                        expectedResource = JsonParser.parseReader(reader).getAsJsonObject().get("resource").getAsString();
                        competingDefinitions++;
                    }
                }
            }
        }
        assertTrue(competingDefinitions >= 2);
        assertTrue(TestCell.values().size() > 31);
        assertEquals(ResourceLocation.parse(expectedResource), TestCell.named("rsadvanced_test:priority").definition().resource());
        assertEquals(ResourceLocation.parse("minecraft:lava"), TestCell.named("my_pack:lava").definition().resource());
        assertEquals(ResourceLocation.parse("minecraft:lava"), TestCell.named("rsadvanced_test:lava").definition().resource());
        assertEquals(ResourceLocation.parse("refinedstorage:storage_housing"),
                TestCell.named("rsadvanced_test:modded_item").definition().resource());
        List<ResourceLocation> ids = TestCell.values().stream().map(TestCell::id).toList();
        assertEquals(ids.stream().sorted(java.util.Comparator.comparing(ResourceLocation::toString)).toList(), ids);
        JsonObject english = json("assets/rsadvanced/lang/en_us.json");
        JsonObject russian = json("assets/rsadvanced/lang/ru_ru.json");
        assertEquals("Infinite Cell (%s)", english.get("item.rsadvanced.infinite_cell").getAsString());
        assertEquals("Бесконечная ячейка (%s)", russian.get("item.rsadvanced.infinite_cell").getAsString());
        assertEquals("Infinite Resources (%s)", english.get("tooltip.rsadvanced.drive_infinite_source").getAsString());
        assertEquals("Бесконечные ресурсы (%s)", russian.get("tooltip.rsadvanced.drive_infinite_source").getAsString());
        for (DiskResourceKind kind : DiskResourceKind.values()) {
            assertEquals(kind, AdvancedContent.disk(kind).get().kind());
            String id = "infinite_" + kind.getSerializedName() + "_disk";
            JsonObject model = json("assets/rsadvanced/models/item/" + id + ".json");
            assertEquals("minecraft:item/generated", model.get("parent").getAsString());
            try (InputStream texture = resource("assets/rsadvanced/textures/item/" + id + ".png")) {
                assertEquals(8, texture.readNBytes(8).length);
            }
            long unit = kind == DiskResourceKind.FLUID ? Platform.INSTANCE.getBucketAmount() : 1;
            var type = InfiniteStorageType.forKind(kind);
            assertEquals(unit, type.getDiskInterfaceTransferQuota(false));
            assertEquals(unit * (kind == DiskResourceKind.FLUID ? 16 : 64), type.getDiskInterfaceTransferQuota(true));
            assertTrue(type.getMapCodec(() -> { }).codec().parse(JsonOps.INSTANCE,
                    JsonParser.parseString("{}" )).error().isPresent());
            boolean rejected = false;
            try {
                type.create(null, () -> { });
            } catch (IllegalArgumentException exception) {
                rejected = exception.getMessage().contains("cell_definition");
            }
            assertTrue(rejected);
        }
    }

    public static void invalidDefinitionsAreRejected() {
        for (String definition : List.of(
                "{\"kind\":\"chemical\",\"resource\":\"minecraft:water\"}",
                "{\"kind\":\"fluid\",\"resource\":\"minecraft:cobblestone\"}",
                "{\"kind\":\"item\",\"resource\":\"minecraft:water\"}",
                "{\"kind\":\"item\",\"resource\":\"missing_mod:resource\"}",
                "{\"kind\":\"item\",\"resource\":\"minecraft:air\"}")) {
            assertTrue(CellDefinition.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(definition)).error().isPresent());
        }
    }

    public static void unknownCellsKeepTheirComponents(GameTestHelper helper) {
        var repository = new StorageRepositoryImpl();
        ItemStack unknown = AdvancedContent.cell(ResourceLocation.parse("missing_pack:cell"), DiskResourceKind.ITEM);
        var operations = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var encoded = ItemStack.CODEC.encodeStart(operations, unknown).getOrThrow();
        ItemStack restored = ItemStack.CODEC.parse(operations, encoded).getOrThrow();
        assertEquals(ResourceLocation.parse("missing_pack:cell"), restored.get(AdvancedComponents.CELL_DEFINITION.get()));
        var item = (InfiniteDiskItem) restored.getItem();
        assertTrue(item.resolve(repository, restored).isEmpty());
        assertEquals(net.minecraft.network.chat.Component.translatable("item.rsadvanced.unknown_infinite_cell"),
                item.getName(restored));
        ItemStack wrongKind = AdvancedContent.cell(ResourceLocation.parse("rsadvanced:water"), DiskResourceKind.ITEM);
        assertTrue(((InfiniteDiskItem) wrongKind.getItem()).resolve(repository, wrongKind).isEmpty());
        assertTrue(item.resolve(repository, new ItemStack(item)).isEmpty());
    }

    public static void worldCatalogDoesNotLeakBetweenSessions(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        ItemStack cell = TestCell.named("rsadvanced:cobblestone").stack();
        var item = (InfiniteDiskItem) cell.getItem();
        var repository = new StorageRepositoryImpl();
        try {
            CellDefinitions.endSession();
            assertTrue(CellDefinitions.serverEntries().isEmpty());
            assertTrue(item.resolve(repository, cell).isEmpty());
            CellDefinitions.startSession(RegistryAccess.EMPTY);
            assertTrue(item.resolve(repository, cell).isEmpty());
        } finally {
            CellDefinitions.startSession(registries);
        }
        assertTrue(item.resolve(repository, cell).isPresent());
    }

    private static InputStream resource(String path) {
        InputStream stream = DiskCatalogTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) {
            throw new AssertionError("Missing bundled resource: " + path);
        }
        return stream;
    }

    private static JsonObject json(String path) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(resource(path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
