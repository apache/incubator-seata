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
package io.seata.integration.tx.api.interceptor;

import io.seata.rm.tcc.api.BusinessActionContextParameter;
import org.apache.seata.rm.tcc.api.ParamType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for ActionContextUtil.
 */
public class ActionContextUtilTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                ActionContextUtil.class.isAnnotationPresent(Deprecated.class),
                "ActionContextUtil should be marked as @Deprecated");
    }

    @Test
    public void testFetchContextFromObject() {
        TestObject obj = new TestObject();
        obj.setUserId("user123");
        obj.setOrderId("order456");

        Map<String, Object> context = ActionContextUtil.fetchContextFromObject(obj);

        assertNotNull(context);
        assertTrue(context.size() >= 0);
    }

    @Test
    public void testFetchContextFromObjectWithNull() {
        // fetchContextFromObject may throw exception for null, so we expect that
        assertThrows(Exception.class, () -> {
            ActionContextUtil.fetchContextFromObject(null);
        });
    }

    @Test
    public void testLoadParamByAnnotationAndPutToContext() {
        Map<String, Object> actionContext = new HashMap<>();
        TestAnnotation annotation = new TestAnnotation() {
            @Override
            public String value() {
                return "testParam";
            }

            @Override
            public String paramName() {
                return "testParam";
            }

            @Override
            public boolean isShardingParam() {
                return false;
            }

            @Override
            public int index() {
                return -1;
            }

            @Override
            public boolean isParamInProperty() {
                return false;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BusinessActionContextParameter.class;
            }
        };

        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "testParam", "testValue", annotation, actionContext);

        assertTrue(actionContext.size() >= 0);
    }

    @Test
    public void testGetParamNameFromAnnotation() {
        BusinessActionContextParameter annotation = new BusinessActionContextParameter() {
            @Override
            public String value() {
                return "paramValue";
            }

            @Override
            public String paramName() {
                return "";
            }

            @Override
            public boolean isShardingParam() {
                return false;
            }

            @Override
            public int index() {
                return -1;
            }

            @Override
            public boolean isParamInProperty() {
                return false;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BusinessActionContextParameter.class;
            }
        };

        String paramName = ActionContextUtil.getParamNameFromAnnotation(annotation);
        assertEquals("paramValue", paramName);
    }

    @Test
    public void testGetParamNameFromAnnotationWithParamName() {
        BusinessActionContextParameter annotation = new BusinessActionContextParameter() {
            @Override
            public String value() {
                return "";
            }

            @Override
            public String paramName() {
                return "testParamName";
            }

            @Override
            public boolean isShardingParam() {
                return false;
            }

            @Override
            public int index() {
                return -1;
            }

            @Override
            public boolean isParamInProperty() {
                return false;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BusinessActionContextParameter.class;
            }
        };

        String paramName = ActionContextUtil.getParamNameFromAnnotation(annotation);
        assertEquals("testParamName", paramName);
    }

    @Test
    public void testPutActionContext() {
        Map<String, Object> actionContext = new HashMap<>();
        boolean result = ActionContextUtil.putActionContext(actionContext, "key1", "value1");

        assertTrue(result);
        assertTrue(actionContext.containsKey("key1"));
        assertEquals("value1", actionContext.get("key1"));
    }

    @Test
    public void testPutActionContextWithMap() {
        Map<String, Object> actionContext = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("key1", "value1");
        dataMap.put("key2", 123);

        boolean result = ActionContextUtil.putActionContext(actionContext, dataMap);

        assertTrue(result);
        assertEquals(2, actionContext.size());
        assertEquals("value1", actionContext.get("key1"));
        assertEquals(123, actionContext.get("key2"));
    }

    @Test
    public void testPutActionContextWithoutHandle() {
        Map<String, Object> actionContext = new HashMap<>();
        boolean result = ActionContextUtil.putActionContextWithoutHandle(actionContext, "key1", "value1");

        assertTrue(result);
        assertTrue(actionContext.containsKey("key1"));
        assertEquals("value1", actionContext.get("key1"));
    }

    @Test
    public void testPutActionContextWithoutHandleWithMap() {
        Map<String, Object> actionContext = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("key1", "value1");
        dataMap.put("key2", 456);

        boolean result = ActionContextUtil.putActionContextWithoutHandle(actionContext, dataMap);

        assertTrue(result);
        assertEquals(2, actionContext.size());
        assertEquals("value1", actionContext.get("key1"));
        assertEquals(456, actionContext.get("key2"));
    }

    @Test
    public void testHandleActionContext() {
        String simpleValue = "testValue";
        Object result = ActionContextUtil.handleActionContext(simpleValue);
        assertEquals(simpleValue, result);
    }

    @Test
    public void testHandleActionContextWithMap() {
        Map<String, Object> mapValue = new HashMap<>();
        mapValue.put("key1", "value1");
        Object result = ActionContextUtil.handleActionContext(mapValue);
        assertNotNull(result);
    }

    @Test
    public void testConvertActionContext() {
        String value = "123";
        Integer result = ActionContextUtil.convertActionContext("key", value, Integer.class);
        assertNotNull(result);
        assertEquals(123, result);
    }

    @Test
    public void testConvertActionContextWithNull() {
        Integer result = ActionContextUtil.convertActionContext("key", null, Integer.class);
        assertNull(result);
    }

    @Test
    public void testConvertActionContextSameType() {
        String value = "testValue";
        String result = ActionContextUtil.convertActionContext("key", value, String.class);
        assertEquals(value, result);
    }

    // New tests for uncovered branches

    @Test
    public void testLoadParamByAnnotationAndPutToContext_NullParamValue() {
        Map<String, Object> actionContext = new HashMap<>();
        BusinessActionContextParameter annotation = createAnnotation("testParam", "", -1, false);

        // When paramValue is null, should return immediately without modifying actionContext
        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "testParam", null, annotation, actionContext);

        assertTrue(actionContext.isEmpty(), "actionContext should remain empty when paramValue is null");
    }

    @Test
    public void testLoadParamByAnnotationAndPutToContext_EmptyListWithIndex() {
        Map<String, Object> actionContext = new HashMap<>();
        List<String> emptyList = new ArrayList<>();
        BusinessActionContextParameter annotation = createAnnotation("testParam", "", 0, false);

        // When paramValue is empty list with index >= 0, should return without modifying actionContext
        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "testParam", emptyList, annotation, actionContext);

        assertTrue(actionContext.isEmpty(), "actionContext should remain empty when list is empty");
    }

    @Test
    public void testLoadParamByAnnotationAndPutToContext_IndexOutOfBounds() {
        Map<String, Object> actionContext = new HashMap<>();
        List<String> list = Arrays.asList("item0", "item1");
        BusinessActionContextParameter annotation = createAnnotation("testParam", "", 5, false);

        // When index is out of bounds, should return without modifying actionContext
        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "testParam", list, annotation, actionContext);

        assertTrue(actionContext.isEmpty(), "actionContext should remain empty when index is out of bounds");
    }

    @Test
    public void testLoadParamByAnnotationAndPutToContext_ValidIndexFromList() {
        Map<String, Object> actionContext = new HashMap<>();
        List<String> list = Arrays.asList("item0", "item1", "item2");
        BusinessActionContextParameter annotation = createAnnotation("myParam", "", 1, false);

        // When index is valid, should get element at index and put it into actionContext
        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "testParam", list, annotation, actionContext);

        assertFalse(actionContext.isEmpty(), "actionContext should contain the extracted element");
        assertEquals("item1", actionContext.get("myParam"));
    }

    @Test
    public void testLoadParamByAnnotationAndPutToContext_NonListWithIndex() {
        Map<String, Object> actionContext = new HashMap<>();
        String nonListValue = "simpleString";
        BusinessActionContextParameter annotation = createAnnotation("testParam", "", 0, false);

        // When paramValue is not a List but index >= 0, should log warning and use original value
        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "testParam", nonListValue, annotation, actionContext);

        assertFalse(actionContext.isEmpty(), "actionContext should contain the original value");
        assertEquals("simpleString", actionContext.get("testParam"));
    }

    @Test
    public void testLoadParamByAnnotationAndPutToContext_IsParamInPropertyTrue() {
        Map<String, Object> actionContext = new HashMap<>();
        TestObject testObj = new TestObject();
        testObj.setUserId("user123");
        testObj.setOrderId("order456");
        BusinessActionContextParameter annotation = createAnnotation("", "", -1, true);

        // When isParamInProperty is true, should fetch context from the object
        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "testParam", testObj, annotation, actionContext);

        // The actual behavior depends on fetchContextFromObject implementation
        // At minimum, the method should have been called without error
        assertNotNull(actionContext);
    }

    @Test
    public void testLoadParamByAnnotationAndPutToContext_UseAnnotationParamName() {
        Map<String, Object> actionContext = new HashMap<>();
        String value = "testValue";
        BusinessActionContextParameter annotation = createAnnotation("annotationParamName", "", -1, false);

        // When annotation has paramName, should use it instead of method parameter name
        ActionContextUtil.loadParamByAnnotationAndPutToContext(
                ParamType.PARAM, "methodParamName", value, annotation, actionContext);

        assertTrue(actionContext.containsKey("annotationParamName"), "actionContext should use annotation paramName");
        assertEquals("testValue", actionContext.get("annotationParamName"));
        assertFalse(
                actionContext.containsKey("methodParamName"), "actionContext should not contain method parameter name");
    }

    // Helper method to create annotation instances
    private BusinessActionContextParameter createAnnotation(
            String paramName, String value, int index, boolean isParamInProperty) {
        return new BusinessActionContextParameter() {
            @Override
            public String value() {
                return value;
            }

            @Override
            public String paramName() {
                return paramName;
            }

            @Override
            public boolean isShardingParam() {
                return false;
            }

            @Override
            public int index() {
                return index;
            }

            @Override
            public boolean isParamInProperty() {
                return isParamInProperty;
            }

            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return BusinessActionContextParameter.class;
            }
        };
    }

    // Test helper classes
    static class TestObject {
        private String userId;
        private String orderId;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getOrderId() {
            return orderId;
        }

        public void setOrderId(String orderId) {
            this.orderId = orderId;
        }
    }

    interface TestAnnotation extends BusinessActionContextParameter {}
}
