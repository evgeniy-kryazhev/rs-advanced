package dev.rsadvanced.fabric.data;

import dev.rsadvanced.data.DiskDataProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class RSAdvancedDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        generator.createPack().addProvider((net.minecraft.data.DataProvider.Factory<DiskDataProvider>) DiskDataProvider::new);
    }
}
