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
package org.apache.seata.server.cluster.raft.snapshot;

import com.alipay.sofa.jraft.entity.LocalFileMetaOutter;
import com.alipay.sofa.jraft.entity.RaftOutter;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.core.store.MappingDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.*;
import org.apache.seata.server.cluster.raft.snapshot.metadata.LeaderMetadataSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.session.SessionSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.vgroup.VGroupSnapshotFile;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.lock.*;
import org.apache.seata.server.session.*;
import org.apache.seata.server.storage.raft.session.RaftSessionManager;
import org.apache.seata.server.storage.raft.store.RaftVGroupMappingStoreManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StoreSnapshotUnitTest extends BaseSpringBootTest {
    @TempDir
    Path directory;

    private SnapshotWriter writer;
    private SnapshotReader reader;

    @BeforeEach
    void open() {
        writer = mock(SnapshotWriter.class);
        reader = mock(SnapshotReader.class);
        when(writer.getPath()).thenReturn(directory.toString());
        when(reader.getPath()).thenReturn(directory.toString());
        when(writer.addFile(anyString())).thenReturn(true);
        when(reader.getFileMeta(anyString())).thenReturn(LocalFileMetaOutter.LocalFileMeta.getDefaultInstance());
        when(reader.load())
                .thenReturn(RaftOutter.SnapshotMeta.newBuilder()
                        .setLastIncludedIndex(3)
                        .setLastIncludedTerm(1)
                        .build());
    }

    @Test
    void vgroupRestoreReplacesOnlyTargetGroup() throws Exception {
        RaftVGroupMappingStoreManager manager = mock(RaftVGroupMappingStoreManager.class);
        MappingDO mapping = new MappingDO();
        mapping.setVGroup("payments");
        mapping.setNamespace("tenant");
        mapping.setCluster("east");
        Map<String, MappingDO> mappings = new HashMap<>(Collections.singletonMap("payments", mapping));
        when(manager.loadVGroupsByUnit("group")).thenReturn(mappings);
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class)) {
            holder.when(SessionHolder::getRootVGroupMappingManager).thenReturn(manager);
            VGroupSnapshotFile file = spy(new VGroupSnapshotFile("group"));
            assertTrue(file.save(writer).isOk());
            doReturn(mappings).when(file).load(anyString());
            assertTrue(file.load(reader));
            verify(manager).clear("group");
            verify(manager)
                    .localAddVGroups(
                            argThat(m -> "tenant".equals(m.get("payments").getNamespace())), eq("group"));
            checkFailures(file);
        }
    }

    @Test
    void sessionRoundTripClearsOldSessionsAndLocks() throws Exception {
        RaftSessionManager manager = mock(RaftSessionManager.class);
        LockManager locks = mock(LockManager.class);
        GlobalSession session = GlobalSession.createGlobalSession("app", "group", "transaction", 1000);
        Map<String, GlobalSession> sessions = new HashMap<>();
        sessions.put(session.getXid(), session);
        when(manager.getSessionMap()).thenReturn(sessions);
        try (MockedStatic<SessionHolder> holder = mockStatic(SessionHolder.class);
                MockedStatic<LockerManagerFactory> factory = mockStatic(LockerManagerFactory.class)) {
            holder.when(() -> SessionHolder.getRootSessionManager("group")).thenReturn(manager);
            factory.when(LockerManagerFactory::getLockManager).thenReturn(locks);
            SessionSnapshotFile file = new SessionSnapshotFile("group");
            assertTrue(file.save(writer).isOk());
            sessions.put("stale", new GlobalSession());
            assertTrue(file.load(reader));
            assertEquals(Collections.singleton(session.getXid()), sessions.keySet());
            assertEquals("transaction", sessions.get(session.getXid()).getTransactionName());
            verify(locks).cleanAllLocks();
            checkFailures(file);
        }
    }

    @Test
    void leaderMetadataRoundTripUpdatesStateMachine() throws Exception {
        RaftServer server = mock(RaftServer.class);
        RaftStateMachine machine = mock(RaftStateMachine.class);
        when(server.getRaftStateMachine()).thenReturn(machine);
        RaftClusterMetadata metadata = new RaftClusterMetadata();
        when(machine.getRaftLeaderMetadata()).thenReturn(metadata);
        try (MockedStatic<RaftServerManager> manager = mockStatic(RaftServerManager.class)) {
            manager.when(() -> RaftServerManager.getRaftServer("group")).thenReturn(server);
            LeaderMetadataSnapshotFile file = new LeaderMetadataSnapshotFile("group");
            assertTrue(file.save(writer).isOk());
            assertTrue(file.load(reader));
            verify(machine).setRaftLeaderMetadata(any(RaftClusterMetadata.class));
            checkFailures(file);
        }
    }

    private void checkFailures(StoreSnapshotFile original) throws Exception {
        when(writer.addFile(anyString())).thenReturn(false);
        assertFalse(original.save(writer).isOk());
        StoreSnapshotFile file = mockingDetails(original).isSpy() ? original : spy(original);
        doReturn(false).when(file).save(any(RaftSnapshot.class), anyString());
        assertFalse(file.save(writer).isOk());
        doThrow(new IOException("disk full")).when(file).save(any(RaftSnapshot.class), anyString());
        assertFalse(file.save(writer).isOk());
        doThrow(new IOException("corrupt snapshot")).when(file).load(anyString());
        assertFalse(file.load(reader));
        when(reader.getFileMeta(anyString())).thenReturn(null);
        assertFalse(original.load(reader));
    }
}
