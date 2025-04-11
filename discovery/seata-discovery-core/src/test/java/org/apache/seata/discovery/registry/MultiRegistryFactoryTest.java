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
import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
public class MultiRegistryFactoryTest {


    @Test
    public void testGetInstance() throws ClassNotFoundException {
        Class.forName("org.apache.seata.discovery.registry.MultiRegistryFactory$MultiRegistryFactoryHolder");
        List<RegistryService> instances = MultiRegistryFactory.getInstances();
        Assertions.assertNotNull(instances);
        assertFalse(instances.isEmpty());
        for (RegistryService registryService : instances) {
            Assertions.assertNotNull(registryService);
        }


    }
    @Test
    public void testGetInstanceNotSupport() throws Exception {
        Object originalConfig = getStaticFieldValue(ConfigurationFactory.class, "CURRENT_FILE_INSTANCE");
        Configuration mockConfig = mock(Configuration.class);
        setStaticFieldValue(ConfigurationFactory.class, "CURRENT_FILE_INSTANCE", mockConfig);
        when(mockConfig.getConfig("registry.type")).thenReturn("file,aaa");
        try {
            ExceptionInInitializerError error = assertThrows(ExceptionInInitializerError.class, RegistryFactory::getInstance);
            assertInstanceOf(NotSupportYetException.class, error.getCause());
            assertEquals("not support registry type: file,aaa", error.getCause().getMessage());
        } finally {
            setStaticFieldValue(ConfigurationFactory.class, "CURRENT_FILE_INSTANCE", originalConfig);
        }
    }

    private static <T> T getStaticFieldValue(Class<?> clazz, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(null);
    }

    private static void setStaticFieldValue(Class<?> clazz, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
