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
package io.seata.spring.annotation.datasource;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for EnableAutoDataSourceProxy annotation compatibility wrapper.
 */
public class EnableAutoDataSourceProxyTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                EnableAutoDataSourceProxy.class.isAnnotationPresent(Deprecated.class),
                "EnableAutoDataSourceProxy should be marked as @Deprecated");
    }

    @Test
    public void testDocumentedAnnotation() {
        assertTrue(
                EnableAutoDataSourceProxy.class.isAnnotationPresent(Documented.class),
                "EnableAutoDataSourceProxy should be marked as @Documented");
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = EnableAutoDataSourceProxy.class.getAnnotation(Retention.class);
        assertNotNull(retention, "EnableAutoDataSourceProxy should have @Retention");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(), "Retention policy should be RUNTIME");
    }

    @Test
    public void testTargetElement() {
        Target target = EnableAutoDataSourceProxy.class.getAnnotation(Target.class);
        assertNotNull(target, "EnableAutoDataSourceProxy should have @Target");
        ElementType[] expectedTargets = {ElementType.TYPE};
        assertArrayEquals(expectedTargets, target.value(), "Target should be TYPE");
    }

    @Test
    public void testImportAnnotation() {
        Import importAnnotation = EnableAutoDataSourceProxy.class.getAnnotation(Import.class);
        assertNotNull(importAnnotation, "EnableAutoDataSourceProxy should have @Import");
        Class<?>[] importClasses = importAnnotation.value();
        assertEquals(1, importClasses.length, "Should import one class");
        assertEquals(
                AutoDataSourceProxyRegistrar.class, importClasses[0], "Should import AutoDataSourceProxyRegistrar");
    }

    @Test
    public void testDefaultUseJdkProxy() throws Exception {
        boolean defaultValue = (boolean)
                EnableAutoDataSourceProxy.class.getMethod("useJdkProxy").getDefaultValue();
        assertFalse(defaultValue, "Default useJdkProxy should be false");
    }

    @Test
    public void testDefaultExcludes() throws Exception {
        String[] defaultValue =
                (String[]) EnableAutoDataSourceProxy.class.getMethod("excludes").getDefaultValue();
        assertEquals(0, defaultValue.length, "Default excludes should be empty array");
    }

    @Test
    public void testDefaultDataSourceProxyMode() throws Exception {
        String defaultValue = (String)
                EnableAutoDataSourceProxy.class.getMethod("dataSourceProxyMode").getDefaultValue();
        assertEquals("AT", defaultValue, "Default dataSourceProxyMode should be AT");
    }

    @Test
    public void testAnnotationCanBeAppliedToClass() {
        @EnableAutoDataSourceProxy
        class TestConfig {}

        assertTrue(
                TestConfig.class.isAnnotationPresent(EnableAutoDataSourceProxy.class),
                "Annotation should be applicable to class");
    }
}
