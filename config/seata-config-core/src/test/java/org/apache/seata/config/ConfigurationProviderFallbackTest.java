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
package org.apache.seata.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigurationProviderFallbackTest {
    @Test
    void absentFileProviderIsAnExpectedFallback() throws Exception {
        assertMissingProvider("file", new EnhancedServiceNotFoundException("missing provider"), false);
    }

    @Test
    void absentRemoteProviderRemainsAnError() throws Exception {
        assertMissingProvider("nacos", new EnhancedServiceNotFoundException("missing provider"), true);
    }

    @Test
    void brokenFileProviderRemainsAnError() throws Exception {
        assertMissingProvider(
                "file",
                new EnhancedServiceNotFoundException(
                        "failed provider", new IllegalStateException("initialization failed")),
                true);
    }

    @Test
    void explicitFileProviderStillTakesPrecedence() throws Exception {
        ConfigurationFactory.getInstance();
        ConfigurationProvider provider = mock(ConfigurationProvider.class);
        Configuration configuration = mock(Configuration.class);
        when(provider.provide()).thenReturn(configuration);
        try (MockedStatic<EnhancedServiceLoader> services = mockStatic(EnhancedServiceLoader.class)) {
            services.when(() -> EnhancedServiceLoader.load(io.seata.config.ConfigurationProvider.class, "file"))
                    .thenThrow(new EnhancedServiceNotFoundException("missing legacy provider"));
            services.when(() -> EnhancedServiceLoader.load(ConfigurationProvider.class, "file", false))
                    .thenReturn(provider);
            assertSame(configuration, getNonSpringConfiguration("file"));
        }
    }

    private void assertMissingProvider(String type, EnhancedServiceNotFoundException failure, boolean expectError)
            throws Exception {
        ConfigurationFactory.getInstance();
        Logger logger = (Logger) LoggerFactory.getLogger(ConfigurationFactory.class);
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        try (MockedStatic<EnhancedServiceLoader> services = mockStatic(EnhancedServiceLoader.class)) {
            services.when(() -> EnhancedServiceLoader.load(io.seata.config.ConfigurationProvider.class, type))
                    .thenThrow(new EnhancedServiceNotFoundException("missing legacy provider"));
            services.when(() -> EnhancedServiceLoader.load(ConfigurationProvider.class, type, false))
                    .thenThrow(failure);
            assertNull(getNonSpringConfiguration(type));
            assertEquals(expectError, events.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR));
        } finally {
            logger.detachAppender(events);
            events.stop();
        }
    }

    private Object getNonSpringConfiguration(String type) throws Exception {
        Method method = ConfigurationFactory.class.getDeclaredMethod("getNonSpringConfiguration", String.class);
        method.setAccessible(true);
        return method.invoke(null, type);
    }
}
