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

import io.seata.common.SagaCostPrint;
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
 *
 * @author lorne.cl
 */
public class StateMachineDBMockServerTests {

    private static StateMachineEngine stateMachineEngine;

    @BeforeAll
    public static void initApplicationContext() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:saga/spring/statemachine_engine_db_mockserver_test.xml");
        stateMachineEngine = applicationContext.getBean("stateMachineEngine", StateMachineEngine.class);
    }

    @Test
    public void testSimpleStateMachine() throws Exception {
        SagaCostPrint.executeAndPrint("5-1", () -> {
            stateMachineEngine.start("simpleTestStateMachine", null, new HashMap<>());
        });
    }

    @Test
    public void testSimpleStateMachineWithChoice() throws Exception {
        String stateMachineName = "simpleChoiceTestStateMachine";

        SagaCostPrint.executeAndPrint("5-2", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            String businessKey = String.valueOf(System.currentTimeMillis());
            StateMachineInstance inst = stateMachineEngine.startWithBusinessKey(stateMachineName, null, businessKey, paramMap);

            Assertions.assertNotNull(inst);
            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());

            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstanceByBusinessKey(businessKey, null);
            Assertions.assertNotNull(inst);
            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });

        SagaCostPrint.executeAndPrint("5-3", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 2);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst);
            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithChoiceAndEnd() throws Exception {
        String stateMachineName = "simpleChoiceAndEndTestStateMachine";

        SagaCostPrint.executeAndPrint("5-4", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);
        });

        SagaCostPrint.executeAndPrint("5-5", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);

            paramMap.put("a", 3);
            stateMachineEngine.start(stateMachineName, null, paramMap);
        });
    }

    @Test
    public void testSimpleInputAssignmentStateMachine() throws Exception {
        String stateMachineName = "simpleInputAssignmentStateMachine";

        SagaCostPrint.executeAndPrint("5-6", () -> {
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

        SagaCostPrint.executeAndPrint("5-7", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testSimpleScriptTaskStateMachineWithLayout() throws Exception {
        String stateMachineName = "designerSimpleScriptTaskStateMachine";

        SagaCostPrint.executeAndPrint("5-8", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
            Assertions.assertNotNull(inst.getEndParams().get("scriptStateResult"));
        });

        SagaCostPrint.executeAndPrint("5-9", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });

        SagaCostPrint.executeAndPrint("5-10", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("scriptThrowException", true);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testSimpleRetryStateMachine() throws Exception {
        String stateMachineName = "simpleRetryStateMachine";

        SagaCostPrint.executeAndPrint("5-11", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
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

        SagaCostPrint.executeAndPrint("5-12", () -> {
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

        SagaCostPrint.executeAndPrint("5-13", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    @Test
    public void testSubStateMachine() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-14", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-15", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "false");

            StateMachineInstance inst = stateMachineEngine.forward(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testSubStateMachineWithLayout() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-16", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-17", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "false");

            StateMachineInstance inst = stateMachineEngine.forward(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testForwardSubStateMachine() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-18", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("fooThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-19", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("fooThrowException", "false");

            StateMachineInstance inst = stateMachineEngine.forward(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testForwardSubStateMachineWithLayout() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        JsonParser jsonParser = JsonParserFactory.getJsonParser("jackson");

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-20", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("fooThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            String graphJson = DesignerJsonTransformer.generateTracingGraphJson(inst, jsonParser);
            Assertions.assertNotNull(graphJson);
            System.out.println(graphJson);

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-21", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("fooThrowException", "false");
            StateMachineInstance inst = stateMachineEngine.forward(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());

            String graphJson2 = DesignerJsonTransformer.generateTracingGraphJson(inst, jsonParser);
            Assertions.assertNotNull(graphJson2);
            System.out.println(graphJson2);
        });
    }

    @Test
    public void testCompensateSubStateMachine() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");
        paramMap.put("compensateFooThrowException", "true");

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-22", () -> {
            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-23", () -> {
            StateMachineInstance inst = stateMachineEngine.compensate(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getCompensationStatus());
        });
    }

    @Test
    public void testCompensateSubStateMachineWithLayout() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        JsonParser jsonParser = JsonParserFactory.getJsonParser("jackson");

        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");
        paramMap.put("compensateFooThrowException", "true");

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-24", () -> {
            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            String graphJson = DesignerJsonTransformer.generateTracingGraphJson(inst, jsonParser);
            Assertions.assertNotNull(graphJson);
            System.out.println(graphJson);

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-25", () -> {
            StateMachineInstance inst = stateMachineEngine.compensate(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getCompensationStatus());

            String graphJson2 = DesignerJsonTransformer.generateTracingGraphJson(inst, jsonParser);
            Assertions.assertNotNull(graphJson2);
            System.out.println(graphJson2);
        });
    }

    @Test
    public void testUserDefCompensateSubStateMachine() throws Exception {
        String stateMachineName = "simpleStateMachineWithUseDefCompensationSubMachine";

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-26", () -> {
            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");
            paramMap.put("compensateFooThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-27", () -> {
            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");
            paramMap.put("compensateFooThrowException", "false");

            StateMachineInstance inst = stateMachineEngine.compensate(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    @Test
    public void testCommitRetryingThenRetryCommitted() throws Exception {
        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-28", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("fooThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-29", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("fooThrowException", "false");

            StateMachineInstance inst = stateMachineEngine.forward(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testCommitRetryingThenRetryRollbacked() throws Exception {
        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-30", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("fooThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-31", () -> {
            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 1);
            paramMap.put("fooThrowException", "false");
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.forward(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    @Test
    public void testRollbackRetryingThenRetryRollbacked() throws Exception {
        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-32", () -> {
            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");
            paramMap.put("compensateFooThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getCompensationStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-33", () -> {
            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "false");
            paramMap.put("compensateFooThrowException", "false");

            StateMachineInstance inst = stateMachineEngine.compensate(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    @Test
    public void testRollbackRetryingTwiceThenRetryRollbacked() throws Exception {
        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");
        paramMap.put("compensateFooThrowException", "true");

        StateMachineInstance inst0 = SagaCostPrint.executeAndPrint("5-34", () -> {
            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getCompensationStatus());

            return inst;
        });

        SagaCostPrint.executeAndPrint("5-35", () -> {
            StateMachineInstance inst = stateMachineEngine.compensate(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getCompensationStatus());
        });

        paramMap.put("barThrowException", "false");
        paramMap.put("compensateFooThrowException", "false");
        SagaCostPrint.executeAndPrint("5-36", () -> {
            StateMachineInstance inst = stateMachineEngine.compensate(inst0.getId(), paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    @Test
    public void testStateMachineWithComplexParams() throws Exception {
        String stateMachineName = "simpleStateMachineWithComplexParamsJackson";

        SagaCostPrint.executeAndPrint("5-37", () -> {
            People people = new People();
            people.setName("lilei");
            people.setAge(18);

            Engineer engineer = new Engineer();
            engineer.setName("programmer");

            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("people", people);
            paramMap.put("career", engineer);

            StateMachineInstance instance = stateMachineEngine.start(stateMachineName, null, paramMap);

            People peopleResult = (People)instance.getEndParams().get("complexParameterMethodResult");
            Assertions.assertNotNull(peopleResult);
            Assertions.assertEquals(people.getName(), peopleResult.getName());

            Assertions.assertEquals(ExecutionStatus.SU, instance.getStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithAsyncState() throws Exception {
        String stateMachineName = "simpleStateMachineWithAsyncState";

        SagaCostPrint.executeAndPrint("5-38", () -> {
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

    @Test
    public void testReloadStateMachineInstance() throws Exception {
        SagaCostPrint.executeAndPrint("5-39", () -> {
            StateMachineInstance instance = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(
                    "10.15.232.93:8091:2019567124");
            System.out.println(instance);
        });
    }
}