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

import io.seata.common.LockStrategyMode;
import io.seata.tm.api.transaction.Propagation;
import org.apache.seata.common.DefaultValues;
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
 * Test cases for GlobalTransactional annotation compatibility wrapper.
 */
public class GlobalTransactionalTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                GlobalTransactional.class.isAnnotationPresent(Deprecated.class),
                "GlobalTransactional should be marked as @Deprecated");
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = GlobalTransactional.class.getAnnotation(Retention.class);
        assertNotNull(retention, "GlobalTransactional should have @Retention");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(), "Retention policy should be RUNTIME");
    }

    @Test
    public void testTargetElements() {
        Target target = GlobalTransactional.class.getAnnotation(Target.class);
        assertNotNull(target, "GlobalTransactional should have @Target");
        ElementType[] expectedTargets = {ElementType.METHOD, ElementType.TYPE};
        assertArrayEquals(expectedTargets, target.value(), "Target should be METHOD and TYPE");
    }

    @Test
    public void testInheritedAnnotation() {
        assertTrue(
                GlobalTransactional.class.isAnnotationPresent(Inherited.class),
                "GlobalTransactional should be marked as @Inherited");
    }

    @Test
    public void testDefaultTimeoutMills() throws Exception {
        int defaultValue =
                (int) GlobalTransactional.class.getMethod("timeoutMills").getDefaultValue();
        assertEquals(
                DefaultValues.DEFAULT_GLOBAL_TRANSACTION_TIMEOUT,
                defaultValue,
                "Default timeoutMills should be DEFAULT_GLOBAL_TRANSACTION_TIMEOUT");
    }

    @Test
    public void testDefaultName() throws Exception {
        String defaultValue =
                (String) GlobalTransactional.class.getMethod("name").getDefaultValue();
        assertEquals("", defaultValue, "Default name should be empty string");
    }

    @Test
    public void testDefaultRollbackFor() throws Exception {
        Class<?>[] defaultValue =
                (Class<?>[]) GlobalTransactional.class.getMethod("rollbackFor").getDefaultValue();
        assertEquals(0, defaultValue.length, "Default rollbackFor should be empty array");
    }

    @Test
    public void testDefaultRollbackForClassName() throws Exception {
        String[] defaultValue = (String[])
                GlobalTransactional.class.getMethod("rollbackForClassName").getDefaultValue();
        assertEquals(0, defaultValue.length, "Default rollbackForClassName should be empty array");
    }

    @Test
    public void testDefaultNoRollbackFor() throws Exception {
        Class<?>[] defaultValue = (Class<?>[])
                GlobalTransactional.class.getMethod("noRollbackFor").getDefaultValue();
        assertEquals(0, defaultValue.length, "Default noRollbackFor should be empty array");
    }

    @Test
    public void testDefaultNoRollbackForClassName() throws Exception {
        String[] defaultValue = (String[])
                GlobalTransactional.class.getMethod("noRollbackForClassName").getDefaultValue();
        assertEquals(0, defaultValue.length, "Default noRollbackForClassName should be empty array");
    }

    @Test
    public void testDefaultPropagation() throws Exception {
        Propagation defaultValue =
                (Propagation) GlobalTransactional.class.getMethod("propagation").getDefaultValue();
        assertEquals(Propagation.REQUIRED, defaultValue, "Default propagation should be REQUIRED");
    }

    @Test
    public void testDefaultLockRetryInterval() throws Exception {
        int defaultValue =
                (int) GlobalTransactional.class.getMethod("lockRetryInterval").getDefaultValue();
        assertEquals(0, defaultValue, "Default lockRetryInterval should be 0");
    }

    @Test
    public void testDefaultLockRetryInternal() throws Exception {
        int defaultValue =
                (int) GlobalTransactional.class.getMethod("lockRetryInternal").getDefaultValue();
        assertEquals(0, defaultValue, "Default lockRetryInternal should be 0");
    }

    @Test
    public void testDefaultLockRetryTimes() throws Exception {
        int defaultValue =
                (int) GlobalTransactional.class.getMethod("lockRetryTimes").getDefaultValue();
        assertEquals(-1, defaultValue, "Default lockRetryTimes should be -1");
    }

    @Test
    public void testDefaultLockStrategyMode() throws Exception {
        LockStrategyMode defaultValue = (LockStrategyMode)
                GlobalTransactional.class.getMethod("lockStrategyMode").getDefaultValue();
        assertEquals(LockStrategyMode.PESSIMISTIC, defaultValue, "Default lockStrategyMode should be PESSIMISTIC");
    }

    @Test
    public void testAnnotationCanBeAppliedToMethod() {
        @GlobalTransactional
        class TestClass {
            @GlobalTransactional
            public void testMethod() {}
        }

        assertTrue(
                TestClass.class.isAnnotationPresent(GlobalTransactional.class),
                "Annotation should be applicable to class");
        try {
            assertTrue(
                    TestClass.class.getMethod("testMethod").isAnnotationPresent(GlobalTransactional.class),
                    "Annotation should be applicable to method");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
