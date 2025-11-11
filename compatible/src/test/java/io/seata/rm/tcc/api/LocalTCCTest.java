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
package io.seata.rm.tcc.api;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for LocalTCC annotation.
 */
public class LocalTCCTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(LocalTCC.class.isAnnotationPresent(Deprecated.class), "LocalTCC should be marked as @Deprecated");
    }

    @Test
    public void testAnnotationRetention() {
        Retention retention = LocalTCC.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testAnnotationTarget() {
        Target target = LocalTCC.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertEquals(1, target.value().length);
        assertEquals(ElementType.TYPE, target.value()[0]);
    }

    @Test
    public void testAnnotationInherited() {
        assertTrue(LocalTCC.class.isAnnotationPresent(Inherited.class), "LocalTCC should be marked as @Inherited");
    }

    @Test
    public void testAnnotationUsage() {
        LocalTCC annotation = TestTccService.class.getAnnotation(LocalTCC.class);
        assertNotNull(annotation, "TestTccService should have LocalTCC annotation");
    }

    @Test
    public void testInheritance() {
        // Test that the annotation is inherited
        LocalTCC annotation = ChildTestTccService.class.getAnnotation(LocalTCC.class);
        assertNotNull(annotation, "ChildTestTccService should inherit LocalTCC annotation from parent");
    }

    @Test
    public void testWithoutAnnotation() {
        LocalTCC annotation = RegularService.class.getAnnotation(LocalTCC.class);
        assertTrue(annotation == null, "RegularService should not have LocalTCC annotation");
    }

    // Test service classes
    @LocalTCC
    public static class TestTccService {
        @TwoPhaseBusinessAction(name = "testAction")
        public boolean prepare(String param) {
            return true;
        }

        public boolean commit(BusinessActionContext context) {
            return true;
        }

        public boolean rollback(BusinessActionContext context) {
            return true;
        }
    }

    public static class ChildTestTccService extends TestTccService {
        // Inherits LocalTCC annotation
    }

    public static class RegularService {
        // No annotation
    }
}
