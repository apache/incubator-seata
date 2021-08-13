
package com.demo.docker.annotation;

import com.demo.e2e.ContainerExtension;
import org.testcontainers.containers.DockerComposeContainer;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Fields of type {@link DockerComposeContainer} annotated with {@link DockerCompose @DockerCompose} can be initialized
 * by {@link ContainerExtension} with the given {@link #value() docker-compose.yml} files, for more details and
 * exampless, refer to the JavaDoc of {@link ContainerExtension}.
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface DockerCompose {
    /**
     * @return the {@code docker-compose.yml} files
     */
    String[] value();
}
