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
package org.apache.seata.spring.annotation.datasource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.type.AnnotationMetadata;

import static org.junit.jupiter.api.Assertions.*;

class AutoDataSourceProxyRegistrarTest {
    @Test
    void registersAnnotationArgumentsAndPreservesExistingDefinition() {
        DefaultListableBeanFactory registry = new DefaultListableBeanFactory();
        AutoDataSourceProxyRegistrar registrar = new AutoDataSourceProxyRegistrar();
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(Custom.class), registry);
        String name = AutoDataSourceProxyRegistrar.BEAN_NAME_SEATA_AUTO_DATA_SOURCE_PROXY_CREATOR;
        BeanDefinition definition = registry.getBeanDefinition(name);
        assertEquals(SeataAutoDataSourceProxyCreator.class.getName(), definition.getBeanClassName());
        assertEquals(
                true,
                definition
                        .getConstructorArgumentValues()
                        .getIndexedArgumentValues()
                        .get(0)
                        .getValue());
        assertArrayEquals(new String[] {"excluded"}, (String[]) definition
                .getConstructorArgumentValues()
                .getIndexedArgumentValues()
                .get(1)
                .getValue());
        assertEquals(
                "XA",
                definition
                        .getConstructorArgumentValues()
                        .getIndexedArgumentValues()
                        .get(2)
                        .getValue());
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(Default.class), registry);
        assertSame(definition, registry.getBeanDefinition(name));
        assertEquals(1, registry.getBeanDefinitionCount());
    }

    @EnableAutoDataSourceProxy(useJdkProxy = true, excludes = "excluded", dataSourceProxyMode = "XA")
    static class Custom {}

    @EnableAutoDataSourceProxy
    static class Default {}
}
