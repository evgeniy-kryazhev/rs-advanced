package dev.rsadvanced.feature.mining;

import dev.rsadvanced.config.MiningSettings;
import dev.rsadvanced.config.RSAdvancedConfig;

public final class MiningBalance {
    private static volatile MiningSettings remoteSettings;
    private static volatile net.minecraft.server.MinecraftServer activeServer;

    public static void bindServer(net.minecraft.server.MinecraftServer server) {
        activeServer = server;
    }

    public static void acceptServerSettings(MiningSettings settings) {
        remoteSettings = settings;
    }

    public static void clearServerSettings() {
        remoteSettings = null;
    }

    private MiningBalance() {
    }

    public static MiningSettings settings() {
        var server = activeServer;
        if (server != null && server.isSameThread()) {
            // A synchronized client snapshot must never replace the authoritative server configuration.
            return RSAdvancedConfig.get().mining();
        }
        return remoteSettings == null ? RSAdvancedConfig.get().mining() : remoteSettings;
    }

    public static long cost(MiningMode mode, float hardness) {
        if (mode == MiningMode.OFF) {
            return 0;
        }
        long fullCost = Math.multiplyExact(settings().baseCost(), mode.speed());
        return hardness == 0 ? Math.max(fullCost / 2, 1) : fullCost;
    }

    public static long veinCost(MiningMode speed, float hardness, int distance) {
        // Match Mekanism's rounding in joules before translating the result to FE.
        double joules = Math.ceil(cost(speed, hardness) * 2.5 * 0.5 * Math.pow(distance, 1.5));
        return Math.max(1, (long) Math.ceil(joules / 2.5));
    }

    public static float speed(MiningMode mode, long available, long required) {
        if (mode == MiningMode.OFF || available <= 0) {
            return 0;
        }
        return available >= required ? mode.speed() : (float) (20.0 * available / required);
    }

    public static double damage(long available) {
        return 4 + 16 * Math.min(1.0, (double) available / settings().weaponCost());
    }
}
