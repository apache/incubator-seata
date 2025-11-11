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
package io.seata.rm.tcc.interceptor;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;
import org.apache.seata.common.Constants;
import org.apache.seata.integration.tx.api.interceptor.TwoPhaseBusinessActionParam;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for TccActionInterceptorHandler.
 */
public class TccActionInterceptorHandlerTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                TccActionInterceptorHandler.class.isAnnotationPresent(Deprecated.class),
                "TccActionInterceptorHandler should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataTccHandler() {
        assertTrue(
                org.apache.seata.rm.tcc.interceptor.TccActionInterceptorHandler.class.isAssignableFrom(
                        TccActionInterceptorHandler.class),
                "Should extend Apache Seata TccActionInterceptorHandler");
    }

    @Test
    public void testConstructor() {
        Object targetBean = new TestTccService();
        Set<String> methodsToProxy = new HashSet<>();
        methodsToProxy.add("prepare");

        TccActionInterceptorHandler handler = new TccActionInterceptorHandler(targetBean, methodsToProxy);

        assertNotNull(handler);
    }

    @Test
    public void testGetAnnotationClass() throws Exception {
        TccActionInterceptorHandler handler = createHandler();

        Method method = TccActionInterceptorHandler.class.getDeclaredMethod("getAnnotationClass");
        method.setAccessible(true);

        Class<?> annotationClass = (Class<?>) method.invoke(handler);

        assertEquals(TwoPhaseBusinessAction.class, annotationClass);
    }

    @Test
    public void testParserCommonFenceConfigEnabled() throws Exception {
        TccActionInterceptorHandler handler = createHandler();

        Method testMethod = TestTccService.class.getMethod("prepareWithFence", String.class, int.class);
        TwoPhaseBusinessAction annotation = testMethod.getAnnotation(TwoPhaseBusinessAction.class);

        Method method = TccActionInterceptorHandler.class.getDeclaredMethod(
                "parserCommonFenceConfig", java.lang.annotation.Annotation.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(handler, annotation);

        assertTrue(result, "Should return true for TCC fence enabled annotation");
    }

    @Test
    public void testParserCommonFenceConfigDisabled() throws Exception {
        TccActionInterceptorHandler handler = createHandler();

        Method testMethod = TestTccService.class.getMethod("prepareWithoutFence", String.class);
        TwoPhaseBusinessAction annotation = testMethod.getAnnotation(TwoPhaseBusinessAction.class);

        Method method = TccActionInterceptorHandler.class.getDeclaredMethod(
                "parserCommonFenceConfig", java.lang.annotation.Annotation.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(handler, annotation);

        assertFalse(result, "Should return false for TCC fence disabled annotation");
    }

    @Test
    public void testParserCommonFenceConfigWithNull() throws Exception {
        TccActionInterceptorHandler handler = createHandler();

        Method method = TccActionInterceptorHandler.class.getDeclaredMethod(
                "parserCommonFenceConfig", java.lang.annotation.Annotation.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(handler, new Object[] {null});

        assertFalse(result, "Should return false for null annotation");
    }

    @Test
    public void testCreateTwoPhaseBusinessActionParam() throws Exception {
        TccActionInterceptorHandler handler = createHandler();

        Method testMethod = TestTccService.class.getMethod("prepareWithFence", String.class, int.class);
        TwoPhaseBusinessAction annotation = testMethod.getAnnotation(TwoPhaseBusinessAction.class);

        Method method = TccActionInterceptorHandler.class.getDeclaredMethod(
                "createTwoPhaseBusinessActionParam", java.lang.annotation.Annotation.class);
        method.setAccessible(true);

        TwoPhaseBusinessActionParam param = (TwoPhaseBusinessActionParam) method.invoke(handler, annotation);

        assertNotNull(param);
        assertEquals("testAction", param.getActionName());
        assertTrue(param.getDelayReport());
        assertTrue(param.getUseCommonFence());
        assertEquals(org.apache.seata.core.model.BranchType.TCC, param.getBranchType());

        Map<String, Object> context = param.getBusinessActionContext();
        assertNotNull(context);
        assertEquals("commitWithFence", context.get(Constants.COMMIT_METHOD));
        assertEquals("rollbackWithFence", context.get(Constants.ROLLBACK_METHOD));
        assertEquals("testAction", context.get(Constants.ACTION_NAME));
        assertEquals(true, context.get(Constants.USE_COMMON_FENCE));
    }

    @Test
    public void testCreateTwoPhaseBusinessActionParamWithoutFence() throws Exception {
        TccActionInterceptorHandler handler = createHandler();

        Method testMethod = TestTccService.class.getMethod("prepareWithoutFence", String.class);
        TwoPhaseBusinessAction annotation = testMethod.getAnnotation(TwoPhaseBusinessAction.class);

        Method method = TccActionInterceptorHandler.class.getDeclaredMethod(
                "createTwoPhaseBusinessActionParam", java.lang.annotation.Annotation.class);
        method.setAccessible(true);

        TwoPhaseBusinessActionParam param = (TwoPhaseBusinessActionParam) method.invoke(handler, annotation);

        assertNotNull(param);
        assertEquals("simpleAction", param.getActionName());
        assertFalse(param.getDelayReport());
        assertFalse(param.getUseCommonFence());

        Map<String, Object> context = param.getBusinessActionContext();
        assertNotNull(context);
        assertEquals("commit", context.get(Constants.COMMIT_METHOD));
        assertEquals("rollback", context.get(Constants.ROLLBACK_METHOD));
        assertEquals("simpleAction", context.get(Constants.ACTION_NAME));
        assertEquals(false, context.get(Constants.USE_COMMON_FENCE));
    }

    @Test
    public void testCreateTwoPhaseBusinessActionParamWithCustomMethods() throws Exception {
        TccActionInterceptorHandler handler = createHandler();

        Method testMethod = TestTccService.class.getMethod("prepareWithCustomMethods", String.class);
        TwoPhaseBusinessAction annotation = testMethod.getAnnotation(TwoPhaseBusinessAction.class);

        Method method = TccActionInterceptorHandler.class.getDeclaredMethod(
                "createTwoPhaseBusinessActionParam", java.lang.annotation.Annotation.class);
        method.setAccessible(true);

        TwoPhaseBusinessActionParam param = (TwoPhaseBusinessActionParam) method.invoke(handler, annotation);

        assertNotNull(param);
        assertEquals("customAction", param.getActionName());

        Map<String, Object> context = param.getBusinessActionContext();
        assertEquals("customCommit", context.get(Constants.COMMIT_METHOD));
        assertEquals("customRollback", context.get(Constants.ROLLBACK_METHOD));
    }

    private TccActionInterceptorHandler createHandler() {
        Object targetBean = new TestTccService();
        Set<String> methodsToProxy = new HashSet<>();
        methodsToProxy.add("prepareWithFence");
        methodsToProxy.add("prepareWithoutFence");
        methodsToProxy.add("prepareWithCustomMethods");
        return new TccActionInterceptorHandler(targetBean, methodsToProxy);
    }

    // Test service class with TCC annotations
    public static class TestTccService {

        @TwoPhaseBusinessAction(
                name = "testAction",
                commitMethod = "commitWithFence",
                rollbackMethod = "rollbackWithFence",
                isDelayReport = true,
                useTCCFence = true)
        public boolean prepareWithFence(String param1, int param2) {
            return true;
        }

        public boolean commitWithFence(BusinessActionContext context) {
            return true;
        }

        public boolean rollbackWithFence(BusinessActionContext context) {
            return true;
        }

        @TwoPhaseBusinessAction(name = "simpleAction")
        public boolean prepareWithoutFence(String param) {
            return true;
        }

        public boolean commit(BusinessActionContext context) {
            return true;
        }

        public boolean rollback(BusinessActionContext context) {
            return true;
        }

        @TwoPhaseBusinessAction(name = "customAction", commitMethod = "customCommit", rollbackMethod = "customRollback")
        public boolean prepareWithCustomMethods(String param) {
            return true;
        }

        public boolean customCommit(BusinessActionContext context) {
            return true;
        }

        public boolean customRollback(BusinessActionContext context) {
            return true;
        }
    }
}
