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

import io.seata.common.LockAndCallback;
import io.seata.common.SagaCostPrint;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.StoreException;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.engine.exception.EngineExecutionException;
import io.seata.saga.engine.impl.DefaultStateMachineConfig;
import io.seata.saga.engine.mock.DemoService.Engineer;
import io.seata.saga.engine.mock.DemoService.People;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * State machine tests with db log store
 *
 * @author lorne.cl
 */
public class StateMachineDBTests extends AbstractServerTest {

    private static StateMachineEngine stateMachineEngine;

    private static int sleepTime = 1500;

    private static int sleepTimeLong = 2500;

    @BeforeAll
    public static void initApplicationContext() throws InterruptedException {

        startSeataServer();

        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:saga/spring/statemachine_engine_db_test.xml");
        stateMachineEngine = applicationContext.getBean("stateMachineEngine", StateMachineEngine.class);
    }

    @AfterAll
    public static void destory() throws InterruptedException {
        stopSeataServer();
    }

    private GlobalTransaction getGlobalTransaction(StateMachineInstance instance) {
        GlobalTransaction globalTransaction = null;
        Map<String, Object> params = instance.getContext();
        if (params != null) {
            globalTransaction = (GlobalTransaction) params.get(DomainConstants.VAR_NAME_GLOBAL_TX);
        }
        if (globalTransaction == null) {
            try {
                globalTransaction = GlobalTransactionContext.reload(instance.getId());
            } catch (TransactionException e) {
                e.printStackTrace();
            }
        }
        return globalTransaction;
    }

    @Test
    public void testSimpleStateMachine() {

        stateMachineEngine.start("simpleTestStateMachine", null, new HashMap<>());
    }

    @Test
    public void testSimpleStateMachineWithChoice() throws Exception {
        String stateMachineName = "simpleChoiceTestStateMachine";

        SagaCostPrint.executeAndPrint("3-1", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            stateMachineEngine.start(stateMachineName, null, paramMap);
        });

        SagaCostPrint.executeAndPrint("3-2", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 2);

