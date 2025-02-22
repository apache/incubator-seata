/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata;

import io.seata.core.rpc.netty.Action1Impl;
import io.seata.core.rpc.netty.ProtocolTestConstants;
import io.seata.core.rpc.netty.RmClientTest;
import io.seata.core.rpc.netty.RmRpcClient;
import io.seata.core.rpc.netty.TmClientTest;
import io.seata.core.rpc.netty.TmRpcClient;
import io.seata.rm.DefaultResourceManager;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import org.apache.seata.mockserver.MockCoordinator;
import org.apache.seata.mockserver.MockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the type MockServerTest
 */
public class MockTest {

    static String RESOURCE_ID = "mock-action-061";
    Logger logger = LoggerFactory.getLogger(MockTest.class);

    @BeforeAll
    public static void before() {
        MockServer.start(ProtocolTestConstants.MOCK_SERVER_PORT);
    }

    @AfterAll
    public static void after() {
        MockServer.close();
        TmRpcClient.getInstance().destroy();
        RmRpcClient.getInstance().destroy();
    }

    @Test
    public void testCommit() throws Exception {
        String xid = doTestCommit(0);
        Assertions.assertEquals(1, Action1Impl.getCommitTimes(xid));
        Assertions.assertEquals(0, Action1Impl.getRollbackTimes(xid));
    }

    @Test
    public void testCommitRetry() throws Exception {
        String xid = doTestCommit(2);
        Assertions.assertEquals(3, Action1Impl.getCommitTimes(xid));
        Assertions.assertEquals(0, Action1Impl.getRollbackTimes(xid));
    }

    @Test
    public void testRollback() throws Exception {
        String xid = doTestRollback(0);
        Assertions.assertEquals(0, Action1Impl.getCommitTimes(xid));
        Assertions.assertEquals(1, Action1Impl.getRollbackTimes(xid));
    }

    @Test
    public void testRollbackRetry() throws Exception {
        String xid = doTestRollback(2);
        Assertions.assertEquals(0, Action1Impl.getCommitTimes(xid));
        Assertions.assertEquals(3, Action1Impl.getRollbackTimes(xid));
    }

    @Test
    public void testTm() throws Exception {
        TmClientTest.testTm();
    }

    @Test
    public void testRm() throws Exception {
        RmClientTest.testRm("testRM01");
    }

    private String doTestCommit(int times) throws TransactionException, NoSuchMethodException {
        TransactionManager tm = TmClientTest.getTm();
        DefaultResourceManager rm = RmClientTest.getRm(RESOURCE_ID);

        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP, "test-commit", 60000);
        logger.info("doTestCommit(0.6.1) xid:{}", xid);
        MockCoordinator.getInstance().setExpectedRetry(xid, times);
        Long branchId = rm.branchRegister(BranchType.TCC, RESOURCE_ID, "1", xid, "{\"mock\":\"mock\"}", "1");
        logger.info("branch register(0.6.1) ok, branchId=" + branchId);
        GlobalStatus commit = tm.commit(xid);
        Assertions.assertEquals(GlobalStatus.Committed, commit);
        return xid;
    }

    private String doTestRollback(int times) throws TransactionException, NoSuchMethodException {
        TransactionManager tm = TmClientTest.getTm();
        DefaultResourceManager rm = RmClientTest.getRm(RESOURCE_ID);

        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP, "test-rollback", 60000);
        logger.info("doTestRollback(0.6.1) xid:{}", xid);
        MockCoordinator.getInstance().setExpectedRetry(xid, times);
        Long branchId = rm.branchRegister(BranchType.TCC, RESOURCE_ID, "1", xid, "{\"mock\":\"mock\"}", "1");
        logger.info("branch register(0.6.1) ok, branchId=" + branchId);
        GlobalStatus rollback = tm.rollback(xid);
        Assertions.assertEquals(GlobalStatus.Rollbacked, rollback);
        return xid;
    }
}
