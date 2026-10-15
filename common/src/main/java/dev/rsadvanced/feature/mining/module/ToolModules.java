package dev.rsadvanced.feature.mining.module;

import java.util.List;

/** Factory-installed modules. Selecting Off changes a setting; it does not uninstall a module. */
public final class ToolModules {
    public static final EnergyToolModule ENERGY = new EnergyToolModule();
    public static final HarvestToolModule HARVEST = new HarvestToolModule();
    public static final SpeedToolModule SPEED = new SpeedToolModule();
    public static final AreaToolModule AREA = new AreaToolModule();
    public static final VeinToolModule VEIN = new VeinToolModule();
    public static final CombatToolModule COMBAT = new CombatToolModule();
    public static final NetworkToolModule NETWORK = new NetworkToolModule();
    public static final List<ToolModule> BUILT_IN = List.of(ENERGY, HARVEST, SPEED, AREA, VEIN, COMBAT, NETWORK);

    private ToolModules() {
    }
}
