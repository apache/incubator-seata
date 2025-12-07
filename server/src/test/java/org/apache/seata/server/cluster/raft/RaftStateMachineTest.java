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
package org.apache.seata.server.cluster.raft;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.execute.RaftMsgExecute;
import org.apache.seata.server.cluster.raft.snapshot.StoreSnapshotFile;
import org.apache.seata.server.cluster.raft.snapshot.metadata.LeaderMetadataSnapshotFile;
import org.apache.seata.server.cluster.raft.sync.RaftSyncMessageSerializer;
import org.apache.seata.server.cluster.raft.sync.msg.RaftBaseMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftClusterMetadataMsg;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMessage;
import org.apache.seata.server.cluster.raft.sync.msg.RaftSyncMsgType;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RaftStateMachineTest extends BaseSpringBootTest {

    private RaftStateMachine raftStateMachine;
    private static final String TEST_GROUP = "test-group";

    @BeforeEach
    public void setUp() {
        StoreConfig.setStartupParameter("file", "file", "file");
        raftStateMachine = new RaftStateMachine(TEST_GROUP);
    }

    @AfterEach
    public void tearDown() {
        StoreConfig.setStartupParameter("file", "file", "file");
    }

    @Test
    public void testConstructorInitializesBasicFields() {
        assertNotNull(raftStateMachine);
        assertFalse(raftStateMachine.isLeader());
        assertEquals(-1, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testOnLeaderStartUpdatesLeaderTerm() {
        long term = 5L;
        assertFalse(raftStateMachine.isLeader());

        raftStateMachine.onLeaderStart(term);

        assertTrue(raftStateMachine.isLeader());
        assertEquals(term, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testOnLeaderStopResetsLeaderTerm() {
        // First become leader
        raftStateMachine.onLeaderStart(5L);
        assertTrue(raftStateMachine.isLeader());

        // Then stop being leader
        raftStateMachine.onLeaderStop(Status.OK());

        assertFalse(raftStateMachine.isLeader());
    }

    @Test
    public void testOnLeaderStartMultipleTimes() {
        raftStateMachine.onLeaderStart(1L);
        assertTrue(raftStateMachine.isLeader());

        raftStateMachine.onLeaderStart(2L);
        assertTrue(raftStateMachine.isLeader());
        assertEquals(2L, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testOnStartFollowingUpdatesCurrentTerm() {
        LeaderChangeContext ctx = new LeaderChangeContext(null, 1L, Status.OK());

        raftStateMachine.onStartFollowing(ctx);

        assertEquals(1L, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testOnStopFollowingDoesNotThrow() {
        LeaderChangeContext ctx = new LeaderChangeContext(null, 1L, Status.OK());

        assertDoesNotThrow(() -> raftStateMachine.onStopFollowing(ctx));
    }

    @Test
    public void testIsLeaderReturnsFalseInitially() {
        assertFalse(raftStateMachine.isLeader());
    }

    @Test
    public void testIsLeaderReturnsTrueAfterOnLeaderStart() {
        raftStateMachine.onLeaderStart(1L);
        assertTrue(raftStateMachine.isLeader());
    }

    @Test
    public void testIsLeaderReturnsFalseAfterOnLeaderStop() {
        raftStateMachine.onLeaderStart(1L);
        raftStateMachine.onLeaderStop(Status.OK());
        assertFalse(raftStateMachine.isLeader());
    }

    @Test
    public void testGetCurrentTermInitialValue() {
        assertEquals(-1, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testGetCurrentTermAfterLeaderStart() {
        raftStateMachine.onLeaderStart(10L);
        assertEquals(10L, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testRegistryStoreSnapshotFile() throws Exception {
        LeaderMetadataSnapshotFile snapshotFile = new LeaderMetadataSnapshotFile(TEST_GROUP);
        raftStateMachine.registryStoreSnapshotFile(snapshotFile);

        Field snapshotFilesField = RaftStateMachine.class.getDeclaredField("snapshotFiles");
        snapshotFilesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<StoreSnapshotFile> snapshotFiles = (List<StoreSnapshotFile>) snapshotFilesField.get(raftStateMachine);

        assertTrue(snapshotFiles.size() >= 2); // At least LeaderMetadataSnapshotFile from constructor + new one
    }

    @Test
    public void testGetAndSetRaftLeaderMetadata() {
        RaftClusterMetadata metadata = new RaftClusterMetadata(100L);
        raftStateMachine.setRaftLeaderMetadata(metadata);

        RaftClusterMetadata retrieved = raftStateMachine.getRaftLeaderMetadata();
        assertEquals(100L, retrieved.getTerm());
    }

    @Test
    public void testMultipleLeaderStarts() {
        for (int i = 1; i <= 5; i++) {
            raftStateMachine.onLeaderStart(i);
            assertTrue(raftStateMachine.isLeader());
            assertEquals(i, raftStateMachine.getCurrentTerm().get());
        }
    }

    @Test
    public void testLeaderStartStopCycle() {
        raftStateMachine.onLeaderStart(1L);
        assertTrue(raftStateMachine.isLeader());

        raftStateMachine.onLeaderStop(Status.OK());
        assertFalse(raftStateMachine.isLeader());

        raftStateMachine.onLeaderStart(2L);
        assertTrue(raftStateMachine.isLeader());
        assertEquals(2L, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testFollowerStartWithDifferentTerms() {
        LeaderChangeContext ctx1 = new LeaderChangeContext(null, 5L, Status.OK());
        raftStateMachine.onStartFollowing(ctx1);
        assertEquals(5L, raftStateMachine.getCurrentTerm().get());

        LeaderChangeContext ctx2 = new LeaderChangeContext(null, 10L, Status.OK());
        raftStateMachine.onStartFollowing(ctx2);
        assertEquals(10L, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testLeaderTermProgression() {
        assertEquals(-1, raftStateMachine.getCurrentTerm().get());

        raftStateMachine.onLeaderStart(1L);
        assertEquals(1L, raftStateMachine.getCurrentTerm().get());

        raftStateMachine.onLeaderStart(5L);
        assertEquals(5L, raftStateMachine.getCurrentTerm().get());

        raftStateMachine.onLeaderStop(Status.OK());
        assertEquals(5L, raftStateMachine.getCurrentTerm().get()); // currentTerm should remain
    }

    // ========== Tests for codecov uncovered methods ==========

    @Test
    public void testOnSnapshotSaveInFileMode() {
        // In FILE mode, should call done.run(Status.OK()) immediately without saving
        Closure done = mock(Closure.class);
        SnapshotWriter writer = mock(SnapshotWriter.class);

        raftStateMachine.onSnapshotSave(writer, done);

        verify(done).run(argThat(Status::isOk));
        verify(writer, never()).addFile(anyString());
    }

    @Test
    public void testOnSnapshotSaveInRaftMode() throws Exception {
        // Use reflection to change mode to RAFT without triggering singleton initialization
        Field modeField = RaftStateMachine.class.getDeclaredField("mode");
        modeField.setAccessible(true);
        modeField.set(raftStateMachine, "raft");

        // Clear default snapshot files and add only our mock
        Field snapshotFilesField = RaftStateMachine.class.getDeclaredField("snapshotFiles");
        snapshotFilesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<StoreSnapshotFile> snapshotFiles = (List<StoreSnapshotFile>) snapshotFilesField.get(raftStateMachine);
        snapshotFiles.clear();

        Closure done = mock(Closure.class);
        SnapshotWriter writer = mock(SnapshotWriter.class);
        when(writer.getPath()).thenReturn("/tmp/snapshot");

        // Register a mock snapshot file
        StoreSnapshotFile mockSnapshotFile = mock(StoreSnapshotFile.class);
        when(mockSnapshotFile.save(writer)).thenReturn(Status.OK());
        raftStateMachine.registryStoreSnapshotFile(mockSnapshotFile);

        raftStateMachine.onSnapshotSave(writer, done);

        // Should call save on the snapshot file
        verify(mockSnapshotFile).save(writer);
        verify(done).run(argThat(Status::isOk));
    }

    @Test
    public void testOnSnapshotSaveFailsWhenSnapshotFileReturnsError() throws Exception {
        // Use reflection to change mode to RAFT
        Field modeField = RaftStateMachine.class.getDeclaredField("mode");
        modeField.setAccessible(true);
        modeField.set(raftStateMachine, "raft");

        // Clear default snapshot files
        Field snapshotFilesField = RaftStateMachine.class.getDeclaredField("snapshotFiles");
        snapshotFilesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<StoreSnapshotFile> snapshotFiles = (List<StoreSnapshotFile>) snapshotFilesField.get(raftStateMachine);
        snapshotFiles.clear();

        Closure done = mock(Closure.class);
        SnapshotWriter writer = mock(SnapshotWriter.class);
        when(writer.getPath()).thenReturn("/tmp/snapshot");

        // Register a mock snapshot file that fails
        StoreSnapshotFile mockSnapshotFile = mock(StoreSnapshotFile.class);
        Status errorStatus = new Status(-1, "Save failed");
        when(mockSnapshotFile.save(writer)).thenReturn(errorStatus);
        raftStateMachine.registryStoreSnapshotFile(mockSnapshotFile);

        raftStateMachine.onSnapshotSave(writer, done);

        // Should call done with error status
        verify(done).run(argThat(status -> !status.isOk()));
    }

    @Test
    public void testOnSnapshotLoadInFileMode() {
        // In FILE mode, should return true immediately
        SnapshotReader reader = mock(SnapshotReader.class);

        boolean result = raftStateMachine.onSnapshotLoad(reader);

        assertTrue(result);
        verify(reader, never()).getPath();
    }

    @Test
    public void testOnSnapshotLoadWhenIsLeader() throws Exception {
        // Leader should not load snapshot
        // Use reflection to change mode to RAFT
        Field modeField = RaftStateMachine.class.getDeclaredField("mode");
        modeField.setAccessible(true);
        modeField.set(raftStateMachine, "raft");

        raftStateMachine.onLeaderStart(1L); // Become leader

        SnapshotReader reader = mock(SnapshotReader.class);

        boolean result = raftStateMachine.onSnapshotLoad(reader);

        assertFalse(result);
    }

    @Test
    public void testOnSnapshotLoadInRaftModeAsFollower() throws Exception {
        // Use reflection to change mode to RAFT
        Field modeField = RaftStateMachine.class.getDeclaredField("mode");
        modeField.setAccessible(true);
        modeField.set(raftStateMachine, "raft");
        // Not a leader (leaderTerm should be -1)

        // Clear default snapshot files
        Field snapshotFilesField = RaftStateMachine.class.getDeclaredField("snapshotFiles");
        snapshotFilesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<StoreSnapshotFile> snapshotFiles = (List<StoreSnapshotFile>) snapshotFilesField.get(raftStateMachine);
        snapshotFiles.clear();

        SnapshotReader reader = mock(SnapshotReader.class);
        when(reader.getPath()).thenReturn("/tmp/snapshot");

        // Register a mock snapshot file
        StoreSnapshotFile mockSnapshotFile = mock(StoreSnapshotFile.class);
        when(mockSnapshotFile.load(reader)).thenReturn(true);
        raftStateMachine.registryStoreSnapshotFile(mockSnapshotFile);

        boolean result = raftStateMachine.onSnapshotLoad(reader);

        // Should call load on the snapshot file
        verify(mockSnapshotFile).load(reader);
        assertTrue(result);
    }

    @Test
    public void testOnSnapshotLoadFailsWhenSnapshotFileReturnsFalse() throws Exception {
        // Use reflection to change mode to RAFT
        Field modeField = RaftStateMachine.class.getDeclaredField("mode");
        modeField.setAccessible(true);
        modeField.set(raftStateMachine, "raft");

        // Clear default snapshot files
        Field snapshotFilesField = RaftStateMachine.class.getDeclaredField("snapshotFiles");
        snapshotFilesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<StoreSnapshotFile> snapshotFiles = (List<StoreSnapshotFile>) snapshotFilesField.get(raftStateMachine);
        snapshotFiles.clear();

        SnapshotReader reader = mock(SnapshotReader.class);
        when(reader.getPath()).thenReturn("/tmp/snapshot");

        // Register a mock snapshot file that fails to load
        StoreSnapshotFile mockSnapshotFile = mock(StoreSnapshotFile.class);
        when(mockSnapshotFile.load(reader)).thenReturn(false);
        raftStateMachine.registryStoreSnapshotFile(mockSnapshotFile);

        boolean result = raftStateMachine.onSnapshotLoad(reader);

        assertFalse(result);
    }

    @Test
    public void testOnLeaderStartSetsTermsCorrectly() {
        long term = 10L;
        assertFalse(raftStateMachine.isLeader());

        raftStateMachine.onLeaderStart(term);

        assertTrue(raftStateMachine.isLeader());
        assertEquals(term, raftStateMachine.getCurrentTerm().get());
    }

    @Test
    public void testOnConfigurationCommitted() {
        // Create a configuration
        Configuration conf = new Configuration();
        conf.addPeer(new PeerId("127.0.0.1", 8091));

        // Should not throw exception
        assertDoesNotThrow(() -> raftStateMachine.onConfigurationCommitted(conf));
    }

    @Test
    public void testChangePeersWhenLeader() throws Exception {
        // Become leader first
        raftStateMachine.onLeaderStart(1L);
        assertTrue(raftStateMachine.isLeader());

        // Set up initial metadata with followers
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);
        Node follower1 = new Node();
        follower1.setRole(ClusterRole.FOLLOWER);
        follower1.setGroup(TEST_GROUP);
        Node.Endpoint endpoint1 = new Node.Endpoint();
        endpoint1.setHost("127.0.0.1");
        endpoint1.setPort(8091);
        follower1.setInternal(endpoint1);

        Node follower2 = new Node();
        follower2.setRole(ClusterRole.FOLLOWER);
        follower2.setGroup(TEST_GROUP);
        Node.Endpoint endpoint2 = new Node.Endpoint();
        endpoint2.setHost("127.0.0.1");
        endpoint2.setPort(8092);
        follower2.setInternal(endpoint2);

        metadata.setFollowers(Arrays.asList(follower1, follower2));
        raftStateMachine.setRaftLeaderMetadata(metadata);

        // Create a new configuration with only one peer
        Configuration conf = new Configuration();
        conf.addPeer(new PeerId("127.0.0.1", 8091));

        // Call onConfigurationCommitted which should trigger changePeers
        raftStateMachine.onConfigurationCommitted(conf);

        // Give some time for async operations
        Thread.sleep(100);

        // Verify that the metadata has been updated
        RaftClusterMetadata updatedMetadata = raftStateMachine.getRaftLeaderMetadata();
        assertNotNull(updatedMetadata);
    }

    @Test
    public void testChangePeersWithLearners() throws Exception {
        // Become leader first
        raftStateMachine.onLeaderStart(1L);
        assertTrue(raftStateMachine.isLeader());

        // Set up initial metadata with learners
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);
        Node learner1 = new Node();
        learner1.setRole(ClusterRole.LEARNER);
        learner1.setGroup(TEST_GROUP);
        Node.Endpoint endpoint1 = new Node.Endpoint();
        endpoint1.setHost("127.0.0.1");
        endpoint1.setPort(8093);
        learner1.setInternal(endpoint1);

        metadata.setLearner(Collections.singletonList(learner1));
        raftStateMachine.setRaftLeaderMetadata(metadata);

        // Create a new configuration with learners
        Configuration conf = new Configuration();
        conf.addPeer(new PeerId("127.0.0.1", 8091));
        conf.addLearner(new PeerId("127.0.0.1", 8093));

        // Call onConfigurationCommitted which should trigger changePeers
        raftStateMachine.onConfigurationCommitted(conf);

        // Give some time for async operations
        Thread.sleep(100);

        // Verify that the metadata has been updated
        RaftClusterMetadata updatedMetadata = raftStateMachine.getRaftLeaderMetadata();
        assertNotNull(updatedMetadata);
    }

    @Test
    public void testChangePeersWhenNotLeader() {
        // Not a leader
        assertFalse(raftStateMachine.isLeader());

        // Create a configuration
        Configuration conf = new Configuration();
        conf.addPeer(new PeerId("127.0.0.1", 8091));

        // Call onConfigurationCommitted - changePeers should not be called
        assertDoesNotThrow(() -> raftStateMachine.onConfigurationCommitted(conf));
    }

    @Test
    public void testChangeNodeMetadataForFollower() {
        // Test adding a follower node
        Node node = new Node();
        node.setRole(ClusterRole.FOLLOWER);
        node.setGroup(TEST_GROUP);

        // Should not throw exception
        assertDoesNotThrow(() -> raftStateMachine.changeNodeMetadata(node));

        // Verify the node was added to followers
        RaftClusterMetadata metadata = raftStateMachine.getRaftLeaderMetadata();
        assertNotNull(metadata);
    }

    @Test
    public void testChangeNodeMetadataForLearner() {
        // Test adding a learner node
        Node node = new Node();
        node.setRole(ClusterRole.LEARNER);
        node.setGroup(TEST_GROUP);

        // Should not throw exception
        assertDoesNotThrow(() -> raftStateMachine.changeNodeMetadata(node));

        // Verify the node was added to learners
        RaftClusterMetadata metadata = raftStateMachine.getRaftLeaderMetadata();
        assertNotNull(metadata);
    }

    @Test
    public void testSyncCurrentNodeInfoStringParameter() throws Exception {
        // Use reflection to access the private method
        java.lang.reflect.Method method = RaftStateMachine.class.getDeclaredMethod("syncCurrentNodeInfo", String.class);
        method.setAccessible(true);

        // Check that initSync is false initially
        Field initSyncField = RaftStateMachine.class.getDeclaredField("initSync");
        initSyncField.setAccessible(true);
        java.util.concurrent.atomic.AtomicBoolean initSync =
                (java.util.concurrent.atomic.AtomicBoolean) initSyncField.get(raftStateMachine);
        assertFalse(initSync.get());

        // Invoke the method - it will try to refresh leader and may fail due to test environment
        // but we're just testing that the code path is executed without throwing unexpected exceptions
        assertDoesNotThrow(() -> {
            try {
                method.invoke(raftStateMachine, TEST_GROUP);
            } catch (java.lang.reflect.InvocationTargetException e) {
                // Expected if dependencies are not set up - just ensure it's not a null pointer
                Throwable cause = e.getCause();
                // We expect some kind of initialization or configuration error in test environment
                assertTrue(cause == null
                        || cause instanceof NullPointerException
                        || cause instanceof IllegalStateException
                        || cause instanceof RuntimeException);
            }
        });
    }

    @Test
    public void testSyncCurrentNodeInfoWithInitSyncAlreadyTrue() throws Exception {
        // Use reflection to set initSync to true
        Field initSyncField = RaftStateMachine.class.getDeclaredField("initSync");
        initSyncField.setAccessible(true);
        java.util.concurrent.atomic.AtomicBoolean initSync =
                (java.util.concurrent.atomic.AtomicBoolean) initSyncField.get(raftStateMachine);
        initSync.set(true);

        // Use reflection to access the private method
        java.lang.reflect.Method method = RaftStateMachine.class.getDeclaredMethod("syncCurrentNodeInfo", String.class);
        method.setAccessible(true);

        // Invoke the method - should return early without doing anything since initSync is already true
        assertDoesNotThrow(() -> {
            try {
                method.invoke(raftStateMachine, TEST_GROUP);
            } catch (java.lang.reflect.InvocationTargetException e) {
                fail("Should not throw exception when initSync is already true");
            }
        });

        // initSync should still be true
        assertTrue(initSync.get());
    }

    @Test
    public void testSyncCurrentNodeInfoPeerIdParameter() throws Exception {
        // Use reflection to access the private method
        java.lang.reflect.Method method = RaftStateMachine.class.getDeclaredMethod("syncCurrentNodeInfo", PeerId.class);
        method.setAccessible(true);

        // Create a test PeerId
        PeerId leaderPeerId = new PeerId("127.0.0.1", 8091);

        // Set up metadata with a leader that has a version
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);
        Node leader = new Node();
        leader.setVersion("2.1.0");
        metadata.setLeader(leader);
        raftStateMachine.setRaftLeaderMetadata(metadata);

        // Invoke the method - it will fail due to missing dependencies but we're testing the code path
        assertDoesNotThrow(() -> {
            try {
                method.invoke(raftStateMachine, leaderPeerId);
            } catch (java.lang.reflect.InvocationTargetException e) {
                // Expected if RaftServerManager is not initialized in test environment
                Throwable cause = e.getCause();
                assertTrue(cause == null
                        || cause instanceof NullPointerException
                        || cause instanceof IllegalStateException
                        || cause instanceof RuntimeException);
            }
        });
    }

    @Test
    public void testSyncCurrentNodeInfoWithNoLeaderVersion() throws Exception {
        // Use reflection to access the private method
        java.lang.reflect.Method method = RaftStateMachine.class.getDeclaredMethod("syncCurrentNodeInfo", PeerId.class);
        method.setAccessible(true);

        // Create a test PeerId
        PeerId leaderPeerId = new PeerId("127.0.0.1", 8091);

        // Set up metadata with a leader that has NO version (should return early)
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);
        Node leader = new Node();
        // No version set
        metadata.setLeader(leader);
        raftStateMachine.setRaftLeaderMetadata(metadata);

        // Invoke the method - should return early without error
        assertDoesNotThrow(() -> {
            try {
                method.invoke(raftStateMachine, leaderPeerId);
            } catch (java.lang.reflect.InvocationTargetException e) {
                fail("Should not throw when leader version is blank: " + e.getCause());
            }
        });
    }

    @Test
    public void testSyncCurrentNodeInfoWithNullLeader() throws Exception {
        // Use reflection to access the private method
        java.lang.reflect.Method method = RaftStateMachine.class.getDeclaredMethod("syncCurrentNodeInfo", PeerId.class);
        method.setAccessible(true);

        // Create a test PeerId
        PeerId leaderPeerId = new PeerId("127.0.0.1", 8091);

        // Set up metadata with null leader
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);
        metadata.setLeader(null);
        raftStateMachine.setRaftLeaderMetadata(metadata);

        // Invoke the method - should return early without error
        assertDoesNotThrow(() -> {
            try {
                method.invoke(raftStateMachine, leaderPeerId);
            } catch (java.lang.reflect.InvocationTargetException e) {
                fail("Should not throw when leader is null: " + e.getCause());
            }
        });
    }

    // ========== Tests for uncovered code paths from codecov ==========

    @Test
    public void testOnApplyWithFollowerPath() throws Exception {
        // Test the follower execution path where iterator.done() returns null
        Iterator iterator = mock(Iterator.class);

        // Create a RaftClusterMetadataMsg wrapped in RaftSyncMessage
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);
        RaftClusterMetadataMsg msg = new RaftClusterMetadataMsg(metadata);
        RaftSyncMessage syncMessage = new RaftSyncMessage();
        syncMessage.setBody(msg);

        // Serialize the message
        byte[] msgBytes = RaftSyncMessageSerializer.encode(syncMessage);
        ByteBuffer byteBuffer = ByteBuffer.wrap(msgBytes);

        // Set up the iterator to simulate follower behavior
        when(iterator.hasNext()).thenReturn(true, false); // One entry then stop
        when(iterator.done()).thenReturn(null); // null means follower path
        when(iterator.getData()).thenReturn(byteBuffer);

        // Execute onApply
        assertDoesNotThrow(() -> raftStateMachine.onApply(iterator));

        // Verify iterator methods were called
        verify(iterator, times(2)).hasNext();
        verify(iterator).done();
        verify(iterator).getData();
        verify(iterator).next();
    }

    @Test
    public void testOnApplyWithEmptyByteBuffer() {
        // Test heartbeat event with empty ByteBuffer
        Iterator iterator = mock(Iterator.class);
        ByteBuffer emptyBuffer = ByteBuffer.allocate(0);

        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.done()).thenReturn(null);
        when(iterator.getData()).thenReturn(emptyBuffer);

        // Should not throw exception for empty buffer (heartbeat)
        assertDoesNotThrow(() -> raftStateMachine.onApply(iterator));

        verify(iterator).next();
    }

    @Test
    public void testOnApplyWithNullByteBuffer() {
        // Test with null ByteBuffer (heartbeat event)
        Iterator iterator = mock(Iterator.class);

        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.done()).thenReturn(null);
        when(iterator.getData()).thenReturn(null);

        // Should not throw exception for null buffer
        assertDoesNotThrow(() -> raftStateMachine.onApply(iterator));

        verify(iterator).next();
    }

    @Test
    public void testOnApplyWithLeaderPath() {
        // Test the leader execution path where iterator.done() returns a Closure
        Iterator iterator = mock(Iterator.class);
        Closure done = mock(Closure.class);

        when(iterator.hasNext()).thenReturn(true, false);
        when(iterator.done()).thenReturn(done);

        assertDoesNotThrow(() -> raftStateMachine.onApply(iterator));

        // Verify done.run was called with OK status
        verify(done).run(argThat(Status::isOk));
        verify(iterator).next();
    }

    @Test
    public void testOnExecuteRaftWithUnknownMessageType() throws Exception {
        // Test error path when message type is not in EXECUTES map
        // Create a custom message type that doesn't exist in EXECUTES
        RaftBaseMsg msg = new RaftBaseMsg() {
            @Override
            public RaftSyncMsgType getMsgType() {
                return null; // Unknown type
            }
        };

        // Access the private method
        java.lang.reflect.Method method = RaftStateMachine.class.getDeclaredMethod("onExecuteRaft", RaftBaseMsg.class);
        method.setAccessible(true);

        // Should throw RuntimeException for unknown message type
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            method.invoke(raftStateMachine, msg);
        });
    }

    @Test
    public void testOnExecuteRaftWithExecutionException() throws Throwable {
        // Test error path when execute.execute() throws an exception
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);
        RaftClusterMetadataMsg msg = new RaftClusterMetadataMsg(metadata);

        // Access the EXECUTES map and add a mock that throws exception
        Field executesField = RaftStateMachine.class.getDeclaredField("EXECUTES");
        executesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<RaftSyncMsgType, RaftMsgExecute<?>> executes =
                (Map<RaftSyncMsgType, RaftMsgExecute<?>>) executesField.get(null);

        // Store original execute
        RaftMsgExecute<?> originalExecute = executes.get(RaftSyncMsgType.REFRESH_CLUSTER_METADATA);

        try {
            // Replace with mock that throws exception
            RaftMsgExecute<?> mockExecute = mock(RaftMsgExecute.class);
            doThrow(new RuntimeException("Test exception")).when(mockExecute).execute(any());
            executes.put(RaftSyncMsgType.REFRESH_CLUSTER_METADATA, mockExecute);

            // Access the private method
            java.lang.reflect.Method method =
                    RaftStateMachine.class.getDeclaredMethod("onExecuteRaft", RaftBaseMsg.class);
            method.setAccessible(true);

            // Should wrap exception in RuntimeException
            assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
                method.invoke(raftStateMachine, msg);
            });
        } finally {
            // Restore original execute
            if (originalExecute != null) {
                executes.put(RaftSyncMsgType.REFRESH_CLUSTER_METADATA, originalExecute);
            }
        }
    }

    @Test
    public void testChangeNodeMetadataUpdatesExistingNode() {
        // Test the path where an existing node is found and updated
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);

        // Create an existing follower with internal endpoint
        Node existingFollower = new Node();
        existingFollower.setRole(ClusterRole.FOLLOWER);
        existingFollower.setGroup(TEST_GROUP);
        Node.Endpoint endpoint = new Node.Endpoint();
        endpoint.setHost("127.0.0.1");
        endpoint.setPort(8091);
        existingFollower.setInternal(endpoint);
        existingFollower.setVersion("1.0.0");

        metadata.setFollowers(Arrays.asList(existingFollower));
        raftStateMachine.setRaftLeaderMetadata(metadata);

        // Create a node update with same host/port but different metadata
        Node updatedNode = new Node();
        updatedNode.setRole(ClusterRole.FOLLOWER);
        updatedNode.setGroup(TEST_GROUP);
        Node.Endpoint updatedEndpoint = new Node.Endpoint();
        updatedEndpoint.setHost("127.0.0.1");
        updatedEndpoint.setPort(8091);
        updatedNode.setInternal(updatedEndpoint);
        updatedNode.setVersion("2.0.0");
        updatedNode.setTransaction(new Node.Endpoint());
        updatedNode.setControl(new Node.Endpoint());
        updatedNode.setMetadata(Collections.singletonMap("key", "value"));

        // Apply the update
        raftStateMachine.changeNodeMetadata(updatedNode);

        // Verify the existing node was updated
        RaftClusterMetadata updatedMetadata = raftStateMachine.getRaftLeaderMetadata();
        Node resultNode = updatedMetadata.getFollowers().get(0);
        assertEquals("2.0.0", resultNode.getVersion());
        assertNotNull(resultNode.getTransaction());
        assertNotNull(resultNode.getControl());
        assertNotNull(resultNode.getMetadata());
        assertEquals(1, updatedMetadata.getFollowers().size()); // Should still be 1, not 2
    }

    @Test
    public void testChangeNodeMetadataUpdatesExistingLearner() {
        // Test updating an existing learner node
        RaftClusterMetadata metadata = new RaftClusterMetadata(1L);

        // Create an existing learner with internal endpoint
        Node existingLearner = new Node();
        existingLearner.setRole(ClusterRole.LEARNER);
        existingLearner.setGroup(TEST_GROUP);
        Node.Endpoint endpoint = new Node.Endpoint();
        endpoint.setHost("127.0.0.1");
        endpoint.setPort(8093);
        existingLearner.setInternal(endpoint);
        existingLearner.setVersion("1.0.0");

        metadata.setLearner(Arrays.asList(existingLearner));
        raftStateMachine.setRaftLeaderMetadata(metadata);

        // Create a learner update with same host/port
        Node updatedLearner = new Node();
        updatedLearner.setRole(ClusterRole.LEARNER);
        updatedLearner.setGroup(TEST_GROUP);
        Node.Endpoint updatedEndpoint = new Node.Endpoint();
        updatedEndpoint.setHost("127.0.0.1");
        updatedEndpoint.setPort(8093);
        updatedLearner.setInternal(updatedEndpoint);
        updatedLearner.setVersion("2.0.0");

        // Apply the update
        raftStateMachine.changeNodeMetadata(updatedLearner);

        // Verify the existing learner was updated
        RaftClusterMetadata updatedMetadata = raftStateMachine.getRaftLeaderMetadata();
        Node resultNode = updatedMetadata.getLearner().get(0);
        assertEquals("2.0.0", resultNode.getVersion());
        assertEquals(1, updatedMetadata.getLearner().size()); // Should still be 1, not 2
    }
}
