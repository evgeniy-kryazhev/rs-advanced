package dev.rsadvanced.fabric.config;

import java.lang.reflect.Field;
import java.util.List;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class FabricConfigScreen {
    private FabricConfigScreen() {
    }

    public static void register() {
        // Energy costs span the full long range. Text fields allow exact values without
        // the precision loss and impractical steps of AutoConfig's default sliders.
        AutoConfig.getGuiRegistry(FabricConfig.class).registerAnnotationProvider(
                (translationKey, field, config, defaults, registry) -> {
                    var bounds = field.getAnnotation(ConfigEntry.BoundedDiscrete.class);
                    var entries = ConfigEntryBuilder.create();
                    var name = Component.translatable(translationKey);
                    var value = readNumber(field, config);
                    var defaultValue = readNumber(field, defaults);
                    if (field.getType() == long.class) {
                        return List.of(entries.startLongField(name, value.longValue())
                                .setMin(bounds.min())
                                .setMax(bounds.max())
                                .setDefaultValue(defaultValue.longValue())
                                .setSaveConsumer(updated -> writeNumber(field, config, updated))
                                .build());
                    }
                    return List.of(entries.startIntField(name, value.intValue())
                            .setMin(Math.toIntExact(bounds.min()))
                            .setMax(Math.toIntExact(bounds.max()))
                            .setDefaultValue(defaultValue.intValue())
                            .setSaveConsumer(updated -> writeNumber(field, config, updated))
                            .build());
                },
                field -> field.getType() == long.class || field.getType() == int.class,
                ConfigEntry.BoundedDiscrete.class);
    }

    private static Number readNumber(Field field, Object config) {
        try {
            return (Number) field.get(config);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot read configuration field " + field.getName(), exception);
        }
    }

    private static void writeNumber(Field field, Object config, Number value) {
        try {
            field.set(config, value);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot write configuration field " + field.getName(), exception);
        }
    }
}
