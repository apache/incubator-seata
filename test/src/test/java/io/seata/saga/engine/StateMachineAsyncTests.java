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
    public void testSimpleCatchesStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCachesStateMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-1 :" + cost);

        Assertions.assertNotNull(inst.getException());
        Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
    }

    @Test
    public void testSimpleScriptTaskStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleScriptTaskStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-2 :" + cost);

        Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        Assertions.assertNotNull(inst.getEndParams().get("scriptStateResult"));


        start = System.currentTimeMillis();

        inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-3 :" + cost);

        Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());


        start = System.currentTimeMillis();
        paramMap.put("scriptThrowException", true);
        inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-4 :" + cost);

        Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
    }

    @Test
    public void testSimpleRetryStateMachine() {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleRetryStateMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-5 :" + cost);


        Assertions.assertNotNull(inst.getException());
        Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
    }

    @Test
    public void testStatusMatchingStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStatusMatchingStateMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-6 :" + cost);

        Assertions.assertNotNull(inst.getException());
        Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
    }

    @Test
    @Disabled("FIXME")
    public void testCompensationStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-7 :" + cost);

        Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        // FIXME: some times, the compensationStatus is RU
        Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus(), "XID: " + inst.getId());
    }

    @Test
    public void testCompensationAndSubStateMachine() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-8 :" + cost);

        Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
    }

    @Test
    public void testCompensationAndSubStateMachineWithLayout() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-9 :" + cost);

        Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
    }

    @Test
    public void testStateMachineWithComplexParams() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        People people = new People();
        people.setName("lilei");
        people.setAge(18);
        paramMap.put("people", people);

        String stateMachineName = "simpleStateMachineWithComplexParams";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;

        People peopleResult = (People) inst.getEndParams().get("complexParameterMethodResult");
        Assertions.assertNotNull(peopleResult);
        Assertions.assertEquals(people.getName(), peopleResult.getName());

        System.out.println("====== XID: " + inst.getId() + " cost2-10 :" + cost);

        Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
    }

    @Test
    public void testSimpleStateMachineWithAsyncState() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleStateMachineWithAsyncState";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost2-11 :" + cost);

        Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
