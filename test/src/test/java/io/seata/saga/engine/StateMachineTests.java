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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * State machine tests
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
    public void testSimpleStateMachineWithChoice() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("a", 1);

        String stateMachineName = "simpleChoiceTestStateMachine";

        stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        start = System.currentTimeMillis();
        paramMap.put("a", 2);
        stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
    }

    @Test
    public void testSimpleStateMachineWithChoiceAndEnd() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleChoiceAndEndTestStateMachine";

        stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        start = System.currentTimeMillis();

        paramMap.put("a", 3);
        stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
    }

    @Test
    public void testSimpleInputAssignmentStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleInputAssignmentStateMachine";

        StateMachineInstance instance = stateMachineEngine.start(stateMachineName, null, paramMap);

        String businessKey = instance.getStateList().get(0).getBusinessKey();
        Assertions.assertNotNull(businessKey);
        System.out.println("====== businessKey :" + businessKey);

        String contextBusinessKey = (String) instance.getEndParams().get(
                instance.getStateList().get(0).getName() + DomainConstants.VAR_NAME_BUSINESSKEY);
        Assertions.assertNotNull(contextBusinessKey);
        System.out.println("====== context businessKey :" + businessKey);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
    }

    @Test
    public void testSimpleCatchesStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCachesStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertNotNull(inst.getException());
        Assertions.assertTrue(ExecutionStatus.FA.equals(inst.getStatus()));
    }

    @Test
    public void testSimpleScriptTaskStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleScriptTaskStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));
        Assertions.assertNotNull(inst.getEndParams().get("scriptStateResult"));


        start = System.currentTimeMillis();

        inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));


        start = System.currentTimeMillis();
        paramMap.put("scriptThrowException", true);
        inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.FA.equals(inst.getStatus()));
    }

    @Test
    public void testSimpleRetryStateMachine() {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleRetryStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertNotNull(inst.getException());
        Assertions.assertTrue(ExecutionStatus.FA.equals(inst.getStatus()));
    }

    @Test
    public void testStatusMatchingStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStatusMatchingStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertNotNull(inst.getException());
        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
    }

    @Test
    public void testCompensationStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getCompensationStatus()));
    }

    @Test
    public void testCompensationAndSubStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
    }

    @Test
    public void testCompensationAndSubStateMachineWithLayout() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
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

        people1.setChildrenArray(new People[] {people2});
        people1.setChildrenList(Arrays.asList(people3));
        Map<String, People> map1 = new HashMap<>(1);
        map1.put("lilei4", people4);
        people1.setChildrenMap(map1);

        String json = JsonParserFactory.getJsonParser("jackson").toJsonString(people1, false, true);
        System.out.println(json);
    }

    @Test
    public void testStateMachineWithComplexParams() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        People people = new People();
        people.setName("lilei");
        people.setAge(18);

        Engineer engineer = new Engineer();
        engineer.setName("programmer");

        paramMap.put("people", people);
        paramMap.put("career", engineer);

        String stateMachineName = "simpleStateMachineWithComplexParams";
        StateMachineInstance instance = stateMachineEngine.start(stateMachineName, null, paramMap);

        People peopleResult = (People) instance.getEndParams().get("complexParameterMethodResult");
        Assertions.assertNotNull(peopleResult);
        Assertions.assertTrue(people.getName().equals(people.getName()));

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(instance.getStatus()));
    }

    @Test
    public void testSimpleStateMachineWithAsyncState() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleStateMachineWithAsyncState";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}