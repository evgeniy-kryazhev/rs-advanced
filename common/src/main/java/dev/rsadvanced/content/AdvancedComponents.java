package dev.rsadvanced.content;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.rsadvanced.RSAdvanced;
import dev.rsadvanced.feature.mining.MiningMode;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

public final class AdvancedComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(RSAdvanced.MOD_ID, Registries.DATA_COMPONENT_TYPE);
    public static final RegistrySupplier<DataComponentType<MiningMode>> MINING_MODE =
            COMPONENTS.register("mining_mode", () -> DataComponentType.<MiningMode>builder()
                    .persistent(MiningMode.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodec(
                            MiningMode.CODEC))
                    .build());
    public static final RegistrySupplier<DataComponentType<ResourceLocation>> CELL_DEFINITION =
            COMPONENTS.register("cell_definition", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    private AdvancedComponents() {
    }

    public static void register() {
        COMPONENTS.register();
    }
}
