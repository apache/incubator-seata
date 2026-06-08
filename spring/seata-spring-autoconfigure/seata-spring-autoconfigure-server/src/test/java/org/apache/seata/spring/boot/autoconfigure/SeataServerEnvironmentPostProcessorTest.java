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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.METRICS_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.PROPERTY_BEAN_MAP;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.SERVER_PREFIX;
import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.STORE_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeataServerEnvironmentPostProcessorTest {
    private SeataServerEnvironmentPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SeataServerEnvironmentPostProcessor();
        PROPERTY_BEAN_MAP.clear();
        try {
            Field field = SeataServerEnvironmentPostProcessor.class.getDeclaredField("INIT");
            field.setAccessible(true);
            ((java.util.concurrent.atomic.AtomicBoolean) field.get(null)).set(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testPostProcessEnvironmentShouldPopulatePropertyBeanMap() {
        MockEnvironment environment = new MockEnvironment();
        SpringApplication application = new SpringApplication();

        processor.postProcessEnvironment(environment, application);

        assertTrue(PROPERTY_BEAN_MAP.containsKey(SERVER_PREFIX));
        assertTrue(PROPERTY_BEAN_MAP.containsKey(STORE_PREFIX));
        assertTrue(PROPERTY_BEAN_MAP.containsKey(METRICS_PREFIX));
    }

    @Test
    void testInitIsIdempotent() {
        MockEnvironment environment = new MockEnvironment();
        SpringApplication application = new SpringApplication();

        // First
        processor.postProcessEnvironment(environment, application);
        int firstSize = PROPERTY_BEAN_MAP.size();

        // Second
        processor.postProcessEnvironment(environment, application);
        int secondSize = PROPERTY_BEAN_MAP.size();

        assertEquals(firstSize, secondSize, "PROPERTY_BEAN_MAP should keep the same size");
    }

    @Test
    void testGetOrderShouldBeHighestPrecedence() {
        assertEquals(org.springframework.core.Ordered.HIGHEST_PRECEDENCE, processor.getOrder());
    }
}
