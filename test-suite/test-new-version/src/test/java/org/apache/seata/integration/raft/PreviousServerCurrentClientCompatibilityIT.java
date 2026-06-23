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
package org.apache.seata.integration.raft;

import org.apache.seata.common.ConfigurationTestHelper;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.core.rpc.netty.RmNettyRemotingClient;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.RMClient;
import org.apache.seata.tm.DefaultTransactionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class PreviousServerCurrentClientCompatibilityIT {

    private static final String APPLICATION_ID = "raft-current-client";
    private static final String TX_SERVICE_GROUP = "default_tx_group";
    private static final String CLUSTER = "default";
    private static final String TX_ADDRS_ENV = "SEATA_RAFT_TX_ADDRS";

    private static TransactionManager transactionManager;
    private static DefaultResourceManager resourceManager;

    @BeforeAll
    static void setUp() {
        String txAddresses = requiredEnv(TX_ADDRS_ENV).replace(',', ';');
        ConfigurationTestHelper.putConfig("registry.type", "file");
        ConfigurationTestHelper.putConfig("service.vgroupMapping." + TX_SERVICE_GROUP, CLUSTER);
        ConfigurationTestHelper.putConfig("service.default.grouplist", txAddresses);

        TmNettyRemotingClient.getInstance(APPLICATION_ID, TX_SERVICE_GROUP).init();
        RMClient.init(APPLICATION_ID, TX_SERVICE_GROUP);
        transactionManager = new DefaultTransactionManager();
        resourceManager = DefaultResourceManager.get();
    }

    @AfterAll
    static void tearDown() {
        try {
            TmNettyRemotingClient.getInstance().destroy();
        } catch (Throwable ignored) {
        }
        try {
            RmNettyRemotingClient.getInstance().destroy();
        } catch (Throwable ignored) {
        }
        ConfigurationTestHelper.removeConfig("service.default.grouplist");
        ConfigurationTestHelper.removeConfig("service.vgroupMapping." + TX_SERVICE_GROUP);
        ConfigurationTestHelper.removeConfig("registry.type");
    }

    @Test
    void shouldSupportCommitAndRollbackAgainstPreviousRaftCluster() throws Exception {
        assertTransactionRoundTrip("previous-server");
    }

    private static void assertTransactionRoundTrip(String phase) throws Exception {
        String commitXid = transactionManager.begin(APPLICATION_ID, TX_SERVICE_GROUP, phase + "-commit", 60000);
        long commitBranchId = registerBranch(commitXid, phase + "-commit");
        Assertions.assertTrue(commitBranchId > 0, "Branch registration should succeed for commit flow");
        Assertions.assertEquals(GlobalStatus.Committed, transactionManager.commit(commitXid));

        String rollbackXid = transactionManager.begin(APPLICATION_ID, TX_SERVICE_GROUP, phase + "-rollback", 60000);
        long rollbackBranchId = registerBranch(rollbackXid, phase + "-rollback");
        Assertions.assertTrue(rollbackBranchId > 0, "Branch registration should succeed for rollback flow");
        GlobalStatus rollbackStatus = transactionManager.rollback(rollbackXid);
        Assertions.assertTrue(
                rollbackStatus == GlobalStatus.Rollbacked || rollbackStatus == GlobalStatus.RollbackRetrying,
                "Rollback should complete or enter retry state");
    }

    private static long registerBranch(String xid, String phase) throws Exception {
        String suffix = phase + '-' + UUID.randomUUID();
        return resourceManager.branchRegister(
                BranchType.AT, "raft-discovery-resource-" + suffix, APPLICATION_ID, xid, "raft_table:" + suffix, "{}");
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        Assertions.assertNotNull(value, name + " must be provided");
        Assertions.assertFalse(value.trim().isEmpty(), name + " must not be blank");
        return value.trim();
    }
}
