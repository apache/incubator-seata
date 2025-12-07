/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.discovery.registry;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The type Registry factory test.
 */
public class RegistryFactoryTest {

    private static final String REGISTRY_TYPE_KEY = ConfigurationKeys.FILE_ROOT_REGISTRY
            + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
            + ConfigurationKeys.FILE_ROOT_TYPE;

    @AfterEach
    public void tearDown() {
        System.clearProperty(REGISTRY_TYPE_KEY);
    }

    /**
     * Test getInstance with default config.
     */
    @Test
    public void testGetInstanceWithDefaultConfig() {
        System.setProperty(REGISTRY_TYPE_KEY, RegistryType.File.name());

        RegistryService instance = RegistryFactory.getInstance();
        assertEquals(FileRegistryServiceImpl.class, instance.getClass());
    }

    /**
     * Test buildRegistryService with invalid registry type.
     */
    @Test
    public void testGetInstanceOfInvalidRegistryType() {
        String invalidRegistryType = "InvalidRegistryType";
        System.setProperty(REGISTRY_TYPE_KEY, invalidRegistryType);

        assertThatThrownBy(RegistryFactoryTest::invokeBuildRegistryService)
                .isExactlyInstanceOf(NotSupportYetException.class)
                .hasMessage("not support registry type: " + invalidRegistryType);
    }

    /**
     * Test buildRegistryService with blank registry type.
     * when the registry type is blank, the default registry type is File
     */
    @Test
    public void testGetInstancesWithBlankRegistryType() throws Throwable {
        System.setProperty(REGISTRY_TYPE_KEY, "");

        RegistryService instance = invokeBuildRegistryService();
        assertEquals(FileRegistryServiceImpl.class, instance.getClass());
    }

    /**
     * Use reflection to call the buildRegistryService method
     */
    private static RegistryService invokeBuildRegistryService() throws Throwable {
        Method buildMethod = RegistryFactory.class.getDeclaredMethod("buildRegistryService");

        buildMethod.setAccessible(true);
        try {
            return (RegistryService) buildMethod.invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
