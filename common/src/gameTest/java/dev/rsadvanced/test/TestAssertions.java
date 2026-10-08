package dev.rsadvanced.test;

import java.util.Objects;

/** Small assertion helpers so game tests need no JUnit runtime in Minecraft. */
public final class TestAssertions {
    private TestAssertions() {
    }

    public static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    public static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Expected condition to be true");
        }
    }

    public static void assertFalse(boolean condition) {
        if (condition) {
            throw new AssertionError("Expected condition to be false");
        }
    }

    public static void assertInstanceOf(Class<?> expectedType, Object value) {
        if (!expectedType.isInstance(value)) {
            throw new AssertionError("Expected instance of " + expectedType.getName() + ", got " + value);
        }
    }
}
