package dev.rsadvanced.test;

import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;

import static dev.rsadvanced.test.TestAssertions.assertEquals;
import static dev.rsadvanced.test.TestAssertions.assertTrue;

/** New scenarios are discovered without editing either loader or the common generator. */
public final class SharedGameTestDiscoveryTest {
    @SharedGameTest("shared_discovery_automatic")
    public static void automaticCompletion() {
        assertTrue(SharedGameTestCatalog.discover().size() >= 30);
    }

    @SharedGameTest(value = "shared_discovery_manual", completion = SharedGameTest.Completion.MANUAL)
    public static void manualCompletion(GameTestHelper helper) {
        helper.runAfterDelay(2, helper::succeed);
    }

    @SharedGameTest("shared_discovery_validation")
    public static void registrationAndFailureValidation() throws Exception {
        var generated = SharedGameTestCatalog.createTests(List.of(ValidScenario.class));
        assertEquals(1, generated.size());
        expectInvalid(List.of(DuplicateOne.class, DuplicateTwo.class), "Duplicate");
        expectInvalid(List.of(WrongArguments.class), "GameTestHelper");
        expectInvalid(List.of(InstanceMethod.class), "public static void");
        expectInvalid(List.of(ReturnValue.class), "public static void");
        expectInvalid(List.of(ManualWithoutHelper.class), "Manual");
        expectInvalid(List.of(InvalidTimeout.class), "timeout");
        expectInvalid(List.of(), "No annotated");
        var method = ThrowingScenario.class.getDeclaredMethod("run");
        try {
            SharedGameTestCatalog.invoke(method, method.getAnnotation(SharedGameTest.class), null);
            throw new AssertionError("Scenario failure was swallowed");
        } catch (IllegalStateException exception) {
            assertTrue(exception.getCause() == ThrowingScenario.FAILURE);
        }
    }

    private static void expectInvalid(List<Class<?>> classes, String message) {
        try {
            SharedGameTestCatalog.createTests(classes);
            throw new AssertionError("Invalid scenarios were accepted: " + classes);
        } catch (IllegalStateException exception) {
            assertTrue(exception.getMessage().contains(message));
        }
    }

    public static final class ValidScenario {
        @SharedGameTest("fixture")
        public static void run() {
        }
    }

    public static final class DuplicateOne {
        @SharedGameTest("duplicate")
        public static void run() {
        }
    }

    public static final class DuplicateTwo {
        @SharedGameTest("duplicate")
        public static void run() {
        }
    }

    public static final class WrongArguments {
        @SharedGameTest("wrong_arguments")
        public static void run(String argument) {
        }
    }

    public static final class InstanceMethod {
        @SharedGameTest("instance")
        public void run() {
        }
    }

    public static final class ReturnValue {
        @SharedGameTest("return_value")
        public static int run() {
            return 1;
        }
    }

    public static final class ManualWithoutHelper {
        @SharedGameTest(value = "manual", completion = SharedGameTest.Completion.MANUAL)
        public static void run() {
        }
    }

    public static final class InvalidTimeout {
        @SharedGameTest(value = "timeout", timeoutTicks = 0)
        public static void run() {
        }
    }

    public static final class ThrowingScenario {
        private static final AssertionError FAILURE = new AssertionError("Fixture failure");

        @SharedGameTest("throwing")
        public static void run() {
            throw FAILURE;
        }
    }
}
