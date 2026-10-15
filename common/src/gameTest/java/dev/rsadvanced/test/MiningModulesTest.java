package dev.rsadvanced.test;

import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.content.AdvancedComponents;
import dev.rsadvanced.feature.connection.NetworkCardSlot;
import dev.rsadvanced.feature.mining.MiningContent;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningMode;
import dev.rsadvanced.feature.mining.MiningNetworking;
import dev.rsadvanced.feature.mining.MiningSelectionPayload;
import dev.rsadvanced.feature.mining.module.ToolModule;
import dev.rsadvanced.feature.mining.module.ToolModuleContainer;
import dev.rsadvanced.feature.mining.module.ToolModuleOption;
import dev.rsadvanced.feature.mining.module.ToolModules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;

import static dev.rsadvanced.test.TestAssertions.*;

public final class MiningModulesTest {
    @SharedGameTest("mining_modules_factory_loadout_and_stack_isolation")
    public static void modulesAreInstalledAndKeepSeparateStackState(GameTestHelper helper) {
        ItemStack first = MiningContent.ITEM.get().createAtEnergyCapacity();
        ItemStack second = MiningContent.ITEM.get().createAtEnergyCapacity();
        var modules = MiningItem.modules(first);
        assertEquals(7, modules.installed().size());
        assertEquals(8, modules.installed().stream().mapToInt(module -> module.options(modules).size()).sum());
        for (ToolModule module : ToolModules.BUILT_IN) {
            assertTrue(modules.find(module.id()).orElseThrow() == module);
        }
        assertTrue(modules.select(ToolModules.SPEED.id(), "fast"));
        assertTrue(modules.select(ToolModules.AREA.id(), "5"));
        assertTrue(modules.select(ToolModules.VEIN.id(), "toggle"));
        assertEquals(MiningMode.FAST, MiningItem.mode(first));
        assertEquals(2, MiningItem.radius(first));
        assertTrue(MiningItem.veinMining(first));
        assertEquals(MiningMode.NORMAL, MiningItem.mode(second));
        assertEquals(0, MiningItem.radius(second));
        assertFalse(MiningItem.veinMining(second));
        assertEquals(MiningItem.CAPACITY, MiningItem.energy(first));
        assertEquals(MiningItem.CAPACITY, MiningItem.energy(second));
        assertTrue(modules.select(ToolModules.SPEED.id(), "off"));
        assertFalse(modules.canMine());
        assertEquals(7, modules.installed().size());
        assertTrue(MiningItem.veinMining(first));
        assertEquals(2, MiningItem.radius(first));
    }

