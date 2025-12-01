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
package org.apache.seata.config.springcloud;

import org.apache.seata.common.holder.ObjectHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;

class SpringApplicationContextProviderTest {

    @AfterEach
    void tearDown() throws Exception {
        // Clear the OBJECT_MAP in ObjectHolder using reflection
        Field objectMapField = org.apache.seata.common.util.ReflectionUtil.getField(ObjectHolder.class, "OBJECT_MAP");
        objectMapField.setAccessible(true);
        java.util.Map<String, Object> objectMap =
                (java.util.Map<String, Object>) objectMapField.get(ObjectHolder.INSTANCE);
        objectMap.clear();
    }

    @Test
    void testSetApplicationContext() {
        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Environment mockEnvironment = Mockito.mock(Environment.class);
        Mockito.when(mockContext.getEnvironment()).thenReturn(mockEnvironment);

        SpringApplicationContextProvider provider = new SpringApplicationContextProvider();
        provider.setApplicationContext(mockContext);

        ApplicationContext storedContext = ObjectHolder.INSTANCE.getObject(ApplicationContext.class);
        Assertions.assertSame(mockContext, storedContext);

        Environment storedEnvironment =
                (Environment) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        Assertions.assertSame(mockEnvironment, storedEnvironment);
    }

    @Test
    void testSetApplicationContextWhenEnvironmentAlreadySet() {
        Environment existingEnvironment = Mockito.mock(Environment.class);
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, existingEnvironment);

        ApplicationContext mockContext = Mockito.mock(ApplicationContext.class);
        Environment mockEnvironment = Mockito.mock(Environment.class);
        Mockito.when(mockContext.getEnvironment()).thenReturn(mockEnvironment);

        SpringApplicationContextProvider provider = new SpringApplicationContextProvider();
        provider.setApplicationContext(mockContext);

        Environment storedEnvironment =
                (Environment) ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        Assertions.assertSame(existingEnvironment, storedEnvironment);
    }

    @Test
    void testPostProcessBeanFactory() {
        ConfigurableListableBeanFactory mockBeanFactory = Mockito.mock(ConfigurableListableBeanFactory.class);

        SpringApplicationContextProvider provider = new SpringApplicationContextProvider();
        provider.postProcessBeanFactory(mockBeanFactory);
    }
}
