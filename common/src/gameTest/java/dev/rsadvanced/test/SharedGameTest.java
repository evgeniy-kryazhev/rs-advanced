package dev.rsadvanced.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines a shared scenario in a common *Test class, without loader-specific registration. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SharedGameTest {
    String value();
    int timeoutTicks() default 100;
    String template() default "rsadvanced_test:empty";
    Completion completion() default Completion.AUTOMATIC;
    // Existing scenarios retain their execution order; new scenarios sort by identifier afterwards.
    int order() default Integer.MAX_VALUE;

    enum Completion {
        AUTOMATIC,
        MANUAL
    }
}
