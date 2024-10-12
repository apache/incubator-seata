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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.ConfigurationTestHelper;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.core.protocol.Protocol;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.apache.seata.mockserver.MockCoordinator;
import org.apache.seata.mockserver.MockServer;
import org.apache.seata.rm.DefaultResourceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the type MockServerTest
 */
public class MockGrpcServerTest {

    static String RESOURCE_ID = "mock-action";

    Logger logger = LoggerFactory.getLogger(MockGrpcServerTest.class);

    @BeforeAll
    public static void before() {
        ConfigurationFactory.reload();
        ConfigurationTestHelper.putConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL, String.valueOf(ProtocolTestConstants.MOCK_SERVER_PORT));
        ConfigurationTestHelper.putConfig(ConfigurationKeys.TRANSPORT_PROTOCOL, Protocol.GRPC.value);
        MockServer.start(ProtocolTestConstants.MOCK_SERVER_PORT);
        TmNettyRemotingClient.getInstance().destroy();
        RmNettyRemotingClient.getInstance().destroy();
    }

    @AfterAll
    public static void after() {
        //MockServer.close();
        ConfigurationTestHelper.removeConfig(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        ConfigurationTestHelper.removeConfig(ConfigurationKeys.TRANSPORT_PROTOCOL);
        TmNettyRemotingClient.getInstance().destroy();
        RmNettyRemotingClient.getInstance().destroy();
    }

    @Test
    public void testCommit() throws TransactionException {
        String xid = doTestCommit(0);
        Assertions.assertEquals(1, Action1Impl.getCommitTimes(xid));
        Assertions.assertEquals(0, Action1Impl.getRollbackTimes(xid));
    }

    @Test
    public void testCommitRetry() throws TransactionException {
        String xid = doTestCommit(2);
        Assertions.assertEquals(3, Action1Impl.getCommitTimes(xid));
        Assertions.assertEquals(0, Action1Impl.getRollbackTimes(xid));
    }

    @Test
    public void testRollback() throws TransactionException {
        String xid = doTestRollback(0);
        Assertions.assertEquals(0, Action1Impl.getCommitTimes(xid));
        Assertions.assertEquals(1, Action1Impl.getRollbackTimes(xid));
    }

    @Test
    public void testRollbackRetry() throws TransactionException {
        String xid = doTestRollback(2);
        Assertions.assertEquals(0, Action1Impl.getCommitTimes(xid));
        Assertions.assertEquals(3, Action1Impl.getRollbackTimes(xid));
    }

    private String doTestCommit(int times) throws TransactionException {
        TransactionManager tm = TmClientTest.getTm();
        DefaultResourceManager rm = RmClientTest.getRm(RESOURCE_ID);

        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP, "test-commit", 60000);
        MockCoordinator.getInstance().setExpectedRetry(xid, times);
        Long branchId = rm.branchRegister(BranchType.TCC, RESOURCE_ID, "1", xid, "{\"mock\":\"mock\"}", "1");
        GlobalStatus commit = tm.commit(xid);
        Assertions.assertEquals(GlobalStatus.Committed, commit);
        return xid;
    }

    private String doTestRollback(int times) throws TransactionException {
        TransactionManager tm = TmClientTest.getTm();
        DefaultResourceManager rm = RmClientTest.getRm(RESOURCE_ID);

        String xid = tm.begin(ProtocolTestConstants.APPLICATION_ID, ProtocolTestConstants.SERVICE_GROUP, "test-rollback", 60000);
        logger.info("doTestRollback xid:{}", xid);
        MockCoordinator.getInstance().setExpectedRetry(xid, times);
        Long branchId = rm.branchRegister(BranchType.TCC, RESOURCE_ID, "1", xid, "{\"mock\":\"mock\"}", "1");
        GlobalStatus rollback = tm.rollback(xid);
        Assertions.assertEquals(GlobalStatus.Rollbacked, rollback);
        return xid;

    }
}