    @SharedGameTest("mining_modules_legacy_stack_round_trip")
    public static void legacyComponentsCardAndOtherCustomDataSurvive(GameTestHelper helper) {
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        tool.set(AdvancedComponents.MINING_MODE.get(), MiningMode.VEIN);
        MiningItem.setRadius(tool, 2);
        net.minecraft.world.item.component.CustomData.update(DataComponents.CUSTOM_DATA, tool,
                tag -> tag.putString("unrelated", "preserved"));
        ItemStack card = new ItemStack(com.refinedmods.refinedstorage.common.content.Items.INSTANCE.getNetworkCard());
        tool.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(card)));
        tool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(), 12345L);
        var registries = helper.getLevel().registryAccess();
        ItemStack loaded = ItemStack.parse(registries, tool.save(registries)).orElseThrow();
        MiningItem.modules(loaded).prepare();
        ItemStack restored = ItemStack.parse(registries, loaded.save(registries)).orElseThrow();
        assertEquals(MiningMode.NORMAL, MiningItem.mode(restored));
        assertTrue(MiningItem.veinMining(restored));
        assertEquals(2, MiningItem.radius(restored));
        assertEquals(12345, MiningItem.energy(restored));
        assertTrue(ItemStack.matches(card, NetworkCardSlot.card(restored)));
        assertEquals("preserved", restored.get(DataComponents.CUSTOM_DATA).copyTag().getString("unrelated"));
        assertEquals(7, MiningItem.modules(restored).installed().size());
    }

    @SharedGameTest("mining_modules_server_command_validation")
    public static void validatesModuleAndChoiceWithoutMutatingRejectedStacks(GameTestHelper helper) {
        var player = MiningOperationTest.survivalPlayer(helper);
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var reference = com.refinedmods.refinedstorage.common.api.RefinedStorageApi.INSTANCE
                .createInventorySlotReference(player, InteractionHand.MAIN_HAND);
        ItemStack before = tool.copy();
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(reference, RSAdvanced.id("unknown"), "on")));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(reference, ToolModules.ENERGY.id(), "fill")));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(reference, ToolModules.AREA.id(), "-3")));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(reference, ToolModules.SPEED.id(), "vein")));
        assertTrue(ItemStack.matches(before, tool));
        assertTrue(MiningNetworking.select(player, new MiningSelectionPayload(reference, ToolModules.SPEED.id(), "slow")));
        assertEquals(MiningMode.SLOW, MiningItem.mode(tool));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        assertFalse(MiningNetworking.select(player, new MiningSelectionPayload(reference, ToolModules.SPEED.id(), "fast")));
        assertEquals(MiningMode.SLOW, MiningItem.mode(tool));
    }

    @SharedGameTest("mining_modules_extension_dispatch")
    public static void additionalModuleParticipatesWithoutChangingTheItemOrMenu(GameTestHelper helper) {
        ItemStack tool = MiningContent.ITEM.get().createAtEnergyCapacity();
        int[] calls = new int[5];
        ToolModule extension = new ToolModule() {
            @Override
            public ResourceLocation id() {
                return RSAdvanced.id("test_extension");
            }

            @Override
            public List<ToolModuleOption> options(ToolModuleContainer container) {
                return List.of(
                        new ToolModuleOption("run", Component.literal("Test"), new ItemStack(Items.STICK), true),
                        new ToolModuleOption("blocked", Component.literal("Disabled"), new ItemStack(Items.BARRIER), false));
            }

            @Override
            public void applyOption(ToolModuleContainer container, String option) {
                calls[0]++;
            }

            @Override
            public float miningSpeed(ToolModuleContainer container,
                                     net.minecraft.world.level.block.state.BlockState state, float speed) {
                return speed * 2;
            }

            @Override
            public void hurtEnemy(ToolModuleContainer container, net.minecraft.world.entity.LivingEntity target,
                                  net.minecraft.world.entity.LivingEntity attacker) {
                calls[1]++;
            }

            @Override
            public void afterBlockMined(ToolModuleContainer container, net.minecraft.server.level.ServerPlayer player,
                                        BlockPos position, net.minecraft.world.level.block.state.BlockState state) {
                calls[2]++;
            }

            @Override
            public InteractionResult use(ToolModuleContainer container, net.minecraft.server.level.ServerPlayer player,
                                         com.refinedmods.refinedstorage.common.api.support.slotreference.SlotReference reference) {
                calls[3]++;
                return InteractionResult.SUCCESS;
            }

            @Override
            public void beforeAttack(ToolModuleContainer container, net.minecraft.world.entity.player.Player player) {
                calls[4]++;
            }
        };
        List<ToolModule> loadout = new ArrayList<>(ToolModules.BUILT_IN);
        loadout.add(extension);
        var container = new ToolModuleContainer(tool, loadout);
        assertTrue(container.select(extension.id(), "run"));
        assertFalse(container.select(extension.id(), "other"));
        assertFalse(container.select(extension.id(), "blocked"));
        assertEquals(40.0f, container.miningSpeed(Blocks.STONE.defaultBlockState()));
        var player = MiningOperationTest.survivalPlayer(helper);
        container.hurtEnemy(player, player);
        container.afterBlockMined(player, helper.absolutePos(new BlockPos(3, 8, 3)), Blocks.STONE.defaultBlockState());
        container.beforeAttack(player);
        var reference = com.refinedmods.refinedstorage.common.api.RefinedStorageApi.INSTANCE
                .createInventorySlotReference(player, InteractionHand.MAIN_HAND);
        assertEquals(InteractionResult.SUCCESS, container.use(player, reference));
        assertEquals(1, calls[0]);
        assertEquals(1, calls[1]);
        assertEquals(1, calls[2]);
        assertEquals(1, calls[3]);
        assertEquals(1, calls[4]);
        // The extension handled the click before the network module's card-menu fallback.
        assertTrue(player.containerMenu == player.inventoryMenu);
        assertEquals(8, container.installed().size());
        assertEquals(10, container.installed().stream().mapToInt(module -> module.options(container).size()).sum());
    }
}
