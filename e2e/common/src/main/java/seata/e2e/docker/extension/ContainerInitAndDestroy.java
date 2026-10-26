/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
// This file is originally from Apache SkyWalking
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package seata.e2e.docker.extension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import seata.e2e.config.E2EConfig;
import seata.e2e.docker.annotation.ContainerHostAndPort;
import seata.e2e.docker.annotation.DockerCompose;
import seata.e2e.docker.annotation.DockerContainer;
import seata.e2e.docker.file.DockerComposeFile;
import seata.e2e.docker.file.Yamls;
import seata.e2e.docker.log.ContainerLoggerFactory;
import seata.e2e.model.HostAndPort;

/**
 * This annotation will initialize fields annotated with annotations in {@link seata.e2e.docker.annotation}
 *
 * <pre>{@code
 * @ExtendWith(ContainerInitAndDestroyExtension.class)
 * @TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * public class SomeTest {
 *     @DockerCompose("docker-compose.yml")
 *     private DockerComposeContainer compose;
 *
 *     @ContainerHostAndPort(name = "service-name1-in-docker-compose.yml", port = 8080)
 *     private HostAndPort someService1HostPort;
 * }
 * }</pre>
 * <p>
 * If you don't use the ContainerInitAndDestroyExtension for some reasons, here is an example:
 *
 * <pre>{@code
 * public class SomeTest {
 *     @DockerCompose("docker-compose.yml")
 *     private DockerComposeContainer compose;
 *
 *     @ContainerHostAndPort(name = "service-name1-in-docker-compose.yml", port = 8080)
 *     private HostAndPort someService1HostPort;
 *
 *     @BeforeAll
 *     public void setUp() throws Exception {
 *         ContainerInitAndDestroy.init(this);
 *     }
 *
 *     @AfterAll
 *     public void stop() throws Exception {
 *         ContainerInitAndDestroy.destroy(this);
 *    }
 * }
 * }</pre>
 *
 * @author jingliu_xiong@foxmail.com
 */
