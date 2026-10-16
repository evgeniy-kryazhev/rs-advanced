package dev.rsadvanced.mixin;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Raw hardness is the same client-side estimate used by Mekanism's speed calculation. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public interface MiningBlockStateAccessor {
    @Accessor("destroySpeed")
    float rsadvanced$rawDestroySpeed();
}
