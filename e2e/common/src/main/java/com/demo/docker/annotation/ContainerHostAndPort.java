package com.demo.docker.annotation;


import com.demo.e2e.ContainerExtension;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Fields of type {@link com.demo.model.HostAndPort} annotated with {@link ContainerHostAndPort @ContainerHostAndPort} can be
 * initialized by {@link ContainerExtension} with the real host and port of the docker container, whose original {@link
 * #name() service name} and {@link #port() exposed port}  defined in {@code docker-compose.yml} are given, for more
 * details and examples, refer to the JavaDoc of {@link ContainerExtension}.
 */
@Documented
@Target(FIELD)
@Retention(RUNTIME)
public @interface ContainerHostAndPort {
    /**
     * @return the original name that is defined in {@code docker-compose.yml}.
     */
    String name();

    /**
     * @return the original port that is exposed in {@code docker-compose.yml}.
     */
    int port();
}
