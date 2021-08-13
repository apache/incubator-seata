package com.demo.e2e;

import com.demo.docker.annotation.*;
import com.demo.docker.file.DockerComposeFile;
import com.demo.docker.log.ContainerLogger;
import com.demo.model.HostAndPort;
import com.demo.utils.Envs;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.shaded.com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.demo.utils.Yamls.load;

/**
 * This annotation will initialize fields annotated with annotations in {@link com.demo.docker.annotation}
 *
 * <pre>{@code
 * @ExtendWith(ContainerExtension.class)
 * @TestInstance(TestInstance.Lifecycle.PER_CLASS)
 * public class SomeTest {
 *     @DockerCompose("docker-compose.yml")
 *     private DockerComposeContainer justForSideEffects;
 *
 *     @ContainerHostAndPort(name = "service-name1-in-docker-compose.yml", port = 8080)
 *     private HostAndPort someService1HostPort;
 * }
 * }</pre>
 *
 * If you don't use the ContainerExtension for some reasons, here is an example:
 *
 * <pre>{@code
 * public class SomeTest {
 *     @DockerCompose("docker/simple/docker-compose.yml")
 *     private DockerComposeContainer justForSideEffects;
 *
 *     @ContainerHostAndPort(name = "service-name1-in-docker-compose.yml", port = 8080)
 *     private HostAndPort someService1HostPort;
 *
 *     @BeforeAll
 *     public void setUp() throws Exception {
 *         ContainersManager.init(this);
 *     }
 *
 *     @AfterAll
 *     public void setUp() throws Exception {
 *         ContainersManager.destroy(this);
 *    }
 * }
 * }</pre>
 */
@Slf4j
public class ContainersManager {

    // Use a unique identifier so that containers created for this compose environment can be identified.
    private static final String IDENTIFIER =
            !Strings.isNullOrEmpty(System.getenv("SEATA_E2E_TEST_ID"))
                    ? System.getenv("SEATA_E2E_TEST_ID") : "seata-e2e-";



    private static final String LOG_DIR_ENV =
            !Strings.isNullOrEmpty(System.getenv("SEATA_E2E_WORKSPACE"))
                    ? (System.getenv("SEATA_E2E_WORKSPACE") + "/logs") : "/tmp/seata/logs";

    // Containers log dir
    private static final Path LOG_DIR = Paths.get(LOG_DIR_ENV);

    static {
        log.info("IDENTIFIER={}", IDENTIFIER);
        log.info("LOG_DIR={}", LOG_DIR);
    }

