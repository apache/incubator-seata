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

import io.seata.spring.boot.autoconfigure.properties.SeataProperties;
import io.seata.spring.boot.autoconfigure.properties.SpringCloudAlibabaConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xingfudeshi@gmail.com
 */
@Configuration(proxyBeanMethods = false)
public class PropertyBeanPostProcessorTest {
    private static AnnotationConfigApplicationContext context;

    @BeforeAll
    public static void initContext() {
        context = new AnnotationConfigApplicationContext(PropertyBeanPostProcessorTest.class);
    }


    @Bean
    public SeataProperties seataProperties() {
        SeataProperties seataProperties = new SeataProperties();
        seataProperties.setApplicationId("test-id");
        return seataProperties;
    }

    @Bean
    public SpringCloudAlibabaConfiguration springCloudAlibabaConfiguration() {
        return new SpringCloudAlibabaConfiguration();
    }

    @AfterAll
    public static void closeContext() {
        context.close();
    }
}
