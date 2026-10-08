package dev.rsadvanced.test.neoforge;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.rsadvanced.test.ClientValidationServer;
import dev.rsadvanced.test.IntegrationGameTests;
import dev.rsadvanced.test.client.CellPresentationClientCheck;
import java.io.File;
import javax.xml.parsers.ParserConfigurationException;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod("rsadvanced_test")
public final class IntegrationTestMod {
    public IntegrationTestMod(IEventBus modBus) {
        ClientValidationServer.register();
        EnvExecutor.runInEnv(Env.CLIENT, () -> CellPresentationClientCheck::register);
        modBus.addListener(IntegrationTestMod::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        event.register(IntegrationGameTests.class);
        String reportPath = System.getProperty("rsadvanced.test.reportFile");
        if (reportPath != null) {
            try {
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(reportPath)));
            } catch (ParserConfigurationException exception) {
                throw new IllegalStateException("Cannot initialize the game test report", exception);
            }
        }
    }
}
