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

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.util.ReflectionUtil;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.FileConfiguration;
import org.apache.seata.spring.boot.autoconfigure.properties.client.RmProperties;
import org.apache.seata.spring.boot.autoconfigure.provider.SpringBootConfigurationProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.seata.common.ConfigurationKeys.XA_BRANCH_EXECUTION_TIMEOUT;
import static org.apache.seata.common.ConfigurationKeys.XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.CLIENT_RM_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClientConfigurationBindingTest {

    private final StandardEnvironment environment = new StandardEnvironment();
    private Object previousEnvironment;
    private Class<?> previousRmProperties;

    @BeforeEach
    void setUp() throws IOException {
        previousEnvironment = ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        previousRmProperties = PROPERTY_BEAN_MAP.put(CLIENT_RM_PREFIX, RmProperties.class);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        Map<String, Object> properties = new HashMap<>();
        for (PropertySource<?> source : new YamlPropertySourceLoader()
                .load("clientExample", new ClassPathResource("client-example/application.yml"))) {
            EnumerablePropertySource<?> yaml = (EnumerablePropertySource<?>) source;
            for (String name : yaml.getPropertyNames()) {
                Object value = yaml.getProperty(name);
                // Preserve the sample keys, but replace default values so a failed lookup cannot pass.
                if (name.startsWith(CLIENT_RM_PREFIX + ".branch-execution-timeout-")) {
                    value = 12345;
                } else if (name.startsWith(CLIENT_RM_PREFIX + ".connection-two-phase-hold-timeout-")) {
                    value = 6789;
                }
                properties.put(name, value);
            }
        }
        environment.getPropertySources().addFirst(new MapPropertySource("clientExample", properties));
    }

    @AfterEach
    void tearDown() throws NoSuchFieldException {
        if (previousEnvironment == null) {
            Map<?, ?> objects = ReflectionUtil.getFieldValue(ObjectHolder.INSTANCE, "OBJECT_MAP");
            objects.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        } else {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, previousEnvironment);
        }
        if (previousRmProperties == null) {
            PROPERTY_BEAN_MAP.remove(CLIENT_RM_PREFIX);
        } else {
            PROPERTY_BEAN_MAP.put(CLIENT_RM_PREFIX, previousRmProperties);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void xaTimeoutsBindFromClientExample(boolean attachConfigurationPropertySources) {
        if (attachConfigurationPropertySources) {
            ConfigurationPropertySources.attach(environment);
        }
        RmProperties properties = Binder.get(environment)
                .bind(CLIENT_RM_PREFIX, RmProperties.class)
                .get();

        assertAll(
                () -> assertEquals(12345, properties.getBranchExecutionTimeoutXA()),
                () -> assertEquals(6789, properties.getConnectionTwoPhaseHoldTimeoutXA()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void xaTimeoutsAreReadByRuntimeConfigurationKeys(boolean attachConfigurationPropertySources) {
        if (attachConfigurationPropertySources) {
            ConfigurationPropertySources.attach(environment);
        }
        Configuration configuration = new SpringBootConfigurationProvider().provide(new FileConfiguration());

        assertAll(
                () -> assertEquals(12345, configuration.getInt(XA_BRANCH_EXECUTION_TIMEOUT)),
                () -> assertEquals(6789, configuration.getInt(XA_CONNECTION_TWO_PHASE_HOLD_TIMEOUT)));
    }
}
