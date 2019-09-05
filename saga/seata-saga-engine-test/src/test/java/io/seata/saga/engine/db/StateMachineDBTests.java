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
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import io.seata.server.Server;
import io.seata.tm.api.GlobalTransaction;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * State machine tests with db log store
 * @author lorne.cl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/statemachine_engine_db_test.xml" })
public class StateMachineDBTests {

    private static Server server;

    private StateMachineEngine stateMachineEngine;

    @BeforeClass
    public static void startSeataServer() throws InterruptedException {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server = new Server();
                    server.main(new String[]{});
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        })).start();
        Thread.sleep(5000);
    }

    private GlobalTransaction getGlobalTransaction(StateMachineInstance instance){
        Map<String, Object> params = instance.getContext();
        if(params != null){
            return (GlobalTransaction)params.get(DomainConstants.VAR_NAME_GLOBAL_TX);
        }
        return null;
    }

    @Test
    public void testSimpleStateMachine() {

        stateMachineEngine.start("simpleTestStateMachine", null, new HashMap<>());
    }

    @Test
    public void testSimpleStateMachineWithChoice() {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("a", 1);

        String stateMachineName = "simpleChoiceTestStateMachine";

        stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        start  = System.currentTimeMillis();
        paramMap.put("a", 2);
        stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
    }

    @Test
    public void testSimpleStateMachineWithChoiceAndEnd() {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleChoiceAndEndTestStateMachine";

        stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        start  = System.currentTimeMillis();

        paramMap.put("a", 3);
        stateMachineEngine.start(stateMachineName, null, paramMap);

        cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
    }

    @Test
    public void testSimpleInputAssignmentStateMachine() {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);

        String stateMachineName = "simpleInputAssignmentStateMachine";

        StateMachineInstance instance = stateMachineEngine.start(stateMachineName, null, paramMap);

        String businessKey = instance.getStateList().get(0).getBusinessKey();
        Assert.assertNotNull(businessKey);
        System.out.println("====== businessKey :" + businessKey);

        String contextBusinessKey = (String)instance.getEndParams().get(instance.getStateList().get(0).getName()+ DomainConstants.VAR_NAME_BUSINESSKEY);
        Assert.assertNotNull(contextBusinessKey);
        System.out.println("====== context businessKey :" + businessKey);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);
    }

    @Test
    public void testSimpleCatchesStateMachine() throws Exception {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCachesStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assert.assertNotNull(inst.getException());
        Assert.assertTrue(ExecutionStatus.FA.equals(inst.getStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assert.assertNotNull(globalTransaction);
        Assert.assertTrue(GlobalStatus.Finished.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testStatusMatchingStateMachine() throws Exception {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStatusMatchingStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assert.assertNotNull(inst.getException());
        Assert.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assert.assertNotNull(globalTransaction);
        System.out.println(globalTransaction.getStatus());
        Assert.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }


    @Test
    public void testCompensationStateMachine() throws Exception {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assert.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
        Assert.assertTrue(ExecutionStatus.SU.equals(inst.getCompensationStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assert.assertNotNull(globalTransaction);
        //End with Rollbacked = Finished
        Assert.assertTrue(GlobalStatus.Finished.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationAndSubStateMachine() throws Exception {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst = stateMachineEngine.start(stateMachineName, null, paramMap);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assert.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assert.assertNotNull(globalTransaction);
        Assert.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationStateMachineForRecovery() throws Exception {

        long start  = System.currentTimeMillis();

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
        Assert.assertNotNull(globalTransaction);
        System.out.println("====== GlobalStatus: "+globalTransaction.getStatus());

        // waiting for global transaction recover
        while (!(ExecutionStatus.SU.equals(inst.getStatus()) || ExecutionStatus.SU.equals(inst.getCompensationStatus()))){
            System.out.println("====== GlobalStatus: "+globalTransaction.getStatus());
            Thread.sleep(2000);
            inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance(inst.getId());
        }
    }

    @Test
    public void testReloadStateMachineInstance(){
        StateMachineInstance instance = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstance("10.15.232.93:8091:2019567124");
        System.out.println(instance);
    }

    @Autowired
    public void setStateMachineEngine(@Qualifier("stateMachineEngine") StateMachineEngine stateMachineEngine) {
        this.stateMachineEngine = stateMachineEngine;
    }
}