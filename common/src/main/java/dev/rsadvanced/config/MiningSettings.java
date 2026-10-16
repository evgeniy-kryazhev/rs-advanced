package dev.rsadvanced.config;

/** FE equivalents of Mekanism 10.7 defaults (2.5 joules per FE). */
public record MiningSettings(long capacity, long chargeRate, long baseCost,
                             int veinLimit, boolean veinEnabled) {
    public static final MiningSettings DEFAULT = new MiningSettings(400_000, 2_000, 4, 128, true);

    public MiningSettings {
        if (capacity < 1 || chargeRate < 1 || baseCost < 1 || veinLimit < 1 || veinLimit > 4096) {
            throw new IllegalArgumentException("Mining energy values must be positive and veinLimit must be 1..4096");
        }
        long maximumBlockCost = Math.multiplyExact(baseCost, 128);
        // Bound the worst supported traversal cost before converting it to an integer.
        if (maximumBlockCost * 0.5 * Math.pow(veinLimit, 1.5) > Long.MAX_VALUE / 2.0) {
            throw new IllegalArgumentException("Vein energy cost overflows a signed long");
        }
    }
}