            stateMachineEngine.start(stateMachineName, null, paramMap);
        });
    }

    @Test
    public void testSimpleStateMachineWithChoiceNoDefault() throws Exception {
        String stateMachineName = "simpleChoiceNoDefaultTestStateMachine";

        try {
            SagaCostPrint.executeAndPrint("3-3", () -> {
                Map<String, Object> paramMap = new HashMap<>(1);
                paramMap.put("a", 3);

                stateMachineEngine.start(stateMachineName, null, paramMap);
            });
        } catch (EngineExecutionException e) {
            Assertions.assertEquals(FrameworkErrorCode.StateMachineNoChoiceMatched, e.getErrcode());
            e.printStackTrace(System.out);
        }
    }

    @Test
    public void testSimpleStateMachineWithChoiceAndEnd() throws Exception {
        String stateMachineName = "simpleChoiceAndEndTestStateMachine";

        SagaCostPrint.executeAndPrint("3-4", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            stateMachineEngine.start(stateMachineName, null, paramMap);
        });

        SagaCostPrint.executeAndPrint("3-5", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 3);

            stateMachineEngine.start(stateMachineName, null, paramMap);
        });
    }

    @Test
    public void testSimpleInputAssignmentStateMachine() throws Exception {
        String stateMachineName = "simpleInputAssignmentStateMachine";

        SagaCostPrint.executeAndPrint("3-6", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            String businessKey = inst.getStateList().get(0).getBusinessKey();
            Assertions.assertNotNull(businessKey);
            System.out.println("====== businessKey :" + businessKey);

            String contextBusinessKey = (String) inst.getEndParams().get(
                    inst.getStateList().get(0).getName() + DomainConstants.VAR_NAME_BUSINESSKEY);
            Assertions.assertNotNull(contextBusinessKey);
            System.out.println("====== context businessKey :" + businessKey);
        });
    }

    @Test
    public void testSimpleCatchesStateMachine() throws Exception {
        String stateMachineName = "simpleCachesStateMachine";

        SagaCostPrint.executeAndPrint("3-7", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            Assertions.assertEquals(GlobalStatus.Finished, globalTransaction.getStatus());
        });
    }

    @Test
    public void testSimpleRetryStateMachine() throws Exception {
        String stateMachineName = "simpleRetryStateMachine";

        SagaCostPrint.executeAndPrint("3-8", () -> {
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

        SagaCostPrint.executeAndPrint("3-9", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            System.out.println(globalTransaction.getStatus());
            Assertions.assertEquals(GlobalStatus.CommitRetrying, globalTransaction.getStatus());
        });
    }

    @Test
    public void testStateMachineWithComplexParams() throws Exception {
        String stateMachineName = "simpleStateMachineWithComplexParamsJackson";

        SagaCostPrint.executeAndPrint("3-10", () -> {
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
    public void testCompensationStateMachine() throws Exception {
        String stateMachineName = "simpleCompensationStateMachine";

        SagaCostPrint.executeAndPrint("3-11", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            //End with Rollbacked = Finished
            Assertions.assertEquals(GlobalStatus.Finished, globalTransaction.getStatus());
        });
    }

    @Test
    public void testCompensationAndSubStateMachine() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        SagaCostPrint.executeAndPrint("3-12", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            Assertions.assertEquals(GlobalStatus.CommitRetrying, globalTransaction.getStatus());
        });
    }

    @Test
    public void testCompensationAndSubStateMachineLayout() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        SagaCostPrint.executeAndPrint("3-13", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            Assertions.assertEquals(GlobalStatus.CommitRetrying, globalTransaction.getStatus());
        });
    }

    @Test
    @Disabled("FIXME: Sometimes it takes a lot of time")
    public void testCompensationStateMachineForRecovery() throws Exception {
        String stateMachineName = "simpleCompensationStateMachineForRecovery";

        SagaCostPrint.executeAndPrint("3-14", () -> {
            Map<String, Object> paramMap = new HashMap<>(5);
            paramMap.put("a", 1);
            paramMap.put("fooThrowExceptionRandomly", "true");
            paramMap.put("barThrowExceptionRandomly", "true");
            paramMap.put("compensateFooThrowExceptionRandomly", "true");
            paramMap.put("compensateBarThrowExceptionRandomly", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());

            // waiting for global transaction recover
            while (!(ExecutionStatus.SU.equals(inst.getStatus()) || ExecutionStatus.SU.equals(inst.getCompensationStatus()))) {
                System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());
                Thread.sleep(1000);
                inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            }
        });
    }

    @Test
    public void testReloadStateMachineInstance() {
        StateMachineInstance instance = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(
                "10.15.232.93:8091:2019567124");
        System.out.println(instance);
    }

    @Test
    public void testSimpleStateMachineWithAsyncState() throws Exception {
        String stateMachineName = "simpleStateMachineWithAsyncState";

        SagaCostPrint.executeAndPrint("3-15", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testSimpleCatchesStateMachineAsync() throws Exception {
        String stateMachineName = "simpleCachesStateMachine";

        SagaCostPrint.executeAndPrint("3-16", () -> {

            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testSimpleRetryStateMachineAsync() throws Exception {
        String stateMachineName = "simpleRetryStateMachine";

        SagaCostPrint.executeAndPrint("3-17", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.FA, inst.getStatus());
        });
    }

    @Test
    public void testStatusMatchingStateMachineAsync() throws Exception {
        String stateMachineName = "simpleStatusMatchingStateMachine";

        SagaCostPrint.executeAndPrint("3-18", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            Assertions.assertEquals(GlobalStatus.CommitRetrying, globalTransaction.getStatus());
        });
    }

    @Disabled("https://github.com/seata/seata/issues/2564")
    public void testCompensationStateMachineAsync() throws Exception {
        String stateMachineName = "simpleCompensationStateMachine";

        SagaCostPrint.executeAndPrint("3-19", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            Assertions.assertEquals(GlobalStatus.Finished, globalTransaction.getStatus());
        });
    }

    @Test
    @Disabled("https://github.com/seata/seata/issues/2414#issuecomment-639546811")
    public void simpleChoiceTestStateMachineAsyncConcurrently() throws Exception {
        String stateMachineName = "simpleCompensationStateMachine";

        final int l1 = 10, l2 = 10;
        final CountDownLatch countDownLatch = new CountDownLatch(l1 * l2);
        final List<Exception> exceptions = new ArrayList<>();

        final AsyncCallback asyncCallback = new AsyncCallback() {
            @Override
            public void onFinished(ProcessContext context, StateMachineInstance stateMachineInstance) {
                countDownLatch.countDown();
            }

            @Override
            public void onError(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {
                exceptions.add(exp);
                countDownLatch.countDown();
            }
        };

        long start = System.nanoTime();
        for (int i = 0; i < l1; i++) {
            final int iValue = i;
            Thread t = new Thread(() -> {
                for (int j = 0; j < l2; j++) {
                    try {
                        SagaCostPrint.executeAndPrint("3-20_" + iValue + "-" + j, () -> {
                            Map<String, Object> paramMap = new HashMap<>(2);
                            paramMap.put("a", 1);
                            paramMap.put("barThrowException", "false");

                            try {
                                stateMachineEngine.startAsync(stateMachineName, null, paramMap, asyncCallback);
                            } catch (Exception e) {
                                exceptions.add(e);
                                countDownLatch.countDown();
                            }
                        });
                    } catch (Exception e) {
                        throw new RuntimeException("startAsync failed", e);
                    }
                }
            });
            t.start();
        }

        countDownLatch.await(10000, TimeUnit.MILLISECONDS);

        long cost = (System.nanoTime() - start) / 1000_000;
        System.out.println("====== cost3-20: " + cost + " ms");

        if (exceptions.size() > 0) {
            Assertions.fail(exceptions.get(0));
        }
    }

    @Test
    @Disabled("https://github.com/seata/seata/issues/2414#issuecomment-651526068")
    public void testCompensationAndSubStateMachineAsync() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        SagaCostPrint.executeAndPrint("3-21", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            Assertions.assertEquals(GlobalStatus.CommitRetrying, globalTransaction.getStatus());
        });
    }

    @Test
    @Disabled("https://github.com/seata/seata/issues/2414#issuecomment-640432396")
    public void testCompensationAndSubStateMachineAsyncWithLayout() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine_layout";

        SagaCostPrint.executeAndPrint("3-22", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            Assertions.assertEquals(GlobalStatus.CommitRetrying, globalTransaction.getStatus());
        });
    }

    @Test
    public void testAsyncStartSimpleStateMachineWithAsyncState() throws Exception {
        String stateMachineName = "simpleStateMachineWithAsyncState";

        SagaCostPrint.executeAndPrint("3-23", () -> {
            Map<String, Object> paramMap = new HashMap<>(1);
            paramMap.put("a", 1);

            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Disabled("FIXME: Sometimes it takes a lot of time")
    public void testStateMachineTransTimeout() throws Exception {
        ((DefaultStateMachineConfig)stateMachineEngine.getStateMachineConfig()).setTransOperationTimeout(1500);

        //first state timeout
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("a", 1);

        //timeout rollback after state machine finished (first state success)
        paramMap.put("fooSleepTime", sleepTime);
        doTestStateMachineTransTimeout(paramMap, 1);

        //timeout rollback before state machine finished (first state success)
        paramMap.put("fooSleepTime", sleepTimeLong);
        doTestStateMachineTransTimeout(paramMap, 2);

        //timeout rollback after state machine finished (first state fail)
        paramMap.put("fooSleepTime", sleepTime);
        paramMap.put("fooThrowException", "true");
        doTestStateMachineTransTimeout(paramMap, 3);

        //timeout rollback before state machine finished (first state fail)
        paramMap.put("fooSleepTime", sleepTimeLong);
        paramMap.put("fooThrowException", "true");
        doTestStateMachineTransTimeout(paramMap, 4);


        //last state timeout
        paramMap = new HashMap<>(3);
        paramMap.put("a", 1);

        //timeout rollback after state machine finished (last state success)
        paramMap.put("barSleepTime", sleepTime);
        doTestStateMachineTransTimeout(paramMap, 5);

        //timeout rollback before state machine finished (last state success)
        paramMap.put("barSleepTime", sleepTimeLong);
        doTestStateMachineTransTimeout(paramMap, 6);

        //timeout rollback after state machine finished (last state fail)
        paramMap.put("barSleepTime", sleepTime);
        paramMap.put("barThrowException", "true");
        doTestStateMachineTransTimeout(paramMap, 7);

        //timeout rollback before state machine finished (last state fail)
        paramMap.put("barSleepTime", sleepTimeLong);
        paramMap.put("barThrowException", "true");
        doTestStateMachineTransTimeout(paramMap, 8);

        ((DefaultStateMachineConfig)stateMachineEngine.getStateMachineConfig()).setTransOperationTimeout(60000 * 30);
    }

    @Test
    @Disabled("FIXME: Sometimes it takes a lot of time")
    public void testStateMachineTransTimeoutAsync() throws Exception {
        ((DefaultStateMachineConfig)stateMachineEngine.getStateMachineConfig()).setTransOperationTimeout(1500);

        //first state timeout
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("a", 1);

        //timeout rollback after state machine finished (first state success)
        paramMap.put("fooSleepTime", sleepTime);
        doTestStateMachineTransTimeoutAsync(paramMap, 1);

        //timeout rollback before state machine finished (first state success)
        paramMap.put("fooSleepTime", sleepTimeLong);
        doTestStateMachineTransTimeoutAsync(paramMap, 2);

        //timeout rollback after state machine finished (first state fail)
        paramMap.put("fooSleepTime", sleepTime);
        paramMap.put("fooThrowException", "true");
        doTestStateMachineTransTimeoutAsync(paramMap, 3);

        //timeout rollback before state machine finished (first state fail)
        paramMap.put("fooSleepTime", sleepTimeLong);
        paramMap.put("fooThrowException", "true");
        doTestStateMachineTransTimeoutAsync(paramMap, 4);


        //last state timeout
        paramMap = new HashMap<>(3);
        paramMap.put("a", 1);

        //timeout rollback after state machine finished (last state success)
        paramMap.put("barSleepTime", sleepTime);
        doTestStateMachineTransTimeoutAsync(paramMap, 5);

        //timeout rollback before state machine finished (last state success)
        paramMap.put("barSleepTime", sleepTimeLong);
        doTestStateMachineTransTimeoutAsync(paramMap, 6);

        //timeout rollback after state machine finished (last state fail)
        paramMap.put("barSleepTime", sleepTime);
        paramMap.put("barThrowException", "true");
        doTestStateMachineTransTimeoutAsync(paramMap, 7);

        //timeout rollback before state machine finished (last state fail)
        paramMap.put("barSleepTime", sleepTimeLong);
        paramMap.put("barThrowException", "true");
        doTestStateMachineTransTimeoutAsync(paramMap, 8);

        ((DefaultStateMachineConfig)stateMachineEngine.getStateMachineConfig()).setTransOperationTimeout(60000 * 30);
    }

    @Test
    public void testStateMachineRecordFailed() throws Exception {
        String stateMachineName = "simpleTestStateMachine";
        String businessKey = "bizKey";

        SagaCostPrint.executeAndPrint("3-24", () -> {
            Assertions.assertDoesNotThrow(() -> stateMachineEngine.startWithBusinessKey(stateMachineName, null, businessKey, new HashMap<>()));
        });

        SagaCostPrint.executeAndPrint("3-25", () -> {
            // use same biz key to mock exception
            Assertions.assertThrows(StoreException.class, () -> stateMachineEngine.startWithBusinessKey(stateMachineName, null, businessKey, new HashMap<>()));
            Assertions.assertNull(RootContext.getXID());
        });
    }

    @Test
    public void testSimpleRetryStateAsUpdateMode() throws Exception {
        String stateMachineName = "simpleUpdateStateMachine";

        SagaCostPrint.executeAndPrint("3-26", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            Thread.sleep(sleepTime);

            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            Assertions.assertEquals(2, inst.getStateList().size());
        });
    }

    @Test
    @Disabled("FIXME")
    public void testSimpleCompensateStateAsUpdateMode() throws Exception {
        String stateMachineName = "simpleUpdateStateMachine";

        SagaCostPrint.executeAndPrint("3-27", () -> {
            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 2);
            paramMap.put("barThrowException", "true");
            paramMap.put("compensateBarThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertNotNull(inst.getException());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            Thread.sleep(sleepTime);

            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            // FIXME: some times, the size is 4
            Assertions.assertEquals(3, inst.getStateList().size());
        });
    }

    @Test
    public void testSimpleSubRetryStateAsUpdateMode() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        SagaCostPrint.executeAndPrint("3-28", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 3);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            Thread.sleep(sleepTime);

            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            Assertions.assertEquals(2, inst.getStateList().size());
        });
    }

    @Test
    public void testSimpleSubCompensateStateAsUpdateMode() throws Exception {
        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        SagaCostPrint.executeAndPrint("3-29", () -> {
            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 4);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            Thread.sleep(sleepTime);

            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            Assertions.assertEquals(2, inst.getStateList().size());
        });
    }

    @Test
    public void testSimpleStateMachineWithLoop() throws Exception {
        String stateMachineName = "simpleLoopTestStateMachine";

        SagaCostPrint.executeAndPrint("3-30", () -> {
            List<Integer> loopList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                loopList.add(i);
            }

            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 1);
            paramMap.put("collection", loopList);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithLoopForward() throws Exception {
        String stateMachineName = "simpleLoopTestStateMachine";

        SagaCostPrint.executeAndPrint("3-31", () -> {
            List<Integer> loopList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                loopList.add(i);
            }

            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 1);
            paramMap.put("collection", loopList);
            paramMap.put("fooThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            Thread.sleep(sleepTime);

            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithLoopCompensate() throws Exception {
        String stateMachineName = "simpleLoopTestStateMachine";

        SagaCostPrint.executeAndPrint("3-32", () -> {
            List<Integer> loopList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                loopList.add(i);
            }

            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 1);
            paramMap.put("collection", loopList);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithLoopCompensateForRecovery() throws Exception {
        String stateMachineName = "simpleLoopTestStateMachine";

        SagaCostPrint.executeAndPrint("3-33", () -> {
            List<Integer> loopList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                loopList.add(i);
            }

            Map<String, Object> paramMap = new HashMap<>(4);
            paramMap.put("a", 1);
            paramMap.put("collection", loopList);
            paramMap.put("barThrowException", "true");
            paramMap.put("compensateFooThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getCompensationStatus());

            Thread.sleep(sleepTime);

            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getCompensationStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithLoopSubMachine() throws Exception {
        String stateMachineName = "simpleLoopTestStateMachine";

        SagaCostPrint.executeAndPrint("3-34", () -> {
            List<Integer> loopList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                loopList.add(i);
            }

            Map<String, Object> paramMap = new HashMap<>(2);
            paramMap.put("a", 2);
            paramMap.put("collection", loopList);

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
        });
    }

    @Test
    public void testSimpleStateMachineWithLoopSubMachineForward() throws Exception {
        String stateMachineName = "simpleLoopTestStateMachine";

        SagaCostPrint.executeAndPrint("3-35", () -> {
            List<Integer> loopList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                loopList.add(i);
            }

            Map<String, Object> paramMap = new HashMap<>(3);
            paramMap.put("a", 2);
            paramMap.put("collection", loopList);
            paramMap.put("barThrowException", "true");

            StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());

            Thread.sleep(sleepTime);

            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            Assertions.assertEquals(ExecutionStatus.UN, inst.getStatus());
        });
    }

    private void doTestStateMachineTransTimeout(Map<String, Object> paramMap, int i) throws Exception {
        String stateMachineName = "simpleCompensationStateMachine";

        SagaCostPrint.executeAndPrint("3-36-" + i, () -> {
            StateMachineInstance inst;
            try {
                inst = stateMachineEngine.start(stateMachineName, null, paramMap);
            } catch (EngineExecutionException e) {
                e.printStackTrace();

                inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(e.getStateMachineInstanceId());
            }

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());

            // waiting for global transaction recover
            while (!ExecutionStatus.SU.equals(inst.getCompensationStatus())) {
                System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());
                Thread.sleep(2500);
                inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            }

            Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus())
                    || ExecutionStatus.SU.equals(inst.getStatus()));
            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    private void doTestStateMachineTransTimeoutAsync(Map<String, Object> paramMap, int i) throws Exception {
        String stateMachineName = "simpleCompensationStateMachine";

        SagaCostPrint.executeAndPrint("3-37-" + i, () -> {
            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());

            // waiting for global transaction recover
            while (!ExecutionStatus.SU.equals(inst.getCompensationStatus())) {
                System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());
                Thread.sleep(2500);
                inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            }

            Assertions.assertTrue(ExecutionStatus.UN.equals(inst.getStatus())
                    || ExecutionStatus.SU.equals(inst.getStatus()));
            Assertions.assertEquals(ExecutionStatus.SU, inst.getCompensationStatus());
        });
    }

    @Test
    @Disabled("FIXME")
    public void testStateMachineCustomRecoverStrategyOnTimeout() throws Exception {
        ((DefaultStateMachineConfig)stateMachineEngine.getStateMachineConfig()).setTransOperationTimeout(1500);

        //first state timeout
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("a", 1);

        //timeout forward after state machine finished (first state success)
        paramMap.put("fooSleepTime", sleepTime);
        doTestStateMachineCustomRecoverStrategyOnTimeout(paramMap, 1);

        //timeout forward before state machine finished (first state success)
        paramMap.put("fooSleepTime", sleepTimeLong);
        doTestStateMachineCustomRecoverStrategyOnTimeout(paramMap, 2);

        //timeout forward after state machine finished (first state fail randomly)
        paramMap.put("fooSleepTime", sleepTime);
        paramMap.put("fooThrowExceptionRandomly", "true");
        doTestStateMachineCustomRecoverStrategyOnTimeout(paramMap, 3);

        //timeout forward before state machine finished (first state fail randomly)
        paramMap.put("fooSleepTime", sleepTimeLong);
        paramMap.put("fooThrowExceptionRandomly", "true");
        doTestStateMachineCustomRecoverStrategyOnTimeout(paramMap, 4);


        //last state timeout
        paramMap = new HashMap<>(3);
        paramMap.put("a", 1);

        //timeout forward after state machine finished (last state success)
        paramMap.put("barSleepTime", sleepTime);
        doTestStateMachineCustomRecoverStrategyOnTimeout(paramMap, 5);

        //timeout forward before state machine finished (last state success)
        paramMap.put("barSleepTime", sleepTimeLong);
        doTestStateMachineCustomRecoverStrategyOnTimeout(paramMap, 6);

        //timeout forward after state machine finished (last state fail randomly)
        paramMap.put("barSleepTime", sleepTime);
        paramMap.put("barThrowExceptionRandomly", "true");
        doTestStateMachineCustomRecoverStrategyOnTimeout(paramMap, 7);

        //timeout forward before state machine finished (last state fail randomly)
        paramMap.put("barSleepTime", sleepTimeLong);
        paramMap.put("barThrowExceptionRandomly", "true");
        doTestStateMachineCustomRecoverStrategyOnTimeout(paramMap, 8);

        ((DefaultStateMachineConfig)stateMachineEngine.getStateMachineConfig()).setTransOperationTimeout(60000 * 30);
    }

    private void doTestStateMachineCustomRecoverStrategyOnTimeout(Map<String, Object> paramMap, int i) throws Exception {
        String stateMachineName = "simpleStateMachineWithRecoverStrategy";

        SagaCostPrint.executeAndPrint("3-38-" + i, () -> {
            StateMachineInstance inst;
            try {
                inst = stateMachineEngine.start(stateMachineName, null, paramMap);
            } catch (EngineExecutionException e) {
                e.printStackTrace();

                inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(e.getStateMachineInstanceId());
            }

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());

            // waiting for global transaction recover
            while (!(ExecutionStatus.SU.equals(inst.getStatus())
                    && GlobalStatus.Finished.equals(globalTransaction.getStatus()))) {
                System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());
                System.out.println("====== StateMachineInstanceStatus: " + inst.getStatus());
                Thread.sleep(2500);
                inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            }

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
            Assertions.assertNull(inst.getCompensationStatus());
        });
    }

    @Test
    @Disabled("FIXME")
    public void testStateMachineCustomRecoverStrategyOnTimeoutAsync() throws Exception {
        ((DefaultStateMachineConfig)stateMachineEngine.getStateMachineConfig()).setTransOperationTimeout(1500);

        //first state timeout
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("a", 1);

        //timeout forward after state machine finished (first state success)
        paramMap.put("fooSleepTime", sleepTime);
        doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(paramMap, 1);

        //timeout forward before state machine finished (first state success)
        paramMap.put("fooSleepTime", sleepTimeLong);
        doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(paramMap, 2);

        //timeout forward after state machine finished (first state fail randomly)
        paramMap.put("fooSleepTime", sleepTime);
        paramMap.put("fooThrowExceptionRandomly", "true");
        doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(paramMap, 3);

        //timeout forward before state machine finished (first state fail randomly)
        paramMap.put("fooSleepTime", sleepTimeLong);
        paramMap.put("fooThrowExceptionRandomly", "true");
        doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(paramMap, 4);


        //last state timeout
        paramMap = new HashMap<>(3);
        paramMap.put("a", 1);

        //timeout forward after state machine finished (last state success)
        paramMap.put("barSleepTime", sleepTime);
        doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(paramMap, 5);

        //timeout forward before state machine finished (last state success)
        paramMap.put("barSleepTime", sleepTimeLong);
        doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(paramMap, 6);

        //timeout forward after state machine finished (last state fail randomly)
        paramMap.put("barSleepTime", sleepTime);
        paramMap.put("barThrowExceptionRandomly", "true");
        doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(paramMap, 7);

        //timeout forward before state machine finished (last state fail randomly)
        paramMap.put("barSleepTime", sleepTimeLong);
        paramMap.put("barThrowExceptionRandomly", "true");
        doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(paramMap, 8);

        ((DefaultStateMachineConfig)stateMachineEngine.getStateMachineConfig()).setTransOperationTimeout(60000 * 30);
    }

    private void doTestStateMachineCustomRecoverStrategyOnTimeoutAsync(Map<String, Object> paramMap, int i) throws Exception {
        String stateMachineName = "simpleStateMachineWithRecoverStrategy";

        SagaCostPrint.executeAndPrint("3-39-" + i, () -> {
            LockAndCallback lockAndCallback = new LockAndCallback();
            StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, lockAndCallback.getCallback());
            lockAndCallback.waittingForFinish(inst);

            GlobalTransaction globalTransaction = getGlobalTransaction(inst);
            Assertions.assertNotNull(globalTransaction);
            System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());

            // waiting for global transaction recover
            while (!(ExecutionStatus.SU.equals(inst.getStatus())
                    && GlobalStatus.Finished.equals(globalTransaction.getStatus()))) {
                System.out.println("====== GlobalStatus: " + globalTransaction.getStatus());
                System.out.println("====== StateMachineInstanceStatus: " + inst.getStatus());
                Thread.sleep(2500);
                inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
            }

            Assertions.assertEquals(ExecutionStatus.SU, inst.getStatus());
            Assertions.assertNull(inst.getCompensationStatus());
        });
    }
}
