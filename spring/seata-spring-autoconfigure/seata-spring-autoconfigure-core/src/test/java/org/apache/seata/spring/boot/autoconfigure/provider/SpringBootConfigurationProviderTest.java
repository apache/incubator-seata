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
package org.apache.seata.spring.boot.autoconfigure.provider;

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.*;

class SpringBootConfigurationProviderTest {
    private Object originalEnvironment;
    private MockEnvironment environment;
    private final SpringBootConfigurationProvider provider = new SpringBootConfigurationProvider();

    @BeforeEach
    void setUp() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        environment = new MockEnvironment();
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
    }

    @AfterEach
    void tearDown() {
        if (originalEnvironment != null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        } else {
            Map<?, ?> objects = (Map<?, ?>) ReflectionTestUtils.getField(ObjectHolder.class, "OBJECT_MAP");
            objects.remove(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        }
    }

    @Test
    void absentOptionalPropertyBeansHaveNoDefault() {
        assertNull(ReflectionTestUtils.invokeMethod(
                provider, "getDefaultValueFromPropertyObject", "seata.json.serializerType"));
        assertNull(ReflectionTestUtils.invokeMethod(
                provider, "getDefaultValueFromPropertyObject", "seata.client.rm.sagaJsonParser"));
        assertNull(ReflectionTestUtils.invokeMethod(
                provider, "getDefaultValueFromPropertyObject", "seata.optionalfixture.value"));
    }

    @Test
    void fallsBackToOriginalConfigurationWithoutPropertyBean() {
        Configuration config = provider.provide(new OriginalConfiguration());
        assertEquals("file-value", config.getConfig("optionalfixture.value"));
    }

    @Test
    void environmentAndCallerDefaultsStillTakePrecedence() {
        Configuration config = provider.provide(new OriginalConfiguration());
        assertEquals("caller-value", config.getConfig("optionalfixture.value", "caller-value"));
        environment.setProperty("seata.optionalfixture.value", "environment-value");
        assertEquals("environment-value", config.getConfig("optionalfixture.value", "caller-value"));
    }

    public static class OriginalConfiguration extends FileConfiguration {
        @Override
        public String getConfig(String key) {
            return "file-value";
        }
    }
}
