package dev.rsadvanced.test;

import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.TestFunction;

/** One test catalog executed in real Minecraft on Fabric and NeoForge. */
public final class IntegrationGameTests {
    @GameTestGenerator
    public Collection<TestFunction> generateTests() {
        if (Boolean.getBoolean("rsadvanced.test.clientServer")) {
            return List.of(new TestFunction("rsadvanced", "rsadvanced.client_validation", "rsadvanced_test:empty",
                    Integer.MAX_VALUE, 0, true, helper -> ClientValidationServer.completed = helper::succeed));
        }
        return SharedGameTestCatalog.discover();
    }
}
