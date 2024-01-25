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
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ExtConfigurationProvider;
import org.apache.seata.config.FileConfiguration;
import org.apache.seata.config.springcloud.SpringApplicationContextProvider;
import org.apache.seata.spring.boot.autoconfigure.properties.registry.RegistryRedisProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_REDIS_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 **/
@Import(SpringApplicationContextProvider.class)
@org.springframework.context.annotation.Configuration
public class RedisAutoInjectionTypeConvertTest {
    private static AnnotationConfigApplicationContext applicationContext;

    @BeforeAll
    public static void initContext() {
        applicationContext = new AnnotationConfigApplicationContext(RedisAutoInjectionTypeConvertTest.class);
    }

    @Bean
    RegistryRedisProperties registryRedisProperties() {
        RegistryRedisProperties registryRedisProperties = new RegistryRedisProperties().setPassword("123456").setDb(1).setServerAddr("localhost:123456");

        PROPERTY_BEAN_MAP.put(REGISTRY_REDIS_PREFIX, RegistryRedisProperties.class);
        return registryRedisProperties;
    }

    @Test
    public void testReadConfigurationItems() {
        FileConfiguration configuration = mock(FileConfiguration.class);
        Configuration currentConfiguration =
            EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
        System.setProperty("seata.registry.redis.db","1");
        assertEquals(1, currentConfiguration.getInt("registry.redis.db"));
        System.setProperty("seata.registry.redis.password","123456");
        assertEquals("123456", currentConfiguration.getConfig("registry.redis.password"));
        System.setProperty("seata.registry.redis.serverAddr","localhost:123456");
        assertEquals("localhost:123456", currentConfiguration.getConfig("registry.redis.serverAddr"));
    }

    @AfterAll
    public static void closeContext() {
        applicationContext.close();
    }
}
