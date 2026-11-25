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
 */
package org.apache.seata.server.spring.listener;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.spring.boot.autoconfigure.SeataCoreEnvironmentPostProcessor;
import org.apache.seata.spring.boot.autoconfigure.SeataServerEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class ClearServerServicePortInitializerTest {

    @Test
    void assignsAvailablePortWhenServerPortIsZeroAndPropagatesConsistently() {
        Object previousEnv = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        String previousPortProp = System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        StandardEnvironment env1 = new StandardEnvironment();
        StandardEnvironment env2 = new StandardEnvironment();
        try (MockedStatic<SeataCoreEnvironmentPostProcessor> core =
                        mockStatic(SeataCoreEnvironmentPostProcessor.class);
                MockedStatic<SeataServerEnvironmentPostProcessor> server =
                        mockStatic(SeataServerEnvironmentPostProcessor.class)) {
            ClearServerServicePortInitializer initializer = new ClearServerServicePortInitializer();
            ServerApplicationListener listener = new ServerApplicationListener();

            env1.getPropertySources()
                    .addFirst(new MapPropertySource("inlined", Collections.singletonMap("server.port", "0")));
            ApplicationEnvironmentPreparedEvent event1 = preparedEvent(env1);

            System.setProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, "19999");
            initializer.onApplicationEvent(event1);

            String port1 = env1.getProperty("server.port");
            assertTrue(port1 != null && Integer.parseInt(port1) > 0);
            assertTrue(ClearServerServicePortInitializer.isPortAvailable(Integer.parseInt(port1)));

            listener.onApplicationEvent(event1);
            assertEquals(port1, env1.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));
            assertEquals(port1, env1.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CONFIG));
            assertEquals(port1, System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));

            env2.getPropertySources()
                    .addFirst(new MapPropertySource("inlined", Collections.singletonMap("server.port", "0")));
            ApplicationEnvironmentPreparedEvent event2 = preparedEvent(env2);

            initializer.onApplicationEvent(event2);
            String port2 = env2.getProperty("server.port");
            assertTrue(port2 != null && Integer.parseInt(port2) > 0);
            assertNotEquals(port1, port2);
        } finally {
            ClearServerServicePortInitializer.releaseReservation(env1);
            ClearServerServicePortInitializer.releaseReservation(env2);
            restoreSpringEnvironment(previousEnv);
            if (previousPortProp == null) {
                System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
            } else {
                System.setProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, previousPortProp);
            }
        }
    }

    @Test
    void doesNotOverrideExplicitServerPortServicePortOrCliArg() {
        String previousPortProp = System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        try {
            ClearServerServicePortInitializer initializer = new ClearServerServicePortInitializer();

            StandardEnvironment explicitServerPortEnv = new StandardEnvironment();
            explicitServerPortEnv
                    .getPropertySources()
                    .addFirst(new MapPropertySource("explicit", Collections.singletonMap("server.port", "18888")));
            ApplicationEnvironmentPreparedEvent explicitServerPortEvent = preparedEvent(explicitServerPortEnv);
            initializer.onApplicationEvent(explicitServerPortEvent);
            assertEquals("18888", explicitServerPortEnv.getProperty("server.port"));
            assertFalse(explicitServerPortEnv
                    .getPropertySources()
                    .contains(ClearServerServicePortInitializer.TEST_SERVER_PORT_PROPERTY_SOURCE));

            StandardEnvironment explicitServicePortEnv = new StandardEnvironment();
            Map<String, Object> serviceProps = new HashMap<>();
            serviceProps.put("server.port", "0");
            serviceProps.put(ConfigurationKeys.SERVER_SERVICE_PORT_CONFIG, "18889");
            explicitServicePortEnv
                    .getPropertySources()
                    .addFirst(new MapPropertySource("explicitService", serviceProps));
            ApplicationEnvironmentPreparedEvent explicitServicePortEvent = preparedEvent(explicitServicePortEnv);
            initializer.onApplicationEvent(explicitServicePortEvent);
            assertEquals("0", explicitServicePortEnv.getProperty("server.port"));
            assertEquals("18889", explicitServicePortEnv.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CONFIG));

            StandardEnvironment cliArgEnv = new StandardEnvironment();
            cliArgEnv
                    .getPropertySources()
                    .addFirst(new MapPropertySource("cli", Collections.singletonMap("server.port", "0")));
            ApplicationEnvironmentPreparedEvent cliArgEvent = preparedEvent(cliArgEnv, "-p", "18890");
            initializer.onApplicationEvent(cliArgEvent);
            assertEquals("0", cliArgEnv.getProperty("server.port"));
            assertFalse(cliArgEnv
                    .getPropertySources()
                    .contains(ClearServerServicePortInitializer.TEST_SERVER_PORT_PROPERTY_SOURCE));
        } finally {
            if (previousPortProp == null) {
                System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
            } else {
                System.setProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, previousPortProp);
            }
        }
    }

    @Test
    void contextClosedAndApplicationFailedReleaseOnlyOwnPortReservation(@TempDir Path tempDir) {
        String previousPortProp = System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        String previousLockDir = System.getProperty(ClearServerServicePortInitializer.PORT_LOCK_DIR_PROPERTY);
        StandardEnvironment env1 = new StandardEnvironment();
        StandardEnvironment env2 = new StandardEnvironment();
        System.setProperty(ClearServerServicePortInitializer.PORT_LOCK_DIR_PROPERTY, tempDir.toString());
        try {
            ClearServerServicePortInitializer initializer = new ClearServerServicePortInitializer();

            env1.getPropertySources()
                    .addFirst(new MapPropertySource("inlined1", Collections.singletonMap("server.port", "0")));
            initializer.onApplicationEvent(preparedEvent(env1));
            int port1 = Integer.parseInt(env1.getProperty("server.port"));

            env2.getPropertySources()
                    .addFirst(new MapPropertySource("inlined2", Collections.singletonMap("server.port", "0")));
            initializer.onApplicationEvent(preparedEvent(env2));
            int port2 = Integer.parseInt(env2.getProperty("server.port"));

            assertNull(ClearServerServicePortInitializer.tryReservePort(port1, tempDir));
            assertNull(ClearServerServicePortInitializer.tryReservePort(port2, tempDir));

            System.setProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, "18891");
            ConfigurableApplicationContext context1 = mock(ConfigurableApplicationContext.class);
            when(context1.getEnvironment()).thenReturn(env1);
            initializer.onApplicationEvent(new ContextClosedEvent(context1));
            assertNull(System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));

            try (ClearServerServicePortInitializer.PortReservation reacquired1 =
                    ClearServerServicePortInitializer.tryReservePort(port1, tempDir)) {
                assertNotNull(reacquired1);
            }
            assertNull(ClearServerServicePortInitializer.tryReservePort(port2, tempDir));

            ConfigurableApplicationContext context2 = mock(ConfigurableApplicationContext.class);
            when(context2.getEnvironment()).thenReturn(env2);
            initializer.onApplicationEvent(new ApplicationFailedEvent(
                    new SpringApplication(ClearServerServicePortInitializerTest.class),
                    new String[0],
                    context2,
                    new IllegalStateException("mock startup failure")));

            try (ClearServerServicePortInitializer.PortReservation reacquired2 =
                    ClearServerServicePortInitializer.tryReservePort(port2, tempDir)) {
                assertNotNull(reacquired2);
            }
        } finally {
            ClearServerServicePortInitializer.releaseReservation(env1);
            ClearServerServicePortInitializer.releaseReservation(env2);
            restoreSystemProperty(ClearServerServicePortInitializer.PORT_LOCK_DIR_PROPERTY, previousLockDir);
            restoreSystemProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, previousPortProp);
        }
    }

    @Test
    void resolveBucketUsesSurefireForkNumberAndFallsBackOnInvalidValue() {
        String previousForkNumber = System.getProperty(ClearServerServicePortInitializer.FORK_NUMBER_PROPERTY);
        try {
            System.setProperty(ClearServerServicePortInitializer.FORK_NUMBER_PROPERTY, "1");
            int bucket1 = ClearServerServicePortInitializer.resolveBucket();
            System.setProperty(ClearServerServicePortInitializer.FORK_NUMBER_PROPERTY, "2");
            int bucket2 = ClearServerServicePortInitializer.resolveBucket();
            assertEquals(1, bucket1);
            assertEquals(2, bucket2);
            assertNotEquals(bucket1, bucket2);

            System.setProperty(ClearServerServicePortInitializer.FORK_NUMBER_PROPERTY, "${surefire.forkNumber}");
            int fallbackBucket = ClearServerServicePortInitializer.resolveBucket();
            assertTrue(fallbackBucket >= 0 && fallbackBucket < 8);
        } finally {
            restoreSystemProperty(ClearServerServicePortInitializer.FORK_NUMBER_PROPERTY, previousForkNumber);
        }
    }

    @Test
    void overlappingForkStartupsReserveDistinctPortsBeforeBind(@TempDir Path tempDir) throws Exception {
        int rangeStart = findAdjacentAvailablePortRange(tempDir);
        Path ready1 = tempDir.resolve("fork1.ready");
        Path ready2 = tempDir.resolve("fork2.ready");
        Path goBind = tempDir.resolve("go.bind");
        Path bound1 = tempDir.resolve("fork1.bound");
        Path bound2 = tempDir.resolve("fork2.bound");
        Path release = tempDir.resolve("release.signal");
        Path log1 = tempDir.resolve("fork1.log");
        Path log2 = tempDir.resolve("fork2.log");

        Process process1 = null;
        Process process2 = null;
        try {
            process1 = startForkWorker(tempDir, rangeStart, rangeStart + 2, 0, ready1, goBind, bound1, release, log1);
            process2 = startForkWorker(tempDir, rangeStart, rangeStart + 2, 0, ready2, goBind, bound2, release, log2);

            int port1 = Integer.parseInt(waitForFileContent(ready1, Duration.ofSeconds(20), process1, log1));
            int port2 = Integer.parseInt(waitForFileContent(ready2, Duration.ofSeconds(20), process2, log2));

            assertTrue(port1 >= rangeStart && port1 < rangeStart + 2);
            assertTrue(port2 >= rangeStart && port2 < rangeStart + 2);
            assertNotEquals(port1, port2);

            assertNull(ClearServerServicePortInitializer.tryReservePort(port1, tempDir));
            assertNull(ClearServerServicePortInitializer.tryReservePort(port2, tempDir));

            Files.write(goBind, "bind".getBytes(StandardCharsets.UTF_8));

            int boundPort1 = Integer.parseInt(waitForFileContent(bound1, Duration.ofSeconds(20), process1, log1));
            int boundPort2 = Integer.parseInt(waitForFileContent(bound2, Duration.ofSeconds(20), process2, log2));
            assertEquals(port1, boundPort1);
            assertEquals(port2, boundPort2);

            Files.write(release, "release".getBytes(StandardCharsets.UTF_8));

            assertTrue(process1.waitFor(20, TimeUnit.SECONDS), () -> "Fork 1 timed out:\n" + readLog(log1));
            assertTrue(process2.waitFor(20, TimeUnit.SECONDS), () -> "Fork 2 timed out:\n" + readLog(log2));
            assertEquals(0, process1.exitValue(), () -> "Fork 1 failed:\n" + readLog(log1));
            assertEquals(0, process2.exitValue(), () -> "Fork 2 failed:\n" + readLog(log2));

            try (ClearServerServicePortInitializer.PortReservation reacquired1 =
                            ClearServerServicePortInitializer.tryReservePort(port1, tempDir);
                    ClearServerServicePortInitializer.PortReservation reacquired2 =
                            ClearServerServicePortInitializer.tryReservePort(port2, tempDir)) {
                assertNotNull(reacquired1);
                assertNotNull(reacquired2);
            }
        } finally {
            destroyQuietly(process1);
            destroyQuietly(process2);
        }
    }

    private static int findAdjacentAvailablePortRange(Path lockDir) {
        for (int candidate = 20000; candidate < 30000; candidate += 2) {
            try (ClearServerServicePortInitializer.PortReservation first =
                    ClearServerServicePortInitializer.tryReservePort(candidate, lockDir)) {
                if (first == null) {
                    continue;
                }
                try (ClearServerServicePortInitializer.PortReservation second =
                        ClearServerServicePortInitializer.tryReservePort(candidate + 1, lockDir)) {
                    if (second != null) {
                        return candidate;
                    }
                }
            }
        }
        throw new IllegalStateException("Failed to find two adjacent available ports for fork test");
    }

    private static Process startForkWorker(
            Path lockDir,
            int minPort,
            int maxPort,
            int bucket,
            Path readyFile,
            Path goBindFile,
            Path boundFile,
            Path releaseFile,
            Path logFile)
            throws IOException {
        Path javaBin = Paths.get(System.getProperty("java.home"), "bin", "java");
        ProcessBuilder pb = new ProcessBuilder(
                javaBin.toString(),
                "-cp",
                System.getProperty("java.class.path"),
                PortReservationForkWorker.class.getName(),
                lockDir.toString(),
                String.valueOf(minPort),
                String.valueOf(maxPort),
                String.valueOf(bucket),
                readyFile.toString(),
                goBindFile.toString(),
                boundFile.toString(),
                releaseFile.toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFile.toFile());
        return pb.start();
    }

    private static String waitForFileContent(Path file, Duration timeout, Process process, Path logFile)
            throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (Files.exists(file)) {
                String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8).trim();
                if (!content.isEmpty()) {
                    return content;
                }
            }
            if (!process.isAlive()) {
                throw new AssertionError(
                        "Fork worker exited prematurely with code " + process.exitValue() + ":\n" + readLog(logFile));
            }
            Thread.sleep(20L);
        }
        throw new AssertionError("Timed out waiting for file " + file + ":\n" + readLog(logFile));
    }

    private static String readLog(Path logFile) {
        if (!Files.exists(logFile)) {
            return "<no log>";
        }
        try {
            return new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "<failed to read log: " + ex.getMessage() + ">";
        }
    }

    private static void destroyQuietly(Process process) {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

    private static void restoreSystemProperty(String key, String previousValue) {
        if (previousValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previousValue);
        }
    }

    private static ApplicationEnvironmentPreparedEvent preparedEvent(StandardEnvironment env, String... args) {
        ApplicationEnvironmentPreparedEvent event = mock(ApplicationEnvironmentPreparedEvent.class);
        when(event.getEnvironment()).thenReturn(env);
        when(event.getArgs()).thenReturn(args);
        return event;
    }

    @SuppressWarnings("unchecked")
    private static void restoreSpringEnvironment(Object previousEnvironment) {
        if (previousEnvironment != null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, previousEnvironment);
        } else {
            Map<String, Object> objects =
                    (Map<String, Object>) ReflectionTestUtils.getField(ObjectHolder.class, "OBJECT_MAP");
            if (objects != null) {
                objects.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
            }
        }
    }

    public static final class PortReservationForkWorker {

        public static void main(String[] args) throws Exception {
            Path lockDir = Paths.get(args[0]);
            int minPort = Integer.parseInt(args[1]);
            int maxPort = Integer.parseInt(args[2]);
            int bucket = Integer.parseInt(args[3]);
            Path readyFile = Paths.get(args[4]);
            Path goBindFile = Paths.get(args[5]);
            Path boundFile = Paths.get(args[6]);
            Path releaseFile = Paths.get(args[7]);

            try (ClearServerServicePortInitializer.PortReservation reservation =
                    ClearServerServicePortInitializer.allocateAvailablePortReservation(
                            lockDir, minPort, maxPort, bucket)) {
                int port = reservation.getPort();
                writeAtomically(readyFile, String.valueOf(port));
                waitForSignal(goBindFile, 20_000L);
                try (ServerSocket socket = new ServerSocket()) {
                    socket.setReuseAddress(false);
                    socket.bind(new InetSocketAddress(port), 1);
                    writeAtomically(boundFile, String.valueOf(socket.getLocalPort()));
                    waitForSignal(releaseFile, 20_000L);
                }
            }
        }

        private static void writeAtomically(Path target, String content) throws IOException {
            Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
            Files.write(tmp, content.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }

        private static void waitForSignal(Path signalFile, long timeoutMillis) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            while (System.nanoTime() < deadline) {
                if (Files.exists(signalFile)) {
                    return;
                }
                Thread.sleep(20L);
            }
            throw new IllegalStateException("Timed out waiting for signal file: " + signalFile);
        }
    }
}
