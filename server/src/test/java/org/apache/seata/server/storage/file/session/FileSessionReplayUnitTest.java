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
package org.apache.seata.server.storage.file.session;

import org.apache.seata.core.model.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.*;
import org.apache.seata.server.storage.file.*;
import org.apache.seata.server.storage.file.store.FileTransactionStoreManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileSessionReplayUnitTest extends BaseSpringBootTest {
    private GlobalSession global(long id, GlobalStatus status) {
        GlobalSession g = new GlobalSession("app", "group", "test", 1000);
        g.setXid("host:8091:" + id);
        g.setTransactionId(id);
        g.setStatus(status);
        return g;
    }

    private BranchSession branch(long tid, long bid, BranchStatus status) {
        BranchSession b = new BranchSession();
        b.setXid("host:8091:" + tid);
        b.setTransactionId(tid);
        b.setBranchId(bid);
        b.setStatus(status);
        return b;
    }

    private TransactionWriteStore event(SessionStorable s, LogOperation op) {
        return new TransactionWriteStore(s, op);
    }

    private FileSessionManager replay(List<TransactionWriteStore> history, List<TransactionWriteStore> current) {
        FileTransactionStoreManager store = mock(FileTransactionStoreManager.class);
        when(store.hasRemaining(true)).thenReturn(true, false);
        when(store.hasRemaining(false)).thenReturn(true, false);
        when(store.readWriteStore(anyInt(), eq(true))).thenReturn(history);
        when(store.readWriteStore(anyInt(), eq(false))).thenReturn(current);
        FileSessionManager manager = new FileSessionManager("replay");
        manager.setTransactionStoreManager(store);
        manager.reload();
        return manager;
    }

    @Test
    void outOfOrderBranchesAreReconciledAfterBothFiles() {
        BranchSession before = branch(1, 7, BranchStatus.Registered);
        FileSessionManager manager = replay(
                Arrays.asList(
                        event(before, LogOperation.BRANCH_ADD),
                        event(branch(2, 8, BranchStatus.Registered), LogOperation.BRANCH_ADD)),
                Arrays.asList(event(global(1, GlobalStatus.Begin), LogOperation.GLOBAL_ADD)));
        assertEquals(
                Collections.singleton("host:8091:1"), manager.getSessionMap().keySet());
        assertEquals(
                1, manager.findGlobalSession("host:8091:1").getBranchSessions().size());
        assertEquals(
                BranchStatus.Registered,
                manager.findGlobalSession("host:8091:1").getBranch(7).getStatus());
    }

    @Test
    void terminalAndRemovedGlobalsCannotBeResurrected() {
        FileSessionManager manager = replay(
                Arrays.asList(
                        event(global(1, GlobalStatus.Begin), LogOperation.GLOBAL_ADD),
                        event(branch(1, 7, BranchStatus.Registered), LogOperation.BRANCH_ADD)),
                Arrays.asList(
                        event(global(1, GlobalStatus.Committed), LogOperation.GLOBAL_UPDATE),
                        event(global(1, GlobalStatus.Begin), LogOperation.GLOBAL_ADD),
                        event(branch(1, 8, BranchStatus.Registered), LogOperation.BRANCH_ADD),
                        event(branch(1, 7, BranchStatus.Registered), LogOperation.BRANCH_REMOVE),
                        event(global(1, GlobalStatus.Begin), LogOperation.GLOBAL_REMOVE),
                        event(global(2, GlobalStatus.Begin), LogOperation.GLOBAL_REMOVE),
                        event(global(2, GlobalStatus.Begin), LogOperation.GLOBAL_REMOVE),
                        event(global(3, GlobalStatus.Rollbacked), LogOperation.GLOBAL_ADD)));
        assertTrue(manager.getSessionMap().isEmpty());
    }

    @Test
    void replayAppliesStatusUpdatesAndBranchRemovals() {
        FileSessionManager manager = replay(
                Arrays.asList(
                        event(global(1, GlobalStatus.Begin), LogOperation.GLOBAL_ADD),
                        event(branch(1, 7, BranchStatus.Registered), LogOperation.BRANCH_ADD)),
                Arrays.asList(
                        event(global(1, GlobalStatus.Committing), LogOperation.GLOBAL_UPDATE),
                        event(branch(1, 7, BranchStatus.PhaseOne_Done), LogOperation.BRANCH_UPDATE),
                        event(branch(1, 7, BranchStatus.Registered), LogOperation.BRANCH_REMOVE),
                        event(branch(1, 99, BranchStatus.Registered), LogOperation.BRANCH_REMOVE),
                        event(branch(2, 8, BranchStatus.Registered), LogOperation.BRANCH_REMOVE)));
        GlobalSession restored = manager.findGlobalSession("host:8091:1");
        assertEquals(GlobalStatus.Committing, restored.getStatus());
        assertTrue(restored.getBranchSessions().isEmpty());
        Map<String, GlobalSession> replacement = new HashMap<>();
        manager.setSessionMap(replacement);
        assertSame(replacement, manager.getSessionMap());
    }

    @Test
    void bufferedBranchAttachesToLaterGlobalAndRemovedBufferIsDiscarded() {
        FileSessionManager manager = replay(
                Arrays.asList(
                        event(branch(1, 7, BranchStatus.Registered), LogOperation.BRANCH_ADD),
                        event(branch(2, 8, BranchStatus.Registered), LogOperation.BRANCH_ADD)),
                Arrays.asList(
                        event(global(1, GlobalStatus.Begin), LogOperation.GLOBAL_ADD),
                        event(global(2, GlobalStatus.Finished), LogOperation.GLOBAL_ADD)));
        assertNotNull(manager.findGlobalSession("host:8091:1").getBranch(7));
        assertNull(manager.findGlobalSession("host:8091:2"));
    }
}
