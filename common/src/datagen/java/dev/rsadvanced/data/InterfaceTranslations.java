package dev.rsadvanced.data;

import java.util.Map;

/** Shared UI strings are separate from the extensible disk catalog. */
public final class InterfaceTranslations {
    public static final Map<String, String> ENGLISH = Map.of(
            "itemGroup.rsadvanced", "RS Advanced",
            "tooltip.rsadvanced.infinite_source", "∞ Inexhaustible resource source",
            "tooltip.rsadvanced.disk_drive", "Install in a Refined Storage Disk Drive",
            "tooltip.rsadvanced.absorbs_returns", "Absorbs its resource without accumulating it.",
            "tooltip.rsadvanced.drive_infinite_source", "Infinite source: %s — ∞");
    public static final Map<String, String> RUSSIAN = Map.of(
            "itemGroup.rsadvanced", "RS Advanced",
            "tooltip.rsadvanced.infinite_source", "∞ Неисчерпаемый источник ресурса",
            "tooltip.rsadvanced.disk_drive", "Устанавливается в Disk Drive Refined Storage",
            "tooltip.rsadvanced.absorbs_returns", "Принимает свой ресурс без накопления.",
            "tooltip.rsadvanced.drive_infinite_source", "Бесконечный источник: %s — ∞");

    private InterfaceTranslations() {
    }
}
