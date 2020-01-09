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
package io.seata.saga.engine.db;

import io.seata.core.model.GlobalStatus;
import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.tm.api.GlobalTransaction;
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
public class StateMachineDBTests extends AbstractServerTest {

    private static StateMachineEngine stateMachineEngine;

    @BeforeAll
    public static void initApplicationContext() throws InterruptedException {

        startSeataServer();

        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:saga/spring/statemachine_engine_db_test.xml");
        stateMachineEngine = applicationContext.getBean("stateMachineEngine", StateMachineEngine.class);
    }

    private GlobalTransaction getGlobalTransaction(StateMachineInstance instance) {
        Map<String, Object> params = instance.getContext();
        if (params != null) {
            return (GlobalTransaction) params.get(DomainConstants.VAR_NAME_GLOBAL_TX);
        }
        return null;
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

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        Assertions.assertTrue(GlobalStatus.Finished.equals(globalTransaction.getStatus()));
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

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        System.out.println(globalTransaction.getStatus());
        Assertions.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
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

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        //End with Rollbacked = Finished
        Assertions.assertTrue(GlobalStatus.Finished.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationAndSubStateMachine() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        Assertions.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationAndSubStateMachineLayout() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        Assertions.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationStateMachineForRecovery() throws Exception {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("fooThrowExceptionRandomly", "true");
        paramMap.put("barThrowExceptionRandomly", "true");
        paramMap.put("compensateFooThrowExceptionRandomly", "true");
        paramMap.put("compensateBarThrowExceptionRandomly", "true");

        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());

        // waiting for global transaction recover
        while (!(ExecutionStatus.SU.equals(inst.getStatus()) || ExecutionStatus.SU.equals(inst.getCompensationStatus()))) {
            System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());
            Thread.sleep(2000);
            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
        }
    }

    @Test
    public void testReloadStateMachineInstance() {
        StateMachineInstance instance = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(
                "10.15.232.93:8091:2019567124");
        System.out.println(instance);
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
    public void testSimpleCatchesStateMachineAsync() throws Exception {

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

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        Assertions.assertTrue(GlobalStatus.Finished.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testSimpleRetryStateMachineAsync() {

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
    public void testStatusMatchingStateMachineAsync() throws Exception {

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

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        Assertions.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationStateMachineAsync() throws Exception {

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

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        Assertions.assertTrue(GlobalStatus.Finished.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationAndSubStateMachineAsync() throws Exception {

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

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        Assertions.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationAndSubStateMachineAsyncWithLayout() throws Exception {

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

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assertions.assertNotNull(globalTransaction);
        Assertions.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testAsyncStartSimpleStateMachineWithAsyncState() {

        long start = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleStateMachineWithAsyncState";

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

        Assertions.assertTrue(ExecutionStatus.SU.equals(inst.getStatus()));

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
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