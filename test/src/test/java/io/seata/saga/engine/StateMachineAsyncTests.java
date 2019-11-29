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

import io.seata.saga.engine.mock.DemoService.People;
import io.seata.saga.proctrl.ProcessContext;
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

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertNotNull(inst.getException());
        Assertions.assertTrue(ExecutionStatus.FA.equals(inst.getStatus()));
    }

    @Test
    public void testSimpleRetryStateMachine() {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleRetryStateMachine";

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

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

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

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

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

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

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

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

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
    }

    @Test
    public void testStateMachineWithComplextParams() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        People people = new People();
        people.setName("lilei");
        people.setAge(18);
        paramMap.put("people", people);

        String stateMachineName = "simpleStateMachineWithComplexParams";

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;

        People peopleResult = (People) inst.getEndParams().get("complexParameterMethodResult");
        Assertions.assertNotNull(peopleResult);
        Assertions.assertTrue(people.getName().equals(people.getName()));

        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));
    }

    @Test
    public void testSimpleStateMachineWithAsyncState() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleStateMachineWithAsyncState";

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void waittingForFinish(StateMachineInstance inst) {
        synchronized (lock) {
            if (ExecutionStatus.RU.equals(inst.getStatus())) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private volatile Object        lock     = new Object();
    private          AsyncCallback callback = new AsyncCallback() {
        @Override
        public void onFinished(ProcessContext context, StateMachineInstance stateMachineInstance) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }

        @Override
        public void onError(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {
            synchronized (lock) {
                lock.notifyAll();
            }
        }
    };
}