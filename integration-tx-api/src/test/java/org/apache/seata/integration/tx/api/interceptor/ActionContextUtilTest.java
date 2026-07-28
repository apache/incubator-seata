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
package org.apache.seata.integration.tx.api.interceptor;

import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.ParamType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ActionContextUtilTest {

    @Test
    public void testGetByIndex() {
        assertEquals(
                "first", ActionContextUtil.getByIndex(ParamType.PARAM, "names", Collections.singletonList("first"), 0));
        assertNull(ActionContextUtil.getByIndex(ParamType.PARAM, "names", Collections.emptyList(), 0));
        assertNull(ActionContextUtil.getByIndex(ParamType.PARAM, "names", Collections.singletonList("first"), 1));
        assertEquals("plain", ActionContextUtil.getByIndex(ParamType.FIELD, "name", "plain", 0));
    }

    @Test
    public void testPutActionContextHandlesValuesAndDetectsChanges() {
        Map<String, Object> context = new HashMap<>();

        assertFalse(ActionContextUtil.putActionContext(context, "nullValue", null));
        assertTrue(ActionContextUtil.putActionContext(context, "name", "seata"));
        assertFalse(ActionContextUtil.putActionContext(context, "name", "seata"));

        assertTrue(ActionContextUtil.putActionContext(context, "payload", new Payload("alice", 7)));
        String payload = (String) context.get("payload");
        assertTrue(payload.contains("\"name\":\"alice\""));
        assertTrue(payload.contains("\"count\":7"));
    }

    @Test
    public void testPutActionContextWithoutHandle() {
        Map<String, Object> context = new HashMap<>();
        Payload payload = new Payload("bob", 9);

        assertTrue(ActionContextUtil.putActionContextWithoutHandle(context, "payload", payload));
        assertEquals(payload, context.get("payload"));
        assertFalse(ActionContextUtil.putActionContextWithoutHandle(context, "payload", payload));
        assertFalse(ActionContextUtil.putActionContextWithoutHandle(context, "payload", null));
    }

    @Test
    public void testConvertActionContext() {
        Payload payload = new Payload("carol", 11);

        assertThrows(
                IllegalArgumentException.class, () -> ActionContextUtil.convertActionContext("count", 1, int.class));
        assertNull(ActionContextUtil.convertActionContext("payload", null, Payload.class));
        assertEquals(payload, ActionContextUtil.convertActionContext("payload", payload, Payload.class));
        assertEquals("12", ActionContextUtil.convertActionContext("count", 12, String.class));

        Payload fromJson =
                ActionContextUtil.convertActionContext("payload", "{\"name\":\"dave\",\"count\":13}", Payload.class);
        assertEquals("dave", fromJson.getName());
        assertEquals(13, fromJson.getCount());

        assertThrows(
                FrameworkException.class,
                () -> ActionContextUtil.convertActionContext("payload", "not-json", Payload.class));
    }

    @Test
    public void testLoadParamByAnnotationAndPutToContext() throws NoSuchMethodException {
        BusinessActionContextParameter propertyAnnotation = getMethod("withPropertyParam", PropertyParam.class)
                .getParameters()[0]
                .getAnnotation(BusinessActionContextParameter.class);
        Map<String, Object> context = new HashMap<>();

        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "param", new PropertyParam(), propertyAnnotation, context);

        assertEquals("fieldValue", context.get("fieldName"));

        BusinessActionContextParameter aliasAnnotation = getMethod("withAliasParam", String.class)
                .getParameters()[0]
                .getAnnotation(BusinessActionContextParameter.class);
        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "origin", "value", aliasAnnotation, context);

        assertEquals("value", context.get("aliasName"));
    }

    @Test
    public void testGetTwoPhaseArgsRequiresAnnotationForNonContextParameter() throws NoSuchMethodException {
        Method validMethod = getMethod("twoPhase", BusinessActionContext.class, String.class);
        String[] keys = ActionContextUtil.getTwoPhaseArgs(validMethod, validMethod.getParameterTypes());

        assertNull(keys[0]);
        assertEquals("code", keys[1]);

        Method invalidMethod = getMethod("missingAnnotation", BusinessActionContext.class, String.class);
        assertThrows(
                IllegalArgumentException.class,
                () -> ActionContextUtil.getTwoPhaseArgs(invalidMethod, invalidMethod.getParameterTypes()));
    }

    private static Method getMethod(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return SampleAction.class.getDeclaredMethod(name, parameterTypes);
    }

    public static class Payload {
        private String name;
        private int count;

        public Payload() {}

        public Payload(String name, int count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static class PropertyParam {
        @BusinessActionContextParameter(paramName = "fieldName")
        private final String field = "fieldValue";
    }

    public static class SampleAction {
        public void withPropertyParam(@BusinessActionContextParameter(isParamInProperty = true) PropertyParam param) {}

        public void withAliasParam(@BusinessActionContextParameter(paramName = "aliasName") String param) {}

        public void twoPhase(BusinessActionContext context, @BusinessActionContextParameter("code") String code) {}

        public void missingAnnotation(BusinessActionContext context, String code) {}
    }
}
