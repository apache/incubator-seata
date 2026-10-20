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
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.spring.boot.autoconfigure.SeataCoreEnvironmentPostProcessor;
import org.apache.seata.spring.boot.autoconfigure.SeataServerEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.context.event.*;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.StandardEnvironment;

import java.util.*;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.core.constants.ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerEnvironmentUnitTest extends BaseSpringBootTest {
    @Test
    void commandLinePortPrecedesEnvironmentAndReadyMarksBootCompletion() {
        Object previous = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        Map<String, String> properties = new HashMap<>();
        for (String key :
                Arrays.asList(SERVER_SERVICE_PORT_CAMEL, "production.deploy.output", "ENV_LOG_SYS_BOOT_COMPLETED")) {
            properties.put(key, System.getProperty(key));
        }
        try (MockedStatic<SeataCoreEnvironmentPostProcessor> core =
                        mockStatic(SeataCoreEnvironmentPostProcessor.class);
                MockedStatic<SeataServerEnvironmentPostProcessor> server =
                        mockStatic(SeataServerEnvironmentPostProcessor.class)) {
            ServerApplicationListener listener = new ServerApplicationListener();
            StandardEnvironment env = new StandardEnvironment();
            ApplicationEnvironmentPreparedEvent event = mock(ApplicationEnvironmentPreparedEvent.class);
            when(event.getEnvironment()).thenReturn(env);
            when(event.getArgs()).thenReturn(new String[] {"-p", "9191"});
            listener.onApplicationEvent(event);
            assertEquals("9191", System.getProperty(SERVER_SERVICE_PORT_CAMEL));
            assertSame(env, ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT));
            assertTrue(listener.supportsEventType(ResolvableType.forClass(ApplicationReadyEvent.class)));
            assertFalse(listener.supportsEventType(ResolvableType.forClass(ApplicationEvent.class)));
            assertFalse(listener.supportsEventType(ResolvableType.NONE));
            System.setProperty("production.deploy.output", "true");
            listener.onApplicationEvent(mock(ApplicationReadyEvent.class));
            assertEquals("true", System.getProperty("ENV_LOG_SYS_BOOT_COMPLETED"));
            assertDoesNotThrow(() -> listener.onApplicationEvent(mock(ApplicationEvent.class)));
        } finally {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, previous);
            properties.forEach((key, value) -> {
                if (value == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, value);
                }
            });
        }
    }
}
