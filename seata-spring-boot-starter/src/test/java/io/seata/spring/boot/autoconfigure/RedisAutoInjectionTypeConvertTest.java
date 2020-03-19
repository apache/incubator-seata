/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.spring.boot.autoconfigure;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.config.Configuration;
import io.seata.config.ExtConfigurationProvider;
import io.seata.config.FileConfiguration;
import io.seata.spring.boot.autoconfigure.properties.registry.RegistryRedisProperties;
import io.seata.spring.boot.autoconfigure.provider.SpringApplicationContextProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author zhangheng
 **/
@Import(SpringApplicationContextProvider.class)
@org.springframework.context.annotation.Configuration
public class RedisAutoInjectionTypeConvertTest {
    private static AnnotationConfigApplicationContext applicationContex;

    @BeforeAll
    public static void initContext() {
        applicationContex = new AnnotationConfigApplicationContext(RedisAutoInjectionTypeConvertTest.class);
    }

    @Bean
    RegistryRedisProperties registryRedisProperties() {
        return new RegistryRedisProperties().setPassword("123456").setDb(1).setServerAddr("localhost:123456");
    }

    @Test
    public void testReadConfigurationItems() {
        FileConfiguration configuration = mock(FileConfiguration.class);
        Configuration currentConfiguration = EnhancedServiceLoader.load(ExtConfigurationProvider.class).provide(configuration);
        assertEquals(1, currentConfiguration.getInt("registry.redis.db"));
        assertEquals("123456", currentConfiguration.getConfig("registry.redis.password"));
        assertEquals("localhost:123456", currentConfiguration.getConfig("registry.redis.serverAddr"));
    }

    @AfterAll
    public static void closeContext() {
        applicationContex.close();
    }
}
