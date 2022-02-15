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

import io.seata.common.LockAndCallback;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.mock.DemoService.People;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * State machine async tests with db log store
 * @author lorne.cl
 */
public class StateMachineAsyncDBMockServerTests {

    private static StateMachineEngine stateMachineEngine;

    @BeforeAll
    public static void initApplicationContext() throws InterruptedException {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:saga/spring/statemachine_engine_db_mockserver_test.xml");
        stateMachineEngine = applicationContext.getBean("stateMachineEngine", StateMachineEngine.class);
    }

    @Test
    public void testSimpleCatchesStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCachesStateMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost4-1 :" + cost);

        Assertions.assertNotNull(inst.getException());
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
        System.out.println("====== XID: " + inst.getId() + " cost4-2 :" + cost);


        Assertions.assertNotNull(inst.getException());
        Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
    }

    @Test
    public void testStatusMatchingStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStatusMatchingStateMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost4-3 :" + cost);

        Assertions.assertNotNull(inst.getException());
        Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
    }

    @Test
    public void testCompensationStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost4-4 :" + cost);

        Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
    }

    @Test
    public void testCompensationAndSubStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost4-5 :" + cost);

        Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
    }

    @Test
    public void testCompensationAndSubStateMachineWithLayout() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== XID: " + inst.getId() + " cost4-6 :" + cost);

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

        String stateMachineName = "simpleStateMachineWithComplexParamsJackson";

        LockAndCallback lockAndCallback = new LockAndCallback();
        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());

        lockAndCallback.waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;

        People peopleResult = (People) inst.getEndParams().get("complexParameterMethodResult");
        Assertions.assertNotNull(peopleResult);
        Assertions.assertEquals(people.getName(), peopleResult.getName());

        System.out.println("====== XID: " + inst.getId() + " cost4-7 :" + cost);

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
        System.out.println("====== XID: " + inst.getId() + " cost4-8 :" + cost);

        Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
