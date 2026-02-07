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
package org.apache.seata.server.cluster.manager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.metadata.MetadataResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.RaftServer;
import org.apache.seata.server.cluster.raft.RaftServerManager;
import org.apache.seata.server.cluster.raft.RaftStateMachine;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.cluster.watch.Watcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.apache.seata.common.ConfigurationKeys.STORE_MODE;
import static org.apache.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClusterWatcherManagerTest extends BaseSpringBootTest {

    private ClusterWatcherManager clusterWatcherManager;
    private ChannelHandlerContext mockChannelHandlerContext;
    private Channel mockChannel;
    private HttpContext<Object> httpContext;

    private static final String TEST_GROUP = "test-group";
    private static final int TEST_TIMEOUT = 5000;
    private static final long TEST_TERM = 1000L;

    @BeforeEach
    void setUp() {
        clusterWatcherManager = new ClusterWatcherManager();

        mockChannel = mock(Channel.class);
        mockChannelHandlerContext = mock(ChannelHandlerContext.class);
        when(mockChannelHandlerContext.channel()).thenReturn(mockChannel);

        Object mockRequest = new Object();
        httpContext = new HttpContext<>(mockRequest, mockChannelHandlerContext, true, HttpContext.HTTP_1_1);

        Map<String, Queue<Watcher<HttpContext>>> http1Watchers = (Map<String, Queue<Watcher<HttpContext>>>)
                ReflectionTestUtils.getField(clusterWatcherManager, "HTTP1_WATCHERS");
        Map<String, Queue<Watcher<HttpContext>>> http2Watchers = (Map<String, Queue<Watcher<HttpContext>>>)
                ReflectionTestUtils.getField(clusterWatcherManager, "HTTP2_WATCHERS");
        Map<String, Long> groupUpdateTerm =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");
        if (http1Watchers != null) {
            http1Watchers.clear();
        }
        if (http2Watchers != null) {
            http2Watchers.clear();
        }
        if (groupUpdateTerm != null) {
            groupUpdateTerm.clear();
        }
    }

    @AfterEach
    void tearDown() {
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor)
                ReflectionTestUtils.getField(clusterWatcherManager, "scheduledThreadPoolExecutor");
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void testOnChangeEventWithInactiveChannel() {
        when(mockChannel.isActive()).thenReturn(false);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, httpContext, TEST_TIMEOUT, TEST_TERM);

        clusterWatcherManager.registryWatcher(watcher);

        // HTTP/1.1 watcher should be in HTTP1_WATCHERS
        Map<String, Queue<Watcher<HttpContext>>> http1Watchers = (Map<String, Queue<Watcher<HttpContext>>>)
                ReflectionTestUtils.getField(clusterWatcherManager, "HTTP1_WATCHERS");
        assertTrue(http1Watchers.containsKey(TEST_GROUP));
        assertEquals(1, http1Watchers.get(TEST_GROUP).size());

        ClusterChangeEvent event = new ClusterChangeEvent(this, TEST_GROUP, TEST_TERM + 1, true);

        assertDoesNotThrow(() -> {
            clusterWatcherManager.onChangeEvent(event);
        });

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, never()).write(any());
        verify(mockChannelHandlerContext, never()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }

    @Test
    void testTimeoutWithInactiveChannel() throws InterruptedException {
        when(mockChannel.isActive()).thenReturn(false);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, httpContext, 1000, TEST_TERM);

        clusterWatcherManager.registryWatcher(watcher);

        clusterWatcherManager.init();

        Thread.sleep(2500);

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, never()).write(any());
        verify(mockChannelHandlerContext, never()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }

    @Test
    void testRegistryWatcherWithInactiveChannel() {
        when(mockChannel.isActive()).thenReturn(false);

        Map<String, Long> groupUpdateTerm =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");
        groupUpdateTerm.put(TEST_GROUP, TEST_TERM + 10);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, httpContext, TEST_TIMEOUT, TEST_TERM);

        assertDoesNotThrow(() -> {
            clusterWatcherManager.registryWatcher(watcher);
        });

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, never()).write(any());
        verify(mockChannelHandlerContext, never()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }

    // --- getMetadataResponse unit tests ---

    @Test
    void getMetadataResponse_whenGroupBlank_usesDefaultGroupFromConfig() {
        String defaultGroup = "default-from-config";
        Configuration mockConfig = mock(Configuration.class);
        when(mockConfig.getConfig(eq(ConfigurationKeys.SERVER_RAFT_GROUP), eq(DEFAULT_SEATA_GROUP)))
                .thenReturn(defaultGroup);

        try (MockedStatic<ConfigurationFactory> configFactoryMock = Mockito.mockStatic(ConfigurationFactory.class);
                MockedStatic<RaftServerManager> raftManagerMock = Mockito.mockStatic(RaftServerManager.class)) {
            configFactoryMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            raftManagerMock
                    .when(() -> RaftServerManager.getRaftServer(defaultGroup))
                    .thenReturn(null);

            MetadataResponse response = clusterWatcherManager.getMetadataResponse(null);
            assertNotNull(response);
            assertNull(response.getNodes());
            assertNull(response.getStoreMode());

            response = clusterWatcherManager.getMetadataResponse("");
            assertNotNull(response);
            assertNull(response.getNodes());
        }
    }

    @Test
    void getMetadataResponse_whenRaftServerNull_returnsEmptyResponse() {
        try (MockedStatic<RaftServerManager> raftManagerMock = Mockito.mockStatic(RaftServerManager.class)) {
            raftManagerMock
                    .when(() -> RaftServerManager.getRaftServer(TEST_GROUP))
                    .thenReturn(null);

            MetadataResponse response = clusterWatcherManager.getMetadataResponse(TEST_GROUP);

            assertNotNull(response);
            assertNull(response.getNodes());
            assertNull(response.getStoreMode());
        }
    }

    @Test
    void getMetadataResponse_whenRaftServerNotNull_leaderNull_returnsResponseWithStoreModeOnly() {
        RaftServer mockRaftServer = mock(RaftServer.class);
        RaftStateMachine mockStateMachine = mock(RaftStateMachine.class);
        RaftClusterMetadata metadata = new RaftClusterMetadata(10L);
        metadata.setLeader(null);
        metadata.setFollowers(Collections.emptyList());
        metadata.setLearner(Collections.emptyList());

        when(mockRaftServer.getRaftStateMachine()).thenReturn(mockStateMachine);
        when(mockStateMachine.getRaftLeaderMetadata()).thenReturn(metadata);

        Configuration mockConfig = mock(Configuration.class);
        when(mockConfig.getConfig(STORE_MODE)).thenReturn("raft");

        try (MockedStatic<RaftServerManager> raftManagerMock = Mockito.mockStatic(RaftServerManager.class);
                MockedStatic<ConfigurationFactory> configFactoryMock = Mockito.mockStatic(ConfigurationFactory.class)) {
            raftManagerMock
                    .when(() -> RaftServerManager.getRaftServer(TEST_GROUP))
                    .thenReturn(mockRaftServer);
            configFactoryMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);

            MetadataResponse response = clusterWatcherManager.getMetadataResponse(TEST_GROUP);

            assertNotNull(response);
            assertEquals("raft", response.getStoreMode());
            assertNull(response.getNodes());
        }
    }

    @Test
    void getMetadataResponse_whenRaftServerNotNull_leaderNotNull_returnsFullResponse() {
        Node leader = new Node();
        leader.setGroup(TEST_GROUP);
        leader.setControl(new Node.Endpoint("127.0.0.1", 7091));
        leader.setTransaction(new Node.Endpoint("127.0.0.1", 8091));
        Node follower = new Node();
        follower.setGroup(TEST_GROUP);
        follower.setControl(new Node.Endpoint("127.0.0.2", 7092));
        follower.setTransaction(new Node.Endpoint("127.0.0.2", 8092));
        List<Node> followers = new ArrayList<>();
        followers.add(follower);
        List<Node> learners = new ArrayList<>();

        RaftClusterMetadata metadata = new RaftClusterMetadata(100L);
        metadata.setLeader(leader);
        metadata.setFollowers(followers);
        metadata.setLearner(learners);

        RaftServer mockRaftServer = mock(RaftServer.class);
        RaftStateMachine mockStateMachine = mock(RaftStateMachine.class);
        when(mockRaftServer.getRaftStateMachine()).thenReturn(mockStateMachine);
        when(mockStateMachine.getRaftLeaderMetadata()).thenReturn(metadata);

        Configuration mockConfig = mock(Configuration.class);
        when(mockConfig.getConfig(STORE_MODE)).thenReturn("raft");

        try (MockedStatic<RaftServerManager> raftManagerMock = Mockito.mockStatic(RaftServerManager.class);
                MockedStatic<ConfigurationFactory> configFactoryMock = Mockito.mockStatic(ConfigurationFactory.class)) {
            raftManagerMock
                    .when(() -> RaftServerManager.getRaftServer(TEST_GROUP))
                    .thenReturn(mockRaftServer);
            configFactoryMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);

            MetadataResponse response = clusterWatcherManager.getMetadataResponse(TEST_GROUP);

            assertNotNull(response);
            assertEquals("raft", response.getStoreMode());
            assertEquals(100L, response.getTerm());
            assertNotNull(response.getNodes());
            assertEquals(2, response.getNodes().size());
            assertEquals(TEST_GROUP, response.getNodes().get(0).getGroup());
            assertEquals(TEST_GROUP, response.getNodes().get(1).getGroup());
        }
    }

    @Test
    void getMetadataResponse_whenGetRaftLeaderMetadataThrows_returnsResponseAndLogsError() {
        RaftServer mockRaftServer = mock(RaftServer.class);
        RaftStateMachine mockStateMachine = mock(RaftStateMachine.class);
        when(mockRaftServer.getRaftStateMachine()).thenReturn(mockStateMachine);
        when(mockStateMachine.getRaftLeaderMetadata()).thenThrow(new RuntimeException("test exception"));

        Configuration mockConfig = mock(Configuration.class);
        when(mockConfig.getConfig(STORE_MODE)).thenReturn("raft");

        try (MockedStatic<RaftServerManager> raftManagerMock = Mockito.mockStatic(RaftServerManager.class);
                MockedStatic<ConfigurationFactory> configFactoryMock = Mockito.mockStatic(ConfigurationFactory.class)) {
            raftManagerMock
                    .when(() -> RaftServerManager.getRaftServer(TEST_GROUP))
                    .thenReturn(mockRaftServer);
            configFactoryMock.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);

            MetadataResponse response = clusterWatcherManager.getMetadataResponse(TEST_GROUP);

            assertNotNull(response);
            assertEquals("raft", response.getStoreMode());
            assertNull(response.getNodes());
        }
    }
}
