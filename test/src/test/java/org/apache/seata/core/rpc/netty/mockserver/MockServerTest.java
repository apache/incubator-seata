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
package org.apache.seata.core.rpc.netty.mockserver;

import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.mockserver.MockCoordinator;
import org.apache.seata.mockserver.MockServer;
import org.apache.seata.rm.DefaultResourceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MockServerTest {

    static String RESOURCE_ID = "mock-action";

    @BeforeAll
    public static void before() {
        MockServer.start();
    }

    @AfterAll
    public static void after() {
        MockServer.close();
    }

    @Test
    public void testCommit() throws TransactionException {
        String xid = doTestCommit(0);
        Assertions.assertEquals(Action1Impl.getCommitTimes(xid), 1);
        Assertions.assertEquals(Action1Impl.getRollbackTimes(xid), 0);
    }

    @Test
    public void testCommitRetry() throws TransactionException {
        String xid = doTestCommit(2);
        Assertions.assertEquals(Action1Impl.getCommitTimes(xid), 3);
        Assertions.assertEquals(Action1Impl.getRollbackTimes(xid), 0);
    }

    @Test
    public void testRollback() throws TransactionException {
        String xid = doTestRollback(0);
        Assertions.assertEquals(Action1Impl.getCommitTimes(xid), 0);
        Assertions.assertEquals(Action1Impl.getRollbackTimes(xid), 1);
    }

    @Test
    public void testRollbackRetry() throws TransactionException {
        String xid = doTestRollback(2);
        Assertions.assertEquals(Action1Impl.getCommitTimes(xid), 0);
        Assertions.assertEquals(Action1Impl.getRollbackTimes(xid), 3);
    }

    private static String doTestCommit(int times) throws TransactionException {
        TransactionManager tm = TmClientTest.getTm();
        DefaultResourceManager rm = RmClientTest.getRm(RESOURCE_ID);

        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP, "test", 60000);
        MockCoordinator.getInstance().setExpectedRetry(xid, times);
        Long branchId = rm.branchRegister(BranchType.AT, RESOURCE_ID, "1", xid, "1", "1");
        GlobalStatus commit = tm.commit(xid);
        Assertions.assertEquals(commit, GlobalStatus.Committed);
        return xid;

    }

    private static String doTestRollback(int times) throws TransactionException {
        TransactionManager tm = TmClientTest.getTm();
        DefaultResourceManager rm = RmClientTest.getRm(RESOURCE_ID);

        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP, "test", 60000);
        MockCoordinator.getInstance().setExpectedRetry(xid, times);
        Long branchId = rm.branchRegister(BranchType.AT, RESOURCE_ID, "1", xid, "1", "1");
        GlobalStatus rollback = tm.rollback(xid);
        Assertions.assertEquals(rollback, GlobalStatus.Rollbacked);
        return xid;

    }
}
