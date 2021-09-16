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
package io.seata.saga.engine.db.mockserver;

import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.mock.DemoService.Engineer;
import io.seata.saga.engine.mock.DemoService.People;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.saga.statelang.parser.JsonParser;
import io.seata.saga.statelang.parser.JsonParserFactory;
import io.seata.saga.statelang.parser.utils.DesignerJsonTransformer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * State machine tests with db log store
 * @author lorne.cl
 */
public class StateMachineDBMockServerTests {

    private static StateMachineEngine stateMachineEngine;

    @BeforeAll
    public static void initApplicationContext() throws InterruptedException {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:saga/spring/statemachine_engine_db_mockserver_test.xml");
        stateMachineEngine = applicationContext.getBean("stateMachineEngine", StateMachineEngine.class);
    }

    @Test
    public void testSimpleStateMachine() {

        stateMachineEngine.start("simpleTestStateMachine", null, new HashMap<>());
    }

    @Test
    public void testSimpleStateMachineWithChoice() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleChoiceTestStateMachine";
        String businessKey = String.valueOf(start);
        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey(stateMachineName, null, businessKey, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
        Assertions.assertNotNull(inst);
        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));

        inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstanceByBusinessKey(businessKey, null);
        Assertions.assertNotNull(inst);
        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));

        start = System.currentTimeMillis();
        paramMap.put("a", 2);
        inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
        Assertions.assertNotNull(inst);
        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));
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
    public void testSimpleCatchesStateMachine() throws Exception {

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
    public void testSimpleScriptTaskStateMachineWithLayout() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "designerSimpleScriptTaskStateMachine";

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
    public void testStatusMatchingStateMachine() throws Exception {

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
    public void testCompensationStateMachine() throws Exception {

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
    public void testSubStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        start = System.currentTimeMillis();

        paramMap.put("barThrowException", "false");
        inst = stateMachineEngine.forward(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));
    }

    @Test
    public void testSubStateMachineWithLayout() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        start = System.currentTimeMillis();

        paramMap.put("barThrowException", "false");
        inst = stateMachineEngine.forward(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));
    }

    @Test
    public void testForwardSubStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("fooThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        start = System.currentTimeMillis();

        paramMap.put("fooThrowException", "false");
        inst = stateMachineEngine.forward(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));
    }

    @Test
    public void testForwardSubStateMachineWithLayout() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("fooThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        JsonParser jsonParser = JsonParserFactory.getJsonParser("jackson");
        String graphJson = DesignerJsonTransformer.generateTracingGraphJson(inst, jsonParser);
        Assertions.assertNotNull(graphJson);
        System.out.println(graphJson);

        start = System.currentTimeMillis();

        paramMap.put("fooThrowException", "false");
        inst = stateMachineEngine.forward(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));

        String graphJson2 = DesignerJsonTransformer.generateTracingGraphJson(inst, jsonParser);
        Assertions.assertNotNull(graphJson2);
        System.out.println(graphJson2);
    }

    @Test
    public void testCompensateSubStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");
        paramMap.put("compensateFooThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        start = System.currentTimeMillis();

        inst = stateMachineEngine.compensate(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getCompensationStatus()));
    }

    @Test
    public void testCompensateSubStateMachineWithLayout() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");
        paramMap.put("compensateFooThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        JsonParser jsonParser = JsonParserFactory.getJsonParser("jackson");
        String graphJson = DesignerJsonTransformer.generateTracingGraphJson(inst, jsonParser);
        Assertions.assertNotNull(graphJson);
        System.out.println(graphJson);

        start = System.currentTimeMillis();

        inst = stateMachineEngine.compensate(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getCompensationStatus()));

        String graphJson2 = DesignerJsonTransformer.generateTracingGraphJson(inst, jsonParser);
        Assertions.assertNotNull(graphJson2);
        System.out.println(graphJson2);
    }

    @Test
    public void testUserDefCompensateSubStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");
        paramMap.put("compensateFooThrowException", "true");

        String stateMachineName = "simpleStateMachineWithUseDefCompensationSubMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        start = System.currentTimeMillis();

        paramMap.put("compensateFooThrowException", "false");
        inst = stateMachineEngine.compensate(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getCompensationStatus()));
    }

    @Test
    public void testCommitRetryingThenRetryCommitted() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("fooThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        paramMap.put("fooThrowException", "false");

        start = System.currentTimeMillis();

        inst = stateMachineEngine.forward(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));
    }

    @Test
    public void testCommitRetryingThenRetryRollbacked() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("fooThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        paramMap.put("fooThrowException", "false");
        paramMap.put("barThrowException", "true");

        start = System.currentTimeMillis();

        inst = stateMachineEngine.forward(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getCompensationStatus()));
    }

    @Test
    public void testRollbackRetryingThenRetryRollbacked() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");
        paramMap.put("compensateFooThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getCompensationStatus()));

        paramMap.put("barThrowException", "false");
        paramMap.put("compensateFooThrowException", "false");

        start = System.currentTimeMillis();

        inst = stateMachineEngine.compensate(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getCompensationStatus()));
    }

    @Test
    public void testRollbackRetryingTwiceThenRetryRollbacked() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");
        paramMap.put("compensateFooThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getCompensationStatus()));

        start = System.currentTimeMillis();

        inst = stateMachineEngine.compensate(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getCompensationStatus()));

        paramMap.put("barThrowException", "false");
        paramMap.put("compensateFooThrowException", "false");

        start = System.currentTimeMillis();

        inst = stateMachineEngine.compensate(inst.getId(), paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getCompensationStatus()));
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

        String stateMachineName = "simpleStateMachineWithComplexParamsJackson";

        StateMachineInstance instance = stateMachineEngine.start(stateMachineName, null, paramMap);

        People peopleResult = (People) instance.getEndParams().get("complexParameterMethodResult");
        Assertions.assertNotNull(peopleResult);
        Assertions.assertTrue(people.getName().equals(people.getName()));

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + instance.getId() + " cost :" + cost);

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

    @Test
    public void testReloadStateMachineInstance() {
        StateMachineInstance instance = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(
                "10.15.232.93:8091:2019567124");
        System.out.println(instance);
    }
}