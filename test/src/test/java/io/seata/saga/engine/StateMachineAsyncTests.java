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

import io.seata.common.LockAndCallback;
import io.seata.common.SagaCostPrint;
import io.seata.saga.engine.mock.DemoService.People;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * State machine async tests
 *
 * @author lorne.cl
 */
public class StateMachineAsyncTests {

    private static StateMachineEngine stateMachineEngine;

    @BeforeAll
    public static void initApplicationContext() {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:saga/spring/statemachine_engine_test.xml");
        stateMachineEngine = applicationContext.getBean("stateMachineEngine", StateMachineEngine.class);
    }

    @Test
    public void testSimpleCatchesStateMachine() throws Exception {
        String stateMachineName = "simpleCachesStateMachine";

        SagaCostPrint.executeAndPrint("2-1", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waitingForFinish(inst);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testSimpleScriptTaskStateMachine() throws Exception {
        String stateMachineName = "simpleScriptTaskStateMachine";

        SagaCostPrint.executeAndPrint("2-2", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
            Assertions.assertNotNull(inst.getEndParams().get("scriptStateResult"));
        });

        SagaCostPrint.executeAndPrint("2-3", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });

        SagaCostPrint.executeAndPrint("2-4", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("scriptThrowException", true);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testSimpleRetryStateMachine() throws Exception {
        String stateMachineName = "simpleRetryStateMachine";

        SagaCostPrint.executeAndPrint("2-5", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waitingForFinish(inst);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testStatusMatchingStateMachine() throws Exception {
        String stateMachineName = "simpleStatusMatchingStateMachine";

        SagaCostPrint.executeAndPrint("2-6", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waitingForFinish(inst);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        });
    }

    @Test
    @Disabled("FIXME")
    public void testCompensationStateMachine() throws Exception {
        String stateMachineName = "simpleCompensationStateMachine";

        SagaCostPrint.executeAndPrint("2-7", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waitingForFinish(inst);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            // FIXME: some times, the compensationStatus is RU
            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus(), "XID: " + inst.getId());
        });
    }

    @Test
    public void testCompensationAndSubStateMachine() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        SagaCostPrint.executeAndPrint("2-8", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waitingForFinish(inst);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        });
    }

    @Test
    public void testCompensationAndSubStateMachineWithLayout() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        SagaCostPrint.executeAndPrint("2-9", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waitingForFinish(inst);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        });
    }

    @Test
    public void testStateMachineWithComplexParams() throws Exception {
        String stateMachineName = "simpleStateMachineWithComplexParams";

        SagaCostPrint.executeAndPrint("2-10", () -> {
            People people = new People();
            people.setName("lilei");
            people.setAge(18);

            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("people", people);

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waitingForFinish(inst);

            People peopleResult = (People)inst.getEndParams().get("complexParameterMethodResult");
            Assertions.assertNotNull(peopleResult);
            Assertions.assertEquals(people.getName(), peopleResult.getName());
            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithAsyncState() throws Exception {
        String stateMachineName = "simpleStateMachineWithAsyncState";

        SagaCostPrint.executeAndPrint("2-11", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waitingForFinish(inst);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
