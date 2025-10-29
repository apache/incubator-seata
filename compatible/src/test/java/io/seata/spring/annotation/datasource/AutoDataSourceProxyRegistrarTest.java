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
package io.seata.spring.annotation.datasource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for AutoDataSourceProxyRegistrar.
 */
public class AutoDataSourceProxyRegistrarTest {

    @Test
    public void testImplementsImportBeanDefinitionRegistrar() {
        assertTrue(
                org.springframework.context.annotation.ImportBeanDefinitionRegistrar.class.isAssignableFrom(
                        AutoDataSourceProxyRegistrar.class),
                "AutoDataSourceProxyRegistrar should implement ImportBeanDefinitionRegistrar");
    }

    @Test
    public void testRegisterBeanDefinitions() {
        AutoDataSourceProxyRegistrar registrar = new AutoDataSourceProxyRegistrar();
        AnnotationMetadata mockMetadata = mock(AnnotationMetadata.class);
        BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

        // Mock the annotation attributes to avoid NPE
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("useJdkProxy", true);
        attributes.put("excludes", new String[] {});
        attributes.put("dataSourceProxyMode", "AT");
        when(mockMetadata.getAnnotationAttributes(EnableAutoDataSourceProxy.class.getName()))
                .thenReturn(attributes);

        // Should not throw exception with proper mocked attributes
        assertDoesNotThrow(() -> registrar.registerBeanDefinitions(mockMetadata, registry));
    }

    @Test
    public void testRegisterBeanDefinitionsWithNullMetadata() {
        AutoDataSourceProxyRegistrar registrar = new AutoDataSourceProxyRegistrar();
        BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

        // Null metadata will cause NPE, which is expected behavior
        assertThrows(NullPointerException.class, () -> registrar.registerBeanDefinitions(null, registry));
    }

    @Test
    public void testRegisterBeanDefinitionsWithRealRegistry() {
        AutoDataSourceProxyRegistrar registrar = new AutoDataSourceProxyRegistrar();
        AnnotationMetadata mockMetadata = mock(AnnotationMetadata.class);
        SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

        // Mock the annotation attributes to avoid NPE
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("useJdkProxy", false);
        attributes.put("excludes", new String[] {"excludedDataSource"});
        attributes.put("dataSourceProxyMode", "XA");
        when(mockMetadata.getAnnotationAttributes(EnableAutoDataSourceProxy.class.getName()))
                .thenReturn(attributes);

        registrar.registerBeanDefinitions(mockMetadata, registry);

        // Verify that the bean has been registered
        assertTrue(registry.containsBeanDefinition(
                AutoDataSourceProxyRegistrar.BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR));
    }

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                AutoDataSourceProxyRegistrar.class.isAnnotationPresent(Deprecated.class),
                "AutoDataSourceProxyRegistrar should be marked as @Deprecated");
    }
}
