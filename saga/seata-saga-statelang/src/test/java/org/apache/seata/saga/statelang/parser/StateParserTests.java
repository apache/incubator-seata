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
package org.apache.seata.saga.statelang.parser;

import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.json.JsonSerializerFactory;
import org.apache.seata.common.json.JsonUtil;
import org.apache.seata.common.util.BeanUtils;
import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
import org.apache.seata.saga.statelang.parser.impl.FastjsonParser;
import org.apache.seata.saga.statelang.parser.impl.JacksonJsonParser;
import org.apache.seata.saga.statelang.parser.impl.StateMachineParserImpl;
import org.apache.seata.saga.statelang.parser.utils.DesignerJsonTransformer;
import org.apache.seata.saga.statelang.parser.utils.IOUtils;
import org.apache.seata.saga.statelang.validator.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * StateParser tests
 */
public class StateParserTests {

    @Test
    public void testParser() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        StateMachine stateMachine =
                StateMachineParserFactory.getStateMachineParser(null).parse(json);
        stateMachine.setGmtCreate(new Date());
        Assertions.assertNotNull(stateMachine);

        JsonSerializer jsonSerializer = JsonSerializerFactory.getSerializer("jackson");
        String outputJson = jsonSerializer.toJSONString(stateMachine, true);
        System.out.println(outputJson);

        JsonSerializer fastjsonSerializer = JsonSerializerFactory.getSerializer("fastjson");
        String fastjsonOutputJson = fastjsonSerializer.toJSONString(stateMachine, true);
        System.out.println(fastjsonOutputJson);

        Assertions.assertEquals("simpleTestStateMachine", stateMachine.getName());
        Assertions.assertFalse(stateMachine.getStates().isEmpty());
    }

    @Test
    public void testParserUsesConfiguredFacadeWhenLegacyParserNameIsUnavailable() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine.json");
        String json = IOUtils.toString(inputStream, "UTF-8");

        StateMachine stateMachine = new StateMachineParserImpl("unavailable-legacy-provider").parse(json);

        Assertions.assertEquals("tracking:测试状态机定义", stateMachine.getComment());
    }

    @Test
    public void testLegacyParserNameEntryPointsAreDeprecated() throws NoSuchMethodException {
        Assertions.assertTrue(StateMachineParserFactory.class
                .getDeclaredMethod("getStateMachineParser", String.class)
                .isAnnotationPresent(Deprecated.class));
        Assertions.assertTrue(StateMachineParserImpl.class
                .getDeclaredConstructor(String.class)
                .isAnnotationPresent(Deprecated.class));
    }

    @Test
    public void testDeprecatedParsersDelegateTextOperationsToJsonUtil() {
        TestValue value = new TestValue("value");
        String typedJson = JsonUtil.toJSONString(value, false, false);
        String untypedJson = JsonUtil.toJSONString(value, true, false);

        assertParserDelegatesToJsonUtil(new FastjsonParser(), value, typedJson, untypedJson);
        assertParserDelegatesToJsonUtil(new JacksonJsonParser(), value, typedJson, untypedJson);
    }

    private void assertParserDelegatesToJsonUtil(
            JsonParser parser, TestValue value, String typedJson, String untypedJson) {
        Assertions.assertEquals(typedJson, parser.toJsonString(value, false, false));
        Assertions.assertEquals(JsonUtil.toJSONString(value, false, true), parser.toJsonString(value, false, true));
        Assertions.assertEquals(untypedJson, parser.toJsonString(value, true, false));
        Assertions.assertEquals(JsonUtil.toJSONString(value, true, true), parser.toJsonString(value, true, true));
        Assertions.assertEquals(JsonUtil.useAutoType(typedJson), parser.useAutoType(typedJson));
        Assertions.assertEquals(value, parser.parse(typedJson, TestValue.class, false));
        Assertions.assertEquals(value, parser.parse(untypedJson, TestValue.class, true));
    }

    @Test
    public void testDesignerJsonTransformer() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_layout.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        JsonSerializer jsonSerializer = JsonSerializerFactory.getSerializer("jackson");
        Map<String, Object> parsedObj =
                DesignerJsonTransformer.toStandardJson(jsonSerializer.parseObject(json, Map.class, true));
        Assertions.assertNotNull(parsedObj);

        String outputJson = jsonSerializer.toJSONString(parsedObj, true);
        System.out.println(outputJson);

        JsonSerializer fastjsonSerializer = JsonSerializerFactory.getSerializer("fastjson");
        Map<String, Object> fastjsonParsedObj =
                DesignerJsonTransformer.toStandardJson(fastjsonSerializer.parseObject(json, Map.class, true));
        Assertions.assertNotNull(fastjsonParsedObj);

        String fastjsonOutputJson = fastjsonSerializer.toJSONString(fastjsonParsedObj, true);
        System.out.println(fastjsonOutputJson);
    }

    @Test
    public void singleInfiniteLoopTest() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_single_infinite_loop.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        Throwable e = Assertions.assertThrows(ValidationException.class, () -> {
            StateMachineParserFactory.getStateMachineParser(null).parse(json);
        });
        System.out.println(e.getMessage());
        Assertions.assertTrue(e.getMessage().endsWith("without outgoing flow to end"));
    }

    @Test
    public void testMultipleInfiniteLoop() throws IOException {
        InputStream inputStream =
                getInputStreamByPath("statelang/simple_statemachine_with_multiple_infinite_loop.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        Throwable e = Assertions.assertThrows(ValidationException.class, () -> {
            StateMachineParserFactory.getStateMachineParser(null).parse(json);
        });
        System.out.println(e.getMessage());
        Assertions.assertTrue(e.getMessage().endsWith("without outgoing flow to end"));
    }

    @Test
    public void testNonExistedName() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_non_existed_name.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        Throwable e = Assertions.assertThrows(ValidationException.class, () -> {
            StateMachineParserFactory.getStateMachineParser(null).parse(json);
        });
        System.out.println(e.getMessage());
        Assertions.assertTrue(e.getMessage().endsWith("does not exist"));
    }

    @Test
    public void testRecursiveSubStateMachine() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_recursive_sub_machine.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        Throwable e = Assertions.assertThrows(ValidationException.class, () -> {
            StateMachineParserFactory.getStateMachineParser(null).parse(json);
        });
        Assertions.assertTrue(e.getMessage().endsWith("call itself"));
    }

    @Test
    public void testGenerateTracingGraphJson() throws Exception {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_layout.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        StateMachine stateMachine =
                StateMachineParserFactory.getStateMachineParser(null).parse(json);
        Map<String, String> machineMap = BeanUtils.objectToMap(stateMachine);
        StateMachineInstance instance =
                (StateMachineInstance) BeanUtils.mapToObject(machineMap, StateMachineInstanceImpl.class);
        Map<String, Object> context = new HashMap<>();
        context.put("test", "test");
        stateMachine.setContent(json);
        instance.setStateMachine(stateMachine);
        JsonSerializer jsonSerializer = JsonSerializerFactory.getSerializer("fastjson");
        String graphJson = DesignerJsonTransformer.generateTracingGraphJson(instance, jsonSerializer);
        Assertions.assertNotNull(graphJson);
    }

    private InputStream getInputStreamByPath(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getClass().getClassLoader();
        }

        return classLoader.getResourceAsStream(path);
    }

    public static class TestValue {

        private String value;

        public TestValue() {}

        TestValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof TestValue && value.equals(((TestValue) object).value);
        }
    }
}
