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
package io.seata.saga.engine;

import io.seata.common.SagaCostPrint;
import io.seata.saga.engine.mock.DemoService.Engineer;
import io.seata.saga.engine.mock.DemoService.People;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.parser.JsonParserFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * State machine tests
 *
 * @author lorne.cl
 */
public class StateMachineTests {

    private static StateMachineEngine stateMachineEngine;

    @BeforeAll
    public static void initApplicationContext() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:saga/spring/statemachine_engine_test.xml");
        stateMachineEngine = applicationContext.getBean("stateMachineEngine", StateMachineEngine.class);
    }

    @Test
    public void testSimpleStateMachine() {

        stateMachineEngine.start("simpleTestStateMachine", null, new HashMap<>());
    }

    @Test
    public void testSimpleStateMachineWithChoice() throws Exception {
        String stateMachineName = "simpleChoiceTestStateMachine";

        SagaCostPrint.executeAndPrint("1-1", () -> {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("a", 1);

            stateMachineEngine.start(stateMachineName, null, paramMap);
        });

        SagaCostPrint.executeAndPrint("1-2", () -> {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("a", 2);

            stateMachineEngine.start(stateMachineName, null, paramMap);
        });
    }

    @Test
    public void testSimpleStateMachineWithChoiceAndEnd() throws Exception {
        String stateMachineName = "simpleChoiceAndEndTestStateMachine";

        SagaCostPrint.executeAndPrint("1-3", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);
        });

        SagaCostPrint.executeAndPrint("1-4", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 3);
            stateMachineEngine.start(stateMachineName, null, paramMap);
        });
    }

    @Test
    public void testSimpleInputAssignmentStateMachine() throws Exception {
        String stateMachineName = "simpleInputAssignmentStateMachine";

        SagaCostPrint.executeAndPrint("1-5", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            String businessKey = inst.getStateList().get(0).getBusinessKey();
            Assertions.assertNotNull(businessKey);
            System.out.println("====== businessKey :" + businessKey);

            String contextBusinessKey = (String)inst.getEndParams().get(
                    inst.getStateList().get(0).getName() + DomainConstants.VAR_NAME_BUSINESSKEY);
            Assertions.assertNotNull(contextBusinessKey);
            System.out.println("====== context businessKey :" + businessKey);
        });
    }

    @Test
    public void testSimpleCatchesStateMachine() throws Exception {
        String stateMachineName = "simpleCachesStateMachine";

        SagaCostPrint.executeAndPrint("1-6", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testSimpleScriptTaskStateMachine() throws Exception {
        String stateMachineName = "simpleScriptTaskStateMachine";

        SagaCostPrint.executeAndPrint("1-7", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
            Assertions.assertNotNull(inst.getEndParams().get("scriptStateResult"));
        });

        SagaCostPrint.executeAndPrint("1-8", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });

        SagaCostPrint.executeAndPrint("1-9", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("scriptThrowException", true);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testSimpleRetryStateMachine() throws Exception {
        String stateMachineName = "simpleRetryStateMachine";

        SagaCostPrint.executeAndPrint("1-10", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testStatusMatchingStateMachine() throws Exception {
        String stateMachineName = "simpleStatusMatchingStateMachine";

        SagaCostPrint.executeAndPrint("1-11", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        });
    }

    @Test
    public void testCompensationStateMachine() throws Exception {
        String stateMachineName = "simpleCompensationStateMachine";

        SagaCostPrint.executeAndPrint("1-12", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    @Test
    public void testCompensationAndSubStateMachine() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        SagaCostPrint.executeAndPrint("1-13", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");


            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        });
    }

    @Test
    public void testCompensationAndSubStateMachineWithLayout() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        SagaCostPrint.executeAndPrint("1-14", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        });
    }

    @Test
    public void testStateComplexParams() {
        People people1 = new People();
        people1.setName("lilei");
        people1.setAge(18);

        People people2 = new People();
        people2.setName("lilei2");
        people2.setAge(19);

        People people3 = new People();
        people3.setName("lilei3");
        people3.setAge(20);

        People people4 = new People();
        people4.setName("lilei4");
        people4.setAge(21);

        people1.setChildrenArray(new People[]{people2});
        people1.setChildrenList(Collections.singletonList(people3));
        Map<String, People> map1 = new HashMap<>(1);
        map1.put("lilei4", people4);
        people1.setChildrenMap(map1);

        String json = JsonParserFactory.getJsonParser("jackson").toJsonString(people1, false, true);
        System.out.println(json);
    }

    @Test
    public void testStateMachineWithComplexParams() throws Exception {
        String stateMachineName = "simpleStateMachineWithComplexParams";

        SagaCostPrint.executeAndPrint("1-15", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            People people = new People();
            people.setName("lilei");
            people.setAge(18);

            Engineer engineer = new Engineer();
            engineer.setName("programmer");

            paramMap.put("people", people);
            paramMap.put("career", engineer);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            People peopleResult = (People)inst.getEndParams().get("complexParameterMethodResult");
            Assertions.assertNotNull(peopleResult);
            Assertions.assertEquals(people.getName(), peopleResult.getName());

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithAsyncState() throws Exception {
        String stateMachineName = "simpleStateMachineWithAsyncState";

        SagaCostPrint.executeAndPrint("1-16", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}