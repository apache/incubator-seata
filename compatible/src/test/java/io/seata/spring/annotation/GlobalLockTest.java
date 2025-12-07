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
package io.seata.spring.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for GlobalLock annotation compatibility wrapper.
 */
public class GlobalLockTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                GlobalLock.class.isAnnotationPresent(Deprecated.class), "GlobalLock should be marked as @Deprecated");
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = GlobalLock.class.getAnnotation(Retention.class);
        assertNotNull(retention, "GlobalLock should have @Retention");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(), "Retention policy should be RUNTIME");
    }

    @Test
    public void testTargetElements() {
        Target target = GlobalLock.class.getAnnotation(Target.class);
        assertNotNull(target, "GlobalLock should have @Target");
        ElementType[] expectedTargets = {ElementType.METHOD, ElementType.TYPE};
        assertArrayEquals(expectedTargets, target.value(), "Target should be METHOD and TYPE");
    }

    @Test
    public void testInheritedAnnotation() {
        assertTrue(GlobalLock.class.isAnnotationPresent(Inherited.class), "GlobalLock should be marked as @Inherited");
    }

    @Test
    public void testDefaultLockRetryInterval() throws Exception {
        int defaultValue = (int) GlobalLock.class.getMethod("lockRetryInterval").getDefaultValue();
        assertEquals(0, defaultValue, "Default lockRetryInterval should be 0");
    }

    @Test
    public void testDefaultLockRetryTimes() throws Exception {
        int defaultValue = (int) GlobalLock.class.getMethod("lockRetryTimes").getDefaultValue();
        assertEquals(-1, defaultValue, "Default lockRetryTimes should be -1");
    }

    @Test
    public void testAnnotationCanBeAppliedToMethod() {
        @GlobalLock
        class TestClass {
            @GlobalLock
            public void testMethod() {}
        }

        assertTrue(TestClass.class.isAnnotationPresent(GlobalLock.class), "Annotation should be applicable to class");
        try {
            assertTrue(
                    TestClass.class.getMethod("testMethod").isAnnotationPresent(GlobalLock.class),
                    "Annotation should be applicable to method");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
