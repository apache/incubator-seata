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

import org.apache.seata.common.util.BeanUtils;
import org.apache.seata.json.JsonParser;
import org.apache.seata.json.JsonParserFactory;
import org.apache.seata.saga.statelang.domain.StateMachine;
import org.apache.seata.saga.statelang.domain.StateMachineInstance;
import org.apache.seata.saga.statelang.domain.impl.StateMachineInstanceImpl;
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
                StateMachineParserFactory.getStateMachineParser().parse(json);
        stateMachine.setGmtCreate(new Date());
        Assertions.assertNotNull(stateMachine);

        JsonParser jsonParser = JsonParserFactory.getInstance();
        String outputJson = jsonParser.toJsonString(stateMachine, true);
        System.out.println(outputJson);

        Assertions.assertEquals("simpleTestStateMachine", stateMachine.getName());
        Assertions.assertFalse(stateMachine.getStates().isEmpty());
    }

    @Test
    public void testDesignerJsonTransformer() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_layout.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        JsonParser jsonParser = JsonParserFactory.getInstance();
        Map<String, Object> parsedObj = DesignerJsonTransformer.toStandardJson(jsonParser.parse(json, Map.class, true));
        Assertions.assertNotNull(parsedObj);

        String outputJson = jsonParser.toJsonString(parsedObj, true);
        System.out.println(outputJson);
    }

    @Test
    public void singleInfiniteLoopTest() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_single_infinite_loop.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        Throwable e = Assertions.assertThrows(ValidationException.class, () -> {
            StateMachineParserFactory.getStateMachineParser().parse(json);
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
            StateMachineParserFactory.getStateMachineParser().parse(json);
        });
        System.out.println(e.getMessage());
        Assertions.assertTrue(e.getMessage().endsWith("without outgoing flow to end"));
    }

    @Test
    public void testNonExistedName() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_non_existed_name.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        Throwable e = Assertions.assertThrows(ValidationException.class, () -> {
            StateMachineParserFactory.getStateMachineParser().parse(json);
        });
        System.out.println(e.getMessage());
        Assertions.assertTrue(e.getMessage().endsWith("does not exist"));
    }

    @Test
    public void testRecursiveSubStateMachine() throws IOException {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_recursive_sub_machine.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        Throwable e = Assertions.assertThrows(ValidationException.class, () -> {
            StateMachineParserFactory.getStateMachineParser().parse(json);
        });
        Assertions.assertTrue(e.getMessage().endsWith("call itself"));
    }

    @Test
    public void testGenerateTracingGraphJson() throws Exception {
        InputStream inputStream = getInputStreamByPath("statelang/simple_statemachine_with_layout.json");
        String json = IOUtils.toString(inputStream, "UTF-8");
        StateMachine stateMachine =
                StateMachineParserFactory.getStateMachineParser().parse(json);
        Map<String, String> machineMap = BeanUtils.objectToMap(stateMachine);
        StateMachineInstance instance =
                (StateMachineInstance) BeanUtils.mapToObject(machineMap, StateMachineInstanceImpl.class);
        Map<String, Object> context = new HashMap<>();
        context.put("test", "test");
        stateMachine.setContent(json);
        instance.setStateMachine(stateMachine);
        JsonParser jsonParser = JsonParserFactory.getInstance();
        String graphJson = DesignerJsonTransformer.generateTracingGraphJson(instance, jsonParser);
        Assertions.assertNotNull(graphJson);
    }

    private InputStream getInputStreamByPath(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = Thread.currentThread().getClass().getClassLoader();
        }

        return classLoader.getResourceAsStream(path);
    }
}
