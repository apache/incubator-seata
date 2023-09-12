/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.saga.statelang.builder;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.seata.saga.statelang.builder.impl.StateMachineBuilderImpl;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.State;
import io.seata.saga.statelang.domain.StateMachine;
import io.seata.saga.statelang.domain.impl.StateMachineImpl;
import io.seata.saga.statelang.parser.StateMachineParserFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.*;

/**
 * Java stream-based builder tests.
 *
 * @author lorne.cl
 */
public class StateBuilderTests {

    final static Map<String, String> SERVICE_STATUS_MAP = new LinkedHashMap<>();
    static ObjectWriter ow;
    static {
        SERVICE_STATUS_MAP.put("return.code == 'S'", ExecutionStatus.SU.name());
        SERVICE_STATUS_MAP.put("return.code == 'F'", ExecutionStatus.FA.name());
        SERVICE_STATUS_MAP.put("$exception{java.lang.Throwable}", ExecutionStatus.UN.name());
        String[] ignorableFieldNames = {"stateMachine"};
        FilterProvider filters = new SimpleFilterProvider().addFilter(
                "filter properties by name", SimpleBeanPropertyFilter.serializeAllExcept(ignorableFieldNames));
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(Object.class, PropertyFilterMixIn.class);
        ow = mapper.writer(filters).withDefaultPrettyPrinter();
    }

    @JsonFilter("filter properties by name")
    static class PropertyFilterMixIn {}

    @Test
    void testBuildStateMachine() throws IOException {
        ClassPathResource resource = new ClassPathResource("statelang/simple_statemachine.json");
        String json = io.seata.saga.statelang.parser.utils.IOUtils.toString(resource.getInputStream(), "UTF-8");
        StateMachine parsedStateMachine = StateMachineParserFactory.getStateMachineParser(null).parse(json);

        StateMachineBuilder stateMachineBuilder = new StateMachineBuilderImpl();
        StateMachine builtStateMachine = stateMachineBuilder
                .withName("simpleTestStateMachine")
                .withComment("测试状态机定义")
                .withStartState("FirstState")
                .withVersion("0.0.1")
                .withStates()
                    .build(ServiceTaskStateBuilder.class)
                        .withName("FirstState")
                        .withServiceName("is.seata.saga.DemoService")
                        .withServiceMethod("foo")
                        .withPersist(false)
                        .withNext("ScriptState")
                        .and()
                    .build(ScriptTaskStateBuilder.class)
                        .withName("ScriptState")
                        .withScriptType("groovy")
                        .withScriptContent("return 'hello ' + inputA")
                        .withInput(Collections.singletonList(new HashMap<String, Object>(){{
                            put("inputA", "$.data1");
                        }}))
                        .withOneOutput("scriptStateResult", "$.#root")
                        .withNext("ChoiceState")
                        .and()
                    .build(ChoiceStateBuilder.class)
                        .withName("ChoiceState")
                        .withChoice("foo == 1", "FirstMatchState")
                        .withChoice("foo == 2", "SecondMatchState")
                        .withDefault("FailState")
                        .and()
                    .build(ServiceTaskStateBuilder.class)
                        .withName("FirstMatchState")
                        .withServiceName("is.seata.saga.DemoService")
                        .withServiceMethod("bar")
                        .withCompensationState("CompensateFirst")
                        .withStatus(SERVICE_STATUS_MAP)
                        .withInput(Arrays.asList(
                                new HashMap<String, Object>() {{
                                    put("inputA1", "$.data1");
                                    put("inputA2", new HashMap<String, Object>() {{
                                        put("a", "$.data2.a");
                                    }});
                                }},
                                new HashMap<String, Object>() {{
                                    put("inputB", "$.header");
                                }}
                        ))
                        .withOneOutput("firstMatchStateResult", "$.#root")
                        .withOneRetry()
                            .withExceptions(Collections.singletonList(Exception.class))
                            .withIntervalSeconds(2)
                            .withMaxAttempts(3)
                            .withBackoffRate(1.5)
                            .and()
                        .withOneCatch()
                            .withExceptions(Collections.singletonList(Exception.class))
                            .withNext("CompensationTrigger")
                            .and()
                        .withNext("SuccessState")
                        .and()
                    .build(ServiceTaskStateBuilder.class)
                        .withName("CompensateFirst")
                        .withServiceName("is.seata.saga.DemoService")
                        .withServiceMethod("compensateBar")
                        .withForCompensation(true)
                        .withForUpdate(true)
                        .withInput(Collections.singletonList(new HashMap<String, Object>(){{
                            put("input", "$.data");
                        }}))
                        .withOneOutput("firstMatchStateResult", "$.#root")
                        .withStatus(SERVICE_STATUS_MAP)
                        .and()
                    .build(CompensationTriggerStateBuilder.class)
                        .withName("CompensationTrigger")
                        .withNext("CompensateEndState")
                        .and()
                    .build(FailEndStateBuilder.class)
                        .withName("CompensateEndState")
                        .withErrorCode("StateCompensated")
                        .withMessage("State Compensated!")
                        .and()
                    .build(SubStateMachineBuilder.class)
                        .withName("SecondMatchState")
                        .withStateMachineName("simpleTestStateMachine")
                        .withInput(Arrays.asList(
                                new HashMap<String, Object>() {{
                                    put("input", "$.data");
                                }},
                                new HashMap<String, Object>() {{
                                    put("header", "$.header");
                                }}
                        ))
                        .withOneOutput("firstMatchStateResult", "$.#root")
                        .withNext("SuccessState")
                        .and()
                    .build(FailEndStateBuilder.class)
                        .withName("FailState")
                        .withErrorCode("DefaultStateError")
                        .withMessage("No Matches!")
                        .and()
                    .build(SuccessEndStateBuilder.class)
                        .withName("SuccessState")
                        .and()
                    .configure()
                .build();
        assertStateMachineEquals((StateMachineImpl) parsedStateMachine, (StateMachineImpl) builtStateMachine);
    }

    static void assertStateMachineEquals(StateMachineImpl expected, StateMachineImpl target)
            throws JsonProcessingException {
        Assertions.assertEquals(expected.getName(), target.getName());
        Assertions.assertEquals(expected.getComment(), target.getComment());
        Assertions.assertEquals(expected.getVersion(), target.getVersion());
        Assertions.assertEquals(expected.getStartState(), target.getStartState());
        Assertions.assertEquals(expected.getRecoverStrategy(), target.getRecoverStrategy());
        Assertions.assertEquals(expected.isPersist(), target.isPersist());
        Assertions.assertEquals(expected.isRetryPersistModeUpdate(), target.isRetryPersistModeUpdate());
        Assertions.assertEquals(expected.isCompensatePersistModeUpdate(), target.isCompensatePersistModeUpdate());

        Assertions.assertEquals(expected.getStates().keySet(), target.getStates().keySet());
        for (String stateName: expected.getStates().keySet()) {
            if ("SecondMatchState".equals(stateName)) {
                // the compensateStateObject of SubStateMachine contains random string
                continue;
            }
            assertStateEquals(stateName, expected.getState(stateName), target.getState(stateName));
        }
    }

    static void assertStateEquals(String stateName, State expected, State target) throws JsonProcessingException {
        String expectedJson = ow.writeValueAsString(expected);
        String targetJson = ow.writeValueAsString(target);
        Assertions.assertEquals(expectedJson, targetJson, String.format("State [%s] is not identical", stateName));
    }
}
