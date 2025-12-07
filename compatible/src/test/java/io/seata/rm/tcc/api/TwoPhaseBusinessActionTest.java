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
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for TwoPhaseBusinessAction annotation.
 */
public class TwoPhaseBusinessActionTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                TwoPhaseBusinessAction.class.isAnnotationPresent(Deprecated.class),
                "TwoPhaseBusinessAction should be marked as @Deprecated");
    }

    @Test
    public void testAnnotationRetention() {
        Retention retention = TwoPhaseBusinessAction.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testAnnotationTarget() {
        Target target = TwoPhaseBusinessAction.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertEquals(1, target.value().length);
        assertEquals(ElementType.METHOD, target.value()[0]);
    }

    @Test
    public void testAnnotationInherited() {
        assertTrue(
                TwoPhaseBusinessAction.class.isAnnotationPresent(Inherited.class),
                "TwoPhaseBusinessAction should be marked as @Inherited");
    }

    @Test
    public void testDefaultValues() throws NoSuchMethodException {
        Method commitMethodAttr = TwoPhaseBusinessAction.class.getMethod("commitMethod");
        assertEquals("commit", commitMethodAttr.getDefaultValue());

        Method rollbackMethodAttr = TwoPhaseBusinessAction.class.getMethod("rollbackMethod");
        assertEquals("rollback", rollbackMethodAttr.getDefaultValue());

        Method isDelayReportAttr = TwoPhaseBusinessAction.class.getMethod("isDelayReport");
        assertEquals(false, isDelayReportAttr.getDefaultValue());

        Method useTCCFenceAttr = TwoPhaseBusinessAction.class.getMethod("useTCCFence");
        assertEquals(false, useTCCFenceAttr.getDefaultValue());

        Method commitArgsClassesAttr = TwoPhaseBusinessAction.class.getMethod("commitArgsClasses");
        Class<?>[] defaultCommitArgs = (Class<?>[]) commitArgsClassesAttr.getDefaultValue();
        assertEquals(1, defaultCommitArgs.length);
        assertEquals(BusinessActionContext.class, defaultCommitArgs[0]);

        Method rollbackArgsClassesAttr = TwoPhaseBusinessAction.class.getMethod("rollbackArgsClasses");
        Class<?>[] defaultRollbackArgs = (Class<?>[]) rollbackArgsClassesAttr.getDefaultValue();
        assertEquals(1, defaultRollbackArgs.length);
        assertEquals(BusinessActionContext.class, defaultRollbackArgs[0]);
    }

    @Test
    public void testAnnotationWithMinimalConfig() throws Exception {
        Method method = TestService.class.getMethod("prepareMinimal", String.class);
        TwoPhaseBusinessAction annotation = method.getAnnotation(TwoPhaseBusinessAction.class);

        assertNotNull(annotation);
        assertEquals("minimalAction", annotation.name());
        assertEquals("commit", annotation.commitMethod());
        assertEquals("rollback", annotation.rollbackMethod());
        assertFalse(annotation.isDelayReport());
        assertFalse(annotation.useTCCFence());
    }

    @Test
    public void testAnnotationWithFullConfig() throws Exception {
        Method method = TestService.class.getMethod("prepareWithFullConfig", String.class, int.class, Object.class);
        TwoPhaseBusinessAction annotation = method.getAnnotation(TwoPhaseBusinessAction.class);

        assertNotNull(annotation);
        assertEquals("fullConfigAction", annotation.name());
        assertEquals("customCommit", annotation.commitMethod());
        assertEquals("customRollback", annotation.rollbackMethod());
        assertTrue(annotation.isDelayReport());
        assertTrue(annotation.useTCCFence());
        assertArrayEquals(new Class<?>[] {BusinessActionContext.class, String.class}, annotation.commitArgsClasses());
        assertArrayEquals(new Class<?>[] {BusinessActionContext.class, String.class}, annotation.rollbackArgsClasses());
    }

    @Test
    public void testAnnotationWithTCCFence() throws Exception {
        Method method = TestService.class.getMethod("prepareWithFence", String.class);
        TwoPhaseBusinessAction annotation = method.getAnnotation(TwoPhaseBusinessAction.class);

        assertNotNull(annotation);
        assertTrue(annotation.useTCCFence());
        assertTrue(annotation.isDelayReport());
    }

    @Test
    public void testAnnotationWithCustomMethods() throws Exception {
        Method method = TestService.class.getMethod("prepareWithCustomMethods", String.class);
        TwoPhaseBusinessAction annotation = method.getAnnotation(TwoPhaseBusinessAction.class);

        assertNotNull(annotation);
        assertEquals("customAction", annotation.name());
        assertEquals("myCommit", annotation.commitMethod());
        assertEquals("myRollback", annotation.rollbackMethod());
    }

    // Test service class
    public static class TestService {

        @TwoPhaseBusinessAction(name = "minimalAction")
        public boolean prepareMinimal(String param) {
            return true;
        }

        @TwoPhaseBusinessAction(
                name = "fullConfigAction",
                commitMethod = "customCommit",
                rollbackMethod = "customRollback",
                isDelayReport = true,
                useTCCFence = true,
                commitArgsClasses = {BusinessActionContext.class, String.class},
                rollbackArgsClasses = {BusinessActionContext.class, String.class})
        public boolean prepareWithFullConfig(String param1, int param2, Object param3) {
            return true;
        }

        @TwoPhaseBusinessAction(name = "fenceAction", useTCCFence = true, isDelayReport = true)
        public boolean prepareWithFence(String param) {
            return true;
        }

        @TwoPhaseBusinessAction(name = "customAction", commitMethod = "myCommit", rollbackMethod = "myRollback")
        public boolean prepareWithCustomMethods(String param) {
            return true;
        }

        public boolean commit(BusinessActionContext context) {
            return true;
        }

        public boolean rollback(BusinessActionContext context) {
            return true;
        }

        public boolean customCommit(BusinessActionContext context, String extra) {
            return true;
        }

        public boolean customRollback(BusinessActionContext context, String extra) {
            return true;
        }

        public boolean myCommit(BusinessActionContext context) {
            return true;
        }

        public boolean myRollback(BusinessActionContext context) {
            return true;
        }
    }
}
