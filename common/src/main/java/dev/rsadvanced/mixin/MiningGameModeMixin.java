package dev.rsadvanced.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.rsadvanced.feature.mining.MiningItem;
import dev.rsadvanced.feature.mining.MiningOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerGameMode.class)
public abstract class MiningGameModeMixin {
    @Shadow protected ServerPlayer player;
    @Unique private Direction rsadvanced$miningFace = Direction.UP;

    @Inject(method = "handleBlockBreakAction", at = @At("HEAD"))
    private void rsadvanced$rememberFace(BlockPos pos, ServerboundPlayerActionPacket.Action action,
                                         Direction face, int maxBuildHeight, int sequence, CallbackInfo callback) {
        if (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            rsadvanced$miningFace = face;
            if (player.getMainHandItem().getItem() instanceof MiningItem) {
                // Remove legacy enchantments before computing the first block's mining progress.
                MiningItem.prepare(player.getMainHandItem());
            }
        }
    }

    @WrapMethod(method = "destroyBlock")
    private boolean rsadvanced$mine(BlockPos pos, Operation<Boolean> original) {
        return MiningOperation.destroy(player, pos, rsadvanced$miningFace, () -> original.call(pos));
    }
}
