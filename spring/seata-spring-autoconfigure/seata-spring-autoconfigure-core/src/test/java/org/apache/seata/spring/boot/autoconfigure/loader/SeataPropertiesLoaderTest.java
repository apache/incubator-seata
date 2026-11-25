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
package org.apache.seata.spring.boot.autoconfigure.loader;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.FileConfiguration;
import org.apache.seata.spring.boot.autoconfigure.SeataCoreEnvironmentPostProcessor;
import org.apache.seata.spring.boot.autoconfigure.provider.SpringBootConfigurationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.MapPropertySource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.BOOTSTRAP_PROPERTY_SOURCE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeataPropertiesLoaderTest {

    private Object previousEnvironment;

    @BeforeEach
    void setUp() {
        previousEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        SeataCoreEnvironmentPostProcessor.init();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (previousEnvironment == null) {
            removeEnvironmentFromObjectHolder();
        } else {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, previousEnvironment);
        }
        ConfigurationFactory.reload();
    }

    @Test
    void testOrderIsAfterSpringCloudPropertySourceBootstrap() {
        SeataPropertiesLoader loader = new SeataPropertiesLoader();
        assertEquals(Ordered.LOWEST_PRECEDENCE, loader.getOrder());

        // Simulate Spring Cloud PropertySourceBootstrapConfiguration order (HIGHEST_PRECEDENCE + 10)
        ApplicationContextInitializer<ConfigurableApplicationContext> cloudPropertySourceInitializer =
                new OrderedInitializer(Ordered.HIGHEST_PRECEDENCE + 10);
        List<ApplicationContextInitializer<ConfigurableApplicationContext>> initializers =
                Arrays.asList(loader, cloudPropertySourceInitializer);
        AnnotationAwareOrderComparator.sort(initializers);

        assertSame(cloudPropertySourceInitializer, initializers.get(0));
        assertSame(loader, initializers.get(1));
    }

    @Test
    void testSkipSpringCloudBootstrapContext() throws Exception {
        removeEnvironmentFromObjectHolder();

        try (AnnotationConfigApplicationContext bootstrapContext = new AnnotationConfigApplicationContext()) {
            bootstrapContext
                    .getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource(BOOTSTRAP_PROPERTY_SOURCE_NAME, Collections.emptyMap()));

            SeataPropertiesLoader loader = new SeataPropertiesLoader();
            loader.initialize(bootstrapContext);

            assertNull(ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT));
            assertFalse(bootstrapContext.getEnvironment().getPropertySources().contains("seataOldConfig"));
        }
    }

    @Test
    void testBootstrapThenMainContextLoadsApplicationConfig() throws Exception {
        removeEnvironmentFromObjectHolder();
        SeataPropertiesLoader loader = new SeataPropertiesLoader();

        try (AnnotationConfigApplicationContext bootstrapContext = new AnnotationConfigApplicationContext();
                AnnotationConfigApplicationContext mainContext = new AnnotationConfigApplicationContext()) {
            // 1. Bootstrap context does not contain application.properties
            bootstrapContext
                    .getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource(BOOTSTRAP_PROPERTY_SOURCE_NAME, Collections.emptyMap()));
            loader.initialize(bootstrapContext);
            assertNull(ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT));

            // 2. Main context has application.properties with seata.config.type=nacos
            mainContext
                    .getEnvironment()
                    .getPropertySources()
                    .addFirst(new MapPropertySource(
                            "applicationConfig", Collections.singletonMap("seata.config.type", "nacos")));
            loader.initialize(mainContext);

            assertSame(
                    mainContext.getEnvironment(),
                    ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT));
            assertTrue(mainContext.getEnvironment().getPropertySources().contains("seataOldConfig"));

            Configuration providerConfig =
                    new SpringBootConfigurationProvider().provide(new FileConfiguration("registry", false));
            assertEquals("nacos", providerConfig.getConfig("config.type"));
        }
    }

    @Test
    void testSpringBootConfigurationProviderWhenEnvironmentIsNull() throws Exception {
        removeEnvironmentFromObjectHolder();
        Configuration providerConfig =
                new SpringBootConfigurationProvider().provide(new FileConfiguration("non-existent-config", false));
        // Falls back to ConfigProperties default ("file") instead of throwing NullPointerException
        assertEquals("file", providerConfig.getConfig("config.type"));
    }

    @SuppressWarnings("unchecked")
    private static void removeEnvironmentFromObjectHolder() throws Exception {
        Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        Map<String, Object> objectMap = (Map<String, Object>) field.get(null);
        objectMap.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
    }

    private static class OrderedInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {
        private final int order;

        OrderedInitializer(int order) {
            this.order = order;
        }

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {}

        @Override
        public int getOrder() {
            return order;
        }
    }
}
