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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for BusinessActionContextParameter annotation.
 */
public class BusinessActionContextParameterTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                BusinessActionContextParameter.class.isAnnotationPresent(Deprecated.class),
                "BusinessActionContextParameter should be marked as @Deprecated");
    }

    @Test
    public void testAnnotationRetention() {
        Retention retention = BusinessActionContextParameter.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testAnnotationTarget() {
        Target target = BusinessActionContextParameter.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertEquals(2, target.value().length);
        assertTrue(containsElementType(target.value(), ElementType.PARAMETER), "Should target PARAMETER");
        assertTrue(containsElementType(target.value(), ElementType.FIELD), "Should target FIELD");
    }

    @Test
    public void testDefaultValues() throws NoSuchMethodException {
        Method valueMethod = BusinessActionContextParameter.class.getMethod("value");
        assertEquals("", valueMethod.getDefaultValue());

        Method paramNameMethod = BusinessActionContextParameter.class.getMethod("paramName");
        assertEquals("", paramNameMethod.getDefaultValue());

        Method isShardingParamMethod = BusinessActionContextParameter.class.getMethod("isShardingParam");
        assertEquals(false, isShardingParamMethod.getDefaultValue());

        Method indexMethod = BusinessActionContextParameter.class.getMethod("index");
        assertEquals(-1, indexMethod.getDefaultValue());

        Method isParamInPropertyMethod = BusinessActionContextParameter.class.getMethod("isParamInProperty");
        assertEquals(false, isParamInPropertyMethod.getDefaultValue());
    }

    @Test
    public void testParameterAnnotationUsage() throws Exception {
        Method method = TestService.class.getMethod("testMethod", String.class, int.class, String.class);
        Parameter[] parameters = method.getParameters();

        // First parameter
        BusinessActionContextParameter annotation1 = parameters[0].getAnnotation(BusinessActionContextParameter.class);
        assertNotNull(annotation1);
        assertEquals("userId", annotation1.value());
        assertEquals("", annotation1.paramName());
        assertEquals(-1, annotation1.index());
        assertFalse(annotation1.isParamInProperty());

        // Second parameter
        BusinessActionContextParameter annotation2 = parameters[1].getAnnotation(BusinessActionContextParameter.class);
        assertNotNull(annotation2);
        assertEquals("", annotation2.value());
        assertEquals("amount", annotation2.paramName());
        assertEquals(0, annotation2.index());
        assertFalse(annotation2.isParamInProperty());

        // Third parameter
        BusinessActionContextParameter annotation3 = parameters[2].getAnnotation(BusinessActionContextParameter.class);
        assertNotNull(annotation3);
        assertTrue(annotation3.isParamInProperty());
    }

    @Test
    public void testFieldAnnotationUsage() throws Exception {
        Field field = TestEntity.class.getDeclaredField("accountId");
        BusinessActionContextParameter annotation = field.getAnnotation(BusinessActionContextParameter.class);

        assertNotNull(annotation);
        assertEquals("account_id", annotation.value());
    }

    @Test
    public void testIsShardingParamDeprecated() throws NoSuchMethodException {
        Method method = BusinessActionContextParameter.class.getMethod("isShardingParam");
        assertTrue(method.isAnnotationPresent(Deprecated.class), "isShardingParam should be marked as @Deprecated");
    }

    private boolean containsElementType(ElementType[] types, ElementType target) {
        for (ElementType type : types) {
            if (type == target) {
                return true;
            }
        }
        return false;
    }

    // Test service for annotation usage
    public static class TestService {
        public void testMethod(
                @BusinessActionContextParameter(value = "userId") String userId,
                @BusinessActionContextParameter(paramName = "amount", index = 0) int amount,
                @BusinessActionContextParameter(isParamInProperty = true) String data) {}
    }

    // Test entity for field annotation
    public static class TestEntity {
        @BusinessActionContextParameter("account_id")
        private String accountId;
    }
}
