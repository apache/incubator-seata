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
package org.apache.seata.server.storage.raft.lock;

import org.apache.seata.core.store.DistributedLockDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class RaftDistributedLockerTest extends BaseSpringBootTest {

    private RaftDistributedLocker raftDistributedLocker;

    @BeforeEach
    public void setUp() {
        raftDistributedLocker = new RaftDistributedLocker();
    }

    @Test
    public void testReleaseLock() {
        DistributedLockDO lockDO = createDistributedLockDO();
        boolean result = raftDistributedLocker.releaseLock(lockDO);
        Assertions.assertTrue(result);
    }

    @Test
    public void testReleaseLockWithNull() {
        boolean result = raftDistributedLocker.releaseLock(null);
        Assertions.assertTrue(result);
    }

    @Test
    public void testReleaseLockWithEmptyKey() {
        DistributedLockDO lockDO = new DistributedLockDO();
        lockDO.setLockKey("");
        boolean result = raftDistributedLocker.releaseLock(lockDO);
        Assertions.assertTrue(result);
    }

    private DistributedLockDO createDistributedLockDO() {
        DistributedLockDO lockDO = new DistributedLockDO();
        lockDO.setLockKey("test-lock-key");
        lockDO.setLockValue("test-lock-value");
        lockDO.setExpireTime(30000L);
        return lockDO;
    }

    // ==================== acquireLock Tests ====================

    @Test
    public void testAcquireLock_WhenLeader() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class)) {
            // Mock as leader
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader(Mockito.anyString()))
                    .thenReturn(true);

            DistributedLockDO lockDO = createDistributedLockDO();
            boolean result = raftDistributedLocker.acquireLock(lockDO);

            Assertions.assertTrue(result, "Should acquire lock when node is leader");
        }
    }

    @Test
    public void testAcquireLock_WhenFollower() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class)) {
            // Mock as follower
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader(Mockito.anyString()))
                    .thenReturn(false);

            DistributedLockDO lockDO = createDistributedLockDO();
            boolean result = raftDistributedLocker.acquireLock(lockDO);

            Assertions.assertFalse(result, "Should not acquire lock when node is follower");
        }
    }

    @Test
    public void testAcquireLock_WithNullLockDO() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class)) {
            // Mock as leader
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader(Mockito.anyString()))
                    .thenReturn(true);

            boolean result = raftDistributedLocker.acquireLock(null);

            Assertions.assertTrue(result, "Should return leader status even with null lockDO");
        }
    }

    @Test
    public void testAcquireLock_WithEmptyKey() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class)) {
            // Mock as leader
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader(Mockito.anyString()))
                    .thenReturn(true);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("");
            boolean result = raftDistributedLocker.acquireLock(lockDO);

            Assertions.assertTrue(result, "Should return leader status even with empty key");
        }
    }

    @Test
    public void testAcquireLock_MultipleCallsAsLeader() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class)) {
            // Mock as leader
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader(Mockito.anyString()))
                    .thenReturn(true);

            DistributedLockDO lockDO1 = createDistributedLockDO();
            DistributedLockDO lockDO2 = createDistributedLockDO();
            lockDO2.setLockKey("another-key");

            boolean result1 = raftDistributedLocker.acquireLock(lockDO1);
            boolean result2 = raftDistributedLocker.acquireLock(lockDO2);

            Assertions.assertTrue(result1);
            Assertions.assertTrue(result2);
        }
    }

    @Test
    public void testAcquireLock_LeadershipChange() {
        try (MockedStatic<RaftServerManager> raftServerManagerMock = Mockito.mockStatic(RaftServerManager.class)) {
            DistributedLockDO lockDO = createDistributedLockDO();

            // First call - node is leader
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader(Mockito.anyString()))
                    .thenReturn(true);
            boolean result1 = raftDistributedLocker.acquireLock(lockDO);
            Assertions.assertTrue(result1, "Should acquire lock when leader");

            // Second call - node becomes follower
            raftServerManagerMock
                    .when(() -> RaftServerManager.isLeader(Mockito.anyString()))
                    .thenReturn(false);
            boolean result2 = raftDistributedLocker.acquireLock(lockDO);
            Assertions.assertFalse(result2, "Should not acquire lock after becoming follower");
        }
    }
}
