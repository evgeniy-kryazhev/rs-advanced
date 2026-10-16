package dev.rsadvanced.test.client;

import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.test.mixin.ItemInHandRendererAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import static dev.rsadvanced.test.TestAssertions.assertFalse;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** Exercises the real held-item renderer, including server-synchronized energy loss while mining. */
final class MiningAnimationClientCheck {
    private static BlockPos target;
    private static int ticks;
    private static boolean miningStarted;
    private static long initialEnergy;

    private MiningAnimationClientCheck() {
    }

    static void start(Minecraft minecraft) {
        verifyRendererUpdates(minecraft);
        minecraft.player.getInventory().selected = 6;
        target = minecraft.player.blockPosition().offset(0, 2, 2);
        ticks = 0;
        miningStarted = false;
        minecraft.getConnection().sendCommand("gamemode survival @s");
        minecraft.getConnection().sendCommand("setblock " + target.getX() + " " + target.getY() + " "
                + target.getZ() + " minecraft:stone");
    }

    static boolean tick(Minecraft minecraft) {
        if (++ticks > 160) {
            throw new AssertionError("Held tool mining animation check timed out");
        }
        if (ticks < 15 || minecraft.gameMode.hasInfiniteItems()) {
            return false;
        }
        var renderer = minecraft.gameRenderer.itemInHandRenderer;
        var state = (ItemInHandRendererAccessor) renderer;
        if (!miningStarted) {
            if (!minecraft.level.getBlockState(target).is(Blocks.STONE)) {
                return false;
            }
            initialEnergy = MiningItem.energy(minecraft.player.getMainHandItem());
            assertRaised(state.rsadvanced$mainHandHeight());
            miningStarted = true;
            minecraft.gameMode.startDestroyBlock(target, Direction.NORTH);
        }
        minecraft.player.resetAttackStrengthTicker();
        minecraft.player.swing(InteractionHand.MAIN_HAND);
        renderer.tick();
        assertFalse(minecraft.player.swinging);
        assertRaised(state.rsadvanced$mainHandHeight());
        if (minecraft.level.getBlockState(target).isAir()
                && MiningItem.energy(minecraft.player.getMainHandItem()) < initialEnergy) {
            // The live stack now contains a server-confirmed energy change, not just local prediction.
            renderer.tick();
            assertTrue(state.rsadvanced$mainHandItem() == minecraft.player.getMainHandItem());
            assertRaised(state.rsadvanced$mainHandHeight());
            minecraft.gameMode.stopDestroyBlock();
            minecraft.getConnection().sendCommand("gamemode creative @s");
            return true;
        }
        minecraft.gameMode.continueDestroyBlock(target, Direction.NORTH);
        return false;
    }

    private static void verifyRendererUpdates(Minecraft minecraft) {
        var player = minecraft.player;
        var renderer = minecraft.gameRenderer.itemInHandRenderer;
        var state = (ItemInHandRendererAccessor) renderer;
        int originalSlot = player.getInventory().selected;
        ItemStack originalMainHand = player.getMainHandItem();
        ItemStack originalOffHand = player.getOffhandItem();
        int otherSlot = (originalSlot + 1) % 9;
        ItemStack originalOtherSlot = player.getInventory().getItem(otherSlot);
        ItemStack tool = player.getInventory().getItem(6).copy();
        try {
            for (InteractionHand hand : InteractionHand.values()) {
                player.setItemInHand(hand, tool.copy());
                for (int tick = 0; tick < 12; tick++) {
                    renderer.tick();
                }
                for (int update = 0; update < 4; update++) {
                    ItemStack updatedTool = player.getItemInHand(hand).copy();
                    updatedTool.set(com.refinedmods.refinedstorage.common.content.DataComponents.INSTANCE.getEnergy(),
                            MiningItem.energy(updatedTool) - 1);
                    player.setItemInHand(hand, updatedTool);
                    player.resetAttackStrengthTicker();
                    renderer.itemUsed(hand);
                    renderer.tick();
                    float height = hand == InteractionHand.MAIN_HAND
                            ? state.rsadvanced$mainHandHeight() : state.rsadvanced$offHandHeight();
                    ItemStack visibleTool = hand == InteractionHand.MAIN_HAND
                            ? state.rsadvanced$mainHandItem() : state.rsadvanced$offHandItem();
                    assertRaised(height);
                    assertTrue(visibleTool == updatedTool);
                    assertTrue(player.getAttackStrengthScale(1) < 1);
                }
            }
            // Moving to another hotbar slot must still animate, even between two mining tools.
            player.getInventory().setItem(otherSlot, tool.copy());
            player.getInventory().selected = otherSlot;
            renderer.tick();
            assertTrue(state.rsadvanced$mainHandHeight() < 0.99f);

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
            player.resetAttackStrengthTicker();
            renderer.tick();
            assertTrue(state.rsadvanced$mainHandHeight() < 0.99f);
        } finally {
            player.getInventory().selected = originalSlot;
            player.setItemInHand(InteractionHand.MAIN_HAND, originalMainHand);
            player.setItemInHand(InteractionHand.OFF_HAND, originalOffHand);
            player.getInventory().setItem(otherSlot, originalOtherSlot);
        }
    }

    private static void assertRaised(float height) {
        assertTrue(Math.abs(height - 1.0f) < 0.001f);
    }
}