public class ContainerInitAndDestroy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerInitAndDestroy.class);
    private static final Path LOG_DIR = Paths.get(E2EConfig.LOG_DIR_ENV);

    static {
        LOGGER.info("IDENTIFIER={}", E2EConfig.IDENTIFIER);
        LOGGER.info("LOG_DIR={}", E2EConfig.LOG_DIR_ENV);
    }

    public static void init(final Object testClass) throws Exception {
        Objects.requireNonNull(testClass, "testClass");

        // Load the docker-compose file and expose services.
        final DockerComposeContainer<?> compose = initDockerComposeField(testClass).orElseThrow(RuntimeException::new);
        compose.start();

        /**
         * Assign the corresponding value to the fileds annotated with {@link com.demo.docker.annotation}, these fileds
         * will be used in test.
         */
        initHostAndPort(testClass, compose);
        initDockerContainers(testClass, compose);
    }

    /**
     * Destroy the containers started by the docker compose in the given test class, this should be typically called in
     * the corresponding {@code @AfterAll} or {@code @AfterEach} method.
     *
     * @param testClass in which the containers should be destroyed
     */
    public static void destroy(final Object testClass) {
        Stream.of(testClass.getClass().getDeclaredFields())
                .filter(ContainerInitAndDestroy::isAnnotatedWithDockerCompose)
                .findFirst()
                .ifPresent(field -> {
                    try {
                        field.setAccessible(true);
                        ((DockerComposeContainer<?>) field.get(testClass)).stop();
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    /**
     * Assign the corresponding value to the fields annotated with {@link DockerContainer}
     *
     * @param testClass
     * @param compose
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private static void initDockerContainers(final Object testClass,
                                             final DockerComposeContainer<?> compose) throws Exception {
        final List<Field> containerFields = Stream.of(testClass.getClass().getDeclaredFields())
                .filter(ContainerInitAndDestroy::isAnnotatedWithDockerContainer)
                .collect(Collectors.toList());
        if (containerFields.isEmpty()) {
            return;
        }

        final Field serviceMap = DockerComposeContainer.class.getDeclaredField("serviceInstanceMap");
        serviceMap.setAccessible(true);
        final Map<String, ContainerState> serviceInstanceMap = (Map<String, ContainerState>) serviceMap.get(compose);

        for (final Field containerField : containerFields) {
            if (containerField.getType() != ContainerState.class) {
                throw new IllegalArgumentException(
                        "@DockerContainer can only be annotated on fields of type " + ContainerState.class.getName()
                                + " but was " + containerField.getType() + "; field \"" + containerField.getName() + "\""
                );
            }
            final DockerContainer dockerContainer = containerField.getAnnotation(DockerContainer.class);
            final String serviceName = dockerContainer.value();
            final Optional<ContainerState> container =
                    serviceInstanceMap.entrySet()
                            .stream()
                            .filter(e -> e.getKey().startsWith(serviceName + "_"))
                            .findFirst()
                            .map(Map.Entry::getValue);
            containerField.setAccessible(true);
            containerField.set(
                    testClass,
                    container.orElseThrow(
                            () -> new NoSuchElementException("cannot find container with name " + serviceName)
                    )
            );
        }
    }

    /**
     * Assign the corresponding value to the fields annotated with {@link ContainerHostAndPort}
     *
     * @param testClass
     * @param compose
     * @throws Exception
     */
    private static void initHostAndPort(final Object testClass,
                                        final DockerComposeContainer<?> compose) throws Exception {
        final Field[] fields = testClass.getClass().getDeclaredFields();
        for (final Field field : fields) {

            if (field.isAnnotationPresent(ContainerHostAndPort.class)) {
                final ContainerHostAndPort hostAndPort = field.getAnnotation(ContainerHostAndPort.class);

                // The service must have been declared using DockerComposeContainer#withExposedService.
                final String host = compose.getServiceHost(hostAndPort.name(), hostAndPort.port());
                final int port = compose.getServicePort(hostAndPort.name(), hostAndPort.port());

                field.setAccessible(true);
                field.set(testClass, new HostAndPort(host, port));
            }
        }
    }


    private static Optional<DockerComposeContainer<?>> initDockerComposeField(final Object testClass) throws Exception {
        final Field[] fields = testClass.getClass().getDeclaredFields();
        final List<Field> dockerComposeFields = Stream.of(fields)
                .filter(ContainerInitAndDestroy::isAnnotatedWithDockerCompose)
                .collect(Collectors.toList());

        if (dockerComposeFields.isEmpty()) {
            return Optional.empty();
        }

        if (dockerComposeFields.size() > 1) {
            throw new RuntimeException("can only have one field annotated with @DockerCompose");
        }

        final Field dockerComposeField = dockerComposeFields.get(0);
        final DockerCompose dockerCompose = dockerComposeField.getAnnotation(DockerCompose.class);

        // Get all docker-compose files
        final List<File> files = Stream.of(dockerCompose.value()).map(Envs::resolve)
                .map(File::new).collect(Collectors.toList());

        final DockerComposeContainer<?> compose = new DockerComposeContainer<>(E2EConfig.IDENTIFIER, files);

        for (final Field field : fields) {
            final WaitStrategy waitStrategy = Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60));
            if (field.isAnnotationPresent(ContainerHostAndPort.class)) {
                final ContainerHostAndPort hostAndPort = field.getAnnotation(ContainerHostAndPort.class);
                    compose.withExposedService(hostAndPort.name(), hostAndPort.port(), waitStrategy);
            }
        }

        compose.withPull(true)
                .withLocalCompose(true)
                .withOptions("--compatibility")
                .withTailChildContainers(true)
                .withRemoveImages(
                        DockerComposeContainer.RemoveImages.LOCAL
                );

        initLoggers(files, compose);

        dockerComposeField.setAccessible(true);
        dockerComposeField.set(testClass, compose);

        return Optional.of(compose);
    }


    /**
     * Initialization of containers logs
     *
     * @param files
     * @param compose
     */
    private static void initLoggers(final List<File> files, final DockerComposeContainer<?> compose) {
        files.forEach(file -> {
            try {

                Yamls.load(file).as(DockerComposeFile.class).getServices().forEach(
                        (service, ignored) -> compose.withLogConsumer(
                                //  The written data is added to the end of the file
                                service, new Slf4jLogConsumer(ContainerLoggerFactory.containerLogger(LOG_DIR, service))
                        )
                );
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }

    public static boolean isAnnotatedWithDockerCompose(final Field field) {
        return field.isAnnotationPresent(DockerCompose.class);
    }

    private static boolean isAnnotatedWithDockerContainer(final Field field) {
        return field.isAnnotationPresent(DockerContainer.class);
    }
}