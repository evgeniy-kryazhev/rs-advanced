package dev.rsadvanced.feature.anchor;

public enum AnchorStatus {
    RECOVERING, ACTIVE, RESERVE, DISABLED, NO_NETWORK, NO_ENERGY, LIMIT_EXCEEDED;

    public String translationKey() {
        return "gui.rsadvanced.network_anchor.status." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
