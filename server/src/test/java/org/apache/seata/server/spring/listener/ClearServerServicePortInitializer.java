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

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Application listener to clear the SERVER_SERVICE_PORT_CAMEL system property
 * during Spring context environment preparation, failure, and close events,
 * and reserve a probed free test server.port across parallel test forks before ServerApplicationListener runs.
 * It runs with higher priority than ServerApplicationListener.
 */
public class ClearServerServicePortInitializer implements GenericApplicationListener, Ordered {

    static final String TEST_SERVER_PORT_PROPERTY_SOURCE = "testServerPort";
    static final String FORK_NUMBER_PROPERTY = "seata.test.forkNumber";
    static final String PORT_LOCK_DIR_PROPERTY = "seata.test.portLockDir";
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearServerServicePortInitializer.class);
    private static final int MIN_PORT = 12000;
    private static final int MAX_PORT = 32000;
    private static final int BUCKET_COUNT = 8;
    private static final int MAX_PROBE_ATTEMPTS = 64;
    private static final int MAX_SCAN_ATTEMPTS = 256;
    private static final ConcurrentHashMap<Integer, PortReservation> RESERVED_PORTS = new ConcurrentHashMap<>();
    private static final Map<ConfigurableEnvironment, PortReservation> ENV_RESERVATIONS =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final Set<Integer> RECENTLY_ALLOCATED_PORTS = ConcurrentHashMap.newKeySet();

    static {
        Runtime.getRuntime()
                .addShutdownHook(new Thread(
                        ClearServerServicePortInitializer::releaseAllReservations,
                        "seata-test-port-reservation-cleanup"));
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Class<?> rawClass = eventType.getRawClass();
        if (rawClass == null) {
            return false;
        }
        // Listen to environment preparation, application failure, and context close events
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(rawClass)
                || ApplicationFailedEvent.class.isAssignableFrom(rawClass)
                || ContextClosedEvent.class.isAssignableFrom(rawClass);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        ConfigurationCache.clear();
        // Clear the property for any of the supported events
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        LOGGER.info("Cleared system property: " + ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            ApplicationEnvironmentPreparedEvent envEvent = (ApplicationEnvironmentPreparedEvent) event;
            assignAvailableServerPortIfNeeded(envEvent.getEnvironment(), envEvent.getArgs());
        } else if (event instanceof ContextClosedEvent) {
            ApplicationContext context = ((ContextClosedEvent) event).getApplicationContext();
            if (context instanceof ConfigurableApplicationContext) {
                releaseReservation(((ConfigurableApplicationContext) context).getEnvironment());
            }
        } else if (event instanceof ApplicationFailedEvent) {
            ConfigurableApplicationContext context = ((ApplicationFailedEvent) event).getApplicationContext();
            if (context != null) {
                releaseReservation(context.getEnvironment());
            }
        }
    }

    @Override
    public int getOrder() {
        // ServerApplicationListener order is LoggingApplicationListener.DEFAULT_ORDER - 1.
        // This listener needs to run before ServerApplicationListener for ApplicationEnvironmentPreparedEvent.
        return LoggingApplicationListener.DEFAULT_ORDER - 2;
    }

    private void assignAvailableServerPortIfNeeded(ConfigurableEnvironment environment, String[] args) {
        if (hasExplicitPortArg(args)) {
            return;
        }
        if (StringUtils.isNotBlank(environment.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, String.class))
                || StringUtils.isNotBlank(environment.getProperty(ConfigurationKeys.ENV_SEATA_PORT_KEY, String.class))
                || StringUtils.isNotBlank(
                        environment.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CONFIG, String.class))) {
            return;
        }
        String serverPort = environment.getProperty("server.port", String.class);
        if (StringUtils.isBlank(serverPort) || "0".equals(serverPort.trim())) {
            PortReservation reservation = allocateAvailablePortReservation();
            PortReservation previous = ENV_RESERVATIONS.put(environment, reservation);
            if (previous != null && previous != reservation) {
                previous.close();
            }
            int port = reservation.getPort();
            environment
                    .getPropertySources()
                    .addFirst(new MapPropertySource(
                            TEST_SERVER_PORT_PROPERTY_SOURCE,
                            Collections.singletonMap("server.port", String.valueOf(port))));
            LOGGER.info("Assigned available test server.port: {}", port);
        }
    }

    private static boolean hasExplicitPortArg(String[] args) {
        if (args != null && args.length >= 2) {
            for (int i = 0; i < args.length; ++i) {
                if ("-p".equalsIgnoreCase(args[i]) && i < args.length - 1) {
                    return true;
                }
            }
        }
        return false;
    }

    static PortReservation allocateAvailablePortReservation() {
        return allocateAvailablePortReservation(resolveLockDir(), MIN_PORT, MAX_PORT, resolveBucket());
    }

    static PortReservation allocateAvailablePortReservation(Path lockDir, int minPort, int maxPort, int bucket) {
        int totalRange = maxPort - minPort;
        if (totalRange <= 0) {
            throw new IllegalArgumentException("Invalid port range: [" + minPort + ", " + maxPort + ")");
        }
        int bucketCount = Math.min(BUCKET_COUNT, totalRange);
        int bucketSize = Math.max(1, totalRange / bucketCount);
        int normalizedBucket = Math.floorMod(bucket, bucketCount);
        int bucketStart = minPort + normalizedBucket * bucketSize;
        int bucketEnd = normalizedBucket == bucketCount - 1 ? maxPort : Math.min(maxPort, bucketStart + bucketSize);
        int currentBucketRange = bucketEnd - bucketStart;

        if (RECENTLY_ALLOCATED_PORTS.size() > Math.max(1, bucketSize / 2)) {
            RECENTLY_ALLOCATED_PORTS.clear();
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < MAX_PROBE_ATTEMPTS; i++) {
            int candidate = bucketStart + random.nextInt(currentBucketRange);
            PortReservation reservation = reserveIfNotRecent(candidate, lockDir);
            if (reservation != null) {
                return reservation;
            }
        }
        for (int i = 0; i < MAX_PROBE_ATTEMPTS; i++) {
            int candidate = minPort + random.nextInt(totalRange);
            PortReservation reservation = reserveIfNotRecent(candidate, lockDir);
            if (reservation != null) {
                return reservation;
            }
        }
        int scanStart = random.nextInt(totalRange);
        int scanLimit = Math.min(totalRange, MAX_SCAN_ATTEMPTS);
        for (int offset = 0; offset < scanLimit; offset++) {
            int candidate = minPort + ((scanStart + offset) % totalRange);
            PortReservation reservation = tryReservePort(candidate, lockDir);
            if (reservation != null) {
                RECENTLY_ALLOCATED_PORTS.add(candidate);
                return reservation;
            }
        }
        for (int i = 0; i < MAX_PROBE_ATTEMPTS; i++) {
            int candidate;
            try (ServerSocket socket = new ServerSocket(0)) {
                candidate = socket.getLocalPort();
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to allocate an available server port for test", ex);
            }
            PortReservation reservation = tryReservePort(candidate, lockDir);
            if (reservation != null) {
                RECENTLY_ALLOCATED_PORTS.add(candidate);
                return reservation;
            }
        }
        throw new IllegalStateException("Failed to allocate an available server port for test");
    }

    private static PortReservation reserveIfNotRecent(int candidate, Path lockDir) {
        if (RECENTLY_ALLOCATED_PORTS.contains(candidate)) {
            return null;
        }
        PortReservation reservation = tryReservePort(candidate, lockDir);
        if (reservation != null) {
            RECENTLY_ALLOCATED_PORTS.add(candidate);
        }
        return reservation;
    }

    static PortReservation tryReservePort(int port, Path lockDir) {
        synchronized (RESERVED_PORTS) {
            if (RESERVED_PORTS.containsKey(port)) {
                return null;
            }
            Path lockFile = lockDir.resolve("port-" + port + ".lock");
            FileChannel channel = null;
            FileLock lock = null;
            boolean retained = false;
            try {
                Files.createDirectories(lockDir);
                channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                lock = channel.tryLock();
                if (lock == null) {
                    return null;
                }
                if (!isPortAvailable(port)) {
                    return null;
                }
                PortReservation reservation = new PortReservation(port, channel, lock);
                RESERVED_PORTS.put(port, reservation);
                retained = true;
                return reservation;
            } catch (OverlappingFileLockException | IOException ex) {
                LOGGER.debug("Failed to reserve port {} with lock file {}", port, lockFile, ex);
                return null;
            } finally {
                if (!retained) {
                    closeQuietly(lock, channel, port);
                }
            }
        }
    }

    static Path resolveLockDir() {
        String configured = System.getProperty(PORT_LOCK_DIR_PROPERTY);
        if (StringUtils.isNotBlank(configured)) {
            return Paths.get(configured.trim());
        }
        String user = System.getProperty("user.name", "default");
        String sanitizedUser = user.replaceAll("[^a-zA-Z0-9._-]", "_");
        return Paths.get(System.getProperty("java.io.tmpdir"), "seata-server-test-ports-" + sanitizedUser);
    }

    static int resolveBucket() {
        String forkNumber = System.getProperty(FORK_NUMBER_PROPERTY);
        if (StringUtils.isNotBlank(forkNumber)) {
            try {
                return Math.floorMod(Integer.parseInt(forkNumber.trim()), BUCKET_COUNT);
            } catch (NumberFormatException ex) {
                LOGGER.debug("Invalid {} '{}', falling back to PID bucket", FORK_NUMBER_PROPERTY, forkNumber, ex);
            }
        }
        return Math.floorMod(ProcessHandle.current().pid(), BUCKET_COUNT);
    }

    static void releaseReservation(ConfigurableEnvironment environment) {
        if (environment == null) {
            return;
        }
        PortReservation reservation = ENV_RESERVATIONS.remove(environment);
        if (reservation != null) {
            reservation.close();
        }
    }

    static void releaseAllReservations() {
        List<PortReservation> envReservations;
        synchronized (ENV_RESERVATIONS) {
            envReservations = new ArrayList<>(ENV_RESERVATIONS.values());
            ENV_RESERVATIONS.clear();
        }
        for (PortReservation reservation : envReservations) {
            reservation.close();
        }
        List<PortReservation> remaining = new ArrayList<>(RESERVED_PORTS.values());
        for (PortReservation reservation : remaining) {
            reservation.close();
        }
    }

    static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(port), 1);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private static void closeQuietly(FileLock lock, FileChannel channel, int port) {
        if (lock != null) {
            try {
                if (lock.isValid()) {
                    lock.release();
                }
            } catch (IOException ex) {
                LOGGER.debug("Failed to release port lock for port {}", port, ex);
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException ex) {
                LOGGER.debug("Failed to close lock channel for port {}", port, ex);
            }
        }
    }

    static final class PortReservation implements AutoCloseable {

        private final int port;
        private final FileChannel channel;
        private final FileLock lock;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private PortReservation(int port, FileChannel channel, FileLock lock) {
            this.port = port;
            this.channel = channel;
            this.lock = lock;
        }

        int getPort() {
            return port;
        }

        @Override
        public void close() {
            if (!released.compareAndSet(false, true)) {
                return;
            }
            synchronized (RESERVED_PORTS) {
                try {
                    closeQuietly(lock, channel, port);
                } finally {
                    RESERVED_PORTS.remove(port, this);
                }
            }
        }
    }
}
