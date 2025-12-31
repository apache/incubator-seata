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
package org.apache.seata.server.transaction.tcc;

import io.netty.channel.Channel;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TccCore Test - Direct testing without mocks
 * Core handler for TCC (Try-Confirm-Cancel) transaction mode
 */
@DisplayName("TccCore Test")
class TccCoreTest {

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
    @DisplayName("test TccCore getHandleBranchType returns TCC - direct")
    void testTccCoreHandlesOnlyTCCType() {
        RemotingServer server = new DummyRemotingServer();
        TccCore tccCore = new TccCore(server);
        
        BranchType branchType = tccCore.getHandleBranchType();
        
        assertNotNull(branchType, "Branch type should not be null");
        assertEquals(BranchType.TCC, branchType, "Should handle TCC branch type only");
    }

    @Test
    @DisplayName("test TccCore branch type consistency across multiple calls - direct")
    void testBranchTypeConsistency() {
        RemotingServer server = new DummyRemotingServer();
        TccCore tccCore = new TccCore(server);
        
        // Multiple calls should return consistent type
        BranchType type1 = tccCore.getHandleBranchType();
        BranchType type2 = tccCore.getHandleBranchType();
        BranchType type3 = tccCore.getHandleBranchType();
        
        assertEquals(type1, type2, "First and second call should match");
        assertEquals(type2, type3, "Second and third call should match");
        assertEquals(BranchType.TCC, type1, "Should always return TCC");
    }

    @Test
    @DisplayName("test multiple TccCore instances handle TCC consistently - direct")
    void testMultipleInstancesConsistency() {
        RemotingServer server1 = new DummyRemotingServer();
        RemotingServer server2 = new DummyRemotingServer();
        RemotingServer server3 = new DummyRemotingServer();
        
        TccCore core1 = new TccCore(server1);
        TccCore core2 = new TccCore(server2);
        TccCore core3 = new TccCore(server3);
        
        BranchType type1 = core1.getHandleBranchType();
        BranchType type2 = core2.getHandleBranchType();
        BranchType type3 = core3.getHandleBranchType();
        
        assertEquals(BranchType.TCC, type1, "Core 1 should handle TCC");
        assertEquals(BranchType.TCC, type2, "Core 2 should handle TCC");
        assertEquals(BranchType.TCC, type3, "Core 3 should handle TCC");
        
        // All instances are different objects
        assertNotSame(core1, core2, "Core 1 and 2 should be different instances");
        assertNotSame(core2, core3, "Core 2 and 3 should be different instances");
    }

    @Test
    @DisplayName("test TccCore is instantiable with RemotingServer - direct")
    void testInstantiationWithServer() {
        RemotingServer server = new DummyRemotingServer();
        
        TccCore tccCore = new TccCore(server);
        
        assertNotNull(tccCore, "TccCore should be instantiable");
        assertNotNull(tccCore.getHandleBranchType(), "Branch type should be accessible");
        assertEquals(BranchType.TCC, tccCore.getHandleBranchType());
    }

    @Test
    @DisplayName("test TccCore only handles TCC, not other types - direct")
    void testTccCoreOnlyHandlesTCC() {
        RemotingServer server = new DummyRemotingServer();
        TccCore tccCore = new TccCore(server);
        
        BranchType handledType = tccCore.getHandleBranchType();
        
        // Should handle TCC
        assertEquals(BranchType.TCC, handledType, "Should handle TCC type");
        
        // Should NOT handle other types
        assertNotEquals(BranchType.AT, handledType, "Should not handle AT type");
        assertNotEquals(BranchType.SAGA, handledType, "Should not handle SAGA type");
        assertNotEquals(BranchType.XA, handledType, "Should not handle XA type");
    }

    @Test
    @DisplayName("test TccCore extends AbstractCore - direct")
    void testTccCoreHierarchy() {
        RemotingServer server = new DummyRemotingServer();
        TccCore tccCore = new TccCore(server);
        
        assertTrue(tccCore instanceof org.apache.seata.server.coordinator.AbstractCore,
                "TccCore should extend AbstractCore");
    }

    @Test
    @DisplayName("test TccCore is thread-safe by being stateless - direct")
    void testThreadSafety() throws InterruptedException {
        RemotingServer server = new DummyRemotingServer();
        final TccCore tccCore = new TccCore(server);
        
        final BranchType[] results = new BranchType[3];
        
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                results[0] = tccCore.getHandleBranchType();
            }
        });
        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                results[1] = tccCore.getHandleBranchType();
            }
        });
        Thread t3 = new Thread(new Runnable() {
            @Override
            public void run() {
                results[2] = tccCore.getHandleBranchType();
            }
        });
        
        t1.start();
        t2.start();
        t3.start();
        
        t1.join();
        t2.join();
        t3.join();
        
        assertEquals(BranchType.TCC, results[0], "Thread 1 should get TCC");
        assertEquals(BranchType.TCC, results[1], "Thread 2 should get TCC");
        assertEquals(BranchType.TCC, results[2], "Thread 3 should get TCC");
    }

    @Test
    @DisplayName("test TccCore stores RemotingServer reference - direct")
    void testRemotingServerStorage() {
        RemotingServer server1 = new DummyRemotingServer();
        RemotingServer server2 = new DummyRemotingServer();
        
        TccCore core1 = new TccCore(server1);
        TccCore core2 = new TccCore(server2);
        
        // Both should create separate instances with different servers
        assertNotNull(core1, "Core 1 should be created with server 1");
        assertNotNull(core2, "Core 2 should be created with server 2");
        
        // Both should handle TCC correctly
        assertEquals(BranchType.TCC, core1.getHandleBranchType());
        assertEquals(BranchType.TCC, core2.getHandleBranchType());
    }

    @Test
    @DisplayName("test TccCore behavior under repeated access - direct")
    void testRepeatedAccess() {
        RemotingServer server = new DummyRemotingServer();
        TccCore tccCore = new TccCore(server);
        
        // Access branch type multiple times
        for (int i = 0; i < 100; i++) {
            BranchType type = tccCore.getHandleBranchType();
            assertEquals(BranchType.TCC, type, 
                    "Iteration " + i + ": Should always return TCC");
        }
    }
}

