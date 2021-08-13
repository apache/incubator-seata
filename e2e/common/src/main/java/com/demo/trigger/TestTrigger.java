package com.demo.trigger;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link TestTemplate} in contrast to @Test , it is not itself a test case but rather a template for test cases.
 * {@link ExtendWith} is used to register {@link org.junit.jupiter.api.extension.Extension}.
 */
@Inherited
@Documented
@TestTemplate
@Retention(RUNTIME)
@Target({METHOD, TYPE})
@ExtendWith(TestTriggerExtension.class)
public @interface TestTrigger {
    /**
     * @return the {@link Throwable} class, when this type of throwable is thrown, the test should be retried; if {@link
     * Throwable Throwable.class} is specified, the failed test will be retried when any exception is thrown. {@code
     * Throwable.class} by default
     */
    Class<? extends Throwable> throwable() default Throwable.class;

    /**
     * @return maximum times to retry, or -1 for infinite retries. {@code -1} by default.
     */
    int value() default 1;

    /**
     * @return the interval between any two retries, in millisecond. {@code 1000} by default.
     * No retry at the beginning.
     */
    long interval() default 10000;
}
