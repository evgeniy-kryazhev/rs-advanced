package dev.rsadvanced.test;

import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;

/** One test catalog executed in real Minecraft on Fabric and NeoForge. */
public final class IntegrationGameTests {
    @GameTestGenerator
    public Collection<TestFunction> generateTests() {
        if (Boolean.getBoolean("rsadvanced.test.clientServer")) {
            return List.of(new TestFunction("rsadvanced", "rsadvanced.client_validation", "rsadvanced_test:empty",
                    Integer.MAX_VALUE, 0, true, helper -> ClientValidationServer.completed = helper::succeed));
        }
        return List.of(
                create("mixed_disks", StorageIntegrationTest::diskWrappersPreserveInfiniteStockAndOrdinaryStock),
                create("absorbed_returns", StorageIntegrationTest::returnedResourcesAreAbsorbedWithoutCacheGrowth),
                create("insertion_priorities", StorageIntegrationTest::insertionPreservesFiniteStockPrioritiesAndVoidExcess),
                create("grid_returns", GridInsertionTest::gridReturnsCobblestoneAndEmptiesWaterBucket),
                create("exporter_limits", ExporterTransferTest::exporterRespectsQuotaAndDestinationCapacity),
                create("disk_catalog", DiskCatalogTest::catalogResourcesAndQuotasAreComplete),
                create("invalid_definitions", DiskCatalogTest::invalidDefinitionsAreRejected),
                create("unknown_cells", DiskCatalogTest::unknownCellsKeepTheirComponents),
                create("catalog_session", DiskCatalogTest::worldCatalogDoesNotLeakBetweenSessions),
                create("drive_display", DiskDriveDisplayTest::driveStatisticsExcludeInfiniteDisks),
                create("access_and_removal", StorageIntegrationTest::multipleDisksAndAccessChangesUpdateInfinityMetadata),
                create("storage_codec", StorageIntegrationTest::sourceCodecStoresOnlyDefinitionIdentity),
                create("disk_inventory_reload", DiskResourcesTest::standardDriveAcceptsDisksAndReloadsTheirStatelessItemStacks),
                create("recipes_and_buckets", helper -> DiskResourcesTest.survivalRecipesMatchAndReturnEmptyBuckets(helper.getLevel())),
                create("network_packet", DiskResourcesTest::infinityPacketRoundTripsMenuIdentityAndResources),
                new TestFunction("rsadvanced", "rsadvanced.datapack_reload", "rsadvanced_test:empty", 120000, 0, true, helper -> {
                    try {
                        DatapackReloadTest.reloadKeepsDefinitionsAndUpdatesRecipes(helper);
                    } catch (Exception exception) {
                        helper.fail("Cannot prepare datapack reload check: " + exception);
                    }
                }));
    }

    private static TestFunction create(String name, Check check) {
        return create(name, helper -> check.run());
    }

    private static TestFunction create(String name, LevelCheck check) {
        return new TestFunction("rsadvanced", "rsadvanced." + name, "rsadvanced_test:empty", 100, 0, true, helper -> {
            try {
                check.run(helper);
                helper.succeed();
            } catch (Exception | AssertionError exception) {
                throw new IllegalStateException("Integration check failed: " + name + ": " + exception.getMessage(), exception);
            }
        });
    }

    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface LevelCheck {
        void run(GameTestHelper helper) throws Exception;
    }
}
