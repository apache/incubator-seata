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
import org.springframework.boot.context.logging.LoggingApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Application listener to clear the SERVER_SERVICE_PORT_CAMEL system property
 * during Spring context environment preparation, refresh, and close events,
 * and allocate a probed free test server.port before ServerApplicationListener runs.
 * It runs with higher priority than ServerApplicationListener.
 */
public class ClearServerServicePortInitializer implements GenericApplicationListener, Ordered {

    static final String TEST_SERVER_PORT_PROPERTY_SOURCE = "testServerPort";
    private static final int MIN_PORT = 12000;
    private static final int MAX_PORT = 32000;
    private static final int BUCKET_COUNT = 8;
    private static final int BUCKET_SIZE = (MAX_PORT - MIN_PORT) / BUCKET_COUNT;
    private static final int MAX_PROBE_ATTEMPTS = 64;
    private static final Set<Integer> ALLOCATED_PORTS = ConcurrentHashMap.newKeySet();

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Class<?> rawClass = eventType.getRawClass();
        if (rawClass == null) {
            return false;
        }
        // Listen to environment preparation, context refresh, and context close events
        return ApplicationEnvironmentPreparedEvent.class.isAssignableFrom(rawClass)
                || ContextClosedEvent.class.isAssignableFrom(rawClass);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        ConfigurationCache.clear();
        // Clear the property for any of the supported events
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        log.info("Cleared system property: " + ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            ApplicationEnvironmentPreparedEvent envEvent = (ApplicationEnvironmentPreparedEvent) event;
            assignAvailableServerPortIfNeeded(envEvent.getEnvironment(), envEvent.getArgs());
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
            int port = allocateAvailablePort();
            environment
                    .getPropertySources()
                    .addFirst(new MapPropertySource(
                            TEST_SERVER_PORT_PROPERTY_SOURCE,
                            Collections.singletonMap("server.port", String.valueOf(port))));
            log.info("Assigned available test server.port: {}", port);
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

    static int allocateAvailablePort() {
        if (ALLOCATED_PORTS.size() > BUCKET_SIZE / 2) {
            ALLOCATED_PORTS.clear();
        }
        int bucket = Math.floorMod(ProcessHandle.current().pid(), BUCKET_COUNT);
        int bucketStart = MIN_PORT + bucket * BUCKET_SIZE;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < MAX_PROBE_ATTEMPTS; i++) {
            int candidate = bucketStart + random.nextInt(BUCKET_SIZE);
            if (!ALLOCATED_PORTS.contains(candidate) && isPortAvailable(candidate) && ALLOCATED_PORTS.add(candidate)) {
                return candidate;
            }
        }
        for (int i = 0; i < MAX_PROBE_ATTEMPTS; i++) {
            int candidate = MIN_PORT + random.nextInt(MAX_PORT - MIN_PORT);
            if (!ALLOCATED_PORTS.contains(candidate) && isPortAvailable(candidate) && ALLOCATED_PORTS.add(candidate)) {
                return candidate;
            }
        }
        try (ServerSocket socket = new ServerSocket(0)) {
            int port = socket.getLocalPort();
            ALLOCATED_PORTS.add(port);
            return port;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to allocate an available server port for test", ex);
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
}
