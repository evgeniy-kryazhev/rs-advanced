package dev.rsadvanced.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rsadvanced.feature.mining.MiningItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MiningHeldItemMixin {
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private ItemStack mainHandItem;
    @Shadow
    private ItemStack offHandItem;
    @Unique
    private int rsadvanced$previousSelectedSlot = -1;

    @Inject(method = "tick", at = @At("HEAD"))
    private void rsadvanced$refreshToolWithoutReequipping(CallbackInfo callback) {
        var player = minecraft.player;
        if (player == null) {
            rsadvanced$previousSelectedSlot = -1;
            return;
        }
        int selectedSlot = player.getInventory().selected;
        ItemStack currentMainHand = player.getMainHandItem();
        if (selectedSlot == rsadvanced$previousSelectedSlot && rsadvanced$isSameTool(mainHandItem, currentMainHand)) {
            // Server energy/component updates replace the stack, not the equipped tool.
            mainHandItem = currentMainHand;
        }
        ItemStack currentOffHand = player.getOffhandItem();
        if (rsadvanced$isSameTool(offHandItem, currentOffHand)) {
            offHandItem = currentOffHand;
        }
        rsadvanced$previousSelectedSlot = selectedSlot;
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float rsadvanced$keepToolRaisedDuringAttackCooldown(float original) {
        if (minecraft.player != null && minecraft.player.getMainHandItem().getItem() instanceof MiningItem) {
            // Change only the renderer's input; the player's real combat cooldown remains untouched.
            return 1.0f;
        }
        return original;
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void rsadvanced$keepToolRaisedAfterUse(InteractionHand hand, CallbackInfo callback) {
        if (minecraft.player != null && minecraft.player.getItemInHand(hand).getItem() instanceof MiningItem) {
            callback.cancel();
        }
    }

    @Unique
    private static boolean rsadvanced$isSameTool(ItemStack previous, ItemStack current) {
        return current.getItem() instanceof MiningItem && previous.is(current.getItem());
    }
}
