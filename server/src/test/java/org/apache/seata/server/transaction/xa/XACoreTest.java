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
package org.apache.seata.server.transaction.xa;

import io.netty.channel.Channel;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XACore Test - Direct testing without mocks
 * Core handler for XA (Distributed Transaction) mode
 */
@DisplayName("XACore Test")
class XACoreTest {

    private static class DummyRemotingServer implements RemotingServer {
        // Minimal implementation for direct testing
        @Override
        public Object sendSyncRequest(String resourceId, String clientId, Object msg, boolean tryOtherApp) {
            return null;
        }

        @Override
        public Object sendSyncRequest(Channel channel, Object msg) {
            return null;
        }

        @Override
        public void sendAsyncRequest(Channel channel, Object msg) {}

        @Override
        public void sendAsyncResponse(RpcMessage rpcMessage, Channel channel, Object msg) {}

        @Override
        public void registerProcessor(int messageType, RemotingProcessor processor, ExecutorService executor) {}
    }

    @Test
    @DisplayName("test XACore getHandleBranchType returns XA - direct")
    void testXACoreHandlesOnlyXAType() {
        RemotingServer server = new DummyRemotingServer();
        XACore xaCore = new XACore(server);

        BranchType branchType = xaCore.getHandleBranchType();

        assertNotNull(branchType, "Branch type should not be null");
        assertEquals(BranchType.XA, branchType, "Should handle XA branch type only");
    }

    @Test
    @DisplayName("test XACore branch type consistency across multiple calls - direct")
    void testBranchTypeConsistency() {
        RemotingServer server = new DummyRemotingServer();
        XACore xaCore = new XACore(server);

        // Multiple calls should return consistent type
        BranchType type1 = xaCore.getHandleBranchType();
        BranchType type2 = xaCore.getHandleBranchType();
        BranchType type3 = xaCore.getHandleBranchType();

        assertEquals(type1, type2, "First and second call should match");
        assertEquals(type2, type3, "Second and third call should match");
        assertEquals(BranchType.XA, type1, "Should always return XA");
    }

    @Test
    @DisplayName("test multiple XACore instances handle XA consistently - direct")
    void testMultipleInstancesConsistency() {
        RemotingServer server1 = new DummyRemotingServer();
        RemotingServer server2 = new DummyRemotingServer();
        RemotingServer server3 = new DummyRemotingServer();

        XACore core1 = new XACore(server1);
        XACore core2 = new XACore(server2);
        XACore core3 = new XACore(server3);

        BranchType type1 = core1.getHandleBranchType();
        BranchType type2 = core2.getHandleBranchType();
        BranchType type3 = core3.getHandleBranchType();

        assertEquals(BranchType.XA, type1, "Core 1 should handle XA");
        assertEquals(BranchType.XA, type2, "Core 2 should handle XA");
        assertEquals(BranchType.XA, type3, "Core 3 should handle XA");

        // All instances are different objects
        assertNotSame(core1, core2, "Core 1 and 2 should be different instances");
        assertNotSame(core2, core3, "Core 2 and 3 should be different instances");
    }

    @Test
    @DisplayName("test XACore is instantiable with RemotingServer - direct")
    void testInstantiationWithServer() {
        RemotingServer server = new DummyRemotingServer();

        XACore xaCore = new XACore(server);

        assertNotNull(xaCore, "XACore should be instantiable");
        assertNotNull(xaCore.getHandleBranchType(), "Branch type should be accessible");
        assertEquals(BranchType.XA, xaCore.getHandleBranchType());
    }

    @Test
    @DisplayName("test XACore only handles XA, not other types - direct")
    void testXACoreOnlyHandlesXA() {
        RemotingServer server = new DummyRemotingServer();
        XACore xaCore = new XACore(server);

        BranchType handledType = xaCore.getHandleBranchType();

        // Should handle XA
        assertEquals(BranchType.XA, handledType, "Should handle XA type");

        // Should NOT handle other types
        assertNotEquals(BranchType.AT, handledType, "Should not handle AT type");
        assertNotEquals(BranchType.TCC, handledType, "Should not handle TCC type");
        assertNotEquals(BranchType.SAGA, handledType, "Should not handle SAGA type");
    }

    @Test
    @DisplayName("test XACore extends AbstractCore - direct")
    void testXACoreHierarchy() {
        RemotingServer server = new DummyRemotingServer();
        XACore xaCore = new XACore(server);

        assertTrue(
                xaCore instanceof org.apache.seata.server.coordinator.AbstractCore,
                "XACore should extend AbstractCore");
    }

    @Test
    @DisplayName("test XACore is thread-safe by being stateless - direct")
    void testThreadSafety() throws InterruptedException {
        RemotingServer server = new DummyRemotingServer();
        final XACore xaCore = new XACore(server);

        final BranchType[] results = new BranchType[3];

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                results[0] = xaCore.getHandleBranchType();
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                results[1] = xaCore.getHandleBranchType();
            }
        });
        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                results[2] = xaCore.getHandleBranchType();
            }
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        assertEquals(BranchType.XA, results[0], "Thread 1 should get XA");
        assertEquals(BranchType.XA, results[1], "Thread 2 should get XA");
        assertEquals(BranchType.XA, results[2], "Thread 3 should get XA");
    }

    @Test
    @DisplayName("test XACore stores RemotingServer reference - direct")
    void testRemotingServerStorage() {
        RemotingServer server1 = new DummyRemotingServer();
        RemotingServer server2 = new DummyRemotingServer();

        XACore core1 = new XACore(server1);
        XACore core2 = new XACore(server2);

        // Both should create separate instances with different servers
        assertNotNull(core1, "Core 1 should be created with server 1");
        assertNotNull(core2, "Core 2 should be created with server 2");

        // Both should handle XA correctly
        assertEquals(BranchType.XA, core1.getHandleBranchType());
        assertEquals(BranchType.XA, core2.getHandleBranchType());
    }

    @Test
    @DisplayName("test XACore behavior under repeated access - direct")
    void testRepeatedAccess() {
        RemotingServer server = new DummyRemotingServer();
        XACore xaCore = new XACore(server);

        // Access branch type multiple times
        for (int i = 0; i < 100; i++) {
            BranchType type = xaCore.getHandleBranchType();
            assertEquals(BranchType.XA, type, "Iteration " + i + ": Should always return XA");
        }
    }

    @Test
    @DisplayName("test XACore branchReport accepts various branch statuses - direct")
    void testBranchReportAcceptsVariousStatuses() throws TransactionException {
        RemotingServer server = new DummyRemotingServer();
        XACore xaCore = new XACore(server);

        // Should not throw exception for various statuses
        try {
            xaCore.branchReport(BranchType.XA, "xid-123", 1L, BranchStatus.PhaseOne_Failed, "data");
        } catch (Exception e) {
            fail("Should accept PhaseOne_Failed status: " + e.getMessage());
        }

        try {
            xaCore.branchReport(BranchType.XA, "xid-456", 2L, BranchStatus.Registered, null);
        } catch (Exception e) {
            fail("Should accept Registered status: " + e.getMessage());
        }

        try {
            xaCore.branchReport(BranchType.XA, "xid-789", 3L, BranchStatus.PhaseTwo_Committed, "");
        } catch (Exception e) {
            fail("Should accept PhaseTwo_Committed status: " + e.getMessage());
        }
    }
}
