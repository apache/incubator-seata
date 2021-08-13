package com.demo.e2e;


import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;


/**
 * This annotation supports the {@link com.demo.docker.annotation.DockerCompose @DockerCompose}, {@link com.demo.docker.annotation.ContainerHost @ContainerHost} and
 * {@link com.demo.docker.annotation.ContainerPort @ContainerPort}, {@link com.demo.docker.annotation.ContainerHostAndPort @ContainerHostAndPort} annotations.
 * You can use {@link SeataE2E} to instead of this annotation, too.
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
 *
 *     @ContainerHostAndPort(name = "service-name2-in-docker-compose.yml", port = 9090)
 *     private HostAndPort someService2HostPort;
 * }
 * }</pre>
 */
public class ContainerExtension implements BeforeAllCallback, AfterAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // context.getRequiredTestInstance() get the actual running object
        ContainersManager.init(context.getRequiredTestInstance());
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        ContainersManager.destroy(context.getRequiredTestInstance());
    }
}
