package com.demo.e2e;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Use this annotation to provide ContainerExtension for all test methods under a class.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ContainerExtension.class)
@Timeout(value = 10, unit = TimeUnit.MINUTES)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public @interface SeataE2E {
}
