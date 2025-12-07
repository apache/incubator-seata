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
package org.apache.seata.discovery.registry;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The type Multi registry factory test.
 */
public class MultiRegistryFactoryTest {

    private static final String REGISTRY_TYPE_KEY = ConfigurationKeys.FILE_ROOT_REGISTRY
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
            + ConfigurationKeys.FILE_ROOT_TYPE;

    private final List<Logger> watchedLoggers = new ArrayList<>();
    private final ListAppender<ILoggingEvent> logWatcher = new ListAppender<>();

    @BeforeEach
    void setUp() {
        logWatcher.start();

        Logger logger = ((Logger) LoggerFactory.getLogger(MultiRegistryFactory.class.getName()));
        logger.addAppender(logWatcher);

        watchedLoggers.add(logger);
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(REGISTRY_TYPE_KEY);
        watchedLoggers.forEach(Logger::detachAndStopAllAppenders);
    }

    /**
     * Test getInstances with default config.
     */
    @Test
    public void testGetInstancesWithDefaultConfig() {
        System.setProperty(REGISTRY_TYPE_KEY, RegistryType.File.name());

        List<RegistryService> instances = MultiRegistryFactory.getInstances();
        assertFalse(instances.isEmpty());

        for (RegistryService service : instances) {
            assertEquals(FileRegistryServiceImpl.class, service.getClass());
        }
    }

    @Test
    public void testGetInstancesWithSameRegistryTypes() throws Throwable {
        String sameRegistryType = "File,file";
        System.setProperty(REGISTRY_TYPE_KEY, sameRegistryType);
        List<RegistryService> instances = invokeBuildRegistryServices();

        assertEquals(1, instances.size());
        assertEquals(FileRegistryServiceImpl.class, instances.get(0).getClass());
        assertTrue(getLogs(Level.INFO).isEmpty());
    }

    @Test
    public void testGetInstancesWithDifferentRegistryTypes() throws Throwable {
        String differentRegistryType = "File,file" + Constants.REGISTRY_TYPE_SPLIT_CHAR + RegistryType.Nacos.name();
        System.setProperty(REGISTRY_TYPE_KEY, differentRegistryType);
        List<RegistryService> instances = invokeBuildRegistryServices();

        assertEquals(2, instances.size());
        assertEquals(MockNacosRegistryService.class, instances.get(1).getClass());
        assertEquals(
                "use multi registry center type: [File, Nacos]",
                getLogs(Level.INFO).get(0));
    }

    /**
     * Test buildRegistryServices with blank registry type.
     * when the registry type is blank, the default registry type is File
     */
    @Test
    public void testGetInstancesWithBlankRegistryType() throws Throwable {
        System.setProperty(REGISTRY_TYPE_KEY, "");

        List<RegistryService> instances = invokeBuildRegistryServices();
        assertEquals(FileRegistryServiceImpl.class, instances.get(0).getClass());
    }

    /**
     * Test buildRegistryServices with invalid registry type.
     */
    @Test
    public void testGetInstancesWithInvalidRegistryType() {
        String invalidRegistryType = "InvalidRegistryType";
        System.setProperty(REGISTRY_TYPE_KEY, invalidRegistryType);

        assertThatThrownBy(MultiRegistryFactoryTest::invokeBuildRegistryServices)
                .isExactlyInstanceOf(NotSupportYetException.class)
                .hasMessage("not support registry type: " + invalidRegistryType);
    }

    /**
     * Use reflection to call the buildRegistryServices method
     */
    private static List<RegistryService> invokeBuildRegistryServices() throws Throwable {
        Method buildMethod = MultiRegistryFactory.class.getDeclaredMethod("buildRegistryServices");
        buildMethod.setAccessible(true);

        try {
            return (List<RegistryService>) buildMethod.invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private List<String> getLogs(Level level) {
        return logWatcher.list.stream()
                .filter(event -> event.getLoggerName().endsWith(MultiRegistryFactory.class.getName())
                        && event.getLevel().equals(level))
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }
}
