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
package io.seata.spring.boot.autoconfigure.properties.client;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.config.springcloud.SpringApplicationContextProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static io.seata.spring.boot.autoconfigure.StarterConstants.LOAD_BALANCE_PREFIX;
import static io.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author xingfudeshi@gmail.com
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
        Configuration currentConfiguration = ConfigurationFactory.getInstance();
        System.setProperty("seata.client.loadBalance.virtualNodes", "30");
        assertEquals(30, currentConfiguration.getInt("client.loadBalance.virtualNodes"));
        System.setProperty("seata.client.loadBalance.type", "test");
        assertEquals("test", currentConfiguration.getString("client.loadBalance.type"));
    }

    @AfterAll
    public static void closeContext() {
        applicationContext.close();
    }
}
