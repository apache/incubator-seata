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
import org.mockito.MockedStatic;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class ClearServerServicePortInitializerTest {

    @Test
    void assignsAvailablePortWhenServerPortIsZeroAndPropagatesConsistently() {
        Object previousEnv = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        String previousPortProp = System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        try (MockedStatic<SeataCoreEnvironmentPostProcessor> core =
                        mockStatic(SeataCoreEnvironmentPostProcessor.class);
                MockedStatic<SeataServerEnvironmentPostProcessor> server =
                        mockStatic(SeataServerEnvironmentPostProcessor.class)) {
            ClearServerServicePortInitializer initializer = new ClearServerServicePortInitializer();
            ServerApplicationListener listener = new ServerApplicationListener();

            StandardEnvironment env1 = new StandardEnvironment();
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

            StandardEnvironment env2 = new StandardEnvironment();
            env2.getPropertySources()
                    .addFirst(new MapPropertySource("inlined", Collections.singletonMap("server.port", "0")));
            ApplicationEnvironmentPreparedEvent event2 = preparedEvent(env2);

            initializer.onApplicationEvent(event2);
            String port2 = env2.getProperty("server.port");
            assertTrue(port2 != null && Integer.parseInt(port2) > 0);
            assertNotEquals(port1, port2);
        } finally {
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
    void contextClosedEventClearsServicePortSystemPropertyOnly() {
        String previousPortProp = System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        try {
            System.setProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, "18891");
            ClearServerServicePortInitializer initializer = new ClearServerServicePortInitializer();
            initializer.onApplicationEvent(new ContextClosedEvent(mock(ConfigurableApplicationContext.class)));
            assertNull(System.getProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL));
        } finally {
            if (previousPortProp == null) {
                System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
            } else {
                System.setProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, previousPortProp);
            }
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
}
