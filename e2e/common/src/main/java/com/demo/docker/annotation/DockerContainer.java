
package com.demo.docker.annotation;


import org.testcontainers.containers.ContainerState;
import com.demo.e2e.ContainerExtension;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Fields of type {@link ContainerState} annotated with {@link DockerContainer @DockerContainer} can be initialized by
 * {@link ContainerExtension} with the docker container, whose {@link #value() service name} defined in {@code
 * docker-compose.yml} are given, for more details and examples, refer to the JavaDoc of {@link ContainerExtension}.
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface DockerContainer {
    String value();
}
