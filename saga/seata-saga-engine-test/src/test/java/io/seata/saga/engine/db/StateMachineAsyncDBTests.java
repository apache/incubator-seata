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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * State machine async tests with db log store
 * @author lorne.cl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:spring/statemachine_engine_db_test.xml" })
public class StateMachineAsyncDBTests {

    private StateMachineEngine stateMachineEngine;

    private static Server server;

    @BeforeClass
    public static void startSeataServer() throws InterruptedException {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File("sessionStore/root.data");
                    if(file.exists()){
                        file.delete();
                    }

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
    public void testSimpleCatchesStateMachine() throws Exception {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCachesStateMachine";

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

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

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);


        Assert.assertNotNull(inst.getException());
        Assert.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assert.assertNotNull(globalTransaction);
        Assert.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationStateMachine() throws Exception {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 1);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleCompensationStateMachine";

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assert.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));
        Assert.assertTrue(ExecutionStatus.SU.equals(inst.getCompensationStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assert.assertNotNull(globalTransaction);
        Assert.assertTrue(GlobalStatus.Finished.equals(globalTransaction.getStatus()));
    }

    @Test
    public void testCompensationAndSubStateMachine() throws Exception {

        long start  = System.currentTimeMillis();

        Map<String, Object> paramMap = new HashMap<>(1);
        paramMap.put("a", 2);
        paramMap.put("barThrowException", "true");

        String stateMachineName = "simpleStateMachineWithCompensationAndSubMachine";

        StateMachineInstance inst = stateMachineEngine.startAsync(stateMachineName, null, paramMap, callback);

        waittingForFinish(inst);

        long cost = System.currentTimeMillis() - start;
        System.out.println("====== cost :" + cost);

        Assert.assertTrue(ExecutionStatus.UN.equals(inst.getStatus()));

        GlobalTransaction globalTransaction = getGlobalTransaction(inst);
        Assert.assertNotNull(globalTransaction);
        Assert.assertTrue(GlobalStatus.CommitRetrying.equals(globalTransaction.getStatus()));
    }

    @Autowired
    public void setStateMachineEngine(@Qualifier("stateMachineEngine") StateMachineEngine stateMachineEngine) {
        this.stateMachineEngine = stateMachineEngine;
    }

    private void waittingForFinish(StateMachineInstance inst){
        synchronized (lock){
            if(ExecutionStatus.RU.equals(inst.getStatus())){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private volatile Object lock = new Object();
    private AsyncCallback callback = new AsyncCallback() {
        @Override
        public void onFinished(ProcessContext context, StateMachineInstance stateMachineInstance) {
            synchronized (lock){
                lock.notifyAll();
            }
        }

        @Override
        public void onError(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {
            synchronized (lock){
                lock.notifyAll();
            }
        }
    };
}