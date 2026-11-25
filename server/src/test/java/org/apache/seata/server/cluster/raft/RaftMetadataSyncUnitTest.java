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

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.rpc.*;
import com.alipay.sofa.jraft.rpc.impl.cli.CliClientServiceImpl;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.metadata.*;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.raft.processor.request.PutNodeMetadataRequest;
import org.apache.seata.server.cluster.raft.processor.response.PutNodeMetadataResponse;
import org.apache.seata.server.cluster.raft.sync.msg.dto.RaftClusterMetadata;
import org.apache.seata.server.store.StoreConfig;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.seata.common.ConfigurationKeys.SERVER_REGISTRY_PORT_CAMEL;
import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RaftMetadataSyncUnitTest extends BaseSpringBootTest {
    @Test
    void metadataSyncPublishesRoleAndHandlesSuccessRejectionAndTransportFailure() throws Exception {
        RaftStateMachine machine = mock(RaftStateMachine.class, CALLS_REAL_METHODS);
        RaftClusterMetadata metadata = new RaftClusterMetadata();
        Node leader = new Node();
        leader.setVersion("2.8.0");
        metadata.setLeader(leader);
        AtomicBoolean syncing = new AtomicBoolean(true);
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        ReflectionTestUtils.setField(machine, "group", "unit");
        ReflectionTestUtils.setField(machine, "initSync", syncing);
        ReflectionTestUtils.setField(machine, "scheduledFuture", future);
        machine.setRaftLeaderMetadata(metadata);
        PeerId local = new PeerId("127.0.0.1", 9091), remote = new PeerId("127.0.0.1", 9092);
        RaftServer server = mock(RaftServer.class);
        when(server.getServerId()).thenReturn(local);
        CliClientServiceImpl cli = mock(CliClientServiceImpl.class);
        RpcClient client = mock(RpcClient.class);
        when(cli.getRpcClient()).thenReturn(client);
        RouteTable routes = mock(RouteTable.class);
        when(routes.getConfiguration("unit")).thenReturn(new Configuration(Collections.singletonList(local)));
        try (MockedStatic<RaftServerManager> servers = mockStatic(RaftServerManager.class);
                MockedStatic<RouteTable> table = mockStatic(RouteTable.class)) {
            servers.when(() -> RaftServerManager.getRaftServer("unit")).thenReturn(server);
            servers.when(RaftServerManager::getCliClientServiceInstance).thenReturn(cli);
            table.when(RouteTable::getInstance).thenReturn(routes);
            ReflectionTestUtils.invokeMethod(machine, "syncCurrentNodeInfo", remote);
            ArgumentCaptor<InvokeCallback> callback = ArgumentCaptor.forClass(InvokeCallback.class);
            ArgumentCaptor<PutNodeMetadataRequest> request = ArgumentCaptor.forClass(PutNodeMetadataRequest.class);
            verify(client)
                    .invokeAsync(
                            eq(remote.getEndpoint()),
                            request.capture(),
                            any(InvokeContext.class),
                            callback.capture(),
                            eq(30000L));
            assertEquals(ClusterRole.FOLLOWER, request.getValue().getNode().getRole());
            PutNodeMetadataResponse response = new PutNodeMetadataResponse(true);
            response.setSuccess(true);
            callback.getValue().complete(response, null);
            verify(future).cancel(true);
            response.setSuccess(false);
            callback.getValue().complete(response, null);
            assertFalse(syncing.get());
            syncing.set(true);
            callback.getValue().complete(null, new IllegalStateException("offline"));
            assertFalse(syncing.get());
            metadata.setLeader(null);
            syncing.set(true);
            ReflectionTestUtils.invokeMethod(machine, "syncCurrentNodeInfo", remote);
            assertFalse(syncing.get());
            when(routes.selectLeader("unit")).thenReturn(null);
            ReflectionTestUtils.invokeMethod(machine, "syncCurrentNodeInfo", "unit");
            assertFalse(syncing.get());
        }
    }

    @Test
    void metadataAdvertisesRegistryPortWhenItDiffersFromListenPort() throws Exception {
        int listenPort = 28091;
        int registryPort = 29091;
        Object originalEnvironment = ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        StoreConfig.setStartupParameter("file", "file", "file");
        XID.setIpAddress("127.0.0.1");
        XID.setPort(listenPort);
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("server.port", "18091");
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, environment);
        System.setProperty(SERVER_REGISTRY_PORT_CAMEL, String.valueOf(registryPort));
        ConfigurationCache.clear();
        try {
            PeerId local = new PeerId("127.0.0.1", 9091), remote = new PeerId("127.0.0.1", 9092);
            RaftServer server = mock(RaftServer.class);
            when(server.getServerId()).thenReturn(local);
            CliClientServiceImpl cli = mock(CliClientServiceImpl.class);
            RpcClient client = mock(RpcClient.class);
            when(cli.getRpcClient()).thenReturn(client);
            RouteTable routes = mock(RouteTable.class);
            when(routes.getConfiguration("unit")).thenReturn(new Configuration(Collections.singletonList(local)));
            try (MockedStatic<RaftServerManager> servers = mockStatic(RaftServerManager.class);
                    MockedStatic<RouteTable> table = mockStatic(RouteTable.class)) {
                servers.when(() -> RaftServerManager.getRaftServer("unit")).thenReturn(server);
                servers.when(RaftServerManager::getCliClientServiceInstance).thenReturn(cli);
                table.when(RouteTable::getInstance).thenReturn(routes);

                RaftStateMachine machine = new RaftStateMachine("unit");

                // metadata refresh after a leader change must advertise the registry port
                RaftClusterMetadata metadata = machine.changeOrInitRaftClusterMetadata();
                Node leader = metadata.getLeader();
                assertNotNull(leader);
                assertNotNull(leader.getTransaction());
                assertEquals(registryPort, leader.getTransaction().getPort());
                assertNotEquals(listenPort, leader.getTransaction().getPort());
                assertEquals(local.getIp(), leader.getTransaction().getHost());
                assertEquals(local.getPort(), leader.getInternal().getPort());

                // the metadata a follower reports to the leader must advertise the registry port as well
                machine.setRaftLeaderMetadata(metadata);
                ReflectionTestUtils.setField(machine, "scheduledFuture", mock(ScheduledFuture.class));
                ReflectionTestUtils.invokeMethod(machine, "syncCurrentNodeInfo", remote);

                ArgumentCaptor<PutNodeMetadataRequest> request = ArgumentCaptor.forClass(PutNodeMetadataRequest.class);
                verify(client)
                        .invokeAsync(
                                eq(remote.getEndpoint()),
                                request.capture(),
                                any(InvokeContext.class),
                                any(InvokeCallback.class),
                                eq(30000L));
                Node reported = request.getValue().getNode();
                assertNotNull(reported.getTransaction());
                assertEquals(registryPort, reported.getTransaction().getPort());
                assertNotEquals(listenPort, reported.getTransaction().getPort());
                assertEquals(local.getPort(), reported.getInternal().getPort());
                assertEquals(ClusterRole.FOLLOWER, reported.getRole());
            }
        } finally {
            System.clearProperty(SERVER_REGISTRY_PORT_CAMEL);
            ConfigurationCache.clear();
            if (originalEnvironment != null) {
                ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
            }
        }
    }
}
