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
package org.apache.seata.spring.boot.autoconfigure.properties.client;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ExtConfigurationProvider;
import org.apache.seata.config.FileConfiguration;
import org.apache.seata.config.springcloud.SpringApplicationContextProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.LOAD_BALANCE_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 **/
@Import(SpringApplicationContextProvider.class)
@org.springframework.context.annotation.Configuration
public class LoadBalancePropertiesTest {
    private static AnnotationConfigApplicationContext applicationContext;

    @BeforeAll
    public static void initContext() {
        applicationContext = new AnnotationConfigApplicationContext(LoadBalancePropertiesTest.class);
    }

    @Bean
    LoadBalanceProperties loadBalanceProperties() {
        LoadBalanceProperties loadBalanceProperties = new LoadBalanceProperties();
        PROPERTY_BEAN_MAP.put(LOAD_BALANCE_PREFIX, LoadBalanceProperties.class);
        return loadBalanceProperties;
    }

    @Test
    public void testLoadBalanceProperties() {
        FileConfiguration configuration = mock(FileConfiguration.class);
        Configuration currentConfiguration =
            EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
        System.setProperty("seata.client.loadBalance.virtualNodes", "30");
        assertEquals(30, currentConfiguration.getInt("client.loadBalance.virtualNodes"));
        System.setProperty("seata.client.loadBalance.type", "test");
        assertEquals("test", currentConfiguration.getConfig("client.loadBalance.type"));

        LoadBalanceProperties loadBalanceProperties = new LoadBalanceProperties();
        loadBalanceProperties.setType("type");
        Assertions.assertEquals("type", loadBalanceProperties.getType());

        loadBalanceProperties.setVirtualNodes(1);
        Assertions.assertEquals(1, loadBalanceProperties.getVirtualNodes());
    }

    @AfterAll
    public static void closeContext() {
        applicationContext.close();
    }
}
