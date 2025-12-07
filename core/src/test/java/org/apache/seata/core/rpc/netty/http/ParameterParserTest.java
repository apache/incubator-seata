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
package org.apache.seata.core.rpc.netty.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.seata.common.rpc.http.HttpContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_NONE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";

    @Test
    void testConvertParamMapWithSingleValue() throws JsonProcessingException {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("key1", Collections.singletonList("value1"));
        paramMap.put("key2", Collections.singletonList("value2"));

        ObjectNode result = ParameterParser.convertParamMap(paramMap);

        assertEquals("value1", result.get("key1").asText());
        assertEquals("value2", result.get("key2").asText());
    }

    @Test
    void testConvertParamMapWithMultipleValues() throws JsonProcessingException {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("key", Arrays.asList("value1", "value2", "value3"));

        ObjectNode result = ParameterParser.convertParamMap(paramMap);

        JsonNode arrayNode = result.get("key");
        assertTrue(arrayNode.isArray());
        assertEquals(3, arrayNode.size());
        assertEquals("value1", arrayNode.get(0).asText());
        assertEquals("value2", arrayNode.get(1).asText());
        assertEquals("value3", arrayNode.get(2).asText());
    }

    @Test
    void testConvertParamMapWithEmptyList() throws JsonProcessingException {
        Map<String, List<String>> paramMap = new HashMap<>();
        paramMap.put("emptyKey", Collections.emptyList());

        ObjectNode result = ParameterParser.convertParamMap(paramMap);

        assertNull(result.get("emptyKey"));
    }

    @Test
    void testGetArgValuesWithRequestBody() throws Exception {
        Method method = TestClass.class.getMethod("objectMethod", Object.class);

        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_BODY);

        ObjectNode paramMap = objectMapper.createObjectNode();
        ObjectNode bodyNode = paramMap.putObject("body");
        bodyNode.put("field1", "value1");
        bodyNode.put("field2", "value2");
        HttpContext httpContext = new HttpContext(null, null, false);
        Object[] args =
                ParameterParser.getArgValues(new ParamMetaData[] {paramMetaData}, method, paramMap, httpContext);

        assertEquals(1, args.length);
        assertNotNull(args[0]);
    }

    @Test
    void testGetArgValuesWithRequestParam() throws Exception {
        Method method = TestClassA.class.getMethod("objectMethod", String.class);

        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_PARAM);
        paramMetaData.setParamName("userName");
        paramMetaData.setDefaultValue("a");
        paramMetaData.setRequired(false);

        ObjectNode paramMap = objectMapper.createObjectNode();
        ObjectNode bodyNode = paramMap.putObject("param");
        bodyNode.put("userName", "LiHua");
        HttpContext httpContext = new HttpContext(null, null, false);
        Object[] args =
                ParameterParser.getArgValues(new ParamMetaData[] {paramMetaData}, method, paramMap, httpContext);

        assertEquals(1, args.length);
        assertNotNull(args[0]);
        assertEquals("LiHua", args[0]);
    }

    @Test
    void testGetArgValuesWithRequestParamAndDefaultValue() throws Exception {
        Method method = TestClassA.class.getMethod("objectMethod", String.class);

        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_PARAM);
        paramMetaData.setParamName("userName");
        paramMetaData.setDefaultValue("XiaMing");
        paramMetaData.setRequired(false);

        ObjectNode paramMap = objectMapper.createObjectNode();
        HttpContext httpContext = new HttpContext(null, null, false);
        Object[] args =
                ParameterParser.getArgValues(new ParamMetaData[] {paramMetaData}, method, paramMap, httpContext);

        assertEquals(1, args.length);
        assertNotNull(args[0]);
        assertEquals("XiaMing", args[0]);
    }

    @Test
    void testGetArgValuesWithRequestParamThrowException() throws Exception {
        Method method = TestClassA.class.getMethod("objectMethod", String.class);

        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_PARAM);
        paramMetaData.setParamName("userName");
        paramMetaData.setDefaultValue(DEFAULT_NONE);
        paramMetaData.setRequired(true);
        assertThrows(IllegalArgumentException.class, () -> {
            ObjectNode paramMap = objectMapper.createObjectNode();
            HttpContext httpContext = new HttpContext(null, null, false);
            ParameterParser.getArgValues(new ParamMetaData[] {paramMetaData}, method, paramMap, httpContext);
        });
    }

    @Test
    void testGetArgValuesWithRequestParamAndReturnNull() throws Exception {
        Method method = TestClassA.class.getMethod("objectMethod", String.class);

        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.REQUEST_PARAM);
        paramMetaData.setParamName("userName");
        paramMetaData.setRequired(false);

        ObjectNode paramMap = objectMapper.createObjectNode();
        HttpContext httpContext = new HttpContext(null, null, false);
        Object[] args =
                ParameterParser.getArgValues(new ParamMetaData[] {paramMetaData}, method, paramMap, httpContext);

        assertEquals(1, args.length);
        assertNull(args[0]);
    }

    @Test
    void testGetArgValuesWithJavaBeanParam() throws Exception {
        Method method = TestClassB.class.getMethod("objectMethod", User.class);

        ParamMetaData paramMetaData = new ParamMetaData();
        paramMetaData.setParamConvertType(ParamMetaData.ParamConvertType.MODEL_ATTRIBUTE);
        ObjectNode paramMap = objectMapper.createObjectNode();
        ObjectNode bodyNode = paramMap.putObject("param");
        bodyNode.put("name", "LiHua");
        bodyNode.put("age", 10);
        HttpContext httpContext = new HttpContext(null, null, false);
        Object[] args =
                ParameterParser.getArgValues(new ParamMetaData[] {paramMetaData}, method, paramMap, httpContext);

        assertEquals(1, args.length);
        assertTrue(args[0] instanceof User);
        assertEquals("LiHua", ((User) args[0]).name);
        assertEquals(10, ((User) args[0]).age);
    }

    // Test support class
    class TestClass {
        public void objectMethod(Object obj) {}
    }

    // Test support classA
    class TestClassA {
        public void objectMethod(String userName) {}
    }

    // Test support classB
    class TestClassB {
        public void objectMethod(User user) {}
    }

    static class User {
        String name;
        Integer age;

        public User() {}

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }
}
