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
package io.seata.integration.http;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for SeataWebMvcConfigurer compatibility wrapper.
 */
public class SeataWebMvcConfigurerTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                SeataWebMvcConfigurer.class.isAnnotationPresent(Deprecated.class),
                "SeataWebMvcConfigurer should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataClass() {
        assertTrue(
                org.apache.seata.integration.http.SeataWebMvcConfigurer.class.isAssignableFrom(
                        SeataWebMvcConfigurer.class),
                "SeataWebMvcConfigurer should extend Apache Seata SeataWebMvcConfigurer");
    }

    @Test
    public void testConstructor() {
        SeataWebMvcConfigurer configurer = new SeataWebMvcConfigurer();
        assertNotNull(configurer);
    }

    @Test
    public void testInstanceOfApacheSeataClass() {
        SeataWebMvcConfigurer configurer = new SeataWebMvcConfigurer();
        assertTrue(
                configurer instanceof org.apache.seata.integration.http.SeataWebMvcConfigurer,
                "Instance should be of Apache Seata SeataWebMvcConfigurer type");
    }
}
