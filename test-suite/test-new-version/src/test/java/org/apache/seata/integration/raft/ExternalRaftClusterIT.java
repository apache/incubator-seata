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
import org.apache.seata.discovery.registry.RegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.RMClient;
import org.apache.seata.tm.DefaultTransactionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

class ExternalRaftClusterIT {

    private static final String APPLICATION_ID = "raft-current-client";
    private static final String TX_SERVICE_GROUP = "default_tx_group";
    private static final String CLUSTER = "default";
    private static final String METADATA_ADDRS_ENV = "SEATA_RAFT_METADATA_ADDRS";
    private static final String LEADER_ADDR_ENV = "SEATA_RAFT_LEADER_ADDR";
    private static final String LEADER_CONTROL_ADDR_ENV = "SEATA_RAFT_LEADER_CONTROL_ADDR";
    private static final String LEADER_TERM_ENV = "SEATA_RAFT_TERM";
    private static final String METADATA_MAX_AGE_KEY = "registry.raft.metadataMaxAgeMs";
    private static final long CLIENT_FAILOVER_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(45);

    private static TransactionManager transactionManager;
    private static DefaultResourceManager resourceManager;

    @BeforeAll
    static void setUp() {
        String metadataAddresses = requiredEnv(METADATA_ADDRS_ENV);
        ConfigurationTestHelper.putConfig("registry.type", "raft");
        ConfigurationTestHelper.putConfig("registry.raft.serverAddr", metadataAddresses);
        ConfigurationTestHelper.putConfig(METADATA_MAX_AGE_KEY, String.valueOf(TimeUnit.MINUTES.toMillis(2)));
        ConfigurationTestHelper.putConfig("service.vgroupMapping." + TX_SERVICE_GROUP, CLUSTER);
        ConfigurationTestHelper.removeConfig("service.default.grouplist");

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
        ConfigurationTestHelper.removeConfig(METADATA_MAX_AGE_KEY);
        ConfigurationTestHelper.removeConfig("registry.raft.serverAddr");
        ConfigurationTestHelper.removeConfig("registry.type");
    }

    @Test
    void shouldSupportCommitRollbackAndLeaderReelectionThroughRaftDiscovery() throws Exception {
        assertTransactionRoundTrip("before-reelection");

        String newLeaderAddress = RaftClusterFailoverHelper.forceLeaderReelection(
                requiredEnv(LEADER_ADDR_ENV),
                requiredEnv(LEADER_CONTROL_ADDR_ENV),
                Long.parseLong(requiredEnv(LEADER_TERM_ENV)));
        waitForClientLeaderSwitch(newLeaderAddress);

        assertTransactionRoundTrip("after-reelection");
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

    private static void waitForClientLeaderSwitch(String expectedLeaderAddress) throws Exception {
        RegistryService<?> registryService = RegistryFactory.getInstance();
        InetSocketAddress expectedLeader = toSocketAddress(expectedLeaderAddress);
        long deadline = System.currentTimeMillis() + CLIENT_FAILOVER_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            for (InetSocketAddress candidate : registryService.aliveLookup(TX_SERVICE_GROUP)) {
                if (sameAddress(candidate, expectedLeader)) {
                    return;
                }
            }
            Thread.sleep(1000L);
        }
        Assertions.fail("Timed out waiting for raft registry metadata to switch to leader " + expectedLeaderAddress);
    }

    private static long registerBranch(String xid, String phase) throws Exception {
        String suffix = phase + '-' + UUID.randomUUID();
        return resourceManager.branchRegister(
                BranchType.AT, "raft-discovery-resource-" + suffix, APPLICATION_ID, xid, "raft_table:" + suffix, "{}");
    }

    private static InetSocketAddress toSocketAddress(String address) {
        int separatorIndex = address.lastIndexOf(':');
        Assertions.assertTrue(separatorIndex > 0, "Invalid raft address: " + address);
        return new InetSocketAddress(
                address.substring(0, separatorIndex), Integer.parseInt(address.substring(separatorIndex + 1)));
    }

    private static boolean sameAddress(InetSocketAddress actual, InetSocketAddress expected) {
        return actual != null
                && expected != null
                && actual.getPort() == expected.getPort()
                && actual.getHostString().equals(expected.getHostString());
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        Assertions.assertNotNull(value, name + " must be provided");
        Assertions.assertFalse(value.trim().isEmpty(), name + " must not be blank");
        return value.trim();
    }
}
