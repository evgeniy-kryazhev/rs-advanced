package dev.rsadvanced.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;

/** Loads a build-generated index rather than scanning platform class loaders or client classes. */
public final class SharedGameTestCatalog {
    private static final String INDEX = "META-INF/rsadvanced/shared-gametest-classes.txt";

    private SharedGameTestCatalog() {
    }

    public static Collection<TestFunction> discover() {
        ClassLoader loader = SharedGameTestCatalog.class.getClassLoader();
        var stream = loader.getResourceAsStream(INDEX);
        if (stream == null) {
            throw new IllegalStateException("Missing shared GameTest index: " + INDEX);
        }
        List<Class<?>> classes = new ArrayList<>();
        try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String name;
            while ((name = reader.readLine()) != null) {
                if (!name.isBlank()) {
                    classes.add(Class.forName(name, false, loader));
                }
            }
        } catch (IOException | ClassNotFoundException exception) {
            throw new IllegalStateException("Cannot load shared GameTest index", exception);
        }
        return createTests(classes);
    }

    static List<TestFunction> createTests(Collection<Class<?>> classes) {
        Map<String, Method> identifiers = new HashMap<>();
        List<Method> methods = new ArrayList<>();
        for (Class<?> type : classes) {
            for (Method method : type.getDeclaredMethods()) {
                SharedGameTest scenario = method.getAnnotation(SharedGameTest.class);
                if (scenario == null) {
                    continue;
                }
                validate(method, scenario);
                Method previous = identifiers.putIfAbsent(scenario.value(), method);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate shared GameTest identifier " + scenario.value()
                            + ": " + previous + " and " + method);
                }
                methods.add(method);
            }
        }
        if (methods.isEmpty()) {
            throw new IllegalStateException("No annotated shared GameTests found");
        }
        methods.sort(Comparator.comparingInt((Method method) -> method.getAnnotation(SharedGameTest.class).order())
                .thenComparing(method -> method.getAnnotation(SharedGameTest.class).value()));
        List<TestFunction> tests = new ArrayList<>();
        for (Method method : methods) {
            SharedGameTest scenario = method.getAnnotation(SharedGameTest.class);
            tests.add(new TestFunction("rsadvanced", "rsadvanced." + scenario.value(), scenario.template(),
                    scenario.timeoutTicks(), 0, true, helper -> invoke(method, scenario, helper)));
        }
        return List.copyOf(tests);
    }

    private static void validate(Method method, SharedGameTest scenario) {
        boolean validParameters = method.getParameterCount() == 0
                || (method.getParameterCount() == 1 && method.getParameterTypes()[0] == GameTestHelper.class);
        if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())
                || !Modifier.isPublic(method.getDeclaringClass().getModifiers())
                || method.getReturnType() != void.class || !validParameters) {
            throw new IllegalStateException("Shared GameTest must be public static void with no arguments or"
                    + " one GameTestHelper argument: " + method);
        }
        if (!scenario.value().matches("[a-z0-9_]+") || scenario.timeoutTicks() <= 0
                || ResourceLocation.tryParse(scenario.template()) == null) {
            throw new IllegalStateException("Invalid shared GameTest identifier, timeout or template: " + method);
        }
        if (scenario.completion() == SharedGameTest.Completion.MANUAL && method.getParameterCount() == 0) {
            throw new IllegalStateException("Manual shared GameTest requires GameTestHelper: " + method);
        }
    }

    static void invoke(Method method, SharedGameTest scenario, GameTestHelper helper) {
        try {
            if (method.getParameterCount() == 0) {
                method.invoke(null);
            } else {
                method.invoke(null, helper);
            }
        } catch (InvocationTargetException exception) {
            // Preserve the scenario failure as the cause rather than exposing reflection's wrapper.
            throw new IllegalStateException("Shared GameTest failed: " + scenario.value()
                    + ": " + exception.getCause(), exception.getCause());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot invoke shared GameTest: " + method, exception);
        }
        if (scenario.completion() == SharedGameTest.Completion.AUTOMATIC) {
            helper.succeed();
        }
    }
}