    public static void init(final Object testClass) throws Exception {
        Objects.requireNonNull(testClass, "testClass");

        // Load the docker-compose file and expose services.
        final DockerComposeContainer<?> compose = initDockerComposeField(testClass).orElseThrow(RuntimeException::new);

        compose.start();

        // Assign the corresponding value to the fileds annotated with {@link com.demo.docker.annotation}.
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
                .filter(ContainersManager::isAnnotatedWithDockerCompose)
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
     * Assign the corresponding value to the fileds annotated with {@link com.demo.docker.annotation.DockerContainer}
     * @param testClass
     * @param compose
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private static void initDockerContainers(final Object testClass,
                                             final DockerComposeContainer<?> compose) throws Exception {
        final List<Field> containerFields = Stream.of(testClass.getClass().getDeclaredFields())
                .filter(ContainersManager::isAnnotatedWithDockerContainer)
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
     * Assign the corresponding value to the fileds annotated with
     * {@link com.demo.docker.annotation.ContainerHost,ContainerPort,ContainerHostAndPort}
     * @param testClass
     * @param compose
     * @throws Exception
     */
    private static void initHostAndPort(final Object testClass,
                                        final DockerComposeContainer<?> compose) throws Exception {
        final Field[] fields = testClass.getClass().getDeclaredFields();
        for (final Field field : fields) {
            if (field.isAnnotationPresent(ContainerHost.class) && field.isAnnotationPresent(ContainerPort.class)) {
                throw new RuntimeException(
                        "field cannot be annotated with both ContainerHost and ContainerPort: " + field.getName()
                );
            }
            if (field.isAnnotationPresent(ContainerHost.class)) {
                final ContainerHost host = field.getAnnotation(ContainerHost.class);
                field.setAccessible(true);
                field.set(testClass, compose.getServiceHost(host.name(), host.port()));
            }
            if (field.isAnnotationPresent(ContainerPort.class)) {
                final ContainerPort host = field.getAnnotation(ContainerPort.class);
                field.setAccessible(true);
                field.set(testClass, compose.getServicePort(host.name(), host.port()));
            }
            if (field.isAnnotationPresent(ContainerHostAndPort.class)) {
                final ContainerHostAndPort hostAndPort = field.getAnnotation(ContainerHostAndPort.class);

                // The service must have been declared using DockerComposeContainer#withExposedService.
                final String host = compose.getServiceHost(hostAndPort.name(), hostAndPort.port());
                final int port = compose.getServicePort(hostAndPort.name(), hostAndPort.port());

                field.setAccessible(true);
                field.set(testClass, HostAndPort.builder().host(host).port(port).build());
            }
        }
    }


    private static Optional<DockerComposeContainer<?>> initDockerComposeField(final Object testClass) throws Exception {
        final Field[] fields = testClass.getClass().getDeclaredFields();
        final List<Field> dockerComposeFields = Stream.of(fields)
                .filter(ContainersManager::isAnnotatedWithDockerCompose)
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

        final DockerComposeContainer<?> compose = new DockerComposeContainer<>(IDENTIFIER, files);

        for (final Field field : fields) {
            if (field.isAnnotationPresent(ContainerHost.class) && field.isAnnotationPresent(ContainerPort.class)) {
                throw new RuntimeException(
                        "field cannot be annotated with both ContainerHost and ContainerPort: " + field.getName()
                );
            }

            //
            final WaitStrategy waitStrategy = Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5));
            if (field.isAnnotationPresent(ContainerHost.class)) {
                final ContainerHost host = field.getAnnotation(ContainerHost.class);
                // Expose Service on Ambassador container.
                compose.withExposedService(host.name(), host.port(), waitStrategy);
            }
            if (field.isAnnotationPresent(ContainerPort.class)) {
                final ContainerPort port = field.getAnnotation(ContainerPort.class);
                compose.withExposedService(port.name(), port.port(), waitStrategy);
            }
            if (field.isAnnotationPresent(ContainerHostAndPort.class)) {
                final ContainerHostAndPort hostAndPort = field.getAnnotation(ContainerHostAndPort.class);
                compose.withExposedService(hostAndPort.name(), hostAndPort.port(), waitStrategy);
            }
        }

        compose.withPull(true)
                .withLocalCompose(true)
                .withTailChildContainers(true)
                .withRemoveImages(
                        DockerComposeContainer.RemoveImages.LOCAL
                );

        initLoggers(files, compose);

        dockerComposeField.setAccessible(true);
        dockerComposeField.set(testClass, compose);

        return Optional.of(compose);
    }


    // Initialization of containers logs
    private static void initLoggers(final List<File> files, final DockerComposeContainer<?> compose) {
        files.forEach(file -> {
            try {

                load(file).as(DockerComposeFile.class).getServices().forEach(
                        (service, ignored) -> compose.withLogConsumer(
                                //  The written data is added to the end of the file
                                service, new Slf4jLogConsumer(new ContainerLogger(LOG_DIR, service + ".log"))
                        )
                );
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        });
    }

    private static boolean isAnnotatedWithDockerCompose(final Field field) {
        return field.isAnnotationPresent(DockerCompose.class);
    }

    private static boolean isAnnotatedWithDockerContainer(final Field field) {
        return field.isAnnotationPresent(DockerContainer.class);
    }
}
