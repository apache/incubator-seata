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
package org.apache.seata.spring.boot.autoconfigure.properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public class SpringCloudAlibabaConfigurationTest {

    @Test
    public void testSpringCloudAlibabaConfiguration() {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext("org.apache.seata.spring.boot.autoconfigure.properties");
        Properties properties = new Properties();
        properties.setProperty("spring.application.name", "test");
        applicationContext.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("my_test", properties));

        SpringCloudAlibabaConfiguration springCloudAlibabaConfiguration = (SpringCloudAlibabaConfiguration) applicationContext.getBean("springCloudAlibabaConfiguration");

        // application id is null
        Assertions.assertEquals("test", springCloudAlibabaConfiguration.getApplicationId());
        // application is not null
        Assertions.assertEquals("test", springCloudAlibabaConfiguration.getApplicationId());
        Assertions.assertEquals("default_tx_group", springCloudAlibabaConfiguration.getTxServiceGroup());
        springCloudAlibabaConfiguration.setTxServiceGroup("default_tx_group_1");
        Assertions.assertEquals("default_tx_group_1", springCloudAlibabaConfiguration.getTxServiceGroup());
        applicationContext.close();
    }
}
