package dev.rsadvanced.feature.mining;

import dev.rsadvanced.config.MiningSettings;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MiningBalanceTest {
    @Test
    void convertsTheMekanismDefaultsToFe() {
        assertEquals(400_000, MiningSettings.DEFAULT.capacity());
        assertEquals(2_000, MiningSettings.DEFAULT.chargeRate());
        assertEquals(80, MiningBalance.cost(MiningMode.NORMAL, 1));
        assertEquals(32, MiningBalance.cost(MiningMode.SLOW, 1));
        assertEquals(512, MiningBalance.cost(MiningMode.FAST, 1));
        assertEquals(40, MiningBalance.cost(MiningMode.NORMAL, 0));
        assertEquals(16, MiningBalance.cost(MiningMode.SLOW, 0));
        assertEquals(256, MiningBalance.cost(MiningMode.FAST, 0));
        assertEquals(0, MiningBalance.cost(MiningMode.OFF, 1));
        assertEquals(40, MiningBalance.veinCost(MiningMode.NORMAL, 1, 1));
        assertEquals(114, MiningBalance.veinCost(MiningMode.NORMAL, 1, 2));
        assertEquals(16, MiningBalance.veinCost(MiningMode.SLOW, 1, 1));
        assertEquals(46, MiningBalance.veinCost(MiningMode.SLOW, 1, 2));
        assertEquals(256, MiningBalance.veinCost(MiningMode.FAST, 1, 1));
        assertEquals(725, MiningBalance.veinCost(MiningMode.FAST, 1, 2));
    }

    @Test
    void scalesLowChargeFromNormalSpeedAndScalesWeaponDamage() {
        assertEquals(128, MiningBalance.speed(MiningMode.FAST, 512, 512));
        assertEquals(10, MiningBalance.speed(MiningMode.FAST, 256, 512));
        assertEquals(0, MiningBalance.speed(MiningMode.NORMAL, 0, 80));
        assertEquals(0, MiningBalance.speed(MiningMode.OFF, 1000, 0));
        assertEquals(4, MiningBalance.damage(0));
        assertEquals(12, MiningBalance.damage(400));
        assertEquals(20, MiningBalance.damage(800));
        assertEquals(20, MiningBalance.damage(10_000));
    }

    @Test
    void rejectsInvalidOrOverflowingBalance() {
        assertThrows(IllegalArgumentException.class, () -> new MiningSettings(0, 2000, 4, 800, 128, true));
        assertThrows(IllegalArgumentException.class, () -> new MiningSettings(400000, 2000, 4, 800, 5000, true));
        assertThrows(ArithmeticException.class, () -> new MiningSettings(400000, 2000, Long.MAX_VALUE, 800, 128, true));
        assertThrows(IllegalArgumentException.class,
                () -> new MiningSettings(Long.MAX_VALUE, 2000, 1_000_000_000_000L, 800, 4096, true));
        assertDoesNotThrow(
                () -> new MiningSettings(Long.MAX_VALUE, 2000, 100_000_000_000L, 800, 4096, true));
    }
}
